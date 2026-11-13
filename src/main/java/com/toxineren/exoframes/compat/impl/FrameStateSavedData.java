package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
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
 * file viene da uno schema piu' recente di quello noto, si scrive un solo ERROR nel log e tutte le voci restano
 * grezze: nessuna viene letta e nessuna si perde, e alla scrittura si conserva il marcatore letto.
 * <p>
 * Solo sul thread del server; chi modifica una voce deve chiamare {@link #setDirty()}.
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
    /** Il marcatore da scrivere: quello corrente, o quello piu' recente letto dal file. */
    private int schema = CURRENT_SCHEMA;

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

    /** La mappa viva UUID -> stato: dopo una modifica va chiamato {@link #setDirty()}. */
    Map<UUID, FrameState> frames() {
        return frames;
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
        int schema = tag.contains(NbtKeys.SCHEMA, Tag.TAG_ANY_NUMERIC) ? tag.getInt(NbtKeys.SCHEMA) : CURRENT_SCHEMA;
        if (schema > CURRENT_SCHEMA) {
            RedstoneExoframes.LOGGER.error("Frame states saved with schema {} (known: {}): keeping {} entries "
                    + "unread and unchanged", schema, CURRENT_SCHEMA, framesTag.size());
            data.schema = schema;
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
     * Dimentica le voci grezze non lette del giocatore (chiamato quando il suo stato viene creato o sostituito con
     * {@code update()} o rimosso con {@code clear()}): la chiave e' confrontata come UUID, cosi' vale anche per una
     * chiave scritta in forma non canonica.
     */
    void dropUnread(UUID player) {
        if (unread.isEmpty()) {
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

    /** Scrive il marcatore di schema e le voci: prima quelle grezze non lette, identiche, poi quelle vive. */
    CompoundTag write(CompoundTag tag) {
        CompoundTag framesTag = new CompoundTag();
        unread.forEach((key, raw) -> framesTag.put(key, raw.copy()));
        frames.forEach((player, state) -> framesTag.put(player.toString(), FrameStateCodec.write(state)));
        tag.putInt(NbtKeys.SCHEMA, schema);
        tag.put(FRAMES, framesTag);
        return tag;
    }
}
