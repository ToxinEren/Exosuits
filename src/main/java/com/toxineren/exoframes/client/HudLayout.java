package com.toxineren.exoframes.client;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.frame.HudLevel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * La disposizione dell'HUD della tuta (design §5.12, "Dove" ed "Elementi"; i numeri e il controllo delle
 * sovrapposizioni in §12 e in anteprime/2026-11-19-m4/layout.py), in Java puro, senza classi di Minecraft: il gruppo
 * per mano principale e grandezza dell'interfaccia, i suoi due sottogruppi, gli elementi dentro di loro e gli angoli
 * della vignetta. I rettangoli sono in pixel dell'interfaccia, con il bordo destro e quello in basso esclusi; gli
 * elementi sono relativi al loro sottogruppo, uguali per le due mani: l'interno non si specchia mai. Poi i disegni
 * dell'HUD nella loro texture (parte 4d, stile A: {@code textures/gui/hud.png}, scritta da tools/art/gui.py), con i
 * nomi della mappa del generatore, e quale disegno va con i colori di {@link HudModel}: i disegni hanno la grandezza
 * dei rettangoli che riempiono e i colori esatti della legenda. Lo prova HudLayoutTest, anche contro gli elementi del
 * gioco base alle sette grandezze del design e contro la PNG del mod.
 */
final class HudLayout {
    /** La larghezza del gruppo (design §5.12, "Dove": "Un gruppo di 88x35 pixel dell'interfaccia"). */
    static final int GROUP_W = 88;
    /** L'altezza del gruppo (design §5.12, "Dove": 88x35). */
    static final int GROUP_H = 35;
    /** La larghezza del sottogruppo della sagoma (design §5.12, "Dove": "la sagoma, 21x35, verso il bordo"). */
    static final int SILHOUETTE_W = 21;
    /** La larghezza del sottogruppo dell'energia (design §5.12, "Dove": "l'energia, 67x35, verso l'interno"). */
    static final int ENERGY_W = 67;
    /** La distanza del gruppo dal bordo laterale, e in alto se stretta (design §5.12, "Dove": "a 2 pixel"). */
    static final int EDGE = 2;
    /**
     * L'ordinata del gruppo dal fondo: {@code y0 = h - 37}, 3 pixel sotto l'ultima riga della chat, che finisce a
     * {@code h - 40} (design §5.12, "Dove").
     */
    static final int ABOVE_BOTTOM = 37;
    /**
     * Sotto questa larghezza accanto allo slot della seconda mano non c'e' posto: {@code w/2 - 124 < 88}, cioe'
     * {@code w < 424} (design §5.12, "Dove").
     */
    static final int NARROW_WIDTH = 424;
    /** La larghezza di un angolo della vignetta (design §5.12, "Elementi", riga "vignetta": 72x40). */
    static final int VIGNETTE_W = 72;
    /** L'altezza di un angolo della vignetta (design §5.12, "Elementi", riga "vignetta": 72x40). */
    static final int VIGNETTE_H = 40;

    /** L'avviso, 8x8 a (2,8) del sottogruppo dell'energia (design §5.12, "Elementi", riga "avviso"). */
    static final Rect WARNING = new Rect(2, 8, 10, 16);
    /** L'ascissa della percentuale senza avviso (design §5.12, "Elementi", riga "percentuale": "da (2,8)"). */
    static final int TEXT_X = 2;
    /** L'ascissa della percentuale dopo l'avviso (design §5.12, riga "percentuale": "o da (12,8) dopo l'avviso"). */
    static final int TEXT_X_AFTER_WARNING = 12;
    /** L'ordinata della riga del testo, da 8 a 17 con l'ombra (design §5.12, riga "percentuale": "da (2,8)"). */
    static final int TEXT_Y = 8;
    /**
     * La percentuale piu' larga, con o senza avviso: l'area del controllo sugli scatti, che SuitHud non usa (dalla
     * riga del testo di design §5.12, con la larghezza di "100 %" nel carattere del gioco).
     */
    static final Rect PERCENT_AREA = new Rect(2, 8, 31, 17);
    /** Il bordo destro, escluso, di "xN" delle celle di scorta (design §5.12, riga "celle di scorta": "a (65,8)"). */
    static final int SPARE_RIGHT = 65;
    /** La larghezza dell'icona della cella (design §5.12, riga "celle di scorta": "un'icona di cella 5x8"). */
    static final int CELL_ICON_W = 5;
    /** L'altezza dell'icona della cella (design §5.12, riga "celle di scorta": 5x8). */
    static final int CELL_ICON_H = 8;
    /**
     * Icona e "xN" fino a due cifre: l'area del controllo sugli scatti, che SuitHud non usa (dalla riga del testo di
     * design §5.12, con la larghezza di due cifre nel carattere del gioco).
     */
    static final Rect SPARE_AREA = new Rect(40, 8, 65, 17);
    /** La cornice della barra, 63x8 a (2,20) del sottogruppo dell'energia (design §5.12, "Elementi", riga "barra"). */
    static final Rect BAR_FRAME = new Rect(2, 20, 65, 28);
    /** Il riempimento della barra, 61x6 dentro la cornice (design §5.12, "Elementi", riga "barra"). */
    static final Rect BAR_FILL = new Rect(3, 21, 64, 27);
    /**
     * L'ascissa del testo senza cella, al posto della barra vuota: nella riga del testo non entra (design §5.12, riga
     * "percentuale").
     */
    static final int NO_CELL_X = 2;
    /** L'ordinata del testo senza cella: la riga della barra (design §5.12, riga "percentuale": "da (2,20)"). */
    static final int NO_CELL_Y = 20;

