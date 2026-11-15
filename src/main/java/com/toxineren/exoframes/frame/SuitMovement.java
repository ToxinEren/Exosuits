package com.toxineren.exoframes.frame;

import net.minecraft.world.entity.player.Player;

/**
 * Acqua e scatto di chi indossa la tuta (design, par. 12.6). Codice comune, sui due lati: lo chiamano il mixin comune
 * di {@code Player} (galleggiamento, corrente, nuoto) e il mixin client del giocatore locale (scatto), una chiamata
 * per aggancio, senza logica nei mixin (design, par. 12.2). Nessun metodo porta il nome del suo bersaglio.
 * <p>
 * Oggi ogni metodo restituisce il valore del gioco e {@link #afterUpdateSwimming} non fa nulla: la tuta in acqua e
 * nello scatto si comporta ancora come il giocatore senza tuta.
 */
public final class SuitMovement {
    private SuitMovement() {
    }

    /**
     * Se i fluidi contano per il movimento di {@code player}; {@code vanilla} e' la risposta del gioco. Oggi
     * restituisce {@code vanilla}.
     */
    public static boolean affectedByFluids(Player player, boolean vanilla) {
        return vanilla;
    }

    /**
     * Se la corrente dei fluidi sposta {@code player}; {@code vanilla} e' la risposta del gioco. Oggi restituisce
     * {@code vanilla}.
     */
    public static boolean pushedByFluid(Player player, boolean vanilla) {
        return vanilla;
    }

    /** Dopo che il gioco ha deciso se {@code player} nuota. Oggi non fa nulla. */
    public static void afterUpdateSwimming(Player player) {
    }

    /**
     * Se il giocatore locale {@code player} ha di che scattare (lato client); {@code vanilla} e' la risposta del gioco,
     * che decide sia l'avvio sia la fine dello scatto. Oggi restituisce {@code vanilla}.
     */
    public static boolean sprintAllowed(Player player, boolean vanilla) {
        return vanilla;
    }
}
