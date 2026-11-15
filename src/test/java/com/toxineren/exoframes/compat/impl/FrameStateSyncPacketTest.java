package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameRules;
import com.toxineren.exoframes.core.frame.FrameState;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Andata e ritorno del pacchetto {@code frame_state_sync}: stato e indicatori insieme, o nessuno dei due. */
class FrameStateSyncPacketTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static FrameState state() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.insertCell(new EnergyCell(10_000, 999));
        return state;
    }

    @Test
    void roundTripsStateAndIndicators() {
        FrameState state = state();
        CompoundTag tag = FrameStateCodec.write(state);
        assertEquals(new FrameIndicators(false, false, EnergyState.LOW),
                FrameIndicators.of(new FrameRules(ExoframesConfig.defaults()).evaluate(state))); // 999 < 10 %
        for (FrameIndicators sent : List.of(new FrameIndicators(true, false, EnergyState.NORMAL),
                new FrameIndicators(false, false, EnergyState.LOW),
                new FrameIndicators(false, true, EnergyState.EMPTY))) {
            FrameStateSyncPacket packet = new FrameStateSyncPacket(PLAYER, Optional.of(tag), Optional.of(sent));
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            FrameStateSyncPacket.CODEC.write(buf, packet);
            assertEquals(packet, FrameStateSyncPacket.CODEC.read(buf));
            assertEquals(0, buf.readableBytes());
        }
    }

    @Test
    void absentStateCarriesNoIndicators() {
        FrameStateSyncPacket packet = new FrameStateSyncPacket(PLAYER, Optional.empty(), Optional.empty());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FrameStateSyncPacket.CODEC.write(buf, packet);
        assertEquals(17, buf.readableBytes()); // 16 dell'UUID, 1 del flag: nessun indicatore sul filo
        assertEquals(packet, FrameStateSyncPacket.CODEC.read(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void mismatchedOptionalsAreRefused() {
        CompoundTag tag = FrameStateCodec.write(state());
        assertThrows(IllegalArgumentException.class,
                () -> new FrameStateSyncPacket(PLAYER, Optional.of(tag), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new FrameStateSyncPacket(PLAYER, Optional.empty(),
                Optional.of(new FrameIndicators(true, false, EnergyState.NORMAL))));
    }
}
