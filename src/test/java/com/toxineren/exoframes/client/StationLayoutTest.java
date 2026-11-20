package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.station.StationMenu;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La disposizione della schermata della stazione (design §5.3, §5.4 e §5.12): i riquadri dentro la schermata e senza
 * sovrapposizioni, la schermata intera alla grandezza minima dell'interfaccia, i punti che tools/client/m4a1_shots.sh
 * clicca a 960x540 con la scala 2, i colori delle condizioni, il giro dell'anteprima, la vista della tuta dalla vista
 * del telaio e nessun colore magenta per i controlli degli scatti.
 */
class StationLayoutTest {
    /** La scala dell'interfaccia della sequenza del client: finestra 960x540, interfaccia 480x270. */
    private static final int SCALE = 2;
    private static final int GUI_WIDTH = 480;
    private static final int GUI_HEIGHT = 270;
    /** Il passo degli slot del gioco base, cornice compresa. */
    private static final int SLOT = 18;

    private static final Rect SCREEN = new Rect(0, 0, StationLayout.WIDTH, StationLayout.HEIGHT);

    @Test
    void widgetsStayInsideTheScreenAndDoNotOverlap() {
        for (Map.Entry<String, Rect> entry : table().entrySet()) {
            Rect r = entry.getValue();
            assertTrue(r.width() > 0 && r.height() > 0, entry.getKey() + " is empty: " + r);
            assertInside(entry.getKey(), r, "the screen", SCREEN);
        }
        assertEquals(List.of(PieceSlot.values()), new ArrayList<>(StationLayout.PIECE_SLOTS.keySet()),
                "the frame panel places");

        Map<String, Rect> places = places();
        for (Map.Entry<String, Rect> entry : places.entrySet()) {
            assertInside(entry.getKey(), entry.getValue(), "FRAME_PANEL", StationLayout.FRAME_PANEL);
            // il secondo bordo della scelta, fuori dal primo, resta nel pannello e non tocca gli altri posti
            Rect ring = entry.getValue().grow(1);
            assertInside(entry.getKey() + "'s selection border", ring, "FRAME_PANEL", StationLayout.FRAME_PANEL);
            for (Map.Entry<String, Rect> other : places.entrySet()) {
                if (other != entry) {
                    assertFalse(overlap(ring, other.getValue()), entry.getKey() + "'s selection border "
                            + ring + " overlaps " + other.getKey() + " " + other.getValue());
                }
            }
        }
        Map<String, Rect> header = header();
        for (Map.Entry<String, Rect> entry : header.entrySet()) {
            assertInside(entry.getKey(), entry.getValue(), "PIECE_PANEL", StationLayout.PIECE_PANEL);
        }
        assertInside("PREVIEW_CLIP", StationLayout.PREVIEW_CLIP, "PREVIEW_PANEL", StationLayout.PREVIEW_PANEL);

        Map<String, Rect> blocks = new LinkedHashMap<>();
        blocks.put("FRAME_PANEL", StationLayout.FRAME_PANEL);
        blocks.put("PIECE_PANEL", StationLayout.PIECE_PANEL);
        blocks.put("PREVIEW_PANEL", StationLayout.PREVIEW_PANEL);
        blocks.put("INVENTORY_LABEL", StationLayout.INVENTORY_LABEL);
        blocks.put("INVENTORY", StationLayout.INVENTORY);
        blocks.put("HOTBAR", StationLayout.HOTBAR);
        blocks.put("TEST_BUTTON", StationLayout.TEST_BUTTON);
        blocks.put("TITLE", StationLayout.TITLE);
        assertNoOverlap(blocks);
        assertNoOverlap(places);
        assertNoOverlap(header);

        // le cornici dei 36 slot del menu, dagli angoli di StationMenu, riempiono inventario e barra
        int left = StationMenu.INVENTORY_X - 1;
        int top = StationMenu.INVENTORY_Y - 1;
        assertEquals(new Rect(left, top, left + 9 * SLOT, top + 3 * SLOT), StationLayout.INVENTORY,
                "INVENTORY and the menu's inventory slots");
        top = StationMenu.HOTBAR_Y - 1;
        assertEquals(new Rect(left, top, left + 9 * SLOT, top + SLOT), StationLayout.HOTBAR,
                "HOTBAR and the menu's hotbar slots");
    }

