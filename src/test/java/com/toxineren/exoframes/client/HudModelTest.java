package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.HudModel.Paint;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.frame.HudLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Che cosa disegna l'HUD ({@link HudModel}, design §5.12): percentuale e riempimento per difetto, il colore
 * dell'energia dallo stato del server, l'avviso che lampeggia dal tick del cambio, la sagoma con la legenda, che e'
 * anche quella della stazione, le celle di scorta, le righe della barra e i contorni, e nessun colore magenta per i
 * controlli degli scatti.
 */
class HudModelTest {

    private static final Optional<EnergyState> NORMAL = Optional.of(EnergyState.NORMAL);
    private static final Optional<EnergyState> LOW = Optional.of(EnergyState.LOW);
    private static final Optional<EnergyState> EMPTY = Optional.of(EnergyState.EMPTY);
    /** Prima del primo pacchetto con gli indicatori. */
    private static final Optional<EnergyState> NO_INDICATORS = Optional.empty();
    /** La durata massima del Rottame nella config predefinita. */
    private static final int SCRAP_MAX = 180;

    @Test
    void percentRoundsDownAndTheBarFollowsTheCharge() {
        HudModel model = HudModel.of(HudLevel.FULL, withCell(10_000, 7_429), NORMAL, 0, 0);
        assertTrue(model.hasCell());
        assertEquals(74, model.percent());
        assertEquals(45, model.fillWidth());
        assertCharge(10_000, 10_000, 100, 61);
        assertCharge(10_000, 0, 0, 0);
        assertCharge(10_000, 1, 0, 0);
        assertCharge(20_000, 19_999, 99, 60);
        // il costruttore tiene la percentuale in [0, 100] e il riempimento in [0, FILL_MAX]
        assertThrows(IllegalArgumentException.class, () -> charge(101, 0));
        assertThrows(IllegalArgumentException.class, () -> charge(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> charge(100, HudModel.FILL_MAX + 1));
        assertThrows(IllegalArgumentException.class, () -> charge(0, -1));
    }

    @Test
    void energyColourFollowsTheServerState() {
        assertEquals(0xFF55FF55, HudModel.energyColor(NORMAL));
        assertEquals(0xFFFF5555, HudModel.energyColor(LOW));
        assertEquals(0xFF8A8A8A, HudModel.energyColor(EMPTY));
        assertEquals(0xFFFFFFFF, HudModel.energyColor(NO_INDICATORS));
        assertEquals(0xFFFF5555, HudModel.of(HudLevel.FULL, withCell(10_000, 5_000), LOW, 0, 0).energyColor());
    }

    @Test
    void lowWarningBlinksEveryHalfSecondFromTheChange() {
        for (long t = 0; t <= 9; t++) {
            assertTrue(HudModel.warningOn(LOW, t), "LOW after " + t + " ticks");
        }
        for (long t = 10; t <= 19; t++) {
            assertFalse(HudModel.warningOn(LOW, t), "LOW after " + t + " ticks");
        }
        assertTrue(HudModel.warningOn(LOW, 20));
        assertFalse(HudModel.warningOn(LOW, 39));
        for (long t : new long[] {0, 10, 15}) {
            assertTrue(HudModel.warningOn(EMPTY, t), "EMPTY after " + t + " ticks");
            assertFalse(HudModel.warningOn(NORMAL, t), "NORMAL after " + t + " ticks");
            assertFalse(HudModel.warningOn(NO_INDICATORS, t), "no indicators after " + t + " ticks");
        }

        FrameState state = withCell(10_000, 500);
        // nel mezzo secondo spento il posto dell'avviso resta (Decisione 5 del piano)
        HudModel low = HudModel.of(HudLevel.FULL, state, LOW, 0, 15);
        assertTrue(low.warningSlot());
        assertFalse(low.warningOn());
        HudModel empty = HudModel.of(HudLevel.FULL, state, EMPTY, 0, 15);
        assertTrue(empty.warningSlot());
        assertTrue(empty.warningOn());
        for (Optional<EnergyState> energy : List.of(NORMAL, NO_INDICATORS)) {
            HudModel model = HudModel.of(HudLevel.FULL, state, energy, 0, 15);
            assertFalse(model.warningSlot(), "warning slot with " + energy);
            assertFalse(model.warningOn(), "warning with " + energy);
        }
    }

    @Test
    void blinkRestartsWhenTheStateChanges() {
        assertEquals(0, HudModel.ticksInState(NO_INDICATORS, LOW, 99));
        assertEquals(1, HudModel.ticksInState(LOW, LOW, 0));
        assertEquals(8, HudModel.ticksInState(LOW, LOW, 7));
        assertEquals(0, HudModel.ticksInState(LOW, NORMAL, 30));
        assertEquals(0, HudModel.ticksInState(NORMAL, LOW, 30));
        assertEquals(0, HudModel.ticksInState(LOW, EMPTY, 5));
        assertEquals(5, HudModel.ticksInState(NO_INDICATORS, NO_INDICATORS, 4));
    }

    @Test
    void silhouetteColoursFollowTheLegend() {
        FrameState state = withCell(10_000, 5_000);
        state.mount(piece(PieceSlot.HELMET, 180));
        state.mount(piece(PieceSlot.TORSO, 90));
        state.mount(piece(PieceSlot.LEFT_ARM, 26));
        state.mount(piece(PieceSlot.RIGHT_ARM, 0));
        Map<PieceSlot, Paint> silhouette = HudModel.of(HudLevel.FULL, state, NORMAL, 0, 0).silhouette();

        assertEquals(List.of(PieceSlot.values()), new ArrayList<>(silhouette.keySet()), "the six slots");
        assertEquals(new Paint(0xFF55FF55, 0xFF1C551C), silhouette.get(PieceSlot.HELMET), "intact helmet");
        assertEquals(new Paint(0xFFFFFF55, 0xFF55551C), silhouette.get(PieceSlot.TORSO), "damaged torso");
        assertEquals(new Paint(0xFFFF5555, 0xFF551C1C), silhouette.get(PieceSlot.LEFT_ARM), "critical left arm");
        assertEquals(new Paint(0xFF8A8A8A, 0xFF2E2E2E), silhouette.get(PieceSlot.RIGHT_ARM), "broken right arm");
        assertEquals(new Paint(0x5A000000, 0xFF464646), silhouette.get(PieceSlot.LEFT_LEG), "unmounted left leg");
        assertEquals(new Paint(0x5A000000, 0xFF464646), silhouette.get(PieceSlot.RIGHT_LEG), "unmounted right leg");
        assertThrows(UnsupportedOperationException.class, () -> silhouette.remove(PieceSlot.HELMET));

        assertEquals(0xFF55FF55, HudModel.conditionColor(PieceCondition.INTACT));
        assertEquals(0xFFFFFF55, HudModel.conditionColor(PieceCondition.DAMAGED));
        assertEquals(0xFFFF5555, HudModel.conditionColor(PieceCondition.CRITICAL));
        assertEquals(0xFF8A8A8A, HudModel.conditionColor(PieceCondition.BROKEN));
        // una legenda sola: la stazione prende la sua da qui (Decisione 3 del piano)
        for (PieceCondition condition : PieceCondition.values()) {
            assertEquals(HudModel.conditionColor(condition), StationLayout.conditionColor(condition),
                    "the station's colour of " + condition);
        }
    }

    @Test
    void barOnlyCarriesNoSilhouette() {
        FrameState state = withCell(10_000, 7_429);
        state.mount(piece(PieceSlot.HELMET, 0));
        state.mount(piece(PieceSlot.TORSO, 180));
        HudModel model = HudModel.of(HudLevel.BAR_ONLY, state, NORMAL, 2, 0);
        assertEquals(HudLevel.BAR_ONLY, model.level());
        assertTrue(model.silhouette().isEmpty(), "silhouette " + model.silhouette());
        assertEquals(74, model.percent());
        assertEquals(45, model.fillWidth());
        assertEquals(2, model.spareCells());

        assertSame(HudModel.NONE, HudModel.of(HudLevel.NONE, null, NO_INDICATORS, 0, 0));
        assertEquals(HudLevel.NONE, HudModel.NONE.level());
        // con un livello da disegnare lo stato serve
        assertThrows(NullPointerException.class, () -> HudModel.of(HudLevel.BAR_ONLY, null, NORMAL, 0, 0));
    }

    @Test
    void noCellMeansNoPercentAndNoBar() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.mount(piece(PieceSlot.HELMET, 180));
        // senza cella il server manda EMPTY
        HudModel model = HudModel.of(HudLevel.FULL, state, EMPTY, 0, 0);
        assertFalse(model.hasCell());
        assertEquals(0, model.percent());
        assertEquals(0, model.fillWidth());
        assertEquals(0xFF8A8A8A, model.energyColor());
        assertTrue(model.warningSlot());
        assertTrue(model.warningOn());
    }

