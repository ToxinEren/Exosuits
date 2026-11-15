package com.toxineren.exoframes.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import org.junit.jupiter.api.Test;

class ExoframesConfigTest {

    private static double sharesSum(ExoframesConfig cfg) {
        return cfg.pieces.shares.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Test
    void defaultsMatchSpec() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        assertEquals(10_000, cfg.energy.cellCapacity);
        assertEquals(2, cfg.energy.idlePerSecond);
        assertEquals(8, cfg.energy.walkPerSecond);
        assertEquals(20, cfg.energy.sprintPerSecond);
        assertEquals(15, cfg.energy.jumpCost);
        assertEquals(0.10, cfg.energy.lowThreshold, 1e-9);
        assertEquals(0.30, cfg.energy.emptySpeedMultiplier, 1e-9);
        assertEquals(0.50, cfg.energy.emptyProtectionMultiplier, 1e-9);
        assertEquals(500, cfg.energy.chargerRePerRedstone);
        assertEquals(50, cfg.energy.chargerRePerSecond);
        assertEquals(2, cfg.energy.cellSwapSeconds);
        assertEquals(0.60, cfg.damage.primaryShare, 1e-9);
        assertEquals(0.25, cfg.damage.repairKitFraction, 1e-9);
        assertEquals(1.25, cfg.damage.brokenTorsoConsumptionMultiplier, 1e-9);
        assertEquals(0.15, cfg.damage.brokenLegSpeedPenalty, 1e-9);
        assertEquals(0.30, cfg.pieces.shares.get(PieceSlot.TORSO), 1e-9);
        assertEquals(0.125, cfg.pieces.shares.get(PieceSlot.LEFT_ARM), 1e-9);
        assertEquals(2, cfg.pieces.baseModuleSlots.get(PieceSlot.TORSO));
        assertEquals(1, cfg.pieces.baseModuleSlots.get(PieceSlot.HELMET));
        assertEquals(180, cfg.tiers.maxDurability.get(Tier.SCRAP));
        assertEquals(360, cfg.tiers.maxDurability.get(Tier.INDUSTRIAL));
        assertEquals(520, cfg.tiers.maxDurability.get(Tier.ARMORED));
        assertEquals(650, cfg.tiers.maxDurability.get(Tier.NETHERITE));
        assertEquals(800, cfg.tiers.maxDurability.get(Tier.PROTOTYPE));
        assertEquals(1.733, cfg.tiers.protectionMultiplier.get(Tier.ARMORED), 1e-9);
        assertEquals(60, cfg.modules.costPerSecond.get(ModuleType.JETPACK));
        assertEquals(200, cfg.modules.costPerUse.get(ModuleType.SHOCKWAVE));
        assertEquals(10, cfg.modules.costPerUse.get(ModuleType.HYDRAULIC_BLADE));
        assertEquals(4, cfg.modules.jetpackMaxSeconds);
        assertEquals(6, cfg.modules.shockwaveMinFallBlocks);
        assertEquals(0.20, cfg.modules.servosSpeedBonus, 1e-9);
        assertEquals(6, cfg.modules.bladeBonusDamage);
        assertEquals(0.15, cfg.frame.baseSpeedPenalty, 1e-9);
        assertEquals(0.5, cfg.frame.knockbackResistance, 1e-9);
        assertEquals(1, cfg.frame.exitHoldSeconds);
        assertTrue(cfg.world.ruinedVillage);
        assertTrue(cfg.world.buriedLab);
        assertTrue(cfg.world.armoredIllager);
        assertTrue(cfg.world.chestLoot);
        assertTrue(cfg.server.ownerOnlyFrames);
    }

