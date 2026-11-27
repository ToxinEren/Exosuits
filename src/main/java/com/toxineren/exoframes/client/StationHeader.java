package com.toxineren.exoframes.client;

import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.core.station.StationView.CellInfo;
import com.toxineren.exoframes.core.station.StationView.PieceInfo;
import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.station.StationTexts;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * L'intestazione della scelta nel pannello centrale della schermata della stazione (design M4 §5.3), dalla vista del
 * telaio, e accanto al nome la freccia che smonta la scelta, con il suo suggerimento:
 * <ul>
 * <li>un pezzo: il nome dell'oggetto, la condizione nel suo colore, la durata e la durata massima allineate a destra, e
 * la barra in proporzione; se i numeri non stanno accanto alla condizione ({@link StationLayout#fitsBeside}, con una
 * durata massima da 10 000 in su) resta la sola durata;</li>
 * <li>uno slot vuoto: il nome dello slot e sotto "Vuoto", tutti e due spenti ("Braccio sinistro: vuoto" non sta nella
 * riga del nome);</li>
 * <li>la cella: il nome, la carica e la capacita' allineate a destra (stanno sempre nella riga, anche con la capacita'
 * piu' grande della config) e la barra rossa; senza cella "Cella" e "Vuoto", spenti;</li>
 * <li>tutti i pezzi: il solo nome.</li>
 * </ul>
 * La freccia e' accesa se la scelta ha qualcosa da smontare ({@link StationLayout#unmountButton}): spenta per tutti i
 * pezzi, per uno slot vuoto e per la cella assente. Disegna con la texture della schermata
 * ({@link StationScreen#TEXTURE}) sopra lo sfondo: chi disegna spegne la fusione alla fine.
 */
final class StationHeader {
    private final Font font;
    /** Il pannello del telaio, che ha le icone dei pezzi e quindi i nomi dei loro oggetti. */
    private final StationFramePanel panel;

    StationHeader(Font font, StationFramePanel panel) {
        this.font = font;
        this.panel = panel;
    }

    /**
     * L'intestazione e la freccia, con l'angolo della schermata in ({@code left}, {@code top}); {@code arrowPressed}
     * vero mentre il tasto sinistro e' tenuto premuto sulla freccia, {@code arrowHover} con il mouse sopra.
     */
    void draw(GuiGraphics graphics, int left, int top, StationView view, boolean arrowPressed, boolean arrowHover) {
        drawSelection(graphics, left, top, view);
        Rect r = StationLayout.UNMOUNT;
        StationLayout.ButtonState state = StationLayout.buttonState(StationLayout.unmountButton(view).isPresent(),
                arrowPressed, arrowHover);
        StationScreen.TEXTURE.blit(graphics, StationLayout.unmountSprite(state), left + r.x0(), top + r.y0());
    }

    /** Le righe del suggerimento nel punto (x, y), relativo all'angolo della schermata: la freccia dice che smonta. */
    List<Component> tooltip(double x, double y) {
        return StationLayout.UNMOUNT.contains(x, y) ? List.of(Component.translatable(StationTexts.UNMOUNT)) : List.of();
    }

    /** Il nome, la condizione, i numeri e la barra della scelta. */
    private void drawSelection(GuiGraphics graphics, int left, int top, StationView view) {
        int selection = view.selection();
        if (selection == StationView.SELECT_ALL) {
            drawText(graphics, left, top, Component.translatable(StationTexts.ALL_PIECES), StationLayout.NAME,
                    StationLayout.TEXT);
            return;
        }
        if (selection == StationView.SELECT_CELL) {
            Optional<CellInfo> cell = view.cell();
            if (cell.isEmpty()) {
                drawEmpty(graphics, left, top, Component.translatable(StationTexts.CELL));
                return;
            }
            int charge = StationFramePanel.charge(cell.get());
            drawText(graphics, left, top, Component.translatable(StationTexts.CELL), StationLayout.NAME,
                    StationLayout.TEXT);
            // la carica sta nella riga intera: allineata a destra entra dove per un pezzo c'e' la condizione, che la
            // cella non ha
            drawTextRight(graphics, left, top, Component.translatable(CellItem.TOOLTIP_CHARGE, charge,
                    cell.get().capacity()), StationLayout.DURABILITY_NUMBERS, StationLayout.TEXT);
            drawBar(graphics, left, top, charge, cell.get().capacity(), StationLayout.DURATA_CELLA);
            return;
        }
        PieceSlot slot = PieceSlot.values()[selection];
        PieceInfo info = view.pieces().get(slot);
        if (info == null) {
            drawEmpty(graphics, left, top, SuitDamage.pieceName(slot));
            return;
        }
        PieceCondition condition = StationFramePanel.condition(info);
        Component conditionName = StationFramePanel.conditionName(condition);
        drawText(graphics, left, top, panel.itemName(slot), StationLayout.NAME, StationLayout.TEXT);
        drawText(graphics, left, top, conditionName, StationLayout.CONDITION, StationLayout.conditionColor(condition));
        Component numbers = Component.translatable(StationTexts.DURABILITY, info.durability(), info.maxDurability());
        if (!StationLayout.fitsBeside(font.width(conditionName), font.width(numbers))) {
            numbers = Component.literal(String.valueOf(info.durability()));
        }
        drawTextRight(graphics, left, top, numbers, StationLayout.DURABILITY_NUMBERS, StationLayout.TEXT);
        drawBar(graphics, left, top, info.durability(), info.maxDurability(), StationLayout.durability(condition));
    }

    /** Un posto vuoto scelto: il suo nome e sotto "Vuoto", tutti e due spenti. */
    private void drawEmpty(GuiGraphics graphics, int left, int top, Component name) {
        drawText(graphics, left, top, name, StationLayout.NAME, StationLayout.TEXT_DIM);
        drawText(graphics, left, top, Component.translatable(StationTexts.EMPTY), StationLayout.CONDITION,
                StationLayout.TEXT_DIM);
    }

    /**
     * La barra: il fondo e, da sinistra, la parte piena in proporzione a {@code value / max}, dal riempimento dato (il
     * colore della condizione, o il rosso della cella).
     */
    private static void drawBar(GuiGraphics graphics, int left, int top, int value, int max, Sprite fill) {
        Rect bar = StationLayout.DURABILITY_BAR;
        StationScreen.TEXTURE.blit(graphics, StationLayout.DURATA_FONDO, left + bar.x0(), top + bar.y0());
        int filled = (int) ((long) bar.width() * Math.max(0, Math.min(value, max)) / max);
        if (filled > 0) {
            StationScreen.TEXTURE.blit(graphics, fill.left(filled), left + bar.x0(), top + bar.y0());
        }
    }

    private void drawText(GuiGraphics graphics, int left, int top, Component text, Rect r, int colour) {
        graphics.drawString(font, text, left + r.x0(), top + r.y0(), colour, false);
    }

    private void drawTextRight(GuiGraphics graphics, int left, int top, Component text, Rect r, int colour) {
        graphics.drawString(font, text, left + r.x1() - font.width(text), top + r.y0(), colour, false);
    }
}
