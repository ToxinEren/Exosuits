package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Il ripristino grezzo dei moduli di {@link FrameState}, senza i controlli dell'installatore. */
class FrameStateTest {

    private final ExoframesConfig cfg = ExoframesConfig.defaults();

    private FrameState withScrapTorso() {
        FrameState state = new FrameState(cfg);
        state.mount(Piece.fresh(PieceSlot.TORSO, Tier.SCRAP, cfg.tiers));
        return state;
    }

    @Test
    void restoreModulesSkipsInstallerChecks() {
        FrameState state = withScrapTorso();
        assertEquals(ModuleInstaller.Reason.TIER_TOO_LOW,
                state.installModule(PieceSlot.TORSO, ModuleType.ENERGY_SHIELD));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.TORSO, ModuleType.JETPACK));
        state.activeToggles().add(ModuleType.JETPACK);

        state.restoreModules(PieceSlot.TORSO, List.of(ModuleType.ENERGY_SHIELD));

        assertEquals(List.of(ModuleType.ENERGY_SHIELD), state.modules().get(PieceSlot.TORSO));
        // i moduli sostituiti lasciano lo slot insieme ai loro toggle
        assertFalse(state.activeToggles().contains(ModuleType.JETPACK));
    }

    @Test
    void restoreModulesNeedsAPiece() {
        FrameState state = new FrameState(cfg);
        assertThrows(IllegalStateException.class,
                () -> state.restoreModules(PieceSlot.TORSO, List.of(ModuleType.JETPACK)));
        assertFalse(state.modules().containsKey(PieceSlot.TORSO));
    }

    @Test
    void restoreModulesRejectsDuplicates() {
        FrameState state = withScrapTorso();
        state.installModule(PieceSlot.TORSO, ModuleType.JETPACK);
        assertThrows(IllegalArgumentException.class, () -> state.restoreModules(PieceSlot.TORSO,
                List.of(ModuleType.ENERGY_SHIELD, ModuleType.ENERGY_SHIELD)));
        assertThrows(IllegalArgumentException.class,
                () -> state.restoreModules(PieceSlot.TORSO, Arrays.asList(ModuleType.ENERGY_SHIELD, null)));
        // un rifiuto lascia lo slot com'era
        assertEquals(List.of(ModuleType.JETPACK), state.modules().get(PieceSlot.TORSO));
    }
}
