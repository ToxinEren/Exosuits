package com.toxineren.exoframes.core.energy;

/** Penalita' applicate al portatore in funzione dello stato energetico. */
public record EnergyPenalties(
        boolean canSprint, boolean modulesEnabled, double speedMultiplier, double protectionMultiplier) {

    public static EnergyPenalties forState(EnergyState state, EnergyPenaltyValues cfg) {
        return switch (state) {
            case NORMAL -> new EnergyPenalties(true, true, 1.0, 1.0);
            case LOW -> new EnergyPenalties(false, false, 1.0, 1.0);
            case EMPTY -> new EnergyPenalties(false, false, cfg.emptySpeed(), cfg.emptyProtection());
        };
    }
}
