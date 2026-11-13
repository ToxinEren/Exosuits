package com.toxineren.exoframes.core.module;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.Objects;

/** Capacita' di slot modulo di un pezzo. */
public final class ModuleSlots {

    private ModuleSlots() {
    }

    /**
     * Slot base del pezzo letti da {@code pieces.baseModuleSlots} piu' slot extra del livello
     * ({@link Tier#extraModuleSlots()}: la config non ha ancora una voce per i livelli).
     *
     * @throws IllegalArgumentException se la config non ha una voce per lo slot
     */
    public static int capacity(PieceSlot slot, Tier tier, ExoframesConfig.Pieces pieces) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(tier, "tier");
        Integer base = Objects.requireNonNull(pieces, "pieces").baseModuleSlots.get(slot);
        if (base == null) {
            throw new IllegalArgumentException("no pieces.baseModuleSlots entry for " + slot);
        }
        return base + tier.extraModuleSlots();
    }
}
