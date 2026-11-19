package com.toxineren.exoframes.compat.impl.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base delle schermate dei menu della mod: chiude la differenza dello sfondo scuro fra 1.20.1 e 1.21.1 dietro un
 * {@link #render} finale e un metodo neutro per il contenuto. Il codice della mod non la estende direttamente: estende
 * {@code compat.client.CompatContainerScreen}.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>sfondo scuro dietro la schermata: su 1.20.1 {@code Screen.renderBackground(GuiGraphics)}, che nessuno chiama per
 * una sottoclasse di {@link AbstractContainerScreen}: qui lo chiama {@link #render} prima di tutto il resto; su 1.21.1
 * {@code renderBackground(GuiGraphics, int, int, float)}, che {@code Screen.render} chiama da se' (e quello di
 * {@link AbstractContainerScreen} chiama poi {@code renderBg}): chiamarlo di nuovo scurirebbe due volte, quindi qui non
 * si chiama.</li>
 * </ul>
 * Uguali sulle due versioni, e lasciati alle sottoclassi: {@code renderBg(GuiGraphics, float, int, int)}, dove le
 * sottoclassi disegnano lo sfondo dei loro pannelli, {@code renderLabels}, {@code mouseClicked}, {@code mouseDragged},
 * {@code mouseReleased}, {@code imageWidth}, {@code imageHeight}, {@code leftPos}, {@code topPos}. Il {@code float} di
 * {@code render} non e' il partialTick su nessuna delle due versioni.
 */
public abstract class ContainerScreenBase<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected ContainerScreenBase(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /**
     * Disegna cio' che sta sopra slot e widget, per esempio i suggerimenti dei propri pulsanti: dopo il disegno del
     * gioco base e prima del suggerimento dello slot sotto il mouse. Di base nulla.
     */
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    /**
     * Il disegno della schermata, nell'ordine: lo sfondo scuro dove la versione lo vuole, il disegno del gioco base
     * (lo sfondo dei pannelli con {@code renderBg}, i widget, gli slot, le etichette, l'oggetto sul cursore),
     * {@link #renderContent} e il suggerimento dello slot sotto il mouse.
     */
    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, delta);
        renderContent(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
