package com.toxineren.exoframes.compat.client;

import com.toxineren.exoframes.compat.impl.client.ClientRenderHooksImpl;
import com.toxineren.exoframes.compat.impl.client.SuitRendererImpl;

/**
 * Punto di accesso alle cuciture solo client, con lo stesso idioma del holder di
 * {@link com.toxineren.exoframes.compat.Compat}: ogni accessor restituisce il singleton dell'unica implementazione in
 * compat/impl/client, creato alla prima chiamata. Un server dedicato non deve mai caricare questa classe: la
 * referenziano solo i package client e gli entrypoint client di platform/ (regola 7 di tools/check_sources.sh).
 */
public final class ClientCompat {
    private ClientCompat() {
    }

    /**
     * Il renderer della tuta. La prima chiamata costruisce il renderer GeckoLib, che prende i modelli delle entita'
     * da Minecraft: va fatta sul thread di rendering, dopo il caricamento delle risorse (per esempio al primo disegno).
     */
    public static SuitRenderer suitRenderer() {
        return SuitRendererHolder.INSTANCE;
    }

    /** Gli agganci ai renderer vanilla per loader (layer sul giocatore); da usare dall'init client. */
    public static ClientRenderHooks renderHooks() {
        return RenderHooksHolder.INSTANCE;
    }

    private static final class SuitRendererHolder {
        static final SuitRenderer INSTANCE = new SuitRendererImpl();
    }

    private static final class RenderHooksHolder {
        static final ClientRenderHooks INSTANCE = new ClientRenderHooksImpl();
    }
}
