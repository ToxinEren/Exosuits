package com.toxineren.exoframes.content;

import net.minecraft.util.StringRepresentable;

/**
 * Le due meta' della stazione di manutenzione (design §5.1), la proprieta' {@code part} del suo stato: la principale,
 * dove il gioco posa il blocco, che porta il bottino, e la laterale, alla destra di chi la posa.
 */
public enum StationPart implements StringRepresentable {
    /** La meta' principale: {@code part=main}. */
    MAIN("main"),
    /** La meta' laterale: {@code part=side}. */
    SIDE("side");

    private final String name;

    StationPart(String name) {
        this.name = name;
    }

    /** Il nome nello stato del blocco, nei blockstate e nelle tabelle di bottino. */
    @Override
    public String getSerializedName() {
        return name;
    }
}
