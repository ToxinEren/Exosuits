package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.station.StationMenu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco del menu della stazione di manutenzione (design §5.2 e §5.6). Il menu non si apre dalla rete del
 * loader: {@code openStation} costruisce il menu del server con lo stesso costruttore del clic vero e lo mette in
 * {@code player.containerMenu} (perche', lo dice il suo Javadoc); che il clic apra davvero la schermata lo prova il
 * client vero, con tools/client/m4a1_shots.sh. I pulsanti passano dal gestore del pacchetto del gioco base
 * ({@code pressButton}), con i suoi controlli. Le posizioni sono relative alla struttura del test: la stazione sta in
 * {@link #MAIN}, il giocatore due blocchi a sud ({@link #STAND}); ogni test chiude il menu e toglie la stazione,
 * anche se fallisce. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class StationMenuGameTests {
    /** Dove sta la stazione dei test, relativo alla struttura. */
    private static final BlockPos MAIN = new BlockPos(3, 1, 3);

    /** Dove stanno i piedi del giocatore, relativo alla struttura: due blocchi a sud della stazione. */
    private static final Vec3 STAND = new Vec3(3.5, 1.0, 5.5);

    /** Numeri di pulsante che non sono quello di prova: nessuno cambia il contatore. */
    private static final int[] OTHER_BUTTONS = {0, 101, 103, 127, -1};

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public StationMenuGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "placeholderButtonCountsOnTheServer", StationMenuGameTests::placeholderButtonCountsOnTheServer);
        add(tests, "placeholderButtonIgnoresSpectatorsAndTheDead",
                StationMenuGameTests::placeholderButtonIgnoresSpectatorsAndTheDead);
        return tests;
    }

    /**
     * Il pulsante di prova conta sul server: ogni pressione del numero {@link StationMenu#TEST_BUTTON} per il menu
     * aperto fa crescere il contatore di uno; gli altri numeri, un id di menu che non e' quello aperto e, chiuso il
     * menu, il pacchetto con l'id vecchio (il gioco base lo scarta) non lo cambiano.
     */
    private static void placeholderButtonCountsOnTheServer(GameTestHelper helper) {
        helper.setBlock(MAIN, ModBlocks.STATION.get());
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        p.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, p, STAND);
        StationMenu menu = openStation(helper, p, MAIN);
        try {
            pressButton(p, menu.containerId, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 1, "presses after the first test button: " + menu.testPresses());
            for (int id : OTHER_BUTTONS) {
                pressButton(p, menu.containerId, id);
                helper.assertTrue(menu.testPresses() == 1, "button " + id + " changed the presses to "
                        + menu.testPresses());
            }
            pressButton(p, menu.containerId + 1, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 1, "the test button of another menu id changed the presses to "
                    + menu.testPresses());
            pressButton(p, menu.containerId, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 2, "presses after the second test button: " + menu.testPresses());

            p.closeContainer();
            helper.assertTrue(p.containerMenu == p.inventoryMenu, "the station menu is still open");
            pressButton(p, menu.containerId, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 2, "the test button of a closed menu changed the presses to "
                    + menu.testPresses());
        } finally {
            closeStation(helper, p);
        }
        helper.succeed();
    }

    /**
     * Uno spettatore non preme il pulsante di prova (il gioco base scarta il pacchetto) e il menu non vale per lui; un
     * giocatore morto, in sopravvivenza, non lo preme nemmeno chiamando il menu direttamente, e il menu non vale per
     * lui. Tutto nello stesso tick: il gioco non arriva a far morire davvero il giocatore finto, che torna a salute
     * piena.
     */
    private static void placeholderButtonIgnoresSpectatorsAndTheDead(GameTestHelper helper) {
        helper.setBlock(MAIN, ModBlocks.STATION.get());
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        standFacingNorth(helper, p, STAND);
        StationMenu menu = openStation(helper, p, MAIN);
        try {
            p.setGameMode(GameType.SPECTATOR);
            pressButton(p, menu.containerId, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 0, "a spectator pressed the test button: " + menu.testPresses());
            helper.assertTrue(!menu.stillValid(p), "the station menu is still valid for a spectator");

            p.setGameMode(GameType.SURVIVAL);
            p.setHealth(0f);
            try {
                helper.assertTrue(!menu.clickMenuButton(p, StationMenu.TEST_BUTTON),
                        "a dead player pressed the test button");
                helper.assertTrue(menu.testPresses() == 0, "presses after a dead player: " + menu.testPresses());
                helper.assertTrue(!menu.stillValid(p), "the station menu is still valid for a dead player");
            } finally {
                p.setHealth(p.getMaxHealth());
            }
        } finally {
            closeStation(helper, p);
        }
        helper.succeed();
    }

    /** Chiude il menu del giocatore, se e' ancora aperto, e toglie la stazione. */
    private static void closeStation(GameTestHelper helper, ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        helper.setBlock(MAIN, Blocks.AIR);
    }
}
