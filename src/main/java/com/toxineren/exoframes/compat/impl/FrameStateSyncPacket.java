package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto S2C con lo stato del telaio di un giocatore, serializzato da {@link FrameStateCodec}; uno stato assente
 * dice al client di dimenticare la copia locale. Si registra e si invia solo tramite {@code Compat.net()}.
 * <p>
 * Formato: UUID del giocatore, boolean di presenza, poi il tag solo se presente. Le chiamate al buffer sono le
 * stesse su 1.20.1 e 1.21.1 ({@code readNbt()} restituisce un CompoundTag su entrambe).
 */
record FrameStateSyncPacket(UUID player, Optional<CompoundTag> state) {
    static final ResourceLocation ID = Ids.of("frame_state_sync");

    static final PacketCodec<FrameStateSyncPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, FrameStateSyncPacket msg) {
            buf.writeUUID(msg.player());
            buf.writeBoolean(msg.state().isPresent());
            msg.state().ifPresent(buf::writeNbt);
        }

        @Override
        public FrameStateSyncPacket read(FriendlyByteBuf buf) {
            UUID player = buf.readUUID();
            boolean present = buf.readBoolean();
            return new FrameStateSyncPacket(player, present ? Optional.ofNullable(buf.readNbt()) : Optional.empty());
        }
    };
}
