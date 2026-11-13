package com.toxineren.exoframes.core.energy;

/** Stato energetico derivato dalla carica della cella. */
public enum EnergyState {
    NORMAL,
    LOW,
    EMPTY;

    /** EMPTY a carica 0; LOW se la frazione e' strettamente sotto la soglia; altrimenti NORMAL. */
    public static EnergyState of(EnergyCell cell, double lowThreshold) {
        if (cell.isEmpty()) {
            return EMPTY;
        }
        return cell.fraction() < lowThreshold ? LOW : NORMAL;
    }
}
