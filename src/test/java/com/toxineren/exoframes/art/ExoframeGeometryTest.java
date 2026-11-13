package com.toxineren.exoframes.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toxineren.exoframes.compat.SuitBones;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Controllo senza gioco della geometria generata da tools/art/build.py: il renderer (GeckoLib) cerca le ossa per
 * nome e le posiziona sui perni del modello armatura, quindi nomi e perni sono un contratto.
 */
class ExoframeGeometryTest {

    private static final String GEO = "/assets/redstone_exoframes/geo/exoframe.geo.json";

    /** Ossa di primo livello e perni del modello armatura di GeckoLib (in pixel, coordinate Bedrock). */
    private static final Map<String, double[]> TOP_LEVEL = new LinkedHashMap<>();

    /** Osso del telaio nudo -> osso di primo livello che lo contiene. */
    private static final Map<String, String> FRAME_BONES = new LinkedHashMap<>();

    static {
        TOP_LEVEL.put("armorHead", new double[] {0, 24, 0});
        TOP_LEVEL.put("armorBody", new double[] {0, 24, 0});
        TOP_LEVEL.put("armorRightArm", new double[] {-5, 22, 0});
        TOP_LEVEL.put("armorLeftArm", new double[] {5, 22, 0});
        TOP_LEVEL.put("armorRightLeg", new double[] {-2, 12, 0});
        TOP_LEVEL.put("armorLeftLeg", new double[] {2, 12, 0});
        FRAME_BONES.put("frameHead", "armorHead");
        FRAME_BONES.put("frameBody", "armorBody");
        FRAME_BONES.put("frameRightArm", "armorRightArm");
        FRAME_BONES.put("frameLeftArm", "armorLeftArm");
        FRAME_BONES.put("frameRightLeg", "armorRightLeg");
        FRAME_BONES.put("frameLeftLeg", "armorLeftLeg");
    }

