package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.core.frame.FrameState;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Implementazione di {@link FrameStateAccess}: il server tiene gli stati in {@link FrameStateSavedData}, il client
 * la copia ricevuta in {@link FrameStateClientCache}; il pacchetto e' {@link FrameStateSyncPacket}.
 * <p>
 * Ogni scrittura ({@link #update}, {@link #mutate}, {@link #clear}) segna il dato come da salvare (lo fa il dato
 * stesso, a ogni sua operazione; {@link #mutate} non come cambio strutturale, vedi
 * {@link FrameStateSavedData#structuralUnsaved()}); {@link #update} e
 * {@link #clear} risincronizzano anche il giocatore, {@link #mutate} no. Nessuna scrittura toglie le voci grezze non
 * lette del giocatore: restano nel file, identiche (vedi {@link FrameStateSavedData}). Se il dato salvato e' in sola
 * lettura (schema piu' recente), le tre scritture non fanno nulla: nessuna modifica, nessun setDirty, nessun sync, un
 * solo ERROR nel log; {@link #get} resta vuoto.
 * <p>
 * Le operazioni di registro ({@link #place}, {@link #enter}, {@link #exit}, i piazzati, l'armatura stivata) passano
 * dal record di {@link FrameStateSavedData} e seguono la stessa regola: ogni scrittura segna il dato come da salvare,
 * e in sola lettura non cambia nulla e da' falso, vuoto o null con lo stesso ERROR una volta. Il posto di un piazzato
 * sta nel dato come id testuale della dimensione ({@link PlacedSpot}) e diventa qui una {@code ResourceKey}.
 * L'armatura stivata passa fra stack e tag con {@link StackTags}, con il provider dei registri del server; il
 * riassunto visivo come tag con {@link SuitSummaryTags}.
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
        try {
            // il dato si segna da salvare anche se la modifica lancia a meta': disco e client allineati alla memoria
            data.updateWorn(p.getUUID(), () -> new FrameState(RedstoneExoframes.config()), change);
        } finally {
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
        // anche se la modifica lancia a meta' il dato si segna da salvare (nessuna sincronizzazione)
        return data.mutateWorn(p.getUUID(), change);
    }

    @Override
    public void clear(ServerPlayer p) {
        FrameStateSavedData data = FrameStateSavedData.get(p.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return;
        }
        data.removeWorn(p.getUUID());
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
    public UUID place(ServerLevel level, FrameState state, Vec3 position, float yaw) {
        Objects.requireNonNull(state, "state must not be null");
        PlacedSpot spot = spotOf(level, position, yaw);
        FrameStateSavedData data = FrameStateSavedData.get(level.getServer());
        if (data.readOnly()) {
            data.logRefusedChange();
            return null;
        }
        return data.place(new FrameRecord(UUID.randomUUID(), state), spot);
    }

    @Override
    public Optional<PlacedFrame> placed(MinecraftServer server, UUID frameId) {
        Objects.requireNonNull(frameId, "frameId must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(server);
        FrameRecord record = data.placed(frameId);
        return record == null ? Optional.empty()
                : Optional.of(view(record, data.spot(frameId), server.registryAccess()));
    }

    @Override
    public List<PlacedFrame> placedIn(ServerLevel level) {
        FrameStateSavedData data = FrameStateSavedData.get(level.getServer());
        HolderLookup.Provider registries = level.getServer().registryAccess();
        List<PlacedFrame> frames = new ArrayList<>();
        for (UUID frameId : data.placedIn(dimensionId(level))) {
            frames.add(view(data.placed(frameId), data.spot(frameId), registries));
        }
        return frames;
    }

    @Override
    public boolean updatePlaced(MinecraftServer server, UUID frameId, Consumer<FrameState> change) {
        Objects.requireNonNull(frameId, "frameId must not be null");
        Objects.requireNonNull(change, "change must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return false;
        }
        return data.updatePlaced(frameId, change);
    }

    @Override
    public boolean removePlaced(MinecraftServer server, UUID frameId) {
        Objects.requireNonNull(frameId, "frameId must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return false;
        }
        return data.removePlaced(frameId);
    }

    @Override
    public boolean enter(ServerPlayer player, UUID frameId) {
        Objects.requireNonNull(frameId, "frameId must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(player.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return false;
        }
        if (!data.enter(player.getUUID(), frameId)) {
            return false;
        }
        syncTo(player);
        return true;
    }

    @Override
    public Optional<UUID> exit(ServerPlayer player, ServerLevel level, Vec3 position, float yaw) {
        PlacedSpot spot = spotOf(level, position, yaw);
        FrameStateSavedData data = FrameStateSavedData.get(player.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return Optional.empty();
        }
        Optional<UUID> frameId = data.exit(player.getUUID(), spot);
        if (frameId.isPresent()) {
            syncTo(player);
        }
        return frameId;
    }

    @Override
    public Optional<UUID> wornFrameId(ServerPlayer player) {
        return Optional.ofNullable(FrameStateSavedData.get(player.server).worn(player.getUUID()))
                .map(FrameRecord::frameId);
    }

    @Override
    public void stow(ServerPlayer player, List<StowedStack> armor) {
        Objects.requireNonNull(armor, "armor must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(player.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return;
        }
        // prima tutte le voci, poi la lista: uno stack che non si scrive non lascia la stiva a meta'
        HolderLookup.Provider registries = player.server.registryAccess();
        List<CompoundTag> entries = new ArrayList<>();
        for (StowedStack stowed : armor) {
            Objects.requireNonNull(stowed, "stowed stack must not be null");
            if (!stowed.stack().isEmpty()) {
                entries.add(StackTags.write(registries, stowed.slot(), stowed.stack()));
            }
        }
        data.addStowed(player.getUUID(), entries);
    }

    @Override
    public List<StowedStack> takeStowed(ServerPlayer player) {
        FrameStateSavedData data = FrameStateSavedData.get(player.server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return List.of();
        }
        HolderLookup.Provider registries = player.server.registryAccess();
        return data.takeStowedOfWorn(player.getUUID(), (frameId, tag) -> StackTags.read(registries, tag, frameId));
    }

    @Override
    public List<StowedStack> takeStowedOfPlaced(MinecraftServer server, UUID frameId) {
        Objects.requireNonNull(frameId, "frameId must not be null");
        FrameStateSavedData data = FrameStateSavedData.get(server);
        if (data.readOnly()) {
            data.logRefusedChange();
            return List.of();
        }
        HolderLookup.Provider registries = server.registryAccess();
        return data.takeStowedOfPlaced(frameId, (id, tag) -> StackTags.read(registries, tag, id));
    }

    @Override
    public CompoundTag writeSummaryTag(SuitSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        CompoundTag tag = new CompoundTag();
        SuitSummaryTags.write(tag, summary);
        return tag;
    }

    @Override
    public Optional<SuitSummary> readSummaryTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag must not be null");
        return SuitSummaryTags.read(tag);
    }

    /**
     * Il telaio piazzato come lo vede il resto del gioco; {@code hasStowed} e' vero solo se almeno una voce stivata
     * si legge (una voce illeggibile non si puo' restituire, e non deve bloccare lo smontaggio).
     */
    private static PlacedFrame view(FrameRecord record, PlacedSpot spot, HolderLookup.Provider registries) {
        // l'id della dimensione e' gia' stato validato in lettura e scritto da un livello vero
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Objects.requireNonNull(ResourceLocation.tryParse(spot.dimension()), "invalid dimension id"));
        boolean hasReadable = false;
        for (CompoundTag entry : record.stowed()) {
            if (StackTags.read(registries, entry, record.frameId()).isPresent()) {
                hasReadable = true;
                break;
            }
        }
        return new PlacedFrame(record.frameId(), record.state(), dimension, new Vec3(spot.x(), spot.y(), spot.z()),
                spot.yaw(), hasReadable);
    }

    private static PlacedSpot spotOf(ServerLevel level, Vec3 position, float yaw) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(position, "position must not be null");
        return new PlacedSpot(dimensionId(level), position.x, position.y, position.z, yaw);
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    @Override
    public void registerCommon() {
        Compat.net().registerS2C(FrameStateSyncPacket.ID, FrameStateSyncPacket.class, FrameStateSyncPacket.CODEC,
                (msg, ctx) -> FrameStateClientCache.accept(msg));
        PlayerEvent.PLAYER_JOIN.register(this::syncTo);
        // registro e giocatori sul disco insieme quando un giocatore esce (FrameStateSavedData.saveWithPlayersIfNeeded)
        PlayerEvent.PLAYER_QUIT.register(player -> FrameStateSavedData.saveWithPlayersIfNeeded(player.server));
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
