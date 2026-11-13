package com.toxineren.exoframes.client;

import com.toxineren.exoframes.core.armor.Tier;
import java.util.Objects;

/**
 * Colori predefiniti dei livelli, per i pezzi non verniciati ({@code Appearance.UNPAINTED}): RGB a 24 bit, regione
 * primaria e secondaria. I colori del Rottame sono quelli di docs/arte/README.md (li controlla TierPaletteTest); gli
 * altri livelli, finche' non hanno una grafica propria, usano gli stessi. Java puro, senza classi di Minecraft.
 */
public final class TierPalette {
    /** Rottame, primario: arancio ruggine. */
    private static final int SCRAP_PRIMARY = 0xB5651D;
    /** Rottame, secondario: verde rame spento. */
    private static final int SCRAP_SECONDARY = 0x5F7D7A;

    private TierPalette() {
    }

    /** Il colore predefinito della regione primaria di un pezzo del livello dato. */
    public static int primary(Tier tier) {
        return switch (Objects.requireNonNull(tier, "tier must not be null")) {
            case SCRAP, INDUSTRIAL, ARMORED, NETHERITE, PROTOTYPE -> SCRAP_PRIMARY;
        };
    }

    /** Il colore predefinito della regione secondaria di un pezzo del livello dato. */
    public static int secondary(Tier tier) {
        return switch (Objects.requireNonNull(tier, "tier must not be null")) {
            case SCRAP, INDUSTRIAL, ARMORED, NETHERITE, PROTOTYPE -> SCRAP_SECONDARY;
        };
    }
}
