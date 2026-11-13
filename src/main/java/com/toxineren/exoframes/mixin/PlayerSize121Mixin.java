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
 * Grandezza reale su 1.21.1 (design, par. 4.8): ingombro di chi indossa la tuta, sui due lati, con occhi, targhetta
 * del nome e punto di seduta, che su 1.21.1 stanno nelle dimensioni. Elencato solo nella sezione {@code mixins} di
 * {@code resources-mc1.21.1/redstone_exoframes.mixins.json}, e compilato solo nelle varianti 1.21.1
 * ({@code build.gradle.kts} lo esclude dalle altre, dove il bersaglio non esiste). Senza stato e senza logica:
 * l'aggancio e' una chiamata a {@link SuitSize}.
 * <p>
 * Bersaglio verificato con javap sui jar di 1.21.1, vanilla e con le patch di NeoForge: {@code getDefaultDimensions}
 * legge una tabella fissa per posa e ignora l'attributo {@code scale}, che {@code getDimensions} applica dopo.
 */
@Mixin(Player.class)
abstract class PlayerSize121Mixin {
    @Inject(method = "getDefaultDimensions(Lnet/minecraft/world/entity/Pose;)"
            + "Lnet/minecraft/world/entity/EntityDimensions;", at = @At("RETURN"), cancellable = true)
    private void exoframes$suitDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        cir.setReturnValue(SuitSize.dimensions((Player) (Object) this, pose, cir.getReturnValue()));
    }
}
