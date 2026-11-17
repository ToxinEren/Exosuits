package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Grandezza e posto del braccio della tuta in prima persona (design, par. 13): l'aritmetica, senza il gioco. */
class FirstPersonArmTest {

    /** I candidati della scala K (design, par. 13). */
    private static final float[] K = {1.65f, 1.3f, 1.0f};

    private static final boolean[] ARMS = {false, true};

    /**
     * La scatola del braccio classico del gioco base, dal perno: x da -3 a 1 per il destro (da -1 a 3 per il sinistro),
     * y da -2 a 10, z da -2 a 2. La mano sono gli ultimi 4 pixel (y da 6 a 10): centro ((-3 + 1) / 2, (6 + 10) / 2,
     * 0) = (-1, 8, 0) per il destro, (1, 8, 0) per il sinistro.
     */
    @Test
    void baseHandIsTheMiddleOfTheLastFourPixelsOfTheBaseArm() {
        assertShift(-1f, 8f, 0f, FirstPersonArm.baseHandPx(false));
        assertShift(1f, 8f, 0f, FirstPersonArm.baseHandPx(true));
    }

    /**
     * Il palmo e' il cubo piu' grande della mano del telaio nella geometria generata (pixel della geometria, y verso
     * l'alto, perno del braccio in (-+5, 22, 0)): il suo centro, visto dal perno con y verso la mano, e' il palmo della
     * tuta.
     */
    @Test
    void suitPalmIsTheCentreOfThePalmCubeOfTheGeneratedGeometry() throws IOException {
        double[] right = palmCentre("frameRightArm");
        assertShift((float) (right[0] + 5), (float) (22 - right[1]), (float) right[2],
                FirstPersonArm.suitPalmPx(false));
        assertShift(-4.75f, 12.625f, 0f, FirstPersonArm.suitPalmPx(false));

        double[] left = palmCentre("frameLeftArm");
        assertShift((float) (left[0] - 5), (float) (22 - left[1]), (float) left[2], FirstPersonArm.suitPalmPx(true));
        assertShift(4.75f, 12.625f, 0f, FirstPersonArm.suitPalmPx(true));
    }

    /** Il perno va in h_v - K * h_s: cosi' il palmo (h_s), ingrandito di K, cade sul centro della mano di base. */
    @Test
    void palmFallsOnTheBaseHandAtEveryCandidateScale() {
        assertShift(6.8375f, -12.83125f, 0f, FirstPersonArm.pivotShiftPx(false, 1.65f, 0f));
        assertShift(5.175f, -8.4125f, 0f, FirstPersonArm.pivotShiftPx(false, 1.3f, 0f));
        assertShift(3.75f, -4.625f, 0f, FirstPersonArm.pivotShiftPx(false, 1.0f, 0f));
        for (float k : K) {
            for (boolean left : ARMS) {
                PilotPose.Shift palm = plus(FirstPersonArm.pivotShiftPx(left, k, 0f),
                        times(k, FirstPersonArm.suitPalmPx(left)));
                PilotPose.Shift hand = FirstPersonArm.baseHandPx(left);
                assertShift(hand.x(), hand.y(), hand.z(), palm);
            }
        }
    }

    @Test
    void sideShiftMovesThePalmAlongTheSideDirection() {
        for (float k : K) {
            for (float s : new float[] {0.5f, 4.5f, 8f}) {
                for (boolean left : ARMS) {
                    PilotPose.Shift palm = plus(FirstPersonArm.pivotShiftPx(left, k, s),
                            times(k, FirstPersonArm.suitPalmPx(left)));
                    PilotPose.Shift moved = plus(palm, times(-1f, FirstPersonArm.baseHandPx(left)));
                    PilotPose.Shift expected = times(s, FirstPersonArm.sideDirection(left));
                    assertShift(expected.x(), expected.y(), expected.z(), moved);
                }
            }
        }
        assertShift(3.6774f, -6.75335f, 3.90555f, FirstPersonArm.pivotShiftPx(false, 1.3f, 4.5f));
    }

