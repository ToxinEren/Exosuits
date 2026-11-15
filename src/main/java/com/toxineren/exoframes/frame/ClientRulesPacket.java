package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.PacketCodec;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Pacchetto S2C con i valori della config del server che il client conta da se' (design §12.4): la capacita' della
 * cella e i secondi dei due tasti da tenere. Il server lo manda a chi entra ({@link ClientRules#register()}); il client
 * li controlla e li tiene in {@link ClientRules#accept}. Il codec non filtra nulla: i limiti li controlla
 * {@code accept}.
 * <p>
 * Formato: tre interi a lunghezza variabile, nell'ordine {@code cellCapacity}, {@code cellSwapSeconds},
 * {@code exitHoldSeconds}.
 */
public record ClientRulesPacket(int cellCapacity, int cellSwapSeconds, int exitHoldSeconds) {
    /** L'id del canale. */
    public static final ResourceLocation ID = Ids.of("client_rules");

    /** Il codec: tre {@code VarInt}, nell'ordine dei campi. */
    public static final PacketCodec<ClientRulesPacket> CODEC = new PacketCodec<>() {
        @Override
        public void write(FriendlyByteBuf buf, ClientRulesPacket msg) {
            buf.writeVarInt(msg.cellCapacity());
            buf.writeVarInt(msg.cellSwapSeconds());
            buf.writeVarInt(msg.exitHoldSeconds());
        }

        @Override
        public ClientRulesPacket read(FriendlyByteBuf buf) {
            int cellCapacity = buf.readVarInt();
            int cellSwapSeconds = buf.readVarInt();
            int exitHoldSeconds = buf.readVarInt();
            return new ClientRulesPacket(cellCapacity, cellSwapSeconds, exitHoldSeconds);
        }
    };

    /**
     * I valori da mandare, presi dalla config data: {@code energy.cellCapacity}, {@code energy.cellSwapSeconds},
     * {@code frame.exitHoldSeconds}.
     */
    public static ClientRulesPacket of(ExoframesConfig config) {
        return new ClientRulesPacket(config.energy.cellCapacity, config.energy.cellSwapSeconds,
                config.frame.exitHoldSeconds);
    }
}
