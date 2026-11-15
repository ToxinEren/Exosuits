package com.toxineren.exoframes.client;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.frame.SuitSize;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * La riga dell'energia (design §12.13): l'unica eccezione al "niente HUD" di §2, una riga di testo con ombra in basso a
 * destra per chi indossa la tuta, con i RE della cella indossata, la percentuale e il pezzo messo peggio
 * ({@link EnergyLine}). Il colore viene dagli indicatori calcolati dal server ({@code clientIndicators}), mai da una
 * soglia del client; senza indicatori la riga e' bianca. La riga sta sopra i sottotitoli, che in quell'angolo la
 * possono coprire (accettato); si nasconde con l'interfaccia nascosta (F1). L'HUD vero arriva in M4. Solo client,
 * disegnata a ogni fotogramma tramite {@code ClientRenderHooks.addHudRenderer}.
 */
public final class EnergyHud {
    /** La riga con RE immagazzinati, capacita' e percentuale. */
    static final String ENERGY = "hud." + RedstoneExoframes.MOD_ID + ".energy";
    /** La riga dell'energia seguita dal nome del pezzo messo peggio e dalla sua condizione. */
    static final String ENERGY_WORST = "hud." + RedstoneExoframes.MOD_ID + ".energy_worst";
    /** La riga di un telaio indossato senza cella. */
    static final String NO_CELL = "hud." + RedstoneExoframes.MOD_ID + ".no_cell";

    static final int NORMAL_COLOR = 0x55FF55;
    static final int LOW_COLOR = 0xFFFF55;
    static final int EMPTY_COLOR = 0xFF5555;
    static final int NO_INDICATORS_COLOR = 0xFFFFFF;
    /** Distanza dal bordo destro, in pixel dell'interfaccia. */
    static final int MARGIN = 4;
    /** Distanza della riga dal bordo inferiore, in pixel dell'interfaccia. */
    static final int BOTTOM = 12;

    private EnergyHud() {
    }

    /**
     * Disegna la riga del giocatore locale; nulla se non c'e' un giocatore, l'interfaccia e' nascosta, il giocatore non
     * indossa la tuta o il client non ha ancora una copia del suo stato.
     */
    static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !SuitSize.wearsSuit(player)) {
            return;
        }
        Optional<FrameState> view = Compat.frameState().clientView(player);
        if (view.isEmpty()) {
            return;
        }
        Component text = EnergyLine.of(view.get()).map(EnergyHud::text)
                .orElseGet(() -> Component.translatable(NO_CELL));
        int color = Compat.frameState().clientIndicators(player).map(EnergyHud::color).orElse(NO_INDICATORS_COLOR);
        Font font = minecraft.font;
        graphics.drawString(font, text, graphics.guiWidth() - font.width(text) - MARGIN, graphics.guiHeight() - BOTTOM,
                color, true);
    }

    private static Component text(EnergyLine line) {
        Component energy = Component.translatable(ENERGY, line.stored(), line.capacity(), line.percent());
        if (line.worst().isEmpty() || line.worstCondition().isEmpty()) {
            return energy;
        }
        return Component.translatable(ENERGY_WORST, energy,
                Component.translatable("piece." + RedstoneExoframes.MOD_ID + "."
                        + line.worst().get().name().toLowerCase(Locale.ROOT)),
                Component.translatable("condition." + RedstoneExoframes.MOD_ID + "."
                        + line.worstCondition().get().name().toLowerCase(Locale.ROOT)));
    }

    private static int color(FrameIndicators indicators) {
        return switch (indicators.energyState()) {
            case NORMAL -> NORMAL_COLOR;
            case LOW -> LOW_COLOR;
            case EMPTY -> EMPTY_COLOR;
        };
    }
}
