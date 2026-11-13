package com.toxineren.exoframes.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnergyCostTest {
    @Test
    void perSecondHasNoPerUseCost() {
        EnergyCost cost = EnergyCost.perSecond(60);
        assertEquals(60, cost.perSecond());
        assertEquals(0, cost.perUse());
    }

    @Test
    void perUseHasNoPerSecondCost() {
        EnergyCost cost = EnergyCost.perUse(200);
        assertEquals(0, cost.perSecond());
        assertEquals(200, cost.perUse());
    }
}
