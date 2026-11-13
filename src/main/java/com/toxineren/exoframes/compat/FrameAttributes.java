package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gli effetti del telaio che passano dagli attributi vanilla dell'entita': velocita' di movimento, resistenza al
 * contraccolpo, armatura e robustezza dell'armatura.
 * <p>
 * I modificatori sono transitori (non salvati con l'entita') e hanno id stabili: vanno riapplicati dallo stato del
 * telaio al login, al respawn (anche l'uscita dall'End, che crea una nuova entita' con i soli valori base degli
 * attributi) e a ogni variazione del telaio; un semplice cambio di dimensione conserva l'entita' e i modificatori e
 * non richiede nulla. Ogni applicazione sostituisce la precedente invece di sommarsi. Il listener di respawn gia'
 * registrato in {@code FrameStateAccessImpl.registerCommon} e' il punto naturale in cui la Milestone 3 chiamera'
 * {@link #apply}. Corsa, nuoto e immunita' al danno da caduta non sono attributi e non passano da qui.
 */
public interface FrameAttributes {
    /**
     * Imposta i quattro modificatori del telaio dagli effetti dati: velocita' come "moltiplica totale" di
     * {@code speedMultiplier - 1}; contraccolpo, armatura ({@link FrameEffects#armorPoints()}) e robustezza come
     * addendi. Un modificatore gia' presente con lo stesso id viene prima rimosso.
     * <p>
     * Va chiamato ai cambi di stato (login, respawn compresa l'uscita dall'End, cambio del telaio), non a ogni tick:
     * ogni chiamata fa reinviare gli attributi al client. Un semplice cambio di dimensione conserva i modificatori.
     */
    void apply(LivingEntity e, FrameEffects fx);

    /** Rimuove i quattro modificatori del telaio, se presenti. */
    void clear(LivingEntity e);
}
