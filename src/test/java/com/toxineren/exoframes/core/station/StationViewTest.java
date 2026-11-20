package com.toxineren.exoframes.core.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La vista del telaio che la stazione manda al client (design M4 §5.5): fatta dallo stato del record, con i pezzi, la
 * cella, le bandierine e la scelta; un valore incoerente si rifiuta, e una vista fatta non cambia piu'.
 */
class StationViewTest {

    private final ExoframesConfig cfg = ExoframesConfig.defaults();

    private Piece fresh(PieceSlot slot) {
        return Piece.fresh(slot, Tier.SCRAP, cfg.tiers);
    }

    /** Uno stato con i soli pezzi Rottame nuovi degli slot dati. */
    private FrameState mounted(PieceSlot... slots) {
        FrameState state = new FrameState(cfg);
        for (PieceSlot slot : slots) {
            state.mount(fresh(slot));
        }
        return state;
    }

    /** L'elmo consumato e dipinto del primo caso, il torso nuovo, la cella e le due bandierine dello stato. */
    private FrameState paintedHelmetAndTorso() {
        FrameState state = new FrameState(cfg);
        state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, 180, 60));
        state.setAppearance(PieceSlot.HELMET, new Appearance(0x3A6EA5, Appearance.UNPAINTED, List.of()));
        state.mount(fresh(PieceSlot.TORSO));
        state.insertCell(new EnergyCell(10_000, 7_420));
        state.setUnlocked(true);
        state.setReinforced(true);
        return state;
    }

    @Test
    void ofCopiesPiecesCellAndFlags() {
        StationView view = StationView.of(paintedHelmetAndTorso(), true, false, 0, true, false);

        assertEquals(Map.of(
                PieceSlot.HELMET, new StationView.PieceInfo(Tier.SCRAP, 60, 180, 0x3A6EA5, Appearance.UNPAINTED),
                PieceSlot.TORSO, new StationView.PieceInfo(Tier.SCRAP, 180, 180, Appearance.UNPAINTED,
                        Appearance.UNPAINTED)), view.pieces());
        assertEquals(Optional.of(new StationView.CellInfo(7_420, 10_000)), view.cell());
        assertTrue(view.unlocked());
        assertTrue(view.reinforced());
        assertTrue(view.canToggleLock());
        assertFalse(view.ownerOnly());
        assertEquals(0, view.selection());
        assertTrue(view.hasStowed());
        assertFalse(view.free());

        StationView other = StationView.of(mounted(PieceSlot.TORSO), false, true, 1, false, true);
        assertEquals(Optional.empty(), other.cell());
        assertFalse(other.unlocked());
        assertFalse(other.reinforced());
        assertFalse(other.canToggleLock());
        assertTrue(other.ownerOnly());
        assertEquals(1, other.selection());
        assertFalse(other.hasStowed());
        assertTrue(other.free());
    }

    @Test
    void unpaintedColoursStayUnpainted() {
        FrameState state = mounted(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_ARM);
        state.setAppearance(PieceSlot.RIGHT_ARM, new Appearance(Appearance.UNPAINTED, 0x000000, List.of()));
        StationView view = StationView.of(state, false, true, 2, false, false);

        StationView.PieceInfo leg = view.pieces().get(PieceSlot.LEFT_LEG);
        assertEquals(Appearance.UNPAINTED, leg.primary());
        assertEquals(Appearance.UNPAINTED, leg.secondary());
        assertEquals(-1, leg.primary());
        StationView.PieceInfo arm = view.pieces().get(PieceSlot.RIGHT_ARM);
        assertEquals(Appearance.UNPAINTED, arm.primary());
        assertEquals(0x000000, arm.secondary());
    }

    @Test
    void initialSelectionIsTheFirstMountedPieceOrTheHelmet() {
        assertEquals(0, StationView.initialSelection(mounted()));
        assertEquals(1, StationView.initialSelection(mounted(PieceSlot.TORSO)));
        assertEquals(2, StationView.initialSelection(mounted(PieceSlot.RIGHT_ARM, PieceSlot.LEFT_ARM)));
        assertEquals(0, StationView.initialSelection(mounted(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG,
                PieceSlot.HELMET)));
    }

    @Test
    void selectionNumbersFollowTheButtonTable() {
        assertEquals(0, StationView.selectionOf(PieceSlot.HELMET));
        assertEquals(1, StationView.selectionOf(PieceSlot.TORSO));
        assertEquals(2, StationView.selectionOf(PieceSlot.LEFT_ARM));
        assertEquals(3, StationView.selectionOf(PieceSlot.RIGHT_ARM));
        assertEquals(4, StationView.selectionOf(PieceSlot.LEFT_LEG));
        assertEquals(5, StationView.selectionOf(PieceSlot.RIGHT_LEG));
        assertEquals(6, StationView.SELECT_CELL);
        assertEquals(7, StationView.SELECT_ALL);
    }

    @Test
    void constructorRejectsInconsistentValues() {
        Map<PieceSlot, StationView.PieceInfo> none = Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> new StationView(none, Optional.empty(), false, false, false, false, 8, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView(none, Optional.empty(), false, false, false, false, -1, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView.PieceInfo(Tier.SCRAP, 181, 180, Appearance.UNPAINTED, Appearance.UNPAINTED));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView.PieceInfo(Tier.SCRAP, -1, 180, Appearance.UNPAINTED, Appearance.UNPAINTED));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView.PieceInfo(Tier.SCRAP, 0, 0, Appearance.UNPAINTED, Appearance.UNPAINTED));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView.PieceInfo(Tier.SCRAP, 90, 180, 0x1000000, Appearance.UNPAINTED));
        assertThrows(IllegalArgumentException.class,
                () -> new StationView.PieceInfo(Tier.SCRAP, 90, 180, Appearance.UNPAINTED, -2));
        assertThrows(IllegalArgumentException.class, () -> new StationView.CellInfo(-1, 10_000));
        assertThrows(IllegalArgumentException.class, () -> new StationView.CellInfo(0, 0));
        assertThrows(NullPointerException.class,
                () -> new StationView(null, Optional.empty(), false, false, false, false, 0, false, false));
        assertThrows(NullPointerException.class,
                () -> new StationView(none, null, false, false, false, false, 0, false, false));
        Map<PieceSlot, StationView.PieceInfo> nullInfo = new EnumMap<>(PieceSlot.class);
        nullInfo.put(PieceSlot.HELMET, null);
        assertThrows(NullPointerException.class,
                () -> new StationView(nullInfo, Optional.empty(), false, false, false, false, 0, false, false));
        assertThrows(NullPointerException.class,
                () -> new StationView.PieceInfo(null, 90, 180, Appearance.UNPAINTED, Appearance.UNPAINTED));
    }

    @Test
    void equalContentMakesEqualViews() {
        StationView first = StationView.of(paintedHelmetAndTorso(), true, true, 0, false, false);
        StationView second = StationView.of(paintedHelmetAndTorso(), true, true, 0, false, false);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        FrameState worn = paintedHelmetAndTorso();
        worn.pieces().get(PieceSlot.TORSO).damage(1);
        assertNotEquals(first, StationView.of(worn, true, true, 0, false, false));

        Map<PieceSlot, StationView.PieceInfo> pieces = first.pieces();
        StationView.PieceInfo torso = pieces.get(PieceSlot.TORSO);
        assertThrows(UnsupportedOperationException.class, () -> pieces.put(PieceSlot.LEFT_ARM, torso));
        assertThrows(UnsupportedOperationException.class, () -> pieces.remove(PieceSlot.TORSO));

        FrameState state = paintedHelmetAndTorso();
        StationView made = StationView.of(state, true, true, 0, false, false);
        state.pieces().get(PieceSlot.HELMET).damage(10);
        state.unmount(PieceSlot.TORSO);
        state.removeCell();
        state.setUnlocked(false);
        assertEquals(first, made);

        Map<PieceSlot, StationView.PieceInfo> source = new EnumMap<>(PieceSlot.class);
        source.put(PieceSlot.HELMET, torso);
        StationView fromSource = new StationView(source, Optional.empty(), false, false, false, false, 0, false,
                false);
        source.put(PieceSlot.TORSO, torso);
        assertEquals(Map.of(PieceSlot.HELMET, torso), fromSource.pieces());
    }
}
