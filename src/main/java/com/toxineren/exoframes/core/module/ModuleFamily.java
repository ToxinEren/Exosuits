package com.toxineren.exoframes.core.module;

import com.toxineren.exoframes.core.armor.PieceSlot;

/** Famiglia di moduli: determina su quali pezzi un modulo puo' essere installato. */
public enum ModuleFamily {
    HELMET,
    TORSO,
    ARM,
    LEGS;

    /** True se un modulo di questa famiglia puo' stare su un pezzo nello slot dato. */
    public boolean accepts(PieceSlot slot) {
        switch (this) {
            case HELMET:
                return slot == PieceSlot.HELMET;
            case TORSO:
                return slot == PieceSlot.TORSO;
            case ARM:
                return slot == PieceSlot.LEFT_ARM || slot == PieceSlot.RIGHT_ARM;
            case LEGS:
                return slot == PieceSlot.LEFT_LEG || slot == PieceSlot.RIGHT_LEG;
            default:
                return false;
        }
    }
}
