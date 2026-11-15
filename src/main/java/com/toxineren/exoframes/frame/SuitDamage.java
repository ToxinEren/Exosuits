package com.toxineren.exoframes.frame;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Usura dei pezzi di chi indossa la tuta (design, par. 12.7). Codice comune: lo chiama il mixin comune di
 * {@code Player} (una chiamata per aggancio, senza logica) nel punto in cui il gioco consuma l'armatura, con
 * l'importo dopo difficolta', scudo e finestra di invulnerabilita' e prima dell'assorbimento dell'armatura (design,
 * par. 12.2).
 * <p>
 * Oggi non fa nulla: il gioco consuma l'armatura come sempre e i pezzi non si consumano ancora.
 */
public final class SuitDamage {
    private SuitDamage() {
    }

    /**
     * Il gioco sta per consumare l'armatura di {@code player} per un colpo di {@code amount} da {@code source}.
     * Oggi non fa nulla.
     */
    public static void onArmorHurt(Player player, DamageSource source, float amount) {
    }
}
