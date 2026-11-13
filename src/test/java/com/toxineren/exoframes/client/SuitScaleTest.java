package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Scala della tuta e sollevamento del pilota (design, par. 4.7). */
class SuitScaleTest {

    @Test
    void raiseAlignsTheHeadPivots() {
        // Il perno di testa e busto (y = 24 px) della tuta in scala attorno ai piedi finisce a 24 * S: il pilota
        // sollevato di PILOT_RAISE_PX ci arriva con il proprio.
        assertEquals(24f * SuitScale.S, 24f + SuitScale.PILOT_RAISE_PX, 1e-4);
    }

    @Test
    void raiseInBlocksUsesThePlayerModelScale() {
        assertEquals(9.6f, SuitScale.PILOT_RAISE_PX, 1e-4);
        assertEquals(0.5625f, SuitScale.pilotRaiseBlocks(), 1e-4);
    }
}
