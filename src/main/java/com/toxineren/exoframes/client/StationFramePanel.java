package com.toxineren.exoframes.client;

import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.core.station.StationView.CellInfo;
import com.toxineren.exoframes.core.station.StationView.PieceInfo;
import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.station.StationTexts;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Il pannello del telaio della schermata della stazione (design M4 §5.3), dalla vista del telaio: i sei posti dei pezzi
 * in forma di corpo e il posto della cella, e sotto il pulsante "Tutti". Un posto con il pezzo ha il bordo del colore
 * della sua condizione e l'icona dell'oggetto con la barra della durata; la cella il bordo rosso della redstone e la
 * sua icona; un posto vuoto il bordo grigio e, dove starebbe l'icona, il fantasma del suo tipo; il posto scelto un
 * secondo bordo bianco, fuori dal primo (con tutti i pezzi scelti, attorno a ogni pezzo montato). "Tutti" e' premuto
 * quando la scelta e' tutti i pezzi o il tasto sinistro e' tenuto premuto su di lui, ed e' sempre acceso. Con i loro
 * suggerimenti.
 * <p>
 * Disegna con la texture della schermata ({@link StationScreen#TEXTURE}) sopra lo sfondo: chi disegna spegne la fusione
 * alla fine. Le icone si rifanno solo quando arriva una vista nuova ({@link #update}).
 */
final class StationFramePanel {
    private final Font font;
    /** La vista delle icone: quando il menu ne ha una nuova si rifanno. */
    private StationView shown;
    /** Le icone dei pezzi montati: lo stack vuoto per un livello senza oggetto. */
    private final Map<PieceSlot, ItemStack> pieceIcons = new EnumMap<>(PieceSlot.class);
    /** L'icona della cella; vuota senza cella. */
    private ItemStack cellIcon = ItemStack.EMPTY;

    StationFramePanel(Font font) {
        this.font = font;
    }

    /** Le icone dei pezzi e della cella della vista, rifatte se la vista e' cambiata dall'ultima volta. */
    void update(StationView view) {
        if (view == shown) {
            return;
        }
        shown = view;
        pieceIcons.clear();
        view.pieces().forEach((slot, info) -> pieceIcons.put(slot, pieceIcon(slot, info)));
        cellIcon = view.cell().map(cell -> CellItem.fromCell(new EnergyCell(cell.capacity(), charge(cell))))
                .orElse(ItemStack.EMPTY);
    }

    /**
     * Il nome dell'oggetto del pezzo montato nello slot, per esempio "Torso Rottame"; il nome dello slot se non ha un
     * oggetto.
     */
    Component itemName(PieceSlot slot) {
        ItemStack icon = pieceIcons.getOrDefault(slot, ItemStack.EMPTY);
        return icon.isEmpty() ? SuitDamage.pieceName(slot) : icon.getHoverName();
    }

    /**
     * I sei posti, la cella e "Tutti", con l'angolo della schermata in ({@code left}, {@code top}); {@code allPressed}
     * vero mentre il tasto sinistro e' tenuto premuto su "Tutti", {@code allHover} con il mouse sopra.
     */
    void draw(GuiGraphics graphics, int left, int top, StationView view, boolean allPressed, boolean allHover) {
        StationLayout.PIECE_SLOTS.forEach((slot, place) -> {
            PieceInfo info = view.pieces().get(slot);
            if (info == null) {
                drawEmptyPlace(graphics, left, top, place, slot.ordinal());
            } else {
                drawPlace(graphics, left, top, place, StationLayout.place(condition(info)), pieceIcons.get(slot));
            }
            if (view.selection() == StationView.selectionOf(slot)
                    || (view.selection() == StationView.SELECT_ALL && info != null)) {
                drawSelection(graphics, left, top, place);
            }
        });
        if (view.cell().isPresent()) {
            drawPlace(graphics, left, top, StationLayout.CELL, StationLayout.POSTO_CELLA, cellIcon);
        } else {
            drawEmptyPlace(graphics, left, top, StationLayout.CELL, StationLayout.CELL_PLACE);
        }
        if (view.selection() == StationView.SELECT_CELL) {
            drawSelection(graphics, left, top, StationLayout.CELL);
        }
        drawAllButton(graphics, left, top, view, allPressed, allHover);
    }

    /**
     * Le righe del suggerimento di un posto nel punto (x, y), relativo all'angolo della schermata; vuote fuori dai
     * posti. Un pezzo: il nome dello slot, il nome dell'oggetto, la durata e la condizione nel suo colore; la cella: il
     * nome e la carica; un posto vuoto il suo nome e "vuoto".
     */
    List<Component> tooltip(StationView view, double x, double y) {
        for (Map.Entry<PieceSlot, Rect> place : StationLayout.PIECE_SLOTS.entrySet()) {
            if (place.getValue().contains(x, y)) {
                return pieceTooltip(view, place.getKey());
            }
        }
        if (StationLayout.CELL.contains(x, y)) {
            Optional<CellInfo> cell = view.cell();
            if (cell.isEmpty()) {
                return List.of(emptySlot(Component.translatable(StationTexts.CELL)));
            }
            return List.of(Component.translatable(StationTexts.CELL), Component.translatable(CellItem.TOOLTIP_CHARGE,
                    charge(cell.get()), cell.get().capacity()).withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    /** Un posto occupato: il disegno del posto con il bordo del suo stato, poi l'icona con la sua barra. */
    private void drawPlace(GuiGraphics graphics, int left, int top, Rect place, Sprite sprite, ItemStack icon) {
        StationScreen.TEXTURE.blit(graphics, sprite, left + place.x0(), top + place.y0());
        int x = left + place.x0() + StationLayout.ICON_INSET;
        int y = top + place.y0() + StationLayout.ICON_INSET;
        graphics.renderItem(icon, x, y);
        graphics.renderItemDecorations(font, icon, x, y);
    }

    /** Un posto vuoto: il fondo scuro con il bordo grigio e, dove starebbe l'icona, il fantasma del suo tipo. */
    private static void drawEmptyPlace(GuiGraphics graphics, int left, int top, Rect place, int number) {
        StationScreen.TEXTURE.blit(graphics, StationLayout.POSTO_VUOTO, left + place.x0(), top + place.y0());
        StationScreen.TEXTURE.blit(graphics, StationLayout.ghostSprite(number),
                left + place.x0() + StationLayout.ICON_INSET, top + place.y0() + StationLayout.ICON_INSET);
    }

    /** Il secondo bordo, bianco, fuori dal primo. */
    private static void drawSelection(GuiGraphics graphics, int left, int top, Rect place) {
        Rect ring = place.grow(1);
        StationScreen.TEXTURE.blit(graphics, StationLayout.SCELTA, left + ring.x0(), top + ring.y0());
    }

    /** "Tutti": il disegno del suo stato e l'etichetta centrata, nel colore del testo. */
    private void drawAllButton(GuiGraphics graphics, int left, int top, StationView view, boolean pressed,
            boolean hover) {
        Rect r = StationLayout.ALL_BUTTON;
        StationLayout.ButtonState state = StationLayout.buttonState(true,
                pressed || view.selection() == StationView.SELECT_ALL, hover);
        StationScreen.TEXTURE.blit(graphics, StationLayout.allSprite(state), left + r.x0(), top + r.y0());
        Component label = Component.translatable(StationTexts.ALL);
        graphics.drawString(font, label, left + r.x0() + (r.width() - font.width(label)) / 2,
                top + r.y0() + (r.height() - font.lineHeight) / 2 + 1, StationLayout.TEXT, false);
    }

    /** Il suggerimento di un posto dei pezzi: nome dello slot, nome dell'oggetto, durata, condizione. */
    private List<Component> pieceTooltip(StationView view, PieceSlot slot) {
        PieceInfo info = view.pieces().get(slot);
        if (info == null) {
            return List.of(emptySlot(SuitDamage.pieceName(slot)));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(SuitDamage.pieceName(slot));
        ItemStack icon = pieceIcons.getOrDefault(slot, ItemStack.EMPTY);
        if (!icon.isEmpty()) {
            lines.add(icon.getHoverName());
        }
        lines.add(Component.translatable(PieceItem.TOOLTIP_DURABILITY, info.durability(), info.maxDurability())
                .withStyle(ChatFormatting.GRAY));
        PieceCondition condition = condition(info);
        int colour = StationLayout.conditionColor(condition) & 0xFFFFFF;
        lines.add(conditionName(condition).withStyle(style -> style.withColor(colour)));
        return lines;
    }

    /**
     * L'icona di un pezzo montato: l'oggetto del pezzo con la durata e l'aspetto della vista. Un livello senza oggetto
     * (in questa parte c'e' solo il Rottame) da' lo stack vuoto: il posto si disegna senza icona.
     */
    private static ItemStack pieceIcon(PieceSlot slot, PieceInfo info) {
        try {
            return PieceItem.fromPiece(new Piece(slot, info.tier(), info.maxDurability(), info.durability()), List.of(),
                    new Appearance(info.primary(), info.secondary(), List.of()));
        } catch (IllegalArgumentException e) {
            return ItemStack.EMPTY;
        }
    }

    /** La condizione del pezzo dalla sua durata. */
    static PieceCondition condition(PieceInfo info) {
        return PieceCondition.of(info.durability(), info.maxDurability());
    }

    /** La carica della cella da mostrare, mai sopra la capacita' (come il suggerimento dell'oggetto). */
    static int charge(CellInfo cell) {
        return Math.min(cell.stored(), cell.capacity());
    }

    /** Il nome della condizione, nella lingua del giocatore. */
    static MutableComponent conditionName(PieceCondition condition) {
        return Component.translatable(StationTexts.CONDITION_PREFIX + condition.name().toLowerCase(Locale.ROOT));
    }

    /** Il suggerimento di un posto vuoto: il nome e "vuoto". */
    private static Component emptySlot(Component name) {
        return Component.translatable(StationTexts.EMPTY_SLOT, name);
    }
}
