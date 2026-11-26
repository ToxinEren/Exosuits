package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.HudLayout.Rect;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.frame.HudLevel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * I rettangoli dell'HUD ({@link HudLayout}, design §5.12, "Dove" ed "Elementi", e §12): il gruppo nell'angolo in basso
 * opposto alla mano principale, o in alto a sinistra in un'interfaccia stretta, lontano dal braccio, dalla barra degli
 * oggetti con le barre di stato, dallo slot della seconda mano, dalle righe della chat e dal mirino alle sette
 * grandezze del design; gli elementi dentro i loro sottogruppi e senza sovrapposizioni; l'interno mai specchiato; gli
 * angoli della vignetta; i riquadri a 960x540 con la scala 2 che usano tools/client/arm_measure.sh e m4c_shots.sh.
 */
class HudLayoutTest {
    /** La scala dell'interfaccia delle sequenze del client: finestra 960x540, interfaccia 480x270. */
    private static final int SCALE = 2;
    /** Le cinque grandezze del design (§5.19) in cui il gruppo sta in basso: larghezza e altezza. */
    private static final int[][] WIDE_SIZES = {{427, 240}, {480, 270}, {640, 360}, {683, 384}, {640, 270}};
    /** Le due grandezze del design piu' strette di 424 pixel: il gruppo va in alto a sinistra. */
    private static final int[][] NARROW_SIZES = {{341, 256}, {320, 240}};
    /** Mano principale destra e sinistra. */
    private static final boolean[] HANDS = {true, false};
    /** L'altezza di una riga di testo: il carattere del gioco e' alto 8 pixel, piu' uno di ombra. */
    private static final int LINE_HEIGHT = 9;
    /** "100 %", la percentuale piu' larga senza avviso (le larghezze sono quelle dei "Fatti del gioco" del piano). */
    private static final int WIDEST_PERCENT = 27;
    /** "9 %", la percentuale piu' larga con l'avviso: con LOW la carica e' sotto il 10 %, con EMPTY e' zero. */
    private static final int WIDEST_PERCENT_WITH_WARNING = 15;
    /** Il segno di moltiplicazione e una cifra: le celle di scorta da 0 a 9. */
    private static final int ONE_DIGIT_SPARE = 11;
    /** Il segno di moltiplicazione e due cifre: le celle di scorta da 10 a 99. */
    private static final int TWO_DIGIT_SPARE = 17;
    /** "Senza cella", il testo senza cella piu' largo ("No cell" e' 33). */
    private static final int NO_CELL_TEXT = 57;

    @Test
    void groupSitsInTheCornerOppositeTheMainHand() {
        assertEquals(new Rect(2, 233, 90, 268), HudLayout.group(480, 270, true));
        assertEquals(new Rect(390, 233, 478, 268), HudLayout.group(480, 270, false));
        assertEquals(new Rect(2, 233, 23, 268), HudLayout.silhouetteGroup(480, 270, true));
        assertEquals(new Rect(23, 233, 90, 268), HudLayout.energyGroup(480, 270, true));
        assertEquals(new Rect(390, 233, 457, 268), HudLayout.energyGroup(480, 270, false));
        assertEquals(new Rect(457, 233, 478, 268), HudLayout.silhouetteGroup(480, 270, false));
        for (boolean mainRight : HANDS) {
            assertEquals(HudLayout.group(480, 270, mainRight), HudLayout.plate(480, 270, mainRight, HudLevel.FULL));
            assertEquals(HudLayout.energyGroup(480, 270, mainRight),
                    HudLayout.plate(480, 270, mainRight, HudLevel.BAR_ONLY));
            assertThrows(IllegalArgumentException.class, () -> HudLayout.plate(480, 270, mainRight, HudLevel.NONE));
        }
    }

    @Test
    void narrowInterfacePutsTheGroupTopLeft() {
        assertTrue(HudLayout.narrow(423));
        assertFalse(HudLayout.narrow(424));
        Rect topLeft = new Rect(2, 2, 90, 37);
        for (boolean mainRight : HANDS) {
            assertEquals(topLeft, HudLayout.group(423, 240, mainRight));
        }
        // la disposizione della mano destra anche con la sinistra: la sagoma a sinistra
        assertEquals(new Rect(2, 2, 23, 37), HudLayout.silhouetteGroup(423, 240, false));
        assertEquals(new Rect(23, 2, 90, 37), HudLayout.energyGroup(423, 240, false));

        Rect justWideEnough = HudLayout.group(424, 240, true);
        assertEquals(new Rect(2, 203, 90, 238), justWideEnough);
        Area offhand = offhand(424, 240, true);
        assertEquals(92.0, offhand.x0(), "424 / 2 - 120");
        assertEquals(2.0, offhand.x0() - justWideEnough.x1(), "the gap between the group and the offhand slot");

        for (int[] size : NARROW_SIZES) {
            for (boolean mainRight : HANDS) {
                assertEquals(topLeft, HudLayout.group(size[0], size[1], mainRight),
                        size[0] + "x" + size[1] + (mainRight ? ", right hand" : ", left hand"));
            }
        }
    }

