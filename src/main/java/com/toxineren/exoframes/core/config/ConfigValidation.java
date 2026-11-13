package com.toxineren.exoframes.core.config;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Valida una {@link ExoframesConfig} e ne produce una copia normalizzata, senza modificare l'input.
 * Per ogni correzione aggiunge un messaggio a {@code problems}:
 *
 * <ul>
 *   <li>sezioni o mappe nulle, voci mancanti o nulle: ripristinate al default;</li>
 *   <li>soglie, frazioni e moltiplicatori in [0, 1]: fuori intervallo vengono clampati (NaN → default);</li>
 *   <li>consumi (energy.*PerSecond, energy.jumpCost) e costi dei moduli: se negativi tornano al default; 0 e'
 *       ammesso (un modpack puo' azzerare un consumo, es. quello da fermo);</li>
 *   <li>capacita' della cella, durabilita' massima dei livelli, RE per redstone del caricatore: devono essere
 *       &gt; 0 (con 0 la cella, il pezzo o il caricatore non avrebbero senso), altrimenti tornano al default;
 *       lo stesso vale per i moltiplicatori di protezione e la durata massima del jetpack;</li>
 *   <li>tempi, slot e conteggi: se negativi tornano al default (0 e' ammesso dove ha senso);</li>
 *   <li>quote dei pezzi: ogni quota non finita o negativa torna al default; se la somma e' finita e &gt; 0 ma
 *       diversa da 1 vengono normalizzate a somma 1; se la somma e' 0 o non finita (overflow a +inf con quote
 *       enormi) tornano tutte al default.</li>
 * </ul>
 *
 * <p>I messaggi sono in inglese (finiscono nei log dei server) e iniziano con il percorso del campo.
 */
public final class ConfigValidation {

    private static final double SHARE_TOLERANCE = 1e-9;

    /** Esito: elenco delle correzioni (vuoto se la config era valida) e la config normalizzata (nuova istanza). */
    public record ValidationResult(List<String> problems, ExoframesConfig normalized) {
        public ValidationResult {
            problems = List.copyOf(problems);
        }
    }

    private ConfigValidation() {}

    public static ValidationResult validate(ExoframesConfig input) {
        List<String> problems = new ArrayList<>();
        ExoframesConfig defaults = ExoframesConfig.defaults();
        ExoframesConfig out = new ExoframesConfig();
        if (input == null) {
            problems.add("config: missing, using defaults");
            return new ValidationResult(problems, out);
        }
        Ctx c = new Ctx(problems);

        if (present(input.energy, "energy", c)) {
            ExoframesConfig.Energy in = input.energy;
            ExoframesConfig.Energy d = defaults.energy;
            ExoframesConfig.Energy o = out.energy;
            o.cellCapacity = c.positive("energy.cellCapacity", in.cellCapacity, d.cellCapacity);
            // Consumi: 0 ammesso. cellCapacity e chargerRePerRedstone restano > 0 (0 non ha senso).
            o.idlePerSecond = c.nonNegative("energy.idlePerSecond", in.idlePerSecond, d.idlePerSecond);
            o.walkPerSecond = c.nonNegative("energy.walkPerSecond", in.walkPerSecond, d.walkPerSecond);
            o.sprintPerSecond = c.nonNegative("energy.sprintPerSecond", in.sprintPerSecond, d.sprintPerSecond);
            o.jumpCost = c.nonNegative("energy.jumpCost", in.jumpCost, d.jumpCost);
            o.lowThreshold = c.unit("energy.lowThreshold", in.lowThreshold, d.lowThreshold);
            o.emptySpeedMultiplier = c.unit("energy.emptySpeedMultiplier", in.emptySpeedMultiplier, d.emptySpeedMultiplier);
            o.emptyProtectionMultiplier =
                    c.unit("energy.emptyProtectionMultiplier", in.emptyProtectionMultiplier, d.emptyProtectionMultiplier);
            o.chargerRePerRedstone = c.positive("energy.chargerRePerRedstone", in.chargerRePerRedstone, d.chargerRePerRedstone);
            o.cellSwapSeconds = c.nonNegative("energy.cellSwapSeconds", in.cellSwapSeconds, d.cellSwapSeconds);
        }

        if (present(input.damage, "damage", c)) {
            ExoframesConfig.Damage in = input.damage;
            ExoframesConfig.Damage d = defaults.damage;
            ExoframesConfig.Damage o = out.damage;
            o.primaryShare = c.unit("damage.primaryShare", in.primaryShare, d.primaryShare);
            o.repairKitFraction = c.unit("damage.repairKitFraction", in.repairKitFraction, d.repairKitFraction);
            o.brokenTorsoConsumptionMultiplier = c.atLeastOne(
                    "damage.brokenTorsoConsumptionMultiplier", in.brokenTorsoConsumptionMultiplier,
                    d.brokenTorsoConsumptionMultiplier);
            o.brokenLegSpeedPenalty = c.unit("damage.brokenLegSpeedPenalty", in.brokenLegSpeedPenalty, d.brokenLegSpeedPenalty);
        }

        if (present(input.pieces, "pieces", c)) {
            validateShares(input.pieces.shares, out.pieces.shares, c);
            if (presentMap(input.pieces.baseModuleSlots, "pieces.baseModuleSlots", c)) {
                for (PieceSlot slot : PieceSlot.values()) {
                    int def = defaults.pieces.baseModuleSlots.get(slot);
                    out.pieces.baseModuleSlots.put(slot,
                            c.nonNegative("pieces.baseModuleSlots." + slot, input.pieces.baseModuleSlots.get(slot), def));
                }
            }
        }

        if (present(input.tiers, "tiers", c)) {
            if (presentMap(input.tiers.maxDurability, "tiers.maxDurability", c)) {
                for (Tier tier : Tier.values()) {
                    int def = defaults.tiers.maxDurability.get(tier);
                    // > 0: un pezzo con durabilita' massima 0 nascerebbe rotto.
                    out.tiers.maxDurability.put(tier,
                            c.positive("tiers.maxDurability." + tier, input.tiers.maxDurability.get(tier), def));
                }
            }
            if (presentMap(input.tiers.protectionMultiplier, "tiers.protectionMultiplier", c)) {
                for (Tier tier : Tier.values()) {
                    double def = defaults.tiers.protectionMultiplier.get(tier);
                    out.tiers.protectionMultiplier.put(tier, c.positive(
                            "tiers.protectionMultiplier." + tier, input.tiers.protectionMultiplier.get(tier), def));
                }
            }
        }

        if (present(input.modules, "modules", c)) {
            ExoframesConfig.Modules in = input.modules;
            ExoframesConfig.Modules d = defaults.modules;
            ExoframesConfig.Modules o = out.modules;
            validateCosts("modules.costPerSecond", in.costPerSecond, d.costPerSecond, o.costPerSecond, c);
            validateCosts("modules.costPerUse", in.costPerUse, d.costPerUse, o.costPerUse, c);
            o.jetpackMaxSeconds = c.positive("modules.jetpackMaxSeconds", in.jetpackMaxSeconds, d.jetpackMaxSeconds);
            o.shockwaveMinFallBlocks =
                    c.nonNegative("modules.shockwaveMinFallBlocks", in.shockwaveMinFallBlocks, d.shockwaveMinFallBlocks);
            o.servosSpeedBonus = c.unit("modules.servosSpeedBonus", in.servosSpeedBonus, d.servosSpeedBonus);
            o.bladeBonusDamage = c.nonNegative("modules.bladeBonusDamage", in.bladeBonusDamage, d.bladeBonusDamage);
        }

        if (present(input.frame, "frame", c)) {
            ExoframesConfig.Frame in = input.frame;
            ExoframesConfig.Frame d = defaults.frame;
            ExoframesConfig.Frame o = out.frame;
            o.baseSpeedPenalty = c.unit("frame.baseSpeedPenalty", in.baseSpeedPenalty, d.baseSpeedPenalty);
            o.knockbackResistance = c.unit("frame.knockbackResistance", in.knockbackResistance, d.knockbackResistance);
            o.exitHoldSeconds = c.nonNegative("frame.exitHoldSeconds", in.exitHoldSeconds, d.exitHoldSeconds);
        }

        if (present(input.world, "world", c)) {
            out.world.ruinedVillage = input.world.ruinedVillage;
            out.world.buriedLab = input.world.buriedLab;
            out.world.armoredIllager = input.world.armoredIllager;
            out.world.chestLoot = input.world.chestLoot;
        }

        if (present(input.server, "server", c)) {
            out.server.ownerOnlyFrames = input.server.ownerOnlyFrames;
        }

        return new ValidationResult(problems, out);
    }

    /** {@code out} parte gia' dai default: viene sovrascritta solo se le quote in ingresso sono utilizzabili. */
    private static void validateShares(Map<PieceSlot, Double> in, EnumMap<PieceSlot, Double> out, Ctx c) {
        if (!presentMap(in, "pieces.shares", c)) {
            return;
        }
        EnumMap<PieceSlot, Double> shares = new EnumMap<>(PieceSlot.class);
        double sum = 0.0;
        for (PieceSlot slot : PieceSlot.values()) {
            Double v = in.get(slot);
            double def = slot.protectionShare();
            double share;
            if (v == null) {
                c.problems.add("pieces.shares." + slot + ": missing, using default " + def);
                share = def;
            } else if (!Double.isFinite(v) || v < 0.0) {
                c.problems.add("pieces.shares." + slot + ": " + v
                        + " is invalid (must be finite and >= 0), using default " + def);
                share = def;
            } else {
                share = v;
            }
            shares.put(slot, share);
            sum += share;
        }
        if (!Double.isFinite(sum) || sum <= 0.0) {
            // Somma 0 (tutte nulle) o +inf (quote enormi in overflow): normalizzare darebbe quote nulle.
            c.problems.add("pieces.shares: sum is " + sum + " (must be finite and > 0), restoring default shares");
            return; // out contiene gia' i default
        }
        if (Math.abs(sum - 1.0) > SHARE_TOLERANCE) {
            c.problems.add("pieces.shares: sum is " + sum + " instead of 1, shares normalized to sum 1");
            for (PieceSlot slot : PieceSlot.values()) {
                shares.put(slot, shares.get(slot) / sum);
            }
        }
        out.putAll(shares);
    }

    /**
     * I moduli presenti nei default devono avere un costo &gt;= 0 (0 ammesso: modulo gratuito); le voci in piu'
     * (moduli senza quel tipo di costo) vengono scartate con un messaggio. {@code out} parte gia' dai default.
     */
    private static void validateCosts(String name, Map<ModuleType, Integer> in, Map<ModuleType, Integer> defaults,
            EnumMap<ModuleType, Integer> out, Ctx c) {
        if (!presentMap(in, name, c)) {
            return;
        }
        for (ModuleType type : ModuleType.values()) {
            Integer def = defaults.get(type);
            Integer v = in.get(type);
            if (def != null) {
                out.put(type, c.nonNegative(name + "." + type, v, def));
            } else if (v != null && v != 0) {
                c.problems.add(name + "." + type + ": this module has no such cost, value " + v + " ignored");
            }
        }
    }

    private static boolean present(Object section, String name, Ctx c) {
        if (section == null) {
            c.problems.add(name + ": section missing, using defaults");
            return false;
        }
        return true;
    }

    private static boolean presentMap(Map<?, ?> map, String name, Ctx c) {
        if (map == null) {
            c.problems.add(name + ": missing, using defaults");
            return false;
        }
        return true;
    }

    /** Raccoglie i messaggi; ogni metodo restituisce il valore corretto. */
    private record Ctx(List<String> problems) {

        int positive(String name, Integer v, int def) {
            if (v == null) {
                problems.add(name + ": missing, using default " + def);
                return def;
            }
            if (v <= 0) {
                problems.add(name + ": " + v + " is invalid (must be > 0), using default " + def);
                return def;
            }
            return v;
        }

        int nonNegative(String name, Integer v, int def) {
            if (v == null) {
                problems.add(name + ": missing, using default " + def);
                return def;
            }
            if (v < 0) {
                problems.add(name + ": " + v + " is invalid (must be >= 0), using default " + def);
                return def;
            }
            return v;
        }

        double positive(String name, Double v, double def) {
            if (v == null) {
                problems.add(name + ": missing, using default " + def);
                return def;
            }
            if (!Double.isFinite(v) || v <= 0.0) {
                problems.add(name + ": " + v + " is invalid (must be > 0), using default " + def);
                return def;
            }
            return v;
        }

        /** Valore in [0, 1]: fuori intervallo viene clampato, NaN torna al default. */
        double unit(String name, double v, double def) {
            if (Double.isNaN(v)) {
                problems.add(name + ": NaN is invalid, using default " + def);
                return def;
            }
            if (v < 0.0 || v > 1.0) {
                double clamped = Math.max(0.0, Math.min(1.0, v));
                problems.add(name + ": " + v + " is outside [0, 1], clamped to " + clamped);
                return clamped;
            }
            return v;
        }

        /** Moltiplicatore che non puo' ridurre: sotto 1 viene portato a 1, NaN/infinito torna al default. */
        double atLeastOne(String name, double v, double def) {
            if (!Double.isFinite(v)) {
                problems.add(name + ": " + v + " is invalid, using default " + def);
                return def;
            }
            if (v < 1.0) {
                problems.add(name + ": " + v + " is below 1, clamped to 1.0");
                return 1.0;
            }
            return v;
        }
    }
}
