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
 * non si disegna piu' azzerano il conteggio, non l'invio; uscire dal mondo azzera tutto. Tutta la decisione sta in
 * {@link ExitHold}: qui si leggono il tasto e le condizioni e si fa quello che dice. Il server decide da solo, sul suo
 * registro, se il giocatore e' davvero in un telaio. Solo client.
 */
public final class ExitKey {
    /** Il nome del tasto nelle impostazioni dei controlli. */
    static final String NAME = "key." + RedstoneExoframes.MOD_ID + ".exit";
    /** La categoria dei tasti della mod. */
    static final String CATEGORY = "key.categories." + RedstoneExoframes.MOD_ID;
    /** Il messaggio di avanzamento sulla barra delle azioni, con la percentuale. */
    static final String HOLD_MESSAGE = "message." + RedstoneExoframes.MOD_ID + ".exit_hold";

    private static final KeyMapping KEY = new KeyMapping(NAME, GLFW.GLFW_KEY_V, CATEGORY);

    /** La decisione del tasto: conteggio, invio, avanzamento mostrato. Solo sul thread del client. */
    private static final ExitHold HOLD = new ExitHold();

    private ExitKey() {
    }

    /** Registra il tasto e gli eventi del client. Una volta, da {@link RedstoneExoframesClient#init()}. */
    static void register() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvent.CLIENT_POST.register(ExitKey::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> HOLD.reset());
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean keyDown = KEY.isDown();
        boolean canCount = keyDown && minecraft.screen == null && SuitLayer.wearsSuit(player);
        int needed = ExitHold.ticksNeeded(RedstoneExoframes.config().frame.exitHoldSeconds);
        switch (HOLD.tick(keyDown, canCount, needed)) {
            case SHOW_PROGRESS -> player.displayClientMessage(Component.translatable(HOLD_MESSAGE, HOLD.percent()),
                    true);
            case CLEAR_PROGRESS -> player.displayClientMessage(Component.empty(), true);
            case SEND -> Compat.net().sendToServer(FrameExitPacket.ID, new FrameExitPacket());
            case CLEAR_AND_SEND -> {
                player.displayClientMessage(Component.empty(), true);
                Compat.net().sendToServer(FrameExitPacket.ID, new FrameExitPacket());
            }
            case NONE -> {
            }
        }
    }
}
