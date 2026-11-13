package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.GameTests;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
/*import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
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
}
