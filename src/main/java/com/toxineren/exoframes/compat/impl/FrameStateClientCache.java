package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copia lato client degli stati del telaio ricevuti con {@link FrameStateSyncPacket}, per UUID del giocatore.
 * <p>
 * Viene toccata dal gestore del pacchetto (gia' sul thread del client grazie a {@code Net}) e svuotata all'uscita
 * dal mondo; la mappa e' comunque concorrente, cosi' una lettura da un altro thread del client non la corrompe.
 * Non referenzia classi del client: e' sicura da caricare anche su un server dedicato.
 */
final class FrameStateClientCache {
    private static final Map<UUID, FrameState> STATES = new ConcurrentHashMap<>();

    private FrameStateClientCache() {
    }

    /**
     * Deposita lo stato ricevuto, o lo dimentica se il pacchetto dice che e' assente. Lo stato e' gia' stato validato
     * dal server: si legge con {@link FrameStateCodec#readTrusted}, cosi' una config del client diversa non scarta
     * moduli che il server ha accettato.
     */
    static void accept(FrameStateSyncPacket msg) {
        if (msg.state().isPresent()) {
            STATES.put(msg.player(), FrameStateCodec.readTrusted(msg.state().get(), RedstoneExoframes.config()));
        } else {
            STATES.remove(msg.player());
        }
    }

    /** L'ultimo stato ricevuto per il giocatore, o null. */
    static FrameState view(UUID player) {
        return STATES.get(player);
    }

    /** Dimentica tutti gli stati (uscita dal mondo). */
    static void clear() {
        STATES.clear();
    }
}
