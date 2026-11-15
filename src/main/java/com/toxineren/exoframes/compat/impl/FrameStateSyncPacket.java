package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import com.toxineren.exoframes.core.energy.EnergyState;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto S2C con lo stato del telaio di un giocatore, serializzato da {@link FrameStateCodec}, e i tre indicatori
 * calcolati dal server ({@link FrameIndicators}, design §12.4); uno stato assente dice al client di dimenticare la
 * copia locale. Stato e indicatori sono presenti o assenti insieme. Si registra e si invia solo tramite
 * {@code Compat.net()}.
 * <p>
 * Formato: UUID del giocatore, boolean di presenza, poi solo se presente il tag, {@code canSprint}, {@code canSwim} e
 * lo stato dell'energia come enum. Le chiamate al buffer sono le stesse su 1.20.1 e 1.21.1 ({@code readNbt()}
 * restituisce un CompoundTag su entrambe, {@code writeEnum}/{@code readEnum} esistono su tutte e due). Un tag che si
 * rilegge null vale come stato assente: gli indicatori si leggono comunque e si scartano.
 */
record FrameStateSyncPacket(UUID player, Optional<CompoundTag> state, Optional<FrameIndicators> indicators) {
    static final ResourceLocation ID = Ids.of("frame_state_sync");

    static final PacketCodec<FrameStateSyncPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, FrameStateSyncPacket msg) {
            buf.writeUUID(msg.player());
            buf.writeBoolean(msg.state().isPresent());
            if (msg.state().isPresent()) {
                FrameIndicators indicators = msg.indicators().orElseThrow();
                buf.writeNbt(msg.state().get());
                buf.writeBoolean(indicators.canSprint());
                buf.writeBoolean(indicators.canSwim());
                buf.writeEnum(indicators.energyState());
            }
        }

        @Override
        public FrameStateSyncPacket read(FriendlyByteBuf buf) {
            UUID player = buf.readUUID();
            if (!buf.readBoolean()) {
                return new FrameStateSyncPacket(player, Optional.empty(), Optional.empty());
            }
            CompoundTag tag = buf.readNbt();
            boolean canSprint = buf.readBoolean();
            boolean canSwim = buf.readBoolean();
            EnergyState energyState = buf.readEnum(EnergyState.class);
            if (tag == null) {
                return new FrameStateSyncPacket(player, Optional.empty(), Optional.empty());
            }
            return new FrameStateSyncPacket(player, Optional.of(tag),
                    Optional.of(new FrameIndicators(canSprint, canSwim, energyState)));
        }
    };

    FrameStateSyncPacket {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(indicators, "indicators");
        if (state.isPresent() != indicators.isPresent()) {
            throw new IllegalArgumentException("state and indicators must be both present or both absent");
        }
    }
}