    @Test
    void groupAvoidsTheArmTheHotbarTheOffhandTheChatAndTheCrosshair() {
        for (int[] size : allSizes()) {
            int w = size[0];
            int h = size[1];
            for (boolean mainRight : HANDS) {
                for (HudLevel level : List.of(HudLevel.FULL, HudLevel.BAR_ONLY)) {
                    Rect plate = HudLayout.plate(w, h, mainRight, level);
                    String where = level + " plate " + plate + " at " + w + "x" + h
                            + (mainRight ? ", right hand" : ", left hand");
                    assertTrue(new Rect(0, 0, w, h).contains(plate), where + " leaves the interface");
                    for (Area area : vanilla(w, h, mainRight)) {
                        assertFalse(area.touches(plate), where + " touches " + area);
                    }
                }
            }
        }
    }

    @Test
    void elementsStayInsideTheirSubgroupAndDoNotOverlap() {
        Rect silhouette = new Rect(0, 0, 21, 35);
        assertEquals(silhouette, new Rect(0, 0, HudLayout.SILHOUETTE_W, HudLayout.GROUP_H));
        PieceSlot[] slots = PieceSlot.values();
        for (int i = 0; i < slots.length; i++) {
            Rect a = HudLayout.piece(slots[i]);
            assertTrue(silhouette.contains(a), slots[i] + " " + a + " leaves the silhouette subgroup");
            for (int j = i + 1; j < slots.length; j++) {
                Rect b = HudLayout.piece(slots[j]);
                assertFalse(a.intersects(b), slots[i] + " " + a + " overlaps " + slots[j] + " " + b);
            }
        }

        Rect energy = new Rect(0, 0, 67, 35);
        assertEquals(energy, new Rect(0, 0, HudLayout.ENERGY_W, HudLayout.GROUP_H));
        Rect textRow = text(HudLayout.TEXT_X, HudLayout.TEXT_Y, HudLayout.SPARE_RIGHT - HudLayout.TEXT_X);
        assertEquals(new Rect(2, 8, 65, 17), textRow);
        for (Rect r : List.of(HudLayout.WARNING, textRow, HudLayout.BAR_FRAME)) {
            assertTrue(energy.contains(r), r + " leaves the energy subgroup");
        }
        Rect frame = HudLayout.BAR_FRAME;
        assertEquals(63, frame.width());
        assertEquals(8, frame.height());
        assertEquals(new Rect(frame.x0() + 1, frame.y0() + 1, frame.x1() - 1, frame.y1() - 1), HudLayout.BAR_FILL);
        assertEquals(HudModel.FILL_MAX, HudLayout.BAR_FILL.width(), "the model's widest fill is the bar's");
        assertTrue(textRow.contains(HudLayout.WARNING));
        assertFalse(textRow.intersects(frame));

        assertEquals(54, HudLayout.spareTextX(ONE_DIGIT_SPARE));
        assertEquals(48, cellIconX(ONE_DIGIT_SPARE));
        assertEquals(48, HudLayout.spareTextX(TWO_DIGIT_SPARE));
        assertEquals(42, cellIconX(TWO_DIGIT_SPARE));

        Rect percent = text(HudLayout.TEXT_X, HudLayout.TEXT_Y, WIDEST_PERCENT);
        Rect percentAfterWarning = text(HudLayout.TEXT_X_AFTER_WARNING, HudLayout.TEXT_Y, WIDEST_PERCENT_WITH_WARNING);
        assertEquals(new Rect(2, 8, 29, 17), percent);
        assertEquals(new Rect(12, 8, 27, 17), percentAfterWarning);
        assertTrue(HudLayout.PERCENT_AREA.contains(percent), "the widest percentage");
        assertTrue(HudLayout.PERCENT_AREA.contains(percentAfterWarning), "the widest percentage after the warning");
        assertFalse(percentAfterWarning.intersects(HudLayout.WARNING), "the percentage after the warning");
        for (int width : new int[] {ONE_DIGIT_SPARE, TWO_DIGIT_SPARE}) {
            Rect spare = new Rect(cellIconX(width), HudLayout.TEXT_Y, HudLayout.SPARE_RIGHT,
                    HudLayout.TEXT_Y + LINE_HEIGHT);
            assertTrue(HudLayout.SPARE_AREA.contains(spare), "the spare cells " + spare);
        }
        assertFalse(HudLayout.PERCENT_AREA.intersects(HudLayout.SPARE_AREA));

        // senza cella il testo prende la riga della barra, vuota (Decisione 1 del piano)
        Rect noCell = text(HudLayout.NO_CELL_X, HudLayout.NO_CELL_Y, NO_CELL_TEXT);
        assertEquals(frame.x0(), noCell.x0());
        assertEquals(frame.y0(), noCell.y0());
        assertTrue(noCell.x1() <= frame.x1(), "the no-cell text " + noCell + " ends after " + frame.x1());
        assertTrue(energy.contains(noCell));
        assertFalse(noCell.intersects(textRow));
    }

