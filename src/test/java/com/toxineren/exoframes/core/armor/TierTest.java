package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TierTest {
    @Test
    void levelsAreOneToFive() {
        Tier[] tiers = Tier.values();
        assertEquals(5, tiers.length);
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(i + 1, tiers[i].level());
        }
    }

    @Test
    void extraSlotsMatchSpec() {
        assertEquals(0, Tier.SCRAP.extraModuleSlots());
        assertEquals(1, Tier.INDUSTRIAL.extraModuleSlots());
        assertEquals(2, Tier.ARMORED.extraModuleSlots());
        assertEquals(2, Tier.NETHERITE.extraModuleSlots());
        assertEquals(3, Tier.PROTOTYPE.extraModuleSlots());
    }

    @Test
    void multipliersMatchSpec() {
        assertEquals(1.000, Tier.SCRAP.protectionMultiplier(), 1e-9);
        assertEquals(1.333, Tier.INDUSTRIAL.protectionMultiplier(), 1e-9);
        assertEquals(1.733, Tier.ARMORED.protectionMultiplier(), 1e-9);
        assertEquals(1.600, Tier.NETHERITE.protectionMultiplier(), 1e-9);
        assertEquals(2.000, Tier.PROTOTYPE.protectionMultiplier(), 1e-9);
    }

    @Test
    void byIdRoundTrips() {
        for (Tier tier : Tier.values()) {
            assertEquals(tier, Tier.byId(tier.id()));
        }
    }

    @Test
    void atLeastIsInclusive() {
        assertTrue(Tier.ARMORED.atLeast(Tier.ARMORED));
        assertTrue(Tier.PROTOTYPE.atLeast(Tier.ARMORED));
        assertFalse(Tier.INDUSTRIAL.atLeast(Tier.ARMORED));
    }
}
