package com.toxineren.exoframes.core.frame;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.Activity;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyMeter;
import com.toxineren.exoframes.core.energy.EnergyPenalties;
import com.toxineren.exoframes.core.energy.EnergyRates;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.module.Activation;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Regole che trasformano uno {@link FrameState} negli {@link FrameEffects} sul portatore. Ogni valore viene dalla config. */
public final class FrameRules {

    /** Punti armatura di un set completo di ferro vanilla: la protezione di riferimento del livello SCRAP. */
    public static final int IRON_ARMOR_POINTS = 15;

    /** La spec non assegna robustezza (toughness) al telaio, come il ferro vanilla. */
    private static final double TOUGHNESS = 0.0;

    private static final int MILLI_PER_RE = 1000;

    private final ExoframesConfig cfg;

    public FrameRules(ExoframesConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
    }

    /** Calcola gli effetti attuali del telaio. Senza cella lo stato energetico e' EMPTY. */
    public FrameEffects evaluate(FrameState s) {
        EnergyState energy = energyState(s);
        EnergyPenalties penalties = EnergyPenalties.forState(energy, cfg.energy);

        Set<ModuleType> enabled = EnumSet.noneOf(ModuleType.class);
        if (penalties.modulesEnabled()) {
            for (ModuleType m : installed(s)) {
                if (activeEachTick(s, m) && moduleOperational(s, m)) {
                    enabled.add(m);
                }
            }
        }

        int moduleMilli = 0;
        for (ModuleType m : enabled) {
            moduleMilli += cfg.modules.costPerSecond.getOrDefault(m, 0) * MILLI_PER_RE / EnergyRates.TICKS_PER_SECOND;
        }

        double speed = 1.0 - cfg.frame.baseSpeedPenalty;
        if (enabled.contains(ModuleType.SERVOS)) {
            speed *= 1.0 + cfg.modules.servosSpeedBonus;
        }
        for (PieceSlot leg : new PieceSlot[] {PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG}) {
            if (mountedAndBroken(s, leg)) {
                speed *= 1.0 - cfg.damage.brokenLegSpeedPenalty;
            }
        }
        speed *= penalties.speedMultiplier();

        double protection = 0.0;
        for (Piece p : s.pieces().values()) {
            if (!p.isBroken()) {
                protection += cfg.pieces.shares.getOrDefault(p.slot(), 0.0) * IRON_ARMOR_POINTS * multiplier(p.tier());
            }
        }
        protection *= penalties.protectionMultiplier();

        double consumption = mountedAndBroken(s, PieceSlot.TORSO) ? cfg.damage.brokenTorsoConsumptionMultiplier : 1.0;

        return new FrameEffects(
                speed,
                penalties.canSprint(),
                true,
                cfg.frame.knockbackResistance,
                false,
                protection,
                TOUGHNESS,
                enabled,
                moduleMilli,
                consumption,
                energy);
    }

    /**
     * True se il modulo e' installato in uno slot con un pezzo montato e integro. I moduli che richiedono
     * entrambe le gambe (SHOCKWAVE) devono essere installati su ENTRAMBE le gambe, montate e integre.
     */
    public boolean moduleOperational(FrameState s, ModuleType m) {
        if (m.requiresBothLegs()) {
            return installedAndIntact(s, PieceSlot.LEFT_LEG, m) && installedAndIntact(s, PieceSlot.RIGHT_LEG, m);
        }
        for (Map.Entry<PieceSlot, List<ModuleType>> e : s.modules().entrySet()) {
            if (e.getValue().contains(m) && mountedAndIntact(s, e.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consuma un tick: attivita' piu' moduli abilitati, per il moltiplicatore del torso rotto.
     * Restituisce gli RE tolti dalla cella (0 senza cella).
     */
    public int tick(FrameState s, Activity a, EnergyMeter meter) {
        EnergyCell cell = s.cell();
        if (cell == null) {
            return 0;
        }
        FrameEffects fx = evaluate(s);
        return meter.tick(cell, a, fx.moduleMilliRePerTick(), fx.consumptionMultiplier());
    }

    /** Punti armatura di un set completo del livello: round(IRON_ARMOR_POINTS x moltiplicatore del livello). */
    public int protectionPointsFor(Tier t) {
        return (int) Math.round(IRON_ARMOR_POINTS * multiplier(t));
    }

    private double multiplier(Tier t) {
        return cfg.tiers.protectionMultiplier.getOrDefault(t, 0.0);
    }

    private EnergyState energyState(FrameState s) {
        EnergyCell cell = s.cell();
        return cell == null ? EnergyState.EMPTY : EnergyState.of(cell, cfg.energy.lowThreshold);
    }

    private static Set<ModuleType> installed(FrameState s) {
        Set<ModuleType> all = EnumSet.noneOf(ModuleType.class);
        s.modules().values().forEach(all::addAll);
        return all;
    }

    /** Passivi sempre, toggle se accesi; HOLD e ON_* sono a evento e non contano per tick. */
    private static boolean activeEachTick(FrameState s, ModuleType m) {
        Activation a = m.activation();
        return a == Activation.PASSIVE || (a == Activation.TOGGLE && s.activeToggles().contains(m));
    }

    private static boolean installedAndIntact(FrameState s, PieceSlot slot, ModuleType m) {
        return mountedAndIntact(s, slot) && s.modules().getOrDefault(slot, List.of()).contains(m);
    }

    private static boolean mountedAndIntact(FrameState s, PieceSlot slot) {
        Piece p = s.pieces().get(slot);
        return p != null && !p.isBroken();
    }

    private static boolean mountedAndBroken(FrameState s, PieceSlot slot) {
        Piece p = s.pieces().get(slot);
        return p != null && p.isBroken();
    }
}
