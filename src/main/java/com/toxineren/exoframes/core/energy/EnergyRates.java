package com.toxineren.exoframes.core.energy;

/** Tariffe di consumo in RE: al secondo per attivita', piu' il costo fisso del salto. */
public record EnergyRates(int idlePerSecond, int walkPerSecond, int sprintPerSecond, int jumpCost) {

    public static final int TICKS_PER_SECOND = 20;

    /** Valori di specifica: 2 / 8 / 20 RE al secondo, salto 15 RE. */
    public static EnergyRates spec() {
        return new EnergyRates(2, 8, 20, 15);
    }

    public int perSecond(Activity a) {
        return switch (a) {
            case IDLE -> idlePerSecond;
            case WALK -> walkPerSecond;
            case SPRINT -> sprintPerSecond;
        };
    }
}
