package com.toxineren.exoframes.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.client.FirstPersonArm;
import com.toxineren.exoframes.client.PilotPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pilota sollevato e parti coperte nascoste nel disegno del giocatore (design, par. 4.7), e il braccio della tuta al
 * posto di quello del giocatore in prima persona (design, par. 13). Senza stato e senza logica: ogni aggancio e' una
 * chiamata a {@link PilotPose} o a {@link FirstPersonArm}.
 * <p>
 * Il sollevamento si aggancia in coda a {@code scale(AbstractClientPlayer, PoseStack, float)} (non al ponte che prende
 * un {@code LivingEntity}): {@code LivingEntityRenderer.render} lo chiama dentro il proprio push/pop, dopo le rotazioni
 * della posa, quindi la traslazione e' nello spazio del modello e sparisce con quel pop, senza nulla da annullare.
 * <p>
 * Bersagli verificati con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge: {@code scale}
 * ha un solo {@code return} e una sola chiamata, dal ponte, tra il push e il pop di
 * {@code LivingEntityRenderer.render}; in {@code render} {@code setModelProperties} si chiama una volta, all'inizio.
 * <p>
 * Il terzo aggancio sta dentro {@code renderHand}, il metodo privato da cui passa ogni braccio in prima persona,
 * subito dopo la chiamata a {@code PlayerModel.setupAnim}, ed e' cancellabile: se {@link FirstPersonArm} ha disegnato
 * il braccio della tuta il metodo di base si interrompe, altrimenti disegna il suo braccio come sempre. Dopo
 * {@code setupAnim} e non in testa al metodo perche' {@code setModelProperties} e' privato: dalla testa una classe
 * client non potrebbe preparare il modello come lo prepara il gioco, mentre in quel punto il gioco lo ha gia' fatto
 * da se'. Bersaglio verificato con javap sugli stessi jar (1.20.1 e 1.21.1, vanilla e con le patch di Forge e
 * NeoForge): in {@code renderHand} c'e' una sola chiamata a {@code PlayerModel.setupAnim}, dopo
 * {@code setModelProperties} e prima dei due disegni (il braccio e la manica); le patch dei loader non toccano
 * {@code renderHand}, stanno in {@code renderRightHand} e {@code renderLeftHand}, che lo chiamano.
 */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("TAIL"))
    private void exoframes$raisePilot(AbstractClientPlayer player, PoseStack pose, float partialTick,
            CallbackInfo ci) {
        PilotPose.onScale(player, pose);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;"
                            + "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V"))
    private void exoframes$hideCoveredParts(AbstractClientPlayer player, float entityYaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        PilotPose.onModelPrepared(player, ((PlayerRenderer) (Object) this).getModel());
    }

    @Inject(method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/model/PlayerModel;"
                            + "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"),
            cancellable = true)
    private void exoframes$suitArm(PoseStack pose, MultiBufferSource buffers, int packedLight,
            AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (FirstPersonArm.onHandPrepared(player, ((PlayerRenderer) (Object) this).getModel(), arm, pose, buffers,
                packedLight)) {
            ci.cancel();
        }
    }
}
