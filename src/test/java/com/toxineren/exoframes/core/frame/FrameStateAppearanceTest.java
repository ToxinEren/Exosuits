package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

/** L'aspetto per slot di {@link FrameState} segue il pezzo montato, come il riporto del danno. */
class FrameStateAppearanceTest {

    private final ExoframesConfig cfg = ExoframesConfig.defaults();
    private final Appearance painted = new Appearance(0x112233, 0x445566,
            List.of(new Decal("minecraft:creeper", "lime")));

    private Piece piece(PieceSlot slot) {
        return Piece.fresh(slot, Tier.SCRAP, cfg.tiers);
    }

    @Test
    void appearanceDefaultsUntilSet() {
        FrameState state = new FrameState(cfg);
        assertEquals(Appearance.DEFAULT, state.appearance(PieceSlot.TORSO));
        state.mount(piece(PieceSlot.TORSO));
        assertEquals(Appearance.DEFAULT, state.appearance(PieceSlot.TORSO));
        state.setAppearance(PieceSlot.TORSO, painted);
        assertEquals(painted, state.appearance(PieceSlot.TORSO));
        assertEquals(Appearance.DEFAULT, state.appearance(PieceSlot.HELMET));
        assertThrows(IllegalStateException.class, () -> state.setAppearance(PieceSlot.HELMET, painted));
    }

    @Test
    void mountAndUnmountResetAppearance() {
        FrameState state = new FrameState(cfg);
        state.mount(piece(PieceSlot.TORSO));
        state.setAppearance(PieceSlot.TORSO, painted);
        state.mount(piece(PieceSlot.TORSO));
        assertEquals(Appearance.DEFAULT, state.appearance(PieceSlot.TORSO));

        state.setAppearance(PieceSlot.TORSO, painted);
        state.unmount(PieceSlot.TORSO);
        assertEquals(Appearance.DEFAULT, state.appearance(PieceSlot.TORSO));
    }
}
