package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyMeterTest {

    private static int run(EnergyMeter meter, EnergyCell cell, Activity a, int extra, double mult, int ticks) {
        int total = 0;
        for (int i = 0; i < ticks; i++) {
            total += meter.tick(cell, a, extra, mult);
        }
        return total;
    }

    @Test
    void idleTwentyTicksDrainsTwoRe() {
        EnergyCell cell = EnergyCell.full(10_000);
        EnergyMeter meter = new EnergyMeter(EnergyRates.spec());
        assertEquals(2, run(meter, cell, Activity.IDLE, 0, 1.0, 20));
        assertEquals(9_998, cell.stored());
    }

    @Test
    void walkOneSecondDrainsEight() {
        EnergyCell cell = EnergyCell.full(10_000);
        run(new EnergyMeter(EnergyRates.spec()), cell, Activity.WALK, 0, 1.0, 20);
        assertEquals(9_992, cell.stored());
    }

    @Test
    void sprintOneSecondDrainsTwenty() {
        EnergyCell cell = EnergyCell.full(10_000);
        run(new EnergyMeter(EnergyRates.spec()), cell, Activity.SPRINT, 0, 1.0, 20);
        assertEquals(9_980, cell.stored());
    }

    @Test
    void multiplierAppliesToActivityAndExtras() {
        EnergyCell cell = EnergyCell.full(10_000);
        run(new EnergyMeter(EnergyRates.spec()), cell, Activity.WALK, 0, 1.25, 20);
        assertEquals(9_990, cell.stored());
        // (100 + 150) milliRE x 1.25 = 312.5 -> 313 per tick: 6 RE in 20 tick (5 se il moltiplicatore saltasse gli extra)
        EnergyCell withExtra = EnergyCell.full(10_000);
        run(new EnergyMeter(EnergyRates.spec()), withExtra, Activity.IDLE, 150, 1.25, 20);
        assertEquals(9_994, withExtra.stored());
    }

    @Test
    void extraMilliRePerTickAddsModuleCosts() {
        EnergyCell cell = EnergyCell.full(10_000);
        // modulo da 3 RE/s = 150 milliRE/tick, sommato ai 2 RE/s da fermo: 5 RE in 20 tick
        EnergyMeter meter = new EnergyMeter(EnergyRates.spec());
        run(meter, cell, Activity.IDLE, 150, 1.0, 20);
        assertEquals(9_995, cell.stored());
    }

    @Test
    void carryNeverExceedsOneRe() {
        EnergyCell cell = EnergyCell.full(10_000);
        EnergyMeter meter = new EnergyMeter(EnergyRates.spec());
        for (int i = 0; i < 500; i++) {
            meter.tick(cell, Activity.WALK, 37, 1.1);
            assertTrue(meter.carryMilliRe() < 1000);
            assertTrue(meter.carryMilliRe() >= 0);
        }
    }

    @Test
    void emptyCellDrainsNothingAndReportsZero() {
        EnergyCell cell = EnergyCell.empty(10_000);
        EnergyMeter meter = new EnergyMeter(EnergyRates.spec());
        assertEquals(0, run(meter, cell, Activity.SPRINT, 0, 1.0, 20));
        assertEquals(0, cell.stored());
    }
}
