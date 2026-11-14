package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto C2S con cui il client chiede di uscire dal telaio (design, par. 5.5), dopo aver tenuto premuto il tasto.
 * Non porta nulla: il server si fida solo di chi lo manda e del proprio registro ({@link FrameEntry#exit}). Lo registra
 * {@link FrameEntry#registerNet()}.
 */
public record FrameExitPacket() {
    /** L'id del canale. */
    public static final ResourceLocation ID = Ids.of("frame_exit");

    /** Il codec vuoto: non scrive e non legge nulla. */
    public static final PacketCodec<FrameExitPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, FrameExitPacket msg) {
            // nessun contenuto
        }

        @Override
        public FrameExitPacket read(FriendlyByteBuf buf) {
            return new FrameExitPacket();
        }
    };
}
