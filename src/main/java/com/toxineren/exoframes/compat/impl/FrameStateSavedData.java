package com.toxineren.exoframes.compat.impl;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}

/**
 * Registro di tutti i telai del mondo, salvato nel DimensionDataStorage dell'overworld con nome {@link #NAME} (file
 * {@code data/redstone_exoframes_frames.dat}). Le operazioni sui telai stanno in {@link FrameRegistry}, la lettura e
 * la scrittura del tag con le regole di tolleranza in {@link FrameRegistryFormat}; qui restano il file, il recupero,
 * la sola lettura e il salvataggio, e un rimando di una riga per ogni operazione del registro.
 * <p>
 * Cio' che passa fra l'inventario di un giocatore e il registro (armatura stivata e restituita, pezzi e celle montati
 * e smontati, il telaio come oggetto) sta in due file, quello del giocatore e questo: ogni operazione strutturale lo
 * ricorda ({@link #structuralUnsaved()}) e all'uscita di un giocatore, quando il gioco scrive il suo file, si scrivono
 * i file di tutti i giocatori e il solo file di questo registro, non gli altri dati salvati dell'overworld
 * ({@link #saveWithPlayersIfNeeded}). Tutti i giocatori e non solo chi esce, perche' il segno strutturale e' uno solo
 * per il registro e puo' venire dall'operazione di un altro giocatore. Questo salvataggio non guarda {@code /save-off}:
 * il gioco scrive comunque il file del giocatore che esce ({@code PlayerList.remove} lo salva senza guardare
 * {@code /save-off}), e rispettarlo separerebbe la coppia file del giocatore e registro su cui si regge il registro
 * unico (design §4.5). I salvataggi del gioco scrivono i giocatori e poi i livelli. Resta un arresto brusco nei
 * millisecondi di un salvataggio. Mentre il salvataggio e' spento con {@code /save-off} non e' cosi': il gioco scrive
 * ancora i file dei giocatori al salvataggio automatico e all'uscita di un giocatore, questo file solo con
 * {@code /save-all}, alla chiusura del server o all'uscita di un giocatore che segue un'operazione strutturale.
 * <p>
 * Si usa un SavedData, e non gli attachment o le capability dei loader, perche' e' un'unica implementazione per i
 * quattro target e lo stato non dipende dall'entita' del giocatore (morte, respawn e cambio di dimensione non lo
 * toccano). Le condizioni di versione sono solo sulle firme di {@code save} e su {@code computeIfAbsent}. Su 1.21.1
 * il {@code DataFixTypes} della factory e' null (nessun datafixer per i dati della mod): Fabric API e NeoForge
 * saltano il datafix in quel caso.
 * <p>
 * Il file si scrive sempre per intero accanto e prende il posto del vecchio in un colpo, lasciando la copia
 * precedente ({@link RegistryFile#write}); una scrittura fallita lascia il dato da salvare. Un file che il gioco non
 * legge non vale mai come registro vuoto: la mod lo rilegge da se' e, se nemmeno cosi' si legge, si usa la copia
 * precedente o, se nemmeno quella si legge, il dato resta in sola lettura per la sessione con un ERROR che dice quale
 * file ripristinare ({@link #get(DimensionDataStorage, Path)}, {@link #afterUnreadFile}).
 * <p>
 * Un file che si legge ma non ha {@code schema} ne' alcuna delle sezioni {@code frames}, {@code worn}, {@code placed}
 * (scritto da altro o svuotato; anche un file senza {@code data}, che il gioco passa come tag vuoto) non e' un
 * registro: e' rovinato come un file che non si legge. Il dato resta in sola lettura per la sessione, con un ERROR che
 * nomina il file ({@link #reportedWithoutSections}); il file non si tocca e la copia precedente non si cerca. Il
 * registro vuoto e scrivibile, che nel gioco nasce solo per un mondo senza file, nei test viene da
 * {@link #createEmpty()}.
 */
