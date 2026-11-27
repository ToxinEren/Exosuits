package com.toxineren.exoframes.compat.client;

import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Cio' che il {@link SuitRenderer} disegna, senza tipi di versione: per ogni slot con un pezzo montato lo stato
 * visivo e i due colori gia' risolti. Uno slot assente e' uno slot senza pezzo; la mappa vuota e' il telaio nudo.
 * La mappa e' una copia non modificabile, ordinata per slot.
 */
public record SuitView(Map<PieceSlot, PieceView> pieces) {

    public SuitView {
        Objects.requireNonNull(pieces, "pieces must not be null");
        Map<PieceSlot, PieceView> copy = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, PieceView> entry : pieces.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "slot must not be null"),
                    Objects.requireNonNull(entry.getValue(), "piece view must not be null"));
        }
        pieces = Collections.unmodifiableMap(copy);
    }

    /**
     * La vista di un riassunto visivo: lo stato visivo e' quello della condizione ({@link #visualState}); un colore
     * {@link Appearance#UNPAINTED} prende il colore predefinito del livello del pezzo dalla funzione della sua regione.
     */
    public static SuitView of(SuitSummary summary, ToIntFunction<Tier> primaryDefault,
            ToIntFunction<Tier> secondaryDefault) {
        Map<PieceSlot, PieceView> pieces = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, PieceLook> entry : summary.pieces().entrySet()) {
            PieceLook look = entry.getValue();
            int state = visualState(look.condition());
            int primary = resolve(look.appearance().primary(), look.tier(), primaryDefault);
            int secondary = resolve(look.appearance().secondary(), look.tier(), secondaryDefault);
            pieces.put(entry.getKey(), new PieceView(state, primary, secondary));
        }
        return new SuitView(pieces);
    }

    /**
     * Lo stato visivo di un pezzo nella sua condizione, quello che {@link SuitRenderer} disegna: integro 0,
     * danneggiato 1, critico 2, rotto 3. L'unica corrispondenza fra condizione e stato visivo, per il telaio nel mondo
     * ({@link #of}) e per l'anteprima della stazione.
     */
    public static int visualState(PieceCondition condition) {
        return switch (Objects.requireNonNull(condition, "condition must not be null")) {
            case INTACT -> 0;
            case DAMAGED -> 1;
            case CRITICAL -> 2;
            case BROKEN -> 3;
        };
    }

    private static int resolve(int color, Tier tier, ToIntFunction<Tier> fallback) {
        return color == Appearance.UNPAINTED ? fallback.applyAsInt(tier) : color;
    }

    /**
     * Un pezzo da disegnare: stato visivo in [0, 3] e colori RGB delle regioni primaria e secondaria. Dei colori
     * restano solo i 24 bit RGB (un eventuale canale alfa si scarta), quindi non valgono mai -1.
     */
    public record PieceView(int state, int primaryRgb, int secondaryRgb) {
        public PieceView {
            if (state < 0 || state > 3) {
                throw new IllegalArgumentException("piece state must be in [0, 3]: " + state);
            }
            primaryRgb &= 0xFFFFFF;
            secondaryRgb &= 0xFFFFFF;
        }
    }
}
