package com.toxineren.exoframes.client;

import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.station.StationView;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * La disposizione della schermata della stazione (design §5.3), in Java puro, senza classi di Minecraft: i
 * rettangoli dei widget della parte 4a-1, i colori, il colore di una condizione, il giro dell'anteprima e la vista
 * della tuta dalla vista del telaio. I rettangoli sono in pixel dell'interfaccia, relativi all'angolo in alto a
 * sinistra della schermata, con il bordo destro e quello in basso esclusi. I colori sono ARGB e tutti opachi: il
 * mondo dietro la schermata non deve trasparire, e nessuno e' magenta per tools/client/arm_measure.sh (il magenta,
 * nella schermata, e' solo quello della tuta nell'anteprima). Lo controlla StationLayoutTest.
 */
final class StationLayout {
    /** Larghezza della schermata. */
    static final int WIDTH = 316;
    /** Altezza della schermata. */
    static final int HEIGHT = 232;

    /** Il titolo, con l'etichetta in alto a sinistra. */
    static final Rect TITLE = new Rect(8, 6, 140, 15);
    /** Il pannello del telaio, a sinistra: i sei posti dei pezzi e la cella. */
    static final Rect FRAME_PANEL = new Rect(7, 17, 73, 135);
    /**
     * I posti dei pezzi nel pannello del telaio, in forma di corpo visto di fronte come l'anteprima all'inizio (il
     * braccio destro a sinistra): ciascuno e' uno slot di 18x18 dentro un bordo di 1 pixel.
     */
    static final Map<PieceSlot, Rect> PIECE_SLOTS = pieceSlots();
    /** Il posto della cella, sotto i pezzi. */
    static final Rect CELL = new Rect(19, 101, 39, 121);
    /** Il pannello centrale, con l'intestazione della scelta. */
    static final Rect PIECE_PANEL = new Rect(77, 17, 211, 135);
    /** Il nome della scelta: il pezzo, la cella o tutti i pezzi. */
    static final Rect NAME = new Rect(81, 22, 191, 31);
    /** La condizione del pezzo scelto, nel suo colore. */
    static final Rect CONDITION = new Rect(81, 34, 150, 43);
    /** La durata e la durata massima del pezzo scelto, allineate a destra. */
    static final Rect DURABILITY_NUMBERS = new Rect(152, 34, 207, 43);
    /** La barra della durata del pezzo scelto (o della carica della cella). */
    static final Rect DURABILITY_BAR = new Rect(81, 44, 207, 49);
    /** Il pannello dell'anteprima 3D della tuta. */
    static final Rect PREVIEW_PANEL = new Rect(215, 17, 309, 135);
    /** Il ritaglio dell'anteprima, dentro il suo pannello: la tuta non ne esce, e trascinarci dentro la gira. */
    static final Rect PREVIEW_CLIP = new Rect(216, 18, 308, 134);
    /** L'etichetta dell'inventario del giocatore. */
    static final Rect INVENTORY_LABEL = new Rect(8, 139, 80, 148);
    /** Le tre file dell'inventario principale del giocatore, cornici degli slot comprese. */
    static final Rect INVENTORY = new Rect(7, 149, 169, 203);
    /** La barra degli oggetti del giocatore, cornici degli slot comprese. */
    static final Rect HOTBAR = new Rect(7, 207, 169, 225);
    /** Il pulsante di prova (il numero 102), dove andra' "salva" del preset 3. */
    static final Rect TEST_BUTTON = new Rect(283, 186, 301, 202);

    /** Lo sfondo della schermata. */
    static final int BACKGROUND = 0xFF1B1E23;
    /** I pannelli. */
    static final int PANEL = 0xFF262B32;
    /** Il bordo della schermata: il bronzo del Rottame. */
    static final int EDGE = 0xFF8A6F52;
    /** Lo sfondo dell'anteprima. */
    static final int PREVIEW_BG = 0xFF14171B;
    /** Il fondo di uno slot: quelli dell'inventario e i posti del pannello del telaio con un pezzo o la cella. */
    static final int SLOT_BG = 0xFF111317;
    /** La cornice degli slot dell'inventario. */
    static final int SLOT_EDGE = 0xFF41464E;
    /** Il fondo di un posto del pannello del telaio senza pezzo (o senza cella). */
    static final int EMPTY_BG = 0xFF0B0C0E;
    /** Il bordo di un posto senza pezzo (o senza cella): il grigio dei pezzi non montati dell'HUD (design §5.12). */
    static final int EMPTY_EDGE = 0xFF464646;
    /** Il rosso della redstone: il bordo del posto della cella e la barra della carica. */
    static final int CELL_RED = 0xFFC81E1E;
    /** Il secondo bordo, fuori dal primo, del posto scelto. */
    static final int SELECTION = 0xFFFFFFFF;
    /** Il fondo della barra della durata. */
    static final int BAR_BG = 0xFF0C0D0F;
    /** Il testo. */
    static final int TEXT = 0xFFE6E6E6;
    /** Il testo spento: uno slot vuoto, le frecce dell'anteprima. */
    static final int TEXT_DIM = 0xFF9AA0A6;
    /** Il pulsante di prova finche' il server non ha confermato nessuna pressione. */
    static final int TEST_IDLE = 0xFF3A3F46;
    /** Il pulsante di prova dopo la prima pressione confermata dal server. */
    static final int TEST_DONE = 0xFF2E8B57;
    /** Un pezzo integro: il verde della legenda dell'HUD (design §5.12). */
    static final int CONDITION_INTACT = 0xFF55FF55;
    /** Un pezzo danneggiato: il giallo della legenda. */
    static final int CONDITION_DAMAGED = 0xFFFFFF55;
    /** Un pezzo critico: il rosso della legenda. */
    static final int CONDITION_CRITICAL = 0xFFFF5555;
    /** Un pezzo rotto: il grigio della legenda. */
    static final int CONDITION_BROKEN = 0xFF8A8A8A;

