package com.toxineren.exoframes.core.frame;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.List;

/**
 * Le zone di un clic su un telaio piazzato (design §5.4), aritmetica pura: in che fascia d'altezza cade e da che lato
 * del telaio. I lati sono quelli del telaio: con yaw 0 guarda a sud (+Z) e ha la destra verso -X, come un giocatore
 * vanilla. L'altezza si misura dai piedi: gambe sotto {@value #LEGS_TOP}, busto e braccia fino a {@value #HEAD_BOTTOM}
 * escluso, testa da li' in su; nella fascia di mezzo un braccio se il clic e' a piu' di {@value #ARM_OFFSET} dal centro
 * verso il suo lato, altrimenti il busto. Un clic esattamente sul centro (0) conta come lato sinistro.
 */
public final class ClickZones {
    /** Sotto questa altezza dai piedi il clic e' sulle gambe. */
    public static final double LEGS_TOP = 1.1;
    /** Da questa altezza dai piedi il clic e' sulla testa. */
    public static final double HEAD_BOTTOM = 2.2;
    /** Distanza laterale dal centro oltre la quale, nella fascia di mezzo, il clic e' su un braccio. */
    public static final double ARM_OFFSET = 0.25;

    private ClickZones() {
    }

    /**
     * Quanto il punto (relativo ai piedi del telaio) sta a destra del centro, sull'asse destro del telaio con yaw
     * {@code yawDegrees}: (-cos yaw, 0, -sin yaw), come per un giocatore che guarda verso (-sin yaw, 0, cos yaw).
     * Positivo a destra, negativo a sinistra.
     */
    public static double rightOf(float yawDegrees, double x, double z) {
        double yaw = Math.toRadians(yawDegrees);
        return -x * Math.cos(yaw) - z * Math.sin(yaw);
    }

    /** Vero se il clic con questa distanza dal centro ({@link #rightOf}) e' sul lato destro del telaio. */
    public static boolean onRightSide(double right) {
        return right > 0;
    }

    /**
     * Le posizioni della fascia d'altezza del clic, la parte cliccata per prima e poi le altre dalla piu' vicina:
     * testa sola; le due gambe dal lato cliccato; nella fascia di mezzo il braccio del lato, il busto e l'altro braccio
     * se il clic e' oltre {@link #ARM_OFFSET}, altrimenti il busto e poi le braccia dal lato cliccato.
     */
    public static List<PieceSlot> bandNearestFirst(double height, double right) {
        boolean rightSide = onRightSide(right);
        if (height >= HEAD_BOTTOM) {
            return List.of(PieceSlot.HELMET);
        }
        if (height < LEGS_TOP) {
            return rightSide ? List.of(PieceSlot.RIGHT_LEG, PieceSlot.LEFT_LEG)
                    : List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG);
        }
        PieceSlot near = rightSide ? PieceSlot.RIGHT_ARM : PieceSlot.LEFT_ARM;
        PieceSlot far = rightSide ? PieceSlot.LEFT_ARM : PieceSlot.RIGHT_ARM;
        if (Math.abs(right) > ARM_OFFSET) {
            return List.of(near, PieceSlot.TORSO, far);
        }
        return List.of(PieceSlot.TORSO, near, far);
    }
}
