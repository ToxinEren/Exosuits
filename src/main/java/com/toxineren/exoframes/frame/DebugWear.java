package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Il telaio di prova del comando di debug (design, par. 5.5): un telaio vero, con il suo record indossato nel registro
 * ({@link Compat#frameState()}), cosi' l'autoriparazione degli slot ({@link SlotGuard}) non scambia i suoi segnaposto
 * per segnaposto rimasti senza stato. Dopo aver vestito o svestito il giocatore ne ricalcola subito le dimensioni
 * ({@link SuitSize}).
 */
public final class DebugWear {
    private DebugWear() {
    }

    /**
     * Crea il record indossato del giocatore se non ne ha uno, con lui come proprietario, oppure ne sostituisce pezzi
     * e cella, cosi' che il riassunto dello stato sia esattamente {@code summary}: per ogni slot del riassunto un
     * pezzo nuovo del suo livello, con la durabilita' piu' alta che da' la sua condizione e con il suo aspetto; gli
     * altri slot vuoti (i pezzi che c'erano si eliminano); una cella carica. L'armatura stivata nel record non si
     * tocca. Poi lo veste ({@link FrameEntry#dress}) e ricalcola subito le dimensioni: cio' che occupava gli slot
     * armatura va nell'inventario, o a terra ai piedi del giocatore se non entra ({@link Handouts#give}).
     */
    public static void wear(ServerPlayer player, SuitSummary summary) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        FrameStateAccess frames = Compat.frameState();
        frames.update(player, state -> rebuild(state, player, summary));
        FrameState state = frames.get(player).orElseThrow(
                () -> new IllegalStateException("no worn frame for " + player.getUUID() + " after update"));
        FrameEntry.dress(player, SuitSummary.of(state));
        player.refreshDimensions(); // grandezza reale nello stesso tick, senza aspettare SuitSize.tick
    }

    /**
     * Elimina il telaio che il giocatore indossa, comunque ci sia entrato: i segnaposto lo lasciano da ogni posto in
     * cui possono stare ({@link Placeholders#removeAll}), l'armatura stivata nel record gli torna come all'uscita (nel
     * suo slot, o con {@link Handouts#give}), poi il record indossato si toglie dal registro; le dimensioni si
     * ricalcolano subito. Senza un record indossato toglie solo i segnaposto.
     *
     * @return quanti segnaposto ha tolto
     */
    public static int remove(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        FrameStateAccess frames = Compat.frameState();
        int removed = Placeholders.removeAll(player);
        if (frames.wornFrameId(player).isPresent()) {
            FrameEntry.returnStowed(player, frames);
            frames.clear(player);
        }
        player.refreshDimensions(); // grandezza vanilla nello stesso tick, senza aspettare SuitSize.tick
        return removed;
    }

    /** Pezzi e cella dello stato sostituiti da quelli di {@code summary}; il proprietario resta, o diventa lui. */
    private static void rebuild(FrameState state, ServerPlayer player, SuitSummary summary) {
        if (state.owner() == null) {
            state.setOwner(player.getUUID());
        }
        for (PieceSlot slot : PieceSlot.values()) {
            state.unmount(slot);
        }
        for (Map.Entry<PieceSlot, PieceLook> entry : summary.pieces().entrySet()) {
            PieceLook look = entry.getValue();
            Piece piece = Piece.fresh(entry.getKey(), look.tier(), RedstoneExoframes.config().tiers);
            piece.damage(piece.maxDurability() - durabilityFor(piece.maxDurability(), look.condition()));
            state.mount(piece);
            state.setAppearance(entry.getKey(), look.appearance());
        }
        state.insertCell(ModItems.REDSTONE_CELL.get().toCell(CellItem.charged()));
    }

    /** La durabilita' piu' alta, fra 0 e {@code max}, che da' la condizione. */
    private static int durabilityFor(int max, PieceCondition condition) {
        for (int durability = max; durability > 0; durability--) {
            if (PieceCondition.of(durability, max) == condition) {
                return durability;
            }
        }
        return 0;
    }
}
