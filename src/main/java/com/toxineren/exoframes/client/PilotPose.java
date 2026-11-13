package com.toxineren.exoframes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Il pilota dentro la tuta (design, par. 4.7 e 5.2): il disegno del giocatore che indossa la tuta si solleva di
 * {@link SuitScale#PILOT_RAISE_PX} pixel del modello, perde le gambe (sempre) e le parti coperte da un pezzo montato,
 * e l'oggetto impugnato passa nella mano della tuta, ingrandito di {@link SuitScale#S}.
 * <p>
 * Tutta la logica sta qui. I due mixin client ({@code mixin/client/PlayerRendererMixin} e
 * {@code mixin/client/ItemInHandLayerMixin}) non hanno stato e fanno una sola chiamata per aggancio ai metodi
 * {@code on...} di questa classe; nessuno di questi lancia. La prima persona non cambia.
 */
public final class PilotPose {
    /**
     * Spostamento orizzontale, in pixel del modello, dal perno del braccio del pilota a quello del braccio della tuta:
     * il perno a 5 px dal centro, scalato di S, finisce a 5 * S (verso l'esterno, quindi -x per il braccio destro).
     */
    static final float ARM_SHIFT_X_PX = 2f;

    /**
     * Spostamento verticale, in pixel del modello con y verso il basso, dal perno del braccio del pilota a quello della
     * tuta: il perno a 22 px dai piedi, scalato di S, sale di 22 * (S - 1) = 8,8 px, meno dei 9,6 del pilota.
     */
    static final float ARM_SHIFT_Y_PX = 0.8f;

    /** Parti del modello del giocatore che si possono nascondere, ognuna con il suo secondo strato della skin. */
    public enum BodyPart {
        HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEGS
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
     * True se il disegno del giocatore va adattato alla tuta: la indossa ({@link SuitLayer#wearsSuit}) e non e'
     * spettatore (per lo spettatore il gioco non disegna i layer, quindi nemmeno la tuta). Non lancia mai.
     */
    public static boolean applies(AbstractClientPlayer player) {
        return SuitLayer.wearsSuit(player) && !player.isSpectator();
    }

    /** Solleva il disegno del pilota, nello spazio del mondo (y verso l'alto). */
    public static void raise(PoseStack pose) {
        pose.translate(0f, SuitScale.pilotRaiseBlocks(), 0f);
    }

    /** Annulla {@link #raise}. */
    public static void lower(PoseStack pose) {
        pose.translate(0f, -SuitScale.pilotRaiseBlocks(), 0f);
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
     * Prima di {@code translateToHand}, nello spazio del modello del giocatore: dal perno del braccio del pilota a
     * quello del braccio della tuta (verso l'esterno e {@link #ARM_SHIFT_Y_PX} piu' in basso).
     */
    public static void beforeHandTransform(PoseStack pose, HumanoidArm arm) {
        float x = arm == HumanoidArm.LEFT ? ARM_SHIFT_X_PX : -ARM_SHIFT_X_PX;
        pose.translate(x / 16f, ARM_SHIFT_Y_PX / 16f, 0f);
    }

    /** Dopo {@code translateToHand}: l'oggetto e la sua distanza dal perno crescono come la tuta. */
    public static void afterHandTransform(PoseStack pose) {
        pose.scale(SuitScale.S, SuitScale.S, SuitScale.S);
    }

    // --- Punti d'ingresso dei mixin: una chiamata per aggancio. ---

    /** In testa a {@code PlayerRenderer.render}. */
    public static void onRenderStart(AbstractClientPlayer player, PoseStack pose) {
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

    /** A ogni uscita di {@code PlayerRenderer.render}, anche quella di un disegno annullato da un evento. */
    public static void onRenderEnd(AbstractClientPlayer player, PoseStack pose) {
        if (applies(player)) {
            lower(pose);
        }
    }

    /** Prima di {@code translateToHand} in {@code ItemInHandLayer.renderArmWithItem}. */
    public static void onBeforeHand(LivingEntity entity, PoseStack pose, HumanoidArm arm) {
        if (entity instanceof AbstractClientPlayer player && applies(player)) {
            beforeHandTransform(pose, arm);
        }
    }

    /** Subito dopo {@code translateToHand} in {@code ItemInHandLayer.renderArmWithItem}. */
    public static void onAfterHand(LivingEntity entity, PoseStack pose) {
        if (entity instanceof AbstractClientPlayer player && applies(player)) {
            afterHandTransform(pose);
        }
    }
}
