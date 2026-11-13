package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.toxineren.exoframes.core.armor.Tier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Colori predefiniti dei livelli: quelli del Rottame sono scritti nelle note d'arte, che fanno fede. */
class TierPaletteTest {

    private static final String ART_NOTES = "docs/arte/README.md";

    @Test
    void scrapDefaultsMatchTheArtNotes() throws IOException {
        String notes = Files.readString(findArtNotes(), StandardCharsets.UTF_8);
        assertEquals(color(notes, "Primario"), TierPalette.primary(Tier.SCRAP));
        assertEquals(color(notes, "secondario"), TierPalette.secondary(Tier.SCRAP));
    }

    @Test
    void everyTierHasDefaults() {
        for (Tier tier : Tier.values()) {
            int primary = TierPalette.primary(tier);
            int secondary = TierPalette.secondary(tier);
            assertTrue(primary >= 0 && primary <= 0xFFFFFF, tier + ": primario fuori da RGB: " + primary);
            assertTrue(secondary >= 0 && secondary <= 0xFFFFFF, tier + ": secondario fuori da RGB: " + secondary);
        }
    }

    /** Il colore scritto come {@code <etichetta> `0xRRGGBB`} nella sezione dei colori del Rottame. */
    private static int color(String notes, String label) {
        Matcher m = Pattern.compile(label + " `0x([0-9A-Fa-f]{6})`").matcher(notes);
        assertTrue(m.find(), "colore '" + label + "' non trovato in " + ART_NOTES);
        return Integer.parseInt(m.group(1), 16);
    }

    /** Le note d'arte, cercate risalendo dalla cartella di lavoro (i test girano in versions/<variante>). */
    private static Path findArtNotes() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(ART_NOTES);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return fail(ART_NOTES + " non trovato risalendo da " + System.getProperty("user.dir"));
    }
}
