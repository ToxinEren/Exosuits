package com.toxineren.exoframes.mixin;

import com.toxineren.exoframes.frame.SuitSize;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grandezza reale su 1.20.1 (design, par. 4.8): ingombro e altezza degli occhi di chi indossa la tuta, sui due lati.
 * Elencato solo nella sezione {@code mixins} di {@code resources-mc1.20.1/redstone_exoframes.mixins.json}, e
 * compilato solo nelle varianti 1.20.1 ({@code build.gradle.kts} lo esclude dalle altre, dove i bersagli non
 * esistono). Senza stato e senza logica: ogni aggancio e' una chiamata a {@link SuitSize}.
 * <p>
 * Bersagli verificati con javap sui jar di 1.20.1, vanilla e con le patch di Forge: {@code getDimensions} legge una
 * tabella fissa per posa e {@code getStandingEyeHeight} restituisce costanti; il gioco li copia nei campi solo in
 * {@code refreshDimensions} e nel costruttore.
 */
@Mixin(Player.class)
abstract class PlayerSize120Mixin {
    @Inject(method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("RETURN"), cancellable = true)
    private void exoframes$suitDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        cir.setReturnValue(SuitSize.dimensions((Player) (Object) this, pose, cir.getReturnValue()));
    }

    @Inject(method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;"
            + "Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("RETURN"), cancellable = true)
    private void exoframes$suitEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(SuitSize.eyeHeight((Player) (Object) this, pose, cir.getReturnValueF()));
    }
}
