package com.toxineren.exoframes.core.module;

/**
 * Condizione ambientale che un modulo passivo o toggle richiede per essere abilitato (e consumare):
 * {@code ALWAYS} non pone condizioni, {@code SUBMERGED} vale solo con il portatore sott'acqua.
 */
public enum ModuleCondition {
    ALWAYS,
    SUBMERGED
}