    @Test
    void screenFitsTheSmallestInterface() {
        assertEquals(2, StationLayout.leftPos(320));
        assertEquals(4, StationLayout.topPos(240));
        assertEquals(55, StationLayout.leftPos(427));
        assertEquals(82, StationLayout.leftPos(480));
        assertEquals(19, StationLayout.topPos(270));

        Rect gui = new Rect(0, 0, 320, 240);
        int left = StationLayout.leftPos(320);
        int top = StationLayout.topPos(240);
        assertInside("the screen", shifted(SCREEN, left, top), "the 320x240 interface", gui);
        for (Map.Entry<String, Rect> entry : table().entrySet()) {
            assertInside(entry.getKey(), shifted(entry.getValue(), left, top), "the 320x240 interface", gui);
        }
    }

    @Test
    void screenPointsAtNineSixtyByFiveFortyMatchTheSequence() {
        assertEquals("632x464+164+38", onScreen(SCREEN));
        assertEquals("184x232+596+74", onScreen(StationLayout.PREVIEW_CLIP));
        assertEquals("36x32+730+410", onScreen(StationLayout.TEST_BUTTON));

        assertTrue(hits(StationLayout.TEST_BUTTON, 748, 426), "the test button point");
        assertTrue(hits(StationLayout.PIECE_SLOTS.get(PieceSlot.TORSO), 244, 144), "the torso point");
        Rect firstSlot = new Rect(StationMenu.INVENTORY_X, StationMenu.INVENTORY_Y, StationMenu.INVENTORY_X + 16,
                StationMenu.INVENTORY_Y + 16);
        assertTrue(hits(firstSlot, 196, 354), "the first inventory slot point");
        assertTrue(hits(StationLayout.PREVIEW_CLIP, 620, 250) && hits(StationLayout.PREVIEW_CLIP, 740, 250),
                "the two ends of the drag");
        assertEquals(60.0, (740 - 620) / (double) SCALE, "the drag in interface pixels");
        assertEquals(90f, StationLayout.rotate(0f, (740 - 620) / (double) SCALE),
                "the drag turns the suit to the side");
        assertFalse(hits(SCREEN, 10, 10), "the neutral point");
    }

    @Test
    void conditionColoursFollowTheLegend() {
        assertEquals(0xFF55FF55, StationLayout.conditionColor(PieceCondition.INTACT));
        assertEquals(0xFFFFFF55, StationLayout.conditionColor(PieceCondition.DAMAGED));
        assertEquals(0xFFFF5555, StationLayout.conditionColor(PieceCondition.CRITICAL));
        assertEquals(0xFF8A8A8A, StationLayout.conditionColor(PieceCondition.BROKEN));
    }

