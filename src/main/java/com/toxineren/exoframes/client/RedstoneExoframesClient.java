package com.toxineren.exoframes.client;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.content.ModEntities;
import com.toxineren.exoframes.content.ModMenus;
import com.toxineren.exoframes.frame.ClientRules;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;

/**
 * Punto d'ingresso comune del client: i tre entrypoint client di platform/ chiamano solo {@link #init()}, dopo
 * l'init comune e solo sul client fisico. Un server dedicato non carica mai questa classe.
 */
public final class RedstoneExoframesClient {
    private RedstoneExoframesClient() {
    }

    public static void init() {
        Compat.frameState().registerClient();
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> ClientRules.reset()); // valori del server dimenticati
        ClientCompat.renderHooks().addPlayerLayer(SuitLayer::new);
        ClientCompat.renderHooks().addHudRenderer(EnergyHud::render);
        EntityRendererRegistry.register(ModEntities.EXOFRAME, ExoframeEntityRenderer::new);
        ExitKey.register();
        CellSwapKey.register();
        // La schermata della stazione vuole il tipo del menu, che su Forge e NeoForge qui non c'e' ancora: listen la
        // registra appena il tipo e' registrato, in tempo per l'evento delle schermate di NeoForge (design §5.20).
        ModMenus.STATION.listen(type -> MenuRegistry.registerScreenFactory(type, StationScreen::new));
    }
}
