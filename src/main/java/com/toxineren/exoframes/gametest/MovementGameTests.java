package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.frame.SuitMovement;
import com.toxineren.exoframes.frame.SuitTick;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco della tuta in acqua (design §12.6). Una vasca profonda 3 ({@code fillPool} da (2, 2, 2) a (4, 4, 4):
 * pavimento di pietra a y 1, vetro attorno); il pelo del fluido e' a circa 2,89 sopra i piedi, quindi gli occhi della
 * tuta (2,67) e quelli di un giocatore senza tuta (1,62) sono sotto. Il giocatore con la tuta entra sul pavimento
 * ancora asciutto e solo dopo arriva il fluido, che riempie anche il punto in cui sta: l'ingresso non dipende da un
 * telaio piazzato nel fluido. Il controllo e' un secondo giocatore senza telaio sul fondo della stessa vasca, in un
 * punto che non tocca il riquadro della tuta (i due non si spingono). Tutti e due in sopravvivenza
 * ({@code survivalPlayer}).
 * <p>
 * {@code isInWater()}, {@code isUnderWater()} e {@code isInLava()} li scrive solo {@code Entity.baseTick}, che per il
 * giocatore finto gira solo dentro {@code ServerPlayer.doTick()} (il livello chiama solo {@code ServerPlayer.tick()}):
 * ogni test fa due {@code doTick()} a ciascuno ({@code isUnderWater()} legge i fluidi sugli occhi del tick prima) e
 * asserisce la condizione di partenza, altrimenti i campi sarebbero quelli di prima e il test passerebbe o fallirebbe
 * per la ragione sbagliata. {@code doTick()} manda anche
 * {@code PLAYER_PRE}/{@code PLAYER_POST}, quindi fa girare {@code SuitTick} con l'ora del server: i tick diretti dopo
 * usano {@code now(helper) + k} con {@code k >= 1}.
 * <p>
 * Lo spettatore non ha test in mondo: per lui il gioco da' gia' falso (vola). Lo scatto sotto il 10 % lo decide il
 * client, con gli indicatori ricevuti dal server; qui il server non ne ha ({@code clientIndicators} e' la cache del
 * client): lo prova il client vero dal Task 23, quando {@code /exoframe debug energy} puo' portare la cella sotto il
 * 10 %. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class MovementGameTests {
    /** I piedi della tuta, sul pavimento della vasca, al centro. */
    private static final Vec3 SUIT_FEET = new Vec3(3.5, 2.0, 3.5);

    /** I piedi del controllo, sul fondo della stessa vasca, fuori dal riquadro della tuta. */
    private static final Vec3 CONTROL_FEET = new Vec3(2.5, 2.0, 2.5);

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public MovementGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "suitIgnoresWaterForMovement", MovementGameTests::suitIgnoresWaterForMovement);
        add(tests, "suitDoesNotSwimSprintingUnderwater", MovementGameTests::suitDoesNotSwimSprintingUnderwater);
        add(tests, "lavaKeepsVanillaMovement", MovementGameTests::lavaKeepsVanillaMovement);
        add(tests, "waterHintOncePerEntry", MovementGameTests::waterHintOncePerEntry);
        return tests;
    }

    /** In acqua i fluidi non contano per il movimento della tuta e la corrente non la sposta; il controllo si'. */
    private static void suitIgnoresWaterForMovement(GameTestHelper helper) {
        ServerPlayer p = suitInPool(helper, Blocks.WATER.defaultBlockState());
        ServerPlayer control = controlInPool(helper);
        settle(p);
        settle(control);
        assertUnderWater(helper, p, "suit");
        assertUnderWater(helper, control, "control");
        helper.assertTrue(!p.isAffectedByFluids(), "fluids still affect the suit in water");
        helper.assertTrue(!p.isPushedByFluid(), "water still pushes the suit");
        helper.assertTrue(control.isAffectedByFluids(), "fluids do not affect the control in water");
        helper.assertTrue(control.isPushedByFluid(), "water does not push the control");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Nel gioco base chi corre con gli occhi sott'acqua entra a nuoto: il controllo nuota, la tuta no.
     */
    private static void suitDoesNotSwimSprintingUnderwater(GameTestHelper helper) {
        ServerPlayer p = suitInPool(helper, Blocks.WATER.defaultBlockState());
        ServerPlayer control = controlInPool(helper);
        settle(p);
        settle(control);
        assertUnderWater(helper, p, "suit");
        assertUnderWater(helper, control, "control");
        p.setSprinting(true);
        control.setSprinting(true);
        p.doTick();
        control.doTick();
        helper.assertTrue(!p.isSwimming(), "the suit swims sprinting under water");
        helper.assertTrue(control.isSwimming(), "the control does not swim sprinting under water");
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * In lava il movimento resta quello del gioco ({@code isAffectedByFluids} vero, come il controllo), salvo le
     * correnti, che non spostano la tuta (design §12.6). La resistenza al fuoco tiene la lava lontana da tuta e
     * controllo nel {@code doTick}: senza, il colpo consumerebbe la tuta e ferirebbe il controllo.
     */
    private static void lavaKeepsVanillaMovement(GameTestHelper helper) {
        ServerPlayer p = suitInPool(helper, Blocks.LAVA.defaultBlockState());
        ServerPlayer control = controlInPool(helper);
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        control.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        settle(p);
        settle(control);
        helper.assertTrue(p.isInLava(), "suit not in lava");
        helper.assertTrue(control.isInLava(), "control not in lava");
        helper.assertTrue(p.isAffectedByFluids(), "lava no longer affects the suit");
        helper.assertTrue(control.isAffectedByFluids(), "lava does not affect the control");
        helper.assertTrue(!p.isPushedByFluid(), "lava currents still push the suit");
        helper.assertTrue(control.isPushedByFluid(), "lava currents do not push the control");
        p.clearFire();
        control.clearFire();
        leaveTestFrame(p);
        helper.succeed();
    }

    /**
     * Il suggerimento dell'acqua parte una volta per ingresso: due tick in acqua, un messaggio; uscita (record
     * scartato), nuovo ingresso nello stesso punto e un tick, un secondo messaggio. Il campo {@code isInWater()} resta
     * quello dell'ultimo {@code doTick}. Il secondo {@code doTick} di {@code settle} fa gia' girare il tick
     * dell'energia con la tuta in acqua: il messaggio puo' partire li', e i due tick diretti dopo non lo ripetono.
     */
    private static void waterHintOncePerEntry(GameTestHelper helper) {
        ServerPlayer p = suitInPool(helper, Blocks.WATER.defaultBlockState());
        settle(p);
        assertUnderWater(helper, p, "suit");
        SuitTick.tick(p, now(helper) + 1);
        SuitTick.tick(p, now(helper) + 2);
        int first = messages(p, SuitMovement.WATER_HINT);
        helper.assertTrue(first == 1, "water hint shown " + first + " times in the first entry, expected 1");
        leaveTestFrame(p);
        wearScrap(helper, p, SUIT_FEET, 5_000);
        SuitTick.tick(p, now(helper) + 3);
        int second = messages(p, SuitMovement.WATER_HINT);
        helper.assertTrue(second == 2, "water hint shown " + second + " times after the second entry, expected 2");
        leaveTestFrame(p);
        helper.succeed();
    }

    /** Un giocatore in sopravvivenza entra nella tuta sul pavimento asciutto, poi la vasca si riempie del fluido. */
    private static ServerPlayer suitInPool(GameTestHelper helper, BlockState fluid) {
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, p, SUIT_FEET, 5_000);
        fillPool(helper, new BlockPos(2, 2, 2), new BlockPos(4, 4, 4), fluid);
        return p;
    }

    /** Il controllo: un giocatore in sopravvivenza senza telaio, sul fondo della vasca gia' piena. */
    private static ServerPlayer controlInPool(GameTestHelper helper) {
        ServerPlayer control = Compat.gameTests().survivalPlayer(helper);
        Vec3 feet = helper.absoluteVec(CONTROL_FEET);
        control.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        return control;
    }

    /**
     * Due {@code doTick()}: il primo scrive {@code isInWater()} e i fluidi sugli occhi, il secondo copia questi ultimi
     * nel segno che {@code isUnderWater()} legge ({@code updateFluidOnEyes} lo aggiorna prima di ricalcolarli).
     */
    private static void settle(ServerPlayer player) {
        player.doTick();
        player.doTick();
    }

    /** La condizione di partenza in acqua: il giocatore la tocca e ha gli occhi sotto il pelo. */
    private static void assertUnderWater(GameTestHelper helper, ServerPlayer player, String who) {
        helper.assertTrue(player.isInWater() && player.isUnderWater(),
                who + " not under water: inWater " + player.isInWater() + ", underWater " + player.isUnderWater());
    }
}
