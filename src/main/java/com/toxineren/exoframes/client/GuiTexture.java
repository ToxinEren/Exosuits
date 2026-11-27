package com.toxineren.exoframes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Una texture dell'interfaccia della mod generata da tools/art/gui.py (parte 4d): la schermata della stazione
 * ({@code textures/gui/station.png}) e i disegni dell'HUD ({@code textures/gui/hud.png}), con la loro grandezza, e il
 * disegno di un suo {@link Sprite} a scala 1. Solo client.
 *
 * @param id     la risorsa della texture
 * @param width  la larghezza della texture
 * @param height l'altezza della texture
 */
record GuiTexture(ResourceLocation id, int width, int height) {
    /**
     * Disegna il disegno con l'angolo in alto a sinistra in {@code (x, y)} dell'interfaccia, a scala 1, con la
     * fusione accesa: {@code blit} non la accende, e i disegni hanno texel traslucidi (la piastra dell'HUD, i veli, i
     * pezzi non montati della sagoma) e trasparenti (gli angoli dei pulsanti, gli anelli). La fusione si accende a ogni
     * disegno perche' il testo, disegnato fra un disegno e l'altro, la spegne quando si scarica; chi disegna la spegne
     * alla fine ({@link #endBlend}), in un {@code finally}.
     */
    void blit(GuiGraphics graphics, Sprite sprite, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(id, x, y, sprite.u(), sprite.v(), sprite.width(), sprite.height(), width, height);
    }

    /** Spegne la fusione accesa dai {@link #blit}: alla fine del disegno, in un {@code finally}. */
    static void endBlend() {
        RenderSystem.disableBlend();
    }
}
