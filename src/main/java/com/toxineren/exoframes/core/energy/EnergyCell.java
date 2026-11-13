package com.toxineren.exoframes.core.energy;

/** Cella di energia in RE: mutabile, con capacita' fissa e carica sempre in [0, capacity]. */
public final class EnergyCell {

    private final int capacity;
    private int stored;

    public EnergyCell(int capacity, int stored) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0: " + capacity);
        }
        if (stored < 0 || stored > capacity) {
            throw new IllegalArgumentException("stored must be in [0, " + capacity + "]: " + stored);
        }
        this.capacity = capacity;
        this.stored = stored;
    }

    public static EnergyCell full(int capacity) {
        return new EnergyCell(capacity, capacity);
    }

    public static EnergyCell empty(int capacity) {
        return new EnergyCell(capacity, 0);
    }

    public int capacity() {
        return capacity;
    }

    public int stored() {
        return stored;
    }

    /** Toglie al massimo {@code amount}; restituisce quanto e' stato effettivamente tolto. */
    public int drain(int amount) {
        requireNonNegative(amount);
        int removed = Math.min(amount, stored);
        stored -= removed;
        return removed;
    }

    /** Aggiunge al massimo {@code amount}; restituisce quanto e' stato effettivamente aggiunto. */
    public int charge(int amount) {
        requireNonNegative(amount);
        int added = Math.min(amount, capacity - stored);
        stored += added;
        return added;
    }

    public double fraction() {
        return (double) stored / capacity;
    }

    public boolean isEmpty() {
        return stored == 0;
    }

    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0: " + amount);
        }
    }
}
