package com.toxineren.exoframes.core.armor;

/** Livello di un pezzo dell'esoscheletro. */
public enum Tier {
    SCRAP(1, "scrap", 0, 1.000, TierTrait.CHEAP_FRAGILE),
    INDUSTRIAL(2, "industrial", 1, 1.333, TierTrait.MINING_POWER),
    ARMORED(3, "armored", 2, 1.733, TierTrait.PROJECTILE_RESISTANCE),
    NETHERITE(4, "netherite", 2, 1.600, TierTrait.FIRE_IMMUNE_LAVA_WALK),
    PROTOTYPE(5, "prototype", 3, 2.000, TierTrait.SELF_REPAIR);

    private final int level;
    private final String id;
    private final int extraModuleSlots;
    private final double protectionMultiplier;
    private final TierTrait trait;

    Tier(int level, String id, int extraModuleSlots, double protectionMultiplier, TierTrait trait) {
        this.level = level;
        this.id = id;
        this.extraModuleSlots = extraModuleSlots;
        this.protectionMultiplier = protectionMultiplier;
        this.trait = trait;
    }

    public int level() {
        return level;
    }

    public String id() {
        return id;
    }

    public int extraModuleSlots() {
        return extraModuleSlots;
    }

    public double protectionMultiplier() {
        return protectionMultiplier;
    }

    public TierTrait trait() {
        return trait;
    }

    /**
     * Cerca un livello per id.
     *
     * @throws IllegalArgumentException se l'id e' sconosciuto (o null)
     */
    public static Tier byId(String id) {
        for (Tier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("unknown tier id: " + id);
    }

    /** True se questo livello e' uguale o superiore a {@code other} (confronto inclusivo). */
    public boolean atLeast(Tier other) {
        return level >= other.level;
    }
}
