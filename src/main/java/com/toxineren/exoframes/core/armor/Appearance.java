package com.toxineren.exoframes.core.armor;

import java.util.List;

/**
 * Aspetto cosmetico di un pezzo: colore primario e secondario (RGB in [0, 0xFFFFFF], oppure {@link #UNPAINTED} per
 * il colore predefinito del livello) e al massimo {@link #MAX_DECALS} decalcomanie, nell'ordine in cui si
 * sovrappongono. La lista e' una copia immutabile.
 */
public record Appearance(int primary, int secondary, List<Decal> decals) {

    /** Colore "non dipinto": si usa il colore predefinito del livello. */
    public static final int UNPAINTED = -1;
    /** Numero massimo di decalcomanie per pezzo. */
    public static final int MAX_DECALS = 4;
    /** Aspetto di un pezzo mai dipinto: nessun colore e nessuna decalcomania. */
    public static final Appearance DEFAULT = new Appearance(UNPAINTED, UNPAINTED, List.of());

    public Appearance {
        checkColor("primary", primary);
        checkColor("secondary", secondary);
        decals = List.copyOf(decals);
        if (decals.size() > MAX_DECALS) {
            throw new IllegalArgumentException("at most " + MAX_DECALS + " decals allowed: " + decals.size());
        }
    }

    private static void checkColor(String name, int color) {
        if (color != UNPAINTED && (color < 0 || color > 0xFFFFFF)) {
            throw new IllegalArgumentException(name + " color must be UNPAINTED or in [0, 0xFFFFFF]: " + color);
        }
    }
}
