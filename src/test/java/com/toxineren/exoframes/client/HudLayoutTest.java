package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.HudLayout.Rect;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * I rettangoli dell'HUD ({@link HudLayout}, design §5.12, "Dove" ed "Elementi", e §12): il gruppo nell'angolo in basso
 * opposto alla mano principale, o in alto a sinistra in un'interfaccia stretta, lontano dal braccio, dalla barra degli
 * oggetti con le barre di stato, dallo slot della seconda mano, dalle righe della chat e dal mirino alle sette
 * grandezze del design; gli elementi dentro i loro sottogruppi e senza sovrapposizioni; l'interno mai specchiato; gli
 * angoli della vignetta; i riquadri a 960x540 con la scala 2 che usano tools/client/arm_measure.sh e m4c_shots.sh; i
 * disegni della texture dell'HUD (parte 4d), letti dalla PNG del mod: ogni texel in un disegno con il suo nome, nessun
 * magenta, la grandezza dei rettangoli che riempiono e i colori esatti della legenda, scelti per ogni colore che
 * {@link HudModel} puo' dare a un pezzo e all'energia.
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
    /** "100 %", la percentuale piu' larga senza avviso (le larghezze sono del carattere del gioco, come Font.width). */
    private static final int WIDEST_PERCENT = 27;
    /** "9 %", la percentuale piu' larga con l'avviso: con LOW la carica e' sotto il 10 %, con EMPTY e' zero. */
    private static final int WIDEST_PERCENT_WITH_WARNING = 15;
    /** Il segno di moltiplicazione e una cifra: le celle di scorta da 0 a 9. Misurata nel gioco ("x0" e' largo 12). */
    private static final int ONE_DIGIT_SPARE = 12;
    /**
     * Il segno di moltiplicazione e due cifre: le celle di scorta da 10 a 99. Misurata nel gioco con la grafica della
     * parte 4d ("x10" e' largo 18: con dieci celle cariche nell'inventario, su 1.21.1-neoforge, la carica rossa
     * dell'icona sta da 130 a 133 nello scatto, cioe' l'icona a 41 nel sottogruppo dell'energia e il testo a 47).
     */
    private static final int TWO_DIGIT_SPARE = 18;
    /** "Senza cella", il testo senza cella piu' largo ("No cell" e' 33). */
    private static final int NO_CELL_TEXT = 57;
    /** La durata massima del Rottame nella config predefinita. */
    private static final int SCRAP_MAX = 180;

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

        assertEquals(53, HudLayout.spareTextX(ONE_DIGIT_SPARE));
        assertEquals(47, cellIconX(ONE_DIGIT_SPARE));
        assertEquals(47, HudLayout.spareTextX(TWO_DIGIT_SPARE));
        assertEquals(41, cellIconX(TWO_DIGIT_SPARE));

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

        // senza cella il testo prende la riga della barra, vuota: nella riga del testo non entra (design §5.12, riga
        // "percentuale")
        Rect noCell = text(HudLayout.NO_CELL_X, HudLayout.NO_CELL_Y, NO_CELL_TEXT);
        assertEquals(frame.x0(), noCell.x0());
        assertEquals(frame.y0(), noCell.y0());
        assertTrue(noCell.x1() <= frame.x1(), "the no-cell text " + noCell + " ends after " + frame.x1());
        assertTrue(energy.contains(noCell));
        assertFalse(noCell.intersects(textRow));
    }

    @Test
    void silhouetteAndWarningRectsAreFixed() {
        // a grafica finita (parte 4d, design §14 "Per la 4d"): i rettangoli che i disegni della texture riempiono
        assertEquals(new Rect(8, 3, 14, 9), HudLayout.piece(PieceSlot.HELMET));
        assertEquals(new Rect(7, 10, 15, 21), HudLayout.piece(PieceSlot.TORSO));
        assertEquals(new Rect(3, 10, 6, 21), HudLayout.piece(PieceSlot.LEFT_ARM));
        assertEquals(new Rect(16, 10, 19, 21), HudLayout.piece(PieceSlot.RIGHT_ARM));
        assertEquals(new Rect(7, 22, 10, 33), HudLayout.piece(PieceSlot.LEFT_LEG));
        assertEquals(new Rect(12, 22, 15, 33), HudLayout.piece(PieceSlot.RIGHT_LEG));
        assertEquals(new Rect(2, 8, 10, 16), HudLayout.WARNING);
    }

    @Test
    void rectIntersectsAndContainsSayYesAndNo() {
        Rect a = new Rect(0, 0, 4, 4);
        assertTrue(a.intersects(new Rect(3, 3, 6, 6)), "one pixel in common");
        assertFalse(a.intersects(new Rect(4, 0, 6, 4)), "touching edges, no pixel in common");
        assertTrue(a.contains(new Rect(1, 1, 4, 4)));
        assertFalse(a.contains(new Rect(1, 1, 5, 4)), "one column outside");
        assertFalse(a.contains(new Rect(-1, 0, 2, 2)), "one column outside on the left");
    }

    @Test
    void spritesCoverTheHudTextureAndCarryTheLegend() {
        SpriteSheet.assertMagentaRuleWorks();
        SpriteSheet hud = SpriteSheet.load("hud.png");
        assertEquals(HudLayout.TEXTURE_W, hud.width());
        assertEquals(HudLayout.TEXTURE_H, hud.height());
        hud.assertCoveredExactlyBy(SpriteSheet.constants(HudLayout.class));

        // ogni disegno ha la grandezza del rettangolo che riempie
        assertEquals(HudLayout.GROUP_W, HudLayout.PIASTRA.width());
        assertEquals(HudLayout.GROUP_H, HudLayout.PIASTRA.height());
        assertEquals(HudLayout.ENERGY_W, HudLayout.PIASTRA_BARRA.width());
        assertEquals(HudLayout.GROUP_H, HudLayout.PIASTRA_BARRA.height());
        assertEquals(HudLayout.BAR_FRAME.width(), HudLayout.BARRA_CORNICE.width());
        assertEquals(HudLayout.BAR_FRAME.height(), HudLayout.BARRA_CORNICE.height());
        for (Sprite figure : List.of(HudLayout.SAGOMA_INTEGRO, HudLayout.SAGOMA_DANNEGGIATO, HudLayout.SAGOMA_CRITICO,
                HudLayout.SAGOMA_ROTTO, HudLayout.SAGOMA_ASSENTE)) {
            assertEquals(HudLayout.SILHOUETTE_W, figure.width());
            assertEquals(HudLayout.GROUP_H, figure.height());
        }
        for (Sprite icon : List.of(HudLayout.CELLA, HudLayout.CELLA_SPENTA)) {
            assertEquals(HudLayout.CELL_ICON_W, icon.width());
            assertEquals(HudLayout.CELL_ICON_H, icon.height());
        }

        // la sagoma: ogni colore che il modello da' a un pezzo, cioe' quello di ogni condizione con la regola di
        // HudModel.of e quello del pezzo non montato, sceglie la figura con quel pieno e quel contorno, come in SuitHud
        for (HudModel.Paint paint : modelPaints()) {
            for (PieceSlot slot : PieceSlot.values()) {
                Sprite piece = HudLayout.silhouettePiece(HudLayout.figure(paint), slot);
                Rect r = HudLayout.piece(slot);
                assertEquals(r.width(), piece.width(), slot + " width");
                assertEquals(r.height(), piece.height(), slot + " height");
                String what = slot + " with " + Integer.toHexString(paint.fill());
                assertEquals(paint.fill(), hud.argb(piece, piece.width() / 2, piece.height() / 2), what + ", centre");
                assertEquals(paint.outline(), hud.argb(piece, piece.width() / 2, piece.height() - 1),
                        what + ", bottom outline");
                for (int y = 0; y < piece.height(); y++) {
                    for (int x = 0; x < piece.width(); x++) {
                        int c = hud.argb(piece, x, y);
                        assertTrue(SpriteSheet.alpha(c) == 0 || c == paint.fill() || c == paint.outline(),
                                what + ": texel " + x + "," + y + " is " + Integer.toHexString(c));
                    }
                }
            }
        }
        assertThrows(IllegalArgumentException.class,
                () -> HudLayout.figure(new HudModel.Paint(HudModel.WHITE, HudModel.WHITE)));

        // la barra e l'avviso: per ogni stato dell'energia del server, e prima del primo pacchetto con gli indicatori,
        // lungo un lampeggio intero, il colore che il modello da' all'energia sceglie un riempimento con la prima riga
        // piu' chiara e, quando il modello accende l'avviso, un avviso di quel colore, come in SuitHud
        Set<Integer> fills = new LinkedHashSet<>();
        Set<Integer> warnings = new LinkedHashSet<>();
        Set<Optional<EnergyState>> warned = new HashSet<>();
        for (Optional<EnergyState> energy : energyStates()) {
            for (long ticks = 0; ticks < 2 * HudModel.BLINK_TICKS; ticks++) {
                HudModel model = HudModel.of(HudLevel.FULL, withCell(), energy, 0, ticks);
                fills.add(model.energyColor());
                if (model.warningOn()) {
                    warnings.add(model.energyColor());
                    warned.add(energy);
                }
            }
        }
        assertEquals(Set.of(Optional.of(EnergyState.LOW), Optional.of(EnergyState.EMPTY)), warned,
                "the states that light the warning");
        for (int colour : fills) {
            Sprite fill = HudLayout.barFill(colour);
            assertEquals(HudLayout.BAR_FILL.width(), fill.width());
            assertEquals(HudLayout.BAR_FILL.height(), fill.height());
            String what = "the fill " + Integer.toHexString(colour);
            hud.assertAll(what + ", first row", fill, 0, 0, fill.width(), 1, HudModel.lighter(colour));
            hud.assertAll(what, fill, 0, 1, fill.width(), fill.height() - 1, colour);
        }
        assertThrows(IllegalArgumentException.class, () -> HudLayout.barFill(HudModel.YELLOW));
        for (int colour : warnings) {
            Sprite warning = HudLayout.warning(colour);
            assertEquals(HudLayout.WARNING.width(), warning.width());
            assertEquals(HudLayout.WARNING.height(), warning.height());
            hud.assertAll("the warning " + Integer.toHexString(colour) + ", bottom row", warning, 0,
                    warning.height() - 1, warning.width(), 1, colour);
        }
        assertThrows(IllegalArgumentException.class, () -> HudLayout.warning(HudModel.GREEN));
        assertEquals(HudLayout.CELLA_SPENTA, HudLayout.cellIcon(0));
        assertEquals(HudLayout.CELLA, HudLayout.cellIcon(1));
    }

    @Test
    void spritePartsStayInsideTheirSprite() {
        Sprite figure = HudLayout.SAGOMA_INTEGRO;
        assertEquals(new Sprite(figure.u() + 8, figure.v() + 3, 6, 6),
                HudLayout.silhouettePiece(figure, PieceSlot.HELMET));
        assertEquals(new Sprite(69, 46, 3, 6), HudLayout.BARRA_VERDE.left(3));
        assertThrows(IllegalArgumentException.class, () -> HudLayout.BARRA_VERDE.left(0));
        assertThrows(IllegalArgumentException.class, () -> HudLayout.BARRA_VERDE.left(HudModel.FILL_MAX + 1));
        assertThrows(IllegalArgumentException.class, () -> figure.part(20, 0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new Sprite(0, 0, 0, 1));
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
        Rect fill = HudLayout.BAR_FILL.offset(right.x0(), right.y0());
        assertEquals("122x12+52+508", screen(fill));
        // il corpo della barra: le cinque righe del colore pieno, sotto la prima, piu' chiara (BAR_FILL_BODY_RIGHT di
        // m4c_shots.sh; design §14 "Per la 4d")
        assertEquals("122x10+52+510", screen(new Rect(fill.x0(), fill.y0() + 1, fill.x1(), fill.y1())));
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

    /**
     * Tutti i colori che {@link HudModel#of} da' a un pezzo della sagoma: per ogni condizione un elmo con una durata
     * che la da' (cercata da 0 alla durata massima del Rottame), con gli altri cinque slot non montati.
     */
    private static Set<HudModel.Paint> modelPaints() {
        Set<HudModel.Paint> paints = new LinkedHashSet<>();
        Set<PieceCondition> reached = EnumSet.noneOf(PieceCondition.class);
        for (int durability = 0; durability <= SCRAP_MAX; durability++) {
            if (reached.add(PieceCondition.of(durability, SCRAP_MAX))) {
                FrameState state = new FrameState(ExoframesConfig.defaults());
                state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, SCRAP_MAX, durability));
                paints.addAll(HudModel.of(HudLevel.FULL, state, Optional.empty(), 0, 0).silhouette().values());
            }
        }
        assertEquals(EnumSet.allOf(PieceCondition.class), reached, "every piece condition");
        return paints;
    }

    /** Ogni stato dell'energia del server, e nessuno: prima del primo pacchetto con gli indicatori. */
    private static List<Optional<EnergyState>> energyStates() {
        List<Optional<EnergyState>> states = new ArrayList<>();
        for (EnergyState state : EnergyState.values()) {
            states.add(Optional.of(state));
        }
        states.add(Optional.empty());
        return states;
    }

    /** Uno stato con la config predefinita e una cella a meta', senza pezzi: la barra ha un riempimento. */
    private static FrameState withCell() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.insertCell(new EnergyCell(10_000, 5_000));
        return state;
    }

    private static List<int[]> allSizes() {
        List<int[]> sizes = new ArrayList<>(List.of(WIDE_SIZES));
        sizes.addAll(List.of(NARROW_SIZES));
        return sizes;
    }

    /**
     * Gli elementi del gioco base che il gruppo non tocca, per la mano principale data, in pixel dell'interfaccia con
     * numeri decimali (le grandezze del controllo del design, §12 e anteprime.py): il braccio della tuta di quella
     * mano, la fascia della barra degli oggetti con le barre di stato, lo slot della seconda mano di quella mano, le
     * righe della chat e il mirino.
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
