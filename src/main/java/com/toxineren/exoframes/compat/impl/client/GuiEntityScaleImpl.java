package com.toxineren.exoframes.compat.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.compat.client.GuiEntityScale;
//? if <1.21 {
/*import org.joml.Matrix4f;
*///?}

/**
 * La scala (s, s, -s) di un'entita' nell'interfaccia, come {@code InventoryScreen.renderEntityInInventory} su ogni
 * versione.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>1.20.1: il gioco base moltiplica la posa per {@code new Matrix4f().scaling(s, s, -s)} con
 * {@code PoseStack.mulPoseMatrix}, che lascia stare le normali, e la sua luce da inventario
 * ({@code INVENTORY_DIFFUSE_LIGHT_0} = (0,2, -1, -1)) le vuole cosi'. {@code PoseStack.scale} di 1.20.1 non va bene al
 * suo posto: con una scala non uniforme moltiplica le normali per {@code Mth.fastInvCubeRoot} del prodotto degli
 * inversi, che per un prodotto negativo da' un valore senza senso, e le normali escono ribaltate in z e
 * lunghissime: la tuta, con la luce di 1.20.1, esce piu' scura.</li>
 * <li>1.21.1: il gioco base usa {@code PoseStack.scale(s, s, -s)}, che ribalta la z delle normali, e la sua luce da
 * inventario ({@code INVENTORY_DIFFUSE_LIGHT_0} = (0,2, -1, 1)) le vuole cosi'; {@code mulPoseMatrix} li' si chiama
 * {@code mulPose(Matrix4f)}.</li>
 * </ul>
 */
public final class GuiEntityScaleImpl implements GuiEntityScale {
    @Override
    public void apply(PoseStack pose, float scale) {
        //? if >=1.21 {
        pose.scale(scale, scale, -scale);
        //?} else {
        /*pose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
        *///?}
    }
}
