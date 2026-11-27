package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Chi puo' mettere le mani su un telaio (design §5.4): montare, smontare, raccoglierlo ed entrarci ({@link #canUse});
 * chi ne cambia il blocco ({@link #canToggleLock(UUID, boolean, FrameState)}).
 */
public final class FramePermissions {
    /**
     * Il livello di permesso degli operatori: chi lo ha blocca e sblocca ogni telaio, e in creativa tocca anche i telai
     * bloccati degli altri.
     */
    private static final int OPERATOR_LEVEL = 2;

    private FramePermissions() {
    }

    /**
     * Vero se la regola {@code server.ownerOnlyFrames} della config e' spenta, o il telaio non ha proprietario, o il
     * giocatore e' il proprietario, o il proprietario l'ha sbloccato, o il giocatore e' in creativa con livello di
     * permesso 2.
     */
    public static boolean canUse(ServerPlayer player, FrameState state) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(state, "state must not be null");
        UUID owner = state.owner();
        return !RedstoneExoframes.config().server.ownerOnlyFrames
                || owner == null
                || owner.equals(player.getUUID())
                || state.unlocked()
                || (player.isCreative() && player.hasPermissions(OPERATOR_LEVEL));
    }

    /**
     * Vero se il giocatore puo' bloccare o sbloccare il telaio per gli altri: la regola di
     * {@link #canToggleLock(UUID, boolean, FrameState)} con il livello di permesso del giocatore
     * ({@code player.hasPermissions(2)}). La stazione la usa per la vista ({@code canToggleLock}) e per il pulsante del
     * lucchetto.
     */
    public static boolean canToggleLock(ServerPlayer player, FrameState state) {
        Objects.requireNonNull(player, "player must not be null");
        return canToggleLock(player.getUUID(), player.hasPermissions(OPERATOR_LEVEL), state);
    }

    /**
     * La regola di chi blocca e sblocca un telaio per gli altri (design §5.11): un operatore (livello di permesso 2) o
     * il proprietario; un telaio senza proprietario lo cambia solo un operatore. Una regola sola per i due ingressi,
     * che si distinguono solo per da dove viene il permesso: la stazione con quello del giocatore
     * ({@link #canToggleLock(ServerPlayer, FrameState)}), {@code /exoframe lock|unlock} con quello della sorgente del
     * comando ({@code source.hasPermission(2)} in {@code ExoframeCommand.setUnlocked}: con {@code execute as} da RCON
     * vale quello della sorgente, non quello del giocatore).
     *
     * @param actor    chi blocca o sblocca
     * @param operator vero se chi agisce ha il livello di permesso 2
     */
    public static boolean canToggleLock(UUID actor, boolean operator, FrameState state) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(state, "state must not be null");
        return operator || actor.equals(state.owner());
    }
}
