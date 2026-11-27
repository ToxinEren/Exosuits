package com.toxineren.exoframes.core.station;

import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * La vista di un telaio piazzato per chi lo guarda dalla stazione di manutenzione (design M4 §5.5): solo cio' che la
 * schermata mostra, fatta sul server dallo stato del record e mandata al client nei dati extra dell'apertura e, quando
 * cambia, con il pacchetto {@code station_view}. Il client non valuta regole con la propria config: anche
 * {@code ownerOnly} e {@code free} arrivano gia' decisi dal server.
 * <p>
 * Per ogni slot montato il livello, la durata, la durata massima e i due colori del pezzo, che restano
 * {@link Appearance#UNPAINTED} se il pezzo non e' dipinto (il colore del livello lo mette il client); la cella, se c'e'
 * (carica e capacita'); se il telaio e' sbloccato ({@code unlocked}) e rinforzato ({@code reinforced}); se il giocatore
 * puo' cambiarne il blocco ({@code canToggleLock}, la regola di {@code /exoframe lock|unlock}); se la regola
 * {@code server.ownerOnlyFrames} e' accesa ({@code ownerOnly}); la scelta del menu ({@code selection}: 0-5 lo slot
 * nell'ordine di {@link PieceSlot}, {@link #SELECT_CELL} la cella, {@link #SELECT_ALL} tutti i pezzi); se il telaio
 * custodisce un'armatura ({@code hasStowed}, solo il si' o il no, mai che cosa); se il giocatore non paga
 * ({@code free}, la capacita' {@code instabuild}). Non ci sono il proprietario, i moduli, i toggle, i riporti del
 * danno ne' l'armatura stivata. I punti di riparazione, i costi e i preset arrivano con la parte 4b, che fa crescere il
 * record e il codec (nessun formato salvato: cambia solo il pacchetto).
 * <p>
 * Il record e' immutabile: il costruttore copia la mappa dei pezzi in una mappa non modificabile, quindi una vista
 * fatta non cambia se cambia lo stato da cui e' nata. Due viste con lo stesso contenuto sono uguali
 * ({@code equals}), e il server rimanda la vista solo quando non e' uguale all'ultima mandata.
 *
 * @throws NullPointerException se la mappa, una sua chiave o un suo valore, o la cella sono null
 * @throws IllegalArgumentException se la scelta non sta fra 0 e {@link #SELECT_ALL}
 */
public record StationView(Map<PieceSlot, PieceInfo> pieces, Optional<CellInfo> cell, boolean unlocked,
        boolean reinforced, boolean canToggleLock, boolean ownerOnly, int selection, boolean hasStowed,
        boolean free) {
    /** La scelta della cella, dopo i sei slot. */
    public static final int SELECT_CELL = 6;
    /** La scelta di tutti i pezzi, l'ultima. */
    public static final int SELECT_ALL = 7;

    public StationView {
        Objects.requireNonNull(pieces, "pieces must not be null");
        Objects.requireNonNull(cell, "cell must not be null");
        Map<PieceSlot, PieceInfo> copy = new EnumMap<>(PieceSlot.class);
        pieces.forEach((slot, info) -> copy.put(Objects.requireNonNull(slot, "piece slot must not be null"),
                Objects.requireNonNull(info, "piece info must not be null")));
        pieces = Collections.unmodifiableMap(copy);
        if (selection < 0 || selection > SELECT_ALL) {
            throw new IllegalArgumentException("selection must be in [0, " + SELECT_ALL + "]: " + selection);
        }
    }

    /**
     * Un pezzo montato: livello, durata (fra 0 e {@code maxDurability}), durata massima (positiva) e i due colori, RGB
     * in [0, 0xFFFFFF] oppure {@link Appearance#UNPAINTED}.
     *
     * @throws NullPointerException se il livello e' null
     * @throws IllegalArgumentException se un valore non e' coerente
     */
    public record PieceInfo(Tier tier, int durability, int maxDurability, int primary, int secondary) {
        public PieceInfo {
            Objects.requireNonNull(tier, "tier must not be null");
            if (maxDurability <= 0) {
                throw new IllegalArgumentException("maxDurability must be > 0: " + maxDurability);
            }
            if (durability < 0 || durability > maxDurability) {
                throw new IllegalArgumentException("durability must be in [0, " + maxDurability + "]: " + durability);
            }
            checkColor("primary", primary);
            checkColor("secondary", secondary);
        }

        private static void checkColor(String name, int color) {
            if (color != Appearance.UNPAINTED && (color < 0 || color > 0xFFFFFF)) {
                throw new IllegalArgumentException(name + " color must be UNPAINTED or in [0, 0xFFFFFF]: " + color);
            }
        }
    }

    /**
     * La cella del telaio: la carica (non negativa) e la capacita' (positiva), in RE.
     *
     * @throws IllegalArgumentException se un valore non e' coerente
     */
    public record CellInfo(int stored, int capacity) {
        public CellInfo {
            if (stored < 0) {
                throw new IllegalArgumentException("stored must be >= 0: " + stored);
            }
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be > 0: " + capacity);
            }
        }
    }

    /**
     * La vista dello stato dato, con le bandierine e la scelta che decide chi chiama (il server). Lo stato si legge
     * adesso: i suoi cambi successivi non toccano la vista.
     *
     * @throws IllegalArgumentException se la scelta non sta fra 0 e {@link #SELECT_ALL}
     */
    public static StationView of(FrameState state, boolean canToggleLock, boolean ownerOnly, int selection,
            boolean hasStowed, boolean free) {
        Objects.requireNonNull(state, "state must not be null");
        Map<PieceSlot, PieceInfo> pieces = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, Piece> entry : state.pieces().entrySet()) {
            Piece piece = entry.getValue();
            Appearance look = state.appearance(entry.getKey());
            pieces.put(entry.getKey(), new PieceInfo(piece.tier(), piece.durability(), piece.maxDurability(),
                    look.primary(), look.secondary()));
        }
        EnergyCell cell = state.cell();
        Optional<CellInfo> cellInfo = cell == null ? Optional.empty()
                : Optional.of(new CellInfo(cell.stored(), cell.capacity()));
        return new StationView(pieces, cellInfo, state.unlocked(), state.reinforced(), canToggleLock, ownerOnly,
                selection, hasStowed, free);
    }

    /**
     * La stessa vista con un'altra scelta: la predizione del client quando sceglie (design §5.6), o il server che rifa'
     * la vista dopo una scelta. Questa vista non cambia.
     *
     * @throws IllegalArgumentException se la scelta non sta fra 0 e {@link #SELECT_ALL}
     */
    public StationView withSelection(int selection) {
        return new StationView(pieces, cell, unlocked, reinforced, canToggleLock, ownerOnly, selection, hasStowed,
                free);
    }

    /** Il numero della scelta dello slot: 0-5, nell'ordine di {@link PieceSlot} (la tabella dei pulsanti, §5.6). */
    public static int selectionOf(PieceSlot slot) {
        return Objects.requireNonNull(slot, "slot must not be null").ordinal();
    }

    /**
     * La scelta con cui la stazione si apre (design §5.5): il primo pezzo montato nell'ordine di {@link PieceSlot},
     * altrimenti l'elmo.
     */
    public static int initialSelection(FrameState state) {
        Objects.requireNonNull(state, "state must not be null");
        for (PieceSlot slot : PieceSlot.values()) {
            if (state.pieces().containsKey(slot)) {
                return selectionOf(slot);
            }
        }
        return selectionOf(PieceSlot.HELMET);
    }
}
