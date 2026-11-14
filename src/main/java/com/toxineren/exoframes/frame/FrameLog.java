package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Le righe del log che dicono dove e' andato un telaio (design, par. 5.5): una riga INFO per ogni evento strutturale
 * (piazzato, raccolto, rimosso, indossato, lasciato, lasciato alla morte), con l'identita' del telaio, il giocatore, la
 * dimensione e la posizione. "Il mio telaio e' sparito" si risponde leggendo il log del server.
 */
public final class FrameLog {
    private FrameLog() {
    }

    /**
     * Scrive la riga di un evento: {@code what} e' il verbo inglese ("placed", "left at death"...); {@code player} chi
     * l'ha fatto, null se nessuno (un oggetto usato senza giocatore).
     */
    public static void event(UUID frameId, String what, Player player, ServerLevel level, Vec3 position) {
        RedstoneExoframes.LOGGER.info("Frame {} {} by {} in {} at {}", frameId, what,
                player == null ? "nobody" : player.getScoreboardName(), level.dimension().location(),
                String.format(Locale.ROOT, "%.2f, %.2f, %.2f", position.x, position.y, position.z));
    }
}
