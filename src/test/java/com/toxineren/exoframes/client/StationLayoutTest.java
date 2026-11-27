package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.StationLayout.ButtonState;
import com.toxineren.exoframes.client.StationLayout.Cursor;
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
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * La disposizione della schermata della stazione (design §5.3, §5.4 e §5.12): i riquadri dentro la schermata e senza
 * sovrapposizioni, lontani da quelli della parte 4b, la schermata intera alla grandezza minima dell'interfaccia, i
 * punti che tools/client/m4a_shots.sh clicca a 960x540 con la scala 2, i colori delle condizioni, il giro
 * dell'anteprima, la vista della tuta dalla vista del telaio e nessun colore magenta per i controlli degli scatti; i
 * posti sotto il puntatore e i numeri dei clic (design §5.3 e §5.6), l'intestazione che perde il massimo quando non ci
 * sta, il disegno di ogni widget nel suo stato; i disegni della texture della schermata (parte 4d), letti dalla PNG del
 * mod: ogni texel in un disegno con il suo nome, lo sfondo opaco, nessun magenta, la grandezza dei rettangoli che
 * riempiono e i colori esatti della legenda.
 */
class StationLayoutTest {
    /** La scala dell'interfaccia della sequenza del client: finestra 960x540, interfaccia 480x270. */
    private static final int SCALE = 2;
    private static final int GUI_WIDTH = 480;
    private static final int GUI_HEIGHT = 270;
    /** Il passo degli slot del gioco base, cornice compresa. */
    private static final int SLOT = 18;

    private static final Rect SCREEN = new Rect(0, 0, StationLayout.WIDTH, StationLayout.HEIGHT);
    /**
     * Il rosso della redstone della cella: il bordo del suo posto e la sua barra della carica. Il codice non lo scrive
     * piu' (sta nei disegni della texture, CELL_RED di tools/art/gui_layout.py): lo legge solo questa prova.
     */
    private static final int CELL_RED = 0xFFC81E1E;

    /**
     * I riquadri dei widget della parte 4b in tools/art/gui_layout.py: materiale, ripara, tutto, le due regioni,
     * tavolozza, predefinito, moduli e il pannello dei preset. Nessun riquadro di questa schermata li tocca, salvo il
     * pannello centrale che li contiene.
     */
    private static final List<Rect> RESERVED_4B = List.of(new Rect(81, 52, 99, 70), new Rect(102, 53, 146, 69),
            new Rect(148, 53, 207, 69), new Rect(81, 73, 143, 85), new Rect(145, 73, 207, 85),
            new Rect(81, 88, 191, 110), new Rect(193, 88, 207, 110), new Rect(81, 113, 171, 131),
            new Rect(175, 138, 309, 225));

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
        // nel pannello del telaio "Tutti" sta accanto ai posti, senza toccare il secondo bordo della scelta
        Map<String, Rect> framePanel = new LinkedHashMap<>(places);
        framePanel.put("ALL_BUTTON", StationLayout.ALL_BUTTON);
        for (Map.Entry<String, Rect> entry : places.entrySet()) {
            assertInside(entry.getKey(), entry.getValue(), "FRAME_PANEL", StationLayout.FRAME_PANEL);
            // il secondo bordo della scelta, fuori dal primo, resta nel pannello e non tocca gli altri posti
            Rect ring = entry.getValue().grow(1);
            assertInside(entry.getKey() + "'s selection border", ring, "FRAME_PANEL", StationLayout.FRAME_PANEL);
            for (Map.Entry<String, Rect> other : framePanel.entrySet()) {
                if (!other.getKey().equals(entry.getKey())) {
                    assertFalse(overlap(ring, other.getValue()), entry.getKey() + "'s selection border "
                            + ring + " overlaps " + other.getKey() + " " + other.getValue());
                }
            }
        }
        assertInside("ALL_BUTTON", StationLayout.ALL_BUTTON, "FRAME_PANEL", StationLayout.FRAME_PANEL);
        // nel pannello centrale la freccia per smontare sta accanto al nome, sopra le altre parti dell'intestazione
        Map<String, Rect> header = header();
        header.put("UNMOUNT", StationLayout.UNMOUNT);
        for (Map.Entry<String, Rect> entry : header.entrySet()) {
            assertInside(entry.getKey(), entry.getValue(), "PIECE_PANEL", StationLayout.PIECE_PANEL);
        }
        assertInside("PREVIEW_CLIP", StationLayout.PREVIEW_CLIP, "PREVIEW_PANEL", StationLayout.PREVIEW_PANEL);
        assertInside("ARROW_LEFT", StationLayout.ARROW_LEFT, "PREVIEW_CLIP", StationLayout.PREVIEW_CLIP);
        assertInside("ARROW_RIGHT", StationLayout.ARROW_RIGHT, "PREVIEW_CLIP", StationLayout.PREVIEW_CLIP);
        assertFalse(overlap(StationLayout.ARROW_LEFT, StationLayout.ARROW_RIGHT), "the two arrows");
        // le frecce a 3 pixel dai bordi del ritaglio, nel suo fondo, l'una lo specchio dell'altra
        Rect clip = StationLayout.PREVIEW_CLIP;
        assertEquals(3, StationLayout.ARROW_LEFT.x0() - clip.x0());
        assertEquals(3, clip.x1() - StationLayout.ARROW_RIGHT.x1());
        assertEquals(StationLayout.ARROW_LEFT.y0(), StationLayout.ARROW_RIGHT.y0());
        assertEquals(StationLayout.ARROW_LEFT.width(), StationLayout.ARROW_RIGHT.width());

