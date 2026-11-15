package com.toxineren.exoframes.client;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Optional;

/**
 * Il contenuto della riga dell'energia (design §12.13), senza classi del gioco: RE immagazzinati, capacita',
 * percentuale per difetto e il pezzo messo peggio fra quelli {@code BROKEN} o {@code CRITICAL}. {@link EnergyHud} lo
 * trasforma in testo; i test JUnit lo provano da solo.
 *
 * @param stored         RE nella cella
 * @param capacity       capacita' della cella
 * @param percent        {@code stored * 100 / capacity}, per difetto
 * @param worst          lo slot del pezzo messo peggio, vuoto se nessun pezzo e' critico o rotto
 * @param worstCondition la sua condizione ({@code BROKEN} o {@code CRITICAL}), vuota insieme a {@code worst}
 */
record EnergyLine(int stored, int capacity, int percent, Optional<PieceSlot> worst,
        Optional<PieceCondition> worstCondition) {

    /**
     * La riga dello stato dato, vuota se il telaio non ha una cella. Il pezzo messo peggio: un {@code BROKEN} vince
     * su ogni {@code CRITICAL}; fra pezzi della stessa condizione la durata relativa piu' bassa (confronto in interi,
     * senza virgola mobile); a parita' il primo nell'ordine di {@link PieceSlot}.
     */
    static Optional<EnergyLine> of(FrameState state) {
        EnergyCell cell = state.cell();
        if (cell == null) {
            return Optional.empty();
        }
        int percent = (int) (cell.stored() * 100L / cell.capacity());
        Piece worst = null;
        for (PieceSlot slot : PieceSlot.values()) {
            Piece piece = state.pieces().get(slot);
            if (piece != null && isWorse(piece, worst)) {
                worst = piece;
            }
        }
        if (worst == null) {
            return Optional.of(new EnergyLine(cell.stored(), cell.capacity(), percent, Optional.empty(),
                    Optional.empty()));
        }
        return Optional.of(new EnergyLine(cell.stored(), cell.capacity(), percent, Optional.of(worst.slot()),
                Optional.of(worst.condition())));
    }

    /**
     * Vero se {@code a} e' critico o rotto e messo strettamente peggio di {@code b} (o {@code b} manca): il confronto
     * stretto lascia vincere, a parita', lo slot visto prima.
     */
    private static boolean isWorse(Piece a, Piece b) {
        PieceCondition ca = a.condition();
        if (ca != PieceCondition.BROKEN && ca != PieceCondition.CRITICAL) {
            return false;
        }
        if (b == null) {
            return true;
        }
        PieceCondition cb = b.condition();
        if (ca != cb) {
            return ca == PieceCondition.BROKEN;
        }
        return a.durability() * (long) b.maxDurability() < b.durability() * (long) a.maxDurability();
    }
}
