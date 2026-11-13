package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.core.frame.FrameState;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Implementazione di {@link FrameStateAccess}: il server tiene gli stati in {@link FrameStateSavedData}, il client
 * la copia ricevuta in {@link FrameStateClientCache}; il pacchetto e' {@link FrameStateSyncPacket}.
 * <p>
 * Ogni mutazione ({@link #update}, {@link #clear}) segna il dato come da salvare e risincronizza il giocatore. Il
 * riferimento a {@link ClientPlayerEvent} sta solo nel corpo di {@link #registerClient()}, chiamato solo dagli
 * entrypoint client: un server dedicato non carica mai classi del client.
 */
public final class FrameStateAccessImpl implements FrameStateAccess {
    @Override
    public Optional<FrameState> get(Player p) {
        if (!p.level().isClientSide() && p instanceof ServerPlayer serverPlayer) {
            return Optional.ofNullable(FrameStateSavedData.get(serverPlayer.server).frames().get(p.getUUID()));
        }
        return clientView(p);
    }

    @Override
    public void update(ServerPlayer p, Consumer<FrameState> change) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        FrameState state = data.frames().computeIfAbsent(p.getUUID(),
                uuid -> new FrameState(RedstoneExoframes.config()));
        change.accept(state);
        data.setDirty();
        syncTo(p);
    }

    @Override
    public void clear(ServerPlayer p) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        data.frames().remove(p.getUUID());
        data.setDirty();
        syncTo(p);
    }

    @Override
    public Optional<FrameState> clientView(Player clientPlayer) {
        return Optional.ofNullable(FrameStateClientCache.view(clientPlayer.getUUID()));
    }

    @Override
    public void syncTo(ServerPlayer p) {
        FrameState state = FrameStateSavedData.get(p.server).frames().get(p.getUUID());
        Optional<CompoundTag> tag = Optional.ofNullable(state).map(FrameStateCodec::write);
        Compat.net().sendToPlayer(p, FrameStateSyncPacket.ID, new FrameStateSyncPacket(p.getUUID(), tag));
    }

    @Override
    public void registerCommon() {
        Compat.net().registerS2C(FrameStateSyncPacket.ID, FrameStateSyncPacket.class, FrameStateSyncPacket.CODEC,
                (msg, ctx) -> FrameStateClientCache.accept(msg));
        PlayerEvent.PLAYER_JOIN.register(this::syncTo);
        //? if >=1.21 {
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> syncTo(player));
        //?} else {
        /*PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> syncTo(player));*/
        //?}
    }

    @Override
    public void registerClient() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> FrameStateClientCache.clear());
    }
}
