package com.toxineren.exoframes.core.armor;

/**
 * Decalcomania di un pezzo, come uno strato di stendardo: motivo e colore, entrambi come stringhe (gli id restano
 * al livello di piattaforma). Entrambi non nulli, non vuoti e lunghi al massimo {@value #MAX_LENGTH} caratteri.
 */
public record Decal(String pattern, String color) {

    /** Lunghezza massima di motivo e colore. */
    public static final int MAX_LENGTH = 64;

    public Decal {
        check("pattern", pattern);
        check("color", color);
    }

    private static void check(String name, String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(name + " must be a non-empty string of at most " + MAX_LENGTH
                    + " characters: " + value);
        }
    }
}
