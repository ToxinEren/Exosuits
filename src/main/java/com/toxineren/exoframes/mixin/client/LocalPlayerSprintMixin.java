package com.toxineren.exoframes.mixin.client;

import com.toxineren.exoframes.frame.SuitMovement;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scatto di chi indossa la tuta (design, par. 12.2 e 12.6): lo decide il client, e il solo lato server non lo ferma.
 * Elencato nella sezione {@code client} delle due configurazioni, senza direttive: la firma e' la stessa sulle quattro
 * varianti. Senza stato e senza logica: l'aggancio e' una chiamata a {@link SuitMovement#sprintAllowed}.
 * <p>
 * Bersaglio verificato con javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge: il metodo
 * privato {@code LocalPlayer.hasEnoughFoodToStartSprinting()} decide sia l'avvio sia la fine dello scatto in
 * {@code aiStep}, anche con il doppio W e lo scatto a interruttore; nei jar diventa {@code method_46743} su Fabric,
 * {@code m_255269_} su Forge, e resta il nome Mojang su NeoForge.
 */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerSprintMixin {
    @Inject(method = "hasEnoughFoodToStartSprinting()Z", at = @At("RETURN"), cancellable = true)
    private void exoframes$sprintGate(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SuitMovement.sprintAllowed((LocalPlayer) (Object) this, cir.getReturnValueZ()));
    }
}
