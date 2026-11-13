package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
//? if <1.21 {
/*import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
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
 * Implementazione di {@link GameTests}. Registrazione: su Forge e NeoForge un ascoltatore di
 * {@code RegisterGameTestsEvent} sul bus del mod, aggiunto solo se il server di test e' attivo (proprieta'
 * forge/neoforge.gameTestServer, mai in produzione); su Fabric non serve nulla, perche' la classe e' l'entrypoint
 * {@code fabric-gametest} di fabric.mod.json. Giocatore finto: su 1.20.1 la connessione e' agganciata a un
 * {@code EmbeddedChannel} come fa gia' 1.21.1 (senza canale Forge lancia un NPE in
 * {@code NetworkFilters.injectIfNecessary} e Fabric lascia i pacchetti in coda senza scriverli).
 */
public final class GameTestsImpl implements GameTests {
    @Override
    public void register(Class<?> testClass) {
        //? if forge {
        /*if (ForgeGameTestHooks.isGametestServer()) {
            EventBuses.getModEventBus(RedstoneExoframes.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("mod event bus not registered with Architectury"))
                    .addListener((RegisterGameTestsEvent event) -> event.register(testClass));
        }
        *///?} else if neoforge {
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
}
