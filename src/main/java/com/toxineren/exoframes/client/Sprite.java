package com.toxineren.exoframes.client;

/**
 * Un disegno in una texture dell'interfaccia generata da tools/art/gui.py (parte 4d, stile A): l'angolo in alto a
 * sinistra {@code (u, v)} e la grandezza, in texel, che a scala 1 sono pixel dell'interfaccia. Java puro: i posti
 * stanno in {@link StationLayout} e {@link HudLayout}, con i nomi della mappa del generatore
 * ({@code python3 tools/art/gui.py --map}, in maiuscolo e con il trattino basso al posto del punto), e i loro JUnit li
 * confrontano con le PNG del mod. Si disegna con {@link GuiTexture#blit}.
 *
 * @param u      la colonna dell'angolo in alto a sinistra
 * @param v      la riga dell'angolo in alto a sinistra
 * @param width  la larghezza
 * @param height l'altezza
 */
record Sprite(int u, int v, int width, int height) {
    /**
     * @throws IllegalArgumentException con un angolo negativo o una grandezza non positiva
     */
    Sprite {
        if (u < 0 || v < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("not a sprite: " + u + "," + v + " " + width + "x" + height);
        }
    }

    /**
     * La parte del disegno da {@code (du, dv)}, larga {@code w} e alta {@code h}: un pezzo della sagoma dalla figura
     * del suo stato, una meta' di un pulsante.
     *
     * @throws IllegalArgumentException se la parte esce dal disegno o e' vuota
     */
    Sprite part(int du, int dv, int w, int h) {
        if (du < 0 || dv < 0 || w <= 0 || h <= 0 || du + w > width || dv + h > height) {
            throw new IllegalArgumentException("part " + du + "," + dv + " " + w + "x" + h + " leaves " + this);
        }
        return new Sprite(u + du, v + dv, w, h);
    }

    /**
     * Le prime {@code w} colonne del disegno, da sinistra: un riempimento in proporzione (barra dell'energia, barra
     * della durata).
     *
     * @throws IllegalArgumentException se {@code w} non e' fra 1 e la larghezza
     */
    Sprite left(int w) {
        return part(0, 0, w, height);
    }
}
