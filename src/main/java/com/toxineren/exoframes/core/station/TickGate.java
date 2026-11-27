package com.toxineren.exoframes.core.station;

/**
 * Un passaggio per tick del server: il menu della stazione lo usa per le azioni che scrivono (al piu' una per tick
 * per menu, design M4 §5.6) e per l'invio della vista del telaio (al piu' una per tick per menu, §5.13). Il tick e'
 * quello del contatore del server ({@code server.getTickCount()}). Un tick e' aperto finche' non e' stato usato; un
 * tick diverso dall'ultimo usato e' sempre aperto, anche uno minore: se il contatore del server torna indietro il
 * limite riparte, invece di restare chiuso fino al tick di prima. Nessun valore fa da sentinella: il primo tick passa
 * sempre, anche {@link Integer#MIN_VALUE}.
 * <p>
 * Non e' sicuro fra thread: lo usa solo il thread del server.
 */
public final class TickGate {
    /** Vero dopo il primo uso: prima nessun tick e' stato usato. */
    private boolean used;
    /** L'ultimo tick usato, valido solo dopo il primo uso. */
    private int lastUsed;

    /** Vero se il tick non e' ancora stato usato: il primo uso, o un tick diverso dall'ultimo usato. */
    public boolean open(int tick) {
        return !used || tick != lastUsed;
    }

    /** Segna il tick come usato: fino a un tick diverso il passaggio resta chiuso. */
    public void use(int tick) {
        used = true;
        lastUsed = tick;
    }

    /** Se il tick e' aperto lo usa e da' vero; altrimenti da' falso e non cambia nulla. */
    public boolean tryUse(int tick) {
        if (!open(tick)) {
            return false;
        }
        use(tick);
        return true;
    }
}
