package com.toxineren.exoframes.client;

/**
 * Grandezza della tuta rispetto al pilota (design, par. 4.7): la tuta si disegna {@link #S} volte piu' grande
 * attorno ai piedi, e il pilota va sollevato di {@link #PILOT_RAISE_PX} pixel del modello perche' collo e spalle
 * coincidano con quelli della tuta. Java puro, senza classi di Minecraft.
 */
public final class SuitScale {
    /** Scala della tuta attorno ai piedi (scelta di Luigi, 13 novembre). */
    public static final float S = 1.4f;

    /**
     * Sollevamento del pilota in pixel del modello: il perno di testa e busto sta a 24 px dai piedi, e scalato
     * attorno ai piedi finisce a 24 * S.
     */
    public static final float PILOT_RAISE_PX = 24f * (S - 1f);

    private SuitScale() {
    }

    /** Il sollevamento del pilota in blocchi: il modello del giocatore e' reso a 15/16 (0,9375). */
    public static float pilotRaiseBlocks() {
        return PILOT_RAISE_PX * 0.9375f / 16f;
    }
}