        Map<String, Rect> blocks = new LinkedHashMap<>();
        blocks.put("FRAME_PANEL", StationLayout.FRAME_PANEL);
        blocks.put("PIECE_PANEL", StationLayout.PIECE_PANEL);
        blocks.put("PREVIEW_PANEL", StationLayout.PREVIEW_PANEL);
        blocks.put("INVENTORY_LABEL", StationLayout.INVENTORY_LABEL);
        blocks.put("INVENTORY", StationLayout.INVENTORY);
        blocks.put("HOTBAR", StationLayout.HOTBAR);
        blocks.put("TITLE", StationLayout.TITLE);
        blocks.put("STOWED", StationLayout.STOWED);
        assertNoOverlap(blocks);
        assertNoOverlap(framePanel);
        assertNoOverlap(header);

        // i widget della parte 4b hanno i loro riquadri, che nessun altro tocca: solo il pannello centrale li contiene
        for (Map.Entry<String, Rect> entry : table().entrySet()) {
            for (Rect reserved : RESERVED_4B) {
                if (!entry.getKey().equals("PIECE_PANEL")) {
                    assertFalse(overlap(entry.getValue(), reserved),
                            entry.getKey() + " " + entry.getValue() + " overlaps the part 4b widget " + reserved);
                }
            }
        }

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
        Rect helmet = StationLayout.PIECE_SLOTS.get(PieceSlot.HELMET);
        assertEquals("40x40+224+80", onScreen(helmet));
        assertEquals("408x236+178+72", onScreen(new Rect(StationLayout.FRAME_PANEL.x0(), StationLayout.FRAME_PANEL.y0(),
                StationLayout.PIECE_PANEL.x1(), StationLayout.PIECE_PANEL.y1())), "the frame and piece panels");
        // lo slot 1 della barra, cornice compresa, dagli angoli degli slot di StationMenu
        int slotLeft = StationMenu.INVENTORY_X - 1 + SLOT;
        int slotTop = StationMenu.HOTBAR_Y - 1;
        Rect hotbarSlot1 = new Rect(slotLeft, slotTop, slotLeft + SLOT, slotTop + SLOT);
        assertEquals("36x36+214+452", onScreen(hotbarSlot1));