final class FrameStateSavedData extends SavedData {
    static final String NAME = "redstone_exoframes_frames";
    /** Versione dello schema che questo codice sa leggere e scrive. */
    static final int CURRENT_SCHEMA = 2;
    /**
     * Il marcatore di schema che {@link #write} scrive per un dato in sola lettura perche' nulla si e' potuto leggere:
     * non e' un numero, quindi chi legge quel tag lo tiene in sola lettura e non lo prende mai per un registro vuoto.
     */
    static final String UNREADABLE_SCHEMA = "unreadable";
    /**
     * La cartella dei dati di ogni server ({@link #dataFolder}), con chiavi deboli: la voce di un server se ne va con
     * lui, e il percorso non tiene in vita nulla del server.
     */
    private static final Map<MinecraftServer, Path> DATA_FOLDERS = new MapMaker().weakKeys().makeMap();

    /** Che cosa costa usare la copia precedente, in coda all'ERROR che lo dice. */
    private static final String PREVIOUS_COPY_COST = "The frame registry is as it was at the save before the last "
            + "one: what was done to frames since then (armor stowed or returned, pieces mounted, frames placed) is "
            + "not in it, while the players' files are up to date";

    /** I telai e le voci non lette: le operazioni del registro, che vedono la sola lettura e i segni di questo dato. */
    private final FrameRegistry registry = new FrameRegistry(this::readOnly, this::setDirty, this::markStructural);
    /**
     * Il tag letto per intero quando il dato e' in sola lettura (schema piu' recente o non numerico, o ne' schema ne'
     * sezioni), altrimenti null.
     */
    private CompoundTag readOnlyRoot;
    /** Vero se il tag letto non aveva ne' schema ne' sezioni: non e' un registro ({@link #reportedWithoutSections}). */
    private boolean withoutSections;
    /** Vero dopo il primo ERROR per una modifica rifiutata, per scriverlo una volta sola. */
    private boolean refusalLogged;
    /** Vero se il dato e' nato vuoto dalla factory e i file non sono ancora stati guardati ({@link #get}). */
    private boolean createdEmpty;
    /** Il file che non si e' potuto leggere, ne' lui ne' la sua copia precedente: il dato e' in sola lettura. */
    private Path unreadableFile;
    /** Vero se un'operazione strutturale ha cambiato il dato dopo l'ultima scrittura riuscita del file. */
    private boolean structuralUnsaved;
    /** Vero dopo l'ERROR per una scrittura fallita, fino alla prossima scrittura riuscita. */
    private boolean writeFailureLogged;

    /**
     * Il dato si crea o si carica una volta per server: e' il momento in cui i risultati di lettura degli stack
     * ricordati da un server precedente (nello stesso processo, con altri registri) vanno dimenticati. (Anche un dato
     * aperto da un test su una copia del file li dimentica: al piu' un avviso per una voce illeggibile si ripete.)
     */
    private FrameStateSavedData() {
        StackTags.forgetOutcomes();
    }

    /**
     * Il registro vuoto e scrivibile, solo per i test: prende il posto del caricamento di un tag vuoto, che e' un file
     * senza sezioni e quindi in sola lettura.
     */
    static FrameStateSavedData createEmpty() {
        return new FrameStateSavedData();
    }

    /** Il dato del mondo del server, creato vuoto se non esiste ancora: {@link #get(DimensionDataStorage, Path)}. */
    static FrameStateSavedData get(MinecraftServer server) {
        return get(server.overworld().getDataStorage(), dataFolder(server));
    }

    /**
     * La cartella dei dati salvati dell'overworld, dove sta il file {@link #NAME}{@code .dat}. Si calcola una volta per
     * server e si ricorda ({@link #DATA_FOLDERS}): {@link #get(MinecraftServer)} la chiede per ogni giocatore a ogni
     * tick.
     */
    static Path dataFolder(MinecraftServer server) {
        return DATA_FOLDERS.computeIfAbsent(server,
                key -> key.getWorldPath(LevelResource.ROOT).resolve("data").normalize());
    }

