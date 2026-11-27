package com.toxineren.exoframes.core.station;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Un passaggio per tick del server (design M4 §5.6 e §5.13): il primo uso di un tick passa e gli altri dello stesso
 * tick no; guardare non usa; un contatore che torna indietro fa ripartire il limite; nessun tick e' escluso.
 */
class TickGateTest {

    @Test
    void firstUseOfATickPasses() {
        TickGate gate = new TickGate();
        assertTrue(gate.tryUse(100), "the first use of tick 100");
        assertFalse(gate.tryUse(100), "a second use of tick 100");
        assertFalse(gate.open(100), "tick 100 is still open after its use");
        assertTrue(gate.tryUse(101), "the first use of tick 101");
    }

    @Test
    void openDoesNotUse() {
        TickGate gate = new TickGate();
        assertTrue(gate.open(5), "tick 5 on a new gate");
        assertTrue(gate.open(5), "looking used tick 5");
        gate.use(5);
        assertFalse(gate.open(5), "tick 5 is still open after its use");
    }

    @Test
    void aCounterGoingBackRestartsTheLimit() {
        TickGate gate = new TickGate();
        assertTrue(gate.tryUse(100), "the first use of tick 100");
        assertTrue(gate.tryUse(40), "tick 40, after the counter went back");
        assertFalse(gate.tryUse(40), "a second use of tick 40");
        assertTrue(gate.tryUse(100), "tick 100 after tick 40");
    }

    @Test
    void extremeTicksPass() {
        TickGate gate = new TickGate();
        assertTrue(gate.tryUse(Integer.MIN_VALUE), "the lowest tick on a new gate");
        assertFalse(gate.tryUse(Integer.MIN_VALUE), "a second use of the lowest tick");
        assertTrue(gate.tryUse(Integer.MAX_VALUE), "the highest tick");
    }
}
