package com.toxineren.exoframes.client;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.content.ModEntities;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

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
        // Segnaposto finche' non arriva il renderer del telaio piazzato: un tipo di entita' senza renderer fa
        // crollare il client appena una sua entita' entra in vista.
        EntityRendererRegistry.register(ModEntities.EXOFRAME, NoopRenderer::new);
    }
}
