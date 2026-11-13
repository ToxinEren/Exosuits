package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Scala della tuta e sollevamento del pilota (design, par. 4.7). */
class SuitScaleTest {

    /** Il generatore della grafica, che ha la sua copia della scala. */
    private static final String ART_GENERATOR = "tools/art/exo.py";

    @Test
    void raiseAlignsTheHeadPivots() {
        // Il perno di testa e busto (y = 24 px) della tuta in scala attorno ai piedi finisce a 24 * S: il pilota
        // sollevato di PILOT_RAISE_PX ci arriva con il proprio.
        assertEquals(24f * SuitScale.S, 24f + SuitScale.PILOT_RAISE_PX, 1e-4);
    }

    @Test
    void raiseInBlocksUsesThePlayerModelScale() {
        assertEquals(9.6f, SuitScale.PILOT_RAISE_PX, 1e-4);
        assertEquals(0.5625f, SuitScale.pilotRaiseBlocks(), 1e-4);
    }

    @Test
    void suitArmPivotOffsetMatchesTheScale() {
        // Perno del braccio destro nelle coordinate del modello armatura (piedi a y = 0, y verso l'alto).
        float pivotX = -5f;
        float pivotY = 22f;
        // La tuta e' scalata di S attorno ai piedi e poi abbassata del sollevamento (SuitLayer), perche' il disegno
        // del giocatore e' gia' sollevato; il pilota resta con il perno dov'e'.
        float suitX = pivotX * SuitScale.S;
        float suitY = pivotY * SuitScale.S - SuitScale.PILOT_RAISE_PX;
        // Differenza in pixel del modello, con y verso il basso come nello spazio del modello del giocatore.
        float dx = suitX - pivotX;
        float dyDown = -(suitY - pivotY);
        assertEquals(-2f, dx, 1e-4);
        assertEquals(0.8f, dyDown, 1e-4);
        // Gli stessi numeri che PilotPose usa per portare l'oggetto impugnato nella mano della tuta.
        assertEquals(2f, PilotPose.ARM_SHIFT_X_PX, 1e-4);
        assertEquals(dyDown, PilotPose.ARM_SHIFT_Y_PX, 1e-4);
    }

    /**
     * Il generatore della grafica (tools/art/exo.py) controlla le distanze dal pilota con la stessa scala con cui il
     * gioco disegna la tuta: se una cambia, l'altra deve seguirla.
     */
    @Test
    void matchesTheArtGenerator() throws IOException {
        String exo = Files.readString(findInRepository(ART_GENERATOR), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?m)^SUIT_SCALE = ([0-9]+(?:\\.[0-9]+)?)$").matcher(exo);
        assertTrue(m.find(), "SUIT_SCALE non trovato in " + ART_GENERATOR);
        assertEquals(Float.parseFloat(m.group(1)), SuitScale.S, 0f);
    }

    /** Un file del repository, cercato risalendo dalla cartella di lavoro (i test girano in versions/<variante>). */
    private static Path findInRepository(String relative) {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return fail(relative + " non trovato risalendo da " + System.getProperty("user.dir"));
    }
}
