package com.toxineren.exoframes.compat;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aggancio dei test in gioco (GameTest vanilla) al loader: l'unico punto con le differenze di registrazione tra
 * Fabric, Forge e NeoForge e di creazione del giocatore finto tra 1.20.1 e 1.21.1.
 */
public interface GameTests {
    /**
     * Registra la classe di test presso il framework GameTest del loader, solo se e' attivo il server di test
     * (altrimenti non fa nulla). Su Fabric non fa nulla: la classe e' dichiarata come entrypoint
     * {@code fabric-gametest} in fabric.mod.json.
     */
    void register(Class<?> testClass);

    /**
     * Crea un giocatore finto connesso nel livello del test, con una connessione dotata di canale (su Forge 1.20.1
     * il giocatore finto vanilla non ha canale e fa fallire l'ingresso nel mondo).
     */
    ServerPlayer mockServerPlayer(GameTestHelper helper);
}
