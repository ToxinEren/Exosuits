package com.toxineren.exoframes.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Andata e ritorno del pacchetto {@code client_rules}: il codec non filtra i valori, li filtra ClientRules.accept. */
class ClientRulesPacketTest {

    @Test
    void roundTrips() {
        for (ClientRulesPacket packet : List.of(new ClientRulesPacket(10_000, 2, 1),
                new ClientRulesPacket(1_000_000, 30, 0), new ClientRulesPacket(-1, 0, 31))) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            ClientRulesPacket.CODEC.write(buf, packet);
            assertEquals(packet, ClientRulesPacket.CODEC.read(buf));
            assertEquals(0, buf.readableBytes());
        }
        assertEquals(new ClientRulesPacket(10_000, 2, 1), ClientRulesPacket.of(ExoframesConfig.defaults()));
    }
}
