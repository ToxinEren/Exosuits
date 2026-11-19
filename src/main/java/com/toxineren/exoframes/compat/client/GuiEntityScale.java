package com.toxineren.exoframes.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * La scala (s, s, -s) di un'entita' disegnata nell'interfaccia, applicata esattamente come la applica il gioco base in
 * {@code InventoryScreen.renderEntityInInventory} su ogni versione: cosi' le normali restano quelle che la luce da
 * inventario di quella versione ({@code Lighting.setupForEntityInInventory}) si aspetta. Il gioco base la applica in
 * modo diverso su 1.20.1 e su 1.21.1, e le normali cambiano con il modo (vedi docs/compat.md). Solo client.
 */
public interface GuiEntityScale {
    /**
     * Moltiplica {@code pose} per la scala (scale, scale, -scale), come il gioco base fa per un'entita'
     * nell'inventario: la z ribaltata porta verso chi guarda cio' che nel mondo sta davanti.
     */
    void apply(PoseStack pose, float scale);
}
