package com.toxineren.exoframes.core.frame;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.Objects;
import java.util.Optional;

/**
 * Il livello dell'HUD della tuta (design §5.12, "Livelli"): che cosa vede chi la indossa. Il client lo calcola dalla
 * propria copia dello stato del telaio, che e' del server: non e' una soglia del client (DEC-23). M5 lo riusera' per i
 * visori ("elmo rotto: niente visori"). F1, il tasto H, lo spettatore e la terza persona non sono livelli: li guarda
 * SuitHud al momento del disegno.
 */
public enum HudLevel {
    /** Un elmo montato che funziona: la sagoma dei pezzi, la barra dell'energia e, in prima persona, la vignetta. */
    FULL,
    /**
     * La tuta senza un elmo che funziona (nessun elmo, l'elmo rotto, il telaio nudo): la barra con la percentuale, le
     * celle di scorta e l'avviso, sulla loro piastra.
     */
    BAR_ONLY,
    /** Niente da disegnare: il giocatore non indossa la tuta, o il client non ha la sua copia dello stato. */
    NONE;

    /**
     * Il livello per chi indossa la tuta ({@code wearsSuit}, da SuitSize.wearsSuit) con la copia dello stato data (da
     * clientView, vuota se il client non l'ha ancora). {@link #NONE} senza la tuta o senza la copia; altrimenti
     * {@link #FULL} se l'elmo e' montato e non e' rotto (un elmo critico funziona ancora), {@link #BAR_ONLY} in tutti
     * gli altri casi. La cella non conta.
     */
    public static HudLevel of(boolean wearsSuit, Optional<FrameState> view) {
        Objects.requireNonNull(view, "view");
        if (!wearsSuit || view.isEmpty()) {
            return NONE;
        }
        Piece helmet = view.get().pieces().get(PieceSlot.HELMET);
        return helmet != null && helmet.condition() != PieceCondition.BROKEN ? FULL : BAR_ONLY;
    }
}
