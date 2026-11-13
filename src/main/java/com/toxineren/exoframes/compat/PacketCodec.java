package com.toxineren.exoframes.compat;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Serializzazione di un messaggio di rete di tipo {@code T} su un {@link FriendlyByteBuf}, uguale sulle due versioni
 * (su 1.21.1 il buffer effettivo e' un {@code RegistryFriendlyByteBuf}, che estende {@link FriendlyByteBuf}).
 * {@link #read} deve leggere esattamente cio' che {@link #write} ha scritto, nello stesso ordine.
 */
public interface PacketCodec<T> {
    /** Scrive il messaggio nel buffer. */
    void write(FriendlyByteBuf buf, T msg);

    /** Legge un messaggio dal buffer. */
    T read(FriendlyByteBuf buf);
}
