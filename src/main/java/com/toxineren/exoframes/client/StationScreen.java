package com.toxineren.exoframes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.client.StationLayout.Rect;
import com.toxineren.exoframes.compat.client.CompatContainerScreen;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.station.StationMenu;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

/**
 * La schermata della stazione di manutenzione (design §5.3), per ora minima: lo sfondo, il pannello dell'anteprima
 * con una tuta fissa disegnata da {@link SuitPreview}, e il pulsante di prova, che va al server e torna (design §6, "Il
 * primo task della 4a"). Il pulsante e' un riquadro grigio finche' il server non ha confermato nessuna pressione,
 * verde dopo la prima, con dentro il numero delle pressioni confermate ({@link StationMenu#testPresses}), piu' chiaro
 * sotto il mouse, con il suo suggerimento. Il clic sul pulsante manda soltanto il numero al server: la schermata non
 * cambia nulla da se', e il cambio che si vede viene dal server.
 */
public final class StationScreen extends CompatContainerScreen<StationMenu> {
    /** Il suggerimento del pulsante di prova, con il numero delle pressioni confermate. */
    private static final String TEST_BUTTON_KEY = "gui." + RedstoneExoframes.MOD_ID + ".station.test_button";

    /**
     * Il magenta del braccio destro della tuta fissa: il colore che nient'altro sullo schermo ha, e che i controlli di
     * tools/client/m4a1_shots.sh cercano nell'anteprima.
     */
    private static final int MAGENTA = 0xFF00FF;

    /**
     * La tuta fissa dell'anteprima: i sei pezzi Rottame intatti, cinque con i colori del livello e il braccio destro
     * magenta, che di fronte sta a sinistra dello schermo: cosi' gli scatti dicono dove e' la tuta e da che parte guarda.
     */
    private static final SuitView FIXED_VIEW = fixedView();

    /** Il modello umanoide a riposo che da' la posa alle ossa della tuta; non si disegna mai. */
    private HumanoidModel<LivingEntity> model;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = StationLayout.WIDTH;
        this.imageHeight = StationLayout.HEIGHT;
        this.titleLabelX = StationLayout.TITLE.x0();
        this.titleLabelY = StationLayout.TITLE.y0();
    }

    @Override
    protected void init() {
        super.init();
        model = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER));
        // I modelli nascono "giovani": GeckoLib copia il segno e rimpicciolirebbe la tuta come quella di un cucciolo.
        model.young = false;
    }

    /** Lo sfondo: la schermata con il suo bordo, il pannello e lo sfondo dell'anteprima, la tuta, il pulsante. */
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + StationLayout.WIDTH, topPos + StationLayout.HEIGHT, StationLayout.EDGE);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + StationLayout.WIDTH - 1, topPos + StationLayout.HEIGHT - 1,
                StationLayout.BACKGROUND);
        fill(graphics, StationLayout.PREVIEW_PANEL, StationLayout.PANEL);
        fill(graphics, StationLayout.PREVIEW_CLIP, StationLayout.PREVIEW_BG);
        Rect clip = StationLayout.PREVIEW_CLIP;
        SuitPreview.drawInGui(graphics, leftPos + clip.x0(), topPos + clip.y0(), leftPos + clip.x1(),
                topPos + clip.y1(), 0f, FIXED_VIEW, minecraft.player, model);
        drawTestButton(graphics, mouseX, mouseY);
    }

    /** Il solo titolo: niente etichetta dell'inventario, che la schermata non ha ancora. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, StationLayout.TEXT, false);
    }

    /** Il suggerimento del pulsante di prova, con il mouse sopra. */
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        if (overTestButton(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(TEST_BUTTON_KEY, menu.testPresses()), mouseX, mouseY);
        }
    }

    /** Il clic sinistro sul pulsante di prova manda il suo numero al server e basta; il resto va al gioco base. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && overTestButton(mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StationMenu.TEST_BUTTON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawTestButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int presses = menu.testPresses();
        int color = presses > 0 ? StationLayout.TEST_DONE : StationLayout.TEST_IDLE;
        fill(graphics, StationLayout.TEST_BUTTON, overTestButton(mouseX, mouseY) ? StationLayout.lighter(color) : color);
        Rect r = StationLayout.TEST_BUTTON;
        graphics.drawCenteredString(font, String.valueOf(presses), leftPos + (r.x0() + r.x1()) / 2,
                topPos + r.y0() + (r.height() - font.lineHeight) / 2 + 1, StationLayout.TEXT);
    }

    /** Riempie un rettangolo della disposizione, relativo all'angolo della schermata. */
    private void fill(GuiGraphics graphics, Rect r, int color) {
        graphics.fill(leftPos + r.x0(), topPos + r.y0(), leftPos + r.x1(), topPos + r.y1(), color);
    }

    private boolean overTestButton(double mouseX, double mouseY) {
        return StationLayout.TEST_BUTTON.contains(mouseX - leftPos, mouseY - topPos);
    }

    private static SuitView fixedView() {
        Map<PieceSlot, SuitView.PieceView> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            boolean marked = slot == PieceSlot.RIGHT_ARM;
            pieces.put(slot, new SuitView.PieceView(0, marked ? MAGENTA : TierPalette.primary(Tier.SCRAP),
                    marked ? MAGENTA : TierPalette.secondary(Tier.SCRAP)));
        }
        return new SuitView(pieces);
    }
}
