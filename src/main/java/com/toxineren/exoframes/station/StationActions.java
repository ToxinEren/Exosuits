package com.toxineren.exoframes.station;

import com.toxineren.exoframes.command.ExoframeCommand;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.core.station.StationAction;
import com.toxineren.exoframes.frame.FrameFitting;
import com.toxineren.exoframes.frame.FramePermissions;
import com.toxineren.exoframes.frame.FrameSounds;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * I pulsanti della stazione di manutenzione sul server (design M4 §5.6, §5.7 e §5.11): il gestore di
 * {@link StationMenu#clickMenuButton}. Il gioco base ha gia' scartato i pulsanti di un menu che non e' quello aperto,
 * quelli degli spettatori e quelli di un menu che non vale piu' ({@link StationMenu#stillValid}); i pacchetti dello
 * stesso giocatore arrivano e si eseguono in ordine sul thread del server. Montare e smontare passano da
 * {@link FrameFitting}, la regola delle mani; il lucchetto dalla regola di {@code /exoframe lock|unlock}
 * ({@link FramePermissions#canToggleLock}).
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
     * <li>le azioni che questa parte non fa ancora (riparare, verniciare, i preset) danno {@link Outcome#IGNORED},
     * senza turno e senza log;</li>
     * <li>al piu' un'azione che scrive per tick del server per menu: le altre dello stesso tick danno
     * {@link Outcome#THROTTLED};</li>
     * <li>il record del telaio deve esserci ancora e il giocatore lo deve poter usare ({@link FramePermissions#canUse},
     * riletto a ogni azione), altrimenti {@link Outcome#REFUSED};</li>
     * <li>montare (dal cursore) o smontare con {@link FrameFitting}, che fa i controlli dell'azione, un solo
     * {@code updatePlaced} e, solo se e' riuscito, consegna, consuma, rifa' il riassunto e suona; oppure il lucchetto
     * ({@link #toggleLock}). Se il registro rifiuta la scrittura, il messaggio {@link StationTexts#FAILED} sulla barra
     * delle azioni e {@link Outcome#FAILED}.</li>
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
        if (!handled(action.kind())) {
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
        Outcome outcome = switch (action.kind()) {
            case MOUNT_PIECE -> outcome(FrameFitting.mountPiece(player, frame, menu.getCarried(), action.slot()));
            case MOUNT_CELL -> outcome(FrameFitting.insertCell(player, frame, menu.getCarried()));
            case UNMOUNT_PIECE -> outcome(FrameFitting.unmountPiece(player, frame, action.slot()));
            case UNMOUNT_CELL -> outcome(FrameFitting.removeCell(player, frame));
            case TOGGLE_LOCK -> toggleLock(player, frame);
            default -> throw new IllegalStateException("not an action of this part: " + action);
        };
        if (outcome == Outcome.FAILED) {
            player.displayClientMessage(Component.translatable(StationTexts.FAILED), true);
        }
        return outcome;
    }

    /**
     * Il lucchetto (design §5.11 e §5.17): sblocca il telaio bloccato e blocca quello sbloccato. Lo puo' fare chi ha
     * il permesso di {@code /exoframe lock|unlock} ({@link FramePermissions#canToggleLock}, il proprietario o un
     * operatore), altrimenti {@link Outcome#REFUSED}, senza messaggi; poi un solo {@code updatePlaced} e, solo se e'
     * riuscito, sulla barra delle azioni la risposta del comando ({@link ExoframeCommand#UNLOCK_DONE} o
     * {@link ExoframeCommand#LOCK_DONE}) e il suono {@link FrameSounds.Sound#UNLOCK} o {@link FrameSounds.Sound#LOCK}
     * nel punto del telaio. Se il registro rifiuta la scrittura, {@link Outcome#FAILED}. Un altro giocatore con la
     * schermata aperta sullo stesso telaio, che bloccato non puo' piu' usarlo, la vede chiudersi al tick dopo
     * ({@link StationMenu#stillValid}).
     */
    private static Outcome toggleLock(ServerPlayer player, PlacedFrame frame) {
        if (!FramePermissions.canToggleLock(player, frame.state())) {
            return Outcome.REFUSED;
        }
        boolean unlock = !frame.state().unlocked();
        if (!Compat.frameState().updatePlaced(player.getServer(), frame.frameId(), s -> s.setUnlocked(unlock))) {
            return Outcome.FAILED;
        }
        player.displayClientMessage(Component.translatable(unlock ? ExoframeCommand.UNLOCK_DONE
                : ExoframeCommand.LOCK_DONE), true);
        FrameSounds.play(player.serverLevel(), frame.position(),
                unlock ? FrameSounds.Sound.UNLOCK : FrameSounds.Sound.LOCK);
        return Outcome.DONE;
    }

    /** L'esito con lo stesso nome del risultato di {@link FrameFitting}. */
    private static Outcome outcome(FrameFitting.Result result) {
        return switch (result) {
            case DONE -> Outcome.DONE;
            case REFUSED -> Outcome.REFUSED;
            case FAILED -> Outcome.FAILED;
        };
    }

    /** Vero per le azioni che scrivono e che questa parte fa: montare e smontare pezzi e cella, e il lucchetto. */
    private static boolean handled(StationAction.Kind kind) {
        return switch (kind) {
            case MOUNT_PIECE, MOUNT_CELL, UNMOUNT_PIECE, UNMOUNT_CELL, TOGGLE_LOCK -> true;
            default -> false;
        };
    }
}