        assertEquals("276,260", centreOnScreen(StationLayout.ALL_BUTTON));
        assertEquals("566,90", centreOnScreen(StationLayout.UNMOUNT));
        assertEquals("244,100", centreOnScreen(helmet));
        assertEquals("244,144", centreOnScreen(StationLayout.PIECE_SLOTS.get(PieceSlot.TORSO)));
        assertEquals("232,470", centreOnScreen(hotbarSlot1));
        assertTrue(hits(helmet, 244, 100), "the helmet point");
        assertTrue(hits(StationLayout.PIECE_SLOTS.get(PieceSlot.TORSO), 244, 144), "the torso point");
        assertTrue(hits(hotbarSlot1, 232, 470), "the point of hotbar slot 1");
        assertFalse(hits(SCREEN, 100, 250), "the point outside the screen");
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
            assertEquals(0xFF, colour >>> 24, hex + " is not opaque");
            assertFalse(magenta(colour), hex + " is magenta for arm_measure.sh");
        }
    }

    @Test
    void spritesCoverTheScreenTextureAndCarryTheLegend() {
        SpriteSheet.assertMagentaRuleWorks();
        SpriteSheet sheet = SpriteSheet.load("station.png");
        assertEquals(StationLayout.TEXTURE_W, sheet.width());
        assertEquals(StationLayout.TEXTURE_H, sheet.height());
        sheet.assertCoveredExactlyBy(SpriteSheet.constants(StationLayout.class));
        // lo sfondo copre la schermata ed e' opaco: il mondo dietro non traspare
        assertEquals(StationLayout.WIDTH, StationLayout.FONDO.width());
        assertEquals(StationLayout.HEIGHT, StationLayout.FONDO.height());
        sheet.assertOpaque("FONDO", StationLayout.FONDO);

        // i posti del pannello del telaio: 20x20, il bordo di 1 pixel nel colore della legenda della condizione, grigio
        // senza pezzo, rosso della redstone con la cella; il secondo bordo bianco
        Rect place = StationLayout.PIECE_SLOTS.get(PieceSlot.HELMET);
        for (PieceCondition condition : PieceCondition.values()) {
            Sprite sprite = StationLayout.place(condition);
            assertEquals(place.width(), sprite.width());
            assertEquals(place.height(), sprite.height());
            sheet.assertRing("the place of a " + condition + " piece", sprite, StationLayout.conditionColor(condition));
        }
        sheet.assertRing("POSTO_VUOTO", StationLayout.POSTO_VUOTO, HudModel.UNMOUNTED_OUTLINE);
        sheet.assertRing("POSTO_CELLA", StationLayout.POSTO_CELLA, CELL_RED);
        assertEquals(place.grow(1).width(), StationLayout.SCELTA.width());
        assertEquals(place.grow(1).height(), StationLayout.SCELTA.height());
        sheet.assertRing("SCELTA", StationLayout.SCELTA, 0xFFFFFFFF);

        // la barra della durata: il fondo e i riempimenti larghi quanto la barra, nel colore della legenda, la prima
        // riga piu' chiara
        Rect bar = StationLayout.DURABILITY_BAR;
        assertEquals(bar.width(), StationLayout.DURATA_FONDO.width());
        assertEquals(bar.height(), StationLayout.DURATA_FONDO.height());
        sheet.assertOpaque("DURATA_FONDO", StationLayout.DURATA_FONDO);
        for (PieceCondition condition : PieceCondition.values()) {
            Sprite fill = StationLayout.durability(condition);
            int colour = StationLayout.conditionColor(condition);
            assertEquals(bar.width(), fill.width());
            assertEquals(bar.height(), fill.height());
            sheet.assertAll("the bar of a " + condition + " piece, first row", fill, 0, 0, fill.width(), 1,
                    HudModel.lighter(colour));
            sheet.assertAll("the bar of a " + condition + " piece", fill, 0, 1, fill.width(), fill.height() - 1,
                    colour);
        }
        sheet.assertAll("DURATA_CELLA", StationLayout.DURATA_CELLA, 0, 1, bar.width(), bar.height() - 1, CELL_RED);

        // le frecce dell'anteprima
        for (Sprite arrow : List.of(StationLayout.FRECCIA_SINISTRA, StationLayout.FRECCIA_DESTRA,
                StationLayout.FRECCIA_SINISTRA_SOPRA, StationLayout.FRECCIA_DESTRA_SOPRA)) {
            assertEquals(StationLayout.ARROW_LEFT.width(), arrow.width());
            assertEquals(StationLayout.ARROW_LEFT.height(), arrow.height());
        }
    }

    @Test
    void placesAreFoundUnderThePointer() {
        for (PieceSlot slot : PieceSlot.values()) {
            Rect place = StationLayout.PIECE_SLOTS.get(slot);
            assertEquals(OptionalInt.of(slot.ordinal()), StationLayout.placeAt(centreX(place), centreY(place)),
                    "the centre of the place of " + slot);
        }
        assertEquals(OptionalInt.of(6), StationLayout.placeAt(centreX(StationLayout.CELL),
                centreY(StationLayout.CELL)), "the centre of the cell place");
        assertTrue(StationLayout.ALL_BUTTON.contains(56, 111), "premise: (56, 111) is in \"All\"");
        assertEquals(OptionalInt.empty(), StationLayout.placeAt(56, 111), "\"All\" is not a place");
        assertEquals(OptionalInt.empty(), StationLayout.placeAt(0, 0), "the corner of the screen");
    }

    @Test
    void placeClicksFollowTheTable() {
        // senza maiuscole: con il cursore vuoto sceglie, con un pezzo del tipo del posto o una cella sul suo posto
        // monta, con altro nulla
        assertEquals(OptionalInt.of(0), StationLayout.placeButton(0, Cursor.EMPTY, false, false));
        assertEquals(OptionalInt.of(3), StationLayout.placeButton(3, Cursor.EMPTY, false, true));
        assertEquals(OptionalInt.of(6), StationLayout.placeButton(6, Cursor.EMPTY, false, false));
        assertEquals(OptionalInt.of(9), StationLayout.placeButton(1, Cursor.FITTING_PIECE, false, false));
        assertEquals(OptionalInt.of(9), StationLayout.placeButton(1, Cursor.FITTING_PIECE, false, true));
        assertEquals(OptionalInt.empty(), StationLayout.placeButton(6, Cursor.FITTING_PIECE, false, false));
        assertEquals(OptionalInt.of(14), StationLayout.placeButton(6, Cursor.CELL, false, true));
        assertEquals(OptionalInt.empty(), StationLayout.placeButton(2, Cursor.CELL, false, false));
        assertEquals(OptionalInt.empty(), StationLayout.placeButton(4, Cursor.OTHER, false, false));
        // con le maiuscole smonta cio' che e' montato, qualunque cosa ci sia sul cursore; un posto vuoto nulla
        assertEquals(OptionalInt.of(21), StationLayout.placeButton(5, Cursor.EMPTY, true, true));
        assertEquals(OptionalInt.of(22), StationLayout.placeButton(6, Cursor.OTHER, true, true));
        assertEquals(OptionalInt.empty(), StationLayout.placeButton(0, Cursor.FITTING_PIECE, true, false));
        assertThrows(IllegalArgumentException.class, () -> StationLayout.placeButton(7, Cursor.EMPTY, false, false));
        assertThrows(IllegalArgumentException.class, () -> StationLayout.placeButton(-1, Cursor.EMPTY, false, false));

        // la freccia smonta la scelta se c'e': un pezzo o la cella, mai tutti i pezzi
        Map<PieceSlot, StationView.PieceInfo> helmet = Map.of(PieceSlot.HELMET,
                new StationView.PieceInfo(Tier.SCRAP, 180, 180, Appearance.UNPAINTED, Appearance.UNPAINTED));
        Optional<StationView.CellInfo> cell = Optional.of(new StationView.CellInfo(3_000, 10_000));
        assertEquals(OptionalInt.of(16), StationLayout.unmountButton(view(helmet, Optional.empty(), 0)));
        assertEquals(OptionalInt.empty(), StationLayout.unmountButton(view(Map.of(), cell, 0)));
        assertEquals(OptionalInt.of(22), StationLayout.unmountButton(view(helmet, cell, StationView.SELECT_CELL)));
        assertEquals(OptionalInt.empty(), StationLayout.unmountButton(view(helmet, Optional.empty(),
                StationView.SELECT_CELL)));
        assertEquals(OptionalInt.empty(), StationLayout.unmountButton(view(helmet, cell, StationView.SELECT_ALL)));
    }

    @Test
    void numbersStayBesideTheConditionOrLoseTheMaximum() {
        // "Danneggiato" (60 pixel) e "180 / 180" (50); "10000 / 10000" (74) no
        assertTrue(StationLayout.fitsBeside(60, 50));
        assertFalse(StationLayout.fitsBeside(60, 74));
        assertTrue(StationLayout.fitsBeside(60, 30));
        // al pixel: 81 + 60 + 4 = 145 = 207 - 62
        assertTrue(StationLayout.fitsBeside(60, 62));
        assertFalse(StationLayout.fitsBeside(60, 63));
        // la cella, senza condizione, con la capacita' massima della config: "1000000 / 1000000 RE" (114)
        assertTrue(StationLayout.fitsBeside(0, 114));
    }

    @Test
    void widgetsTakeTheirSpriteForTheirState() {
        assertEquals(ButtonState.OFF, StationLayout.buttonState(false, true, true));
        assertEquals(ButtonState.PRESSED, StationLayout.buttonState(true, true, true));
        assertEquals(ButtonState.HOVER, StationLayout.buttonState(true, false, true));
        assertEquals(ButtonState.NORMAL, StationLayout.buttonState(true, false, false));

        assertEquals(List.of(StationLayout.TUTTI_NORMALE, StationLayout.TUTTI_SOPRA, StationLayout.TUTTI_PREMUTO,
                StationLayout.TUTTI_SPENTO), List.of(StationLayout.allSprite(ButtonState.NORMAL),
                StationLayout.allSprite(ButtonState.HOVER), StationLayout.allSprite(ButtonState.PRESSED),
                StationLayout.allSprite(ButtonState.OFF)));
        assertEquals(List.of(StationLayout.SMONTA_NORMALE, StationLayout.SMONTA_SOPRA, StationLayout.SMONTA_PREMUTO,
                StationLayout.SMONTA_SPENTO), List.of(StationLayout.unmountSprite(ButtonState.NORMAL),
                StationLayout.unmountSprite(ButtonState.HOVER), StationLayout.unmountSprite(ButtonState.PRESSED),
                StationLayout.unmountSprite(ButtonState.OFF)));
        // ogni widget ha la misura dei suoi disegni
        assertEquals(30, StationLayout.ALL_BUTTON.width());
        assertEquals(16, StationLayout.ALL_BUTTON.height());
        assertEquals(12, StationLayout.UNMOUNT.width());
        assertEquals(12, StationLayout.UNMOUNT.height());
        for (ButtonState state : ButtonState.values()) {
            assertSize("allSprite(" + state + ")", StationLayout.allSprite(state), StationLayout.ALL_BUTTON);
            assertSize("unmountSprite(" + state + ")", StationLayout.unmountSprite(state), StationLayout.UNMOUNT);
        }

        // i fantasmi dei posti vuoti, grandi come l'icona dentro un posto
        assertEquals(List.of(StationLayout.FANTASMA_ELMO, StationLayout.FANTASMA_TORSO, StationLayout.FANTASMA_BRACCIO,
                StationLayout.FANTASMA_BRACCIO, StationLayout.FANTASMA_GAMBA, StationLayout.FANTASMA_GAMBA,
                StationLayout.FANTASMA_CELLA), List.of(StationLayout.ghostSprite(0), StationLayout.ghostSprite(1),
                StationLayout.ghostSprite(2), StationLayout.ghostSprite(3), StationLayout.ghostSprite(4),
                StationLayout.ghostSprite(5), StationLayout.ghostSprite(6)));
        Rect place = StationLayout.PIECE_SLOTS.get(PieceSlot.HELMET);
        Rect icon = new Rect(place.x0() + StationLayout.ICON_INSET, place.y0() + StationLayout.ICON_INSET,
                place.x1() - StationLayout.ICON_INSET, place.y1() - StationLayout.ICON_INSET);
        assertEquals(16, icon.width());
        assertEquals(16, icon.height());
        for (int ghost = 0; ghost <= 6; ghost++) {
            assertSize("ghostSprite(" + ghost + ")", StationLayout.ghostSprite(ghost), icon);
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
        table.put("STOWED", StationLayout.STOWED);
        table.put("ALL_BUTTON", StationLayout.ALL_BUTTON);
        table.put("UNMOUNT", StationLayout.UNMOUNT);
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

    /** Il centro del riquadro, relativo all'angolo della schermata, negli scatti di 960x540 con la scala 2: x,y. */
    private static String centreOnScreen(Rect r) {
        int left = StationLayout.leftPos(GUI_WIDTH);
        int top = StationLayout.topPos(GUI_HEIGHT);
        return (SCALE * (left + r.x0()) + SCALE * r.width() / 2) + ","
                + (SCALE * (top + r.y0()) + SCALE * r.height() / 2);
    }

    private static double centreX(Rect r) {
        return (r.x0() + r.x1()) / 2.0;
    }

    private static double centreY(Rect r) {
        return (r.y0() + r.y1()) / 2.0;
    }

    /** Il disegno e' grande come il riquadro in cui si disegna. */
    private static void assertSize(String name, Sprite sprite, Rect r) {
        assertEquals(r.width(), sprite.width(), name + " width");
        assertEquals(r.height(), sprite.height(), name + " height");
    }

    /** Una vista con i pezzi, la cella e la scelta dati, e le bandierine spente. */
    private static StationView view(Map<PieceSlot, StationView.PieceInfo> pieces, Optional<StationView.CellInfo> cell,
            int selection) {
        return new StationView(pieces, cell, false, false, false, false, selection, false, false);
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
