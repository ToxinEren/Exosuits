package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CellSwapRuleTest {

    /** Trentasette voci senza celle. */
    private static int[] noCells() {
        int[] e = new int[CellSwapRule.SLOTS];
        Arrays.fill(e, -1);
        return e;
    }

    @Test
    void picksTheFullestBetterCell() {
        int[] e = noCells();
        e[3] = 3_000;
        e[20] = 9_000;
        e[36] = 9_000;
        assertEquals(20, CellSwapRule.pick(2_000, e));
    }

    @Test
    void tieGoesToTheLowestIndex() {
        int[] e = noCells();
        e[5] = 4_000;
        e[2] = 4_000;
        assertEquals(2, CellSwapRule.pick(1_000, e));
    }

    @Test
    void offhandComesLast() {
        int[] e = noCells();
        e[35] = 5_000;
        e[36] = 5_000;
        assertEquals(35, CellSwapRule.pick(1_000, e));
        int[] onlyOffhand = noCells();
        onlyOffhand[36] = 5_000;
        assertEquals(CellSwapRule.OFFHAND, CellSwapRule.pick(1_000, onlyOffhand));
    }

    @Test
    void noInstalledCellTakesAnyCharge() {
        int[] e = noCells();
        e[7] = 1;
        assertEquals(7, CellSwapRule.pick(-1, e));
        int[] empty = noCells();
        empty[7] = 0;
        assertEquals(-1, CellSwapRule.pick(-1, empty)); // una cella vuota non si prende
    }

    @Test
    void nothingBetterGivesMinusOne() {
        int[] e = noCells();
        e[0] = 5_000;
        e[1] = 4_000;
        assertEquals(-1, CellSwapRule.pick(5_000, e)); // uguale non e' meglio
        assertEquals(-1, CellSwapRule.pick(0, noCells()));
    }

    @Test
    void wrongLengthIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CellSwapRule.pick(0, new int[36]));
        assertThrows(IllegalArgumentException.class, () -> CellSwapRule.pick(0, new int[38]));
    }
}