    @Test
    void sharesSumToOne() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        assertEquals(PieceSlot.values().length, cfg.pieces.shares.size());
        assertEquals(1.0, sharesSum(cfg), 1e-9);
    }

    @Test
    void validateNormalizesShares() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        for (PieceSlot slot : PieceSlot.values()) {
            cfg.pieces.shares.put(slot, 0.5);
        }
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        for (PieceSlot slot : PieceSlot.values()) {
            assertEquals(1.0 / 6.0, result.normalized().pieces.shares.get(slot), 1e-9, slot.name());
        }
        assertEquals(1.0, sharesSum(result.normalized()), 1e-9);
        assertEquals(1, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().get(0).contains("pieces.shares"), result.problems().toString());
        // L'input non viene modificato.
        assertEquals(0.5, cfg.pieces.shares.get(PieceSlot.TORSO), 1e-9);
        assertNotSame(cfg, result.normalized());
    }

    @Test
    void validateClampsThresholds() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        cfg.energy.lowThreshold = 1.5;
        cfg.energy.emptySpeedMultiplier = -0.2;
        cfg.damage.primaryShare = 2.0;
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        assertEquals(1.0, result.normalized().energy.lowThreshold, 1e-9);
        assertEquals(0.0, result.normalized().energy.emptySpeedMultiplier, 1e-9);
        assertEquals(1.0, result.normalized().damage.primaryShare, 1e-9);
        assertEquals(3, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.lowThreshold")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.emptySpeedMultiplier")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("damage.primaryShare")));
        assertEquals(1.5, cfg.energy.lowThreshold, 1e-9);
    }

    @Test
    void validateReplacesNegativeCapacity() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        cfg.energy.cellCapacity = -5;
        cfg.tiers.maxDurability.put(Tier.ARMORED, 0);
        cfg.modules.costPerSecond.put(ModuleType.DRILL, -1);
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        assertEquals(10_000, result.normalized().energy.cellCapacity);
        assertEquals(520, result.normalized().tiers.maxDurability.get(Tier.ARMORED));
        assertEquals(25, result.normalized().modules.costPerSecond.get(ModuleType.DRILL));
        assertEquals(3, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.cellCapacity")));
        assertEquals(-5, cfg.energy.cellCapacity);
    }

    @Test
    void validatedDefaultsHaveNoProblems() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        assertNotSame(cfg, result.normalized());
        assertEquals(10_000, result.normalized().energy.cellCapacity);
        assertEquals(1.0, sharesSum(result.normalized()), 1e-9);
        assertFalse(result.normalized().pieces.shares.isEmpty());
    }

    @Test
    void validateAcceptsZeroRatesAndCosts() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        cfg.energy.idlePerSecond = 0;
        cfg.energy.walkPerSecond = 0;
        cfg.energy.sprintPerSecond = 0;
        cfg.energy.jumpCost = 0;
        cfg.modules.costPerSecond.put(ModuleType.REBREATHER, 0);
        cfg.modules.costPerUse.put(ModuleType.SHOCKWAVE, 0);
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        assertEquals(0, result.normalized().energy.idlePerSecond);
        assertEquals(0, result.normalized().energy.walkPerSecond);
        assertEquals(0, result.normalized().energy.sprintPerSecond);
        assertEquals(0, result.normalized().energy.jumpCost);
        assertEquals(0, result.normalized().modules.costPerSecond.get(ModuleType.REBREATHER));
        assertEquals(0, result.normalized().modules.costPerUse.get(ModuleType.SHOCKWAVE));
    }

    @Test
    void validateKeepsCapacityDurabilityAndChargerStrictlyPositive() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        cfg.energy.cellCapacity = 0;
        cfg.energy.chargerRePerRedstone = 0;
        cfg.tiers.maxDurability.put(Tier.SCRAP, 0);
        cfg.energy.idlePerSecond = -1;
        ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
        assertEquals(10_000, result.normalized().energy.cellCapacity);
        assertEquals(500, result.normalized().energy.chargerRePerRedstone);
        assertEquals(180, result.normalized().tiers.maxDurability.get(Tier.SCRAP));
        assertEquals(2, result.normalized().energy.idlePerSecond);
        assertEquals(4, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.cellCapacity")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.chargerRePerRedstone")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("tiers.maxDurability.SCRAP")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("energy.idlePerSecond")));
    }

    @Test
    void upperBoundsFallBackToDefaults() {
        assertEquals(1_000_000, ConfigValidation.MAX_CELL_CAPACITY);
        assertEquals(100_000, ConfigValidation.MAX_RATE);
        assertEquals(1_000_000, ConfigValidation.MAX_RE_PER_REDSTONE);
        assertEquals(30, ConfigValidation.MAX_HOLD_SECONDS);
        assertEquals(10.0, ConfigValidation.MAX_MULTIPLIER, 1e-12);

        ExoframesConfig over = ExoframesConfig.defaults();
        over.energy.cellCapacity = ConfigValidation.MAX_CELL_CAPACITY + 1;
        over.energy.walkPerSecond = ConfigValidation.MAX_RATE + 1;
        over.energy.chargerRePerRedstone = ConfigValidation.MAX_RE_PER_REDSTONE + 1;
        over.frame.exitHoldSeconds = ConfigValidation.MAX_HOLD_SECONDS + 1;
        over.damage.brokenTorsoConsumptionMultiplier = ConfigValidation.MAX_MULTIPLIER + 0.5;
        ConfigValidation.ValidationResult result = ConfigValidation.validate(over);
        assertEquals(10_000, result.normalized().energy.cellCapacity);
        assertEquals(8, result.normalized().energy.walkPerSecond);
        assertEquals(500, result.normalized().energy.chargerRePerRedstone);
        assertEquals(1, result.normalized().frame.exitHoldSeconds);
        assertEquals(1.25, result.normalized().damage.brokenTorsoConsumptionMultiplier, 1e-12);
        assertEquals(5, result.problems().size(), result.problems().toString());
        for (String field : new String[] {"energy.cellCapacity", "energy.walkPerSecond", "energy.chargerRePerRedstone",
                "frame.exitHoldSeconds", "damage.brokenTorsoConsumptionMultiplier"}) {
            assertTrue(result.problems().stream().anyMatch(p -> p.startsWith(field)),
                    field + " " + result.problems());
        }

        ExoframesConfig atLimit = ExoframesConfig.defaults();
        atLimit.energy.cellCapacity = ConfigValidation.MAX_CELL_CAPACITY;
        atLimit.energy.walkPerSecond = ConfigValidation.MAX_RATE;
        atLimit.energy.chargerRePerRedstone = ConfigValidation.MAX_RE_PER_REDSTONE;
        atLimit.frame.exitHoldSeconds = ConfigValidation.MAX_HOLD_SECONDS;
        atLimit.damage.brokenTorsoConsumptionMultiplier = ConfigValidation.MAX_MULTIPLIER;
        ConfigValidation.ValidationResult kept = ConfigValidation.validate(atLimit);
        assertTrue(kept.problems().isEmpty(), kept.problems().toString());
        assertEquals(ConfigValidation.MAX_CELL_CAPACITY, kept.normalized().energy.cellCapacity);
        assertEquals(ConfigValidation.MAX_RATE, kept.normalized().energy.walkPerSecond);
        assertEquals(ConfigValidation.MAX_RE_PER_REDSTONE, kept.normalized().energy.chargerRePerRedstone);
        assertEquals(ConfigValidation.MAX_HOLD_SECONDS, kept.normalized().frame.exitHoldSeconds);
        assertEquals(ConfigValidation.MAX_MULTIPLIER, kept.normalized().damage.brokenTorsoConsumptionMultiplier, 1e-12);
    }

    @Test
    void chargerRePerSecondDefaultsToFifty() {
        assertEquals(50, ExoframesConfig.defaults().energy.chargerRePerSecond);
        int[][] cases = {{0, 50, 1}, {ConfigValidation.MAX_RATE + 1, 50, 1}, {70, 70, 0}};
        for (int[] c : cases) {
            ExoframesConfig cfg = ExoframesConfig.defaults();
            cfg.energy.chargerRePerSecond = c[0];
            ConfigValidation.ValidationResult result = ConfigValidation.validate(cfg);
            assertEquals(c[1], result.normalized().energy.chargerRePerSecond, "input " + c[0]);
            assertEquals(c[2], result.problems().size(), "input " + c[0] + " " + result.problems());
            if (c[2] == 1) {
                assertTrue(result.problems().get(0).startsWith("energy.chargerRePerSecond"),
                        result.problems().toString());
            }
        }
    }

    @Test
    void overflowingSharesRestoreDefaults() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        for (PieceSlot slot : PieceSlot.values()) {
            cfg.pieces.shares.put(slot, Double.MAX_VALUE); // finite ciascuna, somma +inf
        }
        assertDefaultSharesRestored(ConfigValidation.validate(cfg));
    }

    @Test
    void allZeroSharesRestoreDefaults() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        for (PieceSlot slot : PieceSlot.values()) {
            cfg.pieces.shares.put(slot, 0.0);
        }
        assertDefaultSharesRestored(ConfigValidation.validate(cfg));
    }

    private static void assertDefaultSharesRestored(ConfigValidation.ValidationResult result) {
        for (PieceSlot slot : PieceSlot.values()) {
            assertEquals(slot.protectionShare(), result.normalized().pieces.shares.get(slot), 1e-12, slot.name());
        }
        assertEquals(1.0, sharesSum(result.normalized()), 1e-9);
        assertEquals(1, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().get(0).startsWith("pieces.shares"), result.problems().toString());
    }
}
