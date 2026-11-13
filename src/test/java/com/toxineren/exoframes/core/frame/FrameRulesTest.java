package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.Activity;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyMeter;
import com.toxineren.exoframes.core.energy.EnergyRates;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrameRulesTest {

    private static final double EPS = 1e-9;

    private final ExoframesConfig cfg = ExoframesConfig.defaults();
    private final FrameRules rules = new FrameRules(cfg);

    /** Telaio con una cella piena e nessun pezzo. */
    private FrameState poweredFrame() {
        FrameState s = new FrameState(cfg);
        s.insertCell(EnergyCell.full(cfg.energy.cellCapacity));
        return s;
    }

    private Piece scrap(PieceSlot slot) {
        return Piece.fresh(slot, Tier.SCRAP, cfg.tiers);
    }

    private FrameState fullScrapSet() {
        return fullSet(Tier.SCRAP);
    }

    private FrameState fullSet(Tier tier) {
        FrameState s = poweredFrame();
        for (PieceSlot slot : PieceSlot.values()) {
            s.mount(Piece.fresh(slot, tier, cfg.tiers));
        }
        return s;
    }

    private static void breakPiece(FrameState s, PieceSlot slot) {
        Piece p = s.pieces().get(slot);
        p.damage(p.durability());
    }

    private static int run(FrameRules rules, FrameState s, Activity a, EnergyMeter meter, int ticks) {
        int total = 0;
        for (int i = 0; i < ticks; i++) {
            total += rules.tick(s, a, meter);
        }
        return total;
    }

    @Test
    void emptyFrameHasBaseSpeedPenalty() {
        FrameEffects fx = rules.evaluate(poweredFrame());
        assertEquals(0.85, fx.speedMultiplier(), EPS);
        assertTrue(fx.canSprint());
        assertTrue(fx.fallDamageImmune());
        assertFalse(fx.canSwim());
        assertEquals(0.5, fx.knockbackResistance(), EPS);
        assertEquals(0.0, fx.protectionPoints(), EPS);
        assertEquals(EnergyState.NORMAL, fx.energyState());
    }

    @Test
    void servosCancelBasePenalty() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.LEFT_LEG));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.LEFT_LEG, ModuleType.SERVOS));
        FrameEffects fx = rules.evaluate(s);
        assertEquals(1.02, fx.speedMultiplier(), EPS);
        assertTrue(fx.enabledModules().contains(ModuleType.SERVOS));
    }

    @Test
    void brokenLegSlowsFurther() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.LEFT_LEG));
        s.mount(scrap(PieceSlot.RIGHT_LEG));
        breakPiece(s, PieceSlot.LEFT_LEG);
        assertEquals(0.85 * 0.85, rules.evaluate(s).speedMultiplier(), EPS);
        breakPiece(s, PieceSlot.RIGHT_LEG);
        assertEquals(0.85 * 0.85 * 0.85, rules.evaluate(s).speedMultiplier(), EPS);
    }

    @Test
    void fullScrapSetProtectsLikeIron() {
        assertEquals(15.0, rules.evaluate(fullScrapSet()).protectionPoints(), EPS);
        assertEquals(15, rules.evaluate(fullScrapSet()).armorPoints());
        assertEquals(15, rules.protectionPointsFor(Tier.SCRAP));
        assertEquals(30, rules.protectionPointsFor(Tier.PROTOTYPE));
    }

    @Test
    void brokenHelmetRemovesItsShare() {
        FrameState s = fullScrapSet();
        breakPiece(s, PieceSlot.HELMET);
        assertEquals(12.75, rules.evaluate(s).protectionPoints(), EPS);
    }

    @Test
    void lowEnergyDisablesModulesAndSprint() {
        FrameState s = new FrameState(cfg);
        s.insertCell(new EnergyCell(10_000, 500));
        s.mount(scrap(PieceSlot.LEFT_LEG));
        s.mount(scrap(PieceSlot.HELMET));
        s.installModule(PieceSlot.LEFT_LEG, ModuleType.SERVOS);
        s.installModule(PieceSlot.HELMET, ModuleType.NIGHT_VISION);
        s.activeToggles().add(ModuleType.NIGHT_VISION);
        FrameEffects fx = rules.evaluate(s);
        assertEquals(EnergyState.LOW, fx.energyState());
        assertFalse(fx.canSprint());
        assertTrue(fx.enabledModules().isEmpty());
        assertEquals(0, fx.moduleMilliRePerTick());
        assertEquals(0.85, fx.speedMultiplier(), EPS);
    }

    @Test
    void emptyEnergyHalvesProtection() {
        FrameState s = fullScrapSet();
        s.removeCell();
        s.insertCell(EnergyCell.empty(cfg.energy.cellCapacity));
        FrameEffects fx = rules.evaluate(s);
        assertEquals(EnergyState.EMPTY, fx.energyState());
        assertEquals(7.5, fx.protectionPoints(), EPS);
        assertEquals(0.85 * 0.30, fx.speedMultiplier(), EPS);
    }

    @Test
    void shockwaveNeedsBothLegs() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.LEFT_LEG));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.LEFT_LEG, ModuleType.SHOCKWAVE));
        assertFalse(rules.moduleOperational(s, ModuleType.SHOCKWAVE));
        s.mount(scrap(PieceSlot.RIGHT_LEG));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.RIGHT_LEG, ModuleType.SHOCKWAVE));
        assertTrue(rules.moduleOperational(s, ModuleType.SHOCKWAVE));
        breakPiece(s, PieceSlot.RIGHT_LEG);
        assertFalse(rules.moduleOperational(s, ModuleType.SHOCKWAVE));
        // evento all'atterraggio: mai abilitato per tick, quindi nessun costo continuo
        assertFalse(rules.evaluate(s).enabledModules().contains(ModuleType.SHOCKWAVE));
    }

    @Test
    void swappingPieceDropsItsModules() {
        FrameState s = poweredFrame();
        s.mount(Piece.fresh(PieceSlot.TORSO, Tier.ARMORED, cfg.tiers));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.TORSO, ModuleType.ENERGY_SHIELD));
        s.activeToggles().add(ModuleType.ENERGY_SHIELD);
        assertTrue(rules.evaluate(s).enabledModules().contains(ModuleType.ENERGY_SHIELD));

        s.unmount(PieceSlot.TORSO);
        s.mount(scrap(PieceSlot.TORSO));

        assertFalse(rules.evaluate(s).enabledModules().contains(ModuleType.ENERGY_SHIELD));
        assertTrue(s.modules().getOrDefault(PieceSlot.TORSO, List.of()).isEmpty());
        assertFalse(s.activeToggles().contains(ModuleType.ENERGY_SHIELD));
    }

    @Test
    void takeModulesHandsBackTheSlotModules() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.HELMET));
        s.installModule(PieceSlot.HELMET, ModuleType.NIGHT_VISION);
        s.activeToggles().add(ModuleType.NIGHT_VISION);
        assertEquals(List.of(ModuleType.NIGHT_VISION), s.takeModules(PieceSlot.HELMET));
        assertTrue(s.modules().getOrDefault(PieceSlot.HELMET, List.of()).isEmpty());
        assertFalse(s.activeToggles().contains(ModuleType.NIGHT_VISION));
        assertEquals(List.of(), s.takeModules(PieceSlot.HELMET));
    }

    @Test
    void tickChargesActiveModules() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.HELMET));
        s.installModule(PieceSlot.HELMET, ModuleType.NIGHT_VISION);
        s.activeToggles().add(ModuleType.NIGHT_VISION);
        EnergyMeter meter = new EnergyMeter(EnergyRates.from(cfg.energy));
        assertEquals(5, run(rules, s, Activity.IDLE, meter, 20));
        assertEquals(cfg.energy.cellCapacity - 5, s.cell().stored());
    }

    @Test
    void brokenTorsoRaisesConsumption() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.TORSO));
        breakPiece(s, PieceSlot.TORSO);
        assertEquals(1.25, rules.evaluate(s).consumptionMultiplier(), EPS);
        EnergyMeter meter = new EnergyMeter(EnergyRates.from(cfg.energy));
        assertEquals(10, run(rules, s, Activity.WALK, meter, 20));
    }

    @Test
    void shockwaveOnLeftLegOnlyIsNotOperational() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.LEFT_LEG));
        s.mount(scrap(PieceSlot.RIGHT_LEG));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.LEFT_LEG, ModuleType.SHOCKWAVE));
        assertFalse(rules.moduleOperational(s, ModuleType.SHOCKWAVE));
    }

    @Test
    void fullIndustrialSetProtectsLikeDiamond() {
        FrameEffects fx = rules.evaluate(fullSet(Tier.INDUSTRIAL));
        assertEquals(15 * 1.333, fx.protectionPoints(), EPS); // 19,995: l'attributo vanilla (floor) darebbe 19
        assertEquals(20, fx.armorPoints());
    }

    @Test
    void fullArmoredSetIsDiamondPlusThirtyPercent() {
        FrameEffects fx = rules.evaluate(fullSet(Tier.ARMORED));
        assertEquals(15 * 1.733, fx.protectionPoints(), EPS); // 25,995
        assertEquals(26, fx.armorPoints());
    }

    @Test
    void fullNetheriteSetHas24ArmorPoints() {
        assertEquals(24, rules.evaluate(fullSet(Tier.NETHERITE)).armorPoints());
    }

    @Test
    void fullPrototypeSetHas30ArmorPoints() {
        assertEquals(30, rules.evaluate(fullSet(Tier.PROTOTYPE)).armorPoints());
    }

    /**
     * Set misto, calcolato a mano con le quote e i moltiplicatori di default:
     * elmo PROTOTYPE 0,15 x 15 x 2,000 = 4,5; torso ARMORED 0,30 x 15 x 1,733 = 7,7985;
     * braccia INDUSTRIAL 2 x 0,125 x 15 x 1,333 = 4,99875; gambe NETHERITE 2 x 0,15 x 15 x 1,600 = 7,2;
     * totale 24,49725 → 24 punti armatura.
     */
    @Test
    void mixedTierSetHas24ArmorPoints() {
        FrameState s = poweredFrame();
        s.mount(Piece.fresh(PieceSlot.HELMET, Tier.PROTOTYPE, cfg.tiers));
        s.mount(Piece.fresh(PieceSlot.TORSO, Tier.ARMORED, cfg.tiers));
        s.mount(Piece.fresh(PieceSlot.LEFT_ARM, Tier.INDUSTRIAL, cfg.tiers));
        s.mount(Piece.fresh(PieceSlot.RIGHT_ARM, Tier.INDUSTRIAL, cfg.tiers));
        s.mount(Piece.fresh(PieceSlot.LEFT_LEG, Tier.NETHERITE, cfg.tiers));
        s.mount(Piece.fresh(PieceSlot.RIGHT_LEG, Tier.NETHERITE, cfg.tiers));
        FrameEffects fx = rules.evaluate(s);
        assertEquals(24.49725, fx.protectionPoints(), EPS);
        assertEquals(24, fx.armorPoints());
        assertEquals(EnergyState.NORMAL, fx.energyState());
    }

    @Test
    void frameWithoutCellIsEmptyAndTickDrainsNothing() {
        FrameState s = new FrameState(cfg);
        for (PieceSlot slot : PieceSlot.values()) {
            s.mount(scrap(slot));
        }
        s.installModule(PieceSlot.LEFT_LEG, ModuleType.SERVOS);
        FrameEffects fx = rules.evaluate(s);
        assertEquals(EnergyState.EMPTY, fx.energyState());
        assertTrue(fx.enabledModules().isEmpty());
        assertEquals(7.5, fx.protectionPoints(), EPS);
        assertEquals(0.85 * 0.30, fx.speedMultiplier(), EPS);
        EnergyMeter meter = new EnergyMeter(EnergyRates.from(cfg.energy));
        assertEquals(0, rules.tick(s, Activity.SPRINT, meter));
        assertEquals(0, run(rules, s, Activity.WALK, meter, 20));
    }
}
