package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}

/**
 * Registro di tutti i telai del mondo, salvato nel DimensionDataStorage dell'overworld con nome {@link #NAME} (file
 * {@code data/redstone_exoframes_frames.dat}).
 * <p>
 * Ogni telaio e' un {@link FrameRecord} (identita', stato, armatura stivata) e sta in una sola delle due mappe: gli
 * indossati per giocatore ({@link NbtKeys#WORN}) e i piazzati per identita' ({@link NbtKeys#PLACED}), con dimensione,
 * posizione e orientamento ({@link PlacedSpot}). Entrare ({@link #enter}) e uscire ({@link #exit}) spostano lo stesso
 * record fra le due mappe dentro questo stesso file: gli spostamenti dentro il registro sono atomici, e un crash non
 * puo' duplicare ne' perdere un telaio fra le due mappe (design §4.5). Cio' che passa fra l'inventario di un giocatore e
 * il registro (armatura stivata e restituita, pezzi e celle montati e smontati, il telaio come oggetto) sta invece in
 * due file, quello del giocatore e questo: ogni operazione strutturale lo ricorda ({@link #structuralUnsaved()}) e
 * all'uscita di un giocatore, quando il gioco scrive il suo file, si scrivono anche gli altri giocatori e questo
 * ({@link #saveWithPlayersIfNeeded}); i salvataggi del gioco scrivono i giocatori e poi i livelli. Resta un arresto
 * brusco nei millisecondi di un salvataggio. L'armatura stivata qui e' una lista di tag opachi.
 * <p>
 * Si usa un SavedData, e non gli attachment o le capability dei loader, perche' e' un'unica implementazione per i
 * quattro target e lo stato non dipende dall'entita' del giocatore (morte, respawn e cambio di dimensione non lo
 * toccano). Le condizioni di versione sono solo sulle firme di {@code save} e su {@code computeIfAbsent}. Su 1.21.1
 * il {@code DataFixTypes} della factory e' null (nessun datafixer per i dati della mod): Fabric API e NeoForge
 * saltano il datafix in quel caso.
 * <p>
 * Il file si scrive sempre per intero accanto e prende il posto del vecchio in un colpo, lasciando la copia
 * precedente ({@link RegistryFile#write}); una scrittura fallita lascia il dato da salvare. Un file che il gioco non
 * legge non vale mai come registro vuoto: si usa la copia precedente o, se nemmeno quella si legge, il dato resta in
 * sola lettura per la sessione con un ERROR che dice quale file ripristinare ({@link #get(DimensionDataStorage, Path)}).
 * <p>
 * In lettura ogni voce e' isolata: una voce che non si legge (valore non compound, chiave non UUID, campo mancante o
 * del tipo sbagliato, eccezione del lettore dello stato) viene saltata con un avviso che ne nomina la chiave, e le
 * altre restano. Cosi' anche una voce con un'identita' gia' letta in un'altra voce: resta la prima letta, prima gli
 * indossati e poi i piazzati, e dentro una sezione in ordine di chiave (le chiavi si leggono ordinate, non nell'ordine
 * della HashMap del tag). La voce saltata non si perde: resta come tag grezzo nella sua sezione e {@code save} la
 * riscrive identica, per sempre: nessuna operazione della mod la toglie (ne' {@code update()} che crea lo stato del
 * giocatore, ne' {@code clear()} che lo toglie), perche' puo' contenere armatura stivata; se mai servisse, si toglie a
 * mano dal file.
 * <p>
 * Una voce grezza non blocca mai il gioco ne' viene coperta: se un record vivo prende la stessa chiave (un giocatore
 * con una voce indossata non letta entra in un telaio; esce o viene piazzato il telaio di un'identita' che ha anche
 * una voce piazzata non letta), il record vivo si scrive sotto la chiave e la voce grezza accanto, identica, sotto
 * {@code <chiave>~unread} (con un numero in coda se anche quella e' presa), con un avviso una volta per chiave. Al
 * caricamento successivo quella chiave non e' un UUID, quindi la voce resta fra le non lette e si riscrive identica
 * sotto lo stesso nome, una volta sola. Se nessun record vivo prende la chiave (lo stato non e' mai stato creato, o
 * e' stato tolto), la voce grezza resta sotto la sua chiave.
 * <p>
 * Accanto alle sezioni c'e' il marcatore {@link NbtKeys#SCHEMA} ({@link #CURRENT_SCHEMA}; assente vale 1). Lo schema 1
 * aveva solo la sezione {@code frames} (uno stato per giocatore): si legge ancora, e ogni stato diventa un record
 * indossato con un'identita' nuova; le sue voci non lette restano in {@code frames}, identiche, per sempre. Se il
 * marcatore e' piu' recente di quello noto, o non e' un numero, il dato e' in sola lettura per questa versione della
 * mod ({@link #readOnly()}): un solo ERROR al caricamento, nessuna voce viene letta, e i punti di modifica non cambiano
 * nulla; il dato non risulta mai da salvare, e se viene salvato lo stesso riproduce il tag letto per intero. Nulla
 * si legge, si cambia o si perde.
 * <p>
 * Regola per le versioni future: dentro un record che si legge, un pezzo con un livello, uno slot o un modulo che
 * questa versione non conosce si scarta alla lettura (con un avviso) e il salvataggio successivo lo cancella. Percio'
 * una versione della mod che aggiunge un livello, uno slot o un id di modulo deve alzare {@link #CURRENT_SCHEMA}: cosi'
 * una versione piu' vecchia apre il file in sola lettura invece di perdere quei pezzi.
 * <p>
 * Un record tolto dal registro ({@link #removeWorn}, {@link #removePlaced}) con armatura stivata ancora dentro, che si
 * legga o no, non la perde: le voci passano identiche in {@link NbtKeys#ORPHANED}, un gruppo per telaio con
 * l'identita' del telaio e il proprietario se noto, con un avviso che nomina il telaio. La lista si riscrive identica
 * a ogni salvataggio (un valore che non e' una lista di compound si conserva dentro un gruppo, sotto
 * {@link NbtKeys#ENTRIES}); nessuno la legge ancora per il gioco. Chi toglie un telaio riprende prima l'armatura che
 * si legge: la lista e' la rete per quello che resta.
 * <p>
 * Solo sul thread del server; i record sono vivi: ogni operazione del registro qui segna da se' il dato come da
 * salvare, e chi cambia uno stato o un'armatura stivata da fuori deve chiamare {@link #setDirty()}. Un ingresso
 * nullo ({@code place}, {@code exit}, l'armatura stivata) e' rifiutato subito con un'eccezione, cosi' il
 * salvataggio non lancia mai. Le identita' create leggendo un file di schema 1 segnano il dato come da salvare gia'
 * al caricamento, cosi' restano le stesse.
 */
