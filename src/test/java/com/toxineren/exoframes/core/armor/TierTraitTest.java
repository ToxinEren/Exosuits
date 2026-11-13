package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TierTraitTest {
    @Test
    void fiveDistinctTraits() {
        assertEquals(5, TierTrait.values().length);
        assertEquals(TierTrait.SELF_REPAIR, TierTrait.valueOf("SELF_REPAIR"));
    }
}
