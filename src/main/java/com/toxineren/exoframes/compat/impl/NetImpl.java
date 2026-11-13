package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Net;
import com.toxineren.exoframes.compat.PacketCodec;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;*/
//?}

/**
 * Implementazione di {@link Net} sul NetworkManager di Architectury.
 * <p>
 * Su 1.20.1 (Architectury 9.2) i canali sono grezzi: si registra un ricevitore per id e si invia un
 * {@code FriendlyByteBuf} scritto con il {@link PacketCodec}. Su 1.21.1 (Architectury 13.0) i pacchetti sono payload
 * tipizzati: ogni canale ha un {@code CustomPacketPayload.Type} e uno {@code StreamCodec} che avvolgono il messaggio
 * nel record {@link Payload}.
 * <p>
 * Regole dei lati. S2C: il ricevitore si registra solo sul client; su un server dedicato di 1.21.1 si registra solo
 * il tipo del payload ({@code registerS2CPayloadType}), che serve al server per poterlo inviare, e mai entrambi nella
 * stessa JVM (su Fabric la doppia registrazione lancia un'eccezione); su un server di 1.20.1 non serve nulla. C2S: il
 * ricevitore si registra su entrambi i lati, perche' anche il client ne ha bisogno per poter inviare; il gestore
 * gira solo sul server.
 * <p>
 * Invio S2C: solo su NeoForge 1.21.1 si chiede prima ad Architectury ({@code NetworkManager.canPlayerReceive}, che
 * li' legge i canali negoziati nella fase di configurazione) se il client del giocatore ha il canale; se no, l'invio
 * si salta in silenzio (una riga DEBUG per id). Li' i payload di Architectury sono opzionali, quindi un client senza
 * la mod supera la negoziazione, e un invio senza controllo lancerebbe un'eccezione dentro l'evento di ingresso
 * (giocatore disconnesso con un messaggio fuorviante). Sugli altri target si invia sempre: un client senza il canale
 * ignora il pacchetto, e su 1.20.1 i canali del client arrivano al server solo dopo l'evento di ingresso, quindi il
 * controllo salterebbe a torto il sync di ingresso.
 * <p>
 * Thread: i gestori sono sempre accodati al thread di gioco con {@code PacketContext.queue}. Su 1.20.1 il buffer
 * viene decodificato prima di accodare, perche' viene rilasciato appena il ricevitore ritorna.
 */
public final class NetImpl implements Net {
    private final Map<ResourceLocation, Channel<?>> channels = new ConcurrentHashMap<>();
    /** Id S2C per cui un invio e' gia' stato saltato (solo NeoForge 1.21.1), per scriverlo una volta sola. */
    private final Set<ResourceLocation> skipped = ConcurrentHashMap.newKeySet();

