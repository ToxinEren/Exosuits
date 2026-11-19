package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.StationBlock;
import com.toxineren.exoframes.core.station.StationReach;
import com.toxineren.exoframes.frame.FramePermissions;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La ricerca del telaio su cui la stazione di manutenzione si apre (design M4 §5.2), sul server. La regola della
 * portata e la scelta fra piu' telai stanno in {@link StationReach}; qui il registro dei telai, la dimensione, la
 * distanza della config ({@code station.reachBlocks}) e chi puo' usare un telaio ({@link FramePermissions#canUse}).
 * La stazione agisce solo sui telai piazzati, mai sulla tuta indossata: il registro dei piazzati non contiene i telai
 * indossati.
 */
public final class StationFinder {
    /** Come e' andata la ricerca. */
    public enum Outcome {
        /** C'e' un telaio a portata che il giocatore puo' usare: quello scelto. */
        FOUND,
        /** Nessun telaio piazzato e' a portata della stazione. */
        NONE_IN_REACH,
        /** Ci sono telai a portata, ma il giocatore non ne puo' usare nessuno. */
        ALL_LOCKED
    }

    /**
     * L'esito della ricerca: il telaio scelto c'e' se e solo se l'esito e' {@link Outcome#FOUND}.
     *
     * @throws IllegalArgumentException se esito e telaio non vanno d'accordo
     */
    public record Result(Outcome outcome, Optional<UUID> frameId) {
        public Result {
            Objects.requireNonNull(outcome, "outcome must not be null");
            Objects.requireNonNull(frameId, "frameId must not be null");
            if ((outcome == Outcome.FOUND) != frameId.isPresent()) {
                throw new IllegalArgumentException("outcome " + outcome + " with frame " + frameId);
            }
        }
    }

    private StationFinder() {
    }

    /**
     * L'impronta della stazione la cui meta' principale, nello stato {@code mainState}, sta in {@code main}: i due
     * blocchi, la principale e la laterale ({@link StationBlock#otherHalf}).
     */
    public static StationReach.Footprint footprint(BlockPos main, BlockState mainState) {
        BlockPos side = StationBlock.otherHalf(main, mainState);
        return StationReach.of(main.getX(), main.getY(), main.getZ(), side.getX(), side.getZ());
    }

    /**
     * Vero se il telaio piazzato e' a portata della stazione: nella stessa dimensione ({@code stationDimension}) e
     * con i piedi a portata dell'impronta ({@link StationReach#inReach}) per la distanza della config.
     */
    public static boolean inReach(PlacedFrame frame, ResourceKey<Level> stationDimension,
            StationReach.Footprint footprint) {
        Vec3 feet = frame.position();
        return frame.dimension().equals(stationDimension) && StationReach.inReach(footprint, feet.x, feet.y, feet.z,
                RedstoneExoframes.config().station.reachBlocks);
    }

    /**
     * Il telaio su cui la stazione si apre per il giocatore: fra i telai piazzati del livello ({@code placedIn}) quelli
     * a portata ({@link #inReach}); nessuno, {@link Outcome#NONE_IN_REACH}. Di quelli, quelli che il giocatore puo'
     * usare; nessuno, {@link Outcome#ALL_LOCKED}. Altrimenti {@link Outcome#FOUND} con il piu' vicino secondo
     * {@link StationReach#nearest}.
     */
    public static Result find(ServerPlayer player, ServerLevel level, BlockPos main, BlockState mainState) {
        StationReach.Footprint footprint = footprint(main, mainState);
        List<PlacedFrame> near = Compat.frameState().placedIn(level).stream()
                .filter(frame -> inReach(frame, level.dimension(), footprint))
                .toList();
        if (near.isEmpty()) {
            return new Result(Outcome.NONE_IN_REACH, Optional.empty());
        }
        List<StationReach.Candidate> usable = near.stream()
                .filter(frame -> FramePermissions.canUse(player, frame.state()))
                .map(frame -> new StationReach.Candidate(frame.frameId(), frame.position().x, frame.position().y,
                        frame.position().z))
                .toList();
        if (usable.isEmpty()) {
            return new Result(Outcome.ALL_LOCKED, Optional.empty());
        }
        return new Result(Outcome.FOUND, StationReach.nearest(footprint, usable).map(StationReach.Candidate::frameId));
    }

    /**
     * La distanza della config come la dice il messaggio "nessun telaio vicino": senza decimali se e' intera
     * ({@code "2"} per 2,0), altrimenti con uno ({@code "2.5"} per 2,5), con il punto in ogni lingua.
     */
    public static String reachText(double reachBlocks) {
        if (reachBlocks == Math.rint(reachBlocks)) {
            return Long.toString((long) reachBlocks);
        }
        return String.format(Locale.ROOT, "%.1f", reachBlocks);
    }
}
