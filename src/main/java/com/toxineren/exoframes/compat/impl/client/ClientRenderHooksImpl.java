package com.toxineren.exoframes.compat.impl.client;

import com.toxineren.exoframes.compat.client.ClientRenderHooks;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if fabric {
/*import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*///?} else if forge {
/*import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
*///?} else {
import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//?}

/**
 * Implementazione di {@link ClientRenderHooks}. Architectury non ha un'API per aggiungere layer a renderer esistenti
 * (ricognizione M3, par. 5), quindi si passa dal loader:
 * <ul>
 * <li>Fabric (rendering-v1 3.0.10 e 5.2.1, uguali): {@code LivingEntityFeatureRendererRegistrationCallback}, che il
 * gioco chiama una volta per ogni renderer di entita' viva, compreso ciascun renderer di skin del giocatore;</li>
 * <li>Forge 47.4: {@code EntityRenderersEvent.AddLayers} sul bus del mod, skin per nome ({@code "default"},
 * {@code "slim"}) con {@code getPlayerSkin(String)};</li>
 * <li>NeoForge 21.1: lo stesso evento, skin per {@code PlayerSkin.Model} con {@code getSkin(PlayerSkin.Model)}.</li>
 * </ul>
 * Su Forge e NeoForge {@code LivingEntityRenderer.addLayer} e' pubblico (su vanilla e' protetto). Il bus del mod si
 * prende da Architectury, gia' disponibile quando l'entrypoint chiama questo metodo dal costruttore della mod.
 */
public final class ClientRenderHooksImpl implements ClientRenderHooks {

    @Override
    public void addPlayerLayer(Function<RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
            RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory) {
        Objects.requireNonNull(factory, "factory must not be null");
        //? if fabric {
        /*LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(factory.apply(player));
            }
        });
        *///?} else if forge {
        /*EventBuses.getModEventBus(RedstoneExoframes.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("mod event bus not registered: " + RedstoneExoframes.MOD_ID))
                .addListener(EventPriority.NORMAL, false, EntityRenderersEvent.AddLayers.class, event -> {
                    for (String skin : event.getSkins()) {
                        addTo(event.getPlayerSkin(skin), factory);
                    }
                });
        *///?} else {
        EventBusesHooks.getModEventBus(RedstoneExoframes.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("mod event bus not available: " + RedstoneExoframes.MOD_ID))
                .addListener(EntityRenderersEvent.AddLayers.class, event -> {
                    for (PlayerSkin.Model skin : event.getSkins()) {
                        addTo(event.getSkin(skin), factory);
                    }
                });
        //?}
    }

    //? if !fabric {
    // Aggiunge il layer al renderer di una skin; un renderer che non e' un PlayerRenderer (di un'altra mod) si salta.
    // Commento di riga e non javadoc: su Fabric questo ramo e' inattivo e sta dentro un commento a blocco.
    private static void addTo(EntityRenderer<? extends Player> renderer,
            Function<RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
                    RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory) {
        if (renderer instanceof PlayerRenderer player) {
            player.addLayer(factory.apply(player));
        }
    }
    //?}
}
