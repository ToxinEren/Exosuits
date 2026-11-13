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
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Controllo senza gioco della geometria generata da tools/art/build.py: il renderer (GeckoLib) cerca le ossa per
 * nome e le posiziona sui perni del modello armatura, quindi nomi e perni sono un contratto.
 */
class ExoframeGeometryTest {

    private static final String GEO = "/assets/redstone_exoframes/geo/exoframe.geo.json";
    private static final String ATLAS = "/assets/redstone_exoframes/textures/entity/exoframe.png";

    /** Ingombri in pixel non scalati, come exo.FRAME_MIN/MAX (telaio nudo) e exo.SUIT_MIN/MAX (pezzi). */
    private static final double[] FRAME_MIN = {-12.75, 0, -8};
    private static final double[] FRAME_MAX = {12.75, 31.25, 10};
    private static final double[] SUIT_MIN = {-13.5, 0, -8.75};
    private static final double[] SUIT_MAX = {13.5, 32, 10.5};

    /** Osso di uno stato di un pezzo: piece<Slot>_s<stato>_<regione>. */
    private static final Pattern STATE_BONE = Pattern.compile("piece[A-Za-z]+_s([0-3])_(p1|p2|n)");

    /** Facce che si scambiano in una copia specchiata in x. */
    private static final Map<String, String> MIRROR_FACE = Map.of("east", "west", "west", "east");

    /** Osso di primo livello che contiene il pezzo di ogni slot. */
    private static final Map<PieceSlot, String> PARENT = new EnumMap<>(PieceSlot.class);

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
        PARENT.put(PieceSlot.HELMET, "armorHead");
        PARENT.put(PieceSlot.TORSO, "armorBody");
        PARENT.put(PieceSlot.RIGHT_ARM, "armorRightArm");
        PARENT.put(PieceSlot.LEFT_ARM, "armorLeftArm");
        PARENT.put(PieceSlot.RIGHT_LEG, "armorRightLeg");
        PARENT.put(PieceSlot.LEFT_LEG, "armorLeftLeg");
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
     * decalcomanie li verificano {@link #everyPieceHasFourStatesAndThreeRegions()} e {@link #decalBonesExist()}.
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

    /**
     * Ossa del telaio nei limiti del telaio nudo, ossa dei pezzi (decalcomanie comprese) nei limiti della tuta con i
     * pezzi: gli stessi di exo.FRAME_MIN/MAX e exo.SUIT_MIN/MAX. Nessun altro osso porta scatole.
     */
    @Test
    void frameAndPiecesStayInsideTheirBounds() {
        int pieceCubes = 0;
        for (Map.Entry<String, JsonObject> entry : BONES.entrySet()) {
            String name = entry.getKey();
            List<JsonObject> cubes = cubes(entry.getValue());
            double[] min;
            double[] max;
            if (name.startsWith("frame")) {
                min = FRAME_MIN;
                max = FRAME_MAX;
            } else if (name.startsWith("piece")) {
                min = SUIT_MIN;
                max = SUIT_MAX;
                pieceCubes += cubes.size();
            } else {
                assertTrue(cubes.isEmpty(), "scatole in un osso che non e' ne' del telaio ne' di un pezzo: " + name);
                continue;
            }
            for (JsonObject cube : cubes) {
                double[] origin = vec(cube, "origin");
                double[] size = vec(cube, "size");
                double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0;
                for (int axis = 0; axis < 3; axis++) {
                    double lo = origin[axis] - inflate;
                    double hi = origin[axis] + size[axis] + inflate;
                    String where = name + " " + cube;
                    assertTrue(size[axis] >= 0, "dimensione negativa in " + where);
                    assertTrue(lo >= min[axis] - 1e-9 && hi <= max[axis] + 1e-9, "scatola fuori dall'ingombro: " + where);
                }
            }
        }
        assertTrue(pieceCubes > 0, "nessuna scatola nei pezzi");
    }

    /**
     * I 72 nomi che il renderer cerca per i pezzi (sei slot, quattro stati, tre regioni), costruiti da
     * {@link SuitBones#piece}, esistono sotto l'osso di primo livello del loro slot, con il suo perno; le regioni
     * primarie hanno scatole in ogni stato. Non ci sono altre ossa dei pezzi oltre a queste e alle decalcomanie.
     */
    @Test
    void everyPieceHasFourStatesAndThreeRegions() {
        Set<String> expected = new HashSet<>();
        for (PieceSlot slot : PieceSlot.values()) {
            String parent = PARENT.get(slot);
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (String region : SuitBones.REGIONS) {
                    String name = SuitBones.piece(slot, state, region);
                    expected.add(name);
                    JsonObject bone = BONES.get(name);
                    assertNotNull(bone, "il renderer cerca " + name + ", assente nella geometria");
                    assertEquals(parent, bone.get("parent").getAsString(), "genitore di " + name);
                    double[] pivot = vec(bone, "pivot");
                    for (int axis = 0; axis < 3; axis++) {
                        assertEquals(TOP_LEVEL.get(parent)[axis], pivot[axis], 1e-9, "perno di " + name);
                    }
                    assertFalse(bone.has("rotation"), "osso del pezzo ruotato: " + name);
                    if (region.equals(SuitBones.PRIMARY)) {
                        assertFalse(cubes(bone).isEmpty(), name + " non ha scatole");
                    }
                }
            }
        }
        assertEquals(72, expected.size());
        for (PieceSlot slot : SuitBones.DECAL_SLOTS) {
            expected.add(SuitBones.decal(slot));
        }
        for (String name : BONES.keySet()) {
            if (name.startsWith("piece")) {
                assertTrue(expected.contains(name), "osso di un pezzo che il renderer non conosce: " + name);
            }
        }
    }