    /**
     * Le rotazioni che il gioco base applica al braccio a riposo in prima persona (Y 45 gradi, Z 120, X 200, Y -135;
     * per il sinistro gli angoli su Y e Z cambiano segno), seguite da zRot del braccio (+-0,1 rad): la direzione
     * laterale, ruotata cosi', sullo schermo va dritta verso l'esterno.
     */
    @Test
    void sideDirectionPointsOutwardOnTheScreenAtRest() {
        for (boolean left : ARMS) {
            double f = left ? -1 : 1;
            double[][] r = mul(mul(mul(mul(rotY(Math.toRadians(f * 45)), rotZ(Math.toRadians(f * 120))),
                    rotX(Math.toRadians(200))), rotY(Math.toRadians(-f * 135))), rotZ(f * 0.1));
            PilotPose.Shift d = FirstPersonArm.sideDirection(left);
            double[] v = {d.x(), d.y(), d.z()};
            double[] onScreen = new double[3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    onScreen[i] += r[i][j] * v[j];
                }
            }
            assertEquals(f, onScreen[0], 2e-3, "x");
            assertEquals(0, onScreen[1], 2e-3, "y");
            assertEquals(0, onScreen[2], 2e-3, "z");
            assertEquals(1, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-3, "length");
        }
    }

    @Test
    void constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight() {
        boolean candidate = false;
        for (float k : K) {
            candidate |= FirstPersonArm.SCALE == k;
        }
        assertTrue(candidate, "SCALE " + FirstPersonArm.SCALE + " is not a candidate");
        assertTrue(1.65f == SuitScale.S, "the first candidate is the scale of the third person");

        assertTrue(FirstPersonArm.SIDE_SHIFT_PX >= 0f && FirstPersonArm.SIDE_SHIFT_PX <= 8f,
                "SIDE_SHIFT_PX " + FirstPersonArm.SIDE_SHIFT_PX);
        assertEquals(Math.rint(FirstPersonArm.SIDE_SHIFT_PX * 2f), FirstPersonArm.SIDE_SHIFT_PX * 2f, 0.0,
                "SIDE_SHIFT_PX is a multiple of 0.5");

        assertEquals(PieceSlot.RIGHT_ARM, FirstPersonArm.armSlot(false));
        assertEquals(PieceSlot.LEFT_ARM, FirstPersonArm.armSlot(true));

        assertMirror(FirstPersonArm.baseHandPx(false), FirstPersonArm.baseHandPx(true));
        assertMirror(FirstPersonArm.suitPalmPx(false), FirstPersonArm.suitPalmPx(true));
        assertMirror(FirstPersonArm.sideDirection(false), FirstPersonArm.sideDirection(true));
        assertMirror(FirstPersonArm.pivotShiftPx(false, 1.3f, 4.5f), FirstPersonArm.pivotShiftPx(true, 1.3f, 4.5f));
    }

    /** Il centro del cubo piu' grande, per volume, fra quelli della mano dell'osso. */
    private static double[] palmCentre(String bone) throws IOException {
        List<double[]> hand = PilotPoseTest.handCubes(bone);
        assertTrue(hand.size() > 3, "mano di " + bone + " non trovata in " + PilotPoseTest.GEO);
        double[] palm = hand.get(0);
        for (double[] c : hand) {
            if (volume(c) > volume(palm)) {
                palm = c;
            }
        }
        return new double[] {(palm[0] + palm[3]) / 2, (palm[1] + palm[4]) / 2, (palm[2] + palm[5]) / 2};
    }

    private static double volume(double[] c) {
        return (c[3] - c[0]) * (c[4] - c[1]) * (c[5] - c[2]);
    }

    private static PilotPose.Shift plus(PilotPose.Shift a, PilotPose.Shift b) {
        return new PilotPose.Shift(a.x() + b.x(), a.y() + b.y(), a.z() + b.z());
    }

    private static PilotPose.Shift times(float k, PilotPose.Shift a) {
        return new PilotPose.Shift(k * a.x(), k * a.y(), k * a.z());
    }

    // Rotazioni destrorse attorno ai tre assi, come matrici 3x3 (righe per colonne).

    private static double[][] rotX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][] {{1, 0, 0}, {0, c, -s}, {0, s, c}};
    }

    private static double[][] rotY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][] {{c, 0, s}, {0, 1, 0}, {-s, 0, c}};
    }

    private static double[][] rotZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][] {{c, -s, 0}, {s, c, 0}, {0, 0, 1}};
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] m = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int n = 0; n < 3; n++) {
                    m[i][j] += a[i][n] * b[n][j];
                }
            }
        }
        return m;
    }

    private static void assertShift(float x, float y, float z, PilotPose.Shift shift) {
        assertEquals(x, shift.x(), 1e-4, "x");
        assertEquals(y, shift.y(), 1e-4, "y");
        assertEquals(z, shift.z(), 1e-4, "z");
    }

    /** Il sinistro ha la x opposta e le stesse y e z del destro. */
    private static void assertMirror(PilotPose.Shift right, PilotPose.Shift left) {
        assertEquals(-right.x(), left.x(), 0.0, "x");
        assertEquals(right.y(), left.y(), 0.0, "y");
        assertEquals(right.z(), left.z(), 0.0, "z");
    }
}
