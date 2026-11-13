package com.toxineren.exoframes.client;

import com.toxineren.exoframes.frame.SuitSize;

/**
 * Grandezza della tuta rispetto al pilota (design, par. 4.7): la tuta si disegna {@link #S} volte piu' grande
 * attorno ai piedi, e il pilota va sollevato di {@link #PILOT_RAISE_PX} pixel del modello perche' collo e spalle
 * coincidano con quelli della tuta. Solo costanti: {@link #S} viene dal codice comune ed e' una costante di
 * compilazione, quindi i test unitari la leggono senza caricare classi di Minecraft.
 */
public final class SuitScale {
    /**
     * Scala della tuta attorno ai piedi: 1,65 perche' la tuta sia alta almeno 3 blocchi (richiesta di Luigi del 13
     * novembre, prima era 1,4); fino a 1,66 il box in piedi del giocatore (1,8 S blocchi) passa sotto un soffitto a 3.
     * E' la scala verticale della grandezza reale ({@link SuitSize#HEIGHT_SCALE}, design par. 4.8): una costante sola,
     * cosi' ingombro e disegno non si separano.
     */
    public static final float S = SuitSize.HEIGHT_SCALE;

    /**
     * Sollevamento del pilota in pixel del modello: il perno di testa e busto sta a 24 px dai piedi, e scalato
     * attorno ai piedi finisce a 24 * S.
     */
    public static final float PILOT_RAISE_PX = 24f * (S - 1f);

    private SuitScale() {
    }
}
