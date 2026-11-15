package com.toxineren.exoframes.compat.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contatore, per UUID del giocatore, delle chiamate a {@code FrameStateAccess.syncTo} (design §12.4): lo legge solo
 * {@code GameTests.syncCount}. Si incrementa anche quando il loader non spedisce il pacchetto (su NeoForge 1.21.1 il
 * giocatore finto non ha il canale) e si dimentica all'uscita del giocatore dal server.
 */
final class SyncCounter {
    private static final Map<UUID, Integer> COUNTS = new ConcurrentHashMap<>();

    private SyncCounter() {
    }

    /** Conta una sincronizzazione del giocatore. */
    static void increment(UUID player) {
        COUNTS.merge(player, 1, Integer::sum);
    }

    /** Le sincronizzazioni contate per il giocatore; 0 se mai. */
    static int count(UUID player) {
        return COUNTS.getOrDefault(player, 0);
    }

    /** Dimentica il conteggio del giocatore (uscita dal server). */
    static void forget(UUID player) {
        COUNTS.remove(player);
    }
}
