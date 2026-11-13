package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.core.frame.FrameState;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;

/**
 * Un telaio del registro ({@link FrameStateSavedData}): identita', stato e armatura stivata. Lo stesso oggetto passa
 * fra la mappa degli indossati e quella dei piazzati quando si entra e si esce, quindi l'identita' non cambia mai.
 * <p>
 * L'armatura stivata e' una lista modificabile di tag opachi ({@code {slot, data_version, item}}, vedi
 * {@link com.toxineren.exoframes.compat.NbtKeys#STOWED}), mai {@code null} e senza elementi {@code null} (aggiungerne
 * uno lancia subito, cosi' il salvataggio non puo' lanciare): qui non si toccano ItemStack. Lo stato e
 * la lista sono vivi: chi li cambia chiama {@code setDirty()} sul dato salvato.
 */
final class FrameRecord {
    private final UUID frameId;
    private final FrameState state;
    private final List<CompoundTag> stowed;

    /** Record senza armatura stivata. */
    FrameRecord(UUID frameId, FrameState state) {
        this(frameId, state, List.of());
    }

    /** Record con una copia (modificabile) della lista {@code stowed}, che non deve avere elementi {@code null}. */
    FrameRecord(UUID frameId, FrameState state, List<CompoundTag> stowed) {
        this.frameId = Objects.requireNonNull(frameId, "frameId must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        // NonNullList rifiuta null in add e set (anche da addAll)
        this.stowed = NonNullList.create();
        this.stowed.addAll(Objects.requireNonNull(stowed, "stowed must not be null"));
    }

    UUID frameId() {
        return frameId;
    }

    FrameState state() {
        return state;
    }

    /** La lista viva dell'armatura stivata. */
    List<CompoundTag> stowed() {
        return stowed;
    }
}
