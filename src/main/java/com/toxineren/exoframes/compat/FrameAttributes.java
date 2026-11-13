package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gli effetti del telaio che passano dagli attributi vanilla dell'entita': velocita' di movimento, resistenza al
 * contraccolpo, armatura e robustezza dell'armatura.
 * <p>
 * I modificatori sono transitori (non salvati con l'entita') e hanno id stabili: vanno riapplicati dallo stato del
 * telaio (al login, al cambio di dimensione, a ogni variazione), e ogni applicazione sostituisce la precedente invece
 * di sommarsi. Corsa, nuoto e immunita' al danno da caduta non sono attributi e non passano da qui.
 */
public interface FrameAttributes {
    /**
     * Imposta i quattro modificatori del telaio dagli effetti dati: velocita' come "moltiplica totale" di
     * {@code speedMultiplier - 1}; contraccolpo, armatura ({@link FrameEffects#armorPoints()}) e robustezza come
     * addendi. Un modificatore gia' presente con lo stesso id viene prima rimosso.
     */
    void apply(LivingEntity e, FrameEffects fx);

    /** Rimuove i quattro modificatori del telaio, se presenti. */
    void clear(LivingEntity e);
}
