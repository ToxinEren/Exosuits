package com.toxineren.exoframes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Il pilota dentro la tuta (design, par. 4.7 e 5.2): il disegno del giocatore che indossa la tuta si solleva di
 * {@link SuitScale#PILOT_RAISE_PX} pixel del modello, perde le gambe (sempre) e le parti coperte da un pezzo montato,
 * e l'oggetto impugnato passa nella mano della tuta, ingrandito di {@link SuitScale#S}.
 * <p>
 * Il sollevamento e' nello spazio del modello, dopo le rotazioni della posa (sdraiato, nuoto, caduta della morte) e
 * dopo l'attributo {@code scale} di 1.21.1: pilota e tuta (che lo annulla in {@link SuitLayer}, nello stesso spazio)
 * restano allineati tra loro e con il corpo a terra in ogni posa. L'etichetta del nome si disegna fuori da quello
 * spazio e non sale con il pilota.
 * <p>
 * Tutta la logica sta qui. I due mixin client ({@code mixin/client/PlayerRendererMixin} e
 * {@code mixin/client/ItemInHandLayerMixin}) non hanno stato e fanno una sola chiamata per aggancio ai metodi
 * {@code on...} di questa classe; nessuno di questi lancia. La prima persona non cambia.
 */
public final class PilotPose {
    /**
     * Spostamento orizzontale, in pixel del modello, dal perno del braccio classico a riposo del pilota (x = 5 dal
     * centro) a quello della tuta: 5 * (S - 1), verso l'esterno. Vale {@link #handShiftPx} per quel perno.
     */
    static final float ARM_SHIFT_X_PX = 2f;

    /**
     * Spostamento verticale, in pixel del modello con y verso il basso, dal perno del braccio classico a riposo del
     * pilota (22 px dai piedi) a quello della tuta: sale di 22 * (S - 1) = 8,8 px, meno dei 9,6 del pilota. Vale
     * {@link #handShiftPx} per quel perno.
     */
    static final float ARM_SHIFT_Y_PX = 0.8f;

    /** Altezza dei piedi nello spazio del modello del giocatore, in pixel (y verso il basso). */
    private static final float FEET_Y_PX = 24f;

    /** Parti del modello del giocatore che si possono nascondere, ognuna con il suo secondo strato della skin. */
    public enum BodyPart {
        HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEGS
    }

    /** Uno spostamento in pixel del modello del giocatore (y verso il basso). */
    record Shift(float x, float y, float z) {
    }

    private PilotPose() {
    }

    /**
     * Le parti del pilota da non disegnare: le gambe sempre (stanno nelle cosce della tuta), la testa con l'elmo, il
     * busto con il torso, ogni braccio con il suo pezzo. Conta solo la presenza del pezzo, non la sua condizione: un
     * pezzo rotto copre come uno integro. L'insieme restituito non si modifica.
     */
    public static Set<BodyPart> hiddenParts(SuitView view) {
        Set<BodyPart> parts = EnumSet.of(BodyPart.LEGS);
        if (view.pieces().containsKey(PieceSlot.HELMET)) {
            parts.add(BodyPart.HEAD);
        }
        if (view.pieces().containsKey(PieceSlot.TORSO)) {
            parts.add(BodyPart.BODY);
        }
        if (view.pieces().containsKey(PieceSlot.LEFT_ARM)) {
            parts.add(BodyPart.LEFT_ARM);
        }
        if (view.pieces().containsKey(PieceSlot.RIGHT_ARM)) {
            parts.add(BodyPart.RIGHT_ARM);
        }
        return Collections.unmodifiableSet(parts);
    }

    /**
     * Spostamento dal perno di un braccio del pilota, in (x, y, z) pixel del modello, al perno del braccio della tuta.
     * La tuta e' il modello scalato di S attorno ai piedi (y = 24) e abbassato del sollevamento, e il suo osso del
     * braccio segue la posizione del braccio del modello di base: ((S - 1) x, (S - 1)(y - 24) + sollevamento,
     * (S - 1) z). A riposo (perno (+-5, 2, 0)) da' (+-2, 0,8, 0); cambia accovacciati (y = 5,2) e con la skin sottile
     * (y = 2,5). Java puro.
     */
    static Shift handShiftPx(float x, float y, float z) {
        float k = SuitScale.S - 1f;
        return new Shift(k * x, k * (y - FEET_Y_PX) + SuitScale.PILOT_RAISE_PX, k * z);
    }

    /**
     * True se il disegno del giocatore va adattato alla tuta: la indossa ({@link SuitLayer#wearsSuit}) e non e'
     * spettatore (per lo spettatore il gioco non disegna i layer, quindi nemmeno la tuta). Non lancia mai.
     */
    public static boolean applies(AbstractClientPlayer player) {
        return SuitLayer.wearsSuit(player) && !player.isSpectator();
    }

    /**
     * Solleva il disegno del pilota di {@link SuitScale#PILOT_RAISE_PX} pixel, nello spazio del modello (y verso il
     * basso, unita' di 16 pixel): va applicato dopo la scala del modello del giocatore.
     */
    public static void raise(PoseStack pose) {
        pose.translate(0f, -SuitScale.PILOT_RAISE_PX / 16f, 0f);
    }

    /** Spegne le parti date e il loro secondo strato della skin. */
    public static void hide(PlayerModel<?> model, Set<BodyPart> parts) {
        for (BodyPart part : parts) {
            switch (part) {
                case HEAD -> {
                    model.head.visible = false;
                    model.hat.visible = false;
                }
                case BODY -> {
                    model.body.visible = false;
                    model.jacket.visible = false;
                }
                case LEFT_ARM -> {
                    model.leftArm.visible = false;
                    model.leftSleeve.visible = false;
                }
                case RIGHT_ARM -> {
                    model.rightArm.visible = false;
                    model.rightSleeve.visible = false;
                }
                case LEGS -> {
                    model.leftLeg.visible = false;
                    model.rightLeg.visible = false;
                    model.leftPants.visible = false;
                    model.rightPants.visible = false;
                }
            }
        }
    }

    /**
     * Prima di {@code translateToHand}, nello spazio del modello del giocatore: dal perno attuale del braccio del
     * pilota a quello del braccio della tuta ({@link #handShiftPx}).
     */
    public static void beforeHandTransform(PoseStack pose, ModelPart arm) {
        Shift shift = handShiftPx(arm.x, arm.y, arm.z);
        pose.translate(shift.x() / 16f, shift.y() / 16f, shift.z() / 16f);
    }

    /** Dopo {@code translateToHand}: l'oggetto e la sua distanza dal perno crescono come la tuta. */
    public static void afterHandTransform(PoseStack pose) {
        pose.scale(SuitScale.S, SuitScale.S, SuitScale.S);
    }

    // --- Punti d'ingresso dei mixin: una chiamata per aggancio. ---

    /**
     * In coda a {@code PlayerRenderer.scale(AbstractClientPlayer, PoseStack, float)}, che
     * {@code LivingEntityRenderer.render} chiama dentro il proprio push/pop dopo rotazioni e ribaltamento: il
     * sollevamento sparisce con quel pop.
     */
    public static void onScale(AbstractClientPlayer player, PoseStack pose) {
        if (applies(player)) {
            raise(pose);
        }
    }

    /**
     * Subito dopo {@code setModelProperties} in {@code PlayerRenderer.render}, che a ogni fotogramma riaccende le parti
     * del modello: qui si spengono quelle coperte.
     */
    public static void onModelPrepared(AbstractClientPlayer player, PlayerModel<?> model) {
        if (!player.isSpectator()) {
            SuitView view = SuitLayer.suitView(player);
            if (view != null) {
                hide(model, hiddenParts(view));
            }
        }
    }

    /**
     * Prima di {@code translateToHand} in {@code ItemInHandLayer.renderArmWithItem}; {@code model} e' il modello del
     * layer, di cui si legge il braccio che impugna.
     */
    public static void onBeforeHand(LivingEntity entity, EntityModel<?> model, PoseStack pose, HumanoidArm arm) {
        if (model instanceof HumanoidModel<?> humanoid && entity instanceof AbstractClientPlayer player
                && applies(player)) {
            beforeHandTransform(pose, arm == HumanoidArm.LEFT ? humanoid.leftArm : humanoid.rightArm);
        }
    }

    /** Subito dopo {@code translateToHand} in {@code ItemInHandLayer.renderArmWithItem}; stesse condizioni di prima. */
    public static void onAfterHand(LivingEntity entity, EntityModel<?> model, PoseStack pose) {
        if (model instanceof HumanoidModel<?> && entity instanceof AbstractClientPlayer player && applies(player)) {
            afterHandTransform(pose);
        }
    }
}
