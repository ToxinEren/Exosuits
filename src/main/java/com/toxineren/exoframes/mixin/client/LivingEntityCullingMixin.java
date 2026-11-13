package com.toxineren.exoframes.mixin.client;

import com.toxineren.exoframes.frame.SuitSize;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Riquadro di culling di chi indossa la tuta (design, par. 4.8): con la grandezza reale cresce in altezza da solo, ma
 * le braccia della tuta con i pezzi restano fuori ai lati, e un giocatore al bordo dello schermo sparirebbe con le
 * braccia ancora in vista. Senza stato e senza logica: l'aggancio e' una chiamata a {@link SuitSize#cullingBox}.
 * <p>
 * Bersaglio verificato con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge:
 * {@code LivingEntity} ridefinisce {@code getBoundingBoxForCulling()}, {@code Player} no, e
 * {@code EntityRenderer.shouldRender} lo chiama sull'entita' da disegnare.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityCullingMixin {
    @Inject(method = "getBoundingBoxForCulling()Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"),
            cancellable = true)
    private void exoframes$widenSuitCulling(CallbackInfoReturnable<AABB> cir) {
        cir.setReturnValue(SuitSize.cullingBox((LivingEntity) (Object) this, cir.getReturnValue()));
    }
}
