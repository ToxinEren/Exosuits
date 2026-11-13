package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.client.PilotPose.BodyPart;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Parti del pilota che non si disegnano dentro la tuta (design, par. 4.7 e 5.2). */
class PilotPoseTest {

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

    private static Set<BodyPart> hidden(PieceSlot slot, PieceView piece) {
        return PilotPose.hiddenParts(new SuitView(Map.of(slot, piece)));
    }
}
