package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.energy.ChargerRule.Result;
import com.toxineren.exoframes.core.energy.ChargerRule.Status;
import org.junit.jupiter.api.Test;

class ChargerRuleTest {
    /** I predefiniti della config: capacita' della cella, RE di una polvere, RE al secondo. */
    private static final int CAP = 10_000;
    private static final int DUST_RE = 500;
    private static final int RATE = 50;

    /** Un tick con i predefiniti. */
    private static Result t(int stored, int dust, int reserve, int carry) {
        return ChargerRule.tick(stored, CAP, dust, reserve, carry, DUST_RE, RATE);
    }

    /** Un tick dai valori lasciati da quello prima. */
    private static Result next(Result r, int capacity, int rePerRedstone, int rePerSecond) {
        return ChargerRule.tick(r.stored(), capacity, r.dust(), r.reserve(), r.carryMilli(), rePerRedstone,
                rePerSecond);
    }

    @Test
    void twoAndAHalfRePerTick() {
        assertEquals(new Result(2, 19, 498, 500, true), t(0, 20, 0, 0));
        assertEquals(new Result(5, 19, 495, 0, true), t(2, 19, 498, 500));
    }

    @Test
    void anEmptyCellFillsInFourThousandTicksWithTwentyDust() {
        Result r = new Result(0, 20, 0, 0, false);
        for (int i = 0; i < 3_999; i++) {
            r = next(r, CAP, DUST_RE, RATE);
        }
        assertEquals(9_997, r.stored());
        assertTrue(r.charging());
        r = next(r, CAP, DUST_RE, RATE);
        assertEquals(new Result(10_000, 0, 0, 0, true), r);
        r = next(r, CAP, DUST_RE, RATE);
        assertEquals(new Result(10_000, 0, 0, 0, false), r);
    }

    @Test
    void aFullCellBurnsNothing() {
        assertEquals(new Result(10_000, 5, 0, 0, false), t(10_000, 5, 0, 700));
        // sopra la capacita' vale piena: la carica non si taglia, la riserva resta per la cella dopo
        assertEquals(new Result(40_000, 5, 123, 0, false), t(40_000, 5, 123, 0));
    }

    @Test
    void noRedstoneMeansNoCharge() {
        assertEquals(new Result(100, 0, 0, 0, false), t(100, 0, 0, 400));
    }

    @Test
    void theReserveIsSpentBeforeTheNextDust() {
        Result r = t(0, 3, 2, 0);
        assertEquals(new Result(2, 3, 0, 500, true), r);
        assertEquals(new Result(5, 2, 497, 0, true), next(r, CAP, DUST_RE, RATE));
        // la riserva finisce a meta' del ritmo del tick: il riporto si lascia cadere
        assertEquals(new Result(1, 3, 0, 0, true), t(0, 3, 1, 0));
    }

    @Test
    void theLastReNeverOverfills() {
        assertEquals(new Result(10_000, 0, 99, 0, true), t(9_999, 0, 100, 0));
    }

    @Test
    void slowRateStillCharges() {
        Result r = new Result(0, 1, 0, 0, false);
        for (int i = 1; i <= 19; i++) {
            r = next(r, CAP, DUST_RE, 1);
            assertEquals(0, r.stored(), "tick " + i);
            assertTrue(r.charging(), "tick " + i);
        }
        assertEquals(950, r.carryMilli());
        r = next(r, CAP, DUST_RE, 1);
        assertEquals(1, r.stored());
        assertEquals(0, r.carryMilli());
    }

    @Test
    void limitsOfTheConfigNeverOverflow() {
        assertEquals(new Result(5_000, 63, 995_000, 999, true),
                ChargerRule.tick(0, 1_000_000, 64, 0, 999, 1_000_000, 100_000));
        // una polvere da 1 RE: una polvere per tick, sotto il ritmo della config
        assertEquals(new Result(1, 63, 0, 0, true), ChargerRule.tick(0, 1_000_000, 64, 0, 0, 1, 100_000));
        assertEquals(new Result(1_000_000, 0, 999_999, 0, true),
                ChargerRule.tick(999_999, 1_000_000, 0, 1_000_000, 0, 1_000_000, 100_000));
    }

