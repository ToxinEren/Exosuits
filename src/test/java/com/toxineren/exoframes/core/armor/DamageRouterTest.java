package com.toxineren.exoframes.core.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.EnumMap;
import java.util.HashMap;
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

    private static int sumCarry(Map<PieceSlot, Integer> carry) {
        return carry.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static void assertCarryInRange(Map<PieceSlot, Integer> carry, String where) {
        for (Map.Entry<PieceSlot, Integer> e : carry.entrySet()) {
            assertTrue(e.getValue() >= 0 && e.getValue() <= 999, where + " " + e);
        }
    }

    /**
     * 100 colpi GENERIC da 1 con riporto: ogni pezzo riceve floor della sua quota cumulata (braccia 12,5 → 12
     * con 500 millesimi in sospeso), e usura + riporti / 1000 = 100 esattamente.
     */
    @Test
    void manySmallHitsFollowShares() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        Map<PieceSlot, Integer> total = new EnumMap<>(PieceSlot.class);
        for (int hit = 0; hit < 100; hit++) {
            DamageRouter.Split split = router.route(DamageKind.GENERIC, 1, mounted, carry);
            assertEquals(0, split.unabsorbed(), "hit " + hit);
            split.perPiece().forEach((slot, wear) -> total.merge(slot, wear, Integer::sum));
        }
        assertEquals(30, total.get(PieceSlot.TORSO));
        assertEquals(15, total.get(PieceSlot.HELMET));
        assertEquals(12, total.get(PieceSlot.LEFT_ARM));
        assertEquals(12, total.get(PieceSlot.RIGHT_ARM));
        assertEquals(15, total.get(PieceSlot.LEFT_LEG));
        assertEquals(15, total.get(PieceSlot.RIGHT_LEG));
        assertEquals(99, sum(total));
        assertEquals(500, carry.get(PieceSlot.LEFT_ARM));
        assertEquals(500, carry.get(PieceSlot.RIGHT_ARM));
        for (PieceSlot slot : new PieceSlot[] {PieceSlot.HELMET, PieceSlot.TORSO, PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG}) {
            assertEquals(0, carry.get(slot), slot.name());
        }
        assertEquals(100 * 1000, sum(total) * 1000 + sumCarry(carry));
    }

    @Test
    void carryIsPerSlotAndBounded() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        for (int hit = 0; hit < 100; hit++) {
            router.route(DamageKind.GENERIC, 1, mounted, carry);
            assertEquals(PieceSlot.values().length, carry.size(), "hit " + hit);
            assertCarryInRange(carry, "hit " + hit);
        }
    }

    @Test
    void carryLeavesUnroutedSlotsAlone() {
        Map<PieceSlot, Piece> mounted = fullSet();
        mounted.put(PieceSlot.HELMET, new Piece(PieceSlot.HELMET, Tier.INDUSTRIAL, 1_000, 0));
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        carry.put(PieceSlot.HELMET, 400);
        router.route(DamageKind.GENERIC, 1, mounted, carry);
        assertEquals(400, carry.get(PieceSlot.HELMET));

        Map<PieceSlot, Integer> untouched = new EnumMap<>(PieceSlot.class);
        untouched.put(PieceSlot.TORSO, 700);
        DamageRouter.Split none = router.route(DamageKind.GENERIC, 3, Map.of(), untouched);
        assertEquals(3, none.unabsorbed());
        assertEquals(Map.of(PieceSlot.TORSO, 700), untouched);
    }

    /**
     * Tipi di danno misti, con un pezzo con riporto non nullo smontato e rimontato (come {@code FrameState}: pezzo
     * nuovo e riporto azzerato) e un pezzo rotto sul posto. Dopo ogni colpo: riporti in [0, 999] e conservazione
     * esatta, usura x 1000 + riporti + millesimi persi allo smontaggio = danno x 1000, da cui
     * danno - 6 - persi / 1000 &lt; usura &lt;= danno.
     */
    @Test
    void carryStaysBoundedAcrossUnmountAndBreak() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        int[] amounts = {1, 1, 3, 1, 2, 1, 7, 1, 1, 4, 0};
        DamageKind[] kinds = DamageKind.values();
        long damage = 0;
        long worn = 0;
        long lostMilli = 0;
        for (int hit = 0; hit < 200; hit++) {
            if (hit == 37) {
                assertTrue(carry.get(PieceSlot.LEFT_ARM) != 0, "riporto del braccio: " + carry);
                mounted.remove(PieceSlot.LEFT_ARM);
                lostMilli += carry.remove(PieceSlot.LEFT_ARM);
            }
            if (hit == 41) {
                mounted.put(PieceSlot.LEFT_ARM, new Piece(PieceSlot.LEFT_ARM, Tier.INDUSTRIAL, 1_000, 1_000));
            }
            if (hit == 90) {
                assertTrue(carry.get(PieceSlot.TORSO) != 0, "riporto del torso: " + carry);
                Piece torso = mounted.get(PieceSlot.TORSO);
                torso.damage(torso.durability());
            }
            int amount = amounts[hit % amounts.length];
            DamageRouter.Split split = router.route(kinds[hit % kinds.length], amount, mounted, carry);
            assertEquals(0, split.unabsorbed(), "hit " + hit);
            damage += amount;
            worn += sum(split.perPiece());
            assertCarryInRange(carry, "hit " + hit);
            assertEquals(damage * 1000, worn * 1000 + sumCarry(carry) + lostMilli, "hit " + hit);
            assertTrue(worn <= damage, "hit " + hit + ": " + worn + " > " + damage);
            assertTrue(worn > damage - 6 - lostMilli / 1000.0, "hit " + hit + ": " + worn + " su " + damage);
        }
    }

    @Test
    void zeroDamageNeverWears() {
        Map<PieceSlot, Piece> mounted = fullSet();
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        for (int hit = 0; hit < 13; hit++) {
            router.route(DamageKind.GENERIC, 1, mounted, carry);
        }
        carry.put(PieceSlot.TORSO, 999);
        Map<PieceSlot, Integer> before = new EnumMap<>(carry);
        for (DamageKind kind : DamageKind.values()) {
            DamageRouter.Split split = router.route(kind, 0, mounted, carry);
            assertTrue(split.perPiece().isEmpty(), kind.name());
            assertEquals(0, split.unabsorbed(), kind.name());
            assertEquals(before, carry, kind.name());
        }
    }

    @Test
    void nullCarryValueCountsAsZero() {
        Map<PieceSlot, Integer> carry = new HashMap<>();
        carry.put(PieceSlot.TORSO, null);
        DamageRouter.Split split = router.route(DamageKind.GENERIC, 200, fullSet(), carry);
        assertEquals(router.route(DamageKind.GENERIC, 200, fullSet()), split);
        assertEquals(0, carry.get(PieceSlot.TORSO));
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

    /**
     * La variante con riporto delega a {@code routeMilli}: sugli importi interi le due danno lo stesso esito e lo
     * stesso riporto, per ogni tipo di danno e ogni insieme non vuoto di pezzi montati, anche dopo 1200 colpi.
     */
    @Test
    void routeMilliMatchesRouteOnWholePoints() {
        Map<PieceSlot, Piece> all = fullSet();
        PieceSlot[] slots = PieceSlot.values();
        for (DamageKind kind : DamageKind.values()) {
            for (int mask = 1; mask < 64; mask++) {
                Map<PieceSlot, Piece> mounted = new EnumMap<>(PieceSlot.class);
                for (int i = 0; i < slots.length; i++) {
                    if ((mask & (1 << i)) != 0) {
                        mounted.put(slots[i], all.get(slots[i]));
                    }
                }
                Map<PieceSlot, Integer> carryA = new EnumMap<>(PieceSlot.class);
                Map<PieceSlot, Integer> carryB = new EnumMap<>(PieceSlot.class);
                for (int n = 0; n <= 1200; n++) {
                    String where = kind + " " + mask + " " + n;
                    assertEquals(router.route(kind, n, mounted, carryA),
                            router.routeMilli(kind, n * 1000L, mounted, carryB), where);
                    assertEquals(carryA, carryB, where);
                }
            }
        }
    }

    /**
     * Mezzo punto GENERIC finisce tutto nel riporto, con i resti maggiori in millesimi (a parita' di resto vince lo
     * slot che viene prima); tre punti dopo, il torso supera i 1000 millesimi e perde un punto.
     */
    @Test
    void fractionsGoToTheCarry() {
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        DamageRouter.Split half = router.routeMilli(DamageKind.GENERIC, 500, fullSet(), carry);
        assertTrue(half.perPiece().isEmpty(), half.toString());
        assertEquals(0, half.unabsorbed());
        assertEquals(75, carry.get(PieceSlot.HELMET));
        assertEquals(150, carry.get(PieceSlot.TORSO));
        assertEquals(63, carry.get(PieceSlot.LEFT_ARM));
        assertEquals(62, carry.get(PieceSlot.RIGHT_ARM));
        assertEquals(75, carry.get(PieceSlot.LEFT_LEG));
        assertEquals(75, carry.get(PieceSlot.RIGHT_LEG));

        DamageRouter.Split three = router.routeMilli(DamageKind.GENERIC, 3000, fullSet(), carry);
        assertEquals(Map.of(PieceSlot.TORSO, 1), three.perPiece());
        assertEquals(0, three.unabsorbed());
        assertEquals(50, carry.get(PieceSlot.TORSO));
        assertEquals(525, carry.get(PieceSlot.HELMET));
        assertEquals(438, carry.get(PieceSlot.LEFT_ARM));
        assertEquals(437, carry.get(PieceSlot.RIGHT_ARM));
        assertEquals(525, carry.get(PieceSlot.LEFT_LEG));
        assertEquals(525, carry.get(PieceSlot.RIGHT_LEG));
    }

    /** Senza pezzi che assorbono, {@code unabsorbed} e' in punti, per difetto, e il riporto non cambia. */
    @Test
    void unabsorbedIsInPoints() {
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        DamageRouter.Split split = router.routeMilli(DamageKind.GENERIC, 2500, Map.of(), carry);
        assertTrue(split.perPiece().isEmpty(), split.toString());
        assertEquals(2, split.unabsorbed());
        assertTrue(carry.isEmpty(), carry.toString());
        assertEquals(0, router.routeMilli(DamageKind.GENERIC, 999, Map.of(), carry).unabsorbed());
    }

    /**
     * L'importo piu' grande ammesso ({@code Integer.MAX_VALUE} punti in millesimi) non trabocca e si conserva tutto
     * fra usura e riporto, dalle due entrate; fuori da [0, massimo] {@code IllegalArgumentException}.
     */
    @Test
    void largeAmountsDoNotOverflow() {
        long max = (long) Integer.MAX_VALUE * 1000;
        Map<PieceSlot, Integer> carry = new EnumMap<>(PieceSlot.class);
        DamageRouter.Split milli = router.routeMilli(DamageKind.GENERIC, max, fullSet(), carry);
        assertEquals(max, sum(milli.perPiece()) * 1000L + sumCarry(carry));
        assertCarryInRange(carry, "routeMilli");

        Map<PieceSlot, Integer> carry2 = new EnumMap<>(PieceSlot.class);
        DamageRouter.Split points = router.route(DamageKind.GENERIC, Integer.MAX_VALUE, fullSet(), carry2);
        assertEquals(max, sum(points.perPiece()) * 1000L + sumCarry(carry2));
        assertCarryInRange(carry2, "route");

        Map<PieceSlot, Integer> carry3 = new EnumMap<>(PieceSlot.class);
        assertThrows(IllegalArgumentException.class,
                () -> router.routeMilli(DamageKind.GENERIC, max + 1, fullSet(), carry3));
        assertThrows(IllegalArgumentException.class,
                () -> router.routeMilli(DamageKind.GENERIC, -1, fullSet(), carry3));
    }
}
