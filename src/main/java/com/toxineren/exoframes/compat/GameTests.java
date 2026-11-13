package com.toxineren.exoframes.compat;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aggancio dei test in gioco (GameTest vanilla) al loader: l'unico punto con le differenze di registrazione tra
 * Fabric, Forge e NeoForge e di creazione del giocatore finto tra 1.20.1 e 1.21.1.
 */
public interface GameTests {
    /**
     * Registra la classe di test presso il framework GameTest, solo se e' attivo il server di test; altrimenti non fa
     * nulla, cosi' in produzione i test non esistono (neanche per il comando {@code /test}). Su Forge e NeoForge
     * aggiunge un ascoltatore di {@code RegisterGameTestsEvent} se e' impostata {@code forge/neoforge.gameTestServer};
     * su Fabric chiama {@code GameTestRegistry.register} se e' impostata {@code fabric-api.gametest} (nessun
     * entrypoint {@code fabric-gametest}: il modulo di Fabric lo registrerebbe a ogni avvio).
     */
    void register(Class<?> testClass);

    /**
     * Crea un giocatore finto connesso nel livello del test, con una connessione dotata di canale (su Forge 1.20.1
     * il giocatore finto vanilla non ha canale e fa fallire l'ingresso nel mondo). Il giocatore resta nella lista
     * dei giocatori del server per il resto dell'esecuzione: i test non devono contare i giocatori ne' scorrere la
     * lista aspettandosi di trovarci solo i propri.
     */
    ServerPlayer mockServerPlayer(GameTestHelper helper);
}