    @Test
    void dragTurnsOneAndAHalfDegreesPerPixel() {
        assertEquals(90f, StationLayout.rotate(0f, 60));
        assertEquals(30f, StationLayout.rotate(300f, 60));
        assertEquals(270f, StationLayout.rotate(0f, -60));

        float[] angles = {0f, 0.5f, 90f, 359.9f, 359.99997f, -90f, 720f};
        double[] drags = {-1e9, -240, -60, -1e-9, 0, 1e-9, 0.25, 60, 240, 1e9, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (float angle : angles) {
            for (double drag : drags) {
                float turned = StationLayout.rotate(angle, drag);
                assertTrue(turned >= 0f && turned < 360f, "rotate(" + angle + ", " + drag + ") = " + turned);
            }
        }
    }

    @Test
    void previewViewComesFromTheStationView() {
        Map<PieceSlot, StationView.PieceInfo> pieces = new EnumMap<>(PieceSlot.class);
        pieces.put(PieceSlot.HELMET, new StationView.PieceInfo(Tier.SCRAP, 60, 180, 0x3A6EA5, Appearance.UNPAINTED));
        pieces.put(PieceSlot.TORSO, new StationView.PieceInfo(Tier.SCRAP, 0, 180, Appearance.UNPAINTED,
                Appearance.UNPAINTED));
        StationView view = new StationView(pieces, Optional.empty(), false, false, false, false, 0, false, false);

        assertEquals(Map.of(
                PieceSlot.HELMET, new SuitView.PieceView(1, 0x3A6EA5, TierPalette.secondary(Tier.SCRAP)),
                PieceSlot.TORSO, new SuitView.PieceView(3, TierPalette.primary(Tier.SCRAP),
                        TierPalette.secondary(Tier.SCRAP))), StationLayout.suitView(view).pieces());
    }

    @Test
    void noScreenColourIsMagentaForTheChecks() {
        assertTrue(magenta(0xFFFF00FF), "control: FF00FF is magenta for arm_measure.sh");
        for (PieceCondition condition : PieceCondition.values()) {
            assertTrue(StationLayout.ALL_COLOURS.contains(StationLayout.conditionColor(condition)),
                    "ALL_COLOURS lacks the colour of " + condition);
        }
        for (int colour : StationLayout.ALL_COLOURS) {
            String hex = Integer.toHexString(colour);
            assertEquals(0xFF, colour >>> 24, hex + " is not opaque: the world would show through the screen");
            assertFalse(magenta(colour), hex + " is magenta for arm_measure.sh");
            // sotto il mouse un pulsante si schiarisce: anche quel colore non e' magenta
            assertFalse(magenta(StationLayout.lighter(colour)), hex + " under the mouse is magenta");
        }
    }

    /** Tutti i riquadri della tabella "Disposizione della schermata", per nome. */
    private static Map<String, Rect> table() {
        Map<String, Rect> table = new LinkedHashMap<>();
        table.put("TITLE", StationLayout.TITLE);
        table.put("FRAME_PANEL", StationLayout.FRAME_PANEL);
        table.putAll(places());
        table.put("PIECE_PANEL", StationLayout.PIECE_PANEL);
        table.putAll(header());
        table.put("PREVIEW_PANEL", StationLayout.PREVIEW_PANEL);
        table.put("PREVIEW_CLIP", StationLayout.PREVIEW_CLIP);
        table.put("INVENTORY_LABEL", StationLayout.INVENTORY_LABEL);
        table.put("INVENTORY", StationLayout.INVENTORY);
        table.put("HOTBAR", StationLayout.HOTBAR);
        table.put("TEST_BUTTON", StationLayout.TEST_BUTTON);
        return table;
    }

    /** I sei posti dei pezzi e la cella. */
    private static Map<String, Rect> places() {
        Map<String, Rect> places = new LinkedHashMap<>();
        StationLayout.PIECE_SLOTS.forEach((slot, r) -> places.put("PIECE_SLOTS " + slot, r));
        places.put("CELL", StationLayout.CELL);
        return places;
    }

    /** Le quattro parti dell'intestazione del pannello centrale. */
    private static Map<String, Rect> header() {
        Map<String, Rect> header = new LinkedHashMap<>();
        header.put("NAME", StationLayout.NAME);
        header.put("CONDITION", StationLayout.CONDITION);
        header.put("DURABILITY_NUMBERS", StationLayout.DURABILITY_NUMBERS);
        header.put("DURABILITY_BAR", StationLayout.DURABILITY_BAR);
        return header;
    }

    private static void assertInside(String name, Rect r, String outerName, Rect outer) {
        assertTrue(r.x0() >= outer.x0() && r.y0() >= outer.y0() && r.x1() <= outer.x1() && r.y1() <= outer.y1(),
                name + " " + r + " is not inside " + outerName + " " + outer);
    }

    private static void assertNoOverlap(Map<String, Rect> rects) {
        List<Map.Entry<String, Rect>> entries = new ArrayList<>(rects.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                Map.Entry<String, Rect> a = entries.get(i);
                Map.Entry<String, Rect> b = entries.get(j);
                assertFalse(overlap(a.getValue(), b.getValue()),
                        a.getKey() + " " + a.getValue() + " overlaps " + b.getKey() + " " + b.getValue());
            }
        }
    }

    /** Vero se i due rettangoli hanno almeno un pixel in comune (bordi destro e in basso esclusi). */
    private static boolean overlap(Rect a, Rect b) {
        return a.x0() < b.x1() && b.x0() < a.x1() && a.y0() < b.y1() && b.y0() < a.y1();
    }

    private static Rect shifted(Rect r, int dx, int dy) {
        return new Rect(r.x0() + dx, r.y0() + dy, r.x1() + dx, r.y1() + dy);
    }

    /** Il riquadro, relativo all'angolo della schermata, negli scatti di 960x540 con la scala 2: LxA+X+Y. */
    private static String onScreen(Rect r) {
        int left = StationLayout.leftPos(GUI_WIDTH);
        int top = StationLayout.topPos(GUI_HEIGHT);
        return SCALE * r.width() + "x" + SCALE * r.height() + "+" + SCALE * (left + r.x0()) + "+"
                + SCALE * (top + r.y0());
    }

    /** Vero se il punto (x, y) degli scatti di 960x540 cade nel riquadro, relativo all'angolo della schermata. */
    private static boolean hits(Rect r, int x, int y) {
        return r.contains(x / (double) SCALE - StationLayout.leftPos(GUI_WIDTH),
                y / (double) SCALE - StationLayout.topPos(GUI_HEIGHT));
    }

    /** La regola del magenta di tools/client/arm_measure.sh, con i canali fra 0 e 1. */
    private static boolean magenta(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return r > 0.2 && b > 0.2 && g < 0.45 * r && g < 0.45 * b;
    }
}