    /**
     * Il dato di {@code storage}, che tiene i suoi file in {@code folder}: lo legge il gioco con la factory della
     * mod. Se il gioco non ha potuto leggere il file (lo dice solo nel log e fa nascere il dato vuoto), la prima
     * richiesta guarda i file ({@link RegistryFile#recover}): con la copia precedente leggibile il dato viene da quella,
     * e' da salvare e prende il posto di quello vuoto in {@code storage}; se nulla si legge il dato resta vuoto e in
     * sola lettura per la sessione ({@link #readOnly()}), cosi' nessuno lo riscrive sopra il file da ripristinare; se
     * nessuno dei due file esiste e' un mondo nuovo. Un dato letto senza schema ne' sezioni, dal gioco o qui, resta in
     * sola lettura come un file illeggibile, con un ERROR che nomina il file ({@link #reportedWithoutSections}). Il
     * server apre cosi' il suo dato; i test ne aprono altri sulla stessa cartella o su una copia.
     */
    static FrameStateSavedData get(DimensionDataStorage storage, Path folder) {
        Supplier<FrameStateSavedData> fresh = () -> {
            FrameStateSavedData data = new FrameStateSavedData();
            data.createdEmpty = true;
            return data;
        };
        Function<CompoundTag, FrameStateSavedData> loader = FrameStateSavedData::load;
        //? if >=1.21 {
        FrameStateSavedData data = storage.computeIfAbsent(
                new SavedData.Factory<>(fresh, (tag, provider) -> loader.apply(tag), null), NAME);
        //?} else {
        /*FrameStateSavedData data = storage.computeIfAbsent(loader, fresh, NAME);*/
        //?}
        if (!data.createdEmpty) {
            return data.reportedWithoutSections(folder.resolve(NAME + ".dat"));
        }
        data.createdEmpty = false;
        FrameStateSavedData recovered = afterUnreadFile(folder.resolve(NAME + ".dat"), data);
        if (recovered != data) {
            storage.set(NAME, recovered);
        }
        return recovered.reportedWithoutSections(folder.resolve(NAME + ".dat"));
    }

    /**
     * Se il dato e' stato letto da un tag senza schema ne' sezioni, la prima volta lo segna come un file illeggibile
     * ({@code file}, che {@link #load} non conosce) e lo scrive in un ERROR, una volta per dato: da li' {@link #write}
     * da' il marcatore {@link #UNREADABLE_SCHEMA} e {@link #logRefusedChange} dice il file da ripristinare. Il file non
     * si tocca e la copia precedente non si cerca.
     */
    private FrameStateSavedData reportedWithoutSections(Path file) {
        if (withoutSections && unreadableFile == null) {
            unreadableFile = file;
            RedstoneExoframes.LOGGER.error("Frame registry file {} has no schema and no sections: frames are "
                    + "read-only this session", file);
        }
        return this;
    }

    /**
     * Il dato da usare quando il gioco non ha letto {@code file} e ha dato {@code empty} (vedi {@link #get}). Prima la
     * mod legge da se' lo stesso file ({@link #readItself}): il gioco puo' non averlo letto per una ragione che non sta
     * nel file (un altro processo che lo teneva aperto, un errore nel percorso di lettura di un loader); se si legge,
     * si usa com'e', con un WARN, e nulla si sposta. Solo se non si legge nemmeno cosi': {@code empty} per un mondo
     * nuovo; il dato della copia precedente, da salvare, se si legge; altrimenti {@code empty} in sola lettura. Ogni
     * caso diverso dal mondo nuovo scrive un ERROR che dice che cosa e' successo e che cosa fare; quello della copia
     * precedente dice anche che cosa costa: il registro torna al salvataggio prima dell'ultimo, mentre i file dei
     * giocatori sono aggiornati.
     */
    static FrameStateSavedData afterUnreadFile(Path file, FrameStateSavedData empty) {
        FrameStateSavedData current = readItself(file);
        if (current != null) {
            RedstoneExoframes.LOGGER.warn("The game could not read the frame registry file {}, but the mod read it "
                    + "itself: using it as it is", file);
            return current;
        }
        RegistryFile.Recovery recovery = RegistryFile.recover(file);
        switch (recovery.kind()) {
            case PREVIOUS -> {
                FrameStateSavedData previous;
                try {
                    previous = load(recovery.root().getCompound("data"));
                } catch (RuntimeException e) {
                    empty.unreadableFile = file;
                    RedstoneExoframes.LOGGER.error("Frame registry file {} and its previous copy {} could not be "
                            + "read: frames are read-only for this session and nothing is written. Restore {} from "
                            + "a backup and restart the server (the unreadable file is kept as {})", file,
                            RegistryFile.oldCopy(file), file, recovery.keptAs(), e);
                    return empty;
                }
                previous.markStructural();
                if (recovery.keptAs() != null) {
                    RedstoneExoframes.LOGGER.error("Frame registry file {} could not be read: using the previous copy "
                            + "{}; the unreadable file is kept as {}. {}", file, RegistryFile.oldCopy(file),
                            recovery.keptAs(), PREVIOUS_COPY_COST);
                } else {
                    RedstoneExoframes.LOGGER.error("Frame registry file {} is missing: using the previous copy {}. {}",
                            file, RegistryFile.oldCopy(file), PREVIOUS_COPY_COST);
                }
                return previous;
            }
            case UNREADABLE -> {
                empty.unreadableFile = file;
                RedstoneExoframes.LOGGER.error("Frame registry file {} could not be read, nor its previous copy {}: "
                        + "frames are read-only for this session and nothing is written. Restore {} from a backup "
                        + "and restart the server (a copy of the unreadable file is kept as {})", file,
                        RegistryFile.oldCopy(file), file, recovery.keptAs() == null ? "none" : recovery.keptAs());
                return empty;
            }
            default -> {
                return empty;
            }
        }
    }

