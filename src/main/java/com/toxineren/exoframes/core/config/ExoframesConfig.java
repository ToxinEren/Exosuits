package com.toxineren.exoframes.core.config;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.EnumMap;

/**
 * Configurazione del mod: POJO con campi pubblici, una sezione annidata per area. Ogni costruttore senza
 * argomenti contiene i valori di specifica; la lettura/scrittura da file spetta a platform/. Le istanze
 * sono mutabili: per ottenere valori sicuri passare da {@link ConfigValidation#validate(ExoframesConfig)}.
 */
public final class ExoframesConfig {

    public Energy energy = new Energy();
    public Damage damage = new Damage();
    public Pieces pieces = new Pieces();
    public Tiers tiers = new Tiers();
    public Modules modules = new Modules();
    public Frame frame = new Frame();
    public World world = new World();
    public Server server = new Server();

    /** Configurazione con tutti i valori di specifica. */
    public static ExoframesConfig defaults() {
        return new ExoframesConfig();
    }

    /** Energia: capacita' della cella e consumi in RE, soglie e penalita'. */
    public static final class Energy {
        public int cellCapacity = 10_000;
        public int idlePerSecond = 2;
        public int walkPerSecond = 8;
        public int sprintPerSecond = 20;
        public int jumpCost = 15;
        public double lowThreshold = 0.10;
        public double emptySpeedMultiplier = 0.30;
        public double emptyProtectionMultiplier = 0.50;
        public int chargerRePerRedstone = 500;
        public int cellSwapSeconds = 2;
    }

    /** Danno: ripartizione tra i pezzi, riparazione ed effetti dei pezzi rotti. */
    public static final class Damage {
        public double primaryShare = 0.60;
        public double repairKitFraction = 0.25;
        public double brokenTorsoConsumptionMultiplier = 1.25;
        public double brokenLegSpeedPenalty = 0.15;
    }

    /** Pezzi: quota di protezione (somma 1) e slot modulo di base per posizione. */
    public static final class Pieces {
        public EnumMap<PieceSlot, Double> shares = new EnumMap<>(PieceSlot.class);
        public EnumMap<PieceSlot, Integer> baseModuleSlots = new EnumMap<>(PieceSlot.class);

        public Pieces() {
            for (PieceSlot slot : PieceSlot.values()) {
                shares.put(slot, slot.protectionShare());
                baseModuleSlots.put(slot, slot.baseModuleSlots());
            }
        }
    }

    /** Livelli: durabilita' massima dei pezzi e moltiplicatore di protezione. */
    public static final class Tiers {
        public EnumMap<Tier, Integer> maxDurability = new EnumMap<>(Tier.class);
        public EnumMap<Tier, Double> protectionMultiplier = new EnumMap<>(Tier.class);

        public Tiers() {
            maxDurability.put(Tier.SCRAP, 180);
            maxDurability.put(Tier.INDUSTRIAL, 360);
            maxDurability.put(Tier.ARMORED, 520);
            maxDurability.put(Tier.NETHERITE, 650);
            maxDurability.put(Tier.PROTOTYPE, 800);
            for (Tier tier : Tier.values()) {
                protectionMultiplier.put(tier, tier.protectionMultiplier());
            }
        }
    }

    /**
     * Moduli: costi energetici e parametri. {@code costPerSecond} contiene solo i moduli a consumo continuo,
     * {@code costPerUse} solo quelli a singolo uso.
     */
    public static final class Modules {
        public EnumMap<ModuleType, Integer> costPerSecond = new EnumMap<>(ModuleType.class);
        public EnumMap<ModuleType, Integer> costPerUse = new EnumMap<>(ModuleType.class);
        public int jetpackMaxSeconds = 4;
        public int shockwaveMinFallBlocks = 6;
        public double servosSpeedBonus = 0.20;
        public int bladeBonusDamage = 6;

        public Modules() {
            for (ModuleType type : ModuleType.values()) {
                if (type.cost().perSecond() > 0) {
                    costPerSecond.put(type, type.cost().perSecond());
                }
                if (type.cost().perUse() > 0) {
                    costPerUse.put(type, type.cost().perUse());
                }
            }
        }
    }

    /** Telaio indossato: penalita' di velocita', resistenza al contraccolpo, uscita. */
    public static final class Frame {
        public double baseSpeedPenalty = 0.15;
        public double knockbackResistance = 0.5;
        public int exitHoldSeconds = 1;
    }

    /** Generazione del mondo e bottino. */
    public static final class World {
        public boolean ruinedVillage = true;
        public boolean buriedLab = true;
        public boolean armoredIllager = true;
        public boolean chestLoot = true;
    }

    /** Regole lato server. */
    public static final class Server {
        public boolean ownerOnlyFrames = true;
    }
}
