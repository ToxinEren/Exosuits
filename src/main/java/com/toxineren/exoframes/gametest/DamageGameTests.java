package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.core.armor.DamageKind;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.frame.FrameSounds;
import com.toxineren.exoframes.frame.SlotGuard;
import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.frame.SuitTick;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dell'usura dei pezzi (design §12.7) e della caduta (§12.8) della parte 3c. Di base un giocatore in
 * sopravvivenza ({@code survivalPlayer}: in creativa il gioco rifiuta il colpo prima di {@code hurtArmor}, e nei primi
 * 60 tick lo rifiuta la protezione d'ingresso) dentro sei pezzi Rottame nuovi (180 di durata), colpito dal cactus:
 * nessuna posizione e nessuna entita', quindi {@code GENERIC}, e nessuna scala di difficolta'. Non tutti: i test della
 * caduta usano la caduta (uno su due gambe rotte), quello della lava la lava, quello dell'incudine l'incudine, quello
 * del telaio nudo un telaio senza pezzi, e quello della creativa e dello spettatore mette da se' quei modi di gioco;
 * ciascuno lo dice nel suo commento. I numeri vengono dalle quote predefinite della config (elmo 0,15, torso 0,30,
 * braccia 0,125, gambe 0,15; quota primaria 0,60), in millesimi di punto. Le regole comuni e gli aiuti stanno in
 * {@link GameTestKit}.
 */
public final class DamageGameTests {
    /** I piedi del test, sopra la pietra che {@code wearFrameAt} posa in (3, 1, 3). */
    private static final Vec3 FEET = new Vec3(3.5, 2.0, 3.5);

    private static final int NEW = 180;

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public DamageGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "cactusHitWearsThePiecesByShare", DamageGameTests::cactusHitWearsThePiecesByShare);
        add(tests, "secondHitInsideTheWindowWearsOnlyTheDifference",
                DamageGameTests::secondHitInsideTheWindowWearsOnlyTheDifference);
        add(tests, "zeroAmountWearsNothing", DamageGameTests::zeroAmountWearsNothing);
        add(tests, "armorBypassingDamageWearsNothing", DamageGameTests::armorBypassingDamageWearsNothing);
        add(tests, "noWearInCreativeOrSpectator", DamageGameTests::noWearInCreativeOrSpectator);
        add(tests, "lavaWearsOncePerAppliedHit", DamageGameTests::lavaWearsOncePerAppliedHit);
        add(tests, "anvilHitsTheHelmet", DamageGameTests::anvilHitsTheHelmet);
        add(tests, "aBreakingPieceLowersTheArmorAndTells", DamageGameTests::aBreakingPieceLowersTheArmorAndTells);
        add(tests, "wearAtEmptyStillReachesTheOwner", DamageGameTests::wearAtEmptyStillReachesTheOwner);
        add(tests, "fallWearsTheLegsAndNeverHurts", DamageGameTests::fallWearsTheLegsAndNeverHurts);
        add(tests, "pearlLandingWearsTheLegs", DamageGameTests::pearlLandingWearsTheLegs);
        add(tests, "fallOnABareFrameWearsNothing", DamageGameTests::fallOnABareFrameWearsNothing);
        add(tests, "brokenLegsPassTheFallToOtherPieces", DamageGameTests::brokenLegsPassTheFallToOtherPieces);
        return tests;
    }

    /**
     * Un colpo di cactus da 8 (8 000 millesimi, nessun primario): elmo e gambe 1 200 (1 punto, riporto 200), torso
     * 2 400 (2 punti, riporto 400), braccia 1 000 (1 punto, riporto 0).
     */
    private static void cactusHitWearsThePiecesByShare(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "cactus hit refused");
        assertWear(helper, p, "after one hit", 179, 200, 178, 400, 179, 0, 179, 200);
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Nella finestra di invulnerabilita' un colpo minore non consuma nulla; uno maggiore consuma la differenza, che e'
     * cio' che il gioco passa a {@code hurtArmor} (10 - 8 = 2: 2 000 millesimi).
     */
    private static void secondHitInsideTheWindowWearsOnlyTheDifference(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "first cactus hit refused");
        helper.assertTrue(!p.hurt(p.damageSources().cactus(), 5.0F), "smaller hit inside the window applied");
        assertWear(helper, p, "after the smaller hit", 179, 200, 178, 400, 179, 0, 179, 200);
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 10.0F), "bigger hit inside the window refused");
        assertWear(helper, p, "after the bigger hit", 179, 500, 177, 0, 179, 250, 179, 500);
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Zero, un importo negativo e {@code NaN} non consumano nulla (dopo uno scudo che para tutto Fabric e NeoForge
     * chiamano {@code hurtArmor} con zero); un millesimo arriva davvero, e va al torso, che ha il resto piu' grande.
     */
    private static void zeroAmountWearsNothing(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        DamageSource cactus = p.damageSources().cactus();
        SuitDamage.onArmorHurt(p, cactus, 0.0F);
        SuitDamage.onArmorHurt(p, cactus, -2.0F);
        SuitDamage.onArmorHurt(p, cactus, Float.NaN);
        assertUnworn(helper, p, "after zero, negative and NaN");
        SuitDamage.onArmorHurt(p, cactus, 0.001F);
        for (PieceSlot slot : PieceSlot.values()) {
            int expected = slot == PieceSlot.TORSO ? 1 : 0;
            helper.assertTrue(carry(p, slot) == expected,
                    slot + " carry " + carry(p, slot) + " after one thousandth, expected " + expected);
        }
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Il danno che aggira l'armatura non consuma i pezzi: il gioco non chiama {@code hurtArmor}. Il colpo di cactus che
     * segue prova che l'aggancio c'e'.
     */
    private static void armorBypassingDamageWearsNothing(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        helper.assertTrue(Compat.damage().bypassesArmor(p.damageSources().generic()),
                "premise: generic damage bypasses the armor");
        helper.assertTrue(p.hurt(p.damageSources().generic(), 8.0F), "generic hit refused");
        assertUnworn(helper, p, "after armor-bypassing damage");
        p.invulnerableTime = 0;
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "cactus hit refused");
        helper.assertTrue(carry(p, PieceSlot.HELMET) == 200,
                "helmet carry " + carry(p, PieceSlot.HELMET) + " after the cactus hit, expected 200");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * In creativa ({@code instabuild}) e da spettatore nulla si consuma, accanto a un giocatore in sopravvivenza che
     * con la stessa chiamata si consuma: senza la coppia il test passerebbe anche con {@code onArmorHurt} vuoto. La
     * chiamata e' diretta, perche' {@code hurt} rifiuterebbe prima i primi due.
     */
    private static void noWearInCreativeOrSpectator(GameTestHelper helper) {
        ServerPlayer creative = Compat.gameTests().mockServerPlayer(helper);
        creative.setGameMode(GameType.CREATIVE); // su 1.20.1 il giocatore finto non nasce sempre con instabuild
        wearScrap(helper, creative, new Vec3(1.5, 2.0, 1.5), 5_000);
        ServerPlayer spectator = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, spectator, new Vec3(4.5, 2.0, 1.5), 5_000);
        spectator.setGameMode(GameType.SPECTATOR);
        ServerPlayer survivor = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, survivor, new Vec3(1.5, 2.0, 4.5), 5_000);
        helper.assertTrue(creative.getAbilities().instabuild && spectator.isSpectator()
                && !survivor.getAbilities().instabuild && !survivor.isSpectator(), "premise: game modes "
                + creative.gameMode.getGameModeForPlayer() + ", " + spectator.gameMode.getGameModeForPlayer() + ", "
                + survivor.gameMode.getGameModeForPlayer());
        for (ServerPlayer x : List.of(creative, spectator, survivor)) {
            SuitDamage.onArmorHurt(x, x.damageSources().cactus(), 8.0F);
        }
        assertUnworn(helper, creative, "creative");
        assertUnworn(helper, spectator, "spectator");
        helper.assertTrue(worn(survivor, PieceSlot.HELMET).durability() == 179
                        && carry(survivor, PieceSlot.HELMET) == 200,
                "survivor helmet " + worn(survivor, PieceSlot.HELMET).durability() + " / "
                        + carry(survivor, PieceSlot.HELMET) + ", expected 179 / 200");
        leaveTestFrame(creative);
        leaveTestFrame(spectator);
        leaveTestFrame(survivor);
        helper.succeed();
    }

    /**
     * La lava consuma una volta per colpo applicato: il secondo colpo uguale nello stesso tick il gioco lo rifiuta
     * (finestra di invulnerabilita'), e con l'aggancio in testa a {@code hurtArmor} non arriva. 4 000 millesimi: solo
     * il torso perde un punto.
     */
    private static void lavaWearsOncePerAppliedHit(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        helper.assertTrue(p.hurt(p.damageSources().lava(), 4.0F), "lava hit refused");
        helper.assertTrue(!p.hurt(p.damageSources().lava(), 4.0F), "second lava hit in the same tick applied");
        assertWear(helper, p, "after lava", 180, 600, 179, 200, 180, 500, 180, 600);
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Un'incudine (tag {@code damages_helmet}) e' un colpo alla testa anche se il sorgente e' ai piedi della vittima.
     * Su 1.20.1 a {@code hurtArmor} arrivano 4,0 punti (elmo -2), su 1.21.1 il gioco li riduce a 3,0 (elmo -1): si
     * chiede solo almeno un punto, e che l'elmo perda piu' di ogni altro pezzo.
     */
    private static void anvilHitsTheHelmet(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        DamageSource anvil = p.damageSources().anvil(null);
        DamageKind kind = Compat.damage().classify(p, anvil);
        helper.assertTrue(kind == DamageKind.HEAD, "anvil classified as " + kind + ", expected HEAD");
        helper.assertTrue(p.hurt(anvil, 4.0F), "anvil hit refused");
        int helmetLost = NEW - worn(p, PieceSlot.HELMET).durability();
        helper.assertTrue(helmetLost >= 1, "helmet lost " + helmetLost + ", expected at least 1");
        for (PieceSlot slot : PieceSlot.values()) {
            int lost = NEW - worn(p, slot).durability();
            helper.assertTrue(slot == PieceSlot.HELMET || lost < helmetLost,
                    slot + " lost " + lost + ", not less than the helmet's " + helmetLost);
        }
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Un colpo rompe l'elmo (gia' critico) e porta il torso in stato critico: un messaggio e un suono per il pezzo
     * rotto, un messaggio per il torso, nessuno per l'elmo che era gia' critico. Il tick dell'energia toglie la
     * protezione dell'elmo (0,85 x 15 = 12,75, arrotondato a 13) e {@code SlotGuard} ridisegna il riassunto.
     */
    private static void aBreakingPieceLowersTheArmorAndTells(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        boolean changed = Compat.frameState().changeWorn(p, s -> {
            s.pieces().get(PieceSlot.HELMET).damage(179);
            s.pieces().get(PieceSlot.TORSO).damage(152);
        });
        helper.assertTrue(changed, "changeWorn refused");
        long t0 = now(helper);
        SuitTick.tick(p, t0);
        OptionalDouble armor = Compat.gameTests().frameModifier(p, "frame_armor");
        helper.assertTrue(armor.equals(OptionalDouble.of(15.0)), "frame_armor " + armor + ", expected 15");
        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "cactus hit refused");
            helper.assertTrue(sounds.sounds().equals(List.of(FrameSounds.Sound.PIECE_BROKEN)),
                    "sounds " + sounds.sounds());
        }
        helper.assertTrue(worn(p, PieceSlot.HELMET).isBroken(),
                "helmet at " + worn(p, PieceSlot.HELMET).durability() + ", expected broken");
        helper.assertTrue(worn(p, PieceSlot.TORSO).durability() == 26
                        && worn(p, PieceSlot.TORSO).condition() == PieceCondition.CRITICAL,
                "torso at " + worn(p, PieceSlot.TORSO).durability() + ", expected 26 (critical)");
        int broken = messages(p, SuitDamage.PIECE_BROKEN);
        int critical = messages(p, SuitDamage.PIECE_CRITICAL);
        helper.assertTrue(broken == 1, broken + " broken-piece messages, expected 1");
        helper.assertTrue(critical == 1, critical + " critical-piece messages, expected 1 (the torso)");
        SuitTick.tick(p, t0 + 1);
        armor = Compat.gameTests().frameModifier(p, "frame_armor");
        helper.assertTrue(armor.equals(OptionalDouble.of(13.0)), "frame_armor " + armor + ", expected 13");
        SlotGuard.tick(p);
        PieceCondition shown = Compat.itemData().suitSummary(p.getItemBySlot(EquipmentSlot.CHEST)).orElseThrow()
                .pieces().get(PieceSlot.HELMET).condition();
        helper.assertTrue(shown == PieceCondition.BROKEN, "summary shows the helmet " + shown + ", expected BROKEN");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * A cella vuota il tick non toglie RE e non segna nulla: la sincronizzazione dopo l'usura viene solo dal segno
     * {@code markDirty}. L'usura da sola non sincronizza; il tick a 5 tick dall'ultima sincronizzazione si'.
     */
    private static void wearAtEmptyStillReachesTheOwner(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        setStored(p, 0);
        long t0 = now(helper);
        SuitTick.tick(p, t0);
        int before = Compat.gameTests().syncCount(p);
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "cactus hit refused");
        int afterHit = Compat.gameTests().syncCount(p);
        helper.assertTrue(afterHit == before, (afterHit - before) + " syncs from the wear itself, expected 0");
        SuitTick.tick(p, t0 + 5);
        int afterTick = Compat.gameTests().syncCount(p);
        helper.assertTrue(afterTick == before + 1, (afterTick - before) + " syncs after the tick, expected 1");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Una caduta di 10 blocchi ({@code ceil((10 - 3) x 1) = 7}, 7 000 millesimi, sulle due versioni): il pilota non
     * perde salute; le gambe sono i primari (60 %: 2 100 ciascuna, 2 punti e riporto 100), gli altri 2 800 secondo le
     * quote (elmo 600, torso 1 200, braccia 500). Il controllo: un giocatore senza telaio, nella stessa struttura,
     * perde esattamente 7,0 (senza, un giocatore protetto per un'altra ragione farebbe passare il test). Il giocatore
     * finto non cade da se' ({@code checkFallDamage} di {@code ServerPlayer} e' vuoto): {@code causeFallDamage} si
     * chiama a mano, e il suo valore non si guarda (e' vero anche quando l'evento annulla).
     */
    private static void fallWearsTheLegsAndNeverHurts(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        p.causeFallDamage(10.0F, 1.0F, p.damageSources().fall());
        assertHealth(helper, p, 20.0F, "suited pilot after the fall");
        assertWear(helper, p, "after the fall", 180, 600, 179, 200, 180, 500, 178, 100);
        ServerPlayer bare = Compat.gameTests().survivalPlayer(helper);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        Vec3 spot = helper.absoluteVec(new Vec3(5.5, 2.0, 5.5));
        bare.moveTo(spot.x, spot.y, spot.z, 0f, 0f);
        bare.causeFallDamage(10.0F, 1.0F, bare.damageSources().fall());
        assertHealth(helper, bare, 13.0F, "player without a frame after the fall");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * L'atterraggio della perla dell'Ender e' una caduta ({@code hurt(fall(), 5.0F)} nel gioco): annullato, e le gambe
     * prendono 1 500 millesimi ciascuna (1 punto, riporto 500); il torso, con 857 millesimi, non perde punti.
     */
    private static void pearlLandingWearsTheLegs(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        helper.assertTrue(!p.hurt(p.damageSources().fall(), 5.0F), "pearl landing not cancelled");
        assertHealth(helper, p, 20.0F, "suited pilot after the pearl landing");
        assertPiece(helper, p, "after the pearl landing", PieceSlot.LEFT_LEG, 179, 500);
        assertPiece(helper, p, "after the pearl landing", PieceSlot.RIGHT_LEG, 179, 500);
        int torso = worn(p, PieceSlot.TORSO).durability();
        helper.assertTrue(torso == NEW, "torso " + torso + " after the pearl landing, expected " + NEW);
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Un telaio senza pezzi protegge comunque il pilota dalla caduta (effetto di base del telaio) e non consuma
     * nulla: nessun pezzo, nessun riporto.
     */
    private static void fallOnABareFrameWearsNothing(GameTestHelper helper) {
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        FrameState state = ownedBy(p);
        state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 5_000));
        wearFrameAt(helper, p, state, FEET);
        p.causeFallDamage(10.0F, 1.0F, p.damageSources().fall());
        assertHealth(helper, p, 20.0F, "pilot of a bare frame after the fall");
        FrameState after = Compat.frameState().get(p).orElseThrow();
        helper.assertTrue(after.pieces().isEmpty(), "pieces " + after.pieces().keySet() + ", expected none");
        helper.assertTrue(after.damageCarryMilli().isEmpty(),
                "carry " + after.damageCarryMilli() + ", expected none");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Con le due gambe rotte la caduta passa tutta agli altri pezzi integri, con le quote su 0,70: elmo 1 500, torso
     * 3 000, braccia 1 250 ciascuna; il pilota non perde salute.
     */
    private static void brokenLegsPassTheFallToOtherPieces(GameTestHelper helper) {
        ServerPlayer p = suited(helper);
        boolean changed = Compat.frameState().changeWorn(p, s -> {
            s.pieces().get(PieceSlot.LEFT_LEG).damage(180);
            s.pieces().get(PieceSlot.RIGHT_LEG).damage(180);
        });
        helper.assertTrue(changed, "changeWorn refused");
        p.causeFallDamage(10.0F, 1.0F, p.damageSources().fall());
        assertHealth(helper, p, 20.0F, "pilot with broken legs after the fall");
        assertPiece(helper, p, "after the fall", PieceSlot.HELMET, 179, 500);
        assertPiece(helper, p, "after the fall", PieceSlot.TORSO, 177, 0);
        assertPiece(helper, p, "after the fall", PieceSlot.LEFT_ARM, 179, 250);
        assertPiece(helper, p, "after the fall", PieceSlot.RIGHT_ARM, 179, 250);
        for (PieceSlot leg : List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG)) {
            int durability = worn(p, leg).durability();
            helper.assertTrue(durability == 0, leg + " " + durability + " after the fall, expected 0");
        }
        leaveTestFrame(p);
        helper.succeed();
    }

    /** Un giocatore in sopravvivenza dentro sei pezzi Rottame nuovi, con 5 000 RE, in {@link #FEET}. */
    private static ServerPlayer suited(GameTestHelper helper) {
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, p, FEET, 5_000);
        return p;
    }

    /** Tutte le durate a 180 e tutti i riporti a 0. */
    private static void assertUnworn(GameTestHelper helper, ServerPlayer p, String when) {
        assertWear(helper, p, when, NEW, 0, NEW, 0, NEW, 0, NEW, 0);
    }

    /**
     * Durata e riporto di ogni pezzo: elmo, torso, le due braccia (uguali) e le due gambe (uguali), in quest'ordine.
     */
    private static void assertWear(GameTestHelper helper, ServerPlayer p, String when, int helmet, int helmetCarry,
            int torso, int torsoCarry, int arm, int armCarry, int leg, int legCarry) {
        assertPiece(helper, p, when, PieceSlot.HELMET, helmet, helmetCarry);
        assertPiece(helper, p, when, PieceSlot.TORSO, torso, torsoCarry);
        assertPiece(helper, p, when, PieceSlot.LEFT_ARM, arm, armCarry);
        assertPiece(helper, p, when, PieceSlot.RIGHT_ARM, arm, armCarry);
        assertPiece(helper, p, when, PieceSlot.LEFT_LEG, leg, legCarry);
        assertPiece(helper, p, when, PieceSlot.RIGHT_LEG, leg, legCarry);
    }

    /** La salute del giocatore, esatta. */
    private static void assertHealth(GameTestHelper helper, ServerPlayer p, float expected, String who) {
        float health = p.getHealth();
        helper.assertTrue(health == expected, who + ": health " + health + ", expected " + expected);
    }

    private static void assertPiece(GameTestHelper helper, ServerPlayer p, String when, PieceSlot slot,
            int durability, int carry) {
        int actual = worn(p, slot).durability();
        int actualCarry = carry(p, slot);
        helper.assertTrue(actual == durability && actualCarry == carry, when + ": " + slot + " " + actual + " / "
                + actualCarry + ", expected " + durability + " / " + carry);
    }
}
