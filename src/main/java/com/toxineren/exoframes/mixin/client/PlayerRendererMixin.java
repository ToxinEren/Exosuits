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
 * ogni aggancio e' una chiamata a {@link PilotPose}. Niente push/pop: se un altro mod annulla il metodo dopo il nostro
 * aggancio in testa, un push spaiato farebbe chiudere il gioco, mentre una traslazione rimasta la elimina il pop di
 * chi ha chiamato il renderer.
 * <p>
 * Bersaglio verificato con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge:
 * {@code setModelProperties} si chiama una volta, all'inizio; Forge e NeoForge aggiungono un'uscita anticipata (evento
 * {@code RenderPlayerEvent.Pre} annullato), che {@code RETURN} copre.
 */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void exoframes$raisePilot(AbstractClientPlayer player, float entityYaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        PilotPose.onRenderStart(player, pose);
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

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void exoframes$lowerPilot(AbstractClientPlayer player, float entityYaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        PilotPose.onRenderEnd(player, pose);
    }
}
