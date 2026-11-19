package com.toxineren.exoframes.client;

/**
 * La disposizione della schermata della stazione (design §5.3): rettangoli e colori, in Java puro, senza classi di
 * Minecraft. I rettangoli sono in pixel dell'interfaccia, relativi all'angolo in alto a sinistra della schermata, con il
 * bordo destro e quello in basso esclusi. I colori sono ARGB e tutti opachi: il mondo dietro la schermata non deve
 * trasparire, e nessuno e' magenta per tools/client/arm_measure.sh (il magenta, nella schermata, e' solo quello della
 * tuta nell'anteprima).
 */
final class StationLayout {
    /** Larghezza della schermata. */
    static final int WIDTH = 316;
    /** Altezza della schermata. */
    static final int HEIGHT = 232;

    /** Il titolo, con l'etichetta in alto a sinistra. */
    static final Rect TITLE = new Rect(8, 6, 140, 15);
    /** Il pannello dell'anteprima 3D della tuta. */
    static final Rect PREVIEW_PANEL = new Rect(215, 17, 309, 135);
    /** Il ritaglio dell'anteprima, dentro il suo pannello: la tuta non ne esce. */
    static final Rect PREVIEW_CLIP = new Rect(216, 18, 308, 134);
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
    /** Il testo. */
    static final int TEXT = 0xFFE6E6E6;
    /** Il pulsante di prova finche' il server non ha confermato nessuna pressione. */
    static final int TEST_IDLE = 0xFF3A3F46;
    /** Il pulsante di prova dopo la prima pressione confermata dal server. */
    static final int TEST_DONE = 0xFF2E8B57;

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

    /**
     * Il colore di un pulsante sotto il mouse: ogni canale si avvicina di un quarto al bianco, l'alfa resta. Un grigio
     * o un verde schiariti non diventano magenta.
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
    }
}
