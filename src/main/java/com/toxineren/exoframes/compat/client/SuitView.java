package com.toxineren.exoframes.compat.client;

import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.core.armor.Appearance;
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
     * La vista di un riassunto visivo: lo stato visivo e' la posizione della condizione ({@code INTACT} 0,
     * {@code DAMAGED} 1, {@code CRITICAL} 2, {@code BROKEN} 3); un colore {@link Appearance#UNPAINTED} prende il
     * colore predefinito del livello del pezzo dalla funzione della sua regione.
     */
    public static SuitView of(SuitSummary summary, ToIntFunction<Tier> primaryDefault,
            ToIntFunction<Tier> secondaryDefault) {
        Map<PieceSlot, PieceView> pieces = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, PieceLook> entry : summary.pieces().entrySet()) {
            PieceLook look = entry.getValue();
            int state = switch (look.condition()) {
                case INTACT -> 0;
                case DAMAGED -> 1;
                case CRITICAL -> 2;
                case BROKEN -> 3;
            };
            int primary = resolve(look.appearance().primary(), look.tier(), primaryDefault);
            int secondary = resolve(look.appearance().secondary(), look.tier(), secondaryDefault);
            pieces.put(entry.getKey(), new PieceView(state, primary, secondary));
        }
        return new SuitView(pieces);
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
