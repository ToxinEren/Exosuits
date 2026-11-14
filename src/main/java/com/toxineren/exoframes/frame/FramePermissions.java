package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Chi puo' mettere le mani su un telaio (design §5.4): montare, smontare, raccoglierlo ed entrarci. */
public final class FramePermissions {
    /** Il livello di permesso con cui un giocatore in creativa tocca anche i telai bloccati degli altri. */
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
}
