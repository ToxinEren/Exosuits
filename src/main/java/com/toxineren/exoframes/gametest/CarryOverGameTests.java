package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dei riporti di §11 nella parte 3c (i lavori rimasti dalla 3b), con in testa la sonda sul tick del
 * giocatore finto su cui contano i test della parte. Le regole comuni a tutte le classi di test e gli aiuti stanno in
 * {@link GameTestKit}.
 */
public final class CarryOverGameTests {
    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public CarryOverGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "probeLevelTicksTheMockWithoutPlayerTickEvents",
                CarryOverGameTests::probeLevelTicksTheMockWithoutPlayerTickEvents);
        return tests;
    }

    /**
     * Sonda: il livello fa avanzare {@code tickCount} del giocatore finto e chiama il suo {@code ServerPlayer.tick()},
     * che scala {@code invulnerableTime} da 20 dopo un colpo (15 dopo cinque tick); {@code Player.tick()}, e quindi
     * {@code TickEvent.PLAYER_PRE}, per il giocatore finto non girano mai. I test di energia, danno e movimento della
     * parte contano su questo: chiamano a mano i tick della mod e non aspettano che un evento li faccia girare.
     */
    private static void probeLevelTicksTheMockWithoutPlayerTickEvents(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        helper.assertTrue(!player.getAbilities().instabuild && !player.getAbilities().invulnerable
                && !player.isSpectator(), "survivalPlayer is not in survival");
        boolean[] seen = {false};
        TickEvent.Player listener = ticked -> {
            if (ticked == player) {
                seen[0] = true;
            }
        };
        TickEvent.PLAYER_PRE.register(listener);
        int start = player.tickCount;
        helper.assertTrue(player.hurt(player.damageSources().cactus(), 1.0F), "spawn protection still on");
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(player.tickCount == start + 5,
                        "tickCount " + player.tickCount + ", expected " + (start + 5));
                helper.assertTrue(player.invulnerableTime == 15,
                        "invulnerableTime " + player.invulnerableTime + ", expected 15");
                helper.assertTrue(!seen[0], "PLAYER_PRE fired for the mock player");
            } finally {
                TickEvent.PLAYER_PRE.unregister(listener);
            }
            helper.succeed();
        });
    }
}
