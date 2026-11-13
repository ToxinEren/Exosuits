package com.toxineren.exoframes.core.energy;

/** Consuma energia a ogni tick accumulando le frazioni in milliRE (1 RE = 1000 milliRE) senza perdita. */
public final class EnergyMeter {

    private static final int MILLI_PER_RE = 1000;

    private final EnergyRates rates;
    private int carry;

    public EnergyMeter(EnergyRates rates) {
        this.rates = rates;
    }

    /** Applica un tick di consumo; restituisce gli RE effettivamente tolti dalla cella. */
    public int tick(EnergyCell cell, Activity activity, int extraMilliRePerTick, double multiplier) {
        int milli = (int) Math.round(
                (rates.perSecond(activity) * (double) MILLI_PER_RE / EnergyRates.TICKS_PER_SECOND
                        + extraMilliRePerTick) * multiplier);
        carry += milli;
        int whole = carry / MILLI_PER_RE;
        carry %= MILLI_PER_RE;
        return whole > 0 ? cell.drain(whole) : 0;
    }

    /**
     * Tariffe usate da questo contatore. Il costo del salto di un telaio indossato si toglie con
     * {@code FrameRules.jump}, che applica anche il moltiplicatore del torso rotto.
     */
    public EnergyRates rates() {
        return rates;
    }

    public int carryMilliRe() {
        return carry;
    }
}