    /**
     * Il dato del file letto dalla mod da se', come lo scrive il gioco (il tag radice con {@code data}); null se il
     * file manca, non si legge, non ha {@code data} o il suo contenuto fa lanciare la lettura.
     */
    private static FrameStateSavedData readItself(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try {
            CompoundTag root = RegistryFile.read(file);
            return root.contains("data", Tag.TAG_COMPOUND) ? load(root.getCompound("data")) : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // Le operazioni del registro, con lo stesso nome e la stessa firma: vedi FrameRegistry.

    Map<UUID, FrameState> frames() {
        return registry.frames();
    }

    FrameRecord worn(UUID player) {
        return registry.worn(player);
    }

    FrameRecord getOrCreateWorn(UUID player, Supplier<FrameState> factory) {
        return registry.getOrCreateWorn(player, factory);
    }

    boolean updateWorn(UUID player, Supplier<FrameState> factory, Consumer<FrameState> change) {
        return registry.updateWorn(player, factory, change);
    }

    boolean changeWorn(UUID player, Consumer<FrameState> change) {
        return registry.changeWorn(player, change);
    }

    boolean mutateWorn(UUID player, Consumer<FrameState> change) {
        return registry.mutateWorn(player, change);
    }

    boolean removeWorn(UUID player) {
        return registry.removeWorn(player);
    }

    UUID place(FrameRecord record, PlacedSpot spot) {
        return registry.place(record, spot);
    }

    FrameRecord placed(UUID frameId) {
        return registry.placed(frameId);
    }

    PlacedSpot spot(UUID frameId) {
        return registry.spot(frameId);
    }

    List<UUID> placedIn(String dimension) {
        return registry.placedIn(dimension);
    }

    boolean removePlaced(UUID frameId) {
        return registry.removePlaced(frameId);
    }

    boolean updatePlaced(UUID frameId, Consumer<FrameState> change) {
        return registry.updatePlaced(frameId, change);
    }

    boolean addStowed(UUID player, List<CompoundTag> entries) {
        return registry.addStowed(player, entries);
    }

    <T> List<T> takeStowedOfWorn(UUID player, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return registry.takeStowedOfWorn(player, reader);
    }

    <T> List<T> takeStowedOfPlaced(UUID frameId, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return registry.takeStowedOfPlaced(frameId, reader);
    }

    List<CompoundTag> orphaned() {
        return registry.orphaned();
    }

    int forgetOrphaned(UUID frameId) {
        return registry.forgetOrphaned(frameId);
    }

    boolean enter(UUID player, UUID frameId) {
        return registry.enter(player, frameId);
    }

    Optional<UUID> exit(UUID player, PlacedSpot spot) {
        return registry.exit(player, spot);
    }

    private static FrameStateSavedData load(CompoundTag tag) {
        return load(tag, FrameStateCodec::read);
    }

    /**
     * Legge il dato con il lettore di stato dato (nel codice e' sempre {@link FrameStateCodec#read}; il parametro
     * esiste per i test), con {@link FrameRegistryFormat#read}. Con uno schema piu' recente o non numerico, o senza
     * schema ne' sezioni, il dato tiene il tag letto per intero ed e' in sola lettura (nel secondo caso l'ERROR con il
     * file lo scrive {@link #get(DimensionDataStorage, Path)}). Un'eccezione che uscisse da qui varrebbe come un file
     * illeggibile (vedi {@link #get(DimensionDataStorage, Path)}): la copia precedente o la sola lettura, mai il
     * registro vuoto.
     */
    static FrameStateSavedData load(CompoundTag tag, BiFunction<CompoundTag, ExoframesConfig, FrameState> reader) {
        FrameStateSavedData data = new FrameStateSavedData();
        switch (FrameRegistryFormat.read(tag, reader, data.registry)) {
            case READ_ONLY -> data.readOnlyRoot = tag.copy();
            case WITHOUT_SECTIONS -> {
                data.readOnlyRoot = tag.copy();
                data.withoutSections = true;
            }
            default -> {
            }
        }
        return data;
    }

    /**
     * Vero se il file e' stato salvato con uno schema piu' recente di {@link #CURRENT_SCHEMA} o con un marcatore non
     * numerico, se non ha ne' schema ne' sezioni, o se ne' il file ne' la sua copia precedente si sono potuti leggere
     * ({@link #get}): questa versione della mod non deve cambiare ne' riscrivere il dato.
     */
    boolean readOnly() {
        return readOnlyRoot != null || unreadableFile != null;
    }

    /**
     * Scrive nel log, una sola volta per questo dato, che una modifica e' stata rifiutata perche' il dato e' in sola
     * lettura.
     */
    void logRefusedChange() {
        if (!readOnly() || refusalLogged) {
            return;
        }
        refusalLogged = true;
        if (unreadableFile != null) {
            RedstoneExoframes.LOGGER.error("Frame registry file {} could not be read: changes are not applied; "
                    + "restore it and restart the server", unreadableFile);
        } else {
            RedstoneExoframes.LOGGER.error("Frame registry was saved by a newer version of the mod with schema {}: "
                    + "changes are not applied; update the mod",
                    SchemaIds.safeId(String.valueOf(readOnlyRoot.get(NbtKeys.SCHEMA))));
        }
    }

    /** In sola lettura il dato non risulta mai da salvare: questa versione non riscrive il file. */
    @Override
    public boolean isDirty() {
        return !readOnly() && super.isDirty();
    }

    /**
     * Segna il dato come da salvare dopo un'operazione strutturale: un telaio piazzato, tolto, indossato o lasciato,
     * armatura stivata o ripresa, pezzi, cella o blocco di un telaio piazzato cambiati, un record indossato creato o
     * tolto. Sono i cambi che spostano qualcosa fra un inventario e il registro: finche' non sono sul disco, l'uscita
     * di un giocatore salva tutto insieme ({@link #saveWithPlayersIfNeeded}).
     */
    private void markStructural() {
        structuralUnsaved = true;
        setDirty();
    }

    /** Vero se un'operazione strutturale ha cambiato il dato dopo l'ultima scrittura riuscita del file. */
    boolean structuralUnsaved() {
        return structuralUnsaved;
    }

    /** Il dato e' sul disco: non piu' da salvare, nessun cambio strutturale in sospeso. */
    void markSaved() {
        setDirty(false);
        structuralUnsaved = false;
    }

    /**
     * All'uscita di un giocatore dal server ({@code PlayerEvent.PLAYER_QUIT}, che i quattro loader mandano in testa a
     * {@code PlayerList.remove}, prima che il gioco salvi il suo file): se un'operazione strutturale non e' ancora sul
     * disco, il gioco salva i file di tutti i giocatori e poi il solo file di questo registro, non gli altri dati
     * salvati dell'overworld. Cosi' all'uscita cio' che si e' spostato fra un inventario e il registro e' sul disco da
     * entrambe le parti. Tutti i giocatori, non solo chi esce, perche' il segno strutturale e' uno solo per il
     * registro e puo' venire dall'operazione di un altro giocatore. Non guarda {@code /save-off}: il gioco scrive
     * comunque il file del giocatore che esce ({@code PlayerList.remove} chiama {@code save(player)} senza guardarlo),
     * e rispettarlo separerebbe la coppia file del giocatore e registro su cui si regge il registro unico (design
     * §4.5).
     */
    static void saveWithPlayersIfNeeded(MinecraftServer server) {
        FrameStateSavedData data = get(server);
        if (!data.structuralUnsaved || data.readOnly()) {
            return;
        }
        server.getPlayerList().saveAll();
        data.saveFile(dataFolder(server).resolve(NAME + ".dat"));
    }

    //? if >=1.21 {
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return write(tag);
    }

    @Override
    public void save(File file, HolderLookup.Provider provider) {
        saveFile(file.toPath());
    }
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag tag) {
        return write(tag);
    }

