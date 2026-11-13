package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DamageRouterTest {

    private final DamageRouter router = newRouter(ExoframesConfig.defaults());

    private static DamageRouter newRouter(ExoframesConfig cfg) {
        return new DamageRouter(cfg.damage, cfg.pieces);
    }

    private static Map<PieceSlot, Piece> fullSet() {
        Map<PieceSlot, Piece> mounted = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            mounted.put(slot, new Piece(slot, Tier.INDUSTRIAL, 1_000, 1_000));
        }
        return mounted;
    }

    private static int sum(Map<PieceSlot, Integer> perPiece) {
        return perPiece.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void headDamageMostlyHitsHelmet() {
        DamageRouter.Split split = router.route(DamageKind.HEAD, 100, fullSet());
        Map<PieceSlot, Integer> p = split.perPiece();
        assertEquals(60, p.get(PieceSlot.HELMET));
        assertEquals(14, p.get(PieceSlot.TORSO));
        assertEquals(6, p.get(PieceSlot.LEFT_ARM));
        assertEquals(6, p.get(PieceSlot.RIGHT_ARM));
        assertEquals(7, p.get(PieceSlot.LEFT_LEG));
        assertEquals(7, p.get(PieceSlot.RIGHT_LEG));
        assertEquals(100, sum(p));
        assertEquals(0, split.unabsorbed());
    }

    @Test
    void fallDamageSplitsBetweenLegs() {
        DamageRouter.Split split = router.route(DamageKind.FALL, 100, fullSet());
        Map<PieceSlot, Integer> p = split.perPiece();
        assertEquals(30, p.get(PieceSlot.LEFT_LEG));
        assertEquals(30, p.get(PieceSlot.RIGHT_LEG));
        int others = p.get(PieceSlot.HELMET) + p.get(PieceSlot.TORSO)
                + p.get(PieceSlot.LEFT_ARM) + p.get(PieceSlot.RIGHT_ARM);
        assertEquals(40, others);
        assertEquals(0, split.unabsorbed());
    }

    @Test
    void genericDamageFollowsShares() {
        DamageRouter.Split split = router.route(DamageKind.GENERIC, 200, fullSet());
        Map<PieceSlot, Integer> p = split.perPiece();
        assertEquals(30, p.get(PieceSlot.HELMET));
        assertEquals(60, p.get(PieceSlot.TORSO));
        assertEquals(25, p.get(PieceSlot.LEFT_ARM));
        assertEquals(25, p.get(PieceSlot.RIGHT_ARM));
        assertEquals(30, p.get(PieceSlot.LEFT_LEG));
        assertEquals(30, p.get(PieceSlot.RIGHT_LEG));
        assertEquals(0, split.unabsorbed());
    }

    @Test
    void brokenPieceReceivesNothing() {
        Map<PieceSlot, Piece> mounted = fullSet();
        mounted.put(PieceSlot.HELMET, new Piece(PieceSlot.HELMET, Tier.INDUSTRIAL, 1_000, 0));
        DamageRouter.Split split = router.route(DamageKind.FROM_ABOVE, 100, mounted);
        Map<PieceSlot, Integer> p = split.perPiece();
        assertFalse(p.containsKey(PieceSlot.HELMET));
        assertEquals(60, p.get(PieceSlot.TORSO));
        assertEquals(100, sum(p));
        assertEquals(0, split.unabsorbed());
    }

    @Test
    void noMountedPiecesLeavesAllUnabsorbed() {
        DamageRouter.Split split = router.route(DamageKind.GENERIC, 50, Map.of());
        assertTrue(split.perPiece().isEmpty());
        assertEquals(50, split.unabsorbed());
    }

    @Test
    void sumAlwaysEqualsAmount() {
        Map<PieceSlot, Piece> mounted = fullSet();
        for (DamageKind kind : DamageKind.values()) {
            for (int amount = 1; amount <= 97; amount++) {
                DamageRouter.Split split = router.route(kind, amount, mounted);
                assertEquals(amount, sum(split.perPiece()), kind + " " + amount);
                assertEquals(0, split.unabsorbed(), kind + " " + amount);
            }
        }
    }

    @Test
    void routerFollowsConfiguredShares() {
        ExoframesConfig cfg = ExoframesConfig.defaults();
        cfg.pieces.shares.put(PieceSlot.HELMET, 0.10);
        cfg.pieces.shares.put(PieceSlot.TORSO, 0.50);
        cfg.pieces.shares.put(PieceSlot.LEFT_ARM, 0.05);
        cfg.pieces.shares.put(PieceSlot.RIGHT_ARM, 0.05);
        cfg.pieces.shares.put(PieceSlot.LEFT_LEG, 0.15);
        cfg.pieces.shares.put(PieceSlot.RIGHT_LEG, 0.15);
        DamageRouter.Split split = newRouter(cfg).route(DamageKind.GENERIC, 100, fullSet());
        Map<PieceSlot, Integer> p = split.perPiece();
        assertEquals(10, p.get(PieceSlot.HELMET));
        assertEquals(50, p.get(PieceSlot.TORSO));
        assertEquals(5, p.get(PieceSlot.LEFT_ARM));
        assertEquals(5, p.get(PieceSlot.RIGHT_ARM));
        assertEquals(15, p.get(PieceSlot.LEFT_LEG));
        assertEquals(15, p.get(PieceSlot.RIGHT_LEG));
        assertEquals(0, split.unabsorbed());
    }

    /** 100 colpi GENERIC da 1 con riporto: ogni colpo usura esattamente 1 e il totale segue le quote. */
    @Test
    void manySmallHitsFollowShares() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        Map<PieceSlot, Integer> total = new EnumMap<>(PieceSlot.class);
        for (int hit = 0; hit < 100; hit++) {
            DamageRouter.Split split = router.route(DamageKind.GENERIC, 1, mounted, carry);
            assertEquals(1, sum(split.perPiece()), "hit " + hit);
            assertEquals(0, split.unabsorbed(), "hit " + hit);
            split.perPiece().forEach((slot, wear) -> total.merge(slot, wear, Integer::sum));
        }
        assertEquals(30, total.get(PieceSlot.TORSO));
        assertEquals(15, total.get(PieceSlot.HELMET));
        assertEquals(15, total.get(PieceSlot.LEFT_LEG));
        assertEquals(15, total.get(PieceSlot.RIGHT_LEG));
        assertEquals(25, total.get(PieceSlot.LEFT_ARM) + total.get(PieceSlot.RIGHT_ARM));
        ExoframesConfig cfg = ExoframesConfig.defaults();
        for (PieceSlot slot : PieceSlot.values()) {
            double exact = 100 * cfg.pieces.shares.get(slot);
            assertTrue(Math.abs(total.get(slot) - exact) <= 1.0, slot + " " + total.get(slot) + " vs " + exact);
        }
        assertEquals(100, sum(total));
    }

    @Test
    void carryIsPerSlotAndBounded() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        for (int hit = 0; hit < 100; hit++) {
            router.route(DamageKind.GENERIC, 1, mounted, carry);
            assertEquals(PieceSlot.values().length, carry.size(), "hit " + hit);
            for (Map.Entry<PieceSlot, Integer> e : carry.entrySet()) {
                assertTrue(Math.abs(e.getValue()) <= 999, "hit " + hit + " " + e);
            }
        }
    }

    @Test
    void carryLeavesUnroutedSlotsAlone() {
        Map<PieceSlot, Piece> mounted = fullSet();
        mounted.put(PieceSlot.HELMET, new Piece(PieceSlot.HELMET, Tier.INDUSTRIAL, 1_000, 0));
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        carry.put(PieceSlot.HELMET, 400);
        DamageRouter.Split split = router.route(DamageKind.GENERIC, 1, mounted, carry);
        assertEquals(1, sum(split.perPiece()));
        assertEquals(400, carry.get(PieceSlot.HELMET));

        Map<PieceSlot, Integer> untouched = new EnumMap<>(PieceSlot.class);
        untouched.put(PieceSlot.TORSO, 700);
        DamageRouter.Split none = router.route(DamageKind.GENERIC, 3, Map.of(), untouched);
        assertEquals(3, none.unabsorbed());
        assertEquals(Map.of(PieceSlot.TORSO, 700), untouched);
    }

    @Test
    void zeroDamageWithCarryWearsNothing() {
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        carry.put(PieceSlot.TORSO, 600);
        carry.put(PieceSlot.HELMET, -300);
        DamageRouter.Split split = router.route(DamageKind.GENERIC, 0, fullSet(), carry);
        assertTrue(split.perPiece().isEmpty());
        assertEquals(0, split.unabsorbed());
        assertEquals(600, carry.get(PieceSlot.TORSO));
        assertEquals(-300, carry.get(PieceSlot.HELMET));
    }

    @Test
    void allMountedPiecesBrokenLeavesAllUnabsorbed() {
        Map<PieceSlot, Piece> mounted = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            mounted.put(slot, new Piece(slot, Tier.INDUSTRIAL, 1_000, 0));
        }
        for (DamageKind kind : DamageKind.values()) {
            DamageRouter.Split split = router.route(kind, 37, mounted);
            assertTrue(split.perPiece().isEmpty(), kind.name());
            assertEquals(37, split.unabsorbed(), kind.name());
        }
    }
}
