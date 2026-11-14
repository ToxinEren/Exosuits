package com.toxineren.exoframes.client;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.frame.FrameExitPacket;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Il tasto di uscita dal telaio (design, par. 5.5), predefinito {@code V}: tenuto premuto per
 * {@code frame.exitHoldSeconds} secondi, senza schermate aperte e con la tuta del giocatore locale disegnata
 * ({@link SuitLayer#wearsSuit}), manda al server una volta {@link FrameExitPacket}; intanto la barra delle azioni mostra
 * l'avanzamento. Lasciare il tasto azzera il conteggio e permette un nuovo invio; una schermata aperta o la tuta che
 * non si disegna piu' azzerano il conteggio, non l'invio; uscire dal mondo azzera tutto. Il server decide da solo, sul
 * suo registro, se il giocatore e' davvero in un telaio. Solo client.
 */
public final class ExitKey {
    /** Il nome del tasto nelle impostazioni dei controlli. */
    static final String NAME = "key." + RedstoneExoframes.MOD_ID + ".exit";
    /** La categoria dei tasti della mod. */
    static final String CATEGORY = "key.categories." + RedstoneExoframes.MOD_ID;
    /** Il messaggio di avanzamento sulla barra delle azioni, con la percentuale. */
    static final String HOLD_MESSAGE = "message." + RedstoneExoframes.MOD_ID + ".exit_hold";

    private static final KeyMapping KEY = new KeyMapping(NAME, GLFW.GLFW_KEY_V, CATEGORY);

    /** Tick consecutivi con il tasto premuto e le condizioni soddisfatte. Solo sul thread del client. */
    private static int heldTicks;
    /** Vero se in questa pressione il pacchetto e' gia' partito. Solo sul thread del client. */
    private static boolean sent;
    /** Vero se la barra delle azioni mostra un avanzamento da togliere quando il conteggio si azzera. */
    private static boolean showingProgress;

    private ExitKey() {
    }

    /** Registra il tasto e gli eventi del client. Una volta, da {@link RedstoneExoframesClient#init()}. */
    static void register() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvent.CLIENT_POST.register(ExitKey::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            heldTicks = 0;
            sent = false;
            showingProgress = false;
        });
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (!KEY.isDown()) {
            sent = false;
            stopCounting(player);
            return;
        }
        if (minecraft.screen != null || !SuitLayer.wearsSuit(player)) {
            stopCounting(player);
            return;
        }
        heldTicks = heldTicks == Integer.MAX_VALUE ? heldTicks : heldTicks + 1;
        int needed = ExitHold.ticksNeeded(RedstoneExoframes.config().frame.exitHoldSeconds);
        if (ExitHold.shouldSend(heldTicks, needed, sent)) {
            sent = true;
            clearProgress(player);
            Compat.net().sendToServer(FrameExitPacket.ID, new FrameExitPacket());
        } else if (!sent) {
            long percent = Math.min(100L, heldTicks * 100L / needed);
            player.displayClientMessage(Component.translatable(HOLD_MESSAGE, percent), true);
            showingProgress = true;
        }
    }

    /** Azzera il conteggio (non l'invio) e toglie l'avanzamento dalla barra delle azioni, se c'era. */
    private static void stopCounting(LocalPlayer player) {
        heldTicks = 0;
        clearProgress(player);
    }

    private static void clearProgress(LocalPlayer player) {
        if (showingProgress) {
            player.displayClientMessage(Component.empty(), true);
            showingProgress = false;
        }
    }
}
