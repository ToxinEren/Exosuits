package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto C2S con cui il client chiede il cambio cella (design §12.10), dopo aver tenuto premuto il tasto. Non porta
 * nulla: il server decide da solo, dal proprio registro e dall'inventario di chi lo manda ({@link CellSwap#handle}).
 * Lo registra {@link CellSwap#registerNet()}.
 */
public record CellSwapPacket() {
    /** L'id del canale. */
    public static final ResourceLocation ID = Ids.of("cell_swap");

    /** Il codec vuoto: non scrive e non legge nulla. */
    public static final PacketCodec<CellSwapPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, CellSwapPacket msg) {
            // nessun contenuto
        }

        @Override
        public CellSwapPacket read(FriendlyByteBuf buf) {
            return new CellSwapPacket();
        }
    };
}
