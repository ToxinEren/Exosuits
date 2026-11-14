package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Le zone del clic su un telaio piazzato ({@link ClickZones}) ai loro confini. */
class ClickZonesTest {
    private static final double TINY = 1e-9;

    @Test
    void heightBordersAreOnePointOneAndTwoPointTwo() {
        assertEquals(1.1, ClickZones.LEGS_TOP);
        assertEquals(2.2, ClickZones.HEAD_BOTTOM);
        assertEquals(List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG), ClickZones.bandNearestFirst(0.0, -0.1));
        assertEquals(List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG), ClickZones.bandNearestFirst(1.1 - TINY, -0.1));
        assertEquals(List.of(PieceSlot.TORSO, PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM),
                ClickZones.bandNearestFirst(1.1, -0.1));
        assertEquals(List.of(PieceSlot.TORSO, PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM),
                ClickZones.bandNearestFirst(2.2 - TINY, -0.1));
        assertEquals(List.of(PieceSlot.HELMET), ClickZones.bandNearestFirst(2.2, -0.1));
        assertEquals(List.of(PieceSlot.HELMET), ClickZones.bandNearestFirst(2.97, 0.4));
        // un giocatore accovacciato fuori dal telaio clicca all'altezza dei suoi occhi (1,27): fascia di mezzo
        assertEquals(PieceSlot.TORSO, ClickZones.bandNearestFirst(1.27, 0.0).get(0));
    }

    @Test
    void armBorderIsAQuarterBlockFromTheCentre() {
        assertEquals(0.25, ClickZones.ARM_OFFSET);
        assertEquals(List.of(PieceSlot.TORSO, PieceSlot.RIGHT_ARM, PieceSlot.LEFT_ARM),
                ClickZones.bandNearestFirst(1.6, 0.25));
        assertEquals(List.of(PieceSlot.RIGHT_ARM, PieceSlot.TORSO, PieceSlot.LEFT_ARM),
                ClickZones.bandNearestFirst(1.6, 0.25 + TINY));
        assertEquals(List.of(PieceSlot.TORSO, PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM),
                ClickZones.bandNearestFirst(1.6, -0.25));
        assertEquals(List.of(PieceSlot.LEFT_ARM, PieceSlot.TORSO, PieceSlot.RIGHT_ARM),
                ClickZones.bandNearestFirst(1.6, -0.25 - TINY));
        assertEquals(List.of(PieceSlot.RIGHT_LEG, PieceSlot.LEFT_LEG), ClickZones.bandNearestFirst(0.5, TINY));
        assertEquals(List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG), ClickZones.bandNearestFirst(0.5, 0.0),
                "the exact centre counts as the left side");
        assertTrue(ClickZones.onRightSide(TINY));
        assertFalse(ClickZones.onRightSide(0.0));
    }

    /** La destra del telaio per i quattro orientamenti e per uno obliquo: yaw 0 ha la destra verso -X. */
    @Test
    void rightIsTheFramesOwnRightForAnyYaw() {
        assertEquals(1.0, ClickZones.rightOf(0f, -1.0, 0.0), TINY);
        assertEquals(-1.0, ClickZones.rightOf(0f, 1.0, 0.0), TINY);
        assertEquals(0.0, ClickZones.rightOf(0f, 0.0, 1.0), TINY, "in front is neither side");
        assertEquals(1.0, ClickZones.rightOf(90f, 0.0, -1.0), TINY, "facing west the right is north");
        assertEquals(1.0, ClickZones.rightOf(180f, 1.0, 0.0), TINY, "facing north the right is east");
        assertEquals(1.0, ClickZones.rightOf(-90f, 0.0, 1.0), TINY, "facing east the right is south");
        assertEquals(1.0, ClickZones.rightOf(270f, 0.0, 1.0), TINY, "yaw is not wrapped");
        double diagonal = Math.sqrt(0.5);
        assertEquals(1.0, ClickZones.rightOf(45f, -diagonal, -diagonal), 1e-6, "facing south-west");
    }
}
