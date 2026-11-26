package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * La riga in chat che, al primo ingresso in un telaio di ogni sessione nel server, nomina i due tasti da tenere
 * premuti, uscita e cambio cella (design §12.13), e il tasto che nasconde l'HUD (design M4 §5.12). I tasti vanno come
 * {@code Component.keybind}, cosi' il client mostra quelli scelti dal giocatore. "Gia' mostrata" sta in un insieme di
 * UUID svuotato all'uscita dal server ({@code PlayerEvent.PLAYER_QUIT}): una rinascita non passa da li' e non ripete
 * la riga.
 */
public final class EntryHints {
    /** La riga dei tasti, con i tre tasti come argomenti. */
    public static final String KEYS_HINT = "message." + RedstoneExoframes.MOD_ID + ".keys_hint";

    /** Il nome del tasto di uscita: scritto qui perche' {@code frame/} non puo' nominare {@code client/}. */
    static final String EXIT_KEY = "key." + RedstoneExoframes.MOD_ID + ".exit";
    /** Il nome del tasto del cambio cella, per la stessa ragione. */
    static final String CELL_SWAP_KEY = "key." + RedstoneExoframes.MOD_ID + ".cell_swap";
    /** Il nome del tasto che nasconde l'HUD, per la stessa ragione. */
    static final String HIDE_HUD_KEY = "key." + RedstoneExoframes.MOD_ID + ".hide_hud";

    /** I giocatori che in questa sessione hanno gia' avuto la riga. */
    private static final Set<UUID> SHOWN = ConcurrentHashMap.newKeySet();

    private EntryHints() {
    }

    /** Registra la pulizia all'uscita dal server. Una volta, nell'init comune. */
    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> SHOWN.remove(player.getUUID()));
    }

    /**
     * Dopo un ingresso riuscito ({@link FrameEntry#enter}): la riga dei tasti in chat, se in questa sessione non c'e'
     * gia' stata.
     */
    public static void onEntered(ServerPlayer player) {
        if (SHOWN.add(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(KEYS_HINT, Component.keybind(EXIT_KEY),
                    Component.keybind(CELL_SWAP_KEY), Component.keybind(HIDE_HUD_KEY)));
        }
    }
}
