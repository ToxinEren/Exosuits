package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.compat.Compat;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * L'ultima posizione sicura di chi indossa un telaio (design, par. 5.4 e 5.5): dimensione e punto dei piedi
 * dell'ultima volta in cui il giocatore, dentro il telaio, stava a terra. E' il punto della tuta, quindi non cambia
 * finche' il giocatore e' in aria, su un veicolo o morto. Serve dove sotto il giocatore non c'e' suolo (la morte nel
 * vuoto, l'uscita sopra il vuoto): il telaio va li' ({@link FrameEntry}).
 * <p>
 * Solo in memoria, per server e per giocatore (UUID, cosi' sopravvive al giocatore nuovo che nasce al respawn): la
 * mappa esterna ha il server come chiave debole confrontata per identita', quindi nulla sopravvive al server ne' passa
 * a un altro mondo aperto nella stessa sessione di gioco (giocatore singolo); la voce di un giocatore si toglie quando
 * lascia il server. Solo sul thread del server.
 */
public final class SafeSpots {
    /** Ogni quanti tick del giocatore si ricorda la posizione: un secondo. */
    public static final int INTERVAL_TICKS = 20;

    private static final Map<MinecraftServer, Map<UUID, Spot>> SPOTS = new MapMaker().weakKeys().makeMap();

    private SafeSpots() {
    }

    /** Una posizione sicura: la dimensione e il punto dei piedi. */
    public record Spot(ResourceKey<Level> dimension, Vec3 position) {
        public Spot {
            Objects.requireNonNull(dimension, "dimension must not be null");
            Objects.requireNonNull(position, "position must not be null");
        }
    }

    /**
     * Registra il tick dei giocatori sul server ({@code TickEvent.PLAYER_POST}) e l'oblio all'uscita dal server
     * ({@code PlayerEvent.PLAYER_QUIT}). Una volta, nell'init comune.
     */
    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(SafeSpots::forget);
    }

    /**
     * Ogni {@link #INTERVAL_TICKS} tick del giocatore: se e' vivo, a terra, non su un veicolo e indossa un telaio
     * (secondo il registro), ricorda la sua dimensione e la sua posizione.
     */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % INTERVAL_TICKS != 0 || !player.isAlive() || !player.onGround()
                || player.isPassenger() || Compat.frameState().wornFrameId(player).isEmpty()) {
            return;
        }
        SPOTS.computeIfAbsent(player.server, server -> new HashMap<>())
                .put(player.getUUID(), new Spot(player.level().dimension(), player.position()));
    }

    /** L'ultima posizione sicura ricordata per il giocatore su questo server, o vuoto. */
    public static Optional<Spot> last(ServerPlayer player) {
        Map<UUID, Spot> spots = SPOTS.get(player.server);
        return spots == null ? Optional.empty() : Optional.ofNullable(spots.get(player.getUUID()));
    }

    private static void forget(ServerPlayer player) {
        Map<UUID, Spot> spots = SPOTS.get(player.server);
        if (spots != null) {
            spots.remove(player.getUUID());
        }
    }
}
