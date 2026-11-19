package com.toxineren.exoframes.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.toxineren.exoframes.compat.client.SuitView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;

/**
 * La tuta disegnata fuori dal giocatore: la trasformazione dallo spazio di un'entita' allo spazio del modello del
 * giocatore, in comune con il telaio piazzato ({@link ExoframeEntityRenderer}), e il disegno in un riquadro
 * dell'interfaccia, l'anteprima della stazione (design §5.4), con {@link SuitDraw}, l'unico disegno della tuta. Il
 * disegno nell'interfaccia ripete i passi del gioco base per un'entita' nell'inventario
 * ({@code InventoryScreen.renderEntityInInventory}): traslazione, scala con la z ribaltata, ribaltamento attorno a z,
 * luce da inventario, luce piena, {@code flush}. Con una differenza su 1.20.1: li' il gioco base applica la scala con
 * {@code mulPoseMatrix}, che lascia stare le normali, mentre qui {@code PoseStack.scale} le ribalta in z come su 1.21.1
 * (dove la luce da inventario se lo aspetta, e su 1.20.1 no): su 1.20.1 la tuta esce piu' scura. Vedi docs/compat.md,
 * "Limiti noti e rinvii".
 */
final class SuitPreview {
    /** Pixel dell'interfaccia per blocco: la tuta armata, alta circa 3,09 blocchi, sta nei 116 pixel del ritaglio. */
    static final float PIXELS_PER_BLOCK = 32f;
    /** Lo spostamento verso chi guarda, come quello del gioco base per un'entita' nell'inventario. */
    static final float Z_FORWARD = 50f;
    /** I pixel dell'interfaccia fra i piedi della tuta e il bordo inferiore del ritaglio. */
    static final int FEET_MARGIN = 6;

    /** La scala del modello del giocatore in {@code PlayerRenderer.scale}: la tuta indossata la riceve con il giocatore. */
    private static final float PLAYER_MODEL_SCALE = 0.9375f;

    private SuitPreview() {
    }

    /**
     * Porta {@code pose} dallo spazio di un'entita' girata di {@code yawDegrees} (piedi nell'origine, y verso l'alto) a
     * quello del modello del giocatore come lo preparano {@code LivingEntityRenderer} per il corpo e
     * {@code PlayerRenderer.scale} per la scala: rotazione di 180 - yaw attorno alla verticale, ribaltamento, scala
     * 0,9375, piedi a y = {@link SuitDraw#FEET_Y} (y verso il basso). E' lo spazio che {@link SuitDraw#draw} vuole.
     */
    static void frameSpace(PoseStack pose, float yawDegrees) {
        pose.mulPose(Axis.YP.rotationDegrees(180f - yawDegrees));
        pose.scale(-1f, -1f, 1f);
        pose.scale(PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE);
        pose.translate(0f, -SuitDraw.FEET_Y, 0f);
    }

    /**
     * Disegna la tuta della vista {@code view} nel riquadro [x0, x1) x [y0, y1) dell'interfaccia, in coordinate assolute,
     * ritagliata al riquadro: i piedi al centro in basso, {@link #FEET_MARGIN} pixel sopra il bordo, {@link
     * #PIXELS_PER_BLOCK} pixel per blocco. Con l'angolo 0 la tuta e' di fronte (il braccio destro a sinistra dello
     * schermo); l'angolo la gira attorno alla verticale. {@code entity} e {@code model} sono quelli che
     * {@link SuitDraw#draw} vuole: un'entita' qualunque, non un animabile di GeckoLib, e il modello umanoide a riposo
     * che da' la posa alle ossa. La pila delle pose, il ritaglio e la luce tornano come erano anche se qualcosa lancia.
     */
    static void drawInGui(GuiGraphics graphics, int x0, int y0, int x1, int y1, float angleDegrees, SuitView view,
            Entity entity, HumanoidModel<?> model) {
        PoseStack pose = graphics.pose();
        graphics.enableScissor(x0, y0, x1, y1);
        pose.pushPose();
        try {
            // i piedi al centro in basso del ritaglio, davanti a cio' che c'e' gia'
            pose.translate((x0 + x1) / 2f, (float) (y1 - FEET_MARGIN), Z_FORWARD);
            pose.scale(PIXELS_PER_BLOCK, PIXELS_PER_BLOCK, -PIXELS_PER_BLOCK);
            // come InventoryScreen: l'alto del mondo in alto sullo schermo
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            // angolo 0: di fronte, come il corpo di un'entita' nell'inventario del gioco base
            frameSpace(pose, 180f - angleDegrees);
            Lighting.setupForEntityInInventory();
            SuitDraw.draw(entity, model, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, 0f, view, 0f);
            // dentro il ritaglio: cio' che resta nel buffer lo disegnerebbe senza
            graphics.flush();
        } finally {
            Lighting.setupFor3DItems();
            pose.popPose();
            graphics.disableScissor();
        }
    }
}
