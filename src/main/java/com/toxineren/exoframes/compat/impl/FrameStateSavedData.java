package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}

/**
 * Stati del telaio di tutti i giocatori del mondo, salvati nel DimensionDataStorage dell'overworld con nome
 * {@link #NAME} (file {@code data/redstone_exoframes_frames.dat}).
 * <p>
 * Si usa un SavedData, e non gli attachment o le capability dei loader, perche' e' un'unica implementazione per i
 * quattro target e lo stato non dipende dall'entita' del giocatore (morte, respawn e cambio di dimensione non lo
 * toccano). Le condizioni di versione sono solo sulla firma di {@code save} e su {@code computeIfAbsent}. Su 1.21.1
 * il {@code DataFixTypes} della factory e' null (nessun datafixer per i dati della mod): Fabric API e NeoForge
 * saltano il datafix in quel caso.
 * <p>
 * In lettura ogni voce e' isolata: una voce che non si legge (valore non compound, chiave non UUID, eccezione del
 * lettore) viene saltata con un avviso che ne nomina la chiave, e le altre restano. La voce saltata non si perde:
 * resta come tag grezzo e {@code save} la riscrive identica, finche' lo stato di quel giocatore non viene creato o
 * sostituito con {@code update()} o rimosso con {@code clear()} ({@link #dropUnread}).
 * <p>
 * Accanto a {@code frames} c'e' il marcatore {@link NbtKeys#SCHEMA} ({@link #CURRENT_SCHEMA}; assente vale 1). Se il
 * marcatore e' piu' recente di quello noto, o non e' un numero, il dato e' in sola lettura per questa versione
 * della mod ({@link #readOnly()}): un solo ERROR al caricamento, nessuna voce viene letta, e i punti di modifica
 * ({@link #getOrCreate}, {@link #remove}, {@link #dropUnread}) non cambiano nulla; il dato non risulta mai da salvare,
 * e se viene salvato lo stesso riproduce il tag letto (stesso marcatore, voci identiche). Nulla si legge, si cambia o
 * si perde.
 * <p>
 * Solo sul thread del server; la mappa di {@link #frames()} e' in sola lettura: si crea e si rimuove con
 * {@link #getOrCreate} e {@link #remove}, e chi modifica uno stato deve chiamare {@link #setDirty()}.
 */
final class FrameStateSavedData extends SavedData {
    static final String NAME = "redstone_exoframes_frames";
    /** Tag con una voce per giocatore, chiave = UUID in forma testuale. */
    private static final String FRAMES = "frames";
    /** Versione dello schema delle voci che questo codice sa leggere e scrive. */
    static final int CURRENT_SCHEMA = 1;

    private final Map<UUID, FrameState> frames = new HashMap<>();
    /** Voci non lette, per chiave originale, riscritte identiche in {@link #write}. */
    private final Map<String, Tag> unread = new LinkedHashMap<>();
    /** Il marcatore letto dal file quando il dato e' in sola lettura (piu' recente o non numerico), altrimenti null. */
    private Tag readOnlyMarker;
    /** Vero dopo il primo ERROR per una modifica rifiutata, per scriverlo una volta sola. */
    private boolean refusalLogged;

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

    /** Vista in sola lettura UUID -> stato (gli stati sono vivi: dopo una loro modifica va chiamato setDirty). */
    Map<UUID, FrameState> frames() {
        return Collections.unmodifiableMap(frames);
    }

    private static FrameStateSavedData load(CompoundTag tag) {
        return load(tag, FrameStateCodec::read);
    }

