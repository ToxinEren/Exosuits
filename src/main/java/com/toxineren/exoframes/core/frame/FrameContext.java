package com.toxineren.exoframes.core.frame;

/**
 * Situazione del portatore nel mondo, fornita dal livello di piattaforma a ogni valutazione: decide le
 * condizioni dei moduli ({@link com.toxineren.exoframes.core.module.ModuleCondition}).
 *
 * @param submerged true se il portatore e' sott'acqua
 */
public record FrameContext(boolean submerged) {

    /** Nessuna condizione ambientale attiva: all'asciutto. */
    public static final FrameContext NONE = new FrameContext(false);
}
