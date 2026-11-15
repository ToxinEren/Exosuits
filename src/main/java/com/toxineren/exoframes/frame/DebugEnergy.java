package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Strumento di debug per gli operatori (design §12.14, {@code /exoframe debug energy}): legge e imposta la carica della
 * cella del telaio indossato. Serve al client vero del cloud e alla checklist per portare la cella a {@code LOW} e a
 * {@code EMPTY} senza aspettare che si scarichi. La carica si imposta sempre con
 * {@link FrameStateAccess#changeWorn}: un cambio strutturale, che sincronizza subito il proprietario, cosi' il client
 * vede subito gli indicatori nuovi (scatto, riga dell'energia).
 */
public final class DebugEnergy {
    /** Esito di {@link #set}. */
    public enum Outcome {
        /** Carica impostata. */
        DONE,
        /** Il giocatore non indossa un telaio: nulla cambia. */
        NOT_WEARING,
        /** Il telaio indossato non ha una cella: nulla cambia, nessuna sincronizzazione. */
        NO_CELL,
        /** Il registro e' in sola lettura e ha rifiutato il cambio (l'ERROR del registro e' gia' nel log). */
        REFUSED
    }

    private DebugEnergy() {
    }

    /**
     * Porta la carica della cella indossata a {@code stored} RE, limitata a [0, capacita' della cella]: la cella si
     * sostituisce con una nuova della stessa capacita', dentro {@link FrameStateAccess#changeWorn}.
     */
    public static Outcome set(ServerPlayer player, int stored) {
        Objects.requireNonNull(player, "player must not be null");
        FrameStateAccess frames = Compat.frameState();
        Optional<FrameState> state = frames.wornFrameId(player).isPresent() ? frames.get(player) : Optional.empty();
        if (state.isEmpty()) {
            return Outcome.NOT_WEARING;
        }
        if (state.get().cell() == null) {
            return Outcome.NO_CELL;
        }
        boolean changed = frames.changeWorn(player, s -> s.insertCell(new EnergyCell(s.cell().capacity(),
                Math.max(0, Math.min(stored, s.cell().capacity())))));
        return changed ? Outcome.DONE : Outcome.REFUSED;
    }

    /**
     * La cella del telaio indossato (lo stato vivo del registro: da leggere, non da cambiare); vuoto senza telaio
     * indossato o senza cella.
     */
    public static Optional<EnergyCell> cell(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        return Compat.frameState().get(player).map(FrameState::cell);
    }
}