    /**
     * Legge il dato con il lettore di voce dato (nel codice e' sempre {@link FrameStateCodec#read}; il parametro
     * esiste per i test). Ogni voce e' isolata: una voce che non si legge viene saltata con un avviso e conservata
     * grezza. Un'eccezione che uscisse da qui farebbe ricreare il dato vuoto e il salvataggio successivo
     * cancellerebbe gli stati di tutti i giocatori.
     */
    static FrameStateSavedData load(CompoundTag tag, BiFunction<CompoundTag, ExoframesConfig, FrameState> reader) {
        FrameStateSavedData data = new FrameStateSavedData();
        CompoundTag framesTag = tag.getCompound(FRAMES);
        Tag marker = tag.get(NbtKeys.SCHEMA);
        boolean numeric = tag.contains(NbtKeys.SCHEMA, Tag.TAG_ANY_NUMERIC);
        if (marker != null && (!numeric || tag.getInt(NbtKeys.SCHEMA) > CURRENT_SCHEMA)) {
            RedstoneExoframes.LOGGER.error("Frame states saved with schema {} (known: {}): read-only, keeping {} "
                    + "entries unread and unchanged", SchemaIds.safeId(marker.toString()), CURRENT_SCHEMA,
                    framesTag.size());
            data.readOnlyMarker = marker.copy();
            for (String key : framesTag.getAllKeys()) {
                data.unread.put(key, framesTag.get(key).copy());
            }
            return data;
        }
        for (String key : framesTag.getAllKeys()) {
            if (!framesTag.contains(key, Tag.TAG_COMPOUND)) {
                RedstoneExoframes.LOGGER.warn("Skipping frame state that is not a compound, kept unread: {}",
                        SchemaIds.safeId(key));
                data.unread.put(key, framesTag.get(key).copy());
                continue;
            }
            UUID player;
            try {
                player = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                RedstoneExoframes.LOGGER.warn("Skipping frame state with invalid player id, kept unread: {}",
                        SchemaIds.safeId(key));
                data.unread.put(key, framesTag.get(key).copy());
                continue;
            }
            try {
                data.frames.put(player, reader.apply(framesTag.getCompound(key), RedstoneExoframes.config()));
            } catch (RuntimeException e) {
                RedstoneExoframes.LOGGER.warn("Skipping corrupt frame state of player {}, kept unread: {}",
                        SchemaIds.safeId(key), e.toString());
                data.unread.put(key, framesTag.get(key).copy());
            }
        }
        return data;
    }

    /**
     * Vero se il file e' stato salvato con uno schema piu' recente di {@link #CURRENT_SCHEMA} o con un marcatore non
     * numerico: questa versione della mod non deve cambiare ne' riscrivere il dato.
     */
    boolean readOnly() {
        return readOnlyMarker != null;
    }

    /**
     * Scrive nel log, una sola volta per questo dato, che una modifica e' stata rifiutata perche' il dato e' in sola
     * lettura.
     */
    void logRefusedChange() {
        if (readOnly() && !refusalLogged) {
            refusalLogged = true;
            RedstoneExoframes.LOGGER.error("Frame states were saved by a newer version of the mod with schema {}: "
                    + "changes are not applied; update the mod", SchemaIds.safeId(readOnlyMarker.toString()));
        }
    }

    /** Lo stato del giocatore, creato con {@code factory} se assente; null, senza cambiare nulla, in sola lettura. */
    FrameState getOrCreate(UUID player, Supplier<FrameState> factory) {
        if (readOnly()) {
            return null;
        }
        return frames.computeIfAbsent(player, k -> factory.get());
    }

    /** Rimuove lo stato del giocatore; false se non c'era o se il dato e' in sola lettura. */
    boolean remove(UUID player) {
        return !readOnly() && frames.remove(player) != null;
    }

    /** In sola lettura il dato non risulta mai da salvare: questa versione non riscrive il file. */
    @Override
    public boolean isDirty() {
        return !readOnly() && super.isDirty();
    }

    /**
     * Dimentica le voci grezze non lette del giocatore (chiamato quando il suo stato viene creato o sostituito con
     * {@code update()} o rimosso con {@code clear()}): la chiave e' confrontata come UUID, cosi' vale anche per una
     * chiave scritta in forma non canonica.
     */
    void dropUnread(UUID player) {
        if (readOnly() || unread.isEmpty()) {
            return;
        }
        unread.keySet().removeIf(key -> {
            try {
                return UUID.fromString(key).equals(player);
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
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
     * Scrive il marcatore di schema (quello letto, se in sola lettura) e le voci: prima quelle grezze non lette,
     * identiche, poi quelle vive.
     */
    CompoundTag write(CompoundTag tag) {
        CompoundTag framesTag = new CompoundTag();
        unread.forEach((key, raw) -> framesTag.put(key, raw.copy()));
        frames.forEach((player, state) -> framesTag.put(player.toString(), FrameStateCodec.write(state)));
        if (readOnly()) {
            tag.put(NbtKeys.SCHEMA, readOnlyMarker.copy());
        } else {
            tag.putInt(NbtKeys.SCHEMA, CURRENT_SCHEMA);
        }
        tag.put(FRAMES, framesTag);
        return tag;
    }
}
