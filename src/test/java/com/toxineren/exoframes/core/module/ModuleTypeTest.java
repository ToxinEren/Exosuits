package com.toxineren.exoframes.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Tier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModuleTypeTest {

    private static void check(ModuleType type, String id, ModuleFamily family, Activation activation,
            int perSecond, int perUse, Tier minTier) {
        assertEquals(id, type.id());
        assertEquals(family, type.family());
        assertEquals(activation, type.activation());
        assertEquals(perSecond, type.cost().perSecond());
        assertEquals(perUse, type.cost().perUse());
        assertEquals(minTier, type.minTier());
    }

    @Test
    void costsMatchSpecTable() {
        check(ModuleType.JETPACK, "jetpack", ModuleFamily.TORSO, Activation.HOLD, 60, 0, Tier.SCRAP);
        check(ModuleType.SHOCKWAVE, "shockwave", ModuleFamily.LEGS, Activation.ON_LANDING, 0, 200, Tier.SCRAP);
        check(ModuleType.NIGHT_VISION, "night_vision", ModuleFamily.HELMET, Activation.TOGGLE, 3, 0, Tier.SCRAP);
        check(ModuleType.SCANNER, "scanner", ModuleFamily.HELMET, Activation.TOGGLE, 5, 0, Tier.SCRAP);
        check(ModuleType.REBREATHER, "rebreather", ModuleFamily.HELMET, Activation.PASSIVE, 4, 0, Tier.SCRAP);
        check(ModuleType.HYDRAULIC_BLADE, "hydraulic_blade", ModuleFamily.ARM, Activation.ON_ATTACK, 0, 10, Tier.SCRAP);
        check(ModuleType.DRILL, "drill", ModuleFamily.ARM, Activation.HOLD, 25, 0, Tier.ARMORED);
        check(ModuleType.ENERGY_SHIELD, "energy_shield", ModuleFamily.TORSO, Activation.TOGGLE, 30, 0, Tier.ARMORED);
        check(ModuleType.SCULK_DETECTOR, "sculk_detector", ModuleFamily.HELMET, Activation.PASSIVE, 1, 0, Tier.SCRAP);
        check(ModuleType.SERVOS, "servos", ModuleFamily.LEGS, Activation.PASSIVE, 5, 0, Tier.SCRAP);
        assertEquals(10, ModuleType.values().length);
    }

    @Test
    void drillAndShieldNeedArmoredTier() {
        assertEquals(Tier.ARMORED, ModuleType.DRILL.minTier());
        assertEquals(Tier.ARMORED, ModuleType.ENERGY_SHIELD.minTier());
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (ModuleType type : ModuleType.values()) {
            assertTrue(ids.add(type.id()), "id duplicato: " + type.id());
        }
    }

    @Test
    void onlyShockwaveRequiresBothLegs() {
        for (ModuleType type : ModuleType.values()) {
            assertEquals(type == ModuleType.SHOCKWAVE, type.requiresBothLegs());
        }
        assertFalse(ModuleType.SERVOS.requiresBothLegs());
    }

    @Test
    void rebreatherIsSubmergedOnly() {
        for (ModuleType type : ModuleType.values()) {
            ModuleCondition expected = type == ModuleType.REBREATHER ? ModuleCondition.SUBMERGED : ModuleCondition.ALWAYS;
            assertEquals(expected, type.condition(), type.id());
        }
    }

    @Test
    void byIdRoundTrips() {
        for (ModuleType type : ModuleType.values()) {
            assertEquals(type, ModuleType.byId(type.id()));
        }
        assertThrows(IllegalArgumentException.class, () -> ModuleType.byId("nope"));
        assertThrows(IllegalArgumentException.class, () -> ModuleType.byId(null));
    }
}
