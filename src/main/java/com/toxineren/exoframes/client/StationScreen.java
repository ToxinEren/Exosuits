package com.toxineren.exoframes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.client.CompatContainerScreen;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.station.StationAction;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.station.StationMenu;
import com.toxineren.exoframes.station.StationTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * La schermata della stazione di manutenzione (design M4 §5.3 e §5.4), con la disposizione di {@link StationLayout} e
 * la grafica nello stile A della texture {@code textures/gui/station.png} (tools/art/gui.py): lo sfondo con i pannelli,
 * le cornici degli slot del giocatore e il fondo dell'anteprima, poi i disegni dei widget nei loro stati. Mostra la
 * vista del telaio che il menu ha dal server ({@link StationMenu#view}):
 * <ul>
 * <li>il pannello del telaio, con i sei posti dei pezzi, la cella e "Tutti" ({@link StationFramePanel});</li>
 * <li>l'intestazione della scelta e la freccia che la smonta ({@link StationHeader});</li>
 * <li>nella riga del titolo, se il telaio custodisce un'armatura, la riga che lo dice (come si ritira lo dice il suo
 * suggerimento: dalla stazione non si ritira);</li>
 * <li>nella riga del titolo, a destra, lo scudo, se il telaio e' rinforzato, e il lucchetto, chiuso o aperto come il
 * telaio e spento se il giocatore non puo' cambiarlo (design §5.11);</li>
 * <li>l'anteprima 3D della tuta del telaio ({@link SuitPreview}, con la vista di {@link StationLayout#suitView}), che
 * trascinando con il tasto sinistro nel ritaglio gira di {@link StationLayout#DEGREES_PER_PIXEL} gradi per pixel, con
 * le due frecce in basso; l'angolo vive nella schermata e parte da 0, di fronte, a ogni apertura, e al server non va
 * nulla;</li>
 * <li>l'inventario del giocatore, gli slot del menu, che funzionano come nel gioco base.</li>
 * </ul>
 * I clic sui widget (design §5.3 e §5.6): sui posti, su "Tutti", sulla freccia e sul lucchetto il tasto sinistro manda
 * al server il numero che dicono {@link StationLayout#placeButton} (dal posto, dall'oggetto sul cursore e dalle
 * maiuscole), {@link StationLayout#unmountButton} e, per il lucchetto, la vista (26 solo con {@code canToggleLock}),
 * come i menu del gioco base: prima il menu del client (che predice la scelta), poi il pacchetto dei pulsanti. Il
 * server ricontrolla tutto, e ogni cambio che si vede, salvo la scelta, arriva con la vista: anche il lucchetto, che il
 * client non predice. Un tasto premuto su un widget non va al gioco base, e nemmeno il suo trascinamento e il suo
 * rilascio: il gioco base, al rilascio, getterebbe la pila del cursore (fuori dalla schermata) o la poserebbe in uno
 * slot, e in creativa il pezzo appena montato e' ancora sul cursore. Passando il mouse su un posto, sulla freccia,
 * sulla riga dell'armatura, sul lucchetto, sullo scudo o sull'anteprima compare il suggerimento; come per gli slot del
 * gioco base, non con un oggetto sul cursore.
 */
public final class StationScreen extends CompatContainerScreen<StationMenu> {
    /** La texture della schermata, scritta da tools/art/gui.py: i posti dei disegni sono in {@link StationLayout}. */
    static final ResourceLocation TEXTURE_ID = Ids.of("textures/gui/station.png");
    /** Il disegno della texture della schermata, per lei e per i suoi pannelli. */
    static final GuiTexture TEXTURE = new GuiTexture(TEXTURE_ID, StationLayout.TEXTURE_W, StationLayout.TEXTURE_H);

    /** I widget della schermata che prendono il clic. */
    private enum Widget {
        /** Un posto del pannello del telaio: un pezzo o la cella. */
        PLACE,
        /** Il pulsante "Tutti". */
        ALL,
        /** La freccia che smonta la scelta. */
        UNMOUNT,
        /** Il lucchetto, che blocca o sblocca il telaio. */
        LOCK
    }

    /** Il modello umanoide a riposo che da' la posa alle ossa della tuta; non si disegna mai. */
    private HumanoidModel<LivingEntity> model;
    /** L'angolo dell'anteprima in gradi, in [0, 360): 0 di fronte. */
    private float angle;
    /** Vero mentre il tasto sinistro, premuto nel ritaglio dell'anteprima, gira la tuta. */
    private boolean rotating;
    /** Dove il tasto sinistro e' stato premuto nel ritaglio (ascissa dell'interfaccia), e l'angolo di allora. */
    private double rotationStartX;
    private float rotationStartAngle;
    /**
     * I tasti del mouse premuti su un widget e non ancora lasciati, un bit per tasto ({@link #bit}): il trascinamento e
     * il rilascio di ciascuno non vanno al gioco base, anche se nel frattempo un altro tasto e' stato premuto altrove.
     */
    private int widgetButtons;
    /** Il widget su cui e' tenuto il tasto sinistro, che lo mostra premuto; null altrimenti. */
    private Widget leftPressed;

    /** La vista che la schermata mostra: quando il menu ne ha una nuova si rifanno la tuta e le icone. */
    private StationView shown;
    /** La tuta dell'anteprima, dalla vista mostrata. */
    private SuitView suit;
    private StationFramePanel framePanel;
    private StationHeader header;

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
        framePanel = new StationFramePanel(font);
        header = new StationHeader(font, framePanel);
        shown = null;
    }

    /**
     * Lo sfondo della texture (la schermata con i pannelli, le cornici degli slot dell'inventario e il fondo
     * dell'anteprima), poi il pannello del telaio, l'intestazione della scelta con la freccia, nella riga del titolo lo
     * scudo e il lucchetto, e l'anteprima con le frecce. La fusione accesa per i disegni si spegne in un
     * {@code finally}.
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        StationView view = refresh();
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        try {
            TEXTURE.blit(graphics, StationLayout.FONDO, leftPos, topPos);
            framePanel.draw(graphics, leftPos, topPos, view, pressed(Widget.ALL),
                    StationLayout.ALL_BUTTON.contains(x, y));
            header.draw(graphics, leftPos, topPos, view, pressed(Widget.UNMOUNT), StationLayout.UNMOUNT.contains(x, y));
            drawShieldAndLock(graphics, view, StationLayout.LOCK.contains(x, y));
            drawPreview(graphics);
        } finally {
            GuiTexture.endBlend();
        }
    }

    /**
     * Lo scudo, solo con il telaio rinforzato, e il lucchetto, chiuso per il telaio bloccato e aperto per quello
     * sbloccato, spento se il giocatore non puo' cambiarlo; {@code lockHover} vero con il mouse sul lucchetto. Tutto
     * dalla vista: il lucchetto cambia solo quando arriva quella del server.
     */
    private void drawShieldAndLock(GuiGraphics graphics, StationView view, boolean lockHover) {
        if (view.reinforced()) {
            TEXTURE.blit(graphics, StationLayout.RINFORZATO, leftPos + StationLayout.SHIELD.x0(),
                    topPos + StationLayout.SHIELD.y0());
        }
        StationLayout.ButtonState state = StationLayout.buttonState(view.canToggleLock(), pressed(Widget.LOCK),
                lockHover);
        TEXTURE.blit(graphics, StationLayout.lockSprite(!view.unlocked(), state), leftPos + StationLayout.LOCK.x0(),
                topPos + StationLayout.LOCK.y0());
    }

    /**
     * Il titolo e l'etichetta dell'inventario, nel colore del testo (il grigio del gioco base non si legge qui), e,
     * se il telaio custodisce un'armatura, la riga che lo dice.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, StationLayout.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, StationLayout.TEXT, false);
        if (menu.view().hasStowed()) {
            graphics.drawString(font, Component.translatable(StationTexts.HOLDS_ARMOR), StationLayout.STOWED.x0(),
                    StationLayout.STOWED.y0(), StationLayout.TEXT, false);
        }
    }

    /** Il suggerimento del widget sotto il mouse, con il cursore vuoto. */
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
     * Un tasto premuto su un widget (un posto, "Tutti", la freccia, il lucchetto) resta della schermata fino al suo
     * rilascio, e non va al gioco base; se e' il sinistro, manda il numero del widget ({@link #act}). Il tasto sinistro
     * nel ritaglio dell'anteprima comincia a girare la tuta. Il resto va al gioco base. Ogni tasto si ricorda per conto
     * suo: una pressione fuori dai widget dimentica solo una pressione dello stesso tasto (se il suo rilascio si e'
     * perso, non ruba quello di una pressione andata al gioco base), mentre il rilascio di un tasto tenuto su un widget
     * resta della schermata anche se nel frattempo un altro tasto e' andato al gioco base. Una pressione sinistra
     * smette di girare la tuta.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            rotating = false;
            leftPressed = null;
        }
        Widget widget = widgetAt(x, y);
        if (widget != null) {
            widgetButtons |= bit(button);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                leftPressed = widget;
                act(widget, x, y);
            }
            return true;
        }
        widgetButtons &= ~bit(button);
        if (button == InputConstants.MOUSE_BUTTON_LEFT && StationLayout.PREVIEW_CLIP.contains(x, y)) {
            rotating = true;
            rotationStartX = mouseX;
            rotationStartAngle = angle;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Il trascinamento del tasto premuto su un widget resta della schermata; trascinando con il tasto sinistro premuto
     * nel ritaglio la tuta gira; ogni altro trascinamento va al gioco base, che lo usa per distribuire gli oggetti del
     * cursore negli slot. L'angolo segue la distanza orizzontale dal punto in cui il tasto e' stato premuto, non la
     * somma degli spostamenti {@code dragX}: su 1.21.1 il gioco manda un trascinamento per fotogramma con gli
     * spostamenti accumulati, e il primo comprende anche il movimento fatto prima della pressione nello stesso
     * fotogramma (a pochi fotogrammi al secondo la tuta saltava di centinaia di gradi).
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if ((widgetButtons & bit(button)) != 0) {
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && rotating) {
            angle = StationLayout.rotate(rotationStartAngle, mouseX - rotationStartX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Il rilascio del tasto premuto su un widget, o del tasto sinistro che chiude una rotazione, non va al gioco base:
     * nel gioco base e' il rilascio a posare l'oggetto del cursore, quindi lascerebbe cadere la pila (rilascio fuori
     * dalla schermata) o la poserebbe nello slot sotto il mouse. La pressione non era andata al gioco base
     * ({@link #mouseClicked}), che non ha nulla in sospeso da chiudere. Ogni altro rilascio va al gioco base.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if ((widgetButtons & bit(button)) != 0) {
            widgetButtons &= ~bit(button);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                leftPressed = null;
            }
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && rotating) {
            rotating = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Il widget nel punto (x, y), relativo all'angolo della schermata; null fuori dai widget. */
    private static Widget widgetAt(double x, double y) {
        if (StationLayout.placeAt(x, y).isPresent()) {
            return Widget.PLACE;
        }
        if (StationLayout.ALL_BUTTON.contains(x, y)) {
            return Widget.ALL;
        }
        if (StationLayout.UNMOUNT.contains(x, y)) {
            return Widget.UNMOUNT;
        }
        return StationLayout.LOCK.contains(x, y) ? Widget.LOCK : null;
    }

    /** Vero mentre il tasto sinistro, premuto sul widget, e' tenuto. */
    private boolean pressed(Widget widget) {
        return leftPressed == widget;
    }

    /** Il bit del tasto del mouse in {@link #widgetButtons}; nessuno per un numero che non e' fra 0 e 31. */
    private static int bit(int button) {
        return button >= 0 && button < Integer.SIZE ? 1 << button : 0;
    }

    /**
     * Il clic sinistro sul widget nel punto (x, y): il numero che dice la tabella dei clic, dalla vista del menu, o
     * nulla. Su un posto contano l'oggetto sul cursore ({@link #cursor}) e le maiuscole; "Tutti" sceglie tutti i pezzi;
     * la freccia smonta la scelta, se ha qualcosa da smontare; il lucchetto blocca o sblocca il telaio se la vista dice
     * che il giocatore puo' ({@code canToggleLock}), e il client non lo cambia: lo cambia la vista che manda il server.
     */
    private void act(Widget widget, double x, double y) {
        StationView view = menu.view();
        OptionalInt number = switch (widget) {
            case PLACE -> {
                int place = StationLayout.placeAt(x, y).orElseThrow();
                yield StationLayout.placeButton(place, cursor(place), Screen.hasShiftDown(),
                        StationLayout.mounted(view, place));
            }
            case ALL -> OptionalInt.of(StationAction.button(StationAction.Kind.SELECT_ALL, 0));
            case UNMOUNT -> StationLayout.unmountButton(view);
            case LOCK -> view.canToggleLock() ? OptionalInt.of(StationAction.button(StationAction.Kind.TOGGLE_LOCK, 0))
                    : OptionalInt.empty();
        };
        number.ifPresent(this::send);
    }

    /**
     * Manda il numero come i menu del gioco base: il menu del client prima (che riconosce il numero e predice la
     * scelta), poi il pacchetto dei pulsanti al server.
     */
    private void send(int number) {
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, number)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, number);
        }
    }

    /**
     * Che cosa c'e' sul cursore per il posto dato: niente, un pezzo che si monta in quello slot, una cella, o altro (un
     * pezzo e' altro per il posto della cella).
     */
    private StationLayout.Cursor cursor(int place) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            return StationLayout.Cursor.EMPTY;
        }
        if (place != StationLayout.CELL_PLACE && carried.getItem() instanceof PieceItem piece
                && piece.kind().slots().contains(PieceSlot.values()[place])) {
            return StationLayout.Cursor.FITTING_PIECE;
        }
        return carried.getItem() instanceof CellItem ? StationLayout.Cursor.CELL : StationLayout.Cursor.OTHER;
    }

    /** La vista del menu, con la tuta dell'anteprima e le icone rifatte se e' cambiata dall'ultima volta. */
    private StationView refresh() {
        StationView view = menu.view();
        if (view != shown) {
            shown = view;
            suit = StationLayout.suitView(view);
            framePanel.update(view);
        }
        return view;
    }

    /**
     * L'anteprima: la tuta all'angolo trascinato sul fondo dello sfondo, poi in basso nel ritaglio le frecce che dicono
     * che si gira. {@link SuitPreview#drawInGui} rimette pila delle pose, ritaglio e luce anche se qualcosa lancia.
     */
    private void drawPreview(GuiGraphics graphics) {
        Rect clip = StationLayout.PREVIEW_CLIP;
        SuitPreview.drawInGui(graphics, leftPos + clip.x0(), topPos + clip.y0(), leftPos + clip.x1(),
                topPos + clip.y1(), angle, suit, minecraft.player, model);
        TEXTURE.blit(graphics, StationLayout.FRECCIA_SINISTRA, leftPos + StationLayout.ARROW_LEFT.x0(),
                topPos + StationLayout.ARROW_LEFT.y0());
        TEXTURE.blit(graphics, StationLayout.FRECCIA_DESTRA, leftPos + StationLayout.ARROW_RIGHT.x0(),
                topPos + StationLayout.ARROW_RIGHT.y0());
    }

    /**
     * Le righe del suggerimento nel punto (x, y), relativo all'angolo della schermata; vuote fuori dai widget. Il
     * lucchetto ha le righe di {@link StationLayout#lockTooltipKeys}, la prima nel colore del testo e le altre in
     * grigio; lo scudo, se c'e', dice che il telaio e' rinforzato.
     */
    private List<Component> tooltip(StationView view, double x, double y) {
        List<Component> lines = new ArrayList<>(framePanel.tooltip(view, x, y));
        lines.addAll(header.tooltip(x, y));
        if (view.hasStowed() && StationLayout.STOWED.contains(x, y)) {
            lines.add(Component.translatable(StationTexts.HOLDS_ARMOR_HOW));
        }
        if (StationLayout.LOCK.contains(x, y)) {
            List<String> keys = StationLayout.lockTooltipKeys(view);
            lines.add(Component.translatable(keys.get(0)));
            for (String key : keys.subList(1, keys.size())) {
                lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        }
        if (view.reinforced() && StationLayout.SHIELD.contains(x, y)) {
            lines.add(Component.translatable(StationTexts.REINFORCED));
        }
        if (!rotating && StationLayout.PREVIEW_CLIP.contains(x, y)) {
            lines.add(Component.translatable(StationTexts.ROTATE));
        }
        return lines;
    }
}
