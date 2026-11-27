package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Montare e smontare pezzi e cella su un telaio piazzato (design M4 §5.7, e M3 §5.4 per le regole): la regola in
 * comune fra le mani ({@link FrameInteractions}) e la stazione di manutenzione ({@code StationActions}), una regola
 * sola con due ingressi. Chi chiama ha gia' scelto lo slot e controllato che il giocatore possa usare il telaio
 * ({@link FramePermissions#canUse}).
 * <p>
 * Il registro e' la verita' (design §4.5): ogni metodo controlla prima tutto cio' che lo stato potrebbe rifiutare e
 * costruisce prima l'oggetto da consegnare, poi fa un solo {@link FrameStateAccess#updatePlaced} con tutto il cambio,
 * e solo se e' riuscito consegna o consuma, rifa' il riassunto dell'entita' del telaio
 * ({@link FramePlacement#refreshView}) e suona nel punto del telaio: {@link FrameSounds.Sound#MOUNT} per montare,
 * {@link FrameSounds.Sound#UNMOUNT} per smontare. Cio' che si consegna va con {@link Handouts#give}: nell'inventario, e
 * cio' che non entra cade fermo ai piedi del giocatore. Lo stack di cui si monta un pezzo o una cella cala di uno,
 * salvo la capacita' {@code instabuild} (la creativa: la convenzione della parte 3b); nient'altro tocca lo stack.
 */
public final class FrameFitting {
    /** L'esito di un cambio. */
    public enum Result {
        /** Il registro ha preso il cambio: il giocatore ha avuto o dato cio' che doveva. */
        DONE,
        /**
         * Nulla da fare: lo slot e' vuoto o occupato, lo stack non va, il pezzo non ha un oggetto. Nulla e' cambiato.
         */
        REFUSED,
        /** Il registro ha rifiutato la scrittura (in sola lettura): nulla e' cambiato, nulla preso e nulla dato. */
        FAILED
    }

    private FrameFitting() {
    }

    /**
     * Monta nello slot il pezzo dello stack: lo stack deve essere un oggetto pezzo del tipo dello slot, altrimenti
     * {@link Result#REFUSED} senza messaggi; con lo slot occupato il messaggio {@link FrameInteractions#SLOT_TAKEN}
     * sulla barra delle azioni e {@link Result#REFUSED} (nessuno scambio: si smonta prima). Il pezzo viene da
     * {@link PieceItem#toPiece}, moduli e aspetto dai dati dello stack, come a mano.
     */
    public static Result mountPiece(ServerPlayer player, PlacedFrame frame, ItemStack stack, PieceSlot slot) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        if (!(stack.getItem() instanceof PieceItem item) || !item.kind().slots().contains(slot)) {
            return Result.REFUSED;
        }
        if (frame.state().pieces().containsKey(slot)) {
            FrameInteractions.tell(player, FrameInteractions.SLOT_TAKEN);
            return Result.REFUSED;
        }
        Piece piece = item.toPiece(stack, slot);
        Optional<PieceData> data = Compat.itemData().piece(stack);
        List<ModuleType> modules = data.map(PieceData::modules).orElse(List.of());
        Appearance appearance = data.map(PieceData::appearance).orElse(Appearance.DEFAULT);
        // Lo slot e' libero e i moduli letti dallo stack sono distinti e non null: mount, restoreModules e
        // setAppearance non rifiutano.
        boolean mounted = Compat.frameState().updatePlaced(player.getServer(), frame.frameId(), s -> {
            s.mount(piece);
            if (!modules.isEmpty()) {
                s.restoreModules(slot, modules);
            }
            s.setAppearance(slot, appearance);
        });
        if (!mounted) {
            return Result.FAILED;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        changed(player, frame, FrameSounds.Sound.MOUNT);
        return Result.DONE;
    }

    /**
     * Monta la cella dello stack, che deve essere un oggetto cella, altrimenti {@link Result#REFUSED}; se il telaio ne
     * ha gia' una, le due si scambiano nello stesso cambio (design M3 §12.10) e la vecchia va al giocatore, anche in
     * creativa. La capacita' e' quella della config ({@link CellItem#toCell}).
     */
    public static Result insertCell(ServerPlayer player, PlacedFrame frame, ItemStack stack) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        if (!(stack.getItem() instanceof CellItem item)) {
            return Result.REFUSED;
        }
        EnergyCell incoming = item.toCell(stack);
        EnergyCell[] old = new EnergyCell[1];
        boolean inserted = Compat.frameState().updatePlaced(player.getServer(), frame.frameId(), s -> {
            old[0] = s.removeCell();
            s.insertCell(incoming);
        });
        if (!inserted) {
            return Result.FAILED;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (old[0] != null) {
            Handouts.give(player, CellItem.fromCell(old[0]));
        }
        changed(player, frame, FrameSounds.Sound.MOUNT);
        return Result.DONE;
    }

    /**
     * Smonta il pezzo dello slot e lo da' al giocatore come oggetto, con durata, moduli e aspetto dello slot
     * ({@link FrameInteractions#pieceStack}). Uno slot vuoto da' {@link Result#REFUSED}; un pezzo di un livello senza
     * oggetto resta montato, con un avviso nel log, e da' {@link Result#REFUSED}.
     */
    public static Result unmountPiece(ServerPlayer player, PlacedFrame frame, PieceSlot slot) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        FrameState state = frame.state();
        if (!state.pieces().containsKey(slot)) {
            return Result.REFUSED;
        }
        ItemStack stack;
        try {
            stack = FrameInteractions.pieceStack(state, slot);
        } catch (IllegalArgumentException e) {
            RedstoneExoframes.LOGGER.warn("Cannot unmount {} from frame {}: {}", slot, frame.frameId(),
                    e.getMessage());
            return Result.REFUSED;
        }
        if (!Compat.frameState().updatePlaced(player.getServer(), frame.frameId(), s -> s.unmount(slot))) {
            return Result.FAILED;
        }
        Handouts.give(player, stack);
        changed(player, frame, FrameSounds.Sound.UNMOUNT);
        return Result.DONE;
    }

    /** Smonta la cella e la da' al giocatore con la sua carica; senza cella {@link Result#REFUSED}. */
    public static Result removeCell(ServerPlayer player, PlacedFrame frame) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        EnergyCell cell = frame.state().cell();
        if (cell == null) {
            return Result.REFUSED;
        }
        ItemStack stack = CellItem.fromCell(cell);
        if (!Compat.frameState().updatePlaced(player.getServer(), frame.frameId(), FrameState::removeCell)) {
            return Result.FAILED;
        }
        Handouts.give(player, stack);
        changed(player, frame, FrameSounds.Sound.UNMOUNT);
        return Result.DONE;
    }

    /** Dopo un cambio riuscito: il riassunto dell'entita' dal record e il suono nel punto del telaio. */
    private static void changed(ServerPlayer player, PlacedFrame frame, FrameSounds.Sound sound) {
        FramePlacement.refreshView(player.serverLevel(), frame.frameId());
        FrameSounds.play(player.serverLevel(), frame.position(), sound);
    }
}