final class FrameStateSavedData extends SavedData {
    static final String NAME = "redstone_exoframes_frames";
    /** Sezione dello schema 1: uno stato per giocatore, chiave = UUID in forma testuale. */
    private static final String FRAMES = "frames";
    /** Versione dello schema che questo codice sa leggere e scrive. */
    static final int CURRENT_SCHEMA = 2;

    /** Telai indossati, per giocatore. */
    private final Map<UUID, FrameRecord> worn = new LinkedHashMap<>();
    /** Suffisso della chiave sotto cui si scrive una voce grezza la cui chiave e' presa da un record vivo. */
    private static final String UNREAD_SUFFIX = "~unread";

    /** Telai piazzati, per identita'. */
    private final Map<UUID, Placed> placed = new LinkedHashMap<>();
    /** Voci non lette di ogni sezione, per chiave originale, riscritte identiche in {@link #write}. */
    private final Map<String, Tag> unreadWorn = new LinkedHashMap<>();
    private final Map<String, Tag> unreadPlaced = new LinkedHashMap<>();
    private final Map<String, Tag> unreadFrames = new LinkedHashMap<>();
    /** Gruppi dell'armatura stivata rimasta in telai tolti, riscritti identici sotto {@link NbtKeys#ORPHANED}. */
    private final List<CompoundTag> orphaned = new ArrayList<>();
    /** Il tag letto per intero quando il dato e' in sola lettura (schema piu' recente o non numerico), altrimenti null. */
    private CompoundTag readOnlyRoot;
    /** Chiavi grezze gia' segnalate come spostate accanto a un record vivo, per avvisare una volta sola. */
    private final Set<String> movedWarned = new HashSet<>();
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

    /** Un record piazzato con il suo posto. */
    private record Placed(FrameRecord record, PlacedSpot spot) {
    }

