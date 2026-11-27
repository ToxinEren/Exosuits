package com.toxineren.exoframes.station;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.core.station.StationAction;
import com.toxineren.exoframes.frame.FrameFitting;
import com.toxineren.exoframes.frame.FramePermissions;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * I pulsanti della stazione di manutenzione sul server (design M4 §5.6 e §5.7): il gestore di
 * {@link StationMenu#clickMenuButton}. Il gioco base ha gia' scartato i pulsanti di un menu che non e' quello aperto,
 * quelli degli spettatori e quelli di un menu che non vale piu' ({@link StationMenu#stillValid}); i pacchetti dello
 * stesso giocatore arrivano e si eseguono in ordine sul thread del server. Montare e smontare passano da
 * {@link FrameFitting}, la regola delle mani.
 */
public final class StationActions {
    /** L'esito di un pulsante. */
    public enum Outcome {
        /** Nulla: un numero fuori dalla tabella, un giocatore morto, un'azione che questa parte non fa ancora. */
        IGNORED,
        /** La scelta e' cambiata. */
        SELECTED,
        /** Un'azione che scrive in un tick in cui il menu ne ha gia' fatta una: ignorata. */
        THROTTLED,
        /**
         * Nulla da fare: il telaio non c'e' piu', il giocatore non lo puo' usare, l'azione non va. Nulla e' cambiato.
         */
        REFUSED,
        /** Il registro ha rifiutato la scrittura: nulla e' cambiato, nulla preso e nulla dato, e il giocatore lo sa. */
        FAILED,
        /** Il telaio e' cambiato. */
        DONE
    }

    private StationActions() {
    }

    /**
     * Il pulsante {@code button} del giocatore sul menu, nell'ordine dei controlli di design §5.6:
     * <ol>
     * <li>il numero deve essere nella tabella ({@link StationAction#decode}) e il giocatore vivo, altrimenti
     * {@link Outcome#IGNORED}, senza log;</li>
     * <li>una scelta (0-7) cambia la scelta del menu e da' {@link Outcome#SELECTED}: la scelta e' libera, non usa il
     * turno di scrittura, e una scelta svuotata nel frattempo resta;</li>
     * <li>le azioni che questa parte non fa ancora (riparare, verniciare, i preset, il lucchetto) danno
     * {@link Outcome#IGNORED}, senza turno e senza log;</li>
     * <li>al piu' un'azione che scrive per tick del server per menu: le altre dello stesso tick danno
     * {@link Outcome#THROTTLED};</li>
     * <li>il record del telaio deve esserci ancora e il giocatore lo deve poter usare ({@link FramePermissions#canUse},
     * riletto a ogni azione), altrimenti {@link Outcome#REFUSED};</li>
     * <li>montare (dal cursore) o smontare con {@link FrameFitting}, che fa i controlli dell'azione, un solo
     * {@code updatePlaced} e, solo se e' riuscito, consegna, consuma, rifa' il riassunto e suona; se il registro
     * rifiuta la scrittura, il messaggio {@link StationTexts#FAILED} sulla barra delle azioni e
     * {@link Outcome#FAILED}.</li>
     * </ol>
     */
    public static Outcome press(StationMenu menu, ServerPlayer player, int button) {
        Objects.requireNonNull(menu, "menu must not be null");
        Objects.requireNonNull(player, "player must not be null");
        Optional<StationAction> decoded = StationAction.decode(button);
        if (decoded.isEmpty() || !player.isAlive()) {
            return Outcome.IGNORED;
        }
        StationAction action = decoded.get();
        if (!action.writes()) {
            menu.select(button);
            return Outcome.SELECTED;
        }
        if (!mountsOrUnmounts(action.kind())) {
            return Outcome.IGNORED;
        }
        MinecraftServer server = player.getServer();
        if (!menu.takeWriteTurn(server.getTickCount())) {
            return Outcome.THROTTLED;
        }
        Optional<PlacedFrame> placed = Compat.frameState().placed(server, menu.frameId());
        if (placed.isEmpty() || !FramePermissions.canUse(player, placed.get().state())) {
            return Outcome.REFUSED;
        }
        PlacedFrame frame = placed.get();
        FrameFitting.Result result = switch (action.kind()) {
            case MOUNT_PIECE -> FrameFitting.mountPiece(player, frame, menu.getCarried(), action.slot());
            case MOUNT_CELL -> FrameFitting.insertCell(player, frame, menu.getCarried());
            case UNMOUNT_PIECE -> FrameFitting.unmountPiece(player, frame, action.slot());
            case UNMOUNT_CELL -> FrameFitting.removeCell(player, frame);
            default -> throw new IllegalStateException("not a mount or unmount action: " + action);
        };
        if (result == FrameFitting.Result.FAILED) {
            player.displayClientMessage(Component.translatable(StationTexts.FAILED), true);
        }
        return switch (result) {
            case DONE -> Outcome.DONE;
            case REFUSED -> Outcome.REFUSED;
            case FAILED -> Outcome.FAILED;
        };
    }

    /** Vero per le azioni che montano o smontano pezzi e cella, le sole che scrivono in questa parte. */
    private static boolean mountsOrUnmounts(StationAction.Kind kind) {
        return switch (kind) {
            case MOUNT_PIECE, MOUNT_CELL, UNMOUNT_PIECE, UNMOUNT_CELL -> true;
            default -> false;
        };
    }
}
