package com.toxineren.exoframes.mixin;

import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.frame.SuitMovement;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Usura e acqua di chi indossa la tuta (design, par. 12.2): nessun evento comune alle quattro varianti copre il punto
 * in cui il gioco consuma l'armatura (l'importo dopo difficolta', scudo e finestra di invulnerabilita'), ne' toglie a
 * un giocatore galleggiamento, corrente e nuoto. Elencato nella sezione {@code mixins} (comune ai due lati) delle due
 * configurazioni, senza direttive: le firme sono le stesse sulle quattro varianti. Senza stato e senza logica: ogni
 * aggancio e' una chiamata a {@link SuitDamage} o a {@link SuitMovement}.
 * <p>
 * Bersagli verificati con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge:
 * {@code Player} ridefinisce {@code hurtArmor}, {@code isAffectedByFluids}, {@code isPushedByFluid} e
 * {@code updateSwimming} con la stessa firma ovunque; nei jar diventano {@code method_6105}, {@code method_29920},
 * {@code method_5675}, {@code method_5790} su Fabric, {@code m_6472_}, {@code m_6129_}, {@code m_6063_},
 * {@code m_5844_} su Forge, e restano i nomi Mojang su NeoForge.
 */
@Mixin(Player.class)
abstract class PlayerSuitMixin {
    @Inject(method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))
    private void exoframes$wearPieces(DamageSource source, float amount, CallbackInfo ci) {
        SuitDamage.onArmorHurt((Player) (Object) this, source, amount);
    }

    @Inject(method = "isAffectedByFluids()Z", at = @At("RETURN"), cancellable = true)
    private void exoframes$fluidMovement(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SuitMovement.affectedByFluids((Player) (Object) this, cir.getReturnValueZ()));
    }

    @Inject(method = "isPushedByFluid()Z", at = @At("RETURN"), cancellable = true)
    private void exoframes$fluidPush(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SuitMovement.pushedByFluid((Player) (Object) this, cir.getReturnValueZ()));
    }

    @Inject(method = "updateSwimming()V", at = @At("TAIL"))
    private void exoframes$noSwimming(CallbackInfo ci) {
        SuitMovement.afterUpdateSwimming((Player) (Object) this);
    }
}
