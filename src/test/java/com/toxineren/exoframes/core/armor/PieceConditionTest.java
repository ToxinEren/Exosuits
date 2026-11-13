package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import org.junit.jupiter.api.Test;

class PieceConditionTest {

    @Test
    void fiftyPercentIsDamaged() {
        assertEquals(PieceCondition.DAMAGED, PieceCondition.of(50, 100));
    }

    @Test
    void fiftyOnePercentIsIntact() {
        assertEquals(PieceCondition.INTACT, PieceCondition.of(51, 100));
    }

    @Test
    void fourteenPercentIsCritical() {
        assertEquals(PieceCondition.CRITICAL, PieceCondition.of(14, 100));
    }

    @Test
    void fifteenPercentIsDamaged() {
        assertEquals(PieceCondition.DAMAGED, PieceCondition.of(15, 100));
    }

    @Test
    void zeroIsBroken() {
        assertEquals(PieceCondition.BROKEN, PieceCondition.of(0, 100));
    }

    @Test
    void repairKitRestoresQuarter() {
        Piece piece = new Piece(PieceSlot.TORSO, Tier.INDUSTRIAL, 100, 10);
        piece.repairFraction(0.25);
        assertEquals(35, piece.durability());
    }

    @Test
    void freshPieceUsesTierDurabilityFromConfig() {
        Piece piece = Piece.fresh(PieceSlot.HELMET, Tier.ARMORED, ExoframesConfig.defaults().tiers);
        assertEquals(520, piece.maxDurability());
        assertEquals(520, piece.durability());
        assertEquals(PieceCondition.INTACT, piece.condition());
    }
}
