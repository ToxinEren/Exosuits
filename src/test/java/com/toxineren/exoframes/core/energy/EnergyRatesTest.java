package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import org.junit.jupiter.api.Test;

class EnergyRatesTest {

    @Test
    void fromConfigMatchesSpec() {
        assertEquals(EnergyRates.spec(), EnergyRates.from(ExoframesConfig.defaults().energy));
    }

    @Test
    void fromConfigReadsEveryRate() {
        ExoframesConfig.Energy e = new ExoframesConfig.Energy();
        e.idlePerSecond = 1;
        e.walkPerSecond = 4;
        e.sprintPerSecond = 9;
        e.jumpCost = 30;
        assertEquals(new EnergyRates(1, 4, 9, 30), EnergyRates.from(e));
    }
}
