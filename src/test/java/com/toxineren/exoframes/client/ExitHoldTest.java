package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Il conteggio del tasto di uscita tenuto premuto (design, par. 5.5), come lo usa {@code ExitKey} a ogni tick. */
class ExitHoldTest {

    @Test
    void sendsOnceWhenTheHoldIsComplete() {
        int needed = ExitHold.ticksNeeded(1);
        assertEquals(20, needed);
        boolean sent = false;
        int sends = 0;
        for (int held = 1; held <= 60; held++) {
            if (ExitHold.shouldSend(held, needed, sent)) {
                assertEquals(20, held, "sent before or after one second");
                sends++;
                sent = true;
            }
        }
        assertEquals(1, sends);
        assertEquals(0, ExitHold.ticksNeeded(0));
        assertTrue(ExitHold.shouldSend(1, ExitHold.ticksNeeded(0), false), "with 0 seconds a press sends at once");
        assertEquals(60, ExitHold.ticksNeeded(3));
    }

    @Test
    void releasingResets() {
        int needed = ExitHold.ticksNeeded(1);
        // tenuto 19 tick e lasciato: il conteggio riparte da zero e non si e' mandato nulla
        for (int held = 1; held <= 19; held++) {
            assertFalse(ExitHold.shouldSend(held, needed, false), "sent after " + held + " ticks");
        }
        // di nuovo premuto: servono ancora 20 tick interi
        assertFalse(ExitHold.shouldSend(19, needed, false));
        assertTrue(ExitHold.shouldSend(20, needed, false));
        // finche' non lo si lascia, una sola volta
        assertFalse(ExitHold.shouldSend(21, needed, true));
        assertFalse(ExitHold.shouldSend(200, needed, true));
        // lasciato dopo l'invio e premuto di nuovo: una nuova tenuta completa manda di nuovo
        assertTrue(ExitHold.shouldSend(20, needed, false));
    }
}
