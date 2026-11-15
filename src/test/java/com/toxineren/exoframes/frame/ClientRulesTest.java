package com.toxineren.exoframes.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** I valori del server per il client: ricevuti, azzerati, fuori dai limiti (design §12.4). */
class ClientRulesTest {

    @BeforeEach
    @AfterEach
    void clean() {
        ClientRules.reset();
        RedstoneExoframes.setConfig(ExoframesConfig.defaults());
    }

    private static ExoframesConfig local() {
        ExoframesConfig local = ExoframesConfig.defaults();
        local.energy.cellCapacity = 4_000;
        local.energy.cellSwapSeconds = 4;
        local.frame.exitHoldSeconds = 2;
        return local;
    }

    private static void assertRules(int cellCapacity, int cellSwapSeconds, int exitHoldSeconds) {
        assertEquals(cellCapacity, ClientRules.cellCapacity());
        assertEquals(cellSwapSeconds, ClientRules.cellSwapSeconds());
        assertEquals(exitHoldSeconds, ClientRules.exitHoldSeconds());
    }

    @Test
    void receivedValuesWinUntilReset() {
        ClientRules.accept(20_000, 5, 3);
        assertRules(20_000, 5, 3);
        ClientRules.reset();
        assertRules(10_000, 2, 1);
    }

    @Test
    void resetFallsBackToTheLocalConfig() {
        RedstoneExoframes.setConfig(local());
        assertRules(4_000, 4, 2);
        ClientRules.accept(20_000, 5, 3);
        assertRules(20_000, 5, 3);
        ClientRules.reset();
        assertRules(4_000, 4, 2);
    }

    @Test
    void outOfRangeValueFallsBackToTheDefault() {
        RedstoneExoframes.setConfig(local());
        ClientRules.accept(0, 31, 2);
        assertRules(10_000, 2, 2); // il predefinito di ExoframesConfig, non quello locale
        ClientRules.accept(2_000_000, 2, 31);
        assertRules(10_000, 2, 1);
        ClientRules.accept(-1, -1, -1);
        assertRules(10_000, 2, 1);
        ClientRules.accept(10_000, 0, 0);
        assertRules(10_000, 0, 0); // zero secondi e' un valore valido della config
        ClientRules.accept(1_000_000, 30, 30);
        assertRules(1_000_000, 30, 30); // i limiti sono compresi
    }
}
