package com.toxineren.exoframes.core.module;

/** Costo energetico di un modulo: continuo (al secondo) e/o a singolo uso. */
public record EnergyCost(int perSecond, int perUse) {

    public static EnergyCost perSecond(int amount) {
        return new EnergyCost(amount, 0);
    }

    public static EnergyCost perUse(int amount) {
        return new EnergyCost(0, amount);
    }
}
