package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.frame.SuitTick;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco del tick dell'energia (design §12.3): consumo da fermi, camminando e correndo, salti, teletrasporti,
 * cambio di dimensione, cavalcature, creativa e spettatore. Tutto nello stesso tick del server: i tempi dati a
 * {@link SuitTick#tick} sono {@code now(helper)} piu' k, sempre in avanti; i numeri vengono dai valori predefiniti
 * della config (fermo 100, cammino 400, corsa 1000 millesimi di RE per tick, salto 15 RE). Le regole comuni e gli
 * aiuti stanno in {@link GameTestKit}.
 */
public final class EnergyGameTests {
    private static final Vec3 FEET = new Vec3(2.5, 1.0, 2.5);

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public EnergyGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "tickListenerCreatesTheRecord", EnergyGameTests::tickListenerCreatesTheRecord);
        add(tests, "idleDrainsTwoPerSecond", EnergyGameTests::idleDrainsTwoPerSecond);
        add(tests, "walkingDrainsEight", EnergyGameTests::walkingDrainsEight);
        add(tests, "sprintingDrainsTwenty", EnergyGameTests::sprintingDrainsTwenty);
        add(tests, "aJumpCostsFifteen", EnergyGameTests::aJumpCostsFifteen);
        add(tests, "noJumpCostWithAnEmptyCell", EnergyGameTests::noJumpCostWithAnEmptyCell);
        add(tests, "teleportCountsAsIdle", EnergyGameTests::teleportCountsAsIdle);
        add(tests, "dimensionChangeRestartsTheWindow", EnergyGameTests::dimensionChangeRestartsTheWindow);
        add(tests, "passengerIsDismountedAndIdle", EnergyGameTests::passengerIsDismountedAndIdle);
        add(tests, "creativeDrainsNothingBesideSurvival", EnergyGameTests::creativeDrainsNothingBesideSurvival);
        add(tests, "spectatorDrainsNothingAndKeepsTheRecord",
                EnergyGameTests::spectatorDrainsNothingAndKeepsTheRecord);
        return tests;
    }

    /**
     * L'ascoltatore di {@code TickEvent.PLAYER_PRE} e' registrato e crea il record (il server non manda l'evento ai
     * giocatori finti, quindi lo si chiama a mano); l'uscita lo scarta, da {@code WornState.changed}.
     */
    private static void tickListenerCreatesTheRecord(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        wearScrap(helper, player, FEET, 5_000);
        helper.assertTrue(!SuitTick.hasRecord(player), "record before the first tick");
        TickEvent.PLAYER_PRE.invoker().tick(player);
        helper.assertTrue(SuitTick.hasRecord(player), "PLAYER_PRE did not create the record");
        leaveTestFrame(player);
        helper.assertTrue(!SuitTick.hasRecord(player), "record kept after leaving the frame");
        helper.succeed();
    }

    /** Fermi per un secondo: 2 RE (100 millesimi della preparazione piu' 20 x 100). */
    private static void idleDrainsTwoPerSecond(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        for (int i = 1; i <= 20; i++) {
            SuitTick.tick(p, t0 + i);
        }
        helper.assertTrue(stored(p) == 4_998, stored(p) + " RE, expected 4 998");
        leaveTestFrame(p);
        helper.succeed();
    }

    /** Un secondo di cammino, 0,2 blocchi per tick: 8 RE. */
    private static void walkingDrainsEight(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        walk(p, p.position(), 0.2, t0, 1, 20);
        helper.assertTrue(stored(p) == 4_992, stored(p) + " RE, expected 4 992");
        leaveTestFrame(p);
        helper.succeed();
    }

    /** Un secondo di corsa: 20 RE. */
    private static void sprintingDrainsTwenty(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        p.setSprinting(true);
        walk(p, p.position(), 0.2, t0, 1, 20);
        helper.assertTrue(stored(p) == 4_980, stored(p) + " RE, expected 4 980");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Un salto della statistica costa 15 RE una volta sola; un salto con {@code hurtTime > 0} (contraccolpo) non si
     * paga, ne' subito ne' piu' tardi.
     */
    private static void aJumpCostsFifteen(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        p.awardStat(Stats.JUMP);
        SuitTick.tick(p, t0 + 1);
        helper.assertTrue(stored(p) == 4_985, stored(p) + " RE after the jump, expected 4 985");
        SuitTick.tick(p, t0 + 2);
        helper.assertTrue(stored(p) == 4_985, stored(p) + " RE with no new jump, expected 4 985");
        p.hurtTime = 10;
        p.awardStat(Stats.JUMP);
        SuitTick.tick(p, t0 + 3);
        helper.assertTrue(stored(p) == 4_985, stored(p) + " RE after a knockback jump, expected 4 985");
        p.hurtTime = 0;
        SuitTick.tick(p, t0 + 4);
        helper.assertTrue(stored(p) == 4_985, stored(p) + " RE: the unpaid jump was paid later");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * A cella vuota il salto non costa nulla e non lancia (un'eccezione nel tick scriverebbe
     * {@code Exoframe tick failed for ...}, che il controllo del log fa fallire); la base della statistica e' riletta
     * anche senza pagare, quindi con la cella di nuovo carica quel salto non si paga.
     */
    private static void noJumpCostWithAnEmptyCell(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        setStored(p, 0);
        p.awardStat(Stats.JUMP);
        SuitTick.tick(p, t0 + 1);
        helper.assertTrue(stored(p) == 0, stored(p) + " RE, expected 0");
        setStored(p, 5_000);
        SuitTick.tick(p, t0 + 2);
        helper.assertTrue(stored(p) == 5_000, stored(p) + " RE: the jump at an empty cell was paid later");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Nove blocchi in un tick sono un teletrasporto: quel tick e' fermo e la finestra riparte. Contato come cammino,
     * la finestra darebbe {@code WALK} per quattro tick (4 998).
     */
    private static void teleportCountsAsIdle(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        Vec3 start = p.position();
        p.setPos(start.x + 9.0, start.y, start.z);
        SuitTick.tick(p, t0 + 1);
        for (int i = 2; i <= 10; i++) {
            SuitTick.tick(p, t0 + i);
        }
        helper.assertTrue(stored(p) == 4_999, stored(p) + " RE, expected 4 999");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Al cambio di dimensione la finestra riparte: dopo tre tick di cammino, quattro tick fermi nel Nether sono
     * {@code IDLE} (senza ripartire, i primi tre sarebbero {@code WALK}). Tutto nello stesso tick del server: il
     * livello non vede mai il giocatore nell'altra dimensione.
     */
    private static void dimensionChangeRestartsTheWindow(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        Vec3 start = p.position();
        ServerLevel overworld = p.serverLevel();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "no nether on the test server");
        walk(p, start, 0.2, t0, 1, 3);
        helper.assertTrue(stored(p) == 4_999, stored(p) + " RE after walking, expected 4 999");
        try {
            p.setServerLevel(nether);
            for (int i = 4; i <= 7; i++) {
                SuitTick.tick(p, t0 + i);
            }
            helper.assertTrue(stored(p) == 4_999,
                    stored(p) == 4_998 ? "4 998: the window was not restarted at the dimension change"
                            : stored(p) + " RE, expected 4 999");
        } finally {
            p.setServerLevel(overworld);
        }
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Un passeggero scende al tick e ha il messaggio, e quel tick e' fermo: il punto di discesa si sposta di 0,2 a
     * ogni tick, e senza la regola del passeggero sarebbe cammino (4 992).
     */
    private static void passengerIsDismountedAndIdle(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        Vec3 start = p.position();
        ServerLevel level = helper.getLevel();
        Boat boat = new Boat(level, start.x, start.y, start.z + 2.0);
        level.addFreshEntity(boat);
        try {
            for (int i = 1; i <= 20; i++) {
                boat.setPos(start.x + 0.2 * i, start.y, start.z + 2.0);
                helper.assertTrue(p.startRiding(boat, true), "could not board");
                SuitTick.tick(p, t0 + i);
                helper.assertTrue(!p.isPassenger(), "still riding after tick " + i);
            }
            helper.assertTrue(stored(p) == 4_998, stored(p) + " RE, expected 4 998");
            int told = messages(p, SuitTick.LEAVE_FRAME_FIRST);
            helper.assertTrue(told == 20, told + " leave-the-frame messages, expected 20");
        } finally {
            boat.discard();
        }
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * In creativa ({@code instabuild}) non si consuma nulla, accanto a un giocatore in sopravvivenza che nello stesso
     * cammino consuma: senza la coppia, "nessun consumo in creativa" passerebbe anche con un tick che non consuma mai.
     */
    private static void creativeDrainsNothingBesideSurvival(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer survival = prepared(helper, new Vec3(1.5, 1.0, 1.5), 5_000, t0);
        ServerPlayer creative = Compat.gameTests().mockServerPlayer(helper);
        creative.setGameMode(GameType.CREATIVE);
        helper.assertTrue(creative.getAbilities().instabuild, "creative player cannot build instantly");
        wearScrap(helper, creative, new Vec3(1.5, 1.0, 5.5), 5_000);
        SuitTick.tick(creative, t0);
        walk(survival, survival.position(), 0.2, t0, 1, 20);
        walk(creative, creative.position(), 0.2, t0, 1, 20);
        helper.assertTrue(stored(survival) == 4_992, stored(survival) + " RE in survival, expected 4 992");
        helper.assertTrue(stored(creative) == 5_000, stored(creative) + " RE in creative, expected 5 000");
        leaveTestFrame(survival);
        leaveTestFrame(creative);
        helper.succeed();
    }

    /**
     * Da spettatore non si consuma nulla e il record resta; tornati in sopravvivenza il tick consuma davvero (il
     * riporto della preparazione, 100 millesimi, e' rimasto).
     */
    private static void spectatorDrainsNothingAndKeepsTheRecord(GameTestHelper helper) {
        long t0 = now(helper);
        ServerPlayer p = prepared(helper, FEET, 5_000, t0);
        Vec3 start = p.position();
        p.setGameMode(GameType.SPECTATOR);
        walk(p, start, 0.1, t0, 1, 20);
        helper.assertTrue(stored(p) == 5_000, stored(p) + " RE as a spectator, expected 5 000");
        helper.assertTrue(SuitTick.hasRecord(p), "spectator lost the record");
        p.setGameMode(GameType.SURVIVAL);
        walk(p, p.position(), 0.1, t0, 21, 40);
        helper.assertTrue(stored(p) == 4_992, stored(p) + " RE back in survival, expected 4 992");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Giocatore in sopravvivenza dentro un telaio Rottame con stored RE, e il tick di preparazione a t0: record creato,
     * 100 millesimi di riporto.
     */
    private static ServerPlayer prepared(GameTestHelper helper, Vec3 feet, int stored, long t0) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, player, feet, stored);
        SuitTick.tick(player, t0);
        helper.assertTrue(SuitTick.hasRecord(player) && stored(player) == stored, "preparation tick failed");
        return player;
    }

    /** Passi in avanti lungo x di step blocchi, uno per tick, da t0 + from a t0 + to compresi, dal punto start. */
    private static void walk(ServerPlayer player, Vec3 start, double step, long t0, int from, int to) {
        for (int i = from; i <= to; i++) {
            player.setPos(start.x + step * (i - from + 1), start.y, start.z);
            SuitTick.tick(player, t0 + i);
        }
    }
}
