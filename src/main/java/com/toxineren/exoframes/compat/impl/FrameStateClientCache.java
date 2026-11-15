package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copia lato client degli stati del telaio ricevuti con {@link FrameStateSyncPacket}, per UUID del giocatore, ognuno
 * con gli indicatori arrivati nello stesso pacchetto (calcolati dal server: il client non li ricalcola mai).
 * <p>
 * Viene toccata dal gestore del pacchetto (gia' sul thread del client grazie a {@code Net}) e svuotata all'uscita
 * dal mondo; la mappa e' comunque concorrente, cosi' una lettura da un altro thread del client non la corrompe.
 * Non referenzia classi del client: e' sicura da caricare anche su un server dedicato.
 */
final class FrameStateClientCache {
    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    private FrameStateClientCache() {
    }

    /**
     * Deposita lo stato ricevuto con i suoi indicatori, o dimentica entrambi se il pacchetto dice che lo stato e'
     * assente. Lo stato e' gia' stato validato dal server: si legge con {@link FrameStateCodec#readTrusted}, cosi' una
     * config del client diversa non scarta moduli che il server ha accettato.
     */
    static void accept(FrameStateSyncPacket msg) {
        if (msg.state().isPresent()) {
            ENTRIES.put(msg.player(), new Entry(
                    FrameStateCodec.readTrusted(msg.state().get(), RedstoneExoframes.config()),
                    msg.indicators().orElseThrow()));
        } else {
            ENTRIES.remove(msg.player());
        }
    }

    /** L'ultimo stato ricevuto per il giocatore, o null. */
    static FrameState view(UUID player) {
        Entry entry = ENTRIES.get(player);
        return entry == null ? null : entry.state();
    }

    /** Gli indicatori ricevuti con l'ultimo stato del giocatore, o null. */
    static FrameIndicators indicators(UUID player) {
        Entry entry = ENTRIES.get(player);
        return entry == null ? null : entry.indicators();
    }

    /** Dimentica tutti gli stati (uscita dal mondo). */
    static void clear() {
        ENTRIES.clear();
    }

    /** Uno stato ricevuto e gli indicatori dello stesso pacchetto. */
    private record Entry(FrameState state, FrameIndicators indicators) {
    }
}
