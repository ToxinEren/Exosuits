package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.StationBlock;
import com.toxineren.exoframes.content.StationPart;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.station.StationReach;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FramePermissions;
import com.toxineren.exoframes.station.StationFinder;
import com.toxineren.exoframes.station.StationMenu;
import com.toxineren.exoframes.station.StationNet;
import com.toxineren.exoframes.station.StationOpening;
import com.toxineren.exoframes.station.StationViewPacket;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco del menu della stazione di manutenzione (design §5.2, §5.5, §5.6, §5.13 e i casi di §5.18): la
 * ricerca del telaio, il clic che non apre nulla, il menu che resta legato al suo telaio, la vista del telaio, nei
 * dati extra dell'apertura e rimandata quando cambia, e lo spostamento rapido fra inventario e barra degli slot del
 * giocatore. Il menu non si apre dalla rete del loader:
 * {@code openStation} cerca il telaio come il clic vero ({@link StationFinder#find}, che deve trovarlo), costruisce il
 * menu del server con lo stesso costruttore del clic e lo mette in {@code player.containerMenu} (perche', lo dice il
 * suo Javadoc); che il clic apra davvero la schermata lo prova il client vero, con tools/client/m4a1_shots.sh. Il clic
 * vero sulla stazione ({@code clickBlock}) si usa solo dove non apre nulla. I pulsanti passano dal gestore del
 * pacchetto del gioco base ({@code pressButton}), con i suoi controlli. Le posizioni sono relative alla struttura del
 * test: la stazione, posata con le due meta' da {@code placeStation} e rivolta a nord, ha la principale in
 * {@link #MAIN} e la laterale in (2, 1, 3), quindi l'impronta x [2, 4], z [3, 4] con la base a y 1; il giocatore A sta
 * due blocchi a sud ({@link #STAND}), il secondo giocatore, B, dove serve, accanto ({@link #OTHER_STAND}). Ogni test
 * chiude i menu e toglie telai, record e le due meta' della stazione, anche se fallisce. Gli aiuti comuni stanno in
 * {@link GameTestKit}.
 */
public final class StationMenuGameTests {
    /** Dove sta la meta' principale della stazione dei test, relativa alla struttura. */
    private static final BlockPos MAIN = new BlockPos(3, 1, 3);

    /** Dove stanno i piedi del giocatore, relativo alla struttura: due blocchi a sud della stazione. */
    private static final Vec3 STAND = new Vec3(3.5, 1.0, 5.5);

    /** Dove stanno i piedi del secondo giocatore, relativo alla struttura: accanto al primo, a ovest. */
    private static final Vec3 OTHER_STAND = new Vec3(2.5, 1.0, 5.5);

    /**
     * Dove sta la meta' laterale rimasta sola di {@code clickWithNoFrameInReachSaysSo}, relativa alla struttura,
     * lontana dalla stazione dei test: con il fronte a nord la sua principale starebbe a est, in (6, 1, 1).
     */
    private static final BlockPos LONE_SIDE = new BlockPos(5, 1, 1);

    /** Numeri di pulsante che non sono quello di prova: nessuno cambia il contatore. */
    private static final int[] OTHER_BUTTONS = {0, 101, 103, 127, -1};

    /** La ricerca senza telai a portata. */
    private static final StationFinder.Result NONE_IN_REACH =
            new StationFinder.Result(StationFinder.Outcome.NONE_IN_REACH, Optional.empty());

    /** La ricerca con soli telai a portata che il giocatore non puo' usare. */
    private static final StationFinder.Result ALL_LOCKED =
            new StationFinder.Result(StationFinder.Outcome.ALL_LOCKED, Optional.empty());

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public StationMenuGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "placeholderButtonCountsOnTheServer", StationMenuGameTests::placeholderButtonCountsOnTheServer);
        add(tests, "placeholderButtonIgnoresSpectatorsAndTheDead",
                StationMenuGameTests::placeholderButtonIgnoresSpectatorsAndTheDead);
        add(tests, "finderUsesTheFootprintAndTheHeight", StationMenuGameTests::finderUsesTheFootprintAndTheHeight);
        add(tests, "finderPicksTheNearestUsableFrame", StationMenuGameTests::finderPicksTheNearestUsableFrame);
        add(tests, "clickWithNoFrameInReachSaysSo", StationMenuGameTests::clickWithNoFrameInReachSaysSo);
        add(tests, "clickOnlyNearLockedFramesSaysLocked", StationMenuGameTests::clickOnlyNearLockedFramesSaysLocked);
        add(tests, "clickWithOffHandOrAsSpectatorDoesNothing",
                StationMenuGameTests::clickWithOffHandOrAsSpectatorDoesNothing);
        add(tests, "menuClosesWhenTheFrameIsPickedUp", StationMenuGameTests::menuClosesWhenTheFrameIsPickedUp);
        add(tests, "menuClosesWhenSomeoneEntersTheFrame", StationMenuGameTests::menuClosesWhenSomeoneEntersTheFrame);
        add(tests, "menuClosesForOthersWhenTheOwnerLocks",
                StationMenuGameTests::menuClosesForOthersWhenTheOwnerLocks);
        add(tests, "menuClosesWithoutAMessageForOtherReasons",
                StationMenuGameTests::menuClosesWithoutAMessageForOtherReasons);
        add(tests, "openingCarriesTheFrameView", StationMenuGameTests::openingCarriesTheFrameView);
        add(tests, "viewFollowsTheRecordWithinATick", StationMenuGameTests::viewFollowsTheRecordWithinATick);
        add(tests, "viewIsSentAtMostOncePerTick", StationMenuGameTests::viewIsSentAtMostOncePerTick);
        add(tests, "viewCarriesThePlayersRights", StationMenuGameTests::viewCarriesThePlayersRights);
        add(tests, "viewForAnotherMenuIsIgnored", StationMenuGameTests::viewForAnotherMenuIsIgnored);
        add(tests, "quickMoveSwapsInventoryAndHotbar", StationMenuGameTests::quickMoveSwapsInventoryAndHotbar);
        return tests;
    }

    /**
     * Il pulsante di prova conta sul server: ogni pressione del numero {@link StationMenu#TEST_BUTTON} per il menu
     * aperto fa crescere il contatore di uno; gli altri numeri, un id di menu che non e' quello aperto e, chiuso il
     * menu, il pacchetto con l'id vecchio (il gioco base lo scarta) non lo cambiano. Sopra la stazione un telaio del
     * giocatore: senza telaio il menu non resterebbe valido.
     */
    private static void placeholderButtonCountsOnTheServer(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        p.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, p, STAND);
        ExoframeEntity frame = frameOnStation(helper, ownedBy(p), MAIN);
        try {
            StationMenu menu = openStation(helper, p, MAIN);
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
            cleanUp(helper, side, frame, p);
        }
        helper.succeed();
    }

    /**
     * Uno spettatore non preme il pulsante di prova (il gioco base scarta il pacchetto) e il menu non vale per lui; un
     * giocatore morto, in sopravvivenza, non lo preme nemmeno chiamando il menu direttamente, e il menu non vale per
     * lui. Prima, e di nuovo in sopravvivenza da vivo, il menu vale: il telaio del giocatore sta sopra la stazione.
     * Tutto nello stesso tick: il gioco non arriva a far morire davvero il giocatore finto, che torna a salute piena.
     */
    private static void placeholderButtonIgnoresSpectatorsAndTheDead(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        p.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, p, STAND);
        ExoframeEntity frame = frameOnStation(helper, ownedBy(p), MAIN);
        try {
            StationMenu menu = openStation(helper, p, MAIN);
            helper.assertTrue(menu.stillValid(p), "the station menu is not valid right after opening");
            p.setGameMode(GameType.SPECTATOR);
            pressButton(p, menu.containerId, StationMenu.TEST_BUTTON);
            helper.assertTrue(menu.testPresses() == 0, "a spectator pressed the test button: " + menu.testPresses());
            helper.assertTrue(!menu.stillValid(p), "the station menu is still valid for a spectator");

            p.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(menu.stillValid(p), "the station menu is not valid again in survival");
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
            cleanUp(helper, side, frame, p);
        }
        helper.succeed();
    }

    /**
     * La ricerca guarda l'impronta e l'altezza (design §5.2), un telaio di A per volta: sopra la principale e sopra la
     * laterale lo trova; a sud, sul pavimento, fino a 2 blocchi dall'impronta compresi (z 6,0) e non oltre (6,01); a
     * est, un gradino piu' in basso, fino a un blocco sotto la base compreso (y 0,0) e non oltre (-0,01); sopra la
     * principale fino a due blocchi sopra la base compresi (y 3,0) e non oltre (4,0). Un telaio nello stesso punto ma
     * in un'altra dimensione non e' a portata. La laterale porta alla principale ({@link StationBlock#mainPos}).
     */
    private static void finderUsesTheFootprintAndTheHeight(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        try {
            helper.assertTrue(RedstoneExoframes.config().station.reachBlocks == 2.0,
                    "premise: station.reachBlocks is not 2.0");
            BlockPos main = helper.absolutePos(MAIN);
            helper.assertTrue(StationBlock.mainPos(helper.absolutePos(side), helper.getBlockState(side)).equals(main),
                    "the side half does not lead to the main half");
            assertFindsOneFrameAt(helper, a, new Vec3(3.5, 2.0, 3.5), true);
            assertFindsOneFrameAt(helper, a, new Vec3(2.5, 2.0, 3.5), true);
            assertFindsOneFrameAt(helper, a, new Vec3(3.5, 1.0, 6.0), true);
            assertFindsOneFrameAt(helper, a, new Vec3(3.5, 1.0, 6.01), false);
            assertFindsOneFrameAt(helper, a, new Vec3(5.5, 0.0, 3.5), true);
            assertFindsOneFrameAt(helper, a, new Vec3(5.5, -0.01, 3.5), false);
            assertFindsOneFrameAt(helper, a, new Vec3(3.5, 3.0, 3.5), true);
            assertFindsOneFrameAt(helper, a, new Vec3(3.5, 4.0, 3.5), false);
            assertOtherDimensionIsOutOfReach(helper, a);
        } finally {
            closeStation(helper, side, a);
        }
        helper.succeed();
    }

    /**
     * Fra i telai a portata la ricerca prende il piu' vicino all'impronta fra quelli che il giocatore puo' usare: con
     * L di B, bloccato, sopra la principale e U di A a un blocco dall'impronta, U; sbloccato L, L, che sta
     * sull'impronta; tolto U e bloccato di nuovo L, nessuno ({@code ALL_LOCKED}).
     */
    private static void finderPicksTheNearestUsableFrame(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerPlayer b = Compat.gameTests().mockServerPlayer(helper);
        b.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity locked = frameOnStation(helper, ownedBy(b), MAIN);
        ExoframeEntity usable = placeFrame(helper, ownedBy(a), new Vec3(3.5, 1.0, 5.0), 0f);
        try {
            assertLockedFor(helper, a, locked);
            assertFind(helper, a, found(usable), "L locked on the station, U of A one block away");
            helper.assertTrue(Compat.frameState().updatePlaced(server, locked.frameId(), s -> s.setUnlocked(true)),
                    "unlocking L failed");
            assertFind(helper, a, found(locked), "L unlocked on the station, U of A one block away");
            removeFrame(server, usable);
            helper.assertTrue(Compat.frameState().updatePlaced(server, locked.frameId(), s -> s.setUnlocked(false)),
                    "locking L again failed");
            assertFind(helper, a, ALL_LOCKED, "only L, locked again");
        } finally {
            removeFrame(server, locked);
            removeFrame(server, usable);
            closeStation(helper, side, a, b);
        }
        helper.succeed();
    }

    /**
     * Con un telaio di A a 3 blocchi dall'impronta, il clic vero di A sulla principale e poi sulla laterale (che porta
     * alla principale) non apre nulla e dice due volte, sulla barra delle azioni, che non c'e' un telaio vicino, con la
     * distanza della config scritta senza decimali ("2"); il server risponde {@code CONSUME}. La distanza si scrive
     * con un decimale se non e' intera. Una meta' laterale rimasta sola non apre nulla e non dice nulla, anche con un
     * telaio a portata ({@link #assertLoneSideHalfOpensNothing}).
     */
    private static void clickWithNoFrameInReachSaysSo(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ExoframeEntity far = placeFrame(helper, ownedBy(a), new Vec3(3.5, 1.0, 7.0), 0f);
        try {
            helper.assertTrue(RedstoneExoframes.config().station.reachBlocks == 2.0,
                    "premise: station.reachBlocks is not 2.0");
            InteractionResult onMain = clickBlock(helper, a, MAIN, InteractionHand.MAIN_HAND);
            InteractionResult onSide = clickBlock(helper, a, side, InteractionHand.MAIN_HAND);
            List<TranslatableContents> received = modMessages(a);
            helper.assertTrue(messages(a, StationBlock.NO_FRAME) == 2 && received.size() == 2,
                    "messages of the mod after the two clicks: " + received);
            helper.assertTrue(Arrays.equals(received.get(1).getArgs(), new Object[] {"2"}),
                    "the last message does not carry the distance \"2\": " + received.get(1));
            helper.assertTrue(a.containerMenu == a.inventoryMenu, "a click with no frame in reach opened a menu");
            helper.assertTrue(onMain == InteractionResult.CONSUME && onSide == InteractionResult.CONSUME,
                    "the server answered " + onMain + " on the main half and " + onSide + " on the side half");
            helper.assertTrue(StationFinder.reachText(2.0).equals("2") && StationFinder.reachText(8.0).equals("8")
                    && StationFinder.reachText(2.5).equals("2.5") && StationFinder.reachText(0.5).equals("0.5"),
                    "reachText: " + StationFinder.reachText(2.0) + ", " + StationFinder.reachText(8.0) + ", "
                            + StationFinder.reachText(2.5) + ", " + StationFinder.reachText(0.5));
            assertLoneSideHalfOpensNothing(helper, a);
        } finally {
            removeFrame(helper.getLevel().getServer(), far);
            closeStation(helper, side, a);
        }
        helper.succeed();
    }

    /**
     * Con il solo telaio di B, bloccato, sopra la principale, il clic vero di A non apre nulla e dice una volta, sulla
     * barra delle azioni, il messaggio del telaio bloccato; il server risponde {@code CONSUME}. Lo stesso con il telaio
     * di B a ovest della laterale, a 1,5 blocchi dall'impronta, e il clic sulla laterale: la laterale porta
     * all'impronta di tutta la stazione (un'impronta presa dallo stato della laterale come se fosse la principale
     * starebbe un blocco piu' a est, e il telaio, a 2,5 blocchi, non sarebbe a portata).
     */
    private static void clickOnlyNearLockedFramesSaysLocked(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerPlayer b = Compat.gameTests().mockServerPlayer(helper);
        b.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity locked = frameOnStation(helper, ownedBy(b), MAIN);
        ExoframeEntity westOfTheSide = null;
        try {
            assertLockedFor(helper, a, locked);
            InteractionResult onMain = clickBlock(helper, a, MAIN, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(a, FrameInteractions.LOCKED) == 1 && modMessages(a).size() == 1,
                    "messages of the mod after the click on the main half: " + modMessages(a));
            helper.assertTrue(a.containerMenu == a.inventoryMenu, "a click near a locked frame opened a menu");
            helper.assertTrue(onMain == InteractionResult.CONSUME, "the server answered " + onMain);

            removeFrame(server, locked);
            westOfTheSide = placeFrame(helper, ownedBy(b), new Vec3(0.5, 1.0, 3.5), 0f);
            assertLockedFor(helper, a, westOfTheSide);
            InteractionResult onSide = clickBlock(helper, a, side, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(a, FrameInteractions.LOCKED) == 2 && modMessages(a).size() == 2,
                    "messages of the mod after the click on the side half: " + modMessages(a));
            helper.assertTrue(a.containerMenu == a.inventoryMenu, "a click near a locked frame opened a menu");
            helper.assertTrue(onSide == InteractionResult.CONSUME, "the server answered " + onSide);
        } finally {
            removeFrame(server, locked);
            if (westOfTheSide != null) {
                removeFrame(server, westOfTheSide);
            }
            closeStation(helper, side, a, b);
        }
        helper.succeed();
    }

    /**
     * Senza telai, il clic vero di A con la mano secondaria e poi da spettatore con la principale non fa nulla: nessun
     * messaggio (un clic trattato per sbaglio direbbe che non c'e' un telaio vicino), nessun menu, e il server passa.
     * Per controllo, lo stesso clic con la principale in sopravvivenza arriva alla stazione e lo dice.
     */
    private static void clickWithOffHandOrAsSpectatorDoesNothing(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        try {
            InteractionResult offHand = clickBlock(helper, a, MAIN, InteractionHand.OFF_HAND);
            InteractionResult spectator;
            a.setGameMode(GameType.SPECTATOR);
            try {
                spectator = clickBlock(helper, a, MAIN, InteractionHand.MAIN_HAND);
            } finally {
                a.setGameMode(GameType.SURVIVAL);
            }
            helper.assertTrue(Compat.gameTests().messagesTo(a).isEmpty(),
                    "messages after the off hand and spectator clicks: " + Compat.gameTests().messagesTo(a));
            helper.assertTrue(a.containerMenu == a.inventoryMenu, "an off hand or spectator click opened a menu");
            helper.assertTrue(!offHand.consumesAction() && !spectator.consumesAction(),
                    "the server answered " + offHand + " to the off hand and " + spectator + " to the spectator");

            clickBlock(helper, a, MAIN, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(a, StationBlock.NO_FRAME) == 1,
                    "control: the main hand click in survival did not reach the station: "
                            + Compat.gameTests().messagesTo(a));
        } finally {
            closeStation(helper, side, a);
        }
        helper.succeed();
    }

    /**
     * A apre la stazione su un suo telaio nudo, mette due lingotti di ferro sul cursore del menu e, accovacciato a mano
     * vuota, raccoglie il telaio: il record non c'e' piu'. Due tick dopo il gioco ha chiuso il menu, A ha avuto una
     * volta il messaggio del telaio non piu' disponibile e i due lingotti sono tornati nel suo inventario; altre
     * chiamate di {@code stillValid} danno falso senza ripetere il messaggio.
     */
    private static void menuClosesWhenTheFrameIsPickedUp(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity frame = frameOnStation(helper, ownedBy(a), MAIN);
        boolean delayed = false;
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            menu.setCarried(new ItemStack(Items.IRON_INGOT, 2));
            a.setShiftKeyDown(true);
            try {
                click(frame, a, 0.0, 1.0, 0.0);
            } finally {
                a.setShiftKeyDown(false);
            }
            helper.assertTrue(Compat.frameState().placed(server, frame.frameId()).isEmpty(),
                    "the frame on the station was not picked up");
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(a.containerMenu == a.inventoryMenu,
                            "the station menu is still open after the frame was picked up");
                    h.assertTrue(messages(a, StationMenu.FRAME_GONE) == 1,
                            "frame gone messages: " + Compat.gameTests().messagesTo(a));
                    h.assertTrue(countInMainInventory(a, Items.IRON_INGOT) == 2 && menu.getCarried().isEmpty(),
                            countInMainInventory(a, Items.IRON_INGOT) + " iron ingots in the inventory, "
                                    + menu.getCarried() + " on the cursor: expected the 2 from the cursor back");
                    h.assertTrue(!menu.stillValid(a) && !menu.stillValid(a),
                            "the station menu is valid again without its frame");
                    h.assertTrue(messages(a, StationMenu.FRAME_GONE) == 1,
                            "frame gone messages after two more checks: " + Compat.gameTests().messagesTo(a));
                } finally {
                    cleanUp(h, side, frame, a);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a);
            }
        }
    }

    /**
     * A apre la stazione sul suo telaio, sbloccato e con una cella carica; B ci entra. Due tick dopo il menu di A e'
     * chiuso, con il messaggio del telaio non piu' disponibile una volta. B esce con il telaio di prova.
     */
    private static void menuClosesWhenSomeoneEntersTheFrame(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerPlayer b = Compat.gameTests().mockServerPlayer(helper);
        b.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, b, OTHER_STAND);
        FrameState state = charged(ownedBy(a));
        state.setUnlocked(true);
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        boolean delayed = false;
        try {
            openStation(helper, a, MAIN);
            helper.assertTrue(FrameEntry.enter(b, frame), "B could not enter the frame on the station");
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(a.containerMenu == a.inventoryMenu,
                            "A's station menu is still open after B entered the frame");
                    h.assertTrue(messages(a, StationMenu.FRAME_GONE) == 1,
                            "frame gone messages to A: " + Compat.gameTests().messagesTo(a));
                } finally {
                    leaveTestFrame(b);
                    cleanUp(h, side, frame, a, b);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                leaveTestFrame(b);
                cleanUp(helper, side, frame, a, b);
            }
        }
    }

    /**
     * Sul telaio di A, sbloccato, aprono la stazione A e B; A lo blocca (il registro, come {@code /exoframe lock}). Due
     * tick dopo il menu di B e' chiuso, con il messaggio del telaio non piu' disponibile una volta; quello di A, il
     * proprietario, e' ancora aperto, e A non ha avuto messaggi della mod. Sbloccato di nuovo il telaio, il menu
     * chiuso di B non torna valido e non ripete il messaggio.
     */
    private static void menuClosesForOthersWhenTheOwnerLocks(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerPlayer b = Compat.gameTests().mockServerPlayer(helper);
        b.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, b, OTHER_STAND);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState state = ownedBy(a);
        state.setUnlocked(true);
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        boolean delayed = false;
        try {
            StationMenu menuOfA = openStation(helper, a, MAIN);
            StationMenu menuOfB = openStation(helper, b, MAIN);
            helper.assertTrue(menuOfB.stillValid(b), "B's station menu is not valid on the unlocked frame");
            helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(), s -> s.setUnlocked(false)),
                    "locking the frame failed");
            assertLockedFor(helper, b, frame);
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(b.containerMenu == b.inventoryMenu,
                            "B's station menu is still open after the owner locked the frame");
                    h.assertTrue(messages(b, StationMenu.FRAME_GONE) == 1,
                            "frame gone messages to B: " + Compat.gameTests().messagesTo(b));
                    h.assertTrue(a.containerMenu == menuOfA, "the owner's station menu closed");
                    h.assertTrue(modMessages(a).isEmpty(), "messages of the mod to the owner: " + modMessages(a));
                    h.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(), s -> s.setUnlocked(true)),
                            "unlocking the frame again failed");
                    h.assertTrue(!menuOfB.stillValid(b) && messages(b, StationMenu.FRAME_GONE) == 1,
                            "B's closed station menu came back with the frame unlocked again: "
                                    + Compat.gameTests().messagesTo(b));
                } finally {
                    cleanUp(h, side, frame, a, b);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a, b);
            }
        }
    }

    /**
     * Le altre ragioni chiudono il menu senza messaggi (chi si allontana non ha bisogno di spiegazioni). Con il telaio
     * di A sopra la stazione, nello stesso tick, {@code stillValid} e' falso con A nel Nether, da spettatore, con la
     * salute a 0 e sedici blocchi piu' in alto (fuori dalla struttura: il gioco base tiene valido il menu fino a 8
     * blocchi dal centro del blocco su 1.20.1, {@code distanceToSqr <= 64}, e fino a
     * {@code canInteractWithBlock(pos, 4.0)} dagli occhi su 1.21.1, cioe' 8,5 dal blocco in sopravvivenza, quindi un
     * punto dentro la struttura non basta), e torna vero ogni volta che la ragione se ne va; il menu del client,
     * costruito da un buffer scritto con {@link StationOpening#write}, vale anche con A lontano. Tolta la principale
     * (la laterale la segue), due tick dopo il menu e' chiuso. In tutto il test nessun messaggio del telaio.
     */
    private static void menuClosesWithoutAMessageForOtherReasons(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerLevel level = helper.getLevel();
        ExoframeEntity frame = frameOnStation(helper, ownedBy(a), MAIN);
        boolean delayed = false;
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            helper.assertTrue(menu.stillValid(a), "the station menu is not valid after opening");
            ServerLevel nether = level.getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null && nether != level, "premise: the test server has no separate Nether");
            a.setServerLevel(nether);
            try {
                helper.assertTrue(!menu.stillValid(a), "the station menu is still valid in the Nether");
            } finally {
                a.setServerLevel(level);
            }
            a.setGameMode(GameType.SPECTATOR);
            try {
                helper.assertTrue(!menu.stillValid(a), "the station menu is still valid for a spectator");
            } finally {
                a.setGameMode(GameType.SURVIVAL);
            }
            a.setHealth(0f);
            try {
                helper.assertTrue(!menu.stillValid(a), "the station menu is still valid for a dead player");
            } finally {
                a.setHealth(a.getMaxHealth());
            }
            Vec3 stand = a.position();
            Vec3 above = helper.absoluteVec(new Vec3(3.5, 17.0, 3.5));
            a.setPos(above.x, above.y, above.z);
            try {
                helper.assertTrue(!menu.stillValid(a), "the station menu is still valid sixteen blocks above");
                assertClientMenuStaysValid(helper, a, frame);
            } finally {
                a.setPos(stand.x, stand.y, stand.z);
            }
            helper.assertTrue(menu.stillValid(a), "the station menu is not valid again with every reason undone");

            helper.setBlock(MAIN, Blocks.AIR);
            helper.assertTrue(helper.getBlockState(side).isAir(), "premise: the side half did not follow the main");
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(a.containerMenu == a.inventoryMenu,
                            "the station menu is still open after the station was removed");
                    h.assertTrue(messages(a, StationMenu.FRAME_GONE) == 0,
                            "frame gone messages for other reasons: " + Compat.gameTests().messagesTo(a));
                } finally {
                    cleanUp(h, side, frame, a);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a);
            }
        }
    }

    /**
     * L'apertura porta la vista del telaio (design §5.5). Sopra la principale un telaio di A, bloccato, con l'elmo
     * consumato (60 su 180) e dipinto, il torso nuovo e una cella da 3 000 RE: la vista dell'apertura e' quella del
     * record con i diritti di A, il proprietario, che ne puo' cambiare il blocco, la regola {@code ownerOnlyFrames}
     * accesa (la config predefinita), la scelta 0 (l'elmo, il primo pezzo montato), nessuna armatura custodita e
     * {@code free} uguale alla capacita' {@code instabuild} di A. Scritta in un buffer, la legge il menu del client,
     * come la fabbrica del tipo; il buffer si rilascia subito dopo il costruttore, e solo dopo si confrontano
     * posizione, {@code frameId} e vista: nulla si legge tardi dal buffer, che su Fabric 1.21.1 si rilascia appena il
     * costruttore torna. Il menu di {@code openStation} ha la stessa vista. Con un telaio con il solo torso la scelta
     * dell'apertura e' 1, con un telaio nudo 0.
     */
    private static void openingCarriesTheFrameView(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        MinecraftServer server = helper.getLevel().getServer();
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        FrameState state = ownedBy(a);
        state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, 180, 60));
        state.setAppearance(PieceSlot.HELMET, new Appearance(0x3A6EA5, Appearance.UNPAINTED, List.of()));
        state.mount(scrapPiece(PieceSlot.TORSO));
        state.insertCell(new EnergyCell(capacity, 3_000));
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        ExoframeEntity other = null;
        try {
            BlockPos main = helper.absolutePos(MAIN);
            PlacedFrame record = placedRecord(helper, frame);
            StationView expected = StationView.of(record.state(), true, true, 0, false, a.getAbilities().instabuild);
            helper.assertTrue(expected.pieces().get(PieceSlot.HELMET).equals(new StationView.PieceInfo(Tier.SCRAP, 60,
                    180, 0x3A6EA5, Appearance.UNPAINTED)) && durability(expected, PieceSlot.TORSO) == 180
                    && expected.cell().equals(Optional.of(new StationView.CellInfo(3_000, capacity)))
                    && !expected.unlocked(), "premise: the record does not hold the test's frame: " + expected);
            StationOpening opening = StationMenu.opening(a, main, record);
            helper.assertTrue(opening.view().equals(expected),
                    "the opening carries " + opening.view() + ", expected " + expected);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            StationMenu client;
            try {
                opening.write(buf);
                client = new StationMenu(1, a.getInventory(), buf);
            } finally {
                buf.release();
            }
            helper.assertTrue(buf.refCnt() == 0, "premise: the extra data buffer was not released");
            helper.assertTrue(client.stationPos().equals(main) && client.frameId().equals(frame.frameId())
                    && client.view().equals(expected), "the client menu read " + client.stationPos() + ", "
                    + client.frameId() + " and " + client.view());

            StationMenu menu = openStation(helper, a, MAIN);
            helper.assertTrue(menu.view().equals(expected), "the opened menu has the view " + menu.view());
            a.closeContainer();

            removeFrame(server, frame);
            FrameState torsoOnly = ownedBy(a);
            torsoOnly.mount(scrapPiece(PieceSlot.TORSO));
            other = frameOnStation(helper, torsoOnly, MAIN);
            int selection = StationMenu.opening(a, main, placedRecord(helper, other)).view().selection();
            helper.assertTrue(selection == 1, "the opening on a frame with only the torso selects " + selection);
            removeFrame(server, other);
            other = frameOnStation(helper, ownedBy(a), MAIN);
            selection = StationMenu.opening(a, main, placedRecord(helper, other)).view().selection();
            helper.assertTrue(selection == 0, "the opening on a bare frame selects " + selection);
        } finally {
            if (other != null) {
                removeFrame(server, other);
            }
            cleanUp(helper, side, frame, a);
        }
        helper.succeed();
    }

    /**
     * La vista segue il record entro un tick (design §5.5): sul telaio di A con il torso nuovo, aperto, il torso perde
     * 50 punti nel registro, come per un danno, un comando o un'altra stazione, senza che nessuno lo annunci. Due tick
     * dopo il menu, ancora aperto, ha mandato una vista, con il torso a 130; altri due tick dopo, senza cambi, nessuna
     * altra. Il menu dei test non ha sincronizzatore: {@code viewsSent} conta le decisioni di invio del menu, anche
     * dove {@code Net} salta il giocatore finto (NeoForge 1.21.1, senza canali negoziati).
     */
    private static void viewFollowsTheRecordWithinATick(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState state = ownedBy(a);
        state.mount(scrapPiece(PieceSlot.TORSO));
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        boolean delayed = false;
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            helper.assertTrue(menu.viewsSent() == 0 && menu.lastSentView().equals(menu.view()),
                    "after opening: " + menu.viewsSent() + " views sent, last " + menu.lastSentView());
            helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(),
                    s -> s.pieces().get(PieceSlot.TORSO).damage(50)), "damaging the torso in the registry failed");
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                boolean passed = false;
                try {
                    h.assertTrue(a.containerMenu == menu, "the station menu closed");
                    h.assertTrue(menu.viewsSent() == 1 && durability(menu.lastSentView(), PieceSlot.TORSO) == 130,
                            "two ticks after the change: " + menu.viewsSent() + " views sent, torso at "
                                    + durability(menu.lastSentView(), PieceSlot.TORSO) + "; expected 1 and 130");
                    passed = true;
                } finally {
                    if (!passed) {
                        cleanUp(h, side, frame, a);
                    }
                }
            }));
            helper.runAfterDelay(4, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(a.containerMenu == menu, "the station menu closed");
                    h.assertTrue(menu.viewsSent() == 1 && durability(menu.lastSentView(), PieceSlot.TORSO) == 130,
                            "two more ticks without changes: " + menu.viewsSent() + " views sent, torso at "
                                    + durability(menu.lastSentView(), PieceSlot.TORSO) + "; expected 1 and 130");
                } finally {
                    cleanUp(h, side, frame, a);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a);
            }
        }
    }

    /**
     * Al piu' una vista per tick per menu (design §5.13): sul telaio di A con l'elmo nuovo, aperto, nello stesso tick
     * l'elmo perde 10 punti e {@code broadcastChanges} manda la vista (una, con l'elmo a 170); l'elmo ne perde altri 10
     * e un secondo {@code broadcastChanges}, nello stesso tick, non manda nulla (ancora una, a 170). Il cambio parte al
     * tick dopo: due tick dopo le viste mandate sono due, con l'elmo a 160.
     */
    private static void viewIsSentAtMostOncePerTick(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState state = ownedBy(a);
        state.mount(scrapPiece(PieceSlot.HELMET));
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        boolean delayed = false;
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(),
                    s -> s.pieces().get(PieceSlot.HELMET).damage(10)), "damaging the helmet in the registry failed");
            menu.broadcastChanges();
            helper.assertTrue(menu.viewsSent() == 1 && durability(menu.lastSentView(), PieceSlot.HELMET) == 170,
                    "after the first change: " + menu.viewsSent() + " views sent, helmet at "
                            + durability(menu.lastSentView(), PieceSlot.HELMET) + "; expected 1 and 170");
            helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(),
                    s -> s.pieces().get(PieceSlot.HELMET).damage(10)), "damaging the helmet again failed");
            menu.broadcastChanges();
            helper.assertTrue(menu.viewsSent() == 1 && durability(menu.lastSentView(), PieceSlot.HELMET) == 170,
                    "after a second change in the same tick: " + menu.viewsSent() + " views sent, helmet at "
                            + durability(menu.lastSentView(), PieceSlot.HELMET) + "; expected 1 and 170");
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                try {
                    h.assertTrue(a.containerMenu == menu, "the station menu closed");
                    h.assertTrue(menu.viewsSent() == 2 && durability(menu.lastSentView(), PieceSlot.HELMET) == 160,
                            "two ticks later: " + menu.viewsSent() + " views sent, helmet at "
                                    + durability(menu.lastSentView(), PieceSlot.HELMET) + "; expected 2 and 160");
                } finally {
                    cleanUp(h, side, frame, a);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a);
            }
        }
    }

    /**
     * La vista porta i diritti di chi guarda (design §5.5). Sul telaio di A, sbloccato: la vista di A dice che ne puo'
     * cambiare il blocco (il proprietario), che la regola {@code ownerOnlyFrames} e' accesa (la config predefinita) e
     * che A, in sopravvivenza, paga; B, che apre il suo menu sullo stesso telaio, non ne puo' cambiare il blocco. A
     * passa in creativa: due tick dopo la sua vista mandata dice che non paga, e B non ha avuto viste nuove. Su un
     * telaio di A con un paio di stivali stivati, sopra la principale, la vista dice che il telaio custodisce
     * un'armatura. Il caso dell'operatore non c'e': farlo operatore scriverebbe {@code ops.json} del server dei test;
     * la regola e' quella del comando.
     */
    private static void viewCarriesThePlayersRights(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ServerPlayer b = Compat.gameTests().mockServerPlayer(helper);
        b.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, b, OTHER_STAND);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState state = ownedBy(a);
        state.setUnlocked(true);
        ExoframeEntity frame = frameOnStation(helper, state, MAIN);
        boolean delayed = false;
        try {
            helper.assertTrue(RedstoneExoframes.config().server.ownerOnlyFrames,
                    "premise: ownerOnlyFrames is not the default");
            StationMenu menuOfA = openStation(helper, a, MAIN);
            StationView ofA = menuOfA.view();
            helper.assertTrue(ofA.canToggleLock() && ofA.ownerOnly() && !ofA.free(), "A's view: " + ofA);
            StationMenu menuOfB = openStation(helper, b, MAIN);
            StationView ofB = menuOfB.view();
            helper.assertTrue(!ofB.canToggleLock() && ofB.ownerOnly() && !ofB.free(), "B's view: " + ofB);
            a.setGameMode(GameType.CREATIVE);
            helper.runAfterDelay(2, () -> runInSequence(helper, h -> {
                ExoframeEntity stowing = null;
                try {
                    h.assertTrue(a.containerMenu == menuOfA && menuOfA.lastSentView().free(),
                            "A's last sent view in creative: " + menuOfA.lastSentView());
                    h.assertTrue(b.containerMenu == menuOfB && menuOfB.viewsSent() == 0,
                            "B got " + menuOfB.viewsSent() + " views after A changed game mode");
                    a.closeContainer();
                    b.closeContainer();
                    removeFrame(server, frame);
                    stowing = frameWithStowedBoots(h, a, ownedBy(a), new Vec3(3.5, 2.0, 3.5));
                    StationMenu menu = openStation(h, a, MAIN);
                    h.assertTrue(menu.view().hasStowed(), "the view of a frame with stowed boots: " + menu.view());
                } finally {
                    if (stowing != null) {
                        Compat.frameState().takeStowedOfPlaced(server, stowing.frameId());
                        removeFrame(server, stowing);
                    }
                    cleanUp(h, side, frame, a, b);
                }
                h.succeed();
            }));
            delayed = true;
        } finally {
            if (!delayed) {
                cleanUp(helper, side, frame, a, b);
            }
        }
    }

    /**
     * Il gestore del pacchetto {@code station_view} ({@link StationNet#apply}, codice comune) mette la vista solo nel
     * menu della stazione aperto con quel numero (design §5.5): sul menu di A, aperto, un pacchetto con un altro numero
     * non cambia la vista, uno con il numero giusto la cambia; chiuso il menu, un pacchetto con il numero vecchio, come
     * uno arrivato dopo la chiusura, non lancia e non cambia nulla; senza giocatore (sul client prima che ci sia) non
     * lancia.
     */
    private static void viewForAnotherMenuIsIgnored(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ExoframeEntity frame = frameOnStation(helper, ownedBy(a), MAIN);
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            StationView opened = menu.view();
            StationView another = new StationView(Map.of(), Optional.empty(), true, true, false, false,
                    StationView.SELECT_ALL, true, true);
            helper.assertTrue(!another.equals(opened), "premise: the other view is the opened one: " + opened);
            StationNet.apply(a, new StationViewPacket(menu.containerId + 1, another));
            helper.assertTrue(menu.view().equals(opened),
                    "a view for another menu id changed the view to " + menu.view());
            StationNet.apply(a, new StationViewPacket(menu.containerId, another));
            helper.assertTrue(menu.view().equals(another), "the view for the open menu was not set: " + menu.view());

            a.closeContainer();
            helper.assertTrue(a.containerMenu == a.inventoryMenu, "the station menu is still open");
            // un'eccezione qui fa fallire il test: il gestore non deve lanciare
            StationNet.apply(a, new StationViewPacket(menu.containerId, opened));
            helper.assertTrue(menu.view().equals(another),
                    "a view after closing changed the closed menu to " + menu.view());
            StationNet.apply(null, new StationViewPacket(menu.containerId, opened));
        } finally {
            cleanUp(helper, side, frame, a);
        }
        helper.succeed();
    }

    /**
     * Lo spostamento rapido (il clic con le maiuscole) nel menu della stazione, che ha gli slot dell'inventario del
     * giocatore (design §5.3): dalle tre file dell'inventario (slot 0-26 del menu) alla barra (27-35) e dalla barra
     * all'inventario, nel primo posto libero; con la barra piena non sposta nulla, non perde nulla e non lascia nulla a
     * terra ne' sul cursore. Nulla esce dal menu (design §5.13: nessuno spostamento rapido monta pezzi). Sopra la
     * stazione un telaio del giocatore: senza telaio il menu non resterebbe valido. Alla fine l'inventario si svuota.
     */
    private static void quickMoveSwapsInventoryAndHotbar(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer a = Compat.gameTests().mockServerPlayer(helper);
        a.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, a, STAND);
        ExoframeEntity frame = frameOnStation(helper, ownedBy(a), MAIN);
        Inventory inventory = a.getInventory();
        try {
            StationMenu menu = openStation(helper, a, MAIN);
            inventory.setItem(9, new ItemStack(Items.IRON_INGOT, 3));

            menu.clicked(0, 0, ClickType.QUICK_MOVE, a);
            helper.assertTrue(inventory.getItem(9).isEmpty() && holds(inventory.getItem(0), Items.IRON_INGOT, 3),
                    "after the quick move from the inventory: slot 9 holds " + inventory.getItem(9)
                            + ", hotbar slot 0 holds " + inventory.getItem(0));
            menu.clicked(27, 0, ClickType.QUICK_MOVE, a);
            helper.assertTrue(inventory.getItem(0).isEmpty() && holds(inventory.getItem(9), Items.IRON_INGOT, 3),
                    "after the quick move from the hotbar: hotbar slot 0 holds " + inventory.getItem(0)
                            + ", slot 9 holds " + inventory.getItem(9));

            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                inventory.setItem(i, new ItemStack(Items.DIRT, 64));
            }
            // prima direttamente: il gioco base ripete la chiamata finche' riceve uno stack non vuoto, e uno stack non
            // vuoto senza spostamento lo farebbe girare per sempre
            ItemStack moved = menu.quickMoveStack(a, 0);
            helper.assertTrue(moved.isEmpty(), "a quick move with a full hotbar returned " + moved);
            menu.clicked(0, 0, ClickType.QUICK_MOVE, a);
            helper.assertTrue(holds(inventory.getItem(9), Items.IRON_INGOT, 3),
                    "with a full hotbar slot 9 holds " + inventory.getItem(9));
            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                helper.assertTrue(holds(inventory.getItem(i), Items.DIRT, 64),
                        "with a full hotbar, hotbar slot " + i + " holds " + inventory.getItem(i));
            }
            helper.assertTrue(countInMainInventory(a, Items.IRON_INGOT) == 3 && menu.getCarried().isEmpty()
                    && itemsIn(helper).isEmpty(), countInMainInventory(a, Items.IRON_INGOT)
                    + " iron ingots in the inventory, " + menu.getCarried() + " on the cursor, "
                    + itemsIn(helper).size() + " items on the ground: expected the 3 ingots in the inventory only");
        } finally {
            cleanUp(helper, side, frame, a);
            inventory.clearContent();
        }
        helper.succeed();
    }

    /**
     * Piazza un telaio nudo del giocatore con i piedi in {@code feet} (relativo alla struttura), cerca alla stazione
     * dei test e toglie il telaio: la ricerca deve trovare proprio quel telaio se {@code inReach}, nessun telaio a
     * portata altrimenti.
     */
    private static void assertFindsOneFrameAt(GameTestHelper helper, ServerPlayer player, Vec3 feet,
            boolean inReach) {
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), feet, 0f);
        try {
            assertFind(helper, player, inReach ? found(frame) : NONE_IN_REACH, "a frame at " + feet);
        } finally {
            removeFrame(helper.getLevel().getServer(), frame);
        }
    }

    /**
     * Una meta' laterale rimasta sola in {@link #LONE_SIDE}, rivolta a nord, posata come la posa {@code /setblock}
     * (nessuno ne aggiorna la forma, quindi resta), con un telaio del giocatore sopra: il telaio sarebbe a portata
     * dell'impronta della stazione con la principale accanto, in (6, 1, 1), che pero' e' aria. Il clic vero del
     * giocatore sulla laterale non lancia, non apre nulla e non dice nulla; il server risponde {@code CONSUME}. Poi
     * toglie telaio e laterale.
     */
    private static void assertLoneSideHalfOpensNothing(GameTestHelper helper, ServerPlayer player) {
        BlockState lone = ModBlocks.STATION.get().defaultBlockState().setValue(StationBlock.FACING, Direction.NORTH)
                .setValue(StationBlock.PART, StationPart.SIDE);
        helper.setBlock(LONE_SIDE, lone);
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), new Vec3(5.5, 2.0, 1.5), 0f);
        try {
            helper.assertTrue(helper.getBlockState(LONE_SIDE).equals(lone)
                    && helper.getBlockState(LONE_SIDE.east()).isAir(), "premise: the side half is not alone");
            int before = modMessages(player).size();
            InteractionResult result = null;
            RuntimeException thrown = null;
            try {
                result = clickBlock(helper, player, LONE_SIDE, InteractionHand.MAIN_HAND);
            } catch (RuntimeException e) {
                thrown = e;
            }
            helper.assertTrue(thrown == null, "the click on a lone side half threw " + thrown);
            helper.assertTrue(player.containerMenu == player.inventoryMenu,
                    "the click on a lone side half opened a menu");
            helper.assertTrue(modMessages(player).size() == before,
                    "the click on a lone side half said something: " + modMessages(player));
            helper.assertTrue(result == InteractionResult.CONSUME,
                    "the server answered " + result + " on a lone side half");
        } finally {
            removeFrame(helper.getLevel().getServer(), frame);
            helper.setBlock(LONE_SIDE, Blocks.AIR);
        }
    }

    /**
     * Un record piazzato nel Nether con i piedi proprio sopra la principale non e' a portata della stazione dei test,
     * e lo sarebbe nella dimensione del suo record; la ricerca non lo vede.
     */
    private static void assertOtherDimensionIsOutOfReach(GameTestHelper helper, ServerPlayer player) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether != level, "premise: the test server has no separate Nether");
        UUID netherId = Compat.frameState().place(nether, ownedBy(player), helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)),
                0f);
        helper.assertTrue(netherId != null, "place returned no frame id");
        try {
            PlacedFrame record = Compat.frameState().placed(server, netherId).orElse(null);
            helper.assertTrue(record != null, "the Nether frame is not placed");
            BlockPos main = helper.absolutePos(MAIN);
            StationReach.Footprint footprint = StationFinder.footprint(main, level.getBlockState(main));
            helper.assertTrue(!StationFinder.inReach(record, level.dimension(), footprint)
                    && StationFinder.inReach(record, nether.dimension(), footprint),
                    "the dimension does not decide the reach of a frame above the station in the Nether");
            assertFind(helper, player, NONE_IN_REACH, "only a frame in the Nether");
        } finally {
            Compat.frameState().removePlaced(server, netherId);
        }
    }

    /**
     * Il menu del client, costruito come lo costruisce la fabbrica del tipo da un buffer scritto con
     * {@link StationOpening#write}, rilegge la stazione e il telaio e vale sempre: il gioco chiama {@code stillValid}
     * solo sul server.
     */
    private static void assertClientMenuStaysValid(GameTestHelper helper, ServerPlayer player, ExoframeEntity frame) {
        BlockPos main = helper.absolutePos(MAIN);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            StationMenu.opening(player, main, placedRecord(helper, frame)).write(buf);
            StationMenu client = new StationMenu(STATION_MENU_ID + 1, player.getInventory(), buf);
            helper.assertTrue(client.stationPos().equals(main) && client.frameId().equals(frame.frameId()),
                    "the client menu read " + client.stationPos() + " and " + client.frameId());
            helper.assertTrue(client.stillValid(player), "the client menu is not valid with the player far away");
        } finally {
            buf.release();
        }
    }

    /** Premessa dei casi con un telaio bloccato: la regola predefinita e un giocatore che non puo' usarlo. */
    private static void assertLockedFor(GameTestHelper helper, ServerPlayer player, ExoframeEntity frame) {
        helper.assertTrue(RedstoneExoframes.config().server.ownerOnlyFrames,
                "premise: ownerOnlyFrames is not the default");
        helper.assertTrue(!FramePermissions.canUse(player, recordState(helper, frame)),
                "premise: the player can use the locked frame");
    }

    /** La ricerca del telaio alla stazione dei test, per il giocatore, deve dare {@code expected}. */
    private static void assertFind(GameTestHelper helper, ServerPlayer player, StationFinder.Result expected,
            String when) {
        BlockPos main = helper.absolutePos(MAIN);
        StationFinder.Result result = StationFinder.find(player, helper.getLevel(), main,
                helper.getLevel().getBlockState(main));
        helper.assertTrue(result.equals(expected), when + ": the station found " + result + ", expected " + expected);
    }

    /** La ricerca che trova il telaio. */
    private static StationFinder.Result found(ExoframeEntity frame) {
        return new StationFinder.Result(StationFinder.Outcome.FOUND, Optional.of(frame.frameId()));
    }

    /** Il record piazzato del telaio, che deve esserci. */
    private static PlacedFrame placedRecord(GameTestHelper helper, ExoframeEntity frame) {
        PlacedFrame record = Compat.frameState().placed(helper.getLevel().getServer(), frame.frameId()).orElse(null);
        helper.assertTrue(record != null, "frame " + frame.frameId() + " is not placed");
        return record;
    }

    /** Vero se lo stack e' dell'oggetto dato, con quel conto. */
    private static boolean holds(ItemStack stack, Item item, int count) {
        return stack.is(item) && stack.getCount() == count;
    }

    /** La durata del pezzo dello slot nella vista; -1 se lo slot e' vuoto. */
    private static int durability(StationView view, PieceSlot slot) {
        StationView.PieceInfo piece = view.pieces().get(slot);
        return piece == null ? -1 : piece.durability();
    }

    /**
     * I messaggi della mod (le chiavi {@code message.redstone_exoframes.}) arrivati al giocatore finto, in ordine. Gli
     * altri messaggi di sistema non contano: ogni giocatore finto creato dopo di lui, anche da un altro test, gli fa
     * arrivare la riga del suo ingresso nel server.
     */
    private static List<TranslatableContents> modMessages(ServerPlayer player) {
        String prefix = "message." + RedstoneExoframes.MOD_ID + ".";
        List<TranslatableContents> found = new ArrayList<>();
        for (Component message : Compat.gameTests().messagesTo(player)) {
            if (message.getContents() instanceof TranslatableContents contents
                    && contents.getKey().startsWith(prefix)) {
                found.add(contents);
            }
        }
        return found;
    }

    /**
     * Chiude i menu ancora aperti dei giocatori, toglie il telaio (il record, se e' ancora piazzato, e l'entita') e le
     * due meta' della stazione.
     */
    private static void cleanUp(GameTestHelper helper, BlockPos side, ExoframeEntity frame,
            ServerPlayer... players) {
        removeFrame(helper.getLevel().getServer(), frame);
        closeStation(helper, side, players);
    }

    /**
     * Chiude i menu dei giocatori che sono ancora aperti e toglie le due meta' della stazione: prima la principale,
     * cosi' la laterale rimasta sola diventa aria senza bottino; poi la laterale {@code side}, se c'e' ancora.
     */
    private static void closeStation(GameTestHelper helper, BlockPos side, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
        helper.setBlock(MAIN, Blocks.AIR);
        helper.setBlock(side, Blocks.AIR);
    }
}
