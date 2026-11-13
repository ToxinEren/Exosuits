package com.toxineren.exoframes.core.frame;

import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.Set;

/**
 * Effetti del telaio sul portatore, calcolati da {@link FrameRules#evaluate(FrameState, FrameContext)}.
 * {@code enabledModules} contiene solo i moduli attivi per tick (passivi e toggle accesi); i moduli a evento
 * ({@code HOLD}, {@code ON_LANDING}, {@code ON_ATTACK}) non compaiono mai.
 */
public record FrameEffects(
        double speedMultiplier,
        boolean canSprint,
        boolean fallDamageImmune,
        double knockbackResistance,
        boolean canSwim,
        double protectionPoints,
        double toughness,
        Set<ModuleType> enabledModules,
        int moduleMilliRePerTick,
        double consumptionMultiplier,
        EnergyState energyState) {

    public FrameEffects {
        enabledModules = Set.copyOf(enabledModules);
    }

    /**
     * Punti armatura interi: {@code Math.round(protectionPoints)}. E' il valore da scrivere nell'attributo
     * armatura vanilla, che {@code LivingEntity.getArmorValue()} tronca per difetto (floor): scrivere il double
     * non arrotondato farebbe valere 19 un set Industriale completo (19,995) invece di 20.
     * {@code protectionPoints} resta il valore esatto, non arrotondato.
     */
    public int armorPoints() {
        return (int) Math.round(protectionPoints);
    }
}
