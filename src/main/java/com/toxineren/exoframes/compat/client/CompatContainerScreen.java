package com.toxineren.exoframes.compat.client;

import com.toxineren.exoframes.compat.impl.client.ContainerScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Il tipo che le schermate dei menu della mod estendono, uguale sulle due versioni del gioco. Lo sfondo scuro dietro
 * la schermata si disegna in modo diverso su 1.20.1 e su 1.21.1: lo chiude la base in {@code compat/impl/client/}
 * (vedi docs/compat.md). Le sottoclassi disegnano lo sfondo dei loro pannelli in {@code renderBg}, uguale sulle due
 * versioni, e cio' che sta sopra slot e widget in {@code renderContent(graphics, mouseX, mouseY)}, ereditato e vuoto
 * di base; {@code render} e' finale e chiama, nell'ordine, lo sfondo scuro, il disegno del gioco base,
 * {@code renderContent} e il suggerimento dello slot sotto il mouse.
 */
public abstract class CompatContainerScreen<T extends AbstractContainerMenu> extends ContainerScreenBase<T> {
    protected CompatContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