    /** La larghezza della texture dei disegni dell'HUD (tools/art/gui.py, {@code textures/gui/hud.png}). */
    static final int TEXTURE_W = 256;
    /** L'altezza della texture dei disegni dell'HUD. */
    static final int TEXTURE_H = 128;

    /** La piastra di fondo con il livello FULL: tutto il gruppo. */
    static final Sprite PIASTRA = new Sprite(0, 0, 88, 35);
    /** La piastra con il livello BAR_ONLY: il solo sottogruppo dell'energia. */
    static final Sprite PIASTRA_BARRA = new Sprite(0, 36, 67, 35);
    /**
     * La figura della sagoma con i sei pezzi integri: verde 55FF55, il contorno a un terzo (un pezzo: silhouettePiece).
     */
    static final Sprite SAGOMA_INTEGRO = new Sprite(90, 0, 21, 35);
    /** La figura della sagoma con i sei pezzi danneggiati: giallo FFFF55 (un pezzo: silhouettePiece). */
    static final Sprite SAGOMA_DANNEGGIATO = new Sprite(112, 0, 21, 35);
    /** La figura della sagoma con i sei pezzi critici: rosso FF5555 (un pezzo: silhouettePiece). */
    static final Sprite SAGOMA_CRITICO = new Sprite(134, 0, 21, 35);
    /** La figura della sagoma con i sei pezzi rotti: grigio 8A8A8A (un pezzo: silhouettePiece). */
    static final Sprite SAGOMA_ROTTO = new Sprite(156, 0, 21, 35);
    /**
     * La figura della sagoma con i sei pezzi non montati: il fondo 5A000000 e il contorno 464646 (un pezzo:
     * silhouettePiece).
     */
    static final Sprite SAGOMA_ASSENTE = new Sprite(178, 0, 21, 35);
    /** La cornice della barra (BAR_FRAME). */
    static final Sprite BARRA_CORNICE = new Sprite(69, 37, 63, 8);
    /** Il riempimento della barra (BAR_FILL) con l'energia NORMAL: verde 55FF55; la prima riga piu' chiara. */
    static final Sprite BARRA_VERDE = new Sprite(69, 46, 61, 6);
    /** Il riempimento della barra (BAR_FILL) con l'energia LOW: rosso FF5555; la prima riga piu' chiara. */
    static final Sprite BARRA_ROSSA = new Sprite(69, 53, 61, 6);
    /** Il riempimento della barra (BAR_FILL) con l'energia EMPTY: grigio 8A8A8A; la prima riga piu' chiara. */
    static final Sprite BARRA_GRIGIA = new Sprite(69, 60, 61, 6);
    /**
     * Il riempimento della barra (BAR_FILL) con l'energia prima del primo pacchetto con gli indicatori: bianco; la
     * prima riga piu' chiara.
     */
    static final Sprite BARRA_BIANCA = new Sprite(69, 67, 61, 6);
    /** L'avviso (WARNING) con l'energia LOW: rosso FF5555. */
    static final Sprite AVVISO_ROSSA = new Sprite(134, 37, 8, 8);
    /** L'avviso (WARNING) con l'energia EMPTY: grigio 8A8A8A. */
    static final Sprite AVVISO_GRIGIA = new Sprite(143, 37, 8, 8);
    /** L'icona della cella di scorta, con almeno una cella. */
    static final Sprite CELLA = new Sprite(152, 37, 5, 8);
    /** L'icona della cella di scorta, spenta: nessuna cella. */
    static final Sprite CELLA_SPENTA = new Sprite(158, 37, 5, 8);

    /**
     * I pezzi della sagoma, relativi al suo sottogruppo, vista da dietro: il braccio sinistro a sinistra (design §5.12,
     * "Elementi", riga "sagoma").
     */
    private static final Map<PieceSlot, Rect> PIECES = pieces();

    private HudLayout() {
    }

