package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.NbtKeys;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
//? if >=1.21 {
import net.minecraft.core.Holder;
//?}
//? if <1.21 {
/*import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
*///?}
//? if fabric {
/*import net.minecraft.gametest.framework.GameTestRegistry;
*///?}
//? if forge {
/*import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.ForgeGameTestHooks;
*///?} else if neoforge {
import com.toxineren.exoframes.RedstoneExoframes;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
//?}

/**
 * Implementazione di {@link GameTests}. Registrazione, solo se il server di test e' attivo (mai in produzione): su
 * Forge e NeoForge un ascoltatore di {@code RegisterGameTestsEvent} sul bus del mod (proprieta'
 * forge/neoforge.gameTestServer); su Fabric {@code GameTestRegistry.register} diretto (proprieta'
 * fabric-api.gametest), al posto dell'entrypoint {@code fabric-gametest}, che il modulo di Fabric registra a ogni
 * avvio e che renderebbe i test eseguibili con {@code /test} su un client normale. Il modulo di Fabric usa la mod
 * dell'entrypoint solo per i metodi {@code @GameTest}; i nostri test nascono da un {@code @GameTestGenerator}.
 * Giocatore finto: su 1.20.1 la connessione e' agganciata a un {@code EmbeddedChannel} come fa gia' 1.21.1 (senza canale Forge lancia un NPE in
 * {@code NetworkFilters.injectIfNecessary} e Fabric lascia i pacchetti in coda senza scriverli).
 */
public final class GameTestsImpl implements GameTests {
    @Override
    public void register(Class<?> testClass) {
        //? if fabric {
        /*if (System.getProperty("fabric-api.gametest") != null) {
            GameTestRegistry.register(testClass);
        }
        *///?} else if forge {
        /*if (ForgeGameTestHooks.isGametestServer()) {
            EventBuses.getModEventBus(RedstoneExoframes.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("mod event bus not registered with Architectury"))
                    .addListener((RegisterGameTestsEvent event) -> event.register(testClass));
        }
        *///?} else {
        if (GameTestHooks.isGametestServer()) {
            ModList.get().getModContainerById(RedstoneExoframes.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("mod container not found: " + RedstoneExoframes.MOD_ID))
                    .getEventBus()
                    .addListener(RegisterGameTestsEvent.class, event -> event.register(testClass));
        }
        //?}
    }

    @Override
    @SuppressWarnings("removal") // su 1.21.1 makeMockServerPlayerInLevel e' deprecato per la rimozione, ma c'e' ancora
    public ServerPlayer mockServerPlayer(GameTestHelper helper) {
        //? if <1.21 {
        /*ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection); // channelActive imposta il canale della connessione
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
        *///?} else {
        return helper.makeMockServerPlayerInLevel();
        //?}
    }

    @Override
    public void writeRawItemData(ItemStack stack, CompoundTag root) {
        ItemPayload.write(stack, root.copy());
    }

    @Override
    public boolean addRawStowed(MinecraftServer server, UUID frameId, CompoundTag entry) {
        FrameStateSavedData data = FrameStateSavedData.get(server);
        FrameRecord record = data.readOnly() ? null : data.placed(frameId);
        if (record == null) {
            return false;
        }
        record.stowed().add(entry.copy());
        data.setDirty();
        return true;
    }

    @Override
    public List<CompoundTag> orphanedStowed(MinecraftServer server, UUID frameId) {
        List<CompoundTag> entries = new ArrayList<>();
        for (CompoundTag group : FrameStateSavedData.get(server).orphaned()) {
            if (SchemaIds.readUuid(group, NbtKeys.FRAME).filter(frameId::equals).isPresent()) {
                ListTag list = group.getList(NbtKeys.ENTRIES, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    entries.add(list.getCompound(i).copy());
                }
            }
        }
        return entries;
    }

    @Override
    public int forgetOrphaned(MinecraftServer server, UUID frameId) {
        return FrameStateSavedData.get(server).forgetOrphaned(frameId);
    }

    @Override
    public Path saveRegistry(MinecraftServer server) {
        FrameStateSavedData.get(server);
        server.overworld().getDataStorage().save();
        return FrameStateSavedData.dataFolder(server).resolve(FrameStateSavedData.NAME + ".dat");
    }

    @Override
    public CompoundTag registryContents(MinecraftServer server) {
        return FrameStateSavedData.get(server).write(new CompoundTag());
    }

    @Override
    public Optional<CompoundTag> readRegistry(MinecraftServer server, Path dataFolder) {
        //? if >=1.21 {
        DimensionDataStorage storage = new DimensionDataStorage(dataFolder.toFile(), server.getFixerUpper(),
                server.registryAccess());
        //?} else {
        /*DimensionDataStorage storage = new DimensionDataStorage(dataFolder.toFile(), server.getFixerUpper());*/
        //?}
        FrameStateSavedData data = FrameStateSavedData.get(storage, dataFolder);
        return data.readOnly() ? Optional.empty() : Optional.of(data.write(new CompoundTag()));
    }

    @Override
    public boolean registryChangeUnsaved(MinecraftServer server) {
        return FrameStateSavedData.get(server).structuralUnsaved();
    }

    @Override
    public boolean portalsAccept(Entity entity, ServerLevel destination) {
        //? if >=1.21 {
        return entity.canUsePortal(false) || entity.canChangeDimensions(entity.level(), destination);
        //?} else {
        /*return entity.canChangeDimensions() || TheEndGatewayBlockEntity.canEntityTeleport(entity);
        *///?}
    }

    /**
     * Un ascoltatore di eventi di gioco registrato nella sezione del blocco {@code center}, consegna immediata (quella
     * predefinita): {@code ServerLevel.gameEvent} lo chiama nello stesso momento in cui l'evento nasce.
     */
    @Override
    public EquipEventCounter countEquipEvents(ServerLevel level, BlockPos center) {
        GameEventListenerRegistry registry = level.getChunkAt(center)
                .getListenerRegistry(SectionPos.blockToSectionCoord(center.getY()));
        EquipListener listener = new EquipListener(new BlockPositionSource(center));
        registry.register(listener);
        return new EquipEventCounter() {
            @Override
            public int count() {
                return listener.count;
            }

            @Override
            public void close() {
                registry.unregister(listener);
            }
        };
    }

    /** Conta gli eventi {@code EQUIP} entro {@value #RADIUS} blocchi dalla sua posizione. */
    private static final class EquipListener implements GameEventListener {
        private static final int RADIUS = 16;
        private final PositionSource source;
        private int count;

        EquipListener(PositionSource source) {
            this.source = source;
        }

        @Override
        public PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return RADIUS;
        }

        //? if >=1.21 {
        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context,
                Vec3 position) {
            if (event.is(GameEvent.EQUIP.key())) {
                count++;
            }
            return false;
        }
        //?} else {
        /*@Override
        public boolean handleGameEvent(ServerLevel level, GameEvent event, GameEvent.Context context, Vec3 position) {
            if (event == GameEvent.EQUIP) {
                count++;
            }
            return false;
        }
        *///?}
    }
}
