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
 * elementi sono relativi al loro sottogruppo, uguali per le due mani: l'interno non si specchia mai. Lo prova
 * HudLayoutTest, anche contro gli elementi del gioco base alle sette grandezze del design.
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
     * La percentuale piu' larga, con o senza avviso: l'area del controllo sugli scatti, che SuitHud non usa (tabella
     * "Disposizione dell'HUD" del piano, dalla riga del testo di design §5.12).
     */
    static final Rect PERCENT_AREA = new Rect(2, 8, 31, 17);
    /** Il bordo destro, escluso, di "xN" delle celle di scorta (design §5.12, riga "celle di scorta": "a (65,8)"). */
    static final int SPARE_RIGHT = 65;
    /** La larghezza dell'icona della cella (design §5.12, riga "celle di scorta": "un'icona di cella 5x8"). */
    static final int CELL_ICON_W = 5;
    /** L'altezza dell'icona della cella (design §5.12, riga "celle di scorta": 5x8). */
    static final int CELL_ICON_H = 8;
    /**
     * Icona e "xN" fino a due cifre: l'area del controllo sugli scatti, che SuitHud non usa (tabella "Disposizione
     * dell'HUD" del piano, dalla riga del testo di design §5.12).
     */
    static final Rect SPARE_AREA = new Rect(40, 8, 65, 17);
    /** La cornice della barra, 63x8 a (2,20) del sottogruppo dell'energia (design §5.12, "Elementi", riga "barra"). */
    static final Rect BAR_FRAME = new Rect(2, 20, 65, 28);
    /** Il riempimento della barra, 61x6 dentro la cornice (design §5.12, "Elementi", riga "barra"). */
    static final Rect BAR_FILL = new Rect(3, 21, 64, 27);
    /** L'ascissa del testo senza cella, al posto della barra vuota (Decisione 1 del piano, su design §5.12). */
    static final int NO_CELL_X = 2;
    /** L'ordinata del testo senza cella: la riga della barra (Decisione 1 del piano, su design §5.12). */
    static final int NO_CELL_Y = 20;

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
