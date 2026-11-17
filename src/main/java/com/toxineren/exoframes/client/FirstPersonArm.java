package com.toxineren.exoframes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;

/**
 * Il braccio della tuta in prima persona (design, par. 13). Dovunque il gioco base disegna in prima persona il braccio
 * del giocatore (la mano principale vuota; il braccio o le braccia che reggono una mappa), chi indossa la tuta vede al
 * suo posto il braccio della tuta di quel lato: il braccio del telaio sempre, e il pezzo del braccio montato su quel
 * lato con lo stato e i colori che mostra la terza persona. Il braccio del pilota e la sua manica non si disegnano.
 * Senza la tuta, e con un altro oggetto in mano, non cambia nulla.
 * <p>
 * <b>Grandezza e posto.</b> Il braccio si disegna {@link #SCALE} volte la sua misura di modello, messo in modo che il
 * centro del palmo della tuta cada sul centro della mano del gioco base, spostato di {@link #SIDE_SHIFT_PX} pixel
 * lungo {@link #sideDirection}. Nel sistema del braccio (origine nel perno, assi ruotati come il braccio di base, pixel
 * del modello, y verso la mano), con {@code h_v} = {@link #baseHandPx}, {@code h_s} = {@link #suitPalmPx} e
 * {@code d = SIDE_SHIFT_PX * sideDirection}, un punto {@code X} del braccio della tuta va in
 * {@code h_v + d + K * (X - h_s)}: il palmo ({@code X = h_s}) cade su {@code h_v + d}, e il perno ({@code X = 0}) va in
 * {@code h_v + d - K * h_s} ({@link #pivotShiftPx}).
 * <p>
 * <b>Perche' nel sistema del braccio.</b> E' il sistema in cui il renderer della tuta mette l'osso del braccio (ne
 * copia perno e rotazione dal braccio del modello di base), quindi l'aritmetica vale in ogni posa: l'oscillazione del
 * colpo, il cambio d'oggetto e le pose della mappa vengono dalle trasformazioni del gioco base, a monte, e non si
 * rifanno. Una traslazione nello spazio della vista non andrebbe bene: su 1.21.1 la pila delle pose della mano
 * comincia con l'inversa della matrice della telecamera.
 * <p>
 * <b>Perche' {@code young}.</b> Il modello del giocatore nasce con il segno {@code young} acceso, e lo spegne solo il
 * disegno in terza persona; il renderer della tuta lo copia dal modello di base e rimpicciolirebbe il braccio come
 * quello di un cucciolo finche' il giocatore non passa dalla terza persona. Lo si spegne prima di disegnare.
 * <p>
 * Nessun campo statico di un tipo di Minecraft: i test unitari caricano la classe senza avviare il gioco.
 */
public final class FirstPersonArm {
    /**
     * K: quante volte la sua misura di modello e' grande il braccio in prima persona. Parte dalla scala della terza
     * persona ({@link SuitScale#S}); i candidati sono 1,65, 1,3 e 1,0 e si sceglie nel client vero il piu' grande che
     * soddisfa i criteri di design, par. 13.
     */
    static final float SCALE = SuitScale.S;

    /**
     * Di quanti pixel del modello il palmo si sposta dal centro della mano del gioco base lungo
     * {@link #sideDirection}: un multiplo di 0,5 fra 0 e 8. Zero: il palmo sta sul centro della mano di base; diverso
     * da zero solo se a nessun K il braccio resta fuori da mirino, barra degli oggetti e riga dell'energia (design,
     * par. 13, secondo giro).
     */
    static final float SIDE_SHIFT_PX = 0f;

    /**
     * Centro della mano del gioco base verso l'esterno, in pixel dal perno: la scatola del braccio classico va in x da
     * -3 a 1 per il destro, centro -1. Con la skin sottile sarebbe 0,5: mezzo pixel non si insegue.
     */
    static final float BASE_HAND_OUT_PX = 1f;

