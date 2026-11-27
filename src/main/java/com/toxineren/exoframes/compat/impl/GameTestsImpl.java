package com.toxineren.exoframes.compat.impl;

import com.mojang.authlib.GameProfile;
import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.NbtKeys;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
//? if >=1.21 {
import net.minecraft.core.Holder;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.crafting.CraftingInput;
//?}
//? if <1.21 {
/*import java.nio.charset.StandardCharsets;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
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
 * Giocatore finto: costruito qui su entrambe le versioni, come lo costruisce {@code makeMockServerPlayerInLevel} di
 * 1.21.1 (connessione agganciata a un {@code EmbeddedChannel}: senza canale Forge 1.20.1 lancia un NPE in
 * {@code NetworkFilters.injectIfNecessary} e Fabric lascia i pacchetti in coda senza scriverli), che come quello
 * vanilla dice sempre {@code isCreative()} (solo quello: le capacita' seguono la modalita' di gioco del mondo dei test,
 * che su 1.20.1-forge non e' la creativa, e i test la impostano da se'), ma con {@code isSpectator()} quello della
 * modalita' di gioco: quello vanilla dice sempre falso, e i test dello spettatore non si potrebbero fare.
 */
public final class GameTestsImpl implements GameTests {
    /**
     * I tick di protezione d'ingresso di un giocatore nuovo: il costruttore di {@code ServerPlayer} mette il campo
     * {@code spawnInvulnerableTime} a 60 e {@code ServerPlayer.tick()} lo scala di uno; finche' e' positivo i colpi non
     * contano.
     */
    private static final int SPAWN_PROTECTION_TICKS = 60;

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
    public ServerPlayer mockServerPlayer(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
        //? if >=1.21 {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        MockPlayer player = new MockPlayer(helper.getLevel().getServer(), helper.getLevel(), profile,
                cookie.clientInformation());
        //?} else {
        /*MockPlayer player = new MockPlayer(helper.getLevel().getServer(), helper.getLevel(), profile);*/
        //?}
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection); // channelActive imposta il canale della connessione
        //? if >=1.21 {
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        //?} else {
        /*helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);*/
        //?}
        return player;
    }

    @Override
    public ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i < SPAWN_PROTECTION_TICKS; i++) {
            player.tick();
        }
        return player;
    }

    @Override
    public List<Component> messagesTo(ServerPlayer player) {
        return player instanceof MockPlayer mock ? List.copyOf(mock.messages) : List.of();
    }

    /**
     * Il giocatore finto di {@link #mockServerPlayer}: dice sempre {@code isCreative()} come quello vanilla (le
     * capacita' seguono la modalita' di gioco, che i test impostano da se'), e' spettatore secondo la sua modalita' di
     * gioco, e ricorda i messaggi di sistema che riceve ({@link #messagesTo}).
     */
    private static final class MockPlayer extends ServerPlayer {
        private final List<Component> messages = new ArrayList<>();

        //? if >=1.21 {
        MockPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
            super(server, level, profile, information);
        }
        //?} else {
        /*MockPlayer(MinecraftServer server, ServerLevel level, GameProfile profile) {
            super(server, level, profile);
        }*/
        //?}

        @Override
        public boolean isCreative() {
            return true;
        }

        @Override
        public void sendSystemMessage(Component message, boolean overlay) {
            messages.add(message);
            super.sendSystemMessage(message, overlay);
        }
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
    public Path registryFolder(MinecraftServer server) {
        return FrameStateSavedData.dataFolder(server);
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
        //? if >=1.21 && neoforge {
        // NeoForge, dopo un file letto, mette sul suo lavoratore di I/O la pulizia dei file temporanei della cartella:
        // si aspetta che finisca, cosi' chi chiama puo' cancellare subito una cartella temporanea senza che la pulizia
        // la cerchi dopo e scriva un ERROR.
        net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
        //?}
        return data.readOnly() ? Optional.empty() : Optional.of(data.write(new CompoundTag()));
    }

    @Override
    public boolean registryChangeUnsaved(MinecraftServer server) {
        return FrameStateSavedData.get(server).structuralUnsaved();
    }

    @Override
    public Refusal refusePlacedUpdates(MinecraftServer server) {
        FrameStateSavedData data = FrameStateSavedData.get(server);
        data.placedUpdateRefusals++;
        return new Refusal() {
            private boolean closed;

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    data.placedUpdateRefusals--;
                }
            }
        };
    }

    @Override
    public int syncCount(ServerPlayer player) {
        return SyncCounter.count(player.getUUID());
    }

    @Override
    public Set<ResourceLocation> attributesToSync(ServerPlayer player) {
        //? if >=1.21 {
        Set<AttributeInstance> pending = player.getAttributes().getAttributesToSync();
        //?} else {
        /*Set<AttributeInstance> pending = player.getAttributes().getDirtyAttributes();*/
        //?}
        Set<ResourceLocation> ids = new HashSet<>();
        for (AttributeInstance inst : pending) {
            //? if >=1.21 {
            ids.add(inst.getAttribute().unwrapKey().orElseThrow().location());
            //?} else {
            /*ids.add(BuiltInRegistries.ATTRIBUTE.getKey(inst.getAttribute()));*/
            //?}
        }
        pending.clear(); // l'insieme vivo del gioco: quello che il prossimo invio manderebbe
        return ids;
    }

    @Override
    public boolean stepIsAttribute() {
        //? if >=1.21 {
        return true;
        //?} else {
        /*return false;
        *///?}
    }

    @Override
    public OptionalDouble frameModifier(ServerPlayer player, String id) {
        //? if >=1.21 {
        ResourceLocation key = Ids.of(id);
        for (Holder<Attribute> attribute : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance inst = player.getAttribute(attribute);
            AttributeModifier modifier = inst == null ? null : inst.getModifier(key);
            if (modifier != null) {
                return OptionalDouble.of(modifier.amount());
            }
        }
        //?} else {
        /*// l'UUID come lo deriva FrameAttributesImpl, senza la sua key(): lancerebbe per un id fuori dai quattro
        UUID key = UUID.nameUUIDFromBytes(Ids.of(id).toString().getBytes(StandardCharsets.UTF_8));
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            AttributeInstance inst = player.getAttribute(attribute);
            AttributeModifier modifier = inst == null ? null : inst.getModifier(key);
            if (modifier != null) {
                return OptionalDouble.of(modifier.getAmount());
            }
        }
        *///?}
        return OptionalDouble.empty();
    }

    @Override
    public ServerPlayer respawn(ServerPlayer player, boolean keepEverything) {
        //? if >=1.21 {
        return player.server.getPlayerList().respawn(player, keepEverything,
                keepEverything ? Entity.RemovalReason.CHANGED_DIMENSION : Entity.RemovalReason.KILLED);
        //?} else {
        /*return player.server.getPlayerList().respawn(player, keepEverything);
        *///?}
    }

    @Override
    public boolean portalsAccept(Entity entity, ServerLevel destination) {
        //? if >=1.21 {
        return entity.canUsePortal(false) || entity.canChangeDimensions(entity.level(), destination);
        //?} else {
        /*return entity.canChangeDimensions() || TheEndGatewayBlockEntity.canEntityTeleport(entity);
        *///?}
    }

    @Override
    public CompoundTag saveBlockEntity(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            throw new IllegalStateException("no block entity at " + pos);
        }
        //? if >=1.21 {
        return entity.saveWithFullMetadata(level.registryAccess());
        //?} else {
        /*return entity.saveWithFullMetadata();
        *///?}
    }

    @Override
    public BlockEntity loadBlockEntity(ServerLevel level, BlockPos pos, CompoundTag saved) {
        //? if >=1.21 {
        BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        //?} else {
        /*BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved);*/
        //?}
        if (loaded == null) {
            throw new IllegalStateException("no block entity loaded at " + pos + " from " + saved);
        }
        level.setBlockEntity(loaded);
        return loaded;
    }

    @Override
    public Optional<ItemStack> recipeResult(MinecraftServer server, ResourceLocation id) {
        //? if >=1.21 {
        return server.getRecipeManager().byKey(id)
                .map(holder -> holder.value().getResultItem(server.registryAccess()).copy());
        //?} else {
        /*return server.getRecipeManager().byKey(id)
                .map(recipe -> recipe.getResultItem(server.registryAccess()).copy());
        *///?}
    }

    /**
     * Su 1.21.1 la griglia e' un {@code CraftingInput}; su 1.20.1 un {@code TransientCraftingContainer}, che vuole un
     * menu: uno anonimo senza slot e mai valido, che nessuno chiama perche' la griglia nasce gia' piena.
     */
    @Override
    public ItemStack craft(ServerLevel level, int width, int height, List<ItemStack> grid) {
        if (width < 1 || height < 1 || grid.size() != width * height) {
            throw new IllegalArgumentException("a " + width + "x" + height + " grid cannot hold " + grid.size()
                    + " stacks");
        }
        //? if >=1.21 {
        CraftingInput input = CraftingInput.of(width, height, grid);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        //?} else {
        /*AbstractContainerMenu menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
        NonNullList<ItemStack> items = NonNullList.create();
        items.addAll(grid);
        TransientCraftingContainer container = new TransientCraftingContainer(menu, width, height, items);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level)
                .map(recipe -> recipe.assemble(container, level.registryAccess())).orElse(ItemStack.EMPTY);
        *///?}
    }

    @Override
    public boolean hasAdvancement(MinecraftServer server, ResourceLocation id) {
        //? if >=1.21 {
        return server.getAdvancements().get(id) != null;
        //?} else {
        /*return server.getAdvancements().getAdvancement(id) != null;
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