    /**
     * Il dato si crea o si carica una volta per server: e' il momento in cui i risultati di lettura degli stack
     * ricordati da un server precedente (nello stesso processo, con altri registri) vanno dimenticati. (Anche un dato
     * aperto da un test su una copia del file li dimentica: al piu' un avviso per una voce illeggibile si ripete.)
     */
    private FrameStateSavedData() {
        StackTags.forgetOutcomes();
    }

    /** Il dato del mondo del server, creato vuoto se non esiste ancora: {@link #get(DimensionDataStorage, Path)}. */
    static FrameStateSavedData get(MinecraftServer server) {
        return get(server.overworld().getDataStorage(), dataFolder(server));
    }

    /** La cartella dei dati salvati dell'overworld, dove sta il file {@link #NAME}{@code .dat}. */
    static Path dataFolder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").normalize();
    }

    /**
     * Il dato di {@code storage}, che tiene i suoi file in {@code folder}: lo legge il gioco con la factory della
     * mod. Se il gioco non ha potuto leggere il file (lo dice solo nel log e fa nascere il dato vuoto), la prima
     * richiesta guarda i file ({@link RegistryFile#recover}): con la copia precedente leggibile il dato viene da quella,
     * e' da salvare e prende il posto di quello vuoto in {@code storage}; se nulla si legge il dato resta vuoto e in
     * sola lettura per la sessione ({@link #readOnly()}), cosi' nessuno lo riscrive sopra il file da ripristinare; se
     * nessuno dei due file esiste e' un mondo nuovo. Il server apre cosi' il suo dato; i test ne aprono altri sulla
     * stessa cartella o su una copia.
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
            return data;
        }
        data.createdEmpty = false;
        FrameStateSavedData recovered = afterUnreadFile(folder.resolve(NAME + ".dat"), data);
        if (recovered != data) {
            storage.set(NAME, recovered);
        }
        return recovered;
    }

    /**
     * Il dato da usare quando il gioco non ha letto {@code file} e ha dato {@code empty} (vedi {@link #get}): {@code
     * empty} per un mondo nuovo; il dato della copia precedente, da salvare, se si legge; altrimenti {@code empty} in
     * sola lettura. Ogni caso diverso dal mondo nuovo scrive un ERROR che dice che cosa e' successo e che cosa fare.
     */
    static FrameStateSavedData afterUnreadFile(Path file, FrameStateSavedData empty) {
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
                            + "{}; the unreadable file is kept as {}", file, RegistryFile.oldCopy(file),
                            recovery.keptAs());
                } else {
                    RedstoneExoframes.LOGGER.error("Frame registry file {} is missing: using the previous copy {}",
                            file, RegistryFile.oldCopy(file));
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

    /** Vista in sola lettura giocatore -> stato del telaio indossato (una copia della mappa; gli stati sono vivi). */
    Map<UUID, FrameState> frames() {
        Map<UUID, FrameState> states = new LinkedHashMap<>();
        worn.forEach((player, record) -> states.put(player, record.state()));
        return Collections.unmodifiableMap(states);
    }

    /** Il record indossato dal giocatore, o null. */
    FrameRecord worn(UUID player) {
        return worn.get(player);
    }

    /**
     * Il record indossato dal giocatore, creato con un'identita' nuova e lo stato di {@code factory} se assente (allora
     * il dato e' da salvare, come dopo ogni operazione strutturale, vedi {@link #structuralUnsaved()}); null, senza
     * cambiare nulla, in sola lettura.
     */
    FrameRecord getOrCreateWorn(UUID player, Supplier<FrameState> factory) {
        if (readOnly()) {
            return null;
        }
        FrameRecord record = worn.get(player);
        if (record == null) {
            record = new FrameRecord(UUID.randomUUID(), factory.get());
            worn.put(player, record);
            markStructural();
        }
        return record;
    }

    /**
     * Applica la modifica allo stato del record indossato dal giocatore, creato come in {@link #getOrCreateWorn} se
     * assente, e segna il dato come da salvare con un cambio strutturale (anche se la modifica lancia): e' la scrittura
     * rara, che sincronizza. Falso, senza cambiare nulla, in sola lettura.
     */
    boolean updateWorn(UUID player, Supplier<FrameState> factory, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = getOrCreateWorn(player, factory);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            markStructural();
        }
        return true;
    }

    /**
     * Applica la modifica allo stato del record indossato dal giocatore e segna il dato come da salvare (anche se la
     * modifica lancia), ma non come cambio strutturale: e' la scrittura ad alta frequenza (l'energia a ogni tick, in
     * una parte successiva), e un salvataggio a ogni uscita di un giocatore per questo sarebbe troppo. Falso, senza
     * cambiare nulla, se il giocatore non indossa un telaio o in sola lettura.
     */
    boolean mutateWorn(UUID player, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = readOnly() ? null : worn.get(player);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            setDirty();
        }
        return true;
    }

    /**
     * Rimuove il record indossato dal giocatore; false se non c'era o se il dato e' in sola lettura. L'armatura
     * stivata rimasta va fra le orfane ({@link #orphaned()}), con il proprietario del telaio o, se non ne ha, il
     * giocatore.
     */
    boolean removeWorn(UUID player) {
        if (readOnly()) {
            return false;
        }
        FrameRecord record = worn.remove(player);
        if (record == null) {
            return false;
        }
        UUID owner = record.state().owner();
        orphan(record, owner != null ? owner : player);
        markStructural();
        return true;
    }

    /**
     * Aggiunge il record ai piazzati nel posto dato e ne restituisce l'identita'; null, senza cambiare nulla, se
     * l'identita' e' gia' nel registro (indossata o piazzata) o se il dato e' in sola lettura.
     */
    UUID place(FrameRecord record, PlacedSpot spot) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(spot, "spot must not be null");
        if (readOnly() || contains(record.frameId())) {
            return null;
        }
        placed.put(record.frameId(), new Placed(record, spot));
        markStructural();
        return record.frameId();
    }

    /** Il record piazzato con questa identita', o null. */
    FrameRecord placed(UUID frameId) {
        Placed entry = placed.get(frameId);
        return entry == null ? null : entry.record();
    }

    /** Il posto del telaio piazzato con questa identita', o null. */
    PlacedSpot spot(UUID frameId) {
        Placed entry = placed.get(frameId);
        return entry == null ? null : entry.spot();
    }

    /** Le identita' dei telai piazzati nella dimensione data, nell'ordine in cui sono stati piazzati o letti. */
    List<UUID> placedIn(String dimension) {
        List<UUID> ids = new ArrayList<>();
        placed.forEach((frameId, entry) -> {
            if (entry.spot().dimension().equals(dimension)) {
                ids.add(frameId);
            }
        });
        return ids;
    }

    /**
     * Rimuove il record piazzato; false se non c'era o se il dato e' in sola lettura. L'armatura stivata rimasta va
     * fra le orfane ({@link #orphaned()}), con il proprietario del telaio se ne ha uno.
     */
    boolean removePlaced(UUID frameId) {
        if (readOnly()) {
            return false;
        }
        Placed entry = placed.remove(frameId);
        if (entry == null) {
            return false;
        }
        orphan(entry.record(), entry.record().state().owner());
        markStructural();
        return true;
    }

    /**
     * Se il telaio e' piazzato, applica la modifica al suo stato e segna il dato come da salvare con un cambio
     * strutturale (anche se la modifica lancia: montaggio, smontaggio, blocco); false, senza cambiare nulla, se non
     * e' piazzato o se il dato e' in sola lettura.
     */
    boolean updatePlaced(UUID frameId, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = readOnly() ? null : placed(frameId);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            markStructural();
        }
        return true;
    }

    /**
     * Aggiunge le voci, in ordine, all'armatura stivata del telaio indossato dal giocatore e segna il dato come da
     * salvare; false, senza cambiare nulla, se il dato e' in sola lettura.
     *
     * @throws IllegalStateException se il giocatore non indossa un telaio
     */
    boolean addStowed(UUID player, List<CompoundTag> entries) {
        List<CompoundTag> copy = List.copyOf(entries);
        if (readOnly()) {
            return false;
        }
        FrameRecord record = worn.get(player);
        if (record == null) {
            throw new IllegalStateException("player " + player + " wears no frame to stow armor in");
        }
        record.stowed().addAll(copy);
        markStructural();
        return true;
    }

    /**
     * Toglie dall'armatura stivata del telaio indossato dal giocatore le voci che {@code reader} legge (riceve
     * identita' del telaio e voce) e le restituisce, nell'ordine; quelle che non legge restano identiche. Vuota se il
     * giocatore non indossa un telaio o se il dato e' in sola lettura; segna il dato come da salvare se il record c'e'.
     */
    <T> List<T> takeStowedOfWorn(UUID player, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return take(readOnly() ? null : worn.get(player), reader);
    }

    /** Come {@link #takeStowedOfWorn}, per il telaio piazzato con questa identita'. */
    <T> List<T> takeStowedOfPlaced(UUID frameId, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return take(readOnly() ? null : placed(frameId), reader);
    }

    private <T> List<T> take(FrameRecord record, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        if (record == null) {
            return List.of();
        }
        List<T> taken = new ArrayList<>();
        try {
            for (Iterator<CompoundTag> it = record.stowed().iterator(); it.hasNext(); ) {
                Optional<T> read = reader.apply(record.frameId(), it.next());
                if (read.isPresent()) {
                    taken.add(read.get());
                    it.remove();
                }
            }
        } finally {
            markStructural();
        }
        return taken;
    }

    /** Copie dei gruppi dell'armatura stivata rimasta in telai tolti, nell'ordine in cui sono stati tolti o letti. */
    List<CompoundTag> orphaned() {
        List<CompoundTag> copies = new ArrayList<>();
        orphaned.forEach(group -> copies.add(group.copy()));
        return copies;
    }

    /**
     * Toglie i gruppi orfani del telaio con questa identita' e dice quanti erano. Solo per i test (vedi
     * {@code GameTests.forgetOrphaned}): nel gioco le voci orfane non si tolgono mai.
     */
    int forgetOrphaned(UUID frameId) {
        int before = orphaned.size();
        orphaned.removeIf(group -> SchemaIds.readUuid(group, NbtKeys.FRAME).filter(frameId::equals).isPresent());
        if (orphaned.size() != before) {
            setDirty();
        }
        return before - orphaned.size();
    }

    /** Mette fra le orfane, identiche, le voci stivate rimaste nel record tolto, con un avviso; nulla se non ce ne sono. */
    private void orphan(FrameRecord record, UUID owner) {
        if (record.stowed().isEmpty()) {
            return;
        }
        ListTag entries = new ListTag();
        record.stowed().forEach(entry -> entries.add(entry.copy()));
        CompoundTag group = new CompoundTag();
        group.put(NbtKeys.FRAME, NbtUtils.createUUID(record.frameId()));
        if (owner != null) {
            group.put(NbtKeys.OWNER, NbtUtils.createUUID(owner));
        }
        group.put(NbtKeys.ENTRIES, entries);
        orphaned.add(group);
        RedstoneExoframes.LOGGER.warn("Frame {} (owner {}) left the registry with {} stowed armor entries still inside: "
                + "kept in the saved data under '{}'", record.frameId(), owner == null ? "none" : owner, entries.size(),
                NbtKeys.ORPHANED);
    }

    /**
     * Sposta il record piazzato fra gli indossati del giocatore (lo stesso oggetto). False, senza cambiare nulla, se
     * il record non e' piazzato, se il giocatore ne indossa gia' uno o se il dato e' in sola lettura. Una voce
     * indossata non letta del giocatore non lo impedisce: resta nel file accanto al record (vedi la classe).
     */
    boolean enter(UUID player, UUID frameId) {
        if (readOnly() || worn.containsKey(player) || !placed.containsKey(frameId)) {
            return false;
        }
        worn.put(player, placed.remove(frameId).record());
        markStructural();
        return true;
    }

    /**
     * Sposta il record indossato dal giocatore fra i piazzati nel posto dato (lo stesso oggetto) e ne restituisce
     * l'identita'; vuoto, senza cambiare nulla, se il giocatore non ne indossa uno o se il dato e' in sola lettura.
     */
    Optional<UUID> exit(UUID player, PlacedSpot spot) {
        Objects.requireNonNull(spot, "spot must not be null");
        if (readOnly() || !worn.containsKey(player)) {
            return Optional.empty();
        }
        FrameRecord record = worn.remove(player);
        placed.put(record.frameId(), new Placed(record, spot));
        markStructural();
        return Optional.of(record.frameId());
    }

    private boolean contains(UUID frameId) {
        if (placed.containsKey(frameId)) {
            return true;
        }
        for (FrameRecord record : worn.values()) {
            if (record.frameId().equals(frameId)) {
                return true;
            }
        }
        return false;
    }

    private static FrameStateSavedData load(CompoundTag tag) {
        return load(tag, FrameStateCodec::read);
    }

    /**
     * Legge il dato con il lettore di stato dato (nel codice e' sempre {@link FrameStateCodec#read}; il parametro
     * esiste per i test). Ogni voce e' isolata: una voce che non si legge viene saltata con un avviso e conservata
     * grezza. Un'eccezione che uscisse da qui varrebbe come un file illeggibile (vedi {@link #get(DimensionDataStorage,
     * Path)}): la copia precedente o la sola lettura, mai il registro vuoto.
     */
    static FrameStateSavedData load(CompoundTag tag, BiFunction<CompoundTag, ExoframesConfig, FrameState> reader) {
        FrameStateSavedData data = new FrameStateSavedData();
        CompoundTag wornTag = tag.getCompound(NbtKeys.WORN);
        CompoundTag placedTag = tag.getCompound(NbtKeys.PLACED);
        CompoundTag framesTag = tag.getCompound(FRAMES);
        Tag marker = tag.get(NbtKeys.SCHEMA);
        boolean numeric = tag.contains(NbtKeys.SCHEMA, Tag.TAG_ANY_NUMERIC);
        if (marker != null && (!numeric || tag.getInt(NbtKeys.SCHEMA) > CURRENT_SCHEMA)) {
            RedstoneExoframes.LOGGER.error("Frame registry saved with schema {} (known: {}): read-only, keeping {} "
                    + "entries unread and unchanged", SchemaIds.safeId(marker.toString()), CURRENT_SCHEMA,
                    wornTag.size() + placedTag.size() + framesTag.size());
            data.readOnlyRoot = tag.copy();
            return data;
        }
        ExoframesConfig config = RedstoneExoframes.config();
        Set<UUID> frameIds = new HashSet<>();
        for (String key : new TreeSet<>(wornTag.getAllKeys())) {
            UUID player = entryKey(wornTag, key, data.unreadWorn, "worn frame");
            if (player == null) {
                continue;
            }
            if (data.worn.containsKey(player)) {
                skip(data.unreadWorn, wornTag, key, "Skipping second worn frame of player {}, kept unread");
                continue;
            }
            FrameRecord record;
            try {
                CompoundTag entry = wornTag.getCompound(key);
                UUID frameId = SchemaIds.readUuid(entry, NbtKeys.ID)
                        .orElseThrow(() -> new IllegalArgumentException("missing or invalid frame id"));
                record = readRecord(entry, frameId, reader, config);
            } catch (RuntimeException e) {
                skip(data.unreadWorn, wornTag, key, "Skipping corrupt worn frame of player {}, kept unread: {}",
                        e.toString());
                continue;
            }
            if (!frameIds.add(record.frameId())) {
                skip(data.unreadWorn, wornTag, key, "Skipping worn frame of player {} with a frame id already in "
                        + "the registry, kept unread: {}", record.frameId());
                continue;
            }
            data.worn.put(player, record);
        }
        for (String key : new TreeSet<>(placedTag.getAllKeys())) {
            UUID frameId = entryKey(placedTag, key, data.unreadPlaced, "placed frame");
            if (frameId == null) {
                continue;
            }
            Placed entry;
            try {
                CompoundTag entryTag = placedTag.getCompound(key);
                entry = new Placed(readRecord(entryTag, frameId, reader, config), readSpot(entryTag));
            } catch (RuntimeException e) {
                skip(data.unreadPlaced, placedTag, key, "Skipping corrupt placed frame {}, kept unread: {}",
                        e.toString());
                continue;
            }
            if (!frameIds.add(frameId)) {
                skip(data.unreadPlaced, placedTag, key, "Skipping placed frame {} with a frame id already in the "
                        + "registry, kept unread");
                continue;
            }
            data.placed.put(frameId, entry);
        }
        Tag orphanedTag = tag.get(NbtKeys.ORPHANED);
        if (orphanedTag instanceof ListTag list && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND)) {
            for (int i = 0; i < list.size(); i++) {
                data.orphaned.add(list.getCompound(i).copy());
            }
        } else if (orphanedTag != null) {
            // nulla si perde: il valore inatteso resta, identico, dentro un gruppo
            RedstoneExoframes.LOGGER.warn("Orphaned stowed armor is not a list of compounds, kept inside one group");
            CompoundTag group = new CompoundTag();
            group.put(NbtKeys.ENTRIES, orphanedTag.copy());
            data.orphaned.add(group);
        }
        for (String key : new TreeSet<>(framesTag.getAllKeys())) {
            UUID player = entryKey(framesTag, key, data.unreadFrames, "schema 1 frame state");
            if (player == null) {
                continue;
            }
            if (data.worn.containsKey(player)) {
                skip(data.unreadFrames, framesTag, key, "Skipping schema 1 frame state of player {} who already "
                        + "wears a frame, kept unread");
                continue;
            }
            try {
                FrameState state = reader.apply(framesTag.getCompound(key), config);
                data.worn.put(player, new FrameRecord(UUID.randomUUID(), state));
                // identita' nuova: va salvata, o cambierebbe a ogni caricamento
                data.setDirty();
            } catch (RuntimeException e) {
                skip(data.unreadFrames, framesTag, key, "Skipping corrupt schema 1 frame state of player {}, kept "
                        + "unread: {}", e.toString());
            }
        }
        return data;
    }

    /**
     * La chiave della voce come UUID; null, con la voce messa fra le non lette e un avviso, se il valore non e' un
     * compound o la chiave non e' un UUID.
     */
    private static UUID entryKey(CompoundTag section, String key, Map<String, Tag> unread, String kind) {
        if (!section.contains(key, Tag.TAG_COMPOUND)) {
            skip(unread, section, key, "Skipping " + kind + " that is not a compound, kept unread: {}");
            return null;
        }
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            skip(unread, section, key, "Skipping " + kind + " with an invalid id, kept unread: {}");
            return null;
        }
    }

    /**
     * Mette la voce fra le non lette e scrive un avviso: il primo segnaposto del formato e' la chiave resa sicura,
     * i seguenti sono {@code details}.
     */
    private static void skip(Map<String, Tag> unread, CompoundTag section, String key, String format,
            Object... details) {
        Object[] args = new Object[details.length + 1];
        args[0] = SchemaIds.safeId(key);
        System.arraycopy(details, 0, args, 1, details.length);
        RedstoneExoframes.LOGGER.warn(format, args);
        unread.put(key, section.get(key).copy());
    }

    /** Stato e armatura stivata di un record; lancia se lo stato manca o se l'armatura stivata non si puo' leggere. */
    private static FrameRecord readRecord(CompoundTag entry, UUID frameId,
            BiFunction<CompoundTag, ExoframesConfig, FrameState> reader, ExoframesConfig config) {
        if (!entry.contains(NbtKeys.STATE, Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("missing frame state");
        }
        FrameState state = reader.apply(entry.getCompound(NbtKeys.STATE), config);
        List<CompoundTag> stowed = new ArrayList<>();
        Tag stowedTag = entry.get(NbtKeys.STOWED);
        if (stowedTag != null) {
            // un'armatura stivata illeggibile non si scarta: il record intero resta fra le non lette
            if (!(stowedTag instanceof ListTag list)
                    || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("stowed armor is not a list of compounds");
            }
            for (int i = 0; i < list.size(); i++) {
                stowed.add(list.getCompound(i).copy());
            }
        }
        return new FrameRecord(frameId, state, stowed);
    }

    /**
     * Il posto di un record piazzato; lancia se un campo manca o e' del tipo sbagliato, o se la dimensione non e' un
     * id valido (chi legge il registro ne fa una {@code ResourceKey}).
     */
    private static PlacedSpot readSpot(CompoundTag entry) {
        if (!entry.contains(NbtKeys.DIM, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("missing dimension");
        }
        if (ResourceLocation.tryParse(entry.getString(NbtKeys.DIM)) == null) {
            throw new IllegalArgumentException("invalid dimension id");
        }
        ListTag pos = entry.getList(NbtKeys.POS, Tag.TAG_DOUBLE);
        if (pos.size() != 3) {
            throw new IllegalArgumentException("position is not three doubles");
        }
        if (!entry.contains(NbtKeys.YAW, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalArgumentException("missing yaw");
        }
        return new PlacedSpot(entry.getString(NbtKeys.DIM), pos.getDouble(0), pos.getDouble(1), pos.getDouble(2),
                entry.getFloat(NbtKeys.YAW));
    }

    /**
     * Vero se il file e' stato salvato con uno schema piu' recente di {@link #CURRENT_SCHEMA} o con un marcatore non
     * numerico, o se ne' il file ne' la sua copia precedente si sono potuti leggere ({@link #get}): questa versione
     * della mod non deve cambiare ne' riscrivere il dato.
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
     * disco, il gioco salva i file di tutti i giocatori e poi i dati dell'overworld, registro compreso. Cosi' ogni volta
     * che il file di un giocatore si scrive, il registro si scrive con lui, e cio' che si e' spostato fra un inventario
     * e il registro e' sul disco da entrambe le parti.
     */
    static void saveWithPlayersIfNeeded(MinecraftServer server) {
        FrameStateSavedData data = get(server);
        if (!data.structuralUnsaved || data.readOnly()) {
            return;
        }
        server.getPlayerList().saveAll();
        server.overworld().getDataStorage().save();
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
     * Scrive il marcatore di schema e le sezioni: in ognuna i record vivi e le voci grezze non lette, identiche (una
     * voce grezza la cui chiave nomina lo stesso UUID di un record vivo va sotto una chiave libera, vedi la classe); la
     * sezione {@code frames} dello schema 1 solo se ha ancora voci non lette, e le orfane se ce ne sono. In sola
     * lettura riscrive il tag letto per intero.
     */
    CompoundTag write(CompoundTag tag) {
        if (readOnly()) {
            for (String key : readOnlyRoot.getAllKeys()) {
                tag.put(key, readOnlyRoot.get(key).copy());
            }
            return tag;
        }
        CompoundTag wornTag = new CompoundTag();
        worn.forEach((player, record) -> {
            CompoundTag entry = writeRecord(record);
            entry.put(NbtKeys.ID, NbtUtils.createUUID(record.frameId()));
            wornTag.put(player.toString(), entry);
        });
        putRaw(wornTag, unreadWorn, worn.keySet());
        CompoundTag placedTag = new CompoundTag();
        placed.forEach((frameId, entry) -> {
            CompoundTag entryTag = writeRecord(entry.record());
            PlacedSpot spot = entry.spot();
            entryTag.putString(NbtKeys.DIM, spot.dimension());
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(spot.x()));
            pos.add(DoubleTag.valueOf(spot.y()));
            pos.add(DoubleTag.valueOf(spot.z()));
            entryTag.put(NbtKeys.POS, pos);
            entryTag.putFloat(NbtKeys.YAW, spot.yaw());
            placedTag.put(frameId.toString(), entryTag);
        });
        putRaw(placedTag, unreadPlaced, placed.keySet());
        tag.putInt(NbtKeys.SCHEMA, CURRENT_SCHEMA);
        tag.put(NbtKeys.WORN, wornTag);
        tag.put(NbtKeys.PLACED, placedTag);
        if (!unreadFrames.isEmpty()) {
            tag.put(FRAMES, rawSection(unreadFrames));
        }
        if (!orphaned.isEmpty()) {
            ListTag orphanedTag = new ListTag();
            orphaned.forEach(group -> orphanedTag.add(group.copy()));
            tag.put(NbtKeys.ORPHANED, orphanedTag);
        }
        return tag;
    }

    /**
     * Aggiunge alla sezione, che ha gia' i record vivi, le voci grezze: identiche sotto la loro chiave, o sotto una
     * chiave libera {@code <chiave>~unread[n]} se la loro chiave nomina un UUID di {@code live} (nulla si copre).
     */
    private void putRaw(CompoundTag section, Map<String, Tag> unread, Set<UUID> live) {
        List<String> moved = new ArrayList<>();
        unread.forEach((key, raw) -> {
            if (namesLive(key, live)) {
                moved.add(key);
            } else {
                section.put(key, raw.copy());
            }
        });
        for (String key : moved) {
            String target = key + UNREAD_SUFFIX;
            for (int n = 2; section.contains(target) || unread.containsKey(target); n++) {
                target = key + UNREAD_SUFFIX + n;
            }
            section.put(target, unread.get(key).copy());
            if (movedWarned.add(key)) {
                RedstoneExoframes.LOGGER.warn("Unread frame entry {} shares its key with a live frame, kept unread "
                        + "as {}", SchemaIds.safeId(key), SchemaIds.safeId(target));
            }
        }
    }

    private static boolean namesLive(String key, Set<UUID> live) {
        try {
            return live.contains(UUID.fromString(key));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static CompoundTag rawSection(Map<String, Tag> unread) {
        CompoundTag section = new CompoundTag();
        unread.forEach((key, raw) -> section.put(key, raw.copy()));
        return section;
    }

    private static CompoundTag writeRecord(FrameRecord record) {
        CompoundTag entry = new CompoundTag();
        entry.put(NbtKeys.STATE, FrameStateCodec.write(record.state()));
        ListTag stowed = new ListTag();
        for (CompoundTag item : record.stowed()) {
            stowed.add(item.copy());
        }
        entry.put(NbtKeys.STOWED, stowed);
        return entry;
    }
}