    @Test
    void spareCellsAreWhiteOrGreyAtZero() {
        FrameState state = withCell(10_000, 5_000);
        assertEquals(0xFF8A8A8A, HudModel.of(HudLevel.FULL, state, NORMAL, 0, 0).spareColor());
        HudModel three = HudModel.of(HudLevel.FULL, state, NORMAL, 3, 0);
        assertEquals(0xFFFFFFFF, three.spareColor());
        assertEquals(3, three.spareCells());
        assertThrows(IllegalArgumentException.class, () -> HudModel.of(HudLevel.FULL, state, NORMAL, -1, 0));
    }

    @Test
    void barFirstRowIsLighterAndOutlinesAreAThird() {
        assertEquals(0xFFAAFFAA, HudModel.lighter(0xFF55FF55));
        assertEquals(0xFFFFAAAA, HudModel.lighter(0xFFFF5555));
        assertEquals(0xFFC4C4C4, HudModel.lighter(0xFF8A8A8A));
        assertEquals(0xFFFFFFFF, HudModel.lighter(0xFFFFFFFF));
        assertEquals(0xFF1C551C, HudModel.outline(0xFF55FF55));
        assertEquals(0xFF55551C, HudModel.outline(0xFFFFFF55));
        assertEquals(0xFF551C1C, HudModel.outline(0xFFFF5555));
        assertEquals(0xFF2E2E2E, HudModel.outline(0xFF8A8A8A));
    }

