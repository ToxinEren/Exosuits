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
 * Ogni scrittura ({@link #update}, {@link #mutate}, {@link #clear}) segna il dato come da salvare; {@link #update} e
 * {@link #clear} risincronizzano anche il giocatore, {@link #mutate} no. {@link #update} e {@link #clear} tolgono le
 * voci grezze non lette del giocatore (vedi {@link FrameStateSavedData}). Se il dato salvato e' in sola lettura
 * (schema piu' recente), le tre scritture non fanno nulla: nessuna modifica, nessun setDirty, nessun sync, un solo
 * ERROR nel log; {@link #get} resta vuoto.
 * <p>
 * Il riferimento a {@link ClientPlayerEvent} sta solo nel corpo di {@link #registerClient()}, chiamato solo dagli
 * entrypoint client: un server dedicato non carica mai classi del client.
 */
public final class FrameStateAccessImpl implements FrameStateAccess {
    @Override
    public Optional<FrameState> get(Player p) {
        if (!p.level().isClientSide()) {
            // sul server logico un Player che non e' un ServerPlayer non ha stato: mai la copia del client
            return p instanceof ServerPlayer serverPlayer
                    ? Optional.ofNullable(FrameStateSavedData.get(serverPlayer.server).worn(p.getUUID()))
                            .map(FrameRecord::state)
                    : Optional.empty();
        }
        return clientView(p);
    }

    @Override
    public void update(ServerPlayer p, Consumer<FrameState> change) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return;
        }
        FrameState state = data.getOrCreateWorn(p.getUUID(), () -> new FrameState(RedstoneExoframes.config()))
                .state();
        data.dropUnread(p.getUUID());
        try {
            change.accept(state);
        } finally {
            // anche se la modifica lancia a meta': disco e client restano allineati alla memoria
            data.setDirty();
            syncTo(p);
        }
    }

    @Override
    public boolean mutate(ServerPlayer p, Consumer<FrameState> change) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return false;
        }
        FrameRecord record = data.worn(p.getUUID());
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            // anche se la modifica lancia a meta': il disco resta allineato alla memoria (nessuna sincronizzazione)
            data.setDirty();
        }
        return true;
    }

    @Override
    public void clear(ServerPlayer p) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return;
        }
        data.removeWorn(p.getUUID());
        data.dropUnread(p.getUUID());
        data.setDirty();
        syncTo(p);
    }

    @Override
    public Optional<FrameState> clientView(Player clientPlayer) {
        return Optional.ofNullable(FrameStateClientCache.view(clientPlayer.getUUID()));
    }

    @Override
    public void syncTo(ServerPlayer p) {
        FrameRecord record = FrameStateSavedData.get(p.server).worn(p.getUUID());
        Optional<CompoundTag> tag = Optional.ofNullable(record).map(worn -> FrameStateCodec.write(worn.state()));
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