    /** Vero se l'interfaccia e' piu' stretta di {@link #NARROW_WIDTH}: il gruppo va in alto a sinistra. */
    static boolean narrow(int guiWidth) {
        return guiWidth < NARROW_WIDTH;
    }

    /**
     * Il gruppo, 88x35, in un'interfaccia {@code w} per {@code h}: nell'angolo in basso opposto alla mano principale,
     * {@code (2, h - 37)} con la destra e {@code (w - 90, h - 37)} con la sinistra; in alto a sinistra, {@code (2, 2)},
     * per tutte e due le mani in un'interfaccia stretta ({@link #narrow}). E' il posto della domanda 8 del design (§9):
     * un cambio della posizione dell'HUD si fa qui.
     */
    static Rect group(int w, int h, boolean mainRight) {
        if (narrow(w)) {
            return new Rect(EDGE, EDGE, EDGE + GROUP_W, EDGE + GROUP_H);
        }
        int x0 = mainRight ? EDGE : w - EDGE - GROUP_W;
        int y0 = h - ABOVE_BOTTOM;
        return new Rect(x0, y0, x0 + GROUP_W, y0 + GROUP_H);
    }

    /**
     * Il sottogruppo della sagoma, 21x35, verso il bordo del gruppo: a sinistra con la disposizione della mano destra
     * (la mano destra, o un'interfaccia stretta con qualunque mano), a destra con la sinistra.
     */
    static Rect silhouetteGroup(int w, int h, boolean mainRight) {
        Rect g = group(w, h, mainRight);
        return rightHandLayout(w, mainRight) ? new Rect(g.x0(), g.y0(), g.x0() + SILHOUETTE_W, g.y1())
                : new Rect(g.x1() - SILHOUETTE_W, g.y0(), g.x1(), g.y1());
    }

    /** Il sottogruppo dell'energia, 67x35, verso l'interno: l'altra parte del gruppo. */
    static Rect energyGroup(int w, int h, boolean mainRight) {
        Rect g = group(w, h, mainRight);
        return rightHandLayout(w, mainRight) ? new Rect(g.x1() - ENERGY_W, g.y0(), g.x1(), g.y1())
                : new Rect(g.x0(), g.y0(), g.x0() + ENERGY_W, g.y1());
    }

    /**
     * La piastra di fondo del livello: con {@link HudLevel#FULL} tutto il gruppo, con {@link HudLevel#BAR_ONLY} il
     * solo sottogruppo dell'energia, che resta dov'e' con {@code FULL} (design §5.12, "Elementi", riga "piastra di
     * fondo").
     *
     * @throws IllegalArgumentException con {@link HudLevel#NONE}, che non disegna nulla
     */
    static Rect plate(int w, int h, boolean mainRight, HudLevel level) {
        return switch (Objects.requireNonNull(level, "level")) {
            case FULL -> group(w, h, mainRight);
            case BAR_ONLY -> energyGroup(w, h, mainRight);
            case NONE -> throw new IllegalArgumentException("the NONE level has no plate");
        };
    }

    /** Il rettangolo del pezzo nello slot, relativo al sottogruppo della sagoma, uguale per le due mani. */
    static Rect piece(PieceSlot slot) {
        return PIECES.get(Objects.requireNonNull(slot, "slot"));
    }

    /**
     * L'ascissa del testo "xN" delle celle di scorta largo {@code textWidth}, ombra compresa, allineato a destra a
     * {@link #SPARE_RIGHT}; l'icona della cella sta a {@code spareTextX - 1 - CELL_ICON_W}.
     */
    static int spareTextX(int textWidth) {
        return SPARE_RIGHT - textWidth;
    }

    /**
     * I quattro angoli della vignetta, 72x40, nell'ordine alto a sinistra, alto a destra, basso a sinistra, basso a
     * destra (design §5.12, "Elementi", riga "vignetta").
     */
    static List<Rect> vignetteCorners(int w, int h) {
        return List.of(new Rect(0, 0, VIGNETTE_W, VIGNETTE_H),
                new Rect(w - VIGNETTE_W, 0, w, VIGNETTE_H),
                new Rect(0, h - VIGNETTE_H, VIGNETTE_W, h),
                new Rect(w - VIGNETTE_W, h - VIGNETTE_H, w, h));
    }

    /**
     * La figura della sagoma con i colori di un pezzo ({@link HudModel#silhouette}): con il pieno della legenda di una
     * condizione la figura di quella condizione, con il pieno di un pezzo non montato quella dei non montati. Il pezzo
     * si ritaglia con {@link #silhouettePiece}; i texel della figura hanno esattamente il pieno e il contorno di
     * {@code paint} (HudLayoutTest).
     *
     * @throws IllegalArgumentException con un pieno che non e' della legenda
     */
    static Sprite figure(HudModel.Paint paint) {
        return switch (Objects.requireNonNull(paint, "paint").fill()) {
            case HudModel.GREEN -> SAGOMA_INTEGRO;
            case HudModel.YELLOW -> SAGOMA_DANNEGGIATO;
            case HudModel.RED -> SAGOMA_CRITICO;
            case HudModel.GREY -> SAGOMA_ROTTO;
            case HudModel.UNMOUNTED_FILL -> SAGOMA_ASSENTE;
            default -> throw new IllegalArgumentException("no figure for " + paint);
        };
    }

