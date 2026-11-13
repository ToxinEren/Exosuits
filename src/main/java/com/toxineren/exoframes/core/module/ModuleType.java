package com.toxineren.exoframes.core.module;

import com.toxineren.exoframes.core.armor.Tier;

/** Tipi di modulo installabili sui pezzi dell'esoscheletro. */
public enum ModuleType {
    JETPACK("jetpack", ModuleFamily.TORSO, Activation.HOLD, EnergyCost.perSecond(60), Tier.SCRAP),
    SHOCKWAVE("shockwave", ModuleFamily.LEGS, Activation.ON_LANDING, EnergyCost.perUse(200), Tier.SCRAP),
    NIGHT_VISION("night_vision", ModuleFamily.HELMET, Activation.TOGGLE, EnergyCost.perSecond(3), Tier.SCRAP),
    SCANNER("scanner", ModuleFamily.HELMET, Activation.TOGGLE, EnergyCost.perSecond(5), Tier.SCRAP),
    REBREATHER("rebreather", ModuleFamily.HELMET, Activation.PASSIVE, EnergyCost.perSecond(4), Tier.SCRAP),
    HYDRAULIC_BLADE("hydraulic_blade", ModuleFamily.ARM, Activation.ON_ATTACK, EnergyCost.perUse(10), Tier.SCRAP),
    DRILL("drill", ModuleFamily.ARM, Activation.HOLD, EnergyCost.perSecond(25), Tier.ARMORED),
    ENERGY_SHIELD("energy_shield", ModuleFamily.TORSO, Activation.TOGGLE, EnergyCost.perSecond(30), Tier.ARMORED),
    SCULK_DETECTOR("sculk_detector", ModuleFamily.HELMET, Activation.PASSIVE, EnergyCost.perSecond(1), Tier.SCRAP),
    SERVOS("servos", ModuleFamily.LEGS, Activation.PASSIVE, EnergyCost.perSecond(5), Tier.SCRAP);

    private final String id;
    private final ModuleFamily family;
    private final Activation activation;
    private final EnergyCost cost;
    private final Tier minTier;

    ModuleType(String id, ModuleFamily family, Activation activation, EnergyCost cost, Tier minTier) {
        this.id = id;
        this.family = family;
        this.activation = activation;
        this.cost = cost;
        this.minTier = minTier;
    }

    public String id() {
        return id;
    }

    public ModuleFamily family() {
        return family;
    }

    public Activation activation() {
        return activation;
    }

    public EnergyCost cost() {
        return cost;
    }

    public Tier minTier() {
        return minTier;
    }

    /** Condizione ambientale per abilitare il modulo: SUBMERGED solo per REBREATHER, ALWAYS per gli altri. */
    public ModuleCondition condition() {
        return this == REBREATHER ? ModuleCondition.SUBMERGED : ModuleCondition.ALWAYS;
    }

    /** True solo per i moduli che richiedono entrambe le gambe (SHOCKWAVE). */
    public boolean requiresBothLegs() {
        return this == SHOCKWAVE;
    }

    /**
     * Cerca un modulo per id.
     *
     * @throws IllegalArgumentException se l'id e' sconosciuto (o null)
     */
    public static ModuleType byId(String id) {
        for (ModuleType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown module id: " + id);
    }
}
