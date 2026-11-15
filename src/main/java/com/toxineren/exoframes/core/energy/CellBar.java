package com.toxineren.exoframes.core.energy;

/**
 * La barra di un oggetto sotto la sua icona (design §12.10): quella della carica della cella e, con la stessa regola,
 * quella della durata dei pezzi ({@code width(durata, durataMassima)}). La larghezza va da 0 a {@link #MAX_WIDTH}
 * pixel e la limita la mod: il gioco base la limita gia', Forge e NeoForge no.
 */
public final class CellBar {
    /** La larghezza della barra piena, in pixel: quella del gioco base. */
    public static final int MAX_WIDTH = 13;

    /** Il colore della barra della cella: rosso della redstone. */
    public static final int CELL_COLOR = 0xE0201C;

    private CellBar() {
    }

    /**
     * La larghezza della barra: {@code stored} riportato in [0, {@code capacity}], in tredicesimi della capacita',
     * arrotondato al piu' vicino (la meta' per eccesso). 0 se {@code capacity} non e' positiva.
     */
    public static int width(int stored, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(stored, capacity));
        return Math.max(0, Math.min(MAX_WIDTH, Math.round((float) MAX_WIDTH * clamped / capacity)));
    }

    /** Vero se la barra si vede: capacita' positiva e la carica (o la durata) non al massimo. */
    public static boolean visible(int stored, int capacity) {
        return capacity > 0 && stored < capacity;
    }
}