    @Test
    void energyIsConserved() {
        int[][] configs = {{500, 50}, {1, 100_000}, {1_000_000, 1}, {7, 333}};
        for (int[] config : configs) {
            int rePerRedstone = config[0];
            int rePerSecond = config[1];
            String label = "rePerRedstone " + rePerRedstone + ", rePerSecond " + rePerSecond;
            Result r = new Result(0, 64, 0, 0, false);
            long total = 64L * rePerRedstone;
            for (int i = 1; i <= 20_000; i++) {
                r = next(r, CAP, rePerRedstone, rePerSecond);
                String at = label + ", tick " + i;
                assertEquals(total, r.stored() + r.reserve() + (long) r.dust() * rePerRedstone, at);
                assertTrue(r.carryMilli() >= 0 && r.carryMilli() <= 999, at);
                assertTrue(r.stored() <= CAP, at);
                assertTrue(r.reserve() >= 0, at);
                assertTrue(r.dust() >= 0, at);
            }
        }
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, 0, 1, 0, 0, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(-1, CAP, 1, 0, 0, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, -1, 0, 0, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, 1, -1, 0, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, 1, 0, -1, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, 1, 0, 1_000, DUST_RE, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, 1, 0, 0, 0, RATE));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.tick(0, CAP, 1, 0, 0, DUST_RE, 0));
        // l'eccezione nomina l'argomento e il valore
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ChargerRule.tick(0, CAP, 1, 0, 1_000, DUST_RE, RATE));
        assertEquals("carryMilli must be in [0, 999]: 1000", e.getMessage());
    }

    @Test
    void invalidArgumentsOfTheReadingsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.status(true, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.status(true, -1, CAP, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.status(true, 0, CAP, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.status(true, 0, CAP, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.percent(0, 0));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.percent(-1, CAP));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.comparator(true, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ChargerRule.comparator(true, -1, CAP));
    }

    @Test
    void statusPrecedence() {
        assertEquals(Status.NO_CELL, ChargerRule.status(false, 0, CAP, 5, 100));
        assertEquals(Status.FULL, ChargerRule.status(true, 10_000, CAP, 0, 0));
        assertEquals(Status.FULL, ChargerRule.status(true, 40_000, CAP, 5, 5));
        assertEquals(Status.NO_REDSTONE, ChargerRule.status(true, 100, CAP, 0, 0));
        assertEquals(Status.CHARGING, ChargerRule.status(true, 100, CAP, 1, 0));
        assertEquals(Status.CHARGING, ChargerRule.status(true, 100, CAP, 0, 1));
    }

    @Test
    void percentRoundsDown() {
        assertEquals(0, ChargerRule.percent(0, CAP));
        assertEquals(0, ChargerRule.percent(99, CAP));
        assertEquals(1, ChargerRule.percent(100, CAP));
        assertEquals(99, ChargerRule.percent(9_999, CAP));
        assertEquals(100, ChargerRule.percent(10_000, CAP));
        assertEquals(100, ChargerRule.percent(40_000, CAP));
        assertEquals(99, ChargerRule.percent(999_999, 1_000_000));
    }

    @Test
    void comparatorFromZeroToFifteen() {
        assertEquals(0, ChargerRule.comparator(false, 5_000, CAP));
        assertEquals(0, ChargerRule.comparator(true, 0, CAP));
        assertEquals(0, ChargerRule.comparator(true, 666, CAP));
        assertEquals(1, ChargerRule.comparator(true, 667, CAP));
        assertEquals(7, ChargerRule.comparator(true, 5_000, CAP));
        assertEquals(14, ChargerRule.comparator(true, 9_999, CAP));
        assertEquals(15, ChargerRule.comparator(true, 10_000, CAP));
        assertEquals(15, ChargerRule.comparator(true, 40_000, CAP));
    }

    @Test
    void chargingStaysTrueOnTheTickTheReserveEnds() {
        // la riserva finisce in questo tick e c'e' altra polvere: nessun tick spento fra una polvere e l'altra
        Result r = t(0, 1, 2, 500);
        assertEquals(new Result(2, 1, 0, 0, true), r);
        assertEquals(new Result(4, 0, 498, 500, true), next(r, CAP, DUST_RE, RATE));
        // senza altra polvere si spegne al tick dopo
        Result last = t(0, 0, 2, 500);
        assertEquals(new Result(2, 0, 0, 0, true), last);
        assertFalse(next(last, CAP, DUST_RE, RATE).charging());
    }
}