    /** Il pezzo dello slot nella figura data: il suo rettangolo del sottogruppo della sagoma ({@link #piece}). */
    static Sprite silhouettePiece(Sprite figure, PieceSlot slot) {
        Rect r = piece(slot);
        return figure.part(r.x0(), r.y0(), r.width(), r.height());
    }

    /**
     * Il riempimento della barra nel colore dell'energia ({@link HudModel#energyColor}): verde, rosso, grigio, o
     * bianco prima del primo pacchetto con gli indicatori; si disegna largo {@link HudModel#fillWidth}.
     *
     * @throws IllegalArgumentException con un colore che non e' dell'energia
     */
    static Sprite barFill(int energyColor) {
        return switch (energyColor) {
            case HudModel.GREEN -> BARRA_VERDE;
            case HudModel.RED -> BARRA_ROSSA;
            case HudModel.GREY -> BARRA_GRIGIA;
            case HudModel.WHITE -> BARRA_BIANCA;
            default -> throw new IllegalArgumentException("no bar fill for " + Integer.toHexString(energyColor));
        };
    }

    /**
     * L'avviso nel colore dell'energia: c'e' solo con {@code LOW}, rosso, e con {@code EMPTY}, grigio
     * ({@link HudModel#warningOn}).
     *
     * @throws IllegalArgumentException con un altro colore
     */
    static Sprite warning(int energyColor) {
        return switch (energyColor) {
            case HudModel.RED -> AVVISO_ROSSA;
            case HudModel.GREY -> AVVISO_GRIGIA;
            default -> throw new IllegalArgumentException("no warning for " + Integer.toHexString(energyColor));
        };
    }

    /** L'icona della cella di scorta: spenta senza celle di scorta, come "x0" e' grigio (scelta della 4d). */
    static Sprite cellIcon(int spareCells) {
        return spareCells > 0 ? CELLA : CELLA_SPENTA;
    }

    /**
     * Il rettangolo sullo schermo con la scala dell'interfaccia data, {@code LxA+X+Y}: la forma dei riquadri di
     * tools/client/arm_measure.sh e delle sequenze degli scatti (per i test e i commenti degli script).
     */
    static String screen(Rect r, int scale) {
        return scale * r.width() + "x" + scale * r.height() + "+" + scale * r.x0() + "+" + scale * r.y0();
    }

    /** Vero con la sagoma a sinistra: la mano destra, o un'interfaccia stretta con qualunque mano. */
    private static boolean rightHandLayout(int w, boolean mainRight) {
        return mainRight || narrow(w);
    }

    private static Map<PieceSlot, Rect> pieces() {
        Map<PieceSlot, Rect> pieces = new EnumMap<>(PieceSlot.class);
        pieces.put(PieceSlot.HELMET, new Rect(8, 3, 14, 9));
        pieces.put(PieceSlot.TORSO, new Rect(7, 10, 15, 21));
        pieces.put(PieceSlot.LEFT_ARM, new Rect(3, 10, 6, 21));
        pieces.put(PieceSlot.RIGHT_ARM, new Rect(16, 10, 19, 21));
        pieces.put(PieceSlot.LEFT_LEG, new Rect(7, 22, 10, 33));
        pieces.put(PieceSlot.RIGHT_LEG, new Rect(12, 22, 15, 33));
        return Collections.unmodifiableMap(pieces);
    }

    /** Un rettangolo [x0, x1) x [y0, y1) in pixel dell'interfaccia. */
    record Rect(int x0, int y0, int x1, int y1) {
        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }

        /** Vero se i due rettangoli hanno almeno un pixel in comune. */
        boolean intersects(Rect other) {
            return x0 < other.x1 && other.x0 < x1 && y0 < other.y1 && other.y0 < y1;
        }

        /** Vero se l'altro rettangolo sta tutto dentro questo. */
        boolean contains(Rect other) {
            return other.x0 >= x0 && other.y0 >= y0 && other.x1 <= x1 && other.y1 <= y1;
        }

        /** Il rettangolo spostato di {@code dx} e {@code dy}: un elemento dal suo sottogruppo all'interfaccia. */
        Rect offset(int dx, int dy) {
            return new Rect(x0 + dx, y0 + dy, x1 + dx, y1 + dy);
        }
    }
}
