package com.toxineren.exoframes.client;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.frame.CellSwapPacket;
import com.toxineren.exoframes.frame.ClientRules;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Il tasto del cambio cella (design §12.10), predefinito {@code R}: tenuto premuto per i secondi del server
 * ({@code energy.cellSwapSeconds} ricevuto con {@code client_rules}, {@link ClientRules#cellSwapSeconds()}; prima del
 * pacchetto quelli della config locale), senza schermate aperte e con la tuta del giocatore locale disegnata
 * ({@link SuitLayer#wearsSuit}), manda al server una volta {@link CellSwapPacket}; intanto la barra delle azioni mostra
 * l'avanzamento. La decisione e' quella del tasto di uscita, {@link ExitHold}, con un'istanza propria. Con il tasto di
 * uscita giu' il cambio non conta: il suo conteggio riparte da zero e l'avanzamento mostrato si toglie, e vince
 * l'uscita. Il server decide da solo, sul suo registro e sull'inventario, se e che cosa cambiare
 * ({@code frame/CellSwap}). Solo client.
 */
public final class CellSwapKey {
    /** Il nome del tasto nelle impostazioni dei controlli. */
    static final String NAME = "key." + RedstoneExoframes.MOD_ID + ".cell_swap";
    /** Il messaggio di avanzamento sulla barra delle azioni, con la percentuale. */
    static final String HOLD_MESSAGE = "message." + RedstoneExoframes.MOD_ID + ".cell_swap_hold";

    private static final KeyMapping KEY = new KeyMapping(NAME, GLFW.GLFW_KEY_R, ExitKey.CATEGORY);

    /** La decisione del tasto: conteggio, invio, avanzamento mostrato. Solo sul thread del client. */
    private static final ExitHold HOLD = new ExitHold();

    private CellSwapKey() {
    }

    /** Registra il tasto e gli eventi del client. Una volta, da {@link RedstoneExoframesClient#init()}. */
    static void register() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvent.CLIENT_POST.register(CellSwapKey::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> HOLD.reset());
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean keyDown = KEY.isDown();
        boolean canCount = keyDown && minecraft.screen == null && SuitLayer.wearsSuit(player) && !ExitKey.keyDown();
        int needed = ExitHold.ticksNeeded(ClientRules.cellSwapSeconds());
        switch (HOLD.tick(keyDown, canCount, needed)) {
            case SHOW_PROGRESS -> player.displayClientMessage(Component.translatable(HOLD_MESSAGE, HOLD.percent()),
                    true);
            case CLEAR_PROGRESS -> player.displayClientMessage(Component.empty(), true);
            case SEND -> Compat.net().sendToServer(CellSwapPacket.ID, new CellSwapPacket());
            case CLEAR_AND_SEND -> {
                player.displayClientMessage(Component.empty(), true);
                Compat.net().sendToServer(CellSwapPacket.ID, new CellSwapPacket());
            }
            case NONE -> {
            }
        }
    }
}
