package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyCellTest {

    @Test
    void fullCellHasSpecCapacity() {
        assertEquals(10_000, EnergyCell.full(10_000).stored());
    }

    @Test
    void drainReturnsActualAmountAndClampsAtZero() {
        EnergyCell cell = new EnergyCell(10_000, 30);
        assertEquals(30, cell.drain(50));
        assertEquals(0, cell.stored());
        assertTrue(cell.isEmpty());
    }

    @Test
    void chargeClampsAtCapacity() {
        EnergyCell cell = new EnergyCell(100, 90);
        assertEquals(10, cell.charge(50));
        assertEquals(100, cell.stored());
    }

    @Test
    void negativeAmountsAreRejected() {
        EnergyCell cell = EnergyCell.full(100);
        assertThrows(IllegalArgumentException.class, () -> cell.drain(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.charge(-1));
    }

    @Test
    void fractionIsStoredOverCapacity() {
        assertEquals(0.25, new EnergyCell(10_000, 2_500).fraction(), 1e-9);
    }
}
