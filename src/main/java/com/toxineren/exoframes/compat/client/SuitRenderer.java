package com.toxineren.exoframes.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Disegna la tuta (telaio e pezzi) su un'entita' umanoide: l'unica cucitura che conosce GeckoLib. Solo client, da
 * chiamare sul thread di rendering dentro il disegno dell'entita' (per esempio da un {@code RenderLayer}), mai prima
 * che i modelli delle entita' siano caricati.
 * <p>
 * Contratto: l'entita' non e' un animabile di GeckoLib ({@code GeoAnimatable}). Solo per queste GeckoLib chiama
 * l'aggancio da cui l'implementazione applica la visibilita' delle ossa ({@code applyBoneVisibilityBySlot}); per un
 * animabile di GeckoLib pezzi e stati non si nasconderebbero.
 */
public interface SuitRenderer {
    /**
     * Disegna la tuta in una sola passata nella posa data: le ossa di primo livello seguono le parti di {@code base}
     * (rotazioni e posizioni, accovacciamento, cavalcatura), ma la visibilita' delle ossa dipende solo da
     * {@code view}, non dai flag {@code visible} delle parti di {@code base}. Eventuali scale o spostamenti della
     * tuta rispetto al giocatore li applica il chiamante su {@code pose}.
     *
     * @param entity l'entita' che indossa la tuta, non un animabile di GeckoLib
     * @param base il modello umanoide gia' in posa per questo fotogramma
     * @param view pezzi, stati e colori da disegnare; vuota per il telaio nudo
     */
    void render(LivingEntity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight,
            float partialTick, SuitView view);
}