    @Test
    void noHudColourIsMagentaForTheChecks() {
        assertTrue(magenta(0xFFFF00FF), "control: FF00FF is magenta for arm_measure.sh");
        List<Integer> drawn = new ArrayList<>();
        for (PieceCondition condition : PieceCondition.values()) {
            drawn.add(HudModel.conditionColor(condition));
            drawn.add(HudModel.outline(HudModel.conditionColor(condition)));
        }
        for (Optional<EnergyState> energy : List.of(NORMAL, LOW, EMPTY, NO_INDICATORS)) {
            drawn.add(HudModel.energyColor(energy));
            drawn.add(HudModel.lighter(HudModel.energyColor(energy)));
        }
        drawn.addAll(List.of(HudModel.PLATE, HudModel.PLATE_EDGE, HudModel.BAR_EDGE, HudModel.BAR_BACK,
                HudModel.UNMOUNTED_FILL, HudModel.UNMOUNTED_OUTLINE, HudModel.CELL_BODY, HudModel.CELL_CAP,
                HudModel.CELL_CHARGE));
        for (int colour : drawn) {
            assertTrue(HudModel.ALL_COLOURS.contains(colour), "ALL_COLOURS lacks " + Integer.toHexString(colour));
        }
        // con la mano destra l'HUD sta nella meta' sinistra degli scatti, dove il controllo "dipinto" di
        // m3d_shots.sh vuole zero magenta; con la sinistra nella meta' destra ("mancino")
        for (int colour : HudModel.ALL_COLOURS) {
            assertFalse(magenta(colour), Integer.toHexString(colour) + " is magenta for arm_measure.sh");
        }
    }

    /** Percentuale e riempimento di una cella di quella capacita' con quella carica. */
    private static void assertCharge(int capacity, int stored, int percent, int fillWidth) {
        HudModel model = HudModel.of(HudLevel.FULL, withCell(capacity, stored), NORMAL, 0, 0);
        assertEquals(percent, model.percent(), "percent of " + stored + "/" + capacity);
        assertEquals(fillWidth, model.fillWidth(), "fill of " + stored + "/" + capacity);
    }

    /** Un modello con la percentuale e il riempimento dati, per i controlli del costruttore. */
    private static HudModel charge(int percent, int fillWidth) {
        return new HudModel(HudLevel.FULL, Map.of(), true, percent, fillWidth, HudModel.GREEN, false, false, 0);
    }

    private static Piece piece(PieceSlot slot, int durability) {
        return new Piece(slot, Tier.SCRAP, SCRAP_MAX, durability);
    }

    /** Uno stato con la config predefinita e una cella, senza pezzi. */
    private static FrameState withCell(int capacity, int stored) {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.insertCell(new EnergyCell(capacity, stored));
        return state;
    }

    /** La regola del magenta di tools/client/arm_measure.sh, con i canali fra 0 e 1 e l'alfa ignorato. */
    private static boolean magenta(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return r > 0.2 && b > 0.2 && g < 0.45 * r && g < 0.45 * b;
    }
}
