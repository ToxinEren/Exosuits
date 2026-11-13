package com.toxineren.exoframes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto d'ingresso comune a tutte le varianti: i bootstrap di piattaforma chiamano {@link #init()}. */
public final class RedstoneExoframes {
    public static final String MOD_ID = "redstone_exoframes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Redstone Exoframes");

    private RedstoneExoframes() {
    }

    public static void init() {
        LOGGER.info("Redstone Exoframes init ({})", MOD_ID);
    }
}