    private static JsonObject description;
    private static final Map<String, JsonObject> BONES = new LinkedHashMap<>();

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = ExoframeGeometryTest.class.getResourceAsStream(GEO)) {
            assertNotNull(in, "risorsa assente: " + GEO + " (rigenerare con python3 tools/art/build.py)");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("1.12.0", root.get("format_version").getAsString());
                JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
                assertEquals(1, geometries.size());
                JsonObject geometry = geometries.get(0).getAsJsonObject();
                description = geometry.getAsJsonObject("description");
                assertEquals("geometry.exoframe", description.get("identifier").getAsString());
                for (JsonElement element : geometry.getAsJsonArray("bones")) {
                    JsonObject bone = element.getAsJsonObject();
                    String name = bone.get("name").getAsString();
                    assertFalse(BONES.containsKey(name), "osso duplicato: " + name);
                    BONES.put(name, bone);
                }
            }
        }
    }

    private static double[] vec(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        assertNotNull(array, "manca " + key);
        assertEquals(3, array.size());
        return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    @Test
    void topLevelBonesHaveTemplatePivots() {
        for (Map.Entry<String, double[]> entry : TOP_LEVEL.entrySet()) {
            JsonObject bone = BONES.get(entry.getKey());
            assertNotNull(bone, "manca l'osso " + entry.getKey());
            assertFalse(bone.has("parent"), entry.getKey() + " deve stare al primo livello");
            double[] pivot = vec(bone, "pivot");
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(entry.getValue()[axis], pivot[axis], 1e-9, "perno di " + entry.getKey());
            }
            assertFalse(bone.has("rotation"), entry.getKey() + " non deve essere ruotato");
        }
        for (Map.Entry<String, JsonObject> entry : BONES.entrySet()) {
            if (!entry.getValue().has("parent")) {
                assertTrue(TOP_LEVEL.containsKey(entry.getKey()), "osso di primo livello inatteso: " + entry.getKey());
            }
        }
    }

    @Test
    void frameBonesExistUnderTheirParents() {
        for (Map.Entry<String, String> entry : FRAME_BONES.entrySet()) {
            JsonObject bone = BONES.get(entry.getKey());
            assertNotNull(bone, "manca l'osso " + entry.getKey());
            assertEquals(entry.getValue(), bone.get("parent").getAsString(), "genitore di " + entry.getKey());
            double[] pivot = vec(bone, "pivot");
            double[] parentPivot = TOP_LEVEL.get(entry.getValue());
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(parentPivot[axis], pivot[axis], 1e-9, "perno di " + entry.getKey() + " diverso dal genitore");
            }
            assertFalse(bone.has("rotation"), "osso del telaio ruotato: " + entry.getKey());
            JsonArray cubes = bone.getAsJsonArray("cubes");
            assertTrue(cubes != null && !cubes.isEmpty(), entry.getKey() + " non ha scatole");
            for (JsonElement cube : cubes) {
                assertFalse(cube.getAsJsonObject().has("rotation"), "scatola ruotata nel telaio: " + entry.getKey());
            }
        }
    }

    /**
     * I nomi che il renderer cerca, costruiti da {@link SuitBones}, esistono nella geometria: le sei ossa di primo
     * livello e le sei del telaio, ciascuna sotto il suo osso di primo livello. I nomi dei pezzi e delle
     * decalcomanie li verifica il test dei pezzi (Task 6).
     */
    @Test
    void rendererFrameBoneNamesExistInGeometry() {
        assertEquals(6, SuitBones.TOP_LEVEL.size());
        assertEquals(6, SuitBones.PARTS.size());
        for (String name : SuitBones.TOP_LEVEL) {
            assertTrue(BONES.containsKey(name), "il renderer cerca " + name + ", assente nella geometria");
        }
        for (int i = 0; i < SuitBones.PARTS.size(); i++) {
            String frame = SuitBones.frame(SuitBones.PARTS.get(i));
            JsonObject bone = BONES.get(frame);
            assertNotNull(bone, "il renderer cerca " + frame + ", assente nella geometria");
            assertEquals(SuitBones.TOP_LEVEL.get(i), bone.get("parent").getAsString(), "genitore di " + frame);
        }
    }

    @Test
    void everyCubeIsInsideTheSuitBox() {
        double[] min = {-9.5, 0, -7};
        double[] max = {9.5, 30.7, 9};
        for (Map.Entry<String, JsonObject> entry : BONES.entrySet()) {
            for (JsonObject cube : cubes(entry.getValue())) {
                double[] origin = vec(cube, "origin");
                double[] size = vec(cube, "size");
                double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0;
                for (int axis = 0; axis < 3; axis++) {
                    double lo = origin[axis] - inflate;
                    double hi = origin[axis] + size[axis] + inflate;
                    String where = entry.getKey() + " " + cube;
                    assertTrue(size[axis] >= 0, "dimensione negativa in " + where);
                    assertTrue(lo >= min[axis] && hi <= max[axis], "scatola fuori dall'ingombro della tuta: " + where);
                }
            }
        }
    }

    @Test
    void uvRectanglesStayInsideTheAtlas() {
        int width = description.get("texture_width").getAsInt();
        int height = description.get("texture_height").getAsInt();
        assertEquals(512, width);
        assertEquals(512, height);
        int faces = 0;
        for (Map.Entry<String, JsonObject> entry : BONES.entrySet()) {
            for (JsonObject cube : cubes(entry.getValue())) {
                JsonObject uv = cube.getAsJsonObject("uv");
                assertNotNull(uv, "UV per faccia mancanti in " + entry.getKey());
                for (Map.Entry<String, JsonElement> face : uv.entrySet()) {
                    assertTrue(List.of("north", "south", "east", "west", "up", "down").contains(face.getKey()),
                            "faccia sconosciuta " + face.getKey());
                    JsonObject rect = face.getValue().getAsJsonObject();
                    JsonArray start = rect.getAsJsonArray("uv");
                    JsonArray size = rect.getAsJsonArray("uv_size");
                    double u0 = start.get(0).getAsDouble();
                    double v0 = start.get(1).getAsDouble();
                    double u1 = u0 + size.get(0).getAsDouble();
                    double v1 = v0 + size.get(1).getAsDouble();
                    String where = entry.getKey() + " " + face.getKey() + " " + rect;
                    assertTrue(Math.min(u0, u1) >= 0 && Math.max(u0, u1) <= width, "u fuori dall'atlante: " + where);
                    assertTrue(Math.min(v0, v1) >= 0 && Math.max(v0, v1) <= height, "v fuori dall'atlante: " + where);
                    faces++;
                }
            }
        }
        assertTrue(faces > 0, "nessuna faccia con UV");
    }

    private static List<JsonObject> cubes(JsonObject bone) {
        JsonArray array = bone.getAsJsonArray("cubes");
        if (array == null) {
            return List.of();
        }
        return array.asList().stream().map(JsonElement::getAsJsonObject).toList();
    }
}
