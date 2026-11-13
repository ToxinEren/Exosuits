package com.toxineren.exoframes.core.armor;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.Objects;

/** Pezzo dell'esoscheletro: mutabile, con durabilita' sempre in [0, maxDurability]. */
public final class Piece {

    private final PieceSlot slot;
    private final Tier tier;
    private final int maxDurability;
    private int durability;

    public Piece(PieceSlot slot, Tier tier, int maxDurability, int durability) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.tier = Objects.requireNonNull(tier, "tier");
        if (maxDurability <= 0) {
            throw new IllegalArgumentException("maxDurability must be > 0: " + maxDurability);
        }
        if (durability < 0 || durability > maxDurability) {
            throw new IllegalArgumentException("durability must be in [0, " + maxDurability + "]: " + durability);
        }
        this.maxDurability = maxDurability;
        this.durability = durability;
    }

    /**
     * Pezzo nuovo a durabilita' piena, con la durabilita' massima del livello letta da {@code tiers.maxDurability}.
     *
     * @throws IllegalArgumentException se la config non ha una durabilita' &gt; 0 per il livello
     */
    public static Piece fresh(PieceSlot slot, Tier tier, ExoframesConfig.Tiers tiers) {
        Integer max = tiers.maxDurability.get(Objects.requireNonNull(tier, "tier"));
        if (max == null) {
            throw new IllegalArgumentException("maxDurability mancante per " + tier);
        }
        return new Piece(slot, tier, max, max);
    }

    public PieceSlot slot() {
        return slot;
    }

    public Tier tier() {
        return tier;
    }

    public int durability() {
        return durability;
    }

    public int maxDurability() {
        return maxDurability;
    }

    public PieceCondition condition() {
        return PieceCondition.of(durability, maxDurability);
    }

    public boolean isBroken() {
        return durability == 0;
    }

    /** Toglie al massimo {@code amount}; restituisce quanto e' stato effettivamente tolto. */
    public int damage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0: " + amount);
        }
        int removed = Math.min(amount, durability);
        durability -= removed;
        return removed;
    }

    /**
     * Ripara la frazione {@code fraction} della durabilita' massima (0.25 = kit di riparazione: +25%),
     * arrotondata all'intero piu' vicino e limitata al massimo.
     */
    public void repairFraction(double fraction) {
        if (!(fraction >= 0.0 && fraction <= 1.0)) {
            throw new IllegalArgumentException("fraction must be in [0, 1]: " + fraction);
        }
        long restored = Math.round(maxDurability * fraction);
        durability = (int) Math.min(maxDurability, durability + restored);
    }

    public void repairFull() {
        durability = maxDurability;
    }
}
