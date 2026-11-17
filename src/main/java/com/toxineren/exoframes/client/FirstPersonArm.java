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
     * K: quante volte la sua misura di modello e' grande il braccio in prima persona, da scegliere nel client vero
     * (1.21.1-neoforge, 960x540, a riposo) fra 1,65 (la scala della terza persona, {@link SuitScale#S}), 1,3 e 1,0: il
     * piu' grande che soddisfa i criteri di design, par. 13, cioe' (a) la mano intera sullo schermo (il palmo
     * d'acciaio, le nocche d'ottone, le tre dita e il pollice, nessuno tagliato dal bordo) con un tratto di
     * avambraccio, nel quarto in basso a destra, (b) nessun pixel nel mirino, nella barra degli oggetti e nella riga
     * dell'energia, (c) fra il 3 % e il 14 % dello schermo. <b>Nessun candidato passa</b>: 1,0 con lo spostamento zero
     * e' il valore provvisorio che il piano da' per questo caso, in attesa di una decisione su che cosa chiede (a) (con
     * questi valori l'avambraccio passa dietro la barra degli oggetti). Le misure sono di
     * {@code tools/client/arm_measure.sh sagoma}, il braccio con i sei pezzi e, fra parentesi, il telaio nudo; il
     * riquadro e' quello con i pezzi; "bordo da y" e' la prima riga in cui il braccio tocca le colonne x 952-959.
     * <ul>
     * <li>Con il palmo sul centro della mano di base ({@link #SIDE_SHIFT_PX} 0) nessun K passa (b), perche'
     * l'avambraccio finisce dietro la barra degli oggetti: 1,65 copertura 13,26 (11,87), barra 13175 (7965), riquadro
     * 469x237+491+303; 1,3 copertura 9,61 (8,55), barra 8468 (4469), riquadro 406x205+554+335; 1,0 copertura 7,05
     * (6,00), barra 4868 (1877), riquadro 358x177+602+363, e il solo in cui il telaio nudo non tocca il bordo destro
     * (con i pezzi: bordo da y 514).</li>
     * <li>Con lo spostamento minimo che libera la barra (b) e (c) valgono, (a) no: il blocco d'acciaio del palmo
     * arriva al bordo destro dello schermo, e il pollice non si distingue. 1,65 con 6,5 (a 6,0 barra 731): copertura
     * 10,33 (9,10), riquadro 288x237+672+303, bordo da y 336 (361). 1,3 con 4,5 (a 4,0 barra 634): copertura 8,77
     * (7,72), riquadro 286x205+674+335, bordo da y 407 (418); delle dita il punto piu' a destra e' a x 886 circa, ma
     * il palmo va oltre. 1,0 con 3,0 (a 2,5 barra 14): copertura 6,85 (6,17), riquadro 279x177+681+363, bordo da y 463
     * (465).</li>
     * </ul>
     */
    static final float SCALE = 1.0f;

    /**
     * Di quanti pixel del modello il palmo si sposta dal centro della mano del gioco base lungo
     * {@link #sideDirection}: un multiplo di 0,5 fra 0 e 8 (design, par. 13, secondo giro). Zero e' il valore
     * provvisorio del caso "nessun candidato passa" (vedi {@link #SCALE}), non uno spostamento che libera la barra.
     * Misurati nel client vero, gli spostamenti minimi che tengono fuori dalla barra degli oggetti l'avambraccio con i
     * pezzi: 6,5 a K 1,65, 4,5 a K 1,3, 3,0 a K 1,0 (mezzo pixel meno: barra 731, 634, 14). Mezzo pixel di modello
     * sposta il braccio di circa 12 pixel dello schermo.
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
