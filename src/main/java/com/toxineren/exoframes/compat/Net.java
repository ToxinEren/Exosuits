package com.toxineren.exoframes.compat;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Pacchetti di rete tipizzati, indipendenti dalla versione: ogni canale ha un id, un tipo di messaggio e un
 * {@link PacketCodec}.
 * <p>
 * {@link #registerS2C} e {@link #registerC2S} vanno chiamati nell'init comune, su entrambi i lati: l'implementazione
 * decide da se' cosa registrare sul lato in cui gira (vedi i singoli metodi). Ogni id si registra una sola volta.
 * <p>
 * Thread: i gestori sono sempre eseguiti sul thread di gioco del lato che riceve (client per S2C, server per C2S),
 * mai sul thread di rete, quindi possono toccare il mondo direttamente. Le eccezioni lanciate da un gestore sono
 * registrate nel log dall'esecutore del thread di gioco della piattaforma e non si propagano a chi ha inviato il
 * pacchetto.
 */
public interface Net {
    /**
     * Registra un canale server -> client. Sul client registra il ricevitore; su un server dedicato registra solo il
     * tipo del pacchetto (dove la versione lo richiede, cioe' su 1.21.1, per poterlo inviare), perche' li' il
     * gestore lato client non deve esistere e una doppia registrazione dello stesso tipo fallirebbe.
     */
    <T> void registerS2C(ResourceLocation id, Class<T> type, PacketCodec<T> codec, BiConsumer<T, ClientContext> handler);

    /**
     * Registra un canale client -> server. La registrazione avviene su entrambi i lati: al client serve per poter
     * inviare, il gestore viene eseguito solo sul server, con il giocatore che ha inviato il pacchetto.
     */
    <T> void registerC2S(ResourceLocation id, Class<T> type, PacketCodec<T> codec, BiConsumer<T, ServerPlayer> handler);

    /**
     * Invia al giocatore un messaggio su un canale S2C registrato. Se il client del giocatore non ha registrato il
     * canale (per esempio un client senza la mod che ha superato la negoziazione), l'invio si salta in silenzio.
     *
     * @throws IllegalStateException se l'id non e' registrato, o e' registrato per la direzione opposta (C2S)
     * @throws IllegalArgumentException se il messaggio non e' del tipo registrato per l'id
     */
    <T> void sendToPlayer(ServerPlayer player, ResourceLocation id, T msg);

    /**
     * Invia al server un messaggio su un canale C2S registrato (solo dal client).
     *
     * @throws IllegalStateException se l'id non e' registrato, o e' registrato per la direzione opposta (S2C)
     * @throws IllegalArgumentException se il messaggio non e' del tipo registrato per l'id
     */
    <T> void sendToServer(ResourceLocation id, T msg);

    /**
     * Il contesto di un pacchetto ricevuto sul client: il giocatore locale, ottenuto pigramente (il gestore gira gia'
     * sul thread del client, quindi il fornitore si puo' chiamare li').
     */
    record ClientContext(Supplier<Player> player) {
    }
}
