package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.toxineren.exoframes.core.module.ModuleCondition;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import java.util.Set;
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

    private static int run(FrameRules rules, FrameState s, Activity a, EnergyMeter meter, FrameContext ctx,
            Set<ModuleType> held, int ticks) {
        int total = 0;
        for (int i = 0; i < ticks; i++) {
            total += rules.tick(s, a, meter, ctx, held);
        }
        return total;
    }

    private EnergyMeter meter() {
        return new EnergyMeter(EnergyRates.from(cfg.energy));
    }

    /** Telaio con cella piena e onda d'urto installata su entrambe le gambe. */
    private FrameState shockwaveFrame() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.LEFT_LEG));
        s.mount(scrap(PieceSlot.RIGHT_LEG));
        s.installModule(PieceSlot.LEFT_LEG, ModuleType.SHOCKWAVE);
        s.installModule(PieceSlot.RIGHT_LEG, ModuleType.SHOCKWAVE);
        return s;
    }

    private FrameState jetpackFrame() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.TORSO));
        assertEquals(ModuleInstaller.Reason.OK, s.installModule(PieceSlot.TORSO, ModuleType.JETPACK));
        return s;
    }

    @Test
    void rebreatherCostsOnlyUnderwater() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.HELMET));
        s.installModule(PieceSlot.HELMET, ModuleType.REBREATHER);
        assertEquals(ModuleCondition.SUBMERGED, ModuleType.REBREATHER.condition());
        assertFalse(rules.evaluate(s).enabledModules().contains(ModuleType.REBREATHER));
        assertFalse(rules.evaluate(s, FrameContext.NONE).enabledModules().contains(ModuleType.REBREATHER));
        FrameContext underwater = new FrameContext(true);
        assertTrue(rules.evaluate(s, underwater).enabledModules().contains(ModuleType.REBREATHER));

        assertEquals(2, run(rules, s, Activity.IDLE, meter(), 20));
        assertEquals(2, run(rules, s, Activity.IDLE, meter(), FrameContext.NONE, Set.of(), 20));
        assertEquals(6, run(rules, s, Activity.IDLE, meter(), underwater, Set.of(), 20));
        assertEquals(cfg.energy.cellCapacity - 10, s.cell().stored());
    }

    @Test
    void heldJetpackCostsSixtyPerSecond() {
        FrameState s = jetpackFrame();
        assertEquals(62, run(rules, s, Activity.IDLE, meter(), FrameContext.NONE, Set.of(ModuleType.JETPACK), 20));
        // senza tenerlo premuto costa solo l'attivita'
        assertEquals(2, run(rules, s, Activity.IDLE, meter(), FrameContext.NONE, Set.of(), 20));
    }

    @Test
    void heldIgnoresNonHoldAndUnmountedModules() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.HELMET));
        s.installModule(PieceSlot.HELMET, ModuleType.NIGHT_VISION);
        // NIGHT_VISION e' un toggle spento: tenerlo non costa; JETPACK non e' installato
        Set<ModuleType> held = Set.of(ModuleType.NIGHT_VISION, ModuleType.JETPACK, ModuleType.SHOCKWAVE);
        assertEquals(2, run(rules, s, Activity.IDLE, meter(), FrameContext.NONE, held, 20));
    }

    @Test
    void lowEnergyBlocksActivation() {
        FrameState s = jetpackFrame();
        assertTrue(rules.canActivate(s, ModuleType.JETPACK));
        s.insertCell(new EnergyCell(cfg.energy.cellCapacity, 999));
        assertFalse(rules.canActivate(s, ModuleType.JETPACK));
        // il jetpack tenuto non costa nulla: restano i 2 RE dell'attivita'
        assertEquals(2, run(rules, s, Activity.IDLE, meter(), FrameContext.NONE, Set.of(ModuleType.JETPACK), 20));
        s.insertCell(new EnergyCell(cfg.energy.cellCapacity, 1_000));
        assertTrue(rules.canActivate(s, ModuleType.JETPACK));
        s.removeCell();
        assertFalse(rules.canActivate(s, ModuleType.JETPACK));
        assertFalse(rules.canActivate(poweredFrame(), ModuleType.JETPACK)); // non installato
    }

    @Test
    void useDrainsPerUseCostOnce() {
        FrameState s = shockwaveFrame();
        assertTrue(rules.use(s, ModuleType.SHOCKWAVE));
        assertEquals(cfg.energy.cellCapacity - 200, s.cell().stored());

        // 150 RE su 10 000: sotto il 10 %, stato LOW
        s.insertCell(new EnergyCell(cfg.energy.cellCapacity, 150));
        assertFalse(rules.canActivate(s, ModuleType.SHOCKWAVE));
        assertFalse(rules.use(s, ModuleType.SHOCKWAVE));
        assertEquals(150, s.cell().stored());

        // 150 RE su 1 000: stato NORMAL ma meno del costo per uso
        s.insertCell(new EnergyCell(1_000, 150));
        assertFalse(rules.canActivate(s, ModuleType.SHOCKWAVE));
        assertFalse(rules.use(s, ModuleType.SHOCKWAVE));
        assertEquals(150, s.cell().stored());

        // modulo non operativo: nessun costo
        FrameState noBlade = poweredFrame();
        assertFalse(rules.use(noBlade, ModuleType.HYDRAULIC_BLADE));
        assertEquals(cfg.energy.cellCapacity, noBlade.cell().stored());
    }

    @Test
    void useRejectsModulesThatAreNotPerUse() {
        FrameState s = jetpackFrame();
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> rules.use(s, ModuleType.JETPACK));
        assertEquals("not a per-use module: JETPACK", e.getMessage());
        assertEquals(cfg.energy.cellCapacity, s.cell().stored());
    }

    @Test
    void brokenTorsoRaisesJumpCost() {
        FrameState s = poweredFrame();
        assertEquals(15, rules.jump(s, meter()));
        s.mount(scrap(PieceSlot.TORSO));
        breakPiece(s, PieceSlot.TORSO);
        assertEquals(19, rules.jump(s, meter()));
        assertEquals(cfg.energy.cellCapacity - 15 - 19, s.cell().stored());
    }

    @Test
    void jumpWithoutCellIsFree() {
        assertEquals(0, rules.jump(new FrameState(cfg), meter()));
    }

    @Test
    void unmountResetsDamageCarry() {
        FrameState s = poweredFrame();
        s.mount(scrap(PieceSlot.TORSO));
        s.damageCarryMilli().put(PieceSlot.TORSO, 700);
        s.damageCarryMilli().put(PieceSlot.HELMET, -200);
        s.unmount(PieceSlot.TORSO);
        assertEquals(0, s.damageCarryMilli().getOrDefault(PieceSlot.TORSO, 0));
        assertEquals(-200, s.damageCarryMilli().get(PieceSlot.HELMET));

        // sostituire il pezzo con mount equivale a smontarlo: il nuovo pezzo parte senza riporto
        s.mount(scrap(PieceSlot.HELMET));
        assertEquals(0, s.damageCarryMilli().getOrDefault(PieceSlot.HELMET, 0));
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
