package com.toxineren.exoframes.core.station;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.Objects;
import java.util.Optional;

/**
 * Un'azione della stazione di manutenzione: il numero di un pulsante del menu del gioco base decodificato (design M4
 * §5.6). Le azioni viaggiano tutte con {@code clickMenuButton}, e il numero dice il tipo ({@link Kind}) e l'argomento
 * ({@code arg}): lo slot nell'ordine di {@link PieceSlot}, la tinta nell'ordine di {@code DyeColor} o il preset (0-2
 * per i preset 1-3). La tabella:
 * <table>
 * <caption>I numeri dei pulsanti</caption>
 * <tr><th>Numeri</th><th>Azione</th></tr>
 * <tr><td>0-5</td><td>sceglie il pezzo dello slot (elmo, torso, braccio sinistro, braccio destro, gamba sinistra,
 * gamba destra)</td></tr>
 * <tr><td>6</td><td>sceglie la cella</td></tr>
 * <tr><td>7</td><td>sceglie tutti i pezzi montati</td></tr>
 * <tr><td>8-13</td><td>monta nello slot il pezzo che il giocatore ha sul cursore</td></tr>
 * <tr><td>14</td><td>monta la cella che il giocatore ha sul cursore (quella montata, se c'e', va
 * all'inventario)</td></tr>
 * <tr><td>16-21</td><td>smonta il pezzo dello slot nell'inventario</td></tr>
 * <tr><td>22</td><td>smonta la cella nell'inventario</td></tr>
 * <tr><td>24</td><td>ripara di un'unita' la scelta</td></tr>
 * <tr><td>25</td><td>ripara la scelta fin dove si arriva</td></tr>
 * <tr><td>26</td><td>blocca o sblocca il telaio</td></tr>
 * <tr><td>32-47</td><td>primario della scelta con la tinta 0-15</td></tr>
 * <tr><td>48</td><td>primario della scelta al colore del livello</td></tr>
 * <tr><td>64-79</td><td>secondario della scelta con la tinta 0-15</td></tr>
 * <tr><td>80</td><td>secondario della scelta al colore del livello</td></tr>
 * <tr><td>96-98</td><td>applica il preset 1-3</td></tr>
 * <tr><td>100-102</td><td>salva nel preset 1-3</td></tr>
 * </table>
 * I numeri della parte 4b (riparare, verniciare, i preset) si decodificano gia': chi li riceve prima che abbiano un
 * gestore li ignora. Un numero che non c'e' (un buco della tabella, un negativo, uno oltre {@link #HIGHEST}) non e'
 * nessuna azione: {@link #decode} da' vuoto. Il numero piu' alto, {@link #HIGHEST}, sta nel byte con cui il pacchetto
 * dei pulsanti di 1.20.1 scrive il numero.
 *
 * @param kind il tipo dell'azione
 * @param arg  l'argomento, fra 0 e {@code kind.count()} escluso
 */
public record StationAction(Kind kind, int arg) {
    /** Il numero piu' alto della tabella: "salva" del preset 3. */
    public static final int HIGHEST = 102;

    /** I tipi delle azioni, ciascuno con il suo primo numero e quanti numeri ha di fila. */
    public enum Kind {
        SELECT_PIECE(0, 6),
        SELECT_CELL(6, 1),
        SELECT_ALL(7, 1),
        MOUNT_PIECE(8, 6),
        MOUNT_CELL(14, 1),
        UNMOUNT_PIECE(16, 6),
        UNMOUNT_CELL(22, 1),
        REPAIR_ONE(24, 1),
        REPAIR_ALL(25, 1),
        TOGGLE_LOCK(26, 1),
        PAINT_PRIMARY(32, 16),
        PRIMARY_TIER_COLOUR(48, 1),
        PAINT_SECONDARY(64, 16),
        SECONDARY_TIER_COLOUR(80, 1),
        APPLY_PRESET(96, 3),
        SAVE_PRESET(100, 3);

        private final int first;
        private final int count;

        Kind(int first, int count) {
            this.first = first;
            this.count = count;
        }

        /** Il primo numero del tipo: quello dell'argomento 0. */
        public int first() {
            return first;
        }

        /** Quanti numeri ha il tipo, uno per argomento. */
        public int count() {
            return count;
        }
    }

    /**
     * @throws NullPointerException     se il tipo e' null
     * @throws IllegalArgumentException se l'argomento non sta fra 0 e {@code kind.count()} escluso
     */
    public StationAction {
        Objects.requireNonNull(kind, "kind must not be null");
        if (arg < 0 || arg >= kind.count()) {
            throw new IllegalArgumentException("argument of " + kind + " must be in [0, " + kind.count() + "): "
                    + arg);
        }
    }

    /** L'azione del numero di un pulsante; vuoto per un numero che non e' nella tabella. */
    public static Optional<StationAction> decode(int button) {
        for (Kind kind : Kind.values()) {
            if (button >= kind.first() && button - kind.first() < kind.count()) {
                return Optional.of(new StationAction(kind, button - kind.first()));
            }
        }
        return Optional.empty();
    }

    /** Il numero del pulsante di questa azione: il primo del tipo piu' l'argomento. */
    public int button() {
        return kind.first() + arg;
    }

    /**
     * Il numero del pulsante dell'azione con il tipo e l'argomento dati.
     *
     * @throws NullPointerException     se il tipo e' null
     * @throws IllegalArgumentException se l'argomento non sta fra 0 e {@code kind.count()} escluso
     */
    public static int button(Kind kind, int arg) {
        return new StationAction(kind, arg).button();
    }

    /**
     * Vero se l'azione cambia il telaio, e sul server ha quindi il limite di una per tick per menu; falso solo per le
     * tre scelte, che restano libere.
     */
    public boolean writes() {
        return kind != Kind.SELECT_PIECE && kind != Kind.SELECT_CELL && kind != Kind.SELECT_ALL;
    }

    /**
     * Lo slot di un'azione su un pezzo (scegliere, montare, smontare): quello dell'argomento, nell'ordine di
     * {@link PieceSlot}.
     *
     * @throws IllegalStateException per un'azione che non e' su un pezzo
     */
    public PieceSlot slot() {
        return switch (kind) {
            case SELECT_PIECE, MOUNT_PIECE, UNMOUNT_PIECE -> PieceSlot.values()[arg];
            default -> throw new IllegalStateException(kind + " has no piece slot");
        };
    }
}
