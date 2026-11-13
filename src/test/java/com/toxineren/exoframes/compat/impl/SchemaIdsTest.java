package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

/** Avvisi una volta per chiave, con un limite all'insieme, e id resi sicuri per log e chiavi. */
class SchemaIdsTest {

    @Test
    void warnsOncePerKeyUpToTheCap() {
        SchemaIds.WarnOnce warnings = new SchemaIds.WarnOnce(3, NOPLogger.NOP_LOGGER);
        assertTrue(warnings.warn("a", "first {}", 1));
        assertFalse(warnings.warn("a", "first {}", 1));
        assertTrue(warnings.warn("b", "second"));
        assertTrue(warnings.warn("c", "third"));
        assertFalse(warnings.warn("d", "fourth"));
        assertFalse(warnings.warn("e", "fifth"));
        assertEquals(3, warnings.size());
    }

    @Test
    void safeIdIsTruncatedAndHasNoControlCharacters() {
        String longId = "x".repeat(70_000);
        assertEquals(64, SchemaIds.safeId(longId).length());
        assertEquals("bad?id??", SchemaIds.safeId("bad\nid\t\u0000"));
        assertEquals("night_vision", SchemaIds.safeId("night_vision"));
        assertEquals(SchemaIds.safeId("y".repeat(64) + "a"), SchemaIds.safeId("y".repeat(64) + "b"));
    }
}