    /** Tutti i colori che la schermata disegna da se', per il controllo del magenta e dell'opacita'. */
    static final List<Integer> ALL_COLOURS = List.of(BACKGROUND, PANEL, EDGE, PREVIEW_BG, SLOT_BG, SLOT_EDGE,
            EMPTY_BG, EMPTY_EDGE, CELL_RED, SELECTION, BAR_BG, TEXT, TEXT_DIM, TEST_IDLE, TEST_DONE,
            CONDITION_INTACT, CONDITION_DAMAGED, CONDITION_CRITICAL, CONDITION_BROKEN);

    /** I gradi di giro dell'anteprima per pixel dell'interfaccia trascinato (design §5.4). */
    static final double DEGREES_PER_PIXEL = 1.5;

    private StationLayout() {
    }

    /** L'ascissa dell'angolo della schermata, centrata in un'interfaccia larga {@code guiWidth}. */
    static int leftPos(int guiWidth) {
        return (guiWidth - WIDTH) / 2;
    }

    /** L'ordinata dell'angolo della schermata, centrata in un'interfaccia alta {@code guiHeight}. */
    static int topPos(int guiHeight) {
        return (guiHeight - HEIGHT) / 2;
    }

    /** Il colore della condizione di un pezzo, con la legenda dell'HUD (design §5.12). */
    static int conditionColor(PieceCondition condition) {
        return switch (Objects.requireNonNull(condition, "condition must not be null")) {
            case INTACT -> CONDITION_INTACT;
            case DAMAGED -> CONDITION_DAMAGED;
            case CRITICAL -> CONDITION_CRITICAL;
            case BROKEN -> CONDITION_BROKEN;
        };
    }

    /**
     * L'angolo dell'anteprima dopo un trascinamento di {@code dragX} pixel dell'interfaccia: {@link #DEGREES_PER_PIXEL}
     * gradi per pixel, riportato in [0, 360). Uno spostamento non finito (che il gioco non manda) lascia l'angolo dove
     * sta, cosi' l'anteprima non si perde.
     */
    static float rotate(float angleDegrees, double dragX) {
        double drag = Double.isFinite(dragX) ? dragX : 0.0;
        double turned = (angleDegrees + DEGREES_PER_PIXEL * drag) % 360.0;
        float angle = (float) (turned < 0.0 ? turned + 360.0 : turned);
        // un valore appena sotto 360 diventa 360 nel passaggio a float: e' di nuovo 0
        return angle >= 360f ? 0f : angle;
    }

    /**
     * La vista della tuta dell'anteprima dalla vista del telaio (design §5.4): per ogni pezzo montato lo stato visivo
     * e' la posizione della sua condizione (integro 0, danneggiato 1, critico 2, rotto 3), e un colore
     * {@link Appearance#UNPAINTED} prende il colore predefinito del livello da {@link TierPalette}; gli slot senza
     * pezzo restano fuori, come in {@link SuitView#of}.
     */
    static SuitView suitView(StationView view) {
        Map<PieceSlot, SuitView.PieceView> pieces = new EnumMap<>(PieceSlot.class);
        view.pieces().forEach((slot, info) -> pieces.put(slot, new SuitView.PieceView(
                PieceCondition.of(info.durability(), info.maxDurability()).ordinal(),
                colour(info.primary(), info.tier(), TierPalette::primary),
                colour(info.secondary(), info.tier(), TierPalette::secondary))));
        return new SuitView(pieces);
    }

    private static int colour(int painted, Tier tier, ToIntFunction<Tier> tierDefault) {
        return painted == Appearance.UNPAINTED ? tierDefault.applyAsInt(tier) : painted;
    }

    /**
     * Il colore di un pulsante sotto il mouse: ogni canale si avvicina di un quarto al bianco, l'alfa resta. Un colore
     * che non e' magenta non lo diventa.
     */
    static int lighter(int argb) {
        int alpha = argb >>> 24;
        int red = lighterChannel((argb >> 16) & 0xFF);
        int green = lighterChannel((argb >> 8) & 0xFF);
        int blue = lighterChannel(argb & 0xFF);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int lighterChannel(int channel) {
        return channel + (255 - channel) / 4;
    }

    private static Map<PieceSlot, Rect> pieceSlots() {
        Map<PieceSlot, Rect> slots = new EnumMap<>(PieceSlot.class);
        slots.put(PieceSlot.HELMET, new Rect(30, 21, 50, 41));
        slots.put(PieceSlot.RIGHT_ARM, new Rect(9, 43, 29, 63));
        slots.put(PieceSlot.TORSO, new Rect(30, 43, 50, 63));
        slots.put(PieceSlot.LEFT_ARM, new Rect(51, 43, 71, 63));
        slots.put(PieceSlot.RIGHT_LEG, new Rect(19, 65, 39, 85));
        slots.put(PieceSlot.LEFT_LEG, new Rect(41, 65, 61, 85));
        return Collections.unmodifiableMap(slots);
    }

    /** Un rettangolo [x0, x1) x [y0, y1) in pixel dell'interfaccia, relativo all'angolo della schermata. */
    record Rect(int x0, int y0, int x1, int y1) {
        /** Vero se il punto, relativo all'angolo della schermata, sta nel rettangolo. */
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }

        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }

        /** Il rettangolo allargato di {@code by} pixel per lato, come il bordo della scelta attorno a un posto. */
        Rect grow(int by) {
            return new Rect(x0 - by, y0 - by, x1 + by, y1 + by);
        }
    }
}
