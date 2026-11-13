package com.toxineren.exoframes.compat.impl;

import java.util.Objects;

/**
 * Dove sta un telaio piazzato nel registro dei telai ({@link FrameStateSavedData}): id della dimensione in forma
 * testuale (es. {@code minecraft:overworld}), posizione e orientamento in gradi. Le coordinate devono essere finite.
 */
record PlacedSpot(String dimension, double x, double y, double z, float yaw) {
    PlacedSpot {
        Objects.requireNonNull(dimension, "dimension must not be null");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw)) {
            throw new IllegalArgumentException("placed spot must be finite: " + x + ", " + y + ", " + z + ", " + yaw);
        }
    }
}
