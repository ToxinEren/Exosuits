package com.toxineren.exoframes.station;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import com.toxineren.exoframes.core.station.StationView;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto S2C {@code redstone_exoframes:station_view} (design M4 §5.13): la vista nuova del telaio per il menu della
 * stazione con il numero {@code containerId}. Lo manda {@link StationMenu#broadcastChanges} quando la vista cambia, al
 * piu' una volta per tick per menu; sul client {@link StationNet#apply} la mette nel menu aperto con quel numero.
 * <p>
 * Formato: {@code writeVarInt} del numero del menu, poi la vista con {@link StationViewCodec}.
 *
 * @param containerId il numero del menu della stazione a cui va la vista
 * @param view la vista del telaio
 */
public record StationViewPacket(int containerId, StationView view) {
    /** L'id del canale. */
    public static final ResourceLocation ID = Ids.of("station_view");

    /** Il codec: il numero del menu, poi la vista. */
    public static final PacketCodec<StationViewPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, StationViewPacket msg) {
            buf.writeVarInt(msg.containerId());
            StationViewCodec.INSTANCE.write(buf, msg.view());
        }

        @Override
        public StationViewPacket read(FriendlyByteBuf buf) {
            int containerId = buf.readVarInt();
            return new StationViewPacket(containerId, StationViewCodec.INSTANCE.read(buf));
        }
    };

    public StationViewPacket {
        Objects.requireNonNull(view, "view must not be null");
    }
}
