package com.toxineren.exoframes.compat.client;

import java.util.function.Function;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Agganci ai renderer vanilla che ogni loader espone in modo diverso. Solo client, da chiamare una volta dall'init
 * client ({@code RedstoneExoframesClient.init()}), prima che il gioco costruisca i renderer delle entita'.
 */
public interface ClientRenderHooks {
    /**
     * Aggiunge un layer al renderer del giocatore, per entrambi i modelli di skin (normale e sottile): la fabbrica
     * riceve il renderer di ciascuna skin e viene chiamata a ogni ricostruzione dei renderer (caricamento e
     * ricaricamento delle risorse), quindi un layer non deve tenere stato che sopravviva al suo renderer.
     */
    void addPlayerLayer(Function<RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
            RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory);
}
