package com.toxineren.exoframes.client;

/**
 * La decisione del tasto di uscita tenuto premuto (design, par. 5.5), senza classi del gioco: {@link ExitKey} gli
 * passa a ogni tick del client se il tasto e' giu' e se si puo' contare (nessuna schermata aperta e la tuta del
 * giocatore locale disegnata) e fa quello che il risultato dice; i test JUnit la provano da sola.
 * <p>
 * Le regole: il conteggio sale di uno a ogni tick con il tasto giu' e le condizioni soddisfatte; quando arriva ai tick
 * che servono il pacchetto parte, una volta sola per pressione; prima, la barra delle azioni mostra la percentuale.
 * Lasciare il tasto azzera il conteggio e permette un nuovo invio; una schermata aperta o la tuta che non si disegna
 * piu' azzerano il conteggio ma non l'invio; {@link #reset} (uscire dal mondo) azzera tutto. Quando il conteggio si
 * azzera, o il pacchetto parte, l'avanzamento mostrato si toglie. Solo sul thread del client.
 */
final class ExitHold {
    /** Tick del client in un secondo. */
    private static final int TICKS_PER_SECOND = 20;

    /** Che cosa deve fare il tasto in questo tick. */
    enum Step {
        /** Nulla. */
        NONE,
        /** Mostrare l'avanzamento ({@link #percent()}) sulla barra delle azioni. */
        SHOW_PROGRESS,
        /** Togliere l'avanzamento mostrato dalla barra delle azioni. */
        CLEAR_PROGRESS,
        /** Mandare il pacchetto di uscita; nulla era mostrato. */
        SEND,
        /** Togliere l'avanzamento mostrato e mandare il pacchetto di uscita. */
        CLEAR_AND_SEND
    }

    /** Tick consecutivi con il tasto giu' e le condizioni soddisfatte. */
    private int heldTicks;
    /** Vero se in questa pressione il pacchetto e' gia' partito. */
    private boolean sent;
    /** Vero se la barra delle azioni mostra un avanzamento da togliere. */
    private boolean showingProgress;
    /** L'ultima percentuale data con {@link Step#SHOW_PROGRESS}. */
    private long percent;

    /**
     * I tick di tenuta che servono per uscire con {@code seconds} secondi di configurazione: zero (una pressione basta)
     * per zero o meno; mai oltre {@link Integer#MAX_VALUE}.
     */
    static int ticksNeeded(int seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, seconds) * (long) TICKS_PER_SECOND);
    }

    /**
     * Un tick del client: {@code keyDown} se il tasto e' giu', {@code canCount} se nessuna schermata e' aperta e la tuta
     * del giocatore locale si disegna, {@code needed} i tick che servono ({@link #ticksNeeded}).
     */
    Step tick(boolean keyDown, boolean canCount, int needed) {
        if (!keyDown) {
            sent = false;
            return stopCounting();
        }
        if (!canCount) {
            return stopCounting();
        }
        heldTicks = heldTicks == Integer.MAX_VALUE ? heldTicks : heldTicks + 1;
        if (!sent && heldTicks >= needed) {
            sent = true;
            boolean wasShowing = showingProgress;
            showingProgress = false;
            return wasShowing ? Step.CLEAR_AND_SEND : Step.SEND;
        }
        if (sent) {
            return Step.NONE;
        }
        percent = Math.min(100L, heldTicks * 100L / needed);
        showingProgress = true;
        return Step.SHOW_PROGRESS;
    }

    /** La percentuale da mostrare dopo un {@link Step#SHOW_PROGRESS}, fra 0 e 100. */
    long percent() {
        return percent;
    }

    /** Uscendo dal mondo: conteggio, invio e avanzamento mostrato tornano a zero. */
    void reset() {
        heldTicks = 0;
        sent = false;
        showingProgress = false;
        percent = 0;
    }

    /** Azzera il conteggio (non l'invio); se un avanzamento era mostrato va tolto. */
    private Step stopCounting() {
        heldTicks = 0;
        if (showingProgress) {
            showingProgress = false;
            return Step.CLEAR_PROGRESS;
        }
        return Step.NONE;
    }
}
