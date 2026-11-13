package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.toxineren.exoframes.client.PilotPose.BodyPart;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Parti del pilota che non si disegnano dentro la tuta (design, par. 4.7 e 5.2). */
class PilotPoseTest {

    /** La geometria generata della tuta. */
    private static final String GEO = "/assets/redstone_exoframes/geo/exoframe.geo.json";

    /**
     * Punto d'impugnatura del gioco base dal perno del braccio, in pixel del modello (y verso il basso):
     * {@code ItemInHandLayer.renderArmWithItem} dopo {@code translateToHand} ruota di -90 gradi in X e di 180 in Y e
     * trasla di (+-1, 2, -10) / 16, cioe' (-+1, 10, -2) nel sistema del braccio (verificato con javap su 1.20.1 e
     * 1.21.1). Destro: x = -1; sinistro: x = +1.
     */
    private static final float VANILLA_GRIP_X_PX = 1f;
    private static final float VANILLA_GRIP_Y_PX = 10f;
    private static final float VANILLA_GRIP_Z_PX = -2f;

    private static final PieceView INTACT = new PieceView(0, 0x808080, 0x404040);
    private static final PieceView BROKEN = new PieceView(3, 0x808080, 0x404040);

    @Test
    void legsAreAlwaysHidden() {
        for (PieceSlot slot : PieceSlot.values()) {
            assertTrue(PilotPose.hiddenParts(new SuitView(Map.of(slot, INTACT))).contains(BodyPart.LEGS),
                    "legs hidden with only " + slot);
        }
        Map<PieceSlot, PieceView> all = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            all.put(slot, INTACT);
        }
        assertTrue(PilotPose.hiddenParts(new SuitView(all)).contains(BodyPart.LEGS));
    }

    @Test
    void bareFrameHidesOnlyTheLegs() {
        assertEquals(EnumSet.of(BodyPart.LEGS), PilotPose.hiddenParts(new SuitView(Map.of())));
    }

    @Test
    void eachMountedPieceHidesItsPart() {
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.HEAD), hidden(PieceSlot.HELMET, INTACT));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.BODY), hidden(PieceSlot.TORSO, INTACT));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.LEFT_ARM), hidden(PieceSlot.LEFT_ARM, INTACT));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.RIGHT_ARM), hidden(PieceSlot.RIGHT_ARM, INTACT));
        // i pezzi delle gambe coprono parti gia' nascoste comunque
        assertEquals(EnumSet.of(BodyPart.LEGS), hidden(PieceSlot.LEFT_LEG, INTACT));
        assertEquals(EnumSet.of(BodyPart.LEGS), hidden(PieceSlot.RIGHT_LEG, INTACT));

        Map<PieceSlot, PieceView> all = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            all.put(slot, INTACT);
        }
        assertEquals(EnumSet.allOf(BodyPart.class), PilotPose.hiddenParts(new SuitView(all)));
    }

    @Test
    void brokenPieceStillHidesItsPart() {
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.HEAD), hidden(PieceSlot.HELMET, BROKEN));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.BODY), hidden(PieceSlot.TORSO, BROKEN));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.LEFT_ARM), hidden(PieceSlot.LEFT_ARM, BROKEN));
        assertEquals(EnumSet.of(BodyPart.LEGS, BodyPart.RIGHT_ARM), hidden(PieceSlot.RIGHT_ARM, BROKEN));
    }

    @Test
    void handShiftAtRestMovesBothArmsOutwardAndDown() {
        // Perni del braccio classico a riposo nel modello del giocatore (pixel, y verso il basso, piedi a y = 24).
        assertShift(-3.25f, 1.3f, 0f, PilotPose.handShiftPx(-5f, 2f, 0f)); // destro
        assertShift(3.25f, 1.3f, 0f, PilotPose.handShiftPx(5f, 2f, 0f)); // sinistro
    }

    @Test
    void handShiftFollowsTheSneakingArm() {
        // Accovacciati HumanoidModel porta le braccia a y = 5,2: (S - 1)(5,2 - 24) + 15,6 = 3,38.
        assertShift(-3.25f, 3.38f, 0f, PilotPose.handShiftPx(-5f, 5.2f, 0f));
        assertShift(3.25f, 3.38f, 0f, PilotPose.handShiftPx(5f, 5.2f, 0f));
    }

    @Test
    void handShiftScalesEveryAxisAboutTheFeet() {
        // Skin sottile (perno y = 2,5) e un perno spostato in z: ogni asse si scala di S - 1 attorno ai piedi.
        assertShift(-3.25f, 1.625f, 0f, PilotPose.handShiftPx(-5f, 2.5f, 0f));
        assertShift(3.25f, 1.3f, -0.65f, PilotPose.handShiftPx(5f, 2f, -1f));
    }

    /**
     * L'oggetto impugnato sta nella mano della tuta (design, par. 4.7): il punto d'impugnatura del gioco base, portato
     * sul perno del braccio della tuta, ingrandito di S e spostato di {@link PilotPose#handOffsetPx}, cade dentro un
     * cubo della mano (palmo, dita, pollice) dell'osso frameRightArm o frameLeftArm della geometria generata. I conti
     * sono nel sistema del braccio della tuta non scalato, cioe' in pixel della geometria (y verso l'alto, piedi a 0):
     * la tuta e' la geometria scalata di S, quindi tutto cio' che segue {@code scale(S)} vale in quei pixel.
     */
    @Test
    void heldItemGripFallsInsideTheSuitHand() throws IOException {
        assertGripInHand("frameRightArm", false, -5f);
        assertGripInHand("frameLeftArm", true, 5f);
    }

    private static void assertGripInHand(String bone, boolean leftArm, float pivotX) throws IOException {
        float side = leftArm ? 1f : -1f;
        PilotPose.Shift offset = PilotPose.handOffsetPx(leftArm);
        // Perno del braccio della tuta in pixel della geometria (il perno del modello armatura e' a y = 22).
        double x = pivotX + side * VANILLA_GRIP_X_PX + offset.x();
        double y = 22 - (VANILLA_GRIP_Y_PX + offset.y());
        double z = VANILLA_GRIP_Z_PX + offset.z();
        List<double[]> hand = handCubes(bone);
        assertTrue(hand.size() > 3, "mano di " + bone + " non trovata");
        boolean inside = false;
        for (double[] c : hand) {
            inside |= x >= c[0] && x <= c[3] && y >= c[1] && y <= c[4] && z >= c[2] && z <= c[5];
        }
        assertTrue(inside, "impugnatura di " + bone + " in (" + x + ", " + y + ", " + z + "), fuori dalla mano");
    }

    /** I cubi della mano di un osso del telaio: quelli che finiscono sotto il polso (y <= 11,25). */
    private static List<double[]> handCubes(String bone) throws IOException {
        List<double[]> cubes = new ArrayList<>();
        try (InputStream in = PilotPoseTest.class.getResourceAsStream(GEO)) {
            assertNotNull(in, "risorsa assente: " + GEO);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                for (JsonElement element : geometry.getAsJsonArray("bones")) {
                    JsonObject b = element.getAsJsonObject();
                    if (!b.get("name").getAsString().equals(bone) || !b.has("cubes")) {
                        continue;
                    }
                    for (JsonElement cube : b.getAsJsonArray("cubes")) {
                        JsonObject c = cube.getAsJsonObject();
                        double[] min = new double[3];
                        double[] max = new double[3];
                        for (int i = 0; i < 3; i++) {
                            min[i] = c.getAsJsonArray("origin").get(i).getAsDouble();
                            max[i] = min[i] + c.getAsJsonArray("size").get(i).getAsDouble();
                        }
                        if (max[1] <= 11.25) {
                            cubes.add(new double[] {min[0], min[1], min[2], max[0], max[1], max[2]});
                        }
                    }
                }
            }
        }
        return cubes;
    }

    private static void assertShift(float x, float y, float z, PilotPose.Shift shift) {
        assertEquals(x, shift.x(), 1e-4, "x");
        assertEquals(y, shift.y(), 1e-4, "y");
        assertEquals(z, shift.z(), 1e-4, "z");
    }

    private static Set<BodyPart> hidden(PieceSlot slot, PieceView piece) {
        return PilotPose.hiddenParts(new SuitView(Map.of(slot, piece)));
    }
}
