package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Una texture dell'interfaccia della mod letta dal classpath (le PNG di tools/art/gui.py, parte 4d), con i controlli
 * che legano i posti dei disegni di {@link StationLayout} e {@link HudLayout} ai texel: per i JUnit dei due.
 */
final class SpriteSheet {
    private final String name;
    private final BufferedImage image;

    private SpriteSheet(String name, BufferedImage image) {
        this.name = name;
        this.image = image;
    }

    /** La texture della mod {@code textures/gui/<file>}. */
    static SpriteSheet load(String file) {
        String path = "/assets/redstone_exoframes/textures/gui/" + file;
        try (InputStream in = SpriteSheet.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, "unreadable " + path);
            return new SpriteSheet(file, image);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tutte le costanti {@link Sprite} della classe, per nome, nell'ordine in cui sono dichiarate. */
    static Map<String, Sprite> constants(Class<?> owner) {
        Map<String, Sprite> out = new LinkedHashMap<>();
        for (Field field : owner.getDeclaredFields()) {
            if (field.getType() == Sprite.class && Modifier.isStatic(field.getModifiers())) {
                try {
                    out.put(field.getName(), (Sprite) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return out;
    }

    int width() {
        return image.getWidth();
    }

    int height() {
        return image.getHeight();
    }

    /** Il texel (x, y) della texture, ARGB senza premoltiplicazione. */
    int argb(int x, int y) {
        return image.getRGB(x, y);
    }

    /** Il texel (dx, dy) del disegno, ARGB. */
    int argb(Sprite sprite, int dx, int dy) {
        assertTrue(dx >= 0 && dy >= 0 && dx < sprite.width() && dy < sprite.height(),
                dx + "," + dy + " outside " + sprite);
        return argb(sprite.u() + dx, sprite.v() + dy);
    }

    /**
     * I disegni stanno nella texture, non si sovrappongono e ciascuno ha almeno un texel non trasparente; ogni texel
     * non trasparente della texture sta in un disegno (nessun disegno senza la sua costante); nessun texel e' magenta
     * per tools/client/arm_measure.sh.
     */
    void assertCoveredExactlyBy(Map<String, Sprite> sprites) {
        String[] owner = new String[width() * height()];
        for (Map.Entry<String, Sprite> entry : sprites.entrySet()) {
            Sprite s = entry.getValue();
            assertTrue(s.u() + s.width() <= width() && s.v() + s.height() <= height(),
                    entry.getKey() + " " + s + " leaves " + name);
            boolean drawn = false;
            for (int y = s.v(); y < s.v() + s.height(); y++) {
                for (int x = s.u(); x < s.u() + s.width(); x++) {
                    int i = y * width() + x;
                    assertTrue(owner[i] == null, entry.getKey() + " overlaps " + owner[i] + " at " + x + "," + y);
                    owner[i] = entry.getKey();
                    drawn |= alpha(argb(x, y)) != 0;
                }
            }
            assertTrue(drawn, entry.getKey() + " " + s + " is empty in " + name);
        }
        List<String> stray = new ArrayList<>();
        int magenta = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                int c = argb(x, y);
                if (alpha(c) != 0 && owner[y * width() + x] == null && stray.size() < 5) {
                    stray.add(x + "," + y);
                }
                if (alpha(c) != 0 && magenta(c)) {
                    magenta++;
                }
            }
        }
        assertTrue(stray.isEmpty(), name + " has texels outside every sprite constant, e.g. " + stray);
        assertEquals(0, magenta, name + " texels that are magenta for arm_measure.sh");
    }

    /** Ogni texel del rettangolo (dx, dy, w, h) del disegno ha esattamente il colore ARGB dato. */
    void assertAll(String what, Sprite sprite, int dx, int dy, int w, int h, int argb) {
        for (int y = dy; y < dy + h; y++) {
            for (int x = dx; x < dx + w; x++) {
                assertEquals(Integer.toHexString(argb), Integer.toHexString(argb(sprite, x, y)),
                        what + ", texel " + x + "," + y + " of " + sprite);
            }
        }
    }

    /** Il bordo di 1 pixel del disegno ha tutto il colore ARGB dato. */
    void assertRing(String what, Sprite sprite, int argb) {
        assertAll(what + ", top", sprite, 0, 0, sprite.width(), 1, argb);
        assertAll(what + ", bottom", sprite, 0, sprite.height() - 1, sprite.width(), 1, argb);
        assertAll(what + ", left", sprite, 0, 0, 1, sprite.height(), argb);
        assertAll(what + ", right", sprite, sprite.width() - 1, 0, 1, sprite.height(), argb);
    }

    /** Ogni texel del disegno e' opaco. */
    void assertOpaque(String what, Sprite sprite) {
        for (int y = 0; y < sprite.height(); y++) {
            for (int x = 0; x < sprite.width(); x++) {
                assertEquals(0xFF, alpha(argb(sprite, x, y)), what + ": texel " + x + "," + y + " is not opaque");
            }
        }
    }

    static int alpha(int argb) {
        return argb >>> 24;
    }

    /** La regola del magenta di tools/client/arm_measure.sh, con i canali fra 0 e 1 e l'alfa ignorato. */
    static boolean magenta(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return r > 0.2 && b > 0.2 && g < 0.45 * r && g < 0.45 * b;
    }

    /** Il controllo stesso: FF00FF e' magenta, la legenda no. */
    static void assertMagentaRuleWorks() {
        assertTrue(magenta(0xFFFF00FF), "control: FF00FF is magenta for arm_measure.sh");
        assertFalse(magenta(HudModel.RED), "control: the legend red is not magenta");
    }
}
