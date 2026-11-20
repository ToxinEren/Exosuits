package com.toxineren.exoframes.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.station.StationView;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * Andata e ritorno della vista del telaio (design M4 §5.5 e §5.13) con {@link StationViewCodec}, da sola, nel pacchetto
 * {@code station_view} ({@link StationViewPacket}) e nei dati extra dell'apertura ({@link StationOpening}): si rilegge
 * uguale e il buffer resta vuoto. La vista piu' lunga che il server puo' scrivere resta sotto un kilobyte.
 */
class StationViewCodecTest {

    /** I sei pezzi con durate diverse e i colori ai due estremi e non dipinti, la cella, tutto vero, la scelta 7. */
    private static StationView fullView() {
        Map<PieceSlot, StationView.PieceInfo> pieces = new EnumMap<>(PieceSlot.class);
        pieces.put(PieceSlot.HELMET, new StationView.PieceInfo(Tier.SCRAP, 60, 180, 0x3A6EA5, Appearance.UNPAINTED));
        pieces.put(PieceSlot.TORSO, new StationView.PieceInfo(Tier.INDUSTRIAL, 360, 360, 0, 0xFFFFFF));
        pieces.put(PieceSlot.LEFT_ARM, new StationView.PieceInfo(Tier.ARMORED, 0, 520, 0xFFFFFF, 0));
        pieces.put(PieceSlot.RIGHT_ARM, new StationView.PieceInfo(Tier.NETHERITE, 649, 650, Appearance.UNPAINTED,
                Appearance.UNPAINTED));
        pieces.put(PieceSlot.LEFT_LEG, new StationView.PieceInfo(Tier.PROTOTYPE, 1, 800, 0, 0));
        pieces.put(PieceSlot.RIGHT_LEG, new StationView.PieceInfo(Tier.SCRAP, 127, 180, 0x123456,
                Appearance.UNPAINTED));
        return new StationView(pieces, Optional.of(new StationView.CellInfo(7_420, 10_000)), true, true, true, true,
                StationView.SELECT_ALL, true, true);
    }

    private static StationView roundTrip(StationView view) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        StationViewCodec.INSTANCE.write(buf, view);
        StationView read = StationViewCodec.INSTANCE.read(buf);
        assertEquals(0, buf.readableBytes(), "bytes left after reading the view");
        return read;
    }

    @Test
    void roundTripsAFullViewAndTheOpening() {
        StationView view = fullView();
        assertEquals(6, view.pieces().size());
        assertEquals(view, roundTrip(view));
        for (PieceSlot missing : PieceSlot.values()) {
            // un bit spento per volta nel byte degli slot: ogni pezzo torna nel suo slot
            Map<PieceSlot, StationView.PieceInfo> five = new EnumMap<>(view.pieces());
            five.remove(missing);
            StationView partial = new StationView(five, view.cell(), true, true, true, true, StationView.SELECT_ALL,
                    true, true);
            assertEquals(partial, roundTrip(partial), "the full view without " + missing);
        }

        StationViewPacket packet = new StationViewPacket(42, view);
        FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
        StationViewPacket.CODEC.write(packetBuf, packet);
        assertEquals(packet, StationViewPacket.CODEC.read(packetBuf));
        assertEquals(0, packetBuf.readableBytes(), "bytes left after reading the packet");

        StationOpening opening = new StationOpening(new BlockPos(1, -60, 2),
                UUID.fromString("6c1f2a8e-93b4-4d57-a0e2-1b9f3c7d5e40"), view);
        FriendlyByteBuf openingBuf = new FriendlyByteBuf(Unpooled.buffer());
        opening.write(openingBuf);
        assertEquals(opening, StationOpening.read(openingBuf));
        assertEquals(0, openingBuf.readableBytes(), "bytes left after reading the opening");
    }

    @Test
    void roundTripsABareFrameWithoutCell() {
        StationView bare = new StationView(Map.of(), Optional.empty(), false, false, false, false, 0, false, false);
        StationView read = roundTrip(bare);
        assertEquals(bare, read);
        assertTrue(read.pieces().isEmpty() && read.cell().isEmpty(), "pieces or a cell out of nothing: " + read);
        // una bandierina accesa per volta: ognuna torna nel suo campo
        for (int flag = 0; flag < 6; flag++) {
            StationView one = new StationView(Map.of(), Optional.empty(), flag == 0, flag == 1, flag == 2, flag == 3,
                    0, flag == 4, flag == 5);
            assertEquals(one, roundTrip(one), "the bare frame with only flag " + flag);
        }
    }

    @Test
    void fullViewStaysUnderAKilobyte() {
        Map<PieceSlot, StationView.PieceInfo> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            // l'id di livello piu' lungo e gli interi variabili piu' lunghi: la vista piu' grande che si possa scrivere
            pieces.put(slot, new StationView.PieceInfo(Tier.INDUSTRIAL, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    0xFFFFFF, 0xFFFFFF));
        }
        StationView largest = new StationView(pieces,
                Optional.of(new StationView.CellInfo(Integer.MAX_VALUE, Integer.MAX_VALUE)), true, true, true, true,
                StationView.SELECT_ALL, true, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        StationViewCodec.INSTANCE.write(buf, largest);
        assertTrue(buf.readableBytes() < 1_024, "the largest view takes " + buf.readableBytes() + " bytes");
        FriendlyByteBuf full = new FriendlyByteBuf(Unpooled.buffer());
        StationViewCodec.INSTANCE.write(full, fullView());
        assertTrue(full.readableBytes() < 1_024, "the full view takes " + full.readableBytes() + " bytes");
    }
}
