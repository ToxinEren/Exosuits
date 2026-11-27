package com.toxineren.exoframes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.client.CompatContainerScreen;
import com.toxineren.exoframes.compat.client.SuitView;
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
import com.toxineren.exoframes.station.StationMenu;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * La schermata della stazione di manutenzione nella parte 4a-1 (design §5.3 e §5.4), con la disposizione di
 * {@link StationLayout} e, dalla parte 4d, la grafica nello stile A della texture {@code textures/gui/station.png}
 * (tools/art/gui.py): lo sfondo con i pannelli, le cornici degli slot del giocatore e il fondo dell'anteprima, poi i
 * disegni dei widget nei loro stati. Mostra in sola lettura la vista del telaio che il menu ha dal server
 * ({@link StationMenu#view}):
 * <ul>
 * <li>il pannello del telaio, i sei posti dei pezzi in forma di corpo e la cella: un pezzo nel posto con il bordo del
 * colore della sua condizione e la sua icona con la barra della durata, un posto vuoto con il bordo grigio, la cella
 * con il bordo rosso, il posto scelto con un secondo bordo bianco, fuori dal primo;</li>
 * <li>l'intestazione della scelta nel pannello centrale: nome, condizione nel suo colore, durata e barra di un pezzo;
 * "vuoto" per uno slot senza pezzo; la carica per la cella; "Tutti i pezzi";</li>
 * <li>l'anteprima 3D della tuta del telaio ({@link SuitPreview}, con la vista di {@link StationLayout#suitView}), che
 * trascinando con il tasto sinistro nel ritaglio gira di {@link StationLayout#DEGREES_PER_PIXEL} gradi per pixel, con
 * le due frecce in basso; l'angolo vive nella schermata e parte da 0, di fronte, a ogni apertura, e al server non va
 * nulla;</li>
 * <li>l'inventario del giocatore, gli slot del menu, che funzionano come nel gioco base;</li>
 * <li>il pulsante di prova, normale finche' il server non ha confermato nessuna pressione, premuto dopo la prima, con
 * dentro il numero delle pressioni confermate ({@link StationMenu#testPresses}), e piu' chiaro sotto il mouse prima
 * della prima pressione: il clic manda soltanto il numero al server, e il cambio che si vede viene dal server.</li>
 * </ul>
 * Passando il mouse su un posto, sull'anteprima o sul pulsante compare il suggerimento; come per gli slot del gioco
 * base, non con un oggetto sul cursore. Nessun clic sul pannello del telaio fa nulla: la scelta e il montaggio sono
 * della parte 4a-2, che disegna i suoi widget dalla stessa texture (i disegni hanno gia' i loro nomi in
 * {@link StationLayout}).
 */
public final class StationScreen extends CompatContainerScreen<StationMenu> {
    private static final String GUI_KEY = "gui." + RedstoneExoframes.MOD_ID + ".";
    /** Il suggerimento del pulsante di prova, con il numero delle pressioni confermate. */
    private static final String TEST_BUTTON_KEY = GUI_KEY + "station.test_button";
    /** Il nome della cella, nell'intestazione e nel suggerimento. */
    private static final String CELL_KEY = GUI_KEY + "station.cell";
    /** L'intestazione della scelta di tutti i pezzi. */
    private static final String ALL_PIECES_KEY = GUI_KEY + "station.all_pieces";
    /** Un posto senza pezzo (o senza cella), con il suo nome. */
    private static final String EMPTY_SLOT_KEY = GUI_KEY + "station.empty_slot";
    /** La durata e la durata massima nell'intestazione. */
    private static final String DURABILITY_KEY = GUI_KEY + "station.durability";
    /** Il suggerimento dell'anteprima. */
    private static final String ROTATE_KEY = GUI_KEY + "station.rotate";
    /** Il nome di una condizione: la chiave continua con il nome della condizione in minuscolo. */
    private static final String CONDITION_KEY = GUI_KEY + "condition.";

    /** Fra il bordo di un posto del pannello del telaio e la sua icona: il bordo e la cornice dello slot. */
    private static final int ICON_INSET = 2;
    /** La texture della schermata, scritta da tools/art/gui.py: i posti dei disegni sono in {@link StationLayout}. */
    static final ResourceLocation TEXTURE_ID = Ids.of("textures/gui/station.png");
    private static final GuiTexture TEXTURE = new GuiTexture(TEXTURE_ID, StationLayout.TEXTURE_W,
            StationLayout.TEXTURE_H);

    /** Il modello umanoide a riposo che da' la posa alle ossa della tuta; non si disegna mai. */
    private HumanoidModel<LivingEntity> model;
    /** L'angolo dell'anteprima in gradi, in [0, 360): 0 di fronte. */
    private float angle;
    /** Vero mentre il tasto sinistro, premuto nel ritaglio dell'anteprima, gira la tuta. */
    private boolean rotating;
    /** Dove il tasto sinistro e' stato premuto nel ritaglio (ascissa dell'interfaccia), e l'angolo di allora. */
    private double rotationStartX;
    private float rotationStartAngle;

    /** La vista che la schermata mostra: quando il menu ne ha una nuova si rifanno la tuta e le icone. */
    private StationView shown;
    /** La tuta dell'anteprima, dalla vista mostrata. */
    private SuitView suit;
    /** Le icone dei pezzi montati: lo stack vuoto per un livello senza oggetto. */
    private final Map<PieceSlot, ItemStack> pieceIcons = new EnumMap<>(PieceSlot.class);
    /** L'icona della cella; vuota senza cella. */
    private ItemStack cellIcon = ItemStack.EMPTY;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = StationLayout.WIDTH;
        this.imageHeight = StationLayout.HEIGHT;
        this.titleLabelX = StationLayout.TITLE.x0();
        this.titleLabelY = StationLayout.TITLE.y0();
        this.inventoryLabelX = StationLayout.INVENTORY_LABEL.x0();
        this.inventoryLabelY = StationLayout.INVENTORY_LABEL.y0();
    }

    @Override
    protected void init() {
        super.init();
        model = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER));
        // I modelli nascono "giovani": GeckoLib copia il segno e rimpicciolirebbe la tuta come quella di un cucciolo.
        model.young = false;
    }

    /**
     * Lo sfondo della texture (la schermata con i pannelli, le cornici degli slot dell'inventario e il fondo
     * dell'anteprima), poi il pannello del telaio, l'intestazione della scelta, l'anteprima con le frecce e il pulsante
     * di prova. La fusione accesa per i disegni si spegne in un {@code finally}.
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        StationView view = refresh();
        try {
            TEXTURE.blit(graphics, StationLayout.FONDO, leftPos, topPos);
            drawFramePanel(graphics, view);
            drawHeader(graphics, view);
            drawPreview(graphics);
            drawTestButton(graphics, mouseX, mouseY);
        } finally {
            GuiTexture.endBlend();
        }
    }

    /** Il titolo e l'etichetta dell'inventario, nel colore del testo: il grigio del gioco base non si legge qui. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, StationLayout.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, StationLayout.TEXT, false);
    }

    /** Il suggerimento del posto, dell'anteprima o del pulsante sotto il mouse, con il cursore vuoto. */
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> lines = tooltip(refresh(), mouseX - leftPos, mouseY - topPos);
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /**
     * Il clic sinistro sul pulsante di prova manda il suo numero al server e basta; nel ritaglio dell'anteprima
     * comincia a girare la tuta. Il resto va al gioco base, che sul pannello del telaio non trova slot e non fa nulla.
     * Una pressione sinistra fuori dal ritaglio smette di girare la tuta: se il rilascio di una rotazione si e' perso,
     * la rotazione non resta armata, e il rilascio di questa pressione va al gioco base.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            rotating = false;
            if (StationLayout.TEST_BUTTON.contains(x, y)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.TEST_BUTTON);
                return true;
            }
            if (StationLayout.PREVIEW_CLIP.contains(x, y)) {
                rotating = true;
                rotationStartX = mouseX;
                rotationStartAngle = angle;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Trascinando con il tasto sinistro premuto nel ritaglio la tuta gira; ogni altro trascinamento va al gioco base,
     * che lo usa per distribuire gli oggetti del cursore negli slot. L'angolo segue la distanza orizzontale dal punto
     * in cui il tasto e' stato premuto, non la somma degli spostamenti {@code dragX}: su 1.21.1 il gioco manda un
     * trascinamento per fotogramma con gli spostamenti accumulati, e il primo comprende anche il movimento fatto prima
     * della pressione nello stesso fotogramma (a pochi fotogrammi al secondo la tuta saltava di centinaia di gradi).
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && rotating) {
            angle = StationLayout.rotate(rotationStartAngle, mouseX - rotationStartX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Il rilascio del tasto sinistro che chiude una rotazione smette di girare la tuta e non va al gioco base: nel
     * gioco base e' il rilascio a posare l'oggetto preso sul cursore, quindi lascerebbe cadere la pila (rilascio fuori
     * dalla schermata) o la poserebbe nello slot sotto il mouse. La pressione che ha cominciato la rotazione non era
     * andata al gioco base ({@link #mouseClicked}), che non ha nulla in sospeso da chiudere. Ogni altro rilascio va al
     * gioco base.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && rotating) {
            rotating = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** La vista del menu, con la tuta dell'anteprima e le icone rifatte se e' cambiata dall'ultima volta. */
    private StationView refresh() {
        StationView view = menu.view();
        if (view != shown) {
            shown = view;
            suit = StationLayout.suitView(view);
            pieceIcons.clear();
            view.pieces().forEach((slot, info) -> pieceIcons.put(slot, pieceIcon(slot, info)));
            cellIcon = view.cell().map(cell -> CellItem.fromCell(new EnergyCell(cell.capacity(), charge(cell))))
                    .orElse(ItemStack.EMPTY);
        }
        return view;
    }

    /** I sei posti dei pezzi e la cella, con il secondo bordo attorno alla scelta (con 7, attorno a ogni pezzo). */
    private void drawFramePanel(GuiGraphics graphics, StationView view) {
        StationLayout.PIECE_SLOTS.forEach((slot, place) -> {
            PieceInfo info = view.pieces().get(slot);
            if (info == null) {
                drawEmptyPlace(graphics, place);
            } else {
                drawPlace(graphics, place, StationLayout.place(condition(info)), pieceIcons.get(slot));
            }
            if (view.selection() == StationView.selectionOf(slot)
                    || (view.selection() == StationView.SELECT_ALL && info != null)) {
                drawSelection(graphics, place);
            }
        });
        if (view.cell().isPresent()) {
            drawPlace(graphics, StationLayout.CELL, StationLayout.POSTO_CELLA, cellIcon);
        } else {
            drawEmptyPlace(graphics, StationLayout.CELL);
        }
        if (view.selection() == StationView.SELECT_CELL) {
            drawSelection(graphics, StationLayout.CELL);
        }
    }

    /** Un posto occupato: il disegno del posto con il bordo del suo stato, poi l'icona con la sua barra. */
    private void drawPlace(GuiGraphics graphics, Rect place, Sprite sprite, ItemStack icon) {
        draw(graphics, sprite, place);
        int x = leftPos + place.x0() + ICON_INSET;
        int y = topPos + place.y0() + ICON_INSET;
        graphics.renderItem(icon, x, y);
        graphics.renderItemDecorations(font, icon, x, y);
    }

    /** Un posto vuoto: il fondo scuro con il bordo grigio. */
    private void drawEmptyPlace(GuiGraphics graphics, Rect place) {
        draw(graphics, StationLayout.POSTO_VUOTO, place);
    }

    /** Il secondo bordo, bianco, fuori dal primo. */
    private void drawSelection(GuiGraphics graphics, Rect place) {
        draw(graphics, StationLayout.SCELTA, place.grow(1));
    }

    /**
     * L'intestazione della scelta: per un pezzo il nome dell'oggetto, la condizione nel suo colore, la durata allineata
     * a destra e la barra in proporzione, del colore della condizione; per uno slot vuoto il suo nome e "vuoto"; per la
     * cella il nome, la carica e la barra rossa; per tutti i pezzi il solo nome.
     */
    private void drawHeader(GuiGraphics graphics, StationView view) {
        int selection = view.selection();
        if (selection == StationView.SELECT_ALL) {
            drawText(graphics, Component.translatable(ALL_PIECES_KEY), StationLayout.NAME, StationLayout.TEXT);
            return;
        }
        if (selection == StationView.SELECT_CELL) {
            Optional<CellInfo> cell = view.cell();
            if (cell.isEmpty()) {
                drawText(graphics, empty(Component.translatable(CELL_KEY)), StationLayout.NAME, StationLayout.TEXT_DIM);
                return;
            }
            int charge = charge(cell.get());
            drawText(graphics, Component.translatable(CELL_KEY), StationLayout.NAME, StationLayout.TEXT);
            // "10000 / 10000 RE" e' piu' largo della colonna dei numeri: allineata a destra, entra dove per un pezzo
            // c'e' la condizione, che la cella non ha
            drawTextRight(graphics, Component.translatable(CellItem.TOOLTIP_CHARGE, charge, cell.get().capacity()),
                    StationLayout.DURABILITY_NUMBERS, StationLayout.TEXT);
            drawBar(graphics, charge, cell.get().capacity(), StationLayout.DURATA_CELLA);
            return;
        }
        PieceSlot slot = PieceSlot.values()[selection];
        PieceInfo info = view.pieces().get(slot);
        if (info == null) {
            drawText(graphics, empty(SuitDamage.pieceName(slot)), StationLayout.NAME, StationLayout.TEXT_DIM);
            return;
        }
        PieceCondition condition = condition(info);
        int colour = StationLayout.conditionColor(condition);
        drawText(graphics, itemName(slot), StationLayout.NAME, StationLayout.TEXT);
        drawText(graphics, conditionName(condition), StationLayout.CONDITION, colour);
        drawTextRight(graphics, Component.translatable(DURABILITY_KEY, info.durability(), info.maxDurability()),
                StationLayout.DURABILITY_NUMBERS, StationLayout.TEXT);
        drawBar(graphics, info.durability(), info.maxDurability(), StationLayout.durability(condition));
    }

    /**
     * La barra dell'intestazione: il fondo e, da sinistra, la parte piena in proporzione a {@code value / max}, dal
     * riempimento dato (il colore della condizione, o il rosso della cella).
     */
    private void drawBar(GuiGraphics graphics, int value, int max, Sprite fill) {
        Rect bar = StationLayout.DURABILITY_BAR;
        draw(graphics, StationLayout.DURATA_FONDO, bar);
        int filled = (int) ((long) bar.width() * Math.max(0, Math.min(value, max)) / max);
        if (filled > 0) {
            TEXTURE.blit(graphics, fill.left(filled), leftPos + bar.x0(), topPos + bar.y0());
        }
    }

    /**
     * L'anteprima: la tuta all'angolo trascinato sul fondo dello sfondo, poi in basso nel ritaglio le frecce che dicono
     * che si gira. {@link SuitPreview#drawInGui} rimette pila delle pose, ritaglio e luce anche se qualcosa lancia.
     */
    private void drawPreview(GuiGraphics graphics) {
        Rect clip = StationLayout.PREVIEW_CLIP;
        SuitPreview.drawInGui(graphics, leftPos + clip.x0(), topPos + clip.y0(), leftPos + clip.x1(),
                topPos + clip.y1(), angle, suit, minecraft.player, model);
        draw(graphics, StationLayout.FRECCIA_SINISTRA, StationLayout.ARROW_LEFT);
        draw(graphics, StationLayout.FRECCIA_DESTRA, StationLayout.ARROW_RIGHT);
    }

    /**
     * Il pulsante di prova: le due meta' esterne della faccia di {@link StationLayout#testButton}, larghe
     * {@link StationLayout#TEST_BUTTON_HALF}, e al centro il numero delle pressioni confermate.
     */
    private void drawTestButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int presses = menu.testPresses();
        Rect r = StationLayout.TEST_BUTTON;
        Sprite face = StationLayout.testButton(presses, r.contains(mouseX - leftPos, mouseY - topPos));
        int half = StationLayout.TEST_BUTTON_HALF;
        TEXTURE.blit(graphics, face.part(0, 0, half, r.height()), leftPos + r.x0(), topPos + r.y0());
        TEXTURE.blit(graphics, face.part(face.width() - half, 0, half, r.height()), leftPos + r.x0() + half,
                topPos + r.y0());
        graphics.drawCenteredString(font, String.valueOf(presses), leftPos + (r.x0() + r.x1()) / 2,
                topPos + r.y0() + (r.height() - font.lineHeight) / 2 + 1, StationLayout.TEXT);
    }

    /** Le righe del suggerimento nel punto (x, y), relativo all'angolo della schermata; vuote fuori dai widget. */
    private List<Component> tooltip(StationView view, double x, double y) {
        if (StationLayout.TEST_BUTTON.contains(x, y)) {
            return List.of(Component.translatable(TEST_BUTTON_KEY, menu.testPresses()));
        }
        for (Map.Entry<PieceSlot, Rect> place : StationLayout.PIECE_SLOTS.entrySet()) {
            if (place.getValue().contains(x, y)) {
                return pieceTooltip(view, place.getKey());
            }
        }
        if (StationLayout.CELL.contains(x, y)) {
            Optional<CellInfo> cell = view.cell();
            if (cell.isEmpty()) {
                return List.of(empty(Component.translatable(CELL_KEY)));
            }
            return List.of(Component.translatable(CELL_KEY), Component.translatable(CellItem.TOOLTIP_CHARGE,
                    charge(cell.get()), cell.get().capacity()).withStyle(ChatFormatting.GRAY));
        }
        if (!rotating && StationLayout.PREVIEW_CLIP.contains(x, y)) {
            return List.of(Component.translatable(ROTATE_KEY));
        }
        return List.of();
    }

    /** Il suggerimento di un posto: nome dello slot, nome dell'oggetto, durata, condizione; vuoto: nome e "vuoto". */
    private List<Component> pieceTooltip(StationView view, PieceSlot slot) {
        PieceInfo info = view.pieces().get(slot);
        if (info == null) {
            return List.of(empty(SuitDamage.pieceName(slot)));
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

    /** Il nome dell'oggetto del pezzo montato, per esempio "Torso Rottame"; il nome dello slot se non ha un oggetto. */
    private Component itemName(PieceSlot slot) {
        ItemStack icon = pieceIcons.getOrDefault(slot, ItemStack.EMPTY);
        return icon.isEmpty() ? SuitDamage.pieceName(slot) : icon.getHoverName();
    }

    private void drawText(GuiGraphics graphics, Component text, Rect r, int colour) {
        graphics.drawString(font, text, leftPos + r.x0(), topPos + r.y0(), colour, false);
    }

    private void drawTextRight(GuiGraphics graphics, Component text, Rect r, int colour) {
        graphics.drawString(font, text, leftPos + r.x1() - font.width(text), topPos + r.y0(), colour, false);
    }

    /** Un disegno della texture nel rettangolo della disposizione, relativo all'angolo della schermata. */
    private void draw(GuiGraphics graphics, Sprite sprite, Rect r) {
        TEXTURE.blit(graphics, sprite, leftPos + r.x0(), topPos + r.y0());
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

    private static PieceCondition condition(PieceInfo info) {
        return PieceCondition.of(info.durability(), info.maxDurability());
    }

    /** La carica della cella da mostrare, mai sopra la capacita' (come il suggerimento dell'oggetto). */
    private static int charge(CellInfo cell) {
        return Math.min(cell.stored(), cell.capacity());
    }

    private static MutableComponent conditionName(PieceCondition condition) {
        return Component.translatable(CONDITION_KEY + condition.name().toLowerCase(Locale.ROOT));
    }

    private static Component empty(Component name) {
        return Component.translatable(EMPTY_SLOT_KEY, name);
    }
}
