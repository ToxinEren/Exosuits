package com.toxineren.exoframes.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.client.PilotPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oggetto impugnato nella mano della tuta (design, par. 4.7): dal perno attuale del braccio del pilota (letto dal
 * modello del layer) a quello della tuta prima di {@code translateToHand}, ingrandito di S subito dopo. Senza stato e
 * senza logica: ogni aggancio e' una chiamata a {@link PilotPose}, che decide se l'entita' e' un giocatore con la
 * tuta. Il pop della posa resta quello
 * del metodo vanilla, tra i due agganci e la fine non ci sono uscite.
 * <p>
 * Bersaglio verificato con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge: una sola
 * chiamata all'interfaccia {@code ArmedModel.translateToHand}, tra {@code pushPose} e {@code popPose}.
 */
@Mixin(ItemInHandLayer.class)
abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand("
                    + "Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void exoframes$toSuitArmPivot(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        PilotPose.onBeforeHand(entity, ((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), pose, arm);
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand("
                    + "Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    shift = At.Shift.AFTER))
    private void exoframes$scaleToSuitHand(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        PilotPose.onAfterHand(entity, ((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), pose);
    }
}
