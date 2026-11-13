package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Stato del telaio per giocatore ({@link FrameState}), con persistenza sul server e copia sincronizzata sul client.
 * <p>
 * Persistenza: un solo SavedData nel DimensionDataStorage dell'overworld, mappa UUID del giocatore -> stato. E' la
 * stessa implementazione sui quattro target (nessun attachment/capability del loader), e lo stato sopravvive alla
 * morte e al cambio di dimensione perche' non sta sull'entita'.
 * <p>
 * Sincronizzazione: il server e' l'unica autorita'. Ogni giocatore riceve solo il proprio stato, intero, con un
 * pacchetto S2C: all'ingresso nel mondo, al respawn, dopo ogni {@link #update} e dopo {@link #clear} (stato
 * assente). Il client conserva l'ultima copia ricevuta finche' non esce dal mondo. Le modifiche avvengono solo sul
 * server con i metodi di scrittura ({@link #update}, {@link #mutate}, {@link #clear}); non esistono ancora pacchetti
 * C2S (arrivano con i loro consumatori).
 * <p>
 * Modifiche ad alta frequenza (per esempio il consumo di energia a ogni tick): {@link #mutate} segna il dato come da
 * salvare senza sincronizzare, e il chiamante invia lo stato con {@link #syncTo} al proprio ritmo (al massimo 4-5
 * volte al secondo).
 */
public interface FrameStateAccess {
    /**
     * Lo stato del giocatore. Sul server logico legge il dato salvato senza crearlo; sul client restituisce la copia
     * sincronizzata ({@link #clientView}). L'oggetto restituito e' lo stato vivo: va trattato in sola lettura, e ogni
     * modifica passa da un metodo di scrittura ({@link #update}, {@link #mutate}, {@link #clear}).
     */
    Optional<FrameState> get(Player p);

    /**
     * Crea lo stato se assente, applica la modifica, segna il dato come da salvare e sincronizza il giocatore. Solo
     * sul server, sul thread di gioco.
     */
    void update(ServerPlayer p, Consumer<FrameState> change);

    /**
     * Se il giocatore ha uno stato, applica la modifica e segna il dato come da salvare (anche se la modifica lancia),
     * senza crearlo e senza sincronizzare: e' il percorso per le modifiche ad alta frequenza, e il chiamante chiama
     * {@link #syncTo} al proprio ritmo. Solo sul server, sul thread di gioco.
     *
     * @return true se lo stato c'era ed e' stata applicata la modifica, false se il giocatore non ha uno stato
     */
    boolean mutate(ServerPlayer p, Consumer<FrameState> change);

    /** Rimuove lo stato del giocatore, segna il dato come da salvare e invia al giocatore uno stato assente. */
    void clear(ServerPlayer p);

    /**
     * La copia sincronizzata lato client dello stato del giocatore, se ricevuta. E' la copia viva della cache: va
     * trattata in sola lettura (le modifiche passano solo dai metodi di scrittura, sul server).
     */
    Optional<FrameState> clientView(Player clientPlayer);

    /** Invia al giocatore il suo stato attuale (o l'assenza di stato). */
    void syncTo(ServerPlayer p);

    /**
     * Registrazioni comuni, da chiamare una volta nell'init comune dopo le altre: il pacchetto S2C di
     * sincronizzazione e gli eventi di ingresso e respawn che sincronizzano il giocatore.
     */
    void registerCommon();

    /**
     * Registrazioni solo client, da chiamare una volta e solo dagli entrypoint client: svuota la copia locale quando
     * il giocatore esce dal mondo. Su un server dedicato non va mai chiamato (carica classi del client).
     */
    void registerClient();
}
