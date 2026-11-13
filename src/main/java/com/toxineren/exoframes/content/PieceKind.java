package com.toxineren.exoframes.content;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.List;
import java.util.Objects;

/**
 * Tipo di un oggetto pezzo: dice in quali posizioni del telaio si puo' montare. Braccio e gamba sono un solo oggetto
 * ciascuno, valido per i due lati.
 */
public enum PieceKind {
    HELMET(List.of(PieceSlot.HELMET)),
    TORSO(List.of(PieceSlot.TORSO)),
    ARM(List.of(PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM)),
    LEG(List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG));

    private final List<PieceSlot> slots;

    PieceKind(List<PieceSlot> slots) {
        this.slots = slots;
    }

    /** Le posizioni in cui si monta un oggetto di questo tipo (sinistra prima di destra); lista immutabile. */
    public List<PieceSlot> slots() {
        return slots;
    }

    /** Il tipo che si monta nella posizione {@code slot}. */
    public static PieceKind of(PieceSlot slot) {
        Objects.requireNonNull(slot, "slot must not be null");
        for (PieceKind kind : values()) {
            if (kind.slots.contains(slot)) {
                return kind;
            }
        }
        throw new IllegalStateException("no piece kind for slot " + slot);
    }
}
