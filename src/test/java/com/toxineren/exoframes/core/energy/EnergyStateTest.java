package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import org.junit.jupiter.api.Test;

class EnergyStateTest {

    private static final double LOW = 0.10;
    private static final ExoframesConfig.Energy CFG = ExoframesConfig.defaults().energy;

    @Test
    void tenPercentIsNormal() {
        assertEquals(EnergyState.NORMAL, EnergyState.of(new EnergyCell(10_000, 1_000), LOW));
    }

    @Test
    void justBelowTenPercentIsLow() {
        assertEquals(EnergyState.LOW, EnergyState.of(new EnergyCell(10_000, 999), LOW));
    }

    @Test
    void zeroIsEmpty() {
        assertEquals(EnergyState.EMPTY, EnergyState.of(EnergyCell.empty(10_000), LOW));
    }

    @Test
    void lowDisablesSprintAndModules() {
        EnergyPenalties p = EnergyPenalties.forState(EnergyState.LOW, CFG);
        assertFalse(p.canSprint());
        assertFalse(p.modulesEnabled());
        assertEquals(1.0, p.speedMultiplier(), 1e-9);
        assertEquals(1.0, p.protectionMultiplier(), 1e-9);
    }

    @Test
    void emptyCutsSpeedAndProtection() {
        EnergyPenalties p = EnergyPenalties.forState(EnergyState.EMPTY, CFG);
        assertFalse(p.canSprint());
        assertFalse(p.modulesEnabled());
        assertEquals(0.30, p.speedMultiplier(), 1e-9);
        assertEquals(0.50, p.protectionMultiplier(), 1e-9);
    }
}
