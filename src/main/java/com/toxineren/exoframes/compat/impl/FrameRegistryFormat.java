package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * Il formato del registro dei telai nel tag sotto {@code data} del file ({@link FrameStateSavedData}): la lettura in un
 * {@link FrameRegistry} e la scrittura da un registro, con le regole di tolleranza.
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
 * Accanto alle sezioni c'e' il marcatore {@link NbtKeys#SCHEMA} ({@link FrameStateSavedData#CURRENT_SCHEMA}; assente
 * vale 1, se c'e' almeno una delle sezioni {@code frames}, {@code worn}, {@code placed}: un tag senza marcatore e senza
 * sezioni non e' un registro, {@link Outcome#WITHOUT_SECTIONS}). Lo schema 1 aveva solo la sezione {@code frames} (uno
 * stato per giocatore): si legge ancora, e ogni stato diventa un record indossato con un'identita' nuova; le sue voci
 * non lette restano in {@code frames}, identiche, per sempre. Se il marcatore e' piu' recente di quello noto, o non
 * e' un numero, il dato e' in sola lettura per questa versione della mod ({@link FrameStateSavedData#readOnly()}): un
 * solo ERROR al caricamento, nessuna voce viene letta, e i punti di modifica non cambiano nulla; il dato non risulta
 * mai da salvare, e se viene salvato lo stesso riproduce il tag letto per intero. Nulla si legge, si cambia o si
 * perde.
 * <p>
 * Regola per le versioni future: dentro un record che si legge, un pezzo con un livello, uno slot o un modulo che
 * questa versione non conosce si scarta alla lettura (con un avviso) e il salvataggio successivo lo cancella. Percio'
 * una versione della mod che aggiunge un livello, uno slot o un id di modulo deve alzare
 * {@link FrameStateSavedData#CURRENT_SCHEMA}: cosi' una versione piu' vecchia apre il file in sola lettura invece di
 * perdere quei pezzi.
 */
final class FrameRegistryFormat {
    /** Sezione dello schema 1: uno stato per giocatore, chiave = UUID in forma testuale. */
    static final String FRAMES = "frames";
    /** Suffisso della chiave sotto cui si scrive una voce grezza la cui chiave e' presa da un record vivo. */
    static final String UNREAD_SUFFIX = "~unread";

    /** Com'e' andata la lettura di un tag ({@link #read}). */
    enum Outcome {
        /** Le voci leggibili sono nel registro, le altre fra le non lette. */
        READ,
        /** Schema piu' recente o non numerico: nulla e' stato letto, il dato va tenuto in sola lettura. */
        READ_ONLY,
        /**
         * Ne' {@link NbtKeys#SCHEMA} ne' alcuna delle sezioni {@link #FRAMES}, {@link NbtKeys#WORN},
         * {@link NbtKeys#PLACED}: il tag non e' un registro (scritto da altro, o svuotato), nulla e' stato letto e il
         * dato va tenuto in sola lettura come per un file rovinato.
         */
        WITHOUT_SECTIONS
    }

    private FrameRegistryFormat() {
    }

    /**
     * Legge il tag nel registro {@code into}, vuoto, con il lettore di stato dato (nel codice e' sempre
     * {@link FrameStateCodec#read}; il parametro esiste per i test). Ogni voce e' isolata: una voce che non si legge
     * viene saltata con un avviso e conservata grezza. Con uno schema piu' recente o non numerico scrive un ERROR, non
     * legge nulla e dice {@link Outcome#READ_ONLY}: tenere il tag letto tocca a chi chiama. Un tag senza marcatore di
     * schema e senza alcuna delle sezioni (conta la presenza delle chiavi, di qualunque tipo, non il loro essere vuote;
     * {@code orphaned} da sola non basta, perche' {@link #write} scrive sempre lo schema) non e' un registro: nessuna
     * riga di log, nulla letto, e l'esito e' {@link Outcome#WITHOUT_SECTIONS}. Se alcune celle lette tengono piu'
     * della capacita' della config, e per questo hanno tenuto la loro, un solo WARN ne dice il numero (design
     * §12.10). Un'eccezione che uscisse da qui varrebbe come un file illeggibile (vedi
     * {@link FrameStateSavedData#afterUnreadFile}): la copia precedente o la sola lettura, mai il registro vuoto.
     */
    static Outcome read(CompoundTag tag, BiFunction<CompoundTag, ExoframesConfig, FrameState> reader,
            FrameRegistry into) {
        if (!tag.contains(NbtKeys.SCHEMA) && !tag.contains(FRAMES) && !tag.contains(NbtKeys.WORN)
                && !tag.contains(NbtKeys.PLACED)) {
            return Outcome.WITHOUT_SECTIONS;
        }
        CompoundTag wornTag = tag.getCompound(NbtKeys.WORN);
        CompoundTag placedTag = tag.getCompound(NbtKeys.PLACED);
        CompoundTag framesTag = tag.getCompound(FRAMES);
        Tag marker = tag.get(NbtKeys.SCHEMA);
        boolean numeric = tag.contains(NbtKeys.SCHEMA, Tag.TAG_ANY_NUMERIC);
        if (marker != null && (!numeric || tag.getInt(NbtKeys.SCHEMA) > FrameStateSavedData.CURRENT_SCHEMA)) {
            RedstoneExoframes.LOGGER.error("Frame registry saved with schema {} (known: {}): read-only, keeping {} "
                    + "entries unread and unchanged", SchemaIds.safeId(marker.toString()),
                    FrameStateSavedData.CURRENT_SCHEMA, wornTag.size() + placedTag.size() + framesTag.size());
            return Outcome.READ_ONLY;
        }
        ExoframesConfig config = RedstoneExoframes.config();
        Set<UUID> frameIds = new HashSet<>();
        for (String key : new TreeSet<>(wornTag.getAllKeys())) {
            UUID player = entryKey(wornTag, key, into.unreadWorn, "worn frame");
            if (player == null) {
                continue;
            }
            if (into.worn.containsKey(player)) {
                skip(into.unreadWorn, wornTag, key, "Skipping second worn frame of player {}, kept unread");
                continue;
            }
            FrameRecord record;
            try {
                CompoundTag entry = wornTag.getCompound(key);
                UUID frameId = SchemaIds.readUuid(entry, NbtKeys.ID)
                        .orElseThrow(() -> new IllegalArgumentException("missing or invalid frame id"));
                record = readRecord(entry, frameId, reader, config);
            } catch (RuntimeException e) {
                skip(into.unreadWorn, wornTag, key, "Skipping corrupt worn frame of player {}, kept unread: {}",
                        e.toString());
                continue;
            }
            if (!frameIds.add(record.frameId())) {
                skip(into.unreadWorn, wornTag, key, "Skipping worn frame of player {} with a frame id already in "
                        + "the registry, kept unread: {}", record.frameId());
                continue;
            }
            into.worn.put(player, record);
        }
        for (String key : new TreeSet<>(placedTag.getAllKeys())) {
            UUID frameId = entryKey(placedTag, key, into.unreadPlaced, "placed frame");
            if (frameId == null) {
                continue;
            }
            FrameRegistry.Placed entry;
            try {
                CompoundTag entryTag = placedTag.getCompound(key);
                entry = new FrameRegistry.Placed(readRecord(entryTag, frameId, reader, config), readSpot(entryTag));
            } catch (RuntimeException e) {
                skip(into.unreadPlaced, placedTag, key, "Skipping corrupt placed frame {}, kept unread: {}",
                        e.toString());
                continue;
            }
            if (!frameIds.add(frameId)) {
                skip(into.unreadPlaced, placedTag, key, "Skipping placed frame {} with a frame id already in the "
                        + "registry, kept unread");
                continue;
            }
            into.placed.put(frameId, entry);
        }
        Tag orphanedTag = tag.get(NbtKeys.ORPHANED);
        if (orphanedTag instanceof ListTag list && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND)) {
            for (int i = 0; i < list.size(); i++) {
                into.orphaned.add(list.getCompound(i).copy());
            }
        } else if (orphanedTag != null) {
            // nulla si perde: il valore inatteso resta, identico, dentro un gruppo
            RedstoneExoframes.LOGGER.warn("Orphaned stowed armor is not a list of compounds, kept inside one group");
            CompoundTag group = new CompoundTag();
            group.put(NbtKeys.ENTRIES, orphanedTag.copy());
            into.orphaned.add(group);
        }
        for (String key : new TreeSet<>(framesTag.getAllKeys())) {
            UUID player = entryKey(framesTag, key, into.unreadFrames, "schema 1 frame state");
            if (player == null) {
                continue;
            }
            if (into.worn.containsKey(player)) {
                skip(into.unreadFrames, framesTag, key, "Skipping schema 1 frame state of player {} who already "
                        + "wears a frame, kept unread");
                continue;
            }
            try {
                FrameState state = reader.apply(framesTag.getCompound(key), config);
                into.worn.put(player, new FrameRecord(UUID.randomUUID(), state));
                // identita' nuova: va salvata, o cambierebbe a ogni caricamento
                into.setDirty();
            } catch (RuntimeException e) {
                skip(into.unreadFrames, framesTag, key, "Skipping corrupt schema 1 frame state of player {}, kept "
                        + "unread: {}", e.toString());
            }
        }
        int overCapacity = cellsOverCapacity(into, config.energy.cellCapacity);
        if (overCapacity > 0) {
            RedstoneExoframes.LOGGER.warn("{} cell(s) in the frame registry hold more than the configured capacity "
                    + "({} RE): they keep their own capacity until they are swapped or drained", overCapacity,
                    config.energy.cellCapacity);
        }
        return Outcome.READ;
    }

    /**
     * Quante celle dei record letti hanno una capacita' piu' grande di quella della config: sono quelle che al
     * caricamento tenevano piu' di {@code configured} RE e hanno tenuto la loro capacita'
     * ({@link FrameStateCodec#read}, design §12.10).
     */
    private static int cellsOverCapacity(FrameRegistry registry, int configured) {
        int count = 0;
        for (FrameRecord record : registry.worn.values()) {
            count += overCapacity(record.state(), configured);
        }
        for (FrameRegistry.Placed entry : registry.placed.values()) {
            count += overCapacity(entry.record().state(), configured);
        }
        return count;
    }

    private static int overCapacity(FrameState state, int configured) {
        return state.cell() != null && state.cell().capacity() > configured ? 1 : 0;
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
     * Scrive nel tag il marcatore di schema e le sezioni del registro: in ognuna i record vivi e le voci grezze non
     * lette, identiche (una voce grezza la cui chiave nomina lo stesso UUID di un record vivo va sotto una chiave
     * libera, vedi la classe); la sezione {@code frames} dello schema 1 solo se ha ancora voci non lette, e le orfane
     * se ce ne sono. La sola lettura la guarda chi chiama ({@link FrameStateSavedData#write}).
     */
    static void write(FrameRegistry registry, CompoundTag tag) {
        CompoundTag wornTag = new CompoundTag();
        registry.worn.forEach((player, record) -> {
            CompoundTag entry = writeRecord(record);
            entry.put(NbtKeys.ID, NbtUtils.createUUID(record.frameId()));
            wornTag.put(player.toString(), entry);
        });
        putRaw(registry, wornTag, registry.unreadWorn, registry.worn.keySet());
        CompoundTag placedTag = new CompoundTag();
        registry.placed.forEach((frameId, entry) -> {
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
        putRaw(registry, placedTag, registry.unreadPlaced, registry.placed.keySet());
        tag.putInt(NbtKeys.SCHEMA, FrameStateSavedData.CURRENT_SCHEMA);
        tag.put(NbtKeys.WORN, wornTag);
        tag.put(NbtKeys.PLACED, placedTag);
        if (!registry.unreadFrames.isEmpty()) {
            tag.put(FRAMES, rawSection(registry.unreadFrames));
        }
        if (!registry.orphaned.isEmpty()) {
            ListTag orphanedTag = new ListTag();
            registry.orphaned.forEach(group -> orphanedTag.add(group.copy()));
            tag.put(NbtKeys.ORPHANED, orphanedTag);
        }
    }

    /**
     * Aggiunge alla sezione, che ha gia' i record vivi, le voci grezze: identiche sotto la loro chiave, o sotto una
     * chiave libera {@code <chiave>~unread[n]} se la loro chiave nomina un UUID di {@code live} (nulla si copre).
     * L'avviso per una chiave spostata si scrive una volta sola per registro ({@link FrameRegistry#movedWarned}).
     */
    private static void putRaw(FrameRegistry registry, CompoundTag section, Map<String, Tag> unread, Set<UUID> live) {
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
            if (registry.movedWarned.add(key)) {
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
