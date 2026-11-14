package com.toxineren.exoframes.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.SuitBones;
import com.toxineren.exoframes.compat.client.SuitBoneRules.Region;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Le decisioni del renderer della tuta sulle ossa: visibilita' per pezzo e stato, regione e colore. */
class SuitBoneRulesTest {

    private static final SuitView BARE = new SuitView(Map.of());

    /** Tutti e sei i pezzi intatti, con colori diversi per slot. */
    private static SuitView full() {
        Map<PieceSlot, PieceView> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            pieces.put(slot, new PieceView(0, 0x110000 * (slot.ordinal() + 1), 0x000011 * (slot.ordinal() + 1)));
        }
        return new SuitView(pieces);
    }

    @Test
    void frameBonesAreAlwaysVisible() {
        for (SuitView view : new SuitView[] {BARE, full()}) {
            for (String bone : SuitBones.TOP_LEVEL) {
                assertTrue(SuitBoneRules.visible(bone, view), bone);
            }
            for (String part : SuitBones.PARTS) {
                assertTrue(SuitBoneRules.visible(SuitBones.frame(part), view), part);
            }
        }
    }

    @Test
    void onlyTheMountedStateOfAPieceIsVisible() {
        SuitView view = new SuitView(Map.of(PieceSlot.TORSO, new PieceView(2, 0xAA0000, 0x00BB00)));

        for (String region : SuitBones.REGIONS) {
            assertTrue(SuitBoneRules.visible(SuitBones.piece(PieceSlot.TORSO, 2, region), view), region);
            for (int state : new int[] {0, 1, 3}) {
                String bone = SuitBones.piece(PieceSlot.TORSO, state, region);
                assertFalse(SuitBoneRules.visible(bone, view), bone);
            }
        }
        assertTrue(SuitBoneRules.visible("pieceTorso_s2_p1", view));
        assertFalse(SuitBoneRules.visible("pieceTorso_s0_p1", view));
    }

    @Test
    void piecesOfEmptySlotsAreHidden() {
        SuitView torsoOnly = new SuitView(Map.of(PieceSlot.TORSO, new PieceView(0, 0, 0)));
        for (PieceSlot slot : PieceSlot.values()) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (String region : SuitBones.REGIONS) {
                    String bone = SuitBones.piece(slot, state, region);
                    assertFalse(SuitBoneRules.visible(bone, BARE), bone);
                    assertEquals(slot == PieceSlot.TORSO && state == 0, SuitBoneRules.visible(bone, torsoOnly),
                            bone);
                }
            }
        }
    }

    @Test
    void decalBonesStayHidden() {
        for (PieceSlot slot : SuitBones.DECAL_SLOTS) {
            assertFalse(SuitBoneRules.visible(SuitBones.decal(slot), BARE), slot.name());
            assertFalse(SuitBoneRules.visible(SuitBones.decal(slot), full()), slot.name());
        }
    }

    @Test
    void regionsFollowTheBoneSuffix() {
        for (PieceSlot slot : PieceSlot.values()) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                assertEquals(Region.PRIMARY, SuitBoneRules.region(SuitBones.piece(slot, state, SuitBones.PRIMARY)));
                assertEquals(Region.SECONDARY,
                        SuitBoneRules.region(SuitBones.piece(slot, state, SuitBones.SECONDARY)));
                assertEquals(Region.NEUTRAL, SuitBoneRules.region(SuitBones.piece(slot, state, SuitBones.NEUTRAL)));
            }
        }
        // telaio, ossa di primo livello, decalcomanie e ossa che le regole non conoscono non si colorano
        for (String bone : SuitBones.TOP_LEVEL) {
            assertEquals(Region.NEUTRAL, SuitBoneRules.region(bone));
        }
        assertEquals(Region.NEUTRAL, SuitBoneRules.region(SuitBones.frame("Body")));
        assertEquals(Region.NEUTRAL, SuitBoneRules.region(SuitBones.decal(PieceSlot.TORSO)));
        assertEquals(Region.NEUTRAL, SuitBoneRules.region("someOtherBone_p1"));
    }

    @Test
    void colorsComeFromThePieceOfTheBoneSlot() {
        SuitView view = full();
        for (PieceSlot slot : PieceSlot.values()) {
            PieceView piece = view.pieces().get(slot);
            assertEquals(piece.primaryRgb(), SuitBoneRules.rgb(SuitBones.piece(slot, 0, SuitBones.PRIMARY), view));
            assertEquals(piece.secondaryRgb(),
                    SuitBoneRules.rgb(SuitBones.piece(slot, 0, SuitBones.SECONDARY), view));
            assertEquals(SuitBoneRules.WHITE, SuitBoneRules.rgb(SuitBones.piece(slot, 0, SuitBones.NEUTRAL), view));
            // un pezzo assente lascia il bianco (l'osso e' nascosto)
            assertEquals(SuitBoneRules.WHITE, SuitBoneRules.rgb(SuitBones.piece(slot, 0, SuitBones.PRIMARY), BARE));
        }
        assertEquals(SuitBoneRules.WHITE, SuitBoneRules.rgb(SuitBones.ARMOR_BODY, view));
    }

    @Test
    void bonesListsEveryControlledBoneOnceWithFrameFirst() {
        Set<String> seen = new HashSet<>(SuitBoneRules.BONES);
        assertEquals(SuitBoneRules.BONES.size(), seen.size());
        // 6 di primo livello + 6 del telaio + 6 slot x 4 stati x 3 regioni + 2 decalcomanie
        assertEquals(6 + 6 + 6 * 4 * 3 + 2, SuitBoneRules.BONES.size());
        // le ossa sempre visibili vengono prima: mostrarle riaccende i figli, che le regole dei pezzi poi decidono
        for (int i = 0; i < 12; i++) {
            assertTrue(SuitBoneRules.visible(SuitBoneRules.BONES.get(i), BARE), SuitBoneRules.BONES.get(i));
        }
        assertThrows(IllegalArgumentException.class, () -> SuitBoneRules.visible("someOtherBone", BARE));
    }
}
