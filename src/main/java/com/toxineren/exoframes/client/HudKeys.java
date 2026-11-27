package com.toxineren.exoframes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * I due tasti dell'HUD (design §5.12 "Tasti"): H nasconde e mostra tutto l'HUD della mod, il tasto della vignetta,
 * senza tasto predefinito (come il "contorno degli spettatori" del gioco base: il posto della domanda 10 del design),
 * accende e spegne la vignetta del casco. Registrati come V e R, nella categoria della mod, e letti a ogni tick del
 * client con {@code consumeClick}: ogni clic, senza schermate aperte e con il giocatore nel mondo, cambia la preferenza
 * in {@link ClientPrefs}, che riscrive il file, e dice il valore nuovo sulla barra delle azioni; con o senza la tuta
 * addosso, perche' la preferenza e' del giocatore, non della tuta. Un clic con una schermata aperta si scarta: il gioco
 * base non registra i clic con una schermata (una H scritta nella chat non e' un clic) e consuma F3+H, e qui si
 * scartano quelli rimasti quando una schermata si apre nello stesso tick. Una {@code RuntimeException} non chiude il
 * gioco: il clic si perde, con un ERROR una volta per sessione (si azzera all'uscita dal mondo). Solo client.
 */
final class HudKeys {
    /** Il nome del tasto che nasconde l'HUD, predefinito H. */
    static final String HIDE_HUD = "key." + RedstoneExoframes.MOD_ID + ".hide_hud";
    /** Il nome del tasto della vignetta. */
    static final String VIGNETTE = "key." + RedstoneExoframes.MOD_ID + ".vignette";
    /** Il tasto predefinito della vignetta: nessuno (design §9, domanda 10). */
    static final int VIGNETTE_DEFAULT = InputConstants.UNKNOWN.getValue();
    /** Il messaggio dell'HUD nascosto, con il tasto che lo mostra come argomento. */
    static final String HUD_HIDDEN = "message." + RedstoneExoframes.MOD_ID + ".hud_hidden";
    /** Il messaggio dell'HUD di nuovo visibile. */
    static final String HUD_SHOWN = "message." + RedstoneExoframes.MOD_ID + ".hud_shown";
    /** Il messaggio della vignetta accesa. */
    static final String VIGNETTE_ON = "message." + RedstoneExoframes.MOD_ID + ".vignette_on";
    /** Il messaggio della vignetta spenta. */
    static final String VIGNETTE_OFF = "message." + RedstoneExoframes.MOD_ID + ".vignette_off";

    private static final KeyMapping HIDE_HUD_KEY = new KeyMapping(HIDE_HUD, GLFW.GLFW_KEY_H, ExitKey.CATEGORY);
    private static final KeyMapping VIGNETTE_KEY = new KeyMapping(VIGNETTE, VIGNETTE_DEFAULT, ExitKey.CATEGORY);

    /** Vero dopo il primo ERROR della sessione. Solo sul thread del client. */
    private static boolean failureLogged;

    private HudKeys() {
    }

    /**
     * Registra i due tasti e gli eventi del client. Una volta, da {@link RedstoneExoframesClient#init()}, dopo
     * {@link ClientPrefs#load()}.
     */
    static void register() {
        KeyMappingRegistry.register(HIDE_HUD_KEY);
        KeyMappingRegistry.register(VIGNETTE_KEY);
        ClientTickEvent.CLIENT_POST.register(HudKeys::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> failureLogged = false);
    }

    private static void tick(Minecraft minecraft) {
        try {
            while (HIDE_HUD_KEY.consumeClick()) {
                LocalPlayer player = minecraft.player;
                if (minecraft.screen == null && player != null) {
                    player.displayClientMessage(ClientPrefs.toggleHud()
                            ? Component.translatable(HUD_HIDDEN, Component.keybind(HIDE_HUD))
                            : Component.translatable(HUD_SHOWN), true);
                }
            }
            while (VIGNETTE_KEY.consumeClick()) {
                LocalPlayer player = minecraft.player;
                if (minecraft.screen == null && player != null) {
                    player.displayClientMessage(Component.translatable(ClientPrefs.toggleVignette() ? VIGNETTE_ON
                            : VIGNETTE_OFF), true);
                }
            }
        } catch (RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                RedstoneExoframes.LOGGER.error("Suit HUD keys failed with {}; the key press is ignored (logged once "
                        + "per session)", e.getClass().getName(), e);
            }
        }
    }
}
