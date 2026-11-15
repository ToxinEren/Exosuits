package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellBarTest {

    @Test
    void zeroIsZero() {
        assertEquals(0, CellBar.width(0, 10_000));
        assertTrue(CellBar.visible(0, 10_000));
    }

    @Test
    void fullIsThirteen() {
        assertEquals(13, CellBar.width(10_000, 10_000));
        assertEquals(CellBar.MAX_WIDTH, CellBar.width(180, 180));
    }

    @Test
    void roundsHalfUp() {
        assertEquals(1, CellBar.width(500, 10_000)); // 0,65
        assertEquals(7, CellBar.width(5_000, 10_000)); // 6,5
        assertEquals(0, CellBar.width(384, 10_000)); // 0,4992
        assertEquals(7, CellBar.width(90, 180)); // 6,5
    }

    @Test
    void aboveCapacityIsClamped() {
        assertEquals(13, CellBar.width(20_000, 10_000));
        assertEquals(0, CellBar.width(-5, 10_000));
        assertEquals(13, CellBar.width(1_000_000, 1_000_000));
    }

    @Test
    void hiddenWhenFullOrCapacityNotPositive() {
        assertFalse(CellBar.visible(10_000, 10_000));
        assertFalse(CellBar.visible(20_000, 10_000));
        assertFalse(CellBar.visible(5, 0));
        assertFalse(CellBar.visible(0, -1));
        assertEquals(0, CellBar.width(5, 0));
        assertEquals(0, CellBar.width(5, -1));
        assertEquals(0xE0201C, CellBar.CELL_COLOR);
    }
}
