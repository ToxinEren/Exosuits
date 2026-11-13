package com.toxineren.exoframes.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.client.PilotPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pilota sollevato e parti coperte nascoste nel disegno del giocatore (design, par. 4.7). Senza stato e senza logica:
 * ogni aggancio e' una chiamata a {@link PilotPose}.
 * <p>
 * Il sollevamento si aggancia in coda a {@code scale(AbstractClientPlayer, PoseStack, float)} (non al ponte che prende
 * un {@code LivingEntity}): {@code LivingEntityRenderer.render} lo chiama dentro il proprio push/pop, dopo le rotazioni
 * della posa, quindi la traslazione e' nello spazio del modello e sparisce con quel pop, senza nulla da annullare.
 * <p>
 * Bersagli verificati con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge: {@code scale}
 * ha un solo {@code return} e una sola chiamata, dal ponte, tra il push e il pop di
 * {@code LivingEntityRenderer.render}; in {@code render} {@code setModelProperties} si chiama una volta, all'inizio.
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
}
