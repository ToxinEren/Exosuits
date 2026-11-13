package com.toxineren.exoframes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Nomi delle ossa della tuta costruiti da {@link SuitBones}: sono il contratto con la geometria generata. */
class SuitBonesTest {

    @Test
    void pieceNamesFollowTheContract() {
        assertEquals("pieceLeftArm_s2_p1", SuitBones.piece(PieceSlot.LEFT_ARM, 2, "p1"));
        assertEquals("pieceHelmet_s0_p2", SuitBones.piece(PieceSlot.HELMET, 0, "p2"));
        assertEquals("pieceTorso_s3_n", SuitBones.piece(PieceSlot.TORSO, 3, "n"));
        assertEquals("pieceRightArm_s1_p1", SuitBones.piece(PieceSlot.RIGHT_ARM, 1, SuitBones.PRIMARY));
        assertEquals("pieceLeftLeg_s1_p2", SuitBones.piece(PieceSlot.LEFT_LEG, 1, SuitBones.SECONDARY));
        assertEquals("pieceRightLeg_s0_n", SuitBones.piece(PieceSlot.RIGHT_LEG, 0, SuitBones.NEUTRAL));
        assertEquals("pieceTorso_decal", SuitBones.decal(PieceSlot.TORSO));
        assertEquals("pieceLeftArm_decal", SuitBones.decal(PieceSlot.LEFT_ARM));
        assertEquals("frameHead", SuitBones.frame("Head"));
        assertEquals("frameLeftLeg", SuitBones.frame("LeftLeg"));

        assertEquals(List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg",
                "armorLeftLeg"), SuitBones.TOP_LEVEL);
        assertEquals("armorHead", SuitBones.ARMOR_HEAD);
        assertEquals("armorBody", SuitBones.ARMOR_BODY);
        assertEquals("armorRightArm", SuitBones.ARMOR_RIGHT_ARM);
        assertEquals("armorLeftArm", SuitBones.ARMOR_LEFT_ARM);
        assertEquals("armorRightLeg", SuitBones.ARMOR_RIGHT_LEG);
        assertEquals("armorLeftLeg", SuitBones.ARMOR_LEFT_LEG);
        assertEquals(List.of("p1", "p2", "n"), SuitBones.REGIONS);
        assertEquals(4, SuitBones.STATES);

        // 6 slot x 4 stati x 3 regioni, tutti diversi
        Set<String> names = new HashSet<>();
        for (PieceSlot slot : PieceSlot.values()) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (String region : SuitBones.REGIONS) {
                    names.add(SuitBones.piece(slot, state, region));
                }
            }
        }
        assertEquals(72, names.size());
    }

    @Test
    void rejectsUnknownRegionAndState() {
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, 0, "p3"));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, 0, "P1"));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, 0, null));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, -1, "p1"));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.piece(PieceSlot.TORSO, 4, "p1"));
        assertThrows(NullPointerException.class, () -> SuitBones.piece(null, 0, "p1"));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.frame("Tail"));
        assertThrows(IllegalArgumentException.class, () -> SuitBones.frame("head"));
        assertThrows(NullPointerException.class, () -> SuitBones.decal(null));
    }
}
