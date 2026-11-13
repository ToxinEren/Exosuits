package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.HashMap;
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
 * In lettura ogni voce e' isolata: una voce corrotta viene scartata con un avviso, le altre restano.
 * <p>
 * Solo sul thread del server; chi modifica una voce deve chiamare {@link #setDirty()}.
 */
final class FrameStateSavedData extends SavedData {
    static final String NAME = "redstone_exoframes_frames";
    /** Tag con una voce per giocatore, chiave = UUID in forma testuale. */
    private static final String FRAMES = "frames";

    private final Map<UUID, FrameState> frames = new HashMap<>();

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
     * esiste per i test). Ogni voce e' isolata: se la sua lettura lancia, si scarta solo quella voce con un avviso.
     * Un'eccezione che uscisse da qui farebbe ricreare il dato vuoto e il salvataggio successivo cancellerebbe gli
     * stati di tutti i giocatori.
     */
    static FrameStateSavedData load(CompoundTag tag, BiFunction<CompoundTag, ExoframesConfig, FrameState> reader) {
        FrameStateSavedData data = new FrameStateSavedData();
        CompoundTag framesTag = tag.getCompound(FRAMES);
        for (String key : framesTag.getAllKeys()) {
            if (!framesTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }
            UUID player;
            try {
                player = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                RedstoneExoframes.LOGGER.warn("Ignoring frame state with invalid player id: {}", key);
                continue;
            }
            try {
                data.frames.put(player, reader.apply(framesTag.getCompound(key), RedstoneExoframes.config()));
            } catch (RuntimeException e) {
                RedstoneExoframes.LOGGER.warn("Skipping corrupt frame state of player {}: {}", key, e.toString());
            }
        }
        return data;
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

    private CompoundTag write(CompoundTag tag) {
        CompoundTag framesTag = new CompoundTag();
        frames.forEach((player, state) -> framesTag.put(player.toString(), FrameStateCodec.write(state)));
        tag.put(FRAMES, framesTag);
        return tag;
    }
}
