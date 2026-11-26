package com.toxineren.exoframes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.client.HudLayout.Rect;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameIndicators;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.frame.HudLevel;
import com.toxineren.exoframes.frame.SuitSize;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * L'HUD della tuta (design M4 §5.12): al posto della riga dell'energia di M3, per chi indossa la tuta, un gruppo
 * nell'angolo in basso opposto alla mano principale ({@link HudLayout}) con la barra dell'energia, la percentuale, le
 * celle di scorta e l'avviso e, con un elmo che funziona, la sagoma dei sei pezzi colorata per condizione e, in prima
 * persona, la vignetta del casco nei quattro angoli dello schermo. Che cosa disegnare lo decide {@link HudModel},
 * calcolato una volta per tick del client (il partialTick non serve: l'HUD cambia a tick) dalla copia dello stato del
 * server ({@code clientView}) e dagli indicatori che il server manda ({@code clientIndicators}): il client non
 * confronta la carica con soglie sue (DEC-23). Si disegna dal gancio dell'HUD di {@code ClientRenderHooks} a ogni
 * fotogramma, con rettangoli pieni, testi e la texture della vignetta, tutto a scala 1, senza toccare la pila delle
 * pose; nulla con l'interfaccia nascosta (F1), con l'HUD nascosto da H ({@link ClientPrefs}), da spettatore. Sagoma,
 * avviso, icona della cella e texture della vignetta sono segnaposto fino alla grafica della parte 4d (Decisioni 4 e
 * 14 del piano). Una {@code RuntimeException} nel tick o nel disegno non chiude il gioco: quel tick l'HUD sparisce, o
 * quel fotogramma non si disegna, con un ERROR una volta per sessione ciascuno (si azzerano all'uscita dal mondo), e
 * la fusione accesa per la vignetta si spegne comunque. Solo client. {@link #register()} gira una volta durante
 * l'init della mod, che su Forge e NeoForge e' un thread del caricamento delle mod; tutto il resto (tick, disegno,
 * azzeramento e lo stato che condividono) gira sul thread del client.
 */
final class SuitHud {
    /** La percentuale dell'energia, con il numero come argomento. */
    static final String PERCENT = "hud." + RedstoneExoframes.MOD_ID + ".percent";
    /** Il testo al posto della barra di un telaio senza cella (Decisione 1 del piano). */
    static final String NO_CELL = "hud." + RedstoneExoframes.MOD_ID + ".no_cell";
    /** Il segno davanti al numero delle celle di scorta: il segno di moltiplicazione (U+00D7). */
    private static final String TIMES = "\u00d7";
    /**
     * La texture della vignetta del casco, generata da tools/gen_placeholders.py: i quattro angoli di
     * {@link HudLayout#VIGNETTE_W} x {@link HudLayout#VIGNETTE_H} gia' al loro posto, nell'ordine di
     * {@link HudLayout#vignetteCorners} due per riga (alto a sinistra, alto a destra; basso a sinistra, basso a
     * destra), cosi' il disegno non specchia nulla (Decisione 4 del piano).
     */
    static final ResourceLocation VIGNETTE_TEXTURE = Ids.of("textures/gui/hud_vignette.png");
    private static final int VIGNETTE_TEXTURE_W = 2 * HudLayout.VIGNETTE_W;
    private static final int VIGNETTE_TEXTURE_H = 2 * HudLayout.VIGNETTE_H;

    /** Il modello dell'ultimo tick: quello che il disegno mostra fino al tick dopo. */
    private static HudModel model = HudModel.NONE;
    /** Lo stato dell'energia del tick prima, per il lampeggio dell'avviso. */
    private static Optional<EnergyState> energyBefore = Optional.empty();
    /** I tick passati nello stato dell'energia di adesso ({@link HudModel#ticksInState}). */
    private static long ticksInState;
    /** Vero dopo il primo ERROR della sessione nel tick. */
    private static boolean updateFailureLogged;
    /** Vero dopo il primo ERROR della sessione nel disegno. */
    private static boolean drawFailureLogged;

    private SuitHud() {
    }

    /**
     * Aggancia il disegno all'HUD del gioco e il calcolo del modello al tick del client, e l'azzeramento all'uscita
     * dal mondo. Una volta, da {@link RedstoneExoframesClient#init()}: durante l'init della mod, su Forge e NeoForge
     * in un thread del caricamento delle mod.
     */
    static void register() {
        ClientCompat.renderHooks().addHudRenderer(SuitHud::render);
        ClientTickEvent.CLIENT_POST.register(SuitHud::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> reset());
    }

    /**
     * Il modello di questo tick: {@link HudModel#NONE} senza giocatore, altrimenti dal livello
     * ({@link HudLevel#of}), dallo stato dell'energia del server e dai tick passati in quello stato, e dalle celle di
     * scorta, contate solo se l'HUD ha qualcosa da disegnare.
     */
    private static void tick(Minecraft minecraft) {
        try {
            LocalPlayer player = minecraft.player;
            if (player == null) {
                model = HudModel.NONE;
                return;
            }
            Optional<FrameState> view = Compat.frameState().clientView(player);
            HudLevel level = HudLevel.of(SuitSize.wearsSuit(player), view);
            Optional<EnergyState> energy = Compat.frameState().clientIndicators(player)
                    .map(FrameIndicators::energyState);
            ticksInState = HudModel.ticksInState(energyBefore, energy, ticksInState);
            energyBefore = energy;
            int spareCells = level == HudLevel.NONE ? 0 : spareCells(player);
            model = HudModel.of(level, view.orElse(null), energy, spareCells, ticksInState);
        } catch (RuntimeException e) {
            model = HudModel.NONE;
            if (!updateFailureLogged) {
                updateFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Suit HUD update failed with {}; the HUD is hidden until an update "
                        + "works (logged once per session)", e.getClass().getName(), e);
            }
        }
    }

    /**
     * Le celle di scorta (design §5.12, Decisione 9 del piano): la somma delle pile di celle di redstone con carica
     * maggiore di zero nell'inventario (barra compresa) e nella seconda mano. La cella montata nel telaio non e'
     * nell'inventario e non conta.
     */
    private static int spareCells(Player player) {
        Inventory inventory = player.getInventory();
        return chargedCells(inventory.items) + chargedCells(inventory.offhand);
    }

    private static int chargedCells(List<ItemStack> stacks) {
        int cells = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.REDSTONE_CELL.get()) && Compat.itemData().cellEnergy(stack) > 0) {
                cells += stack.getCount();
            }
        }
        return cells;
    }

    /**
     * Disegna il modello dell'ultimo tick: per prima, con {@link HudLevel#FULL}, in prima persona e con la preferenza
     * accesa ({@link ClientPrefs#vignette()}), la vignetta, sotto tutto il resto; poi, nell'angolo della mano
     * principale di adesso, la piastra del livello, con {@link HudLevel#FULL} la sagoma, poi il sottogruppo
     * dell'energia.
     */
    private static void render(GuiGraphics graphics) {
        try {
            HudModel shown = model;
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (shown.level() == HudLevel.NONE || minecraft.options.hideGui || ClientPrefs.hudHidden()
                    || player == null || player.isSpectator()) {
                return;
            }
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();
            if (shown.level() == HudLevel.FULL && minecraft.options.getCameraType().isFirstPerson()
                    && ClientPrefs.vignette()) {
                drawVignette(graphics, w, h);
            }
            boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
            framed(graphics, HudLayout.plate(w, h, mainRight, shown.level()), HudModel.PLATE_EDGE, HudModel.PLATE);
            if (shown.level() == HudLevel.FULL) {
                drawSilhouette(graphics, HudLayout.silhouetteGroup(w, h, mainRight), shown);
            }
            drawEnergy(graphics, minecraft.font, HudLayout.energyGroup(w, h, mainRight), shown);
        } catch (RuntimeException e) {
            if (!drawFailureLogged) {
                drawFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Suit HUD drawing failed with {}; the HUD is skipped for this frame "
                        + "(logged once per session)", e.getClass().getName(), e);
            }
        }
    }

    /**
     * La vignetta del casco (design M4 §5.12, "Vignetta"): i quattro angoli della texture, ciascuno nel suo angolo
     * dell'interfaccia ({@link HudLayout#vignetteCorners}), a scala 1. {@code blit} non accende la fusione e la texture
     * e' traslucida: la fusione si accende qui e si spegne in un {@code finally}, cosi' un'eccezione fra un
     * {@code blit} e l'altro non la lascia accesa per cio' che si disegna dopo. Il gancio dell'HUD disegna dopo tutto
     * l'HUD del gioco base, e la differenza fra i loader e' voluta (design M4 §5.12 e §10): solo su NeoForge 1.21.1
     * gli elementi del gioco base che toccano gli angoli (le icone degli effetti in alto a destra, l'indicatore del
     * salvataggio e i sottotitoli in basso a destra) stanno sopra la vignetta, perche' NeoForge manda il suo evento
     * dentro il gestore degli strati, con il test di profondita' acceso, e gli strati del gioco base stanno a z piu'
     * alte; su Fabric 1.21.1, dove Fabric API lo manda alla fine di {@code Gui.render}, dopo che il test di profondita'
     * si e' spento, e sulle due 1.20.1 la vignetta copre le icone degli effetti, i sottotitoli e l'indicatore del
     * salvataggio, che con Forge 1.20.1 non si disegna affatto ({@code ForgeGui} non chiama il {@code Gui.render} del
     * gioco base, l'unico che lo disegna). Gli avvisi in alto a destra (i toast) stanno sopra su tutte e quattro le
     * varianti: il gioco base li disegna dopo l'HUD e dopo le schermate ({@code GameRenderer.render}).
     */
    private static void drawVignette(GuiGraphics graphics, int w, int h) {
        List<Rect> corners = HudLayout.vignetteCorners(w, h);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            for (int i = 0; i < corners.size(); i++) {
                Rect corner = corners.get(i);
                // nella texture l'angolo i sta nella colonna i % 2 e nella riga i / 2
                graphics.blit(VIGNETTE_TEXTURE, corner.x0(), corner.y0(), i % 2 * HudLayout.VIGNETTE_W,
                        i / 2 * HudLayout.VIGNETTE_H, HudLayout.VIGNETTE_W, HudLayout.VIGNETTE_H, VIGNETTE_TEXTURE_W,
                        VIGNETTE_TEXTURE_H);
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    /** I sei pezzi, ciascuno nel suo rettangolo del sottogruppo della sagoma: il contorno e dentro il pieno. */
    private static void drawSilhouette(GuiGraphics graphics, Rect group, HudModel shown) {
        for (Map.Entry<PieceSlot, HudModel.Paint> piece : shown.silhouette().entrySet()) {
            HudModel.Paint paint = piece.getValue();
            framed(graphics, HudLayout.piece(piece.getKey()).offset(group.x0(), group.y0()), paint.outline(),
                    paint.fill());
        }
    }

    /**
     * Il sottogruppo dell'energia: l'avviso se acceso, la percentuale (dopo il posto dell'avviso, se c'e') o, senza
     * cella, il testo al posto della barra; l'icona della cella e "xN" allineati a destra; la barra.
     */
    private static void drawEnergy(GuiGraphics graphics, Font font, Rect group, HudModel shown) {
        int x = group.x0();
        int y = group.y0();
        int colour = shown.energyColor();
        if (shown.warningOn()) {
            drawWarning(graphics, x + HudLayout.WARNING.x0(), y + HudLayout.WARNING.y0(), colour);
        }
        if (shown.hasCell()) {
            int textX = shown.warningSlot() ? HudLayout.TEXT_X_AFTER_WARNING : HudLayout.TEXT_X;
            graphics.drawString(font, Component.translatable(PERCENT, shown.percent()), x + textX,
                    y + HudLayout.TEXT_Y, colour, true);
        } else {
            graphics.drawString(font, Component.translatable(NO_CELL), x + HudLayout.NO_CELL_X,
                    y + HudLayout.NO_CELL_Y, HudModel.GREY, true);
        }
        Component spare = Component.literal(TIMES + shown.spareCells());
        int spareX = x + HudLayout.spareTextX(font.width(spare));
        drawCellIcon(graphics, spareX - 1 - HudLayout.CELL_ICON_W, y + HudLayout.TEXT_Y);
        graphics.drawString(font, spare, spareX, y + HudLayout.TEXT_Y, shown.spareColor(), true);
        if (shown.hasCell()) {
            framed(graphics, HudLayout.BAR_FRAME.offset(x, y), HudModel.BAR_EDGE, HudModel.BAR_BACK);
            if (shown.fillWidth() > 0) {
                Rect fill = HudLayout.BAR_FILL.offset(x, y);
                int fillEnd = fill.x0() + shown.fillWidth();
                graphics.fill(fill.x0(), fill.y0(), fillEnd, fill.y0() + 1, HudModel.lighter(colour));
                graphics.fill(fill.x0(), fill.y0() + 1, fillEnd, fill.y1(), colour);
            }
        }
    }

    /**
     * L'avviso, 8x8 da (x, y), nel colore dell'energia: le righe del triangolo pieno, dalla colonna {@code 3 - riga/2}
     * alla {@code 4 + riga/2}, senza le colonne 3 e 4 delle righe 2, 3, 4 e 6, dove il punto esclamativo resta del
     * colore della piastra.
     */
    private static void drawWarning(GuiGraphics graphics, int x, int y, int colour) {
        for (int row = 0; row < HudLayout.WARNING.height(); row++) {
            int first = 3 - row / 2;
            int last = 4 + row / 2;
            if (row == 2 || row == 3 || row == 4 || row == 6) {
                graphics.fill(x + first, y + row, x + 3, y + row + 1, colour);
                graphics.fill(x + 5, y + row, x + last + 1, y + row + 1, colour);
            } else {
                graphics.fill(x + first, y + row, x + last + 1, y + row + 1, colour);
            }
        }
    }

    /** L'icona della cella, 5x8 da (x, y): il tappo nelle colonne 1-3 della riga 0, il corpo, la carica. */
    private static void drawCellIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y, x + 4, y + 1, HudModel.CELL_CAP);
        graphics.fill(x, y + 1, x + HudLayout.CELL_ICON_W, y + HudLayout.CELL_ICON_H, HudModel.CELL_BODY);
        graphics.fill(x + 1, y + 3, x + 4, y + 7, HudModel.CELL_CHARGE);
    }

    /**
     * Il rettangolo con il bordo di 1 pixel nel colore {@code edge} e dentro il colore {@code inside}, senza
     * sovrapporli: con un colore traslucido (la piastra, un pezzo non montato) nessun pixel si fonde due volte.
     */
    private static void framed(GuiGraphics graphics, Rect r, int edge, int inside) {
        graphics.fill(r.x0(), r.y0(), r.x1(), r.y0() + 1, edge);
        graphics.fill(r.x0(), r.y1() - 1, r.x1(), r.y1(), edge);
        graphics.fill(r.x0(), r.y0() + 1, r.x0() + 1, r.y1() - 1, edge);
        graphics.fill(r.x1() - 1, r.y0() + 1, r.x1(), r.y1() - 1, edge);
        graphics.fill(r.x0() + 1, r.y0() + 1, r.x1() - 1, r.y1() - 1, inside);
    }

    /** Dimentica lo stato dell'energia, i tick passati in lui e i due ERROR gia' scritti. All'uscita dal mondo. */
    private static void reset() {
        energyBefore = Optional.empty();
        ticksInState = 0;
        updateFailureLogged = false;
        drawFailureLogged = false;
    }
}