    /**
     * Centro della mano del gioco base lungo il braccio, in pixel dal perno: la mano sono gli ultimi 4 pixel della
     * scatola del braccio (y da 6 a 10), centro 8.
     */
    static final float BASE_HAND_DOWN_PX = 8f;

    /**
     * Centro del palmo della tuta verso l'esterno, in pixel dal perno: il cubo del palmo di {@code frameRightArm} va in
     * x da -12,25 a -7,25 nella geometria ({@code palm} in tools/art/designs/frame.py), centro -9,75; il perno e' a
     * x = -5: 9,75 - 5 = 4,75. Provato sulla geometria generata in {@code FirstPersonArmTest}.
     */
    static final float SUIT_PALM_OUT_PX = 4.75f;

    /**
     * Centro del palmo della tuta lungo il braccio, in pixel dal perno: il cubo del palmo va in y da 8,5 a 10,25 nella
     * geometria (y verso l'alto), centro 9,375; il perno e' a y = 22: 22 - 9,375 = 12,625.
     */
    static final float SUIT_PALM_DOWN_PX = 12.625f;

    /**
     * Le tre componenti di {@link #sideDirection}, la direzione che con il braccio a riposo sullo schermo va verso
     * l'esterno senza cambiare distanza, lunga 1. Vengono dalle rotazioni con cui il gioco base mette il braccio in
     * prima persona ({@code ItemInHandRenderer.renderPlayerArm}: Y 45 gradi, Z 120, X 200, Y -135, uguali su 1.20.1 e
     * 1.21.1; per il sinistro gli angoli su Y e Z cambiano segno) seguite da {@code zRot} +-0,1 rad del braccio: detta
     * R quella rotazione, R per questa direzione da' (+-1, 0, 0), cioe' la prima riga di R. Provato in
     * {@code FirstPersonArmTest}.
     */
    static final float SIDE_DIR_OUT = 0.3328f;

    /** Componente lungo il braccio, verso la mano, di {@link #sideDirection}; vedi {@link #SIDE_DIR_OUT}. */
    static final float SIDE_DIR_DOWN = 0.3687f;

    /** Componente in profondita' (z del braccio) di {@link #sideDirection}; vedi {@link #SIDE_DIR_OUT}. */
    static final float SIDE_DIR_BACK = 0.8679f;

    private FirstPersonArm() {
    }

    /**
     * Il centro della mano del gioco base nel sistema del braccio, in pixel dal perno (y verso la mano): x negativa
     * per il braccio destro, positiva per il sinistro. Java puro.
     */
    static PilotPose.Shift baseHandPx(boolean leftArm) {
        return new PilotPose.Shift(leftArm ? BASE_HAND_OUT_PX : -BASE_HAND_OUT_PX, BASE_HAND_DOWN_PX, 0f);
    }

    /**
     * Il centro del palmo della tuta nel sistema del braccio, in pixel della tuta non scalata dal perno (y verso la
     * mano): x negativa per il braccio destro, positiva per il sinistro. Java puro.
     */
    static PilotPose.Shift suitPalmPx(boolean leftArm) {
        return new PilotPose.Shift(leftArm ? SUIT_PALM_OUT_PX : -SUIT_PALM_OUT_PX, SUIT_PALM_DOWN_PX, 0f);
    }

    /**
     * La direzione, nel sistema del braccio, che a riposo sullo schermo va verso l'esterno (destra per il braccio
     * destro, sinistra per il sinistro): x negativa per il destro, positiva per il sinistro. Java puro.
     */
    static PilotPose.Shift sideDirection(boolean leftArm) {
        return new PilotPose.Shift(leftArm ? SIDE_DIR_OUT : -SIDE_DIR_OUT, SIDE_DIR_DOWN, SIDE_DIR_BACK);
    }

