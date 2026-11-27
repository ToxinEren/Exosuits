package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * La mappa del generatore dell'interfaccia (tools/art/gui_map.json, scritta da tools/art/gui.py --map-json e
 * confrontata byte per byte da tools/check_art.sh) contro {@link StationLayout} e {@link HudLayout}: i nomi e i posti
 * dei disegni delle due texture, i rettangoli dei widget attorno a cui lo sfondo della schermata cuoce qualcosa e quelli
 * dei pezzi nelle figure della sagoma. Le prove delle PNG dicono che ogni texel sta in un disegno; questa dice che ogni
 * disegno ha il suo nome (due disegni della stessa grandezza con i nomi scambiati passano quelle prove) e che lo sfondo
 * cuoce prese, incavi e cornici dove il codice mette i widget.
 */
class GuiMapTest {
    /** La mappa del generatore, fuori dalle risorse del mod: non entra nei jar. */
    private static final String MAP = "tools/art/gui_map.json";

    /**
     * Cio' che lo sfondo cuoce e {@link StationLayout} non ha ancora, tutti widget della parte 4b: il pannello dei
     * preset, la tavolozza e il predefinito (con la vaschetta attorno), le prime due righe dei preset (con la
     * scanalatura sotto) e la riga dei costi. Chi aggiunge a StationLayout il rettangolo di uno di questi lo sposta in
     * {@link #matched()}.
     */
    private static final List<String> BACKGROUND_ONLY = List.of("pannello.preset", "tavolozza", "predefinito",
            "preset.0", "preset.1", "riga.costi");

    @Test
    void spriteNamesAndPlacesMatchTheArtGenerator() throws IOException {
        JsonObject map = load();
        assertSprites(map.getAsJsonObject("textures/gui/station.png"), StationLayout.class, StationLayout.TEXTURE_W,
                StationLayout.TEXTURE_H);
        assertSprites(map.getAsJsonObject("textures/gui/hud.png"), HudLayout.class, HudLayout.TEXTURE_W,
                HudLayout.TEXTURE_H);
    }

    @Test
    void backgroundIsBakedWhereTheStationLayoutPutsTheWidgets() throws IOException {
        Map<String, int[]> baked = rects(load().getAsJsonObject("sfondo"));
        Map<String, StationLayout.Rect> matched = matched();
        Set<String> classified = new TreeSet<>(matched.keySet());
        classified.addAll(BACKGROUND_ONLY);
        assertEquals(matched.size() + BACKGROUND_ONLY.size(), classified.size(), "a name both matched and baked-only");
        // un rettangolo che lo sfondo cuoce in piu', o che non cuoce piu', va messo in matched() o in BACKGROUND_ONLY,
        // o tolto
        assertEquals(classified, new TreeSet<>(baked.keySet()), "what the background bakes, in " + MAP);
        for (Map.Entry<String, StationLayout.Rect> entry : matched.entrySet()) {
            int[] r = baked.get(entry.getKey());
            assertEquals(entry.getValue(), new StationLayout.Rect(r[0], r[1], r[2], r[3]),
                    entry.getKey() + ": the background bakes it elsewhere");
        }
    }

    @Test
    void silhouettePiecesAreWhereTheHudLayoutCutsThem() throws IOException {
        Map<String, int[]> pieces = rects(load().getAsJsonObject("sagoma"));
        Set<String> slots = new TreeSet<>();
        for (PieceSlot slot : PieceSlot.values()) {
            String name = slot.name().toLowerCase(Locale.ROOT);
            slots.add(name);
            int[] r = pieces.get(name);
            if (r != null) {
                assertEquals(HudLayout.piece(slot), new HudLayout.Rect(r[0], r[1], r[2], r[3]),
                        name + ": the figures draw it elsewhere");
            }
        }
        assertEquals(slots, new TreeSet<>(pieces.keySet()), "the silhouette pieces in " + MAP);
    }

    /** I widget che lo sfondo cuoce e che {@link StationLayout} ha: per nome della mappa, il rettangolo del codice. */
    private static Map<String, StationLayout.Rect> matched() {
        Map<String, StationLayout.Rect> matched = new LinkedHashMap<>();
        matched.put("pannello.telaio", StationLayout.FRAME_PANEL);
        matched.put("pannello.pezzo", StationLayout.PIECE_PANEL);
        matched.put("pannello.anteprima", StationLayout.PREVIEW_PANEL);
        for (PieceSlot slot : PieceSlot.values()) {
            matched.put("pezzo." + slot.name().toLowerCase(Locale.ROOT), StationLayout.PIECE_SLOTS.get(slot));
        }
        matched.put("cella", StationLayout.CELL);
        matched.put("durata.barra", StationLayout.DURABILITY_BAR);
        matched.put("anteprima.ritaglio", StationLayout.PREVIEW_CLIP);
        matched.put("inventario", StationLayout.INVENTORY);
        matched.put("barra", StationLayout.HOTBAR);
        return matched;
    }

    /**
     * La grandezza della texture e i suoi disegni nella mappa sono, per nome e posto, quelli delle costanti
     * {@link Sprite} della classe: lo stesso nome in maiuscolo con il trattino basso al posto del punto
     * ({@code tutti.normale} e' {@code TUTTI_NORMALE}), nessun disegno senza costante e nessuna costante senza disegno.
     */
    private static void assertSprites(JsonObject texture, Class<?> owner, int width, int height) {
        String what = owner.getSimpleName();
        JsonArray size = texture.getAsJsonArray("grandezza");
        assertEquals(List.of(width, height), List.of(size.get(0).getAsInt(), size.get(1).getAsInt()),
                what + " texture size");
        Map<String, Sprite> expected = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : rects(texture.getAsJsonObject("disegni")).entrySet()) {
            int[] s = entry.getValue();
            expected.put(entry.getKey().toUpperCase(Locale.ROOT).replace('.', '_'), new Sprite(s[0], s[1], s[2], s[3]));
        }
        Map<String, Sprite> constants = SpriteSheet.constants(owner);
        assertEquals(new TreeSet<>(expected.keySet()), new TreeSet<>(constants.keySet()),
                what + " sprite constants against the names in " + MAP);
        for (Map.Entry<String, Sprite> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), constants.get(entry.getKey()), what + "." + entry.getKey());
        }
    }

    /** Un oggetto della mappa con un rettangolo di quattro interi per nome, nell'ordine della mappa. */
    private static Map<String, int[]> rects(JsonObject object) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonArray a = entry.getValue().getAsJsonArray();
            assertEquals(4, a.size(), entry.getKey() + " in " + MAP);
            out.put(entry.getKey(), new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(),
                a.get(3).getAsInt()});
        }
        return out;
    }

    private static JsonObject load() throws IOException {
        return JsonParser.parseString(Files.readString(findInRepository(MAP), StandardCharsets.UTF_8))
                .getAsJsonObject();
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
        return fail(relative + " not found going up from " + System.getProperty("user.dir"));
    }
}
