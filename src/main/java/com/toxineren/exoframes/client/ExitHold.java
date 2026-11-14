package com.toxineren.exoframes.client;

/**
 * Il conteggio puro del tasto di uscita tenuto premuto (design, par. 5.5), senza classi del gioco: {@link ExitKey} lo
 * usa a ogni tick del client, e i test JUnit lo provano da soli.
 */
final class ExitHold {
    /** Tick del client in un secondo. */
    private static final int TICKS_PER_SECOND = 20;

    private ExitHold() {
    }

    /**
     * I tick di tenuta che servono per uscire con {@code seconds} secondi di configurazione: zero (una pressione basta)
     * per zero o meno; mai oltre {@link Integer#MAX_VALUE}.
     */
    static int ticksNeeded(int seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, seconds) * (long) TICKS_PER_SECOND);
    }

    /**
     * Vero se, con il tasto tenuto da {@code heldTicks} tick (il primo tick premuto conta uno), il pacchetto di uscita
     * va mandato adesso: la tenuta e' completa e in questa pressione non e' ancora partito.
     */
    static boolean shouldSend(int heldTicks, int needed, boolean alreadySent) {
        return !alreadySent && heldTicks >= needed;
    }
}