    /**
     * Dove va il perno del braccio della tuta, in pixel dal perno del braccio di base e nel suo sistema:
     * {@code baseHandPx + sideShiftPx * sideDirection - scale * suitPalmPx}. Cosi' il palmo, ingrandito di
     * {@code scale} attorno al perno spostato, cade sul centro della mano di base piu' lo spostamento laterale. A
     * spostamento zero, braccio destro: (6,8375, -12,83125, 0) a 1,65, (5,175, -8,4125, 0) a 1,3, (3,75, -4,625, 0) a
     * 1,0. Java puro.
     */
    static PilotPose.Shift pivotShiftPx(boolean leftArm, float scale, float sideShiftPx) {
        PilotPose.Shift hand = baseHandPx(leftArm);
        PilotPose.Shift side = sideDirection(leftArm);
        PilotPose.Shift palm = suitPalmPx(leftArm);
        return new PilotPose.Shift(hand.x() + sideShiftPx * side.x() - scale * palm.x(),
                hand.y() + sideShiftPx * side.y() - scale * palm.y(),
                hand.z() + sideShiftPx * side.z() - scale * palm.z());
    }

    /** Lo slot del pezzo del braccio: {@code LEFT_ARM} per il sinistro, {@code RIGHT_ARM} per il destro. Java puro. */
    static PieceSlot armSlot(boolean leftArm) {
        return leftArm ? PieceSlot.LEFT_ARM : PieceSlot.RIGHT_ARM;
    }

    // --- Punto d'ingresso del mixin: una chiamata per aggancio. ---

    /**
     * In {@code PlayerRenderer.renderHand}, subito dopo la chiamata a {@code PlayerModel.setupAnim}: il gioco base ha
     * gia' preparato il modello (le proprieta' del giocatore, nessun attacco, non accovacciato, nessun nuoto, la posa
     * a riposo) e sta per disegnare {@code arm}, il braccio destro o sinistro di {@code model}. Se il giocatore indossa
     * la tuta disegna al suo posto il braccio della tuta di quel lato e restituisce true: il chiamante interrompe il
     * metodo di base, che non disegna ne' il braccio ne' la manica. Altrimenti restituisce false e il gioco disegna il
     * suo braccio. Non lancia: {@code RuntimeException} e {@code LinkageError} vanno nel log una volta per caricamento
     * dei renderer ({@link SuitDraw#logFailureOnce}), il gioco disegna il suo braccio e {@code pose} resta com'era.
     * {@code partialTick} e' 0: {@code renderHand} non ne ha uno e la tuta non ha animazioni.
     */
    public static boolean onHandPrepared(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
            ModelPart arm, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        SuitView view = SuitLayer.suitView(player); // non lancia; null: senza tuta, disegna il gioco
        if (view == null) {
            return false;
        }
        try {
            boolean left = arm == model.leftArm;
            arm.xRot = 0f; // come fa il metodo di base nell'istruzione che segue l'aggancio
            // Il modello nasce "giovane" e lo corregge solo il disegno in terza persona: GeckoLib copia il segno
            // e dimezzerebbe il braccio. Un giocatore non e' mai un cucciolo.
            model.young = false;
            // Copia privata della posa, come SuitDraw.draw: se il renderer lancia a meta', le sue pose restano qui.
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.last().pose());
            local.last().normal().set(pose.last().normal());
            arm.translateAndRotate(local); // nel sistema del braccio di base: perno e rotazione
            PilotPose.Shift pivot = pivotShiftPx(left, SCALE, SIDE_SHIFT_PX);
            local.translate(pivot.x() / 16f, pivot.y() / 16f, pivot.z() / 16f);
            local.scale(SCALE, SCALE, SCALE);
            // Ritorno allo spazio del modello: perno e rotazione del braccio li rimette il renderer della tuta.
            local.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).conjugate());
            local.translate(-arm.x / 16f, -arm.y / 16f, -arm.z / 16f);
            ClientCompat.suitRenderer().renderArm(player, model, local, buffers, packedLight, 0f, view, armSlot(left));
            return true;
        } catch (RuntimeException | LinkageError e) {
            SuitDraw.logFailureOnce(e); // il gioco disegna il suo braccio
            return false;
        }
    }
}
