package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Aspetto dei pezzi (colori e decalcomanie) e validazione di {@link Decal}. */
class AppearanceTest {

    private static final Decal STRIPE = new Decal("minecraft:stripe_bottom", "red");

    @Test
    void defaultIsUnpainted() {
        assertEquals(Appearance.UNPAINTED, Appearance.DEFAULT.primary());
        assertEquals(Appearance.UNPAINTED, Appearance.DEFAULT.secondary());
        assertTrue(Appearance.DEFAULT.decals().isEmpty());
        assertEquals(-1, Appearance.UNPAINTED);
        assertEquals(4, Appearance.MAX_DECALS);
    }

    @Test
    void rejectsColorOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new Appearance(0x1000000, Appearance.UNPAINTED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Appearance(Appearance.UNPAINTED, -2, List.of()));
        // gli estremi dell'intervallo sono validi
        new Appearance(0, 0xFFFFFF, List.of());
    }

    @Test
    void rejectsTooManyDecals() {
        assertThrows(IllegalArgumentException.class,
                () -> new Appearance(0, 0, List.of(STRIPE, STRIPE, STRIPE, STRIPE, STRIPE)));
        assertEquals(4, new Appearance(0, 0, List.of(STRIPE, STRIPE, STRIPE, STRIPE)).decals().size());
    }

    @Test
    void decalsListIsImmutableCopy() {
        List<Decal> source = new ArrayList<>(List.of(STRIPE));
        Appearance appearance = new Appearance(0x112233, Appearance.UNPAINTED, source);
        source.add(STRIPE);
        assertEquals(List.of(STRIPE), appearance.decals());
        assertThrows(UnsupportedOperationException.class, () -> appearance.decals().add(STRIPE));
    }

    @Test
    void decalRejectsEmptyNullOrLongFields() {
        String tooLong = "a".repeat(65);
        assertThrows(IllegalArgumentException.class, () -> new Decal("", "red"));
        assertThrows(IllegalArgumentException.class, () -> new Decal("stripe", ""));
        assertThrows(IllegalArgumentException.class, () -> new Decal(null, "red"));
        assertThrows(IllegalArgumentException.class, () -> new Decal("stripe", null));
        assertThrows(IllegalArgumentException.class, () -> new Decal(tooLong, "red"));
        assertEquals(64, new Decal("a".repeat(64), "red").pattern().length());
    }
}
