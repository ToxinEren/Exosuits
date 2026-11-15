package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Il punto unico per "lo stato indossato e' cambiato" (design §12.14): dopo la scrittura nel registro lo chiamano
 * l'ingresso, l'uscita e la morte ({@link FrameEntry}) e il comando di debug ({@link DebugWear#wear} e
 * {@link DebugWear#remove}). Ricalcola l'ingombro del giocatore e scrive la riga del log; quando lo stato indossato e'
 * finito (uscita, morte, {@code debug wear via}) scarta anche il record del tick ({@link SuitTick}) e toglie gli
 * attributi della tuta. All'ingresso non applica nulla: gli attributi li applica il tick alla prima valutazione del
 * record. Rientro nel server e rinascita non passano di qui: li copre il record del tick (§12.3). {@link SlotGuard}
 * non lo chiama, perche' non scrive mai il registro: i suoi cambi degli slot sono viste del registro, non cambi dello
 * stato.
 * <p>
 * Vincoli d'ordine della parte 3b, che chi chiama rispetta: i segnaposto si tolgono prima di riprendere l'armatura
 * stivata ({@code FrameEntry.returnStowed}) e prima di {@code exit} ({@link Placeholders#removeAll} resta in
 * {@code FrameEntry.exit}, {@code FrameEntry.onDeath} e {@code DebugWear.remove}); all'ingresso il teletrasporto nel
 * punto del telaio viene prima di {@link #changed}, cosi' l'ingombro si calcola li', e l'eliminazione dell'entita' e
 * {@code SafeSpots.remember} restano prima; all'uscita {@link #changed} viene prima del passo fuori dal telaio, che
 * misura il giocatore vanilla; {@code FramePlacement.spawnView} resta prima di {@link #changed}, il messaggio
 * {@code FrameEntry.ELSEWHERE} dopo.
 */
public final class WornState {
    private WornState() {
    }

    /**
     * Lo stato indossato del giocatore e' appena cambiato nel registro: ricalcola subito le dimensioni del giocatore e
     * scrive la riga del log dell'evento ({@link FrameLog#event}). Non basterebbero giocatore ed evento: identita' del
     * telaio, livello e posizione li passa chi chiama, perche' dopo {@code exit} il telaio non e' piu' indossato (il
     * registro non la da' piu') e alla morte la posizione e' il punto di riposo del telaio, non quella del giocatore.
     * Se dopo il cambio il giocatore non indossa piu' un telaio scarta il record del tick ({@link SuitTick#forget}) e
     * toglie gli attributi della tuta ({@code Compat.attributes().clear}, idempotente).
     *
     * @param event il verbo inglese della riga del log ("entered", "left", "left at death"...)
     */
    static void changed(ServerPlayer player, String event, UUID frameId, ServerLevel level, Vec3 position) {
        player.refreshDimensions();
        FrameLog.event(frameId, event, player, level, position);
        if (Compat.frameState().wornFrameId(player).isEmpty()) {
            SuitTick.forget(player);
            Compat.attributes().clear(player);
        }
    }
}
