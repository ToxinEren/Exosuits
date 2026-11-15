package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Gli effetti del telaio che passano dagli attributi vanilla dell'entita': velocita' di movimento, resistenza al
 * contraccolpo, armatura e robustezza dell'armatura, e su 1.21.1 il gradino della tuta (passo di un blocco).
 * <p>
 * I modificatori sono transitori (non salvati con l'entita') e hanno id stabili: un oggetto giocatore nuovo (ingresso
 * nel server, rinascita, ritorno dall'End) ha i soli valori base degli attributi. Li applica il tick dell'energia
 * ({@code frame/SuitTick}) alla prima valutazione di ogni oggetto giocatore nuovo e quando gli effetti cambiano, mai
 * l'aggancio di {@code hurtArmor}; li toglie {@code frame/WornState} quando lo stato indossato finisce. Un semplice
 * cambio di dimensione conserva l'entita' e i modificatori e non richiede nulla. Ogni applicazione sostituisce la
 * precedente invece di sommarsi, e tocca un modificatore solo se importo o operazione cambiano. Corsa, nuoto e
 * immunita' al danno da caduta non sono attributi e non passano da qui.
 * <p>
 * Il gradino: su 1.21.1 e' il quinto modificatore di {@link #apply} ({@code frame_step}, +0,4 su
 * {@code generic.step_height}), che il client riceve con gli attributi; su 1.20.1 il passo e' un campo dell'entita',
 * il {@code ServerPlayer} ha gia' 1,0 e lo alza per il solo giocatore locale {@link #localStep}.
 */
public interface FrameAttributes {
    /**
     * Imposta i modificatori del telaio dagli effetti dati: velocita' come "moltiplica totale" di
     * {@code speedMultiplier - 1}; contraccolpo, armatura ({@link FrameEffects#armorPoints()}) e robustezza come
     * addendi; su 1.21.1 anche il gradino, {@code redstone_exoframes:frame_step}, +0,4 su {@code generic.step_height}.
     * <p>
     * Idempotente: un modificatore gia' presente con lo stesso importo e la stessa operazione non si tocca, cosi' una
     * applicazione senza cambi non fa reinviare nulla al client; uno diverso si toglie e si rimette. Lo chiama il tick
     * dell'energia alla prima valutazione di ogni oggetto giocatore nuovo e quando gli effetti cambiano.
     */
    void apply(LivingEntity e, FrameEffects fx);

    /** Rimuove i modificatori del telaio (su 1.21.1 anche il gradino), se presenti. */
    void clear(LivingEntity e);

    /**
     * Il gradino della tuta per il giocatore locale, chiamato a ogni tick da {@code SuitSize.tick} solo per lui
     * ({@code player.isLocalPlayer()}); mai su un {@code ServerPlayer}, per il quale non fa nulla. Su 1.20.1 alza il
     * campo del passo a 1,0 mentre indossa la tuta e lo riporta a 0,6 solo se l'ha alzato la mod e vale ancora 1,0,
     * cosi' non cancella il passo messo da un'altra mod; un giocatore locale nuovo (rinascita, cambio di dimensione)
     * non e' mai stato alzato. Su 1.21.1 non fa nulla: il passo e' il quinto modificatore di {@link #apply}.
     */
    void localStep(Player player, boolean wearing);
}
