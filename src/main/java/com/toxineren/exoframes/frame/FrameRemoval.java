package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * L'eliminazione per sempre di un telaio piazzato ({@code /exoframe remove}, design §5.4: solo dal registro), con
 * tutto cio' che puo' diventare oggetto lasciato a terra ai suoi piedi.
 */
public final class FrameRemoval {
    private FrameRemoval() {
    }

    /**
     * L'esito di {@link #remove}: se il telaio e' stato tolto, e gli slot dei pezzi del cui livello non c'e' un
     * oggetto, che se ne sono andati con il telaio (anche quando il registro ha rifiutato a meta').
     */
    public record Result(boolean removed, List<PieceSlot> deleted) {
        public Result {
            deleted = List.copyOf(deleted);
        }
    }

    /**
     * Elimina per sempre il telaio piazzato. Prima ne esce tutto cio' che puo' diventare oggetto, a terra fermo ai
     * piedi del telaio ({@link Handouts#dropStill}), e ogni oggetto arriva a terra solo dopo aver lasciato il record:
     * ogni pezzo montato con i moduli e l'aspetto del suo slot ({@link FrameInteractions#pieceStack}), la cella con la
     * sua carica, l'armatura stivata che si legge. Poi il record si toglie ({@link FrameStateAccess#removePlaced}: le
     * voci stivate che non si leggono vanno fra quelle dei telai tolti), l'entita' sparisce e il log lo dice a nome di
     * {@code by}. Un pezzo del cui livello non c'e' un oggetto se ne va con il telaio: il suo slot e' in
     * {@link Result#deleted}. Non tolto senza il record piazzato o se il registro rifiuta un passo (cio' che e' gia' a
     * terra ci resta).
     */
    public static Result remove(ServerPlayer by, ExoframeEntity frame) {
        MinecraftServer server = by.getServer();
        FrameStateAccess frames = Compat.frameState();
        Optional<PlacedFrame> record = frames.placed(server, frame.frameId());
        if (record.isEmpty()) {
            return new Result(false, List.of());
        }
        UUID frameId = record.get().frameId();
        FrameState state = record.get().state();
        Level level = frame.level();
        Vec3 feet = record.get().position();
        List<PieceSlot> deleted = new ArrayList<>();
        for (PieceSlot slot : PieceSlot.values()) {
            if (state.pieces().get(slot) == null) {
                continue;
            }
            ItemStack stack;
            try {
                stack = FrameInteractions.pieceStack(state, slot);
            } catch (IllegalArgumentException e) {
                deleted.add(slot);
                continue;
            }
            if (!frames.updatePlaced(server, frameId, s -> s.unmount(slot))) {
                return new Result(false, deleted);
            }
            Handouts.dropStill(level, feet, stack);
        }
        if (state.cell() != null) {
            ItemStack cell = CellItem.fromCell(state.cell());
            if (!frames.updatePlaced(server, frameId, FrameState::removeCell)) {
                return new Result(false, deleted);
            }
            Handouts.dropStill(level, feet, cell);
        }
        for (StowedStack stowed : frames.takeStowedOfPlaced(server, frameId)) {
            Handouts.dropStill(level, feet, stowed.stack());
        }
        if (!frames.removePlaced(server, frameId)) {
            return new Result(false, deleted);
        }
        frame.discard();
        FrameLog.event(frameId, "removed", by, (ServerLevel) level, feet);
        return new Result(true, deleted);
    }
}
