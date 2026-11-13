package com.toxineren.exoframes.core.armor;

/** Posizione di un pezzo sul telaio, con la sua quota di protezione e gli slot modulo di base. */
public enum PieceSlot {
    HELMET(0.15, 1),
    TORSO(0.30, 2),
    LEFT_ARM(0.125, 1),
    RIGHT_ARM(0.125, 1),
    LEFT_LEG(0.15, 1),
    RIGHT_LEG(0.15, 1);

    private final double protectionShare;
    private final int baseModuleSlots;

    PieceSlot(double protectionShare, int baseModuleSlots) {
        this.protectionShare = protectionShare;
        this.baseModuleSlots = baseModuleSlots;
    }

    /** Quota della protezione totale coperta da questo pezzo; le sei quote sommano a 1. */
    public double protectionShare() {
        return protectionShare;
    }

    public int baseModuleSlots() {
        return baseModuleSlots;
    }
}