    @Override
    public <T> void registerS2C(ResourceLocation id, Class<T> type, PacketCodec<T> codec,
            BiConsumer<T, ClientContext> handler) {
        Channel<T> channel = open(id, type, codec, true);
        boolean client = Platform.getEnvironment() == Env.CLIENT;
        // il messaggio e' gia' decodificato quando arriva qui: si accoda solo il gestore
        BiConsumer<T, NetworkManager.PacketContext> receive =
                (msg, ctx) -> ctx.queue(() -> handler.accept(msg, new ClientContext(ctx::getPlayer)));
        //? if >=1.21 {
        if (client) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), channel.payloadType, channel.streamCodec,
                    (payload, ctx) -> receive.accept(payload.value(), ctx));
        } else {
            NetworkManager.registerS2CPayloadType(channel.payloadType, channel.streamCodec);
        }
        //?} else {
        /*if (client) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), id, (buf, ctx) -> receive.accept(codec.read(buf), ctx));
        }*/
        //?}
    }

    @Override
    public <T> void registerC2S(ResourceLocation id, Class<T> type, PacketCodec<T> codec,
            BiConsumer<T, ServerPlayer> handler) {
        Channel<T> channel = open(id, type, codec, false);
        // il messaggio e' gia' decodificato quando arriva qui: si accoda solo il gestore
        BiConsumer<T, NetworkManager.PacketContext> receive =
                (msg, ctx) -> ctx.queue(() -> handler.accept(msg, (ServerPlayer) ctx.getPlayer()));
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.c2s(), channel.payloadType, channel.streamCodec,
                (payload, ctx) -> receive.accept(payload.value(), ctx));
        //?} else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), id, (buf, ctx) -> receive.accept(codec.read(buf), ctx));*/
        //?}
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer player, ResourceLocation id, T msg) {
        Channel<T> channel = lookup(id, msg, true);
        //? if >=1.21 && neoforge {
        if (!NetworkManager.canPlayerReceive(player, channel.payloadType)) {
            if (skipped.add(id)) {
                RedstoneExoframes.LOGGER.debug("Not sending packet {}: the client has not negotiated the channel", id);
            }
            return;
        }
        //?}
        //? if >=1.21 {
        NetworkManager.sendToPlayer(player, channel.wrap(msg));
        //?} else {
        /*NetworkManager.sendToPlayer(player, id, channel.encode(msg));*/
        //?}
    }

    @Override
    public <T> void sendToServer(ResourceLocation id, T msg) {
        Channel<T> channel = lookup(id, msg, false);
        //? if >=1.21 {
        NetworkManager.sendToServer(channel.wrap(msg));
        //?} else {
        /*NetworkManager.sendToServer(id, channel.encode(msg));*/
        //?}
    }

    /** Crea e memorizza il canale dell'id; un id gia' registrato e' un errore. */
    private <T> Channel<T> open(ResourceLocation id, Class<T> type, PacketCodec<T> codec, boolean clientbound) {
        Channel<T> channel = new Channel<>(id, type, codec, clientbound);
        if (channels.putIfAbsent(id, channel) != null) {
            throw new IllegalStateException("Packet " + id + " is already registered");
        }
        return channel;
    }

    /** Il canale registrato per l'id, controllando direzione e tipo del messaggio. */
    @SuppressWarnings("unchecked")
    private <T> Channel<T> lookup(ResourceLocation id, T msg, boolean clientbound) {
        Channel<?> channel = channels.get(id);
        if (channel == null) {
            throw new IllegalStateException("No packet registered with id " + id);
        }
        if (channel.clientbound != clientbound) {
            throw new IllegalStateException("Packet " + id + " is registered as "
                    + (channel.clientbound ? "server-to-client" : "client-to-server") + " and cannot be sent this way");
        }
        if (!channel.type.isInstance(msg)) {
            throw new IllegalArgumentException("Packet " + id + " expects " + channel.type.getName() + " but got "
                    + (msg == null ? "null" : msg.getClass().getName()));
        }
        return (Channel<T>) channel;
    }

    /** Cio' che serve per inviare su un id: il tipo del messaggio, il codec e, su 1.21.1, il tipo del payload. */
    private static final class Channel<T> {
        final Class<T> type;
        final PacketCodec<T> codec;
        final boolean clientbound;
        //? if >=1.21 {
        final CustomPacketPayload.Type<Payload<T>> payloadType;
        final StreamCodec<RegistryFriendlyByteBuf, Payload<T>> streamCodec;
        //?}

        Channel(ResourceLocation id, Class<T> type, PacketCodec<T> codec, boolean clientbound) {
            this.type = type;
            this.codec = codec;
            this.clientbound = clientbound;
            //? if >=1.21 {
            this.payloadType = new CustomPacketPayload.Type<>(id);
            this.streamCodec = StreamCodec.of((buf, payload) -> codec.write(buf, payload.value()),
                    buf -> new Payload<>(payloadType, codec.read(buf)));
            //?}
        }

        //? if >=1.21 {
        Payload<T> wrap(T msg) {
            return new Payload<>(payloadType, msg);
        }
        //?} else {
        /*FriendlyByteBuf encode(T msg) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            codec.write(buf, msg);
            return buf;
        }*/
        //?}
    }

    //? if >=1.21 {
    /** Il payload di 1.21.1: avvolge un messaggio del canale con il tipo del canale. */
    private record Payload<T>(CustomPacketPayload.Type<Payload<T>> type, T value) implements CustomPacketPayload {
    }
    //?}
}
