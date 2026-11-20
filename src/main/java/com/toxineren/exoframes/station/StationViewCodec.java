package com.toxineren.exoframes.station;

import com.toxineren.exoframes.compat.PacketCodec;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.station.StationView;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Il codec della vista del telaio ({@link StationView}, design M4 §5.5), per i dati extra dell'apertura
 * ({@link StationOpening}) e per il pacchetto {@code station_view} ({@link StationViewPacket}). Usa solo interi
 * variabili, interi, booleani, byte e una stringa per l'id del livello, uguali sui quattro buffer (su Fabric 1.21.1 un
 * {@code FriendlyByteBuf} semplice, su NeoForge un {@code RegistryFriendlyByteBuf}); niente NBT ne' codec degli stack.
 * La vista piu' grande che si possa scrivere (sei pezzi con l'id di livello piu' lungo e gli interi variabili di 5
 * byte) occupa 188 byte, meno di un kilobyte, contro il limite di 32 600 byte dei dati extra di Forge e NeoForge.
 * <p>
 * Formato, nell'ordine:
 * <ol>
 * <li>un byte con un bit per slot montato: il bit {@code i} e' lo slot {@code PieceSlot.values()[i]};</li>
 * <li>per ogni slot montato, nell'ordine di {@link PieceSlot}: {@code writeUtf} dell'id del livello
 * ({@link Tier#id()}), {@code writeVarInt} della durata e della durata massima, {@code writeInt} del colore primario e
 * del secondario ({@code -1} se non dipinto);</li>
 * <li>{@code writeBoolean} della cella e, se c'e', {@code writeVarInt} della carica e della capacita';</li>
 * <li>un byte di bandierine: bit 0 {@code unlocked}, 1 {@code reinforced}, 2 {@code canToggleLock}, 3
 * {@code ownerOnly}, 4 {@code hasStowed}, 5 {@code free};</li>
 * <li>un byte con la scelta.</li>
 * </ol>
 * La lettura rifa' il record con il suo costruttore: un livello sconosciuto o un valore incoerente lanciano, perche'
 * la vista la scrive solo il server della stessa versione.
 */
public final class StationViewCodec implements PacketCodec<StationView> {
    /** L'unica istanza: il codec non ha stato. */
    public static final StationViewCodec INSTANCE = new StationViewCodec();

    private static final int UNLOCKED = 1;
    private static final int REINFORCED = 1 << 1;
    private static final int CAN_TOGGLE_LOCK = 1 << 2;
    private static final int OWNER_ONLY = 1 << 3;
    private static final int HAS_STOWED = 1 << 4;
    private static final int FREE = 1 << 5;

    private StationViewCodec() {
    }

    @Override
    public void write(FriendlyByteBuf buf, StationView view) {
        int mounted = 0;
        for (PieceSlot slot : view.pieces().keySet()) {
            mounted |= 1 << slot.ordinal();
        }
        buf.writeByte(mounted);
        for (PieceSlot slot : PieceSlot.values()) {
            StationView.PieceInfo piece = view.pieces().get(slot);
            if (piece != null) {
                buf.writeUtf(piece.tier().id());
                buf.writeVarInt(piece.durability());
                buf.writeVarInt(piece.maxDurability());
                buf.writeInt(piece.primary());
                buf.writeInt(piece.secondary());
            }
        }
        buf.writeBoolean(view.cell().isPresent());
        view.cell().ifPresent(cell -> {
            buf.writeVarInt(cell.stored());
            buf.writeVarInt(cell.capacity());
        });
        buf.writeByte(flag(view.unlocked(), UNLOCKED) | flag(view.reinforced(), REINFORCED)
                | flag(view.canToggleLock(), CAN_TOGGLE_LOCK) | flag(view.ownerOnly(), OWNER_ONLY)
                | flag(view.hasStowed(), HAS_STOWED) | flag(view.free(), FREE));
        buf.writeByte(view.selection());
    }

    @Override
    public StationView read(FriendlyByteBuf buf) {
        int mounted = buf.readUnsignedByte();
        Map<PieceSlot, StationView.PieceInfo> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            if ((mounted & (1 << slot.ordinal())) != 0) {
                Tier tier = Tier.byId(buf.readUtf());
                int durability = buf.readVarInt();
                int maxDurability = buf.readVarInt();
                int primary = buf.readInt();
                int secondary = buf.readInt();
                pieces.put(slot, new StationView.PieceInfo(tier, durability, maxDurability, primary, secondary));
            }
        }
        Optional<StationView.CellInfo> cell = Optional.empty();
        if (buf.readBoolean()) {
            int stored = buf.readVarInt();
            cell = Optional.of(new StationView.CellInfo(stored, buf.readVarInt()));
        }
        int flags = buf.readUnsignedByte();
        int selection = buf.readUnsignedByte();
        return new StationView(pieces, cell, (flags & UNLOCKED) != 0, (flags & REINFORCED) != 0,
                (flags & CAN_TOGGLE_LOCK) != 0, (flags & OWNER_ONLY) != 0, selection, (flags & HAS_STOWED) != 0,
                (flags & FREE) != 0);
    }

    private static int flag(boolean value, int bit) {
        return value ? bit : 0;
    }
}
