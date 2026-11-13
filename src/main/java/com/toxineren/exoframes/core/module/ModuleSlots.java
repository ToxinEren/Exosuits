package com.toxineren.exoframes.core.module;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;

/** Capacita' di slot modulo di un pezzo. */
public final class ModuleSlots {

    private ModuleSlots() {
    }

    /** Slot base del pezzo piu' slot extra del livello. */
    public static int capacity(PieceSlot slot, Tier tier) {
        return slot.baseModuleSlots() + tier.extraModuleSlots();
    }
}
