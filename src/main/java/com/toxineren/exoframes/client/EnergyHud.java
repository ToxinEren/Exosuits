package com.toxineren.exoframes.client;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.frame.SuitSize;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * La riga dell'energia (design §12.13): l'unica eccezione al "niente HUD" di §2, testo con ombra nell'angolo in alto a
 * sinistra per chi indossa la tuta. La prima riga ha i RE della cella indossata, la capacita' e la percentuale; il suo
 * colore viene dagli indicatori calcolati dal server ({@code clientIndicators}), mai da una soglia del client, e senza
 * indicatori e' bianca. Sotto, solo quando un pezzo e' {@code CRITICAL} o {@code BROKEN}, una seconda riga con il pezzo
 * messo peggio ({@link EnergyLine}) e la sua condizione, gialla se critico e rossa se rotto. In alto a sinistra la riga
 * non tocca la barra degli oggetti, l'indicatore del salvataggio ne' i sottotitoli, che stanno in basso; la copre la
 * schermata di F3. Si nasconde con l'interfaccia nascosta (F1). L'HUD vero arriva in M4. Solo client, disegnata a ogni
 * fotogramma tramite {@code ClientRenderHooks.addHudRenderer}.
 */
public final class EnergyHud {
    /** La riga con RE immagazzinati, capacita' e percentuale. */
    static final String ENERGY = "hud." + RedstoneExoframes.MOD_ID + ".energy";
    /** La seconda riga: il nome del pezzo messo peggio e la sua condizione. */
    static final String WORST = "hud." + RedstoneExoframes.MOD_ID + ".worst";
    /** La riga di un telaio indossato senza cella. */
    static final String NO_CELL = "hud." + RedstoneExoframes.MOD_ID + ".no_cell";

    static final int NORMAL_COLOR = 0x55FF55;
    static final int LOW_COLOR = 0xFFFF55;
    static final int EMPTY_COLOR = 0xFF5555;
    static final int NO_INDICATORS_COLOR = 0xFFFFFF;
    /** Distanza delle righe dal bordo sinistro, in pixel dell'interfaccia. */
    static final int LEFT_MARGIN = 4;
    /** Distanza della prima riga dal bordo superiore, in pixel dell'interfaccia. */
    static final int TOP = 4;
    /** Distanza fra la prima e la seconda riga, in pixel dell'interfaccia. */
    static final int LINE_HEIGHT = 10;

    private EnergyHud() {
    }

    /**
     * Disegna le righe del giocatore locale; nulla se non c'e' un giocatore, l'interfaccia e' nascosta, il giocatore non
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
        Optional<EnergyLine> line = EnergyLine.of(view.get());
        Component energy = line.map(l -> Component.translatable(ENERGY, l.stored(), l.capacity(), l.percent()))
                .orElseGet(() -> Component.translatable(NO_CELL));
        int color = Compat.frameState().clientIndicators(player).map(EnergyHud::color).orElse(NO_INDICATORS_COLOR);
        Font font = minecraft.font;
        graphics.drawString(font, energy, LEFT_MARGIN, TOP, color, true);
        line.ifPresent(l -> drawWorst(graphics, font, l));
    }

    /** La seconda riga, solo se un pezzo e' critico o rotto: gialla per un pezzo critico, rossa per uno rotto. */
    private static void drawWorst(GuiGraphics graphics, Font font, EnergyLine line) {
        if (line.worst().isEmpty() || line.worstCondition().isEmpty()) {
            return;
        }
        PieceCondition condition = line.worstCondition().get();
        Component worst = Component.translatable(WORST, SuitDamage.pieceName(line.worst().get()),
                Component.translatable("condition." + RedstoneExoframes.MOD_ID + "."
                        + condition.name().toLowerCase(Locale.ROOT)));
        graphics.drawString(font, worst, LEFT_MARGIN, TOP + LINE_HEIGHT,
                condition == PieceCondition.BROKEN ? EMPTY_COLOR : LOW_COLOR, true);
    }

    private static int color(FrameIndicators indicators) {
        return switch (indicators.energyState()) {
            case NORMAL -> NORMAL_COLOR;
            case LOW -> LOW_COLOR;
            case EMPTY -> EMPTY_COLOR;
        };
    }
}
