package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
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
 * record fra le due mappe dentro questo stesso file, quindi un crash fra due salvataggi non puo' duplicare ne' perdere
 * un telaio (design §4.5). L'armatura stivata qui e' una lista di tag opachi.
 * <p>
 * Si usa un SavedData, e non gli attachment o le capability dei loader, perche' e' un'unica implementazione per i
 * quattro target e lo stato non dipende dall'entita' del giocatore (morte, respawn e cambio di dimensione non lo
 * toccano). Le condizioni di versione sono solo sulla firma di {@code save} e su {@code computeIfAbsent}. Su 1.21.1
 * il {@code DataFixTypes} della factory e' null (nessun datafixer per i dati della mod): Fabric API e NeoForge
 * saltano il datafix in quel caso.
 * <p>
 * In lettura ogni voce e' isolata: una voce che non si legge (valore non compound, chiave non UUID, campo mancante o
 * del tipo sbagliato, eccezione del lettore dello stato) viene saltata con un avviso che ne nomina la chiave, e le
 * altre restano. Cosi' anche una voce con un'identita' gia' letta in un'altra voce: resta la prima letta, prima gli
 * indossati e poi i piazzati, e dentro una sezione in ordine di chiave (le chiavi si leggono ordinate, non nell'ordine
 * della HashMap del tag). La voce saltata non si perde: resta come tag grezzo nella sua sezione e {@code save} la
 * riscrive identica; quelle indossate finche' lo stato di quel giocatore non viene creato o sostituito con
 * {@code update()} o rimosso con {@code clear()} ({@link #dropUnread}).
 * <p>
 * Una voce grezza non blocca mai il gioco ne' viene coperta: se un record vivo prende la stessa chiave (un giocatore
 * con una voce indossata non letta entra in un telaio; esce o viene piazzato il telaio di un'identita' che ha anche
 * una voce piazzata non letta), il record vivo si scrive sotto la chiave e la voce grezza accanto, identica, sotto
 * {@code <chiave>~unread} (con un numero in coda se anche quella e' presa), con un avviso una volta per chiave. Al
 * caricamento successivo quella chiave non e' un UUID, quindi la voce resta fra le non lette e si riscrive identica
 * sotto lo stesso nome; {@link #dropUnread} non la tocca piu'.
 * <p>
 * Accanto alle sezioni c'e' il marcatore {@link NbtKeys#SCHEMA} ({@link #CURRENT_SCHEMA}; assente vale 1). Lo schema 1
 * aveva solo la sezione {@code frames} (uno stato per giocatore): si legge ancora, e ogni stato diventa un record
 * indossato con un'identita' nuova; le sue voci non lette restano in {@code frames}, identiche, finche' non vengono
 * tolte. Se il marcatore e' piu' recente di quello noto, o non e' un numero, il dato e' in sola lettura per questa
 * versione della mod ({@link #readOnly()}): un solo ERROR al caricamento, nessuna voce viene letta, e i punti di
 * modifica non cambiano nulla; il dato non risulta mai da salvare, e se viene salvato lo stesso riproduce il tag letto
 * per intero. Nulla si legge, si cambia o si perde.
 * <p>
 * Solo sul thread del server; i record sono vivi: chi cambia uno stato o un'armatura stivata deve chiamare
 * {@link #setDirty()}, come dopo ogni operazione del registro. Un ingresso nullo ({@code place}, {@code exit},
 * l'armatura stivata) e' rifiutato subito con un'eccezione, cosi' il salvataggio non lancia mai. Le identita' create
 * leggendo un file di schema 1 segnano il dato come da salvare gia' al caricamento, cosi' restano le stesse.
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
    /** Il tag letto per intero quando il dato e' in sola lettura (schema piu' recente o non numerico), altrimenti null. */
    private CompoundTag readOnlyRoot;
    /** Chiavi grezze gia' segnalate come spostate accanto a un record vivo, per avvisare una volta sola. */
    private final Set<String> movedWarned = new HashSet<>();
    /** Vero dopo il primo ERROR per una modifica rifiutata, per scriverlo una volta sola. */
    private boolean refusalLogged;

    /** Un record piazzato con il suo posto. */
    private record Placed(FrameRecord record, PlacedSpot spot) {
    }

    private FrameStateSavedData() {
    }

    /** Il dato del mondo del server, creato vuoto se non esiste ancora. */
    static FrameStateSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        //? if >=1.21 {
        return storage.computeIfAbsent(
                new SavedData.Factory<>(FrameStateSavedData::new, (tag, provider) -> load(tag), null), NAME);
        //?} else {
        /*return storage.computeIfAbsent(FrameStateSavedData::load, FrameStateSavedData::new, NAME);*/
        //?}
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
     * Il record indossato dal giocatore, creato con un'identita' nuova e lo stato di {@code factory} se assente; null,
     * senza cambiare nulla, in sola lettura.
     */
    FrameRecord getOrCreateWorn(UUID player, Supplier<FrameState> factory) {
        if (readOnly()) {
            return null;
        }
        return worn.computeIfAbsent(player, k -> new FrameRecord(UUID.randomUUID(), factory.get()));
    }

    /** Rimuove il record indossato dal giocatore; false se non c'era o se il dato e' in sola lettura. */
    boolean removeWorn(UUID player) {
        return !readOnly() && worn.remove(player) != null;
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

    /** Rimuove il record piazzato; false se non c'era o se il dato e' in sola lettura. */
    boolean removePlaced(UUID frameId) {
        return !readOnly() && placed.remove(frameId) != null;
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
     * grezza. Un'eccezione che uscisse da qui farebbe ricreare il dato vuoto e il salvataggio successivo
     * cancellerebbe tutti i telai.
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
     * numerico: questa versione della mod non deve cambiare ne' riscrivere il dato.
     */
    boolean readOnly() {
        return readOnlyRoot != null;
    }

    /**
     * Scrive nel log, una sola volta per questo dato, che una modifica e' stata rifiutata perche' il dato e' in sola
     * lettura.
     */
    void logRefusedChange() {
        if (readOnly() && !refusalLogged) {
            refusalLogged = true;
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
     * Dimentica le voci indossate grezze non lette del giocatore, di schema 2 e di schema 1 (chiamato quando il suo
     * stato viene creato o sostituito con {@code update()} o rimosso con {@code clear()}): la chiave e' confrontata
     * come UUID, cosi' vale anche per una chiave scritta in forma non canonica.
     */
    void dropUnread(UUID player) {
        if (readOnly()) {
            return;
        }
        unreadWorn.keySet().removeIf(key -> isPlayerKey(key, player));
        unreadFrames.keySet().removeIf(key -> isPlayerKey(key, player));
    }

    private static boolean isPlayerKey(String key, UUID player) {
        try {
            return UUID.fromString(key).equals(player);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    //? if >=1.21 {
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return write(tag);
    }
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag tag) {
        return write(tag);
    }*/
    //?}

    /**
     * Scrive il marcatore di schema e le sezioni: in ognuna i record vivi e le voci grezze non lette, identiche (una
     * voce grezza la cui chiave nomina lo stesso UUID di un record vivo va sotto una chiave libera, vedi la classe); la
     * sezione {@code frames} dello schema 1 solo se ha ancora voci non lette. In sola lettura riscrive il tag letto
     * per intero.
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