    /** Le quattro copie di uno slot e di una regione hanno le stesse scatole, nello stesso ordine. */
    @Test
    void stateCopiesShareGeometry() {
        for (PieceSlot slot : PieceSlot.values()) {
            for (String region : SuitBones.REGIONS) {
                List<String> first = geometry(bone(SuitBones.piece(slot, 0, region)));
                for (int state = 1; state < SuitBones.STATES; state++) {
                    String name = SuitBones.piece(slot, state, region);
                    assertEquals(first, geometry(bone(name)), "scatole di " + name + " diverse dallo stato 0");
                }
            }
        }
    }

    /**
     * Le facce di ogni stato stanno nella sua zona dell'atlante (righe 128 + 96 * stato, alte 96), quelle del telaio
     * sopra la riga 128; due rettangoli diversi non si sovrappongono (una copia specchiata riusa il rettangolo della
     * sorgente, quindi rettangoli identici sono ammessi).
     */
    @Test
    void stateRegionsDoNotShareTexels() {
        Set<List<Integer>> rects = new HashSet<>();
        for (Map.Entry<String, JsonObject> entry : BONES.entrySet()) {
            String name = entry.getKey();
            Matcher m = STATE_BONE.matcher(name);
            int top;
            int bottom;
            if (m.matches()) {
                int state = Integer.parseInt(m.group(1));
                top = 128 + 96 * state;
                bottom = top + 96;
            } else if (name.startsWith("frame")) {
                top = 0;
                bottom = 128;
            } else {
                continue;
            }
            for (JsonObject cube : cubes(entry.getValue())) {
                for (Map.Entry<String, JsonElement> face : cube.getAsJsonObject("uv").entrySet()) {
                    List<Integer> rect = rect(face.getValue().getAsJsonObject());
                    String where = name + " " + face.getKey() + " " + rect;
                    assertTrue(rect.get(1) >= top && rect.get(3) <= bottom, "faccia fuori dalla sua zona: " + where);
                    rects.add(rect);
                }
            }
        }
        List<List<Integer>> all = new ArrayList<>(rects);
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                List<Integer> a = all.get(i);
                List<Integer> b = all.get(j);
                boolean overlap = a.get(0) < b.get(2) && b.get(0) < a.get(2) && a.get(1) < b.get(3) && b.get(1) < a.get(3);
                assertFalse(overlap, "rettangoli sovrapposti nell'atlante: " + a + " e " + b);
            }
        }
    }

    /**
     * Le ossa delle decalcomanie, nominate da {@link SuitBones#decal}, esistono sotto l'osso dello slot, con una
     * sola scatola piatta (un lato di 0,25 px) i cui texel sono tutti trasparenti.
     */
    @Test
    void decalBonesExist() throws IOException {
        BufferedImage atlas = atlas();
        for (PieceSlot slot : SuitBones.DECAL_SLOTS) {
            String name = SuitBones.decal(slot);
            JsonObject bone = BONES.get(name);
            assertNotNull(bone, "il renderer cerca " + name + ", assente nella geometria");
            assertEquals(PARENT.get(slot), bone.get("parent").getAsString(), "genitore di " + name);
            List<JsonObject> cubes = cubes(bone);
            assertEquals(1, cubes.size(), name + " deve avere una sola scatola");
            double[] size = vec(cubes.get(0), "size");
            assertEquals(0.25, Math.min(size[0], Math.min(size[1], size[2])), 1e-9, name + " non e' piatta");
            JsonObject uv = cubes.get(0).getAsJsonObject("uv");
            assertFalse(uv.entrySet().isEmpty(), name + " non ha facce");
            for (Map.Entry<String, JsonElement> face : uv.entrySet()) {
                List<Integer> rect = rect(face.getValue().getAsJsonObject());
                for (int y = rect.get(1); y < rect.get(3); y++) {
                    for (int x = rect.get(0); x < rect.get(2); x++) {
                        assertEquals(0, atlas.getRGB(x, y) >>> 24, "texel non trasparente in " + name + " a " + x + "," + y);
                    }
                }
            }
        }
    }

    /**
     * Braccia e gambe: un solo item serve i due lati, quindi in ogni stato e regione le scatole del pezzo sinistro
     * sono quelle del destro specchiate in x, e ogni faccia usa lo stesso rettangolo dell'atlante specchiato
     * (uv_size negativo in u, est e ovest scambiati).
     */
    @Test
    void leftAndRightPiecesAreMirrored() {
        PieceSlot[][] pairs = {{PieceSlot.RIGHT_ARM, PieceSlot.LEFT_ARM}, {PieceSlot.RIGHT_LEG, PieceSlot.LEFT_LEG}};
        for (PieceSlot[] pair : pairs) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (String region : SuitBones.REGIONS) {
                    String rightName = SuitBones.piece(pair[0], state, region);
                    String leftName = SuitBones.piece(pair[1], state, region);
                    Map<String, JsonObject> left = new HashMap<>();
                    for (JsonObject cube : cubes(bone(leftName))) {
                        left.put(box(vec(cube, "origin"), vec(cube, "size")), cube);
                    }
                    List<JsonObject> right = cubes(bone(rightName));
                    assertEquals(right.size(), left.size(), "numero di scatole di " + leftName + " e " + rightName);
                    for (JsonObject cube : right) {
                        double[] origin = vec(cube, "origin");
                        double[] size = vec(cube, "size");
                        double[] mirrored = {-(origin[0] + size[0]), origin[1], origin[2]};
                        JsonObject twin = left.get(box(mirrored, size));
                        assertNotNull(twin, "manca in " + leftName + " la copia specchiata di " + cube);
                        JsonObject uv = cube.getAsJsonObject("uv");
                        JsonObject twinUv = twin.getAsJsonObject("uv");
                        assertEquals(uv.size(), twinUv.size(), "facce della copia specchiata di " + cube);
                        for (Map.Entry<String, JsonElement> face : uv.entrySet()) {
                            String swapped = MIRROR_FACE.getOrDefault(face.getKey(), face.getKey());
                            assertTrue(twinUv.has(swapped), "manca la faccia " + swapped + " in " + leftName);
                            assertEquals(rect(face.getValue().getAsJsonObject()), rect(twinUv.getAsJsonObject(swapped)),
                                    "rettangolo della faccia " + swapped + " in " + leftName);
                            double sizeU = twinUv.getAsJsonObject(swapped).getAsJsonArray("uv_size").get(0).getAsDouble();
                            double rightU = face.getValue().getAsJsonObject().getAsJsonArray("uv_size").get(0).getAsDouble();
                            assertEquals(-rightU, sizeU, 1e-9, "faccia " + swapped + " in " + leftName + " non specchiata");
                        }
                    }
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

    private static JsonObject bone(String name) {
        JsonObject bone = BONES.get(name);
        assertNotNull(bone, "manca l'osso " + name);
        return bone;
    }

    /** Origine e dimensioni delle scatole di un osso, nell'ordine del file. */
    private static List<String> geometry(JsonObject bone) {
        List<String> out = new ArrayList<>();
        for (JsonObject cube : cubes(bone)) {
            out.add(box(vec(cube, "origin"), vec(cube, "size")));
        }
        return out;
    }

    private static String box(double[] origin, double[] size) {
        return Arrays.toString(origin) + " " + Arrays.toString(size);
    }

    /** Il rettangolo di una faccia come [u minimo, v minimo, u massimo, v massimo] (uv_size puo' essere negativo). */
    private static List<Integer> rect(JsonObject face) {
        JsonArray start = face.getAsJsonArray("uv");
        JsonArray size = face.getAsJsonArray("uv_size");
        int u0 = start.get(0).getAsInt();
        int v0 = start.get(1).getAsInt();
        int u1 = u0 + size.get(0).getAsInt();
        int v1 = v0 + size.get(1).getAsInt();
        return List.of(Math.min(u0, u1), Math.min(v0, v1), Math.max(u0, u1), Math.max(v0, v1));
    }

    private static BufferedImage atlas() throws IOException {
        try (InputStream in = ExoframeGeometryTest.class.getResourceAsStream(ATLAS)) {
            assertNotNull(in, "risorsa assente: " + ATLAS);
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, "atlante illeggibile: " + ATLAS);
            return image;
        }
    }
}
