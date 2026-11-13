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
import com.toxineren.exoframes.core.module.ModuleCondition;
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

    /** Scorciatoia per {@link #evaluate(FrameState, FrameContext)} con {@link FrameContext#NONE}. */
    public FrameEffects evaluate(FrameState s) {
        return evaluate(s, FrameContext.NONE);
    }

    /**
     * Calcola gli effetti attuali del telaio. Senza cella lo stato energetico e' EMPTY. Un modulo passivo o toggle
     * e' abilitato (e consuma) solo se la sua {@link ModuleCondition} e' soddisfatta dal contesto.
     */
    public FrameEffects evaluate(FrameState s, FrameContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        EnergyState energy = energyState(s);
        EnergyPenalties penalties = EnergyPenalties.forState(energy, cfg.energy);

        Set<ModuleType> enabled = EnumSet.noneOf(ModuleType.class);
        if (penalties.modulesEnabled()) {
            for (ModuleType m : installed(s)) {
                if (activeEachTick(s, m) && conditionMet(m, ctx) && moduleOperational(s, m)) {
                    enabled.add(m);
                }
            }
        }

        int moduleMilli = 0;
        for (ModuleType m : enabled) {
            moduleMilli += milliRePerTick(m);
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

        double consumption = consumptionMultiplier(s);

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
     * True se il modulo puo' essere attivato ora: e' operativo, lo stato energetico e' NORMAL e, per i moduli a
     * singolo uso, la cella contiene almeno {@code modules.costPerUse}.
     */
    public boolean canActivate(FrameState s, ModuleType m) {
        Objects.requireNonNull(m, "module");
        if (!moduleOperational(s, m) || energyState(s) != EnergyState.NORMAL) {
            return false;
        }
        int perUse = cfg.modules.costPerUse.getOrDefault(m, 0);
        return perUse <= 0 || s.cell().stored() >= perUse;
    }

    /** Scorciatoia per {@link #tick(FrameState, Activity, EnergyMeter, FrameContext, Set)} senza contesto ne' moduli tenuti. */
    public int tick(FrameState s, Activity a, EnergyMeter meter) {
        return tick(s, a, meter, FrameContext.NONE, Set.of());
    }

    /**
     * Consuma un tick: attivita' piu' moduli abilitati nel contesto, piu' i moduli {@code HOLD} tenuti premuti
     * ({@code held}) che {@link #canActivate} consente, il tutto per il moltiplicatore del torso rotto. I moduli di
     * {@code held} che non sono {@code HOLD} vengono ignorati. Restituisce gli RE tolti dalla cella (0 senza cella).
     */
    public int tick(FrameState s, Activity a, EnergyMeter meter, FrameContext ctx, Set<ModuleType> held) {
        Objects.requireNonNull(held, "held");
        EnergyCell cell = s.cell();
        if (cell == null) {
            return 0;
        }
        FrameEffects fx = evaluate(s, ctx);
        int moduleMilli = fx.moduleMilliRePerTick();
        for (ModuleType m : held) {
            if (m.activation() == Activation.HOLD && canActivate(s, m)) {
                moduleMilli += milliRePerTick(m);
            }
        }
        return meter.tick(cell, a, moduleMilli, fx.consumptionMultiplier());
    }

    /**
     * Usa un modulo a evento ({@code ON_LANDING} o {@code ON_ATTACK}): se {@link #canActivate} lo consente toglie
     * {@code modules.costPerUse} dalla cella e restituisce true, altrimenti restituisce false senza toccare la cella.
     *
     * @throws IllegalArgumentException se il modulo non e' a singolo uso
     */
    public boolean use(FrameState s, ModuleType m) {
        Objects.requireNonNull(m, "module");
        if (m.activation() != Activation.ON_LANDING && m.activation() != Activation.ON_ATTACK) {
            throw new IllegalArgumentException("not a per-use module: " + m);
        }
        if (!canActivate(s, m)) {
            return false;
        }
        s.cell().drain(cfg.modules.costPerUse.getOrDefault(m, 0));
        return true;
    }

    /**
     * Toglie il costo del salto, {@code Math.round(jumpCost x moltiplicatore di consumo)} con il costo letto dalle
     * tariffe del contatore; restituisce gli RE effettivamente tolti (0 senza cella).
     */
    public int jump(FrameState s, EnergyMeter meter) {
        EnergyCell cell = s.cell();
        if (cell == null) {
            return 0;
        }
        return cell.drain((int) Math.round(meter.rates().jumpCost() * consumptionMultiplier(s)));
    }

    /** Punti armatura di un set completo del livello: round(IRON_ARMOR_POINTS x moltiplicatore del livello). */
    public int protectionPointsFor(Tier t) {
        return (int) Math.round(IRON_ARMOR_POINTS * multiplier(t));
    }

    private int milliRePerTick(ModuleType m) {
        return cfg.modules.costPerSecond.getOrDefault(m, 0) * MILLI_PER_RE / EnergyRates.TICKS_PER_SECOND;
    }

    /** Moltiplicatore dei consumi: quello del torso rotto se il torso e' montato e rotto, altrimenti 1. */
    private double consumptionMultiplier(FrameState s) {
        return mountedAndBroken(s, PieceSlot.TORSO) ? cfg.damage.brokenTorsoConsumptionMultiplier : 1.0;
    }

    private static boolean conditionMet(ModuleType m, FrameContext ctx) {
        return switch (m.condition()) {
            case ALWAYS -> true;
            case SUBMERGED -> ctx.submerged();
        };
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
