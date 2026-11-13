package com.toxineren.exoframes.core.armor;

/** Stato visivo di un pezzo in base alla frazione di durabilita' rimasta. */
public enum PieceCondition {
    /** Sopra il 50%. */
    INTACT,
    /** Dal 15% al 50% compresi. */
    DAMAGED,
    /** Sotto il 15% ma sopra 0. */
    CRITICAL,
    /** Durabilita' 0. */
    BROKEN;

    public static PieceCondition of(int durability, int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("max must be > 0: " + max);
        }
        if (durability < 0 || durability > max) {
            throw new IllegalArgumentException("durability must be in [0, " + max + "]: " + durability);
        }
        if (durability == 0) {
            return BROKEN;
        }
        // Confronti in interi: durability/max > 1/2 e durability/max < 15/100, senza errori in virgola mobile.
        long d = durability;
        if (d * 2 > max) {
            return INTACT;
        }
        if (d * 100 < 15L * max) {
            return CRITICAL;
        }
        return DAMAGED;
    }
}
