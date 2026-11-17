package com.toxineren.exoframes.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.core.armor.PieceSlot;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Disegna la tuta (telaio e pezzi): l'unica cucitura che conosce GeckoLib. La usano sia la tuta indossata da un
 * giocatore sia il telaio piazzato, che non e' un {@code LivingEntity}: lo stesso oggetto sullo schermo; e, con
 * {@link #renderArm}, il braccio della tuta in prima persona. Solo client, da chiamare sul thread di rendering dentro
 * il disegno dell'entita' (per esempio da un {@code RenderLayer} o da un {@code EntityRenderer}) o, per
 * {@link #renderArm}, dentro il disegno della mano in prima persona; mai prima che i modelli delle entita' siano
 * caricati.
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
     * @param entity l'entita' che porta la tuta (il giocatore o il telaio piazzato), non un animabile di GeckoLib;
     *     GeckoLib la usa solo come {@code Entity} (identita', bagliore), senza conversioni
     * @param base il modello umanoide gia' in posa per questo fotogramma; non si disegna
     * @param view pezzi, stati e colori da disegnare; vuota per il telaio nudo
     */
    void render(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight,
            float partialTick, SuitView view);

    /**
     * Disegna un braccio solo della tuta (la prima persona, design §13). Come {@link #render}: stessa posa (l'osso
     * del braccio segue perno e rotazione del braccio di {@code base}), stesso spazio, stessa entita' non animabile
     * di GeckoLib; ma si vedono le sole ossa che {@link SuitBoneRules#visibleForArm} mostra per {@code arm}, con i
     * colori di {@code view}. Dopo la chiamata non resta nulla di impostato: il {@link #render} successivo disegna la
     * tuta intera.
     *
     * @param arm {@link PieceSlot#RIGHT_ARM} o {@link PieceSlot#LEFT_ARM}
     * @throws IllegalArgumentException se {@code arm} non e' uno dei due bracci, prima di toccare qualunque cosa
     */
    void renderArm(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight,
            float partialTick, SuitView view, PieceSlot arm);
}
