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
}
