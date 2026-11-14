package com.toxineren.exoframes.client;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.content.ModEntities;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

/**
 * Punto d'ingresso comune del client: i tre entrypoint client di platform/ chiamano solo {@link #init()}, dopo
 * l'init comune e solo sul client fisico. Un server dedicato non carica mai questa classe.
 */
public final class RedstoneExoframesClient {
    private RedstoneExoframesClient() {
    }

    public static void init() {
        Compat.frameState().registerClient();
        ClientCompat.renderHooks().addPlayerLayer(SuitLayer::new);
        EntityRendererRegistry.register(ModEntities.EXOFRAME, ExoframeEntityRenderer::new);
        ExitKey.register();
    }
}
