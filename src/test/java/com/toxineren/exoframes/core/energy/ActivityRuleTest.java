package com.toxineren.exoframes.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActivityRuleTest {

    private static final Activity I = Activity.IDLE;
    private static final Activity W = Activity.WALK;
    private static final Activity S = Activity.SPRINT;

    @Test
    void stillIsIdle() {
        ActivityRule rule = new ActivityRule();
        for (int i = 0; i < 5; i++) {
            assertEquals(I, rule.next(0.0, false, false));
        }
    }

    @Test
    void exactlyTheThresholdIsStill() {
        ActivityRule rule = new ActivityRule();
        assertEquals(I, rule.next(ActivityRule.MOVING, false, false));
        assertEquals(W, rule.next(0.011, false, false));
    }

    @Test
    void jitteryWalkStaysWalk() {
        ActivityRule rule = new ActivityRule();
        assertEquals(I, rule.next(0.0, false, false));
        double[] samples = {0.43, 0.0, 0.43, 0.0, 0.43, 0.0};
        for (double sample : samples) {
            assertEquals(W, rule.next(sample, false, false));
        }
    }

    @Test
    void stopKeepsThreeTicksOfTail() {
        ActivityRule rule = new ActivityRule();
        assertEquals(W, rule.next(0.2, false, false));
        assertEquals(W, rule.next(0.0, false, false));
        assertEquals(W, rule.next(0.0, false, false));
        assertEquals(W, rule.next(0.0, false, false));
        assertEquals(I, rule.next(0.0, false, false));
    }

    @Test
    void sprintNeedsMovement() {
        ActivityRule rule = new ActivityRule();
        assertEquals(I, rule.next(0.0, true, false));
        assertEquals(S, rule.next(0.2, true, false));
        assertEquals(W, rule.next(0.2, false, false));
    }

    @Test
    void teleportIsIdleAndClearsTheWindow() {
        ActivityRule rule = new ActivityRule();
        assertEquals(W, rule.next(0.2, false, false));
        assertEquals(I, rule.next(9.0, false, false));
        assertEquals(I, rule.next(0.0, false, false));
        assertEquals(W, new ActivityRule().next(ActivityRule.TELEPORT, false, false));
    }

    @Test
    void passengerIsIdleAndClearsTheWindow() {
        ActivityRule rule = new ActivityRule();
        assertEquals(W, rule.next(0.2, false, false));
        assertEquals(I, rule.next(0.2, true, true));
        assertEquals(I, rule.next(0.0, false, false));
    }
}