    @Test
    void insideIsNeverMirrored() {
        // vista da dietro: il braccio sinistro a sinistra
        assertTrue(HudLayout.piece(PieceSlot.LEFT_ARM).x0() < HudLayout.piece(PieceSlot.TORSO).x0());
        assertTrue(HudLayout.piece(PieceSlot.TORSO).x0() < HudLayout.piece(PieceSlot.RIGHT_ARM).x0());
        assertTrue(HudLayout.piece(PieceSlot.LEFT_LEG).x0() < HudLayout.piece(PieceSlot.RIGHT_LEG).x0());

        for (int[] size : WIDE_SIZES) {
            int w = size[0];
            int h = size[1];
            // la sagoma sta verso il bordo, l'energia e' l'altro sottogruppo
            Rect right = HudLayout.group(w, h, true);
            Rect rightSilhouette = HudLayout.silhouetteGroup(w, h, true);
            assertEquals(right.x0(), rightSilhouette.x0());
            assertEquals(new Rect(rightSilhouette.x1(), right.y0(), right.x1(), right.y1()),
                    HudLayout.energyGroup(w, h, true));
            Rect left = HudLayout.group(w, h, false);
            Rect leftSilhouette = HudLayout.silhouetteGroup(w, h, false);
            assertEquals(left.x1(), leftSilhouette.x1());
            assertEquals(new Rect(left.x0(), left.y0(), leftSilhouette.x0(), left.y1()),
                    HudLayout.energyGroup(w, h, false));
        }
        // le costanti dell'energia contano dal suo bordo sinistro con tutte e due le mani: il testo si legge da
        // sinistra
        assertEquals(2, HudLayout.TEXT_X);
        assertEquals(25, HudLayout.energyGroup(480, 270, true).x0() + HudLayout.TEXT_X);
        assertEquals(392, HudLayout.energyGroup(480, 270, false).x0() + HudLayout.TEXT_X);
    }

    @Test
    void vignetteCornersStayInTheCorners() {
        assertEquals(List.of(new Rect(0, 0, 72, 40), new Rect(408, 0, 480, 40), new Rect(0, 230, 72, 270),
                new Rect(408, 230, 480, 270)), HudLayout.vignetteCorners(480, 270));
        for (int[] size : WIDE_SIZES) {
            int w = size[0];
            int h = size[1];
            List<Area> elements = List.of(hotbarBand(w, h), offhand(w, h, true), offhand(w, h, false), chat(h),
                    crosshair(w, h));
            for (Rect corner : HudLayout.vignetteCorners(w, h)) {
                for (Area area : elements) {
                    assertFalse(area.touches(corner), "corner " + corner + " at " + w + "x" + h + " touches " + area);
                }
            }
        }
        // sotto i 384 pixel gli angoli in basso passano sopra lo slot della seconda mano: accettato (design §5.12);
        // il test lo dice, cosi' un cambio della soglia si vede
        for (int[] size : NARROW_SIZES) {
            int w = size[0];
            int h = size[1];
            List<Rect> corners = HudLayout.vignetteCorners(w, h);
            assertTrue(offhand(w, h, true).touches(corners.get(2)), "bottom left corner at " + w + "x" + h);
            assertTrue(offhand(w, h, false).touches(corners.get(3)), "bottom right corner at " + w + "x" + h);
        }
    }

