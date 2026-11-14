package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.client.ExitHold.Step;
import org.junit.jupiter.api.Test;

/** La decisione del tasto di uscita tenuto premuto ({@link ExitHold}), come la usa {@code ExitKey} a ogni tick. */
class ExitHoldTest {
    private static final int ONE_SECOND = 20;

    /** Tiene il tasto giu' (con le condizioni soddisfatte) per {@code ticks} tick e da' quante volte ha mandato. */
    private static int hold(ExitHold hold, int ticks) {
        int sends = 0;
        for (int i = 0; i < ticks; i++) {
            Step step = hold.tick(true, true, ONE_SECOND);
            if (step == Step.SEND || step == Step.CLEAR_AND_SEND) {
                sends++;
            }
        }
        return sends;
    }

    @Test
    void ticksNeededFollowTheSeconds() {
        assertEquals(20, ExitHold.ticksNeeded(1));
        assertEquals(60, ExitHold.ticksNeeded(3));
        assertEquals(0, ExitHold.ticksNeeded(0));
        assertEquals(0, ExitHold.ticksNeeded(-5));
        assertEquals(Integer.MAX_VALUE, ExitHold.ticksNeeded(Integer.MAX_VALUE));
    }

    /** Il pacchetto parte al ventesimo tick, una volta sola finche' il tasto resta giu'; prima si vede l'avanzamento. */
    @Test
    void sendsOnceWhenTheHoldIsComplete() {
        ExitHold hold = new ExitHold();
        for (int held = 1; held < ONE_SECOND; held++) {
            assertEquals(Step.SHOW_PROGRESS, hold.tick(true, true, ONE_SECOND), "tick " + held);
            assertEquals(held * 100L / ONE_SECOND, hold.percent());
        }
        assertEquals(Step.CLEAR_AND_SEND, hold.tick(true, true, ONE_SECOND));
        for (int held = 0; held < 100; held++) {
            assertEquals(Step.NONE, hold.tick(true, true, ONE_SECOND), "a second send while still held");
        }
    }

    /** Con zero secondi una pressione manda subito, senza avanzamento da togliere. */
    @Test
    void zeroSecondsSendsAtTheFirstTick() {
        ExitHold hold = new ExitHold();
        assertEquals(Step.SEND, hold.tick(true, true, ExitHold.ticksNeeded(0)));
        assertEquals(Step.NONE, hold.tick(true, true, ExitHold.ticksNeeded(0)));
    }

    /** Lasciato prima del tempo: l'avanzamento si toglie, nulla parte, e una nuova pressione riparte da zero. */
    @Test
    void releasingResetsTheCount() {
        ExitHold hold = new ExitHold();
        assertEquals(0, hold(hold, 19));
        assertEquals(Step.CLEAR_PROGRESS, hold.tick(false, false, ONE_SECOND));
        assertEquals(Step.NONE, hold.tick(false, false, ONE_SECOND), "progress cleared twice");
        assertEquals(0, hold(hold, 19), "the count did not start again from zero");
        assertEquals(1, hold(hold, 1));
    }

    /** Lasciato dopo l'invio e premuto di nuovo: una nuova tenuta completa manda di nuovo. */
    @Test
    void releasingAfterTheSendAllowsANewOne() {
        ExitHold hold = new ExitHold();
        assertEquals(1, hold(hold, 40));
        assertEquals(Step.NONE, hold.tick(false, false, ONE_SECOND), "nothing shown after the send");
        assertEquals(1, hold(hold, 20));
    }

    /**
     * Una schermata aperta (o la tuta che non si disegna) a meta' tenuta azzera il conteggio e toglie l'avanzamento;
     * chiusa, con il tasto ancora giu', servono di nuovo 20 tick. Dopo l'invio non permette un secondo invio: serve
     * lasciare il tasto.
     */
    @Test
    void aScreenStopsTheCountButNotTheSend() {
        ExitHold hold = new ExitHold();
        assertEquals(0, hold(hold, 15));
        assertEquals(Step.CLEAR_PROGRESS, hold.tick(true, false, ONE_SECOND));
        assertEquals(Step.NONE, hold.tick(true, false, ONE_SECOND));
        assertEquals(0, hold(hold, 19), "the count did not start again after the screen");
        assertEquals(1, hold(hold, 1));
        assertEquals(Step.NONE, hold.tick(true, false, ONE_SECOND), "a screen after the send");
        assertEquals(0, hold(hold, 40), "sent again without releasing the key");
    }

    /** Uscire dal mondo azzera tutto: anche l'invio di una pressione mai lasciata. */
    @Test
    void resetForgetsEverything() {
        ExitHold hold = new ExitHold();
        assertEquals(1, hold(hold, 20));
        hold.reset();
        assertEquals(1, hold(hold, 20));
        assertEquals(0, hold(hold, 5));
        hold.reset();
        assertEquals(Step.NONE, hold.tick(false, false, ONE_SECOND), "progress still marked as shown after reset");
    }
}