    @Override
    public void save(File file) {
        saveFile(file.toPath());
    }*/
    //?}

    /**
     * Il salvataggio del gioco ({@code DimensionDataStorage.save}): se il dato e' da salvare lo scrive con
     * {@link RegistryFile#write} (file temporaneo, copia precedente, spostamento atomico) nel formato del gioco, la
     * versione dei dati e il tag sotto {@code data}. Se la scrittura fallisce il dato resta da salvare, e il salvataggio
     * successivo riprova.
     */
    private void saveFile(Path file) {
        if (!isDirty()) {
            return;
        }
        saveTo(file, fileRoot(SharedConstants.getCurrentVersion().getDataVersion().getVersion()),
                NbtIo::writeCompressed);
    }

    /** Il tag radice del file: {@link #write} sotto {@code data} e la versione dei dati, come lo scrive il gioco. */
    CompoundTag fileRoot(int dataVersion) {
        CompoundTag root = new CompoundTag();
        root.put("data", write(new CompoundTag()));
        return NbtUtils.addDataVersion(root, dataVersion);
    }

    /**
     * Scrive {@code root} in {@code file} con {@link RegistryFile#write}; se riesce il dato e' salvato
     * ({@link #markSaved}), se fallisce resta da salvare e un solo ERROR, con il percorso, lo dice fino alla prossima
     * scrittura riuscita. Non lancia.
     *
     * @return true se il file e' stato scritto
     */
    boolean saveTo(Path file, CompoundTag root, RegistryFile.TagWriter writer) {
        try {
            RegistryFile.write(file, root, writer);
        } catch (IOException | RuntimeException e) {
            if (!writeFailureLogged) {
                writeFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Could not save the frame registry to {}: the previous file is kept, "
                        + "and the next save tries again", file, e);
            }
            return false;
        }
        writeFailureLogged = false;
        markSaved();
        return true;
    }

    /**
     * Scrive il marcatore di schema e le sezioni ({@link FrameRegistryFormat#write}). In sola lettura per uno schema
     * piu' recente o non numerico riscrive il tag letto per intero (cosi' anche un tag senza schema ne' sezioni finche'
     * {@link #get(DimensionDataStorage, Path)} non lo ha segnato come file rovinato). In sola lettura perche' ne' il
     * file ne' la copia precedente si sono letti, o il file non e' un registro, non c'e' un tag da riscrivere: non
     * lancia e scrive solo il marcatore {@link #UNREADABLE_SCHEMA}, senza sezioni, cosi' chi rilegge quel tag ha di
     * nuovo un dato in sola lettura e mai un registro vuoto. Il gioco comunque non arriva qui: un dato in sola lettura
     * non risulta mai da salvare ({@link #isDirty()}), e il salvataggio su file lo salta.
     */
    CompoundTag write(CompoundTag tag) {
        if (unreadableFile != null) {
            tag.putString(NbtKeys.SCHEMA, UNREADABLE_SCHEMA);
            return tag;
        }
        if (readOnly()) {
            for (String key : readOnlyRoot.getAllKeys()) {
                tag.put(key, readOnlyRoot.get(key).copy());
            }
            return tag;
        }
        FrameRegistryFormat.write(registry, tag);
        return tag;
    }
}