    @Test
    void screenRectsAtNineSixtyMatchTheSequence() {
        assertEquals("176x70+4+466", screen(HudLayout.group(480, 270, true)));
        assertEquals("176x70+780+466", screen(HudLayout.group(480, 270, false)));
        assertEquals("134x70+46+466", screen(HudLayout.plate(480, 270, true, HudLevel.BAR_ONLY)));
        assertEquals("134x70+780+466", screen(HudLayout.plate(480, 270, false, HudLevel.BAR_ONLY)));
        assertEquals("42x70+4+466", screen(HudLayout.silhouetteGroup(480, 270, true)));
        assertEquals("42x70+914+466", screen(HudLayout.silhouetteGroup(480, 270, false)));
        Rect right = HudLayout.energyGroup(480, 270, true);
        Rect left = HudLayout.energyGroup(480, 270, false);
        assertEquals("122x12+52+508", screen(HudLayout.BAR_FILL.offset(right.x0(), right.y0())));
        assertEquals("58x18+50+482", screen(HudLayout.PERCENT_AREA.offset(right.x0(), right.y0())));
        assertEquals("58x18+784+482", screen(HudLayout.PERCENT_AREA.offset(left.x0(), left.y0())));
        assertEquals("50x18+126+482", screen(HudLayout.SPARE_AREA.offset(right.x0(), right.y0())));
        List<String> corners = new ArrayList<>();
        for (Rect corner : HudLayout.vignetteCorners(480, 270)) {
            corners.add(screen(corner));
        }
        assertEquals(List.of("144x80+0+0", "144x80+816+0", "144x80+0+460", "144x80+816+460"), corners);
    }

    /** Il riquadro sullo schermo a 960x540 con la scala 2. */
    private static String screen(Rect r) {
        return HudLayout.screen(r, SCALE);
    }

    /** Una riga di testo larga {@code width} da (x, y), ombra compresa. */
    private static Rect text(int x, int y, int width) {
        return new Rect(x, y, x + width, y + LINE_HEIGHT);
    }

    /** L'ascissa dell'icona della cella prima del testo "xN" largo {@code textWidth}, con un pixel fra i due. */
    private static int cellIconX(int textWidth) {
        return HudLayout.spareTextX(textWidth) - 1 - HudLayout.CELL_ICON_W;
    }

    private static List<int[]> allSizes() {
        List<int[]> sizes = new ArrayList<>(List.of(WIDE_SIZES));
        sizes.addAll(List.of(NARROW_SIZES));
        return sizes;
    }

    /**
     * Gli elementi del gioco base che il gruppo non tocca, per la mano principale data, in pixel dell'interfaccia con
     * numeri decimali (le grandezze del controllo del design, anteprime.py; la tabella "Disposizione dell'HUD" del
     * piano): il braccio della tuta di quella mano, la fascia della barra degli oggetti con le barre di stato, lo slot
     * della seconda mano di quella mano, le righe della chat e il mirino.
     */
    private static List<Area> vanilla(int w, int h, boolean mainRight) {
        return List.of(arm(w, h, mainRight), hotbarBand(w, h), offhand(w, h, mainRight), chat(h), crosshair(w, h));
    }

    /**
     * Il braccio della tuta in prima persona (D2): misurato solo a 960x540 con la scala 2, qui scalato con l'altezza
     * come il disegno in prima persona ({@code k = h / 270}), dal fondo, alto {@code 102,5 k}, dal bordo
     * {@code w/2 + 97 k} al lato della mano. Una deduzione del design (§5.12 e §12), che gli scatti provano solo a
     * 960x540.
     */
    private static Area arm(int w, int h, boolean mainRight) {
        double k = h / 270.0;
        double edge = w / 2.0 + 97 * k;
        double top = h - 102.5 * k;
        return mainRight ? new Area("the right arm", edge, top, w, h) : new Area("the left arm", 0, top, w - edge, h);
    }

    /** La barra degli oggetti con le barre di stato: 380x100+290+440 a 960x540 (design §12). */
    private static Area hotbarBand(int w, int h) {
        return new Area("the hotbar band", w / 2.0 - 95, h - 50, w / 2.0 + 95, h);
    }

    /** Lo slot della seconda mano, dal lato opposto alla mano principale. */
    private static Area offhand(int w, int h, boolean mainRight) {
        return mainRight
                ? new Area("the offhand slot of the right hand", w / 2.0 - 120, h - 23, w / 2.0 - 91, h)
                : new Area("the offhand slot of the left hand", w / 2.0 + 91, h - 23, w / 2.0 + 120, h);
    }

    /** Le dieci righe della chat, larghe 332, che finiscono 40 pixel sopra il fondo. */
    private static Area chat(int h) {
        return new Area("the chat lines", 0, h - 130, 332, h - 40);
    }

    private static Area crosshair(int w, int h) {
        return new Area("the crosshair", w / 2.0 - 15, h / 2.0 - 15, w / 2.0 + 15, h / 2.0 + 15);
    }

    /** Un elemento del gioco base: un rettangolo con i bordi decimali, x1 e y1 esclusi. */
    private record Area(String name, double x0, double y0, double x1, double y1) {
        /** Vero se il rettangolo dell'HUD ha almeno un pixel in comune con l'elemento. */
        boolean touches(Rect r) {
            return r.x0() < x1 && x0 < r.x1() && r.y0() < y1 && y0 < r.y1();
        }
    }
}
