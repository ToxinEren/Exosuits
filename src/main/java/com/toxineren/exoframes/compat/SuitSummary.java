package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Riassunto solo visivo di un telaio indossato: per ogni slot con un pezzo montato, livello, stato visivo e aspetto.
 * Sta nei dati del segnaposto del torso (chiave {@link NbtKeys#VISUAL}, vedi {@link ItemDataAccess#suitSummary}),
 * cosi' la sincronizzazione vanilla degli slot armatura lo porta a ogni client che vede il giocatore; il layer di
 * rendering lo legge. Non porta durabilita', moduli ne' energia: lo stato completo resta nel {@link FrameState}.
 * <p>
 * La mappa e' una copia non modificabile, ordinata per slot; uno slot assente e' uno slot senza pezzo.
 */
public record SuitSummary(Map<PieceSlot, PieceLook> pieces) {

    /** Telaio nudo: nessun pezzo montato. */
    public static final SuitSummary BARE = new SuitSummary(Map.of());

    public SuitSummary {
        Objects.requireNonNull(pieces, "pieces must not be null");
        Map<PieceSlot, PieceLook> copy = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, PieceLook> entry : pieces.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "slot must not be null"),
                    Objects.requireNonNull(entry.getValue(), "piece look must not be null"));
        }
        pieces = Collections.unmodifiableMap(copy);
    }

    /** Il riassunto dei pezzi montati nello stato: livello e condizione del pezzo, aspetto dello slot. */
    public static SuitSummary of(FrameState state) {
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, Piece> entry : state.pieces().entrySet()) {
            PieceSlot slot = entry.getKey();
            Piece piece = entry.getValue();
            pieces.put(slot, new PieceLook(piece.tier(), piece.condition(), state.appearance(slot)));
        }
        return new SuitSummary(pieces);
    }

    /** Aspetto di un pezzo nel riassunto: livello, stato visivo e aspetto; nessun componente puo' essere null. */
    public record PieceLook(Tier tier, PieceCondition condition, Appearance appearance) {
        public PieceLook {
            Objects.requireNonNull(tier, "tier must not be null");
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(appearance, "appearance must not be null");
        }
    }
}
