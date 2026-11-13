package com.toxineren.exoframes.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.RedstoneExoframes;
import org.junit.jupiter.api.Test;

class CoreSmokeTest {
    @Test
    void modIdIsStable() {
        assertEquals("redstone_exoframes", RedstoneExoframes.MOD_ID);
    }
}
