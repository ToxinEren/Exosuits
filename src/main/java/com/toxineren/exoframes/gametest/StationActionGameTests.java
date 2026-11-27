package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.command.ExoframeCommand;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.FrameFitting;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FramePlacement;
import com.toxineren.exoframes.frame.FrameSight;
import com.toxineren.exoframes.frame.FrameSounds;
import com.toxineren.exoframes.frame.FrameSounds.Sound;
import com.toxineren.exoframes.station.StationActions;
import com.toxineren.exoframes.station.StationActions.Outcome;
import com.toxineren.exoframes.station.StationMenu;
import com.toxineren.exoframes.station.StationTexts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dei pulsanti della stazione di manutenzione (design M4 §5.6, §5.7, §5.11 e i casi di §5.18): la scelta,
 * libera e portata al client dalla vista; montare pezzi e cella dal cursore e smontarli nell'inventario, anche pieno, e
 * in creativa; i rifiuti, senza cambi e senza suoni; una scrittura per tick per menu; il record e il permesso riletti a
 * ogni azione; il registro che rifiuta la scrittura; un pezzo di un livello senza oggetto; il lucchetto, del
 * proprietario e di un operatore, che chiude la schermata di chi non puo' piu' usare il telaio; il ramo
 * {@code /exoframe debug reinforced} sul telaio guardato e su quello indossato. Montare e smontare passano da
 * {@link FrameFitting}, la regola delle mani; il lucchetto dalla regola di {@code /exoframe lock|unlock}
 * ({@code FramePermissions.canToggleLock}).
 * <p>
 * Come in {@link StationMenuGameTests}: la stazione, posata da {@code placeStation} con il fronte a nord, ha la meta'
 * principale in {@link #MAIN}; il giocatore A, in sopravvivenza, sta due blocchi a sud ({@link #STAND}) rivolto a nord,
 * il giocatore B o l'operatore O ({@code GameTests.operatorPlayer}), dove serve, accanto ({@link #OTHER_STAND}), anche
 * loro in sopravvivenza; il telaio sta sopra la principale ({@code frameOnStation}) e il menu si apre con
 * {@code openStation}, senza la rete. "Premere n" e' il pacchetto dei pulsanti del gioco base ({@code pressButton}, con
 * i suoi controlli e il {@code broadcastChanges} finale); {@link StationActions#press} chiamato direttamente da'
 * l'esito. Due azioni che scrivono sullo stesso menu stanno in tick diversi: ogni menu ne fa una per tick. I suoni si
 * registrano nel riquadro della struttura; ogni test toglie menu, cursori, telai, record, oggetti a terra e le due
 * meta' della stazione, anche se fallisce ({@link Bench}).
 */
public final class StationActionGameTests {
    /** Dove sta la meta' principale della stazione dei test, relativa alla struttura. */
    private static final BlockPos MAIN = new BlockPos(3, 1, 3);

    /** Dove stanno i piedi del giocatore A, relativo alla struttura: due blocchi a sud della stazione. */
    private static final Vec3 STAND = new Vec3(3.5, 1.0, 5.5);

    /** Dove stanno i piedi del giocatore B, relativo alla struttura: accanto ad A, a ovest. */
    private static final Vec3 OTHER_STAND = new Vec3(2.5, 1.0, 5.5);

    /** Il primario dell'elmo dipinto dei test. */
    private static final int BLUE = 0x3A6EA5;

    /** Gli slot dell'inventario principale del giocatore: la barra e le tre file. */
    private static final int MAIN_INVENTORY_SLOTS = 36;

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public StationActionGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "selectionsAreFreeAndTravelInTheView", StationActionGameTests::selectionsAreFreeAndTravelInTheView);
        add(tests, "emptiedSelectionStaysAndDoesNothing", StationActionGameTests::emptiedSelectionStaysAndDoesNothing);
        add(tests, "mountTakesOnePieceFromTheCursor", StationActionGameTests::mountTakesOnePieceFromTheCursor);
        add(tests, "mountRefusesTakenSlotsAndWrongItems", StationActionGameTests::mountRefusesTakenSlotsAndWrongItems);
        add(tests, "unmountGivesThePieceBack", StationActionGameTests::unmountGivesThePieceBack);
        add(tests, "unmountWithAFullInventoryDropsAtTheFeet",
                StationActionGameTests::unmountWithAFullInventoryDropsAtTheFeet);
        add(tests, "cellMountsSwapsAndUnmounts", StationActionGameTests::cellMountsSwapsAndUnmounts);
        add(tests, "oneWritePerTickPerMenu", StationActionGameTests::oneWritePerTickPerMenu);
        add(tests, "actionsRecheckTheRecordAndThePermission",
                StationActionGameTests::actionsRecheckTheRecordAndThePermission);
        add(tests, "refusedWriteTakesNothingAndSaysSo", StationActionGameTests::refusedWriteTakesNothingAndSaysSo);
        add(tests, "unmountWithoutAnItemKeepsThePiece", StationActionGameTests::unmountWithoutAnItemKeepsThePiece);
        add(tests, "ownerTogglesTheLockFromTheStation", StationActionGameTests::ownerTogglesTheLockFromTheStation);
        add(tests, "othersCannotToggleTheLock", StationActionGameTests::othersCannotToggleTheLock);
        add(tests, "operatorTogglesAnyLock", StationActionGameTests::operatorTogglesAnyLock);
        add(tests, "lockingFromTheStationClosesAnotherScreen",
                StationActionGameTests::lockingFromTheStationClosesAnotherScreen);
        add(tests, "debugReinforcedMarksWornAndPlacedFrames",
                StationActionGameTests::debugReinforcedMarksWornAndPlacedFrames);
        return tests;
    }

    /**
     * Le scelte sono libere e viaggiano nella vista (design §5.6): sul telaio di A con elmo e torso, nello stesso tick,
     * A preme 1, 6, 7 e 3 e il menu ha ogni volta quella scelta; poi preme 16 e l'elmo si smonta, perche' le scelte non
     * hanno usato il turno di scrittura del tick. Due tick dopo l'ultima vista mandata ha la scelta 3 e non ha l'elmo.
     */
    private static void selectionsAreFreeAndTravelInTheView(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(withPieces(bench.a, PieceSlot.HELMET, PieceSlot.TORSO));
            StationMenu menu = bench.open(bench.a);
            for (int selection : new int[] {1, 6, 7, 3}) {
                pressButton(bench.a, menu.containerId, selection);
                helper.assertTrue(menu.selection() == selection,
                        "after button " + selection + " the selection is " + menu.selection());
            }
            pressButton(bench.a, menu.containerId, 16);
            helper.assertTrue(!recordState(helper, frame).pieces().containsKey(PieceSlot.HELMET),
                    "button 16 after four selections in the same tick did not unmount the helmet");
            bench.last(2, h -> {
                StationView sent = menu.lastSentView();
                h.assertTrue(sent.selection() == 3 && !sent.pieces().containsKey(PieceSlot.HELMET),
                        "two ticks later the last view sent has the selection " + sent.selection() + " and the pieces "
                                + sent.pieces().keySet());
            });
        });
    }

    /**
     * Una scelta svuotata resta e non fa nulla (design §5.6 e §5.18): sul telaio di A, sbloccato, con elmo e torso, A
     * sceglie il torso; B, accanto, lo smonta con la regola delle mani; lo smontaggio del torso da parte di A non fa
     * nulla: A non ha torsi (B ne ha uno), si sente il solo suono di B e la scelta resta il torso. Due tick dopo la
     * vista ha la scelta 1 e nessun torso.
     */
    private static void emptiedSelectionStaysAndDoesNothing(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer b = bench.player(OTHER_STAND);
            FrameState state = withPieces(bench.a, PieceSlot.HELMET, PieceSlot.TORSO);
            state.setUnlocked(true);
            ExoframeEntity frame = bench.frame(state);
            StationMenu menu = bench.open(bench.a);
            pressButton(bench.a, menu.containerId, 1);
            helper.assertTrue(menu.selection() == 1, "after button 1 the selection is " + menu.selection());
            FrameFitting.Result byHand = FrameFitting.unmountPiece(b, record(helper, frame), PieceSlot.TORSO);
            helper.assertTrue(byHand == FrameFitting.Result.DONE, "B unmounting the torso by hand: " + byHand);
            assertPress(helper, menu, bench.a, 17, Outcome.REFUSED);
            helper.assertTrue(countInMainInventory(bench.a, ModItems.SCRAP_TORSO.get()) == 0
                    && countInMainInventory(b, ModItems.SCRAP_TORSO.get()) == 1, "torsos: A has "
                    + countInMainInventory(bench.a, ModItems.SCRAP_TORSO.get()) + ", B has "
                    + countInMainInventory(b, ModItems.SCRAP_TORSO.get()) + "; expected 0 and 1");
            bench.assertSounds(List.of(Sound.UNMOUNT), "after the unmount of an emptied selection");
            helper.assertTrue(menu.selection() == 1, "the emptied selection moved to " + menu.selection());
            bench.last(2, h -> {
                StationView sent = menu.lastSentView();
                h.assertTrue(sent.selection() == 1 && !sent.pieces().containsKey(PieceSlot.TORSO),
                        "two ticks later the last view sent has the selection " + sent.selection() + " and the pieces "
                                + sent.pieces().keySet());
            });
        });
    }

    /**
     * Montare prende un pezzo dal cursore (design §5.7): sul telaio nudo di A, con l'elmo Rottame a 100 su 180 dipinto
     * sul cursore, il pulsante 8 lo monta con la sua durata e il suo colore, il cursore si svuota, suona il montaggio e
     * il riassunto dell'entita' ha l'elmo. Al tick dopo, in creativa, il pulsante 9 monta il torso del cursore e il
     * cursore lo tiene ancora.
     */
    private static void mountTakesOnePieceFromTheCursor(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(ownedBy(bench.a));
            StationMenu menu = bench.open(bench.a);
            menu.setCarried(scrapPieceStack(PieceSlot.HELMET, 100, new Appearance(BLUE, Appearance.UNPAINTED,
                    List.of())));
            pressButton(bench.a, menu.containerId, 8);
            FrameState state = recordState(helper, frame);
            Piece helmet = state.pieces().get(PieceSlot.HELMET);
            helper.assertTrue(helmet != null && helmet.durability() == 100 && helmet.maxDurability() == 180
                    && state.appearance(PieceSlot.HELMET).primary() == BLUE, "the mounted helmet is " + helmet
                    + " painted " + state.appearance(PieceSlot.HELMET));
            helper.assertTrue(menu.getCarried().isEmpty(), "the cursor holds " + menu.getCarried());
            bench.assertSounds(List.of(Sound.MOUNT), "after mounting the helmet");
            helper.assertTrue(frame.summary().pieces().containsKey(PieceSlot.HELMET),
                    "the entity summary has no helmet: " + frame.summary());
            bench.last(1, h -> {
                bench.a.setGameMode(GameType.CREATIVE);
                menu.setCarried(new ItemStack(ModItems.SCRAP_TORSO.get()));
                pressButton(bench.a, menu.containerId, 9);
                h.assertTrue(recordState(h, frame).pieces().containsKey(PieceSlot.TORSO),
                        "button 9 in creative did not mount the torso");
                h.assertTrue(menu.getCarried().is(ModItems.SCRAP_TORSO.get()) && menu.getCarried().getCount() == 1,
                        "in creative the cursor holds " + menu.getCarried() + " after mounting the torso");
            });
        });
    }

    /**
     * Montare rifiuta lo slot occupato e cio' che non e' del posto (design §5.6 e §5.7), un tentativo per tick sul
     * telaio di A con il solo elmo: un elmo sul posto dell'elmo (il messaggio dello slot occupato, il cursore uguale);
     * un braccio sul posto dell'elmo (nessun messaggio); tre lingotti di ferro, il cursore vuoto, una cella sul posto
     * del torso; un torso sul posto della cella. Ogni volta {@code REFUSED}; in tutto nessun suono, e il record ha
     * sempre il solo elmo e nessuna cella.
     */
    private static void mountRefusesTakenSlotsAndWrongItems(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(withPieces(bench.a, PieceSlot.HELMET));
            StationMenu menu = bench.open(bench.a);
            ItemStack helmet = new ItemStack(ModItems.SCRAP_HELMET.get());
            menu.setCarried(helmet.copy());
            assertPress(helper, menu, bench.a, 8, Outcome.REFUSED);
            helper.assertTrue(messages(bench.a, FrameInteractions.SLOT_TAKEN) == 1,
                    "messages for the taken slot: " + messages(bench.a, FrameInteractions.SLOT_TAKEN));
            helper.assertTrue(ItemStack.matches(helmet, menu.getCarried()), "the cursor holds " + menu.getCarried());
            assertOnlyTheHelmet(helper, frame, "a helmet on the helmet place");
            int modMessages = modMessages(bench.a);
            bench.then(1, h -> {
                menu.setCarried(new ItemStack(ModItems.SCRAP_ARM.get()));
                assertPress(h, menu, bench.a, 8, Outcome.REFUSED);
                h.assertTrue(modMessages(bench.a) == modMessages, "an arm on the helmet place said "
                        + (modMessages(bench.a) - modMessages) + " messages");
                assertOnlyTheHelmet(h, frame, "an arm on the helmet place");
            });
            bench.then(2, h -> {
                ItemStack iron = new ItemStack(Items.IRON_INGOT, 3);
                menu.setCarried(iron.copy());
                assertPress(h, menu, bench.a, 9, Outcome.REFUSED);
                h.assertTrue(ItemStack.matches(iron, menu.getCarried()), "the cursor holds " + menu.getCarried());
                assertOnlyTheHelmet(h, frame, "iron ingots on the torso place");
            });
            bench.then(3, h -> {
                menu.setCarried(ItemStack.EMPTY);
                assertPress(h, menu, bench.a, 9, Outcome.REFUSED);
                assertOnlyTheHelmet(h, frame, "an empty cursor on the torso place");
            });
            bench.then(4, h -> {
                menu.setCarried(cell(3_000));
                assertPress(h, menu, bench.a, 9, Outcome.REFUSED);
                assertOnlyTheHelmet(h, frame, "a cell on the torso place");
            });
            bench.last(5, h -> {
                menu.setCarried(new ItemStack(ModItems.SCRAP_TORSO.get()));
                assertPress(h, menu, bench.a, 14, Outcome.REFUSED);
                assertOnlyTheHelmet(h, frame, "a torso on the cell place");
                bench.assertSounds(List.of(), "after six refused mounts");
            });
        });
    }

    /**
     * Smontare da' il pezzo indietro (design §5.7): sul telaio di A con i sei pezzi (l'elmo a 60 su 180, dipinto) e una
     * cella da 3 000 RE, il pulsante 16 da' ad A l'elmo con la sua durata e il suo colore, il record e il riassunto
     * dell'entita' lo perdono, suona lo smontaggio; al tick dopo il 22 da' la cella con la sua carica. Poi l'entita' se
     * ne va e il record resta, come con il suo chunk scaricato (il riallineamento la ricrea solo dopo due controlli a
     * 100 tick l'uno dall'altro): al tick dopo lo smontaggio del torso riesce lo stesso, e l'entita' rifatta dal record
     * non ha ne' l'elmo ne' il torso.
     */
    private static void unmountGivesThePieceBack(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            FrameState state = withPieces(bench.a, PieceSlot.values());
            state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, 180, 60));
            state.setAppearance(PieceSlot.HELMET, new Appearance(BLUE, Appearance.UNPAINTED, List.of()));
            state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 3_000));
            ExoframeEntity frame = bench.frame(state);
            StationMenu menu = bench.open(bench.a);
            pressButton(bench.a, menu.containerId, 16);
            Optional<PieceData> data = Compat.itemData().piece(findInMainInventory(bench.a,
                    ModItems.SCRAP_HELMET.get()));
            helper.assertTrue(data.isPresent() && data.get().tier() == Tier.SCRAP && data.get().durability() == 60
                    && data.get().maxDurability() == 180 && data.get().appearance().primary() == BLUE,
                    "the helmet given back carries " + data);
            helper.assertTrue(!recordState(helper, frame).pieces().containsKey(PieceSlot.HELMET),
                    "the record still has the helmet");
            bench.assertSounds(List.of(Sound.UNMOUNT), "after unmounting the helmet");
            helper.assertTrue(!frame.summary().pieces().containsKey(PieceSlot.HELMET),
                    "the entity summary still has the helmet");
            bench.then(1, h -> {
                pressButton(bench.a, menu.containerId, 22);
                ItemStack cell = findInMainInventory(bench.a, ModItems.REDSTONE_CELL.get());
                h.assertTrue(!cell.isEmpty() && energy(cell) == 3_000, "the cell given back is " + cell);
                h.assertTrue(recordState(h, frame).cell() == null, "the record still has the cell");
                bench.assertSounds(List.of(Sound.UNMOUNT, Sound.UNMOUNT), "after unmounting the cell");
                frame.discard();
                h.assertTrue(Compat.frameState().placed(h.getLevel().getServer(), frame.frameId()).isPresent(),
                        "the record went away with the entity");
            });
            bench.last(2, h -> {
                assertPress(h, menu, bench.a, 17, Outcome.DONE);
                h.assertTrue(countInMainInventory(bench.a, ModItems.SCRAP_TORSO.get()) == 1,
                        "A has " + countInMainInventory(bench.a, ModItems.SCRAP_TORSO.get()) + " torsos");
                h.assertTrue(!recordState(h, frame).pieces().containsKey(PieceSlot.TORSO),
                        "the record still has the torso");
                bench.assertSounds(List.of(Sound.UNMOUNT, Sound.UNMOUNT, Sound.UNMOUNT),
                        "after unmounting the torso without the entity");
                ExoframeEntity view = bench.adopt(FramePlacement.spawnView(h.getLevel(), record(h, frame)));
                h.assertTrue(!view.summary().pieces().containsKey(PieceSlot.HELMET)
                        && !view.summary().pieces().containsKey(PieceSlot.TORSO),
                        "the entity made again from the record has the pieces " + view.summary().pieces().keySet());
            });
        });
    }

    /**
     * Con l'inventario pieno cio' che si smonta cade ai piedi (design §5.18): con i 36 posti dell'inventario di A pieni
     * di terra, il pulsante 16 lascia l'elmo fermo ai piedi di A, con il ritardo di raccolta, e la terra resta.
     */
    private static void unmountWithAFullInventoryDropsAtTheFeet(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            bench.frame(withPieces(bench.a, PieceSlot.HELMET));
            StationMenu menu = bench.open(bench.a);
            fillWithDirt(bench.a);
            pressButton(bench.a, menu.containerId, 16);
            helper.assertTrue(countInMainInventory(bench.a, ModItems.SCRAP_HELMET.get()) == 0,
                    "the helmet went into a full inventory");
            ItemEntity drop = assertDroppedAtFeet(helper, bench.a, ModItems.SCRAP_HELMET.get());
            helper.assertTrue(drop.hasPickUpDelay(), "the helmet at the feet can be picked up at once");
            helper.assertTrue(countInMainInventory(bench.a, Items.DIRT) == MAIN_INVENTORY_SLOTS * 64,
                    "the dirt changed: " + countInMainInventory(bench.a, Items.DIRT));
        });
    }

    /**
     * La cella si monta, si scambia e si smonta (design §5.7 e §5.18), un'azione per tick sul telaio di A senza cella:
     * la cella da 3 000 del cursore si monta e il cursore si svuota; quella da 8 000 prende il suo posto e la vecchia
     * va nell'inventario; con l'inventario pieno di terra quella da 500 prende il posto di quella da 8 000, che cade
     * ferma ai piedi; il pulsante 22 lascia ai piedi quella da 500, e il record resta senza cella; in creativa la cella
     * da 1 000 si monta e resta sul cursore.
     */
    private static void cellMountsSwapsAndUnmounts(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(ownedBy(bench.a));
            StationMenu menu = bench.open(bench.a);
            menu.setCarried(cell(3_000));
            pressButton(bench.a, menu.containerId, 14);
            assertCell(helper, frame, 3_000);
            helper.assertTrue(menu.getCarried().isEmpty(), "the cursor holds " + menu.getCarried());
            bench.assertSounds(List.of(Sound.MOUNT), "after mounting the cell");
            bench.then(1, h -> {
                menu.setCarried(cell(8_000));
                pressButton(bench.a, menu.containerId, 14);
                assertCell(h, frame, 8_000);
                ItemStack old = findInMainInventory(bench.a, ModItems.REDSTONE_CELL.get());
                h.assertTrue(!old.isEmpty() && energy(old) == 3_000, "the cell given back is " + old);
            });
            bench.then(2, h -> {
                fillWithDirt(bench.a);
                menu.setCarried(cell(500));
                pressButton(bench.a, menu.containerId, 14);
                assertCell(h, frame, 500);
                ItemEntity drop = assertDroppedAtFeet(h, bench.a, ModItems.REDSTONE_CELL.get());
                h.assertTrue(energy(drop.getItem()) == 8_000, "the cell at the feet is " + drop.getItem());
                drop.discard();
            });
            bench.then(3, h -> {
                pressButton(bench.a, menu.containerId, 22);
                h.assertTrue(recordState(h, frame).cell() == null, "the record still has a cell");
                ItemEntity drop = assertDroppedAtFeet(h, bench.a, ModItems.REDSTONE_CELL.get());
                h.assertTrue(energy(drop.getItem()) == 500, "the cell at the feet is " + drop.getItem());
                drop.discard();
                bench.assertSounds(List.of(Sound.MOUNT, Sound.MOUNT, Sound.MOUNT, Sound.UNMOUNT),
                        "after three mounts and an unmount");
            });
            bench.last(4, h -> {
                bench.a.setGameMode(GameType.CREATIVE);
                menu.setCarried(cell(1_000));
                pressButton(bench.a, menu.containerId, 14);
                assertCell(h, frame, 1_000);
                h.assertTrue(menu.getCarried().is(ModItems.REDSTONE_CELL.get()) && energy(menu.getCarried()) == 1_000,
                        "in creative the cursor holds " + menu.getCarried() + " after mounting the cell");
            });
        });
    }

    /**
     * Una scrittura per tick per menu (design §5.6 e §5.18): sul telaio di A, sbloccato, con elmo, torso e una cella,
     * aperto anche da B sulla stessa stazione, nello stesso tick lo smontaggio dell'elmo di A riesce, quello del torso
     * di A no ({@code THROTTLED}), la scelta di A resta libera, e lo smontaggio della cella di B, dal suo menu, riesce:
     * il torso resta montato, l'elmo va ad A e la cella a B. Al tick dopo lo smontaggio del torso di A riesce.
     */
    private static void oneWritePerTickPerMenu(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer b = bench.player(OTHER_STAND);
            FrameState state = withPieces(bench.a, PieceSlot.HELMET, PieceSlot.TORSO);
            state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 3_000));
            state.setUnlocked(true);
            ExoframeEntity frame = bench.frame(state);
            StationMenu menuOfA = bench.open(bench.a);
            StationMenu menuOfB = bench.open(b);
            assertPress(helper, menuOfA, bench.a, 16, Outcome.DONE);
            assertPress(helper, menuOfA, bench.a, 17, Outcome.THROTTLED);
            assertPress(helper, menuOfA, bench.a, 1, Outcome.SELECTED);
            assertPress(helper, menuOfB, b, 22, Outcome.DONE);
            FrameState now = recordState(helper, frame);
            helper.assertTrue(now.pieces().keySet().equals(EnumSet.of(PieceSlot.TORSO)) && now.cell() == null,
                    "after one tick of presses the record has " + now.pieces().keySet() + " and the cell "
                            + now.cell());
            helper.assertTrue(countInMainInventory(bench.a, ModItems.SCRAP_HELMET.get()) == 1
                    && countInMainInventory(b, ModItems.REDSTONE_CELL.get()) == 1, "A has "
                    + countInMainInventory(bench.a, ModItems.SCRAP_HELMET.get()) + " helmets, B "
                    + countInMainInventory(b, ModItems.REDSTONE_CELL.get()) + " cells; expected 1 and 1");
            bench.last(1, h -> {
                assertPress(h, menuOfA, bench.a, 17, Outcome.DONE);
                h.assertTrue(recordState(h, frame).pieces().isEmpty()
                        && countInMainInventory(bench.a, ModItems.SCRAP_TORSO.get()) == 1,
                        "the torso was not unmounted in the next tick");
            });
        });
    }

    /**
     * Ogni azione rilegge il record e il permesso (design §5.6 e §5.18): sul telaio di A, sbloccato, con l'elmo,
     * aperto anche da B, A lo blocca, e prima che il menu di B si chiuda lo smontaggio dell'elmo di B non fa nulla e
     * non da' nulla; al tick dopo il record se ne va, e lo smontaggio dell'elmo di A non fa nulla, non lancia e non da'
     * nulla.
     */
    private static void actionsRecheckTheRecordAndThePermission(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer b = bench.player(OTHER_STAND);
            FrameState state = withPieces(bench.a, PieceSlot.HELMET);
            state.setUnlocked(true);
            ExoframeEntity frame = bench.frame(state);
            StationMenu menuOfA = bench.open(bench.a);
            StationMenu menuOfB = bench.open(b);
            MinecraftServer server = helper.getLevel().getServer();
            helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(), s -> s.setUnlocked(false)),
                    "locking the frame failed");
            assertPress(helper, menuOfB, b, 16, Outcome.REFUSED);
            helper.assertTrue(recordState(helper, frame).pieces().containsKey(PieceSlot.HELMET),
                    "B unmounted the helmet of a locked frame");
            helper.assertTrue(b.getInventory().isEmpty(), "B got something from a locked frame");
            bench.last(1, h -> {
                h.assertTrue(Compat.frameState().removePlaced(server, frame.frameId()), "removing the record failed");
                assertPress(h, menuOfA, bench.a, 16, Outcome.REFUSED);
                h.assertTrue(bench.a.getInventory().isEmpty() && itemsIn(h).isEmpty(),
                        "an action on a removed record gave something");
            });
        });
    }

    /**
     * Il registro che rifiuta la scrittura non prende nulla e lo dice (design §5.6 e §5.18): sul telaio di A con
     * l'elmo, con la scrittura rifiutata, lo smontaggio dell'elmo da' {@code FAILED}, il messaggio della stazione che
     * non e' riuscita, nulla nell'inventario, e il record ha ancora l'elmo; al tick dopo il montaggio del torso del
     * cursore, con la scrittura rifiutata, da' {@code FAILED} e il torso resta sul cursore. In tutto nessun suono. La
     * scrittura rifiutata si apre e si chiude nello stesso corpo sincrono: i test girano insieme sullo stesso registro.
     */
    private static void refusedWriteTakesNothingAndSaysSo(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(withPieces(bench.a, PieceSlot.HELMET));
            StationMenu menu = bench.open(bench.a);
            MinecraftServer server = helper.getLevel().getServer();
            Outcome unmount;
            try (GameTests.Refusal refusal = Compat.gameTests().refusePlacedUpdates(server)) {
                unmount = StationActions.press(menu, bench.a, 16);
            }
            helper.assertTrue(unmount == Outcome.FAILED, "the unmount with the write refused gave " + unmount);
            helper.assertTrue(messages(bench.a, StationTexts.FAILED) == 1,
                    "messages of the failed change: " + messages(bench.a, StationTexts.FAILED));
            helper.assertTrue(bench.a.getInventory().isEmpty(), "the refused unmount gave something");
            helper.assertTrue(recordState(helper, frame).pieces().containsKey(PieceSlot.HELMET),
                    "the record lost the helmet");
            bench.last(1, h -> {
                menu.setCarried(new ItemStack(ModItems.SCRAP_TORSO.get()));
                Outcome mount;
                try (GameTests.Refusal refusal = Compat.gameTests().refusePlacedUpdates(server)) {
                    mount = StationActions.press(menu, bench.a, 9);
                }
                h.assertTrue(mount == Outcome.FAILED, "the mount with the write refused gave " + mount);
                h.assertTrue(menu.getCarried().is(ModItems.SCRAP_TORSO.get()) && menu.getCarried().getCount() == 1,
                        "the cursor holds " + menu.getCarried() + " after the refused mount");
                h.assertTrue(!recordState(h, frame).pieces().containsKey(PieceSlot.TORSO), "the record has the torso");
                bench.assertSounds(List.of(), "after two refused writes");
            });
        });
    }

    /**
     * Un pezzo di un livello senza oggetto non si smonta (design §5.7 e §5.18): sul telaio di A con un elmo Industriale
     * montato nel registro (in questa parte l'Industriale non ha oggetti), lo smontaggio da' {@code REFUSED}, l'elmo
     * resta, nulla si consegna e nulla suona; l'avviso che lo dice ha la sua riga in tools/log_allowlist.txt.
     */
    private static void unmountWithoutAnItemKeepsThePiece(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(ownedBy(bench.a));
            Piece industrial = new Piece(PieceSlot.HELMET, Tier.INDUSTRIAL, 240, 240);
            helper.assertTrue(Compat.frameState().updatePlaced(helper.getLevel().getServer(), frame.frameId(),
                    s -> s.mount(industrial)), "mounting the industrial helmet in the registry failed");
            StationMenu menu = bench.open(bench.a);
            assertPress(helper, menu, bench.a, 16, Outcome.REFUSED);
            Piece helmet = recordState(helper, frame).pieces().get(PieceSlot.HELMET);
            helper.assertTrue(helmet != null && helmet.tier() == Tier.INDUSTRIAL,
                    "the record has the helmet " + helmet);
            helper.assertTrue(bench.a.getInventory().isEmpty() && itemsIn(helper).isEmpty(),
                    "a piece without an item was handed out");
            bench.assertSounds(List.of(), "after an unmount without an item");
        });
    }

    /**
     * Il proprietario sblocca e blocca dalla stazione (design §5.11 e §5.17): sul telaio di A, bloccato, il pulsante 26
     * lo sblocca, A ha sulla barra delle azioni il messaggio di {@code /exoframe unlock} e suona lo sblocco; al tick
     * dopo lo stesso pulsante lo blocca, con il messaggio di {@code /exoframe lock} e il suono del blocco. Due tick
     * dopo l'ultima vista mandata ha il telaio bloccato.
     */
    private static void ownerTogglesTheLockFromTheStation(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(ownedBy(bench.a));
            StationMenu menu = bench.open(bench.a);
            assertPress(helper, menu, bench.a, 26, Outcome.DONE);
            helper.assertTrue(recordState(helper, frame).unlocked(), "button 26 did not unlock the frame");
            helper.assertTrue(messages(bench.a, ExoframeCommand.UNLOCK_DONE) == 1,
                    "unlock messages: " + messages(bench.a, ExoframeCommand.UNLOCK_DONE));
            bench.assertSounds(List.of(Sound.UNLOCK), "after unlocking");
            bench.then(1, h -> {
                assertPress(h, menu, bench.a, 26, Outcome.DONE);
                h.assertTrue(!recordState(h, frame).unlocked(), "button 26 did not lock the frame again");
                h.assertTrue(messages(bench.a, ExoframeCommand.LOCK_DONE) == 1,
                        "lock messages: " + messages(bench.a, ExoframeCommand.LOCK_DONE));
                bench.assertSounds(List.of(Sound.UNLOCK, Sound.LOCK), "after locking again");
            });
            bench.last(3, h -> {
                StationView sent = menu.lastSentView();
                h.assertTrue(!sent.unlocked(), "two ticks after locking the last view sent is " + sent);
            });
        });
    }

    /**
     * Chi non e' il proprietario non cambia il lucchetto (design §5.11): sul telaio di A, sbloccato, B in sopravvivenza
     * apre la stazione; la sua vista non ha {@code canToggleLock}, e il suo pulsante 26 da' {@code REFUSED}: il telaio
     * resta sbloccato, B non ha messaggi e nulla suona.
     */
    private static void othersCannotToggleTheLock(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer b = bench.player(OTHER_STAND);
            FrameState state = ownedBy(bench.a);
            state.setUnlocked(true);
            ExoframeEntity frame = bench.frame(state);
            StationMenu menuOfB = bench.open(b);
            helper.assertTrue(!menuOfB.view().canToggleLock(), "B's view: " + menuOfB.view());
            assertPress(helper, menuOfB, b, 26, Outcome.REFUSED);
            helper.assertTrue(recordState(helper, frame).unlocked(), "B locked A's frame");
            helper.assertTrue(messages(b, ExoframeCommand.UNLOCK_DONE) + messages(b, ExoframeCommand.LOCK_DONE)
                    + modMessages(b) == 0, "messages to B: " + Compat.gameTests().messagesTo(b));
            bench.assertSounds(List.of(), "after B's button 26");
        });
    }

    /**
     * Un operatore cambia il lucchetto di ogni telaio, ma solo se lo puo' usare (design §5.6 e §5.11). Su un telaio
     * senza proprietario A, che lo puo' usare, non ha {@code canToggleLock} e il suo pulsante 26 da' {@code REFUSED};
     * O, un operatore in sopravvivenza, ha {@code canToggleLock} e il suo 26 lo sblocca. Al tick dopo il telaio diventa
     * di A, sempre sbloccato, e il 26 di O lo blocca: un operatore ha bloccato il telaio di un altro. Al tick dopo il
     * 26 di O da' {@code REFUSED} e il telaio resta bloccato: in sopravvivenza O non puo' usare il telaio bloccato di
     * A, e {@code canUse} viene prima della regola del lucchetto. In tutto si sentono lo sblocco e il blocco.
     */
    private static void operatorTogglesAnyLock(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer o = bench.operator(OTHER_STAND);
            ExoframeEntity frame = bench.frame(new FrameState(RedstoneExoframes.config()));
            StationMenu menuOfA = bench.open(bench.a);
            StationMenu menuOfO = bench.open(o);
            helper.assertTrue(!menuOfA.view().canToggleLock(), "A's view: " + menuOfA.view());
            assertPress(helper, menuOfA, bench.a, 26, Outcome.REFUSED);
            helper.assertTrue(menuOfO.view().canToggleLock(), "O's view: " + menuOfO.view());
            assertPress(helper, menuOfO, o, 26, Outcome.DONE);
            helper.assertTrue(recordState(helper, frame).unlocked(), "O did not unlock the frame without an owner");
            bench.then(1, h -> {
                h.assertTrue(Compat.frameState().updatePlaced(h.getLevel().getServer(), frame.frameId(),
                        s -> s.setOwner(bench.a.getUUID())), "giving the frame to A failed");
                assertPress(h, menuOfO, o, 26, Outcome.DONE);
                FrameState state = recordState(h, frame);
                h.assertTrue(bench.a.getUUID().equals(state.owner()) && !state.unlocked(), "after O's button 26 on A's"
                        + " frame the owner is " + state.owner() + " and the frame is unlocked: " + state.unlocked());
            });
            bench.last(2, h -> {
                assertPress(h, menuOfO, o, 26, Outcome.REFUSED);
                h.assertTrue(!recordState(h, frame).unlocked(), "O in survival unlocked A's locked frame");
                bench.assertSounds(List.of(Sound.UNLOCK, Sound.LOCK), "after O's three buttons 26");
            });
        });
    }

    /**
     * Bloccare dalla stazione chiude la schermata di un altro (design §5.2 e §5.11): sul telaio di A, sbloccato, aprono
     * la stazione A e B, e A preme 26 con il pacchetto dei pulsanti del gioco base. Due tick dopo il menu di B e'
     * chiuso, con il messaggio del telaio non piu' disponibile, e quello di A, il proprietario, e' ancora aperto, senza
     * quel messaggio.
     */
    private static void lockingFromTheStationClosesAnotherScreen(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ServerPlayer b = bench.player(OTHER_STAND);
            FrameState state = ownedBy(bench.a);
            state.setUnlocked(true);
            ExoframeEntity frame = bench.frame(state);
            StationMenu menuOfA = bench.open(bench.a);
            bench.open(b);
            pressButton(bench.a, menuOfA.containerId, 26);
            helper.assertTrue(!recordState(helper, frame).unlocked(), "A's button 26 did not lock the frame");
            bench.last(2, h -> {
                h.assertTrue(b.containerMenu == b.inventoryMenu, "B's station menu is still open after A locked");
                h.assertTrue(messages(b, StationMenu.FRAME_GONE) == 1,
                        "frame gone messages to B: " + Compat.gameTests().messagesTo(b));
                h.assertTrue(bench.a.containerMenu == menuOfA, "A's station menu closed");
                h.assertTrue(messages(bench.a, StationMenu.FRAME_GONE) == 0,
                        "frame gone messages to A: " + Compat.gameTests().messagesTo(bench.a));
            });
        });
    }

    /**
     * {@code /exoframe debug reinforced} segna il telaio guardato e quello indossato (design §5.11). A, davanti alla
     * stazione, guarda il suo telaio e apre la stazione; "debug reinforced true" lo rende rinforzato, e due tick dopo
     * l'ultima vista mandata lo dice; "debug reinforced false" lo riporta com'era. Tolti menu, telaio e stazione, A
     * indossa un telaio: "debug reinforced true" rende rinforzato quello indossato. Uscito A dal telaio di prova, il
     * record indossato se ne va, e senza un telaio indossato o guardato il comando da' 0.
     */
    private static void debugReinforcedMarksWornAndPlacedFrames(GameTestHelper helper) {
        Bench bench = new Bench(helper);
        bench.run(() -> {
            ExoframeEntity frame = bench.frame(ownedBy(bench.a));
            helper.assertTrue(FrameSight.inSight(bench.a).map(ExoframeEntity::frameId)
                    .equals(Optional.of(frame.frameId())), "premise: A does not look at the frame on the station");
            StationMenu menu = bench.open(bench.a);
            helper.assertTrue(exoframeCommand(helper, bench.a, 2, "debug reinforced true") == 1,
                    "debug reinforced true failed on the frame in sight");
            helper.assertTrue(recordState(helper, frame).reinforced(), "the frame in sight is not reinforced");
            bench.last(2, h -> {
                h.assertTrue(menu.lastSentView().reinforced(),
                        "two ticks later the last view sent is " + menu.lastSentView());
                h.assertTrue(exoframeCommand(h, bench.a, 2, "debug reinforced false") == 1,
                        "debug reinforced false failed on the frame in sight");
                h.assertTrue(!recordState(h, frame).reinforced(), "the frame in sight is still reinforced");
                bench.removeStationAndFrames();
                try {
                    wearScrap(h, bench.a, STAND, 3_000);
                    h.assertTrue(exoframeCommand(h, bench.a, 2, "debug reinforced true") == 1,
                            "debug reinforced true failed on the worn frame");
                    h.assertTrue(Compat.frameState().get(bench.a).map(FrameState::reinforced).orElse(false),
                            "the worn frame is not reinforced");
                } finally {
                    leaveTestFrame(bench.a);
                }
                h.assertTrue(Compat.frameState().wornFrameId(bench.a).isEmpty(), "the worn record is still there");
                h.assertTrue(exoframeCommand(h, bench.a, 2, "debug reinforced true") == 0,
                        "debug reinforced without a worn frame or one in sight did not give 0");
            });
        });
    }

    /** {@link StationActions#press} del giocatore sul menu deve dare {@code expected}. */
    private static void assertPress(GameTestHelper helper, StationMenu menu, ServerPlayer player, int button,
            Outcome expected) {
        Outcome outcome = StationActions.press(menu, player, button);
        helper.assertTrue(outcome == expected, "button " + button + " of " + player.getGameProfile().getName()
                + " gave " + outcome + ", expected " + expected);
    }

    /** Il record ha il solo elmo e nessuna cella. */
    private static void assertOnlyTheHelmet(GameTestHelper helper, ExoframeEntity frame, String when) {
        FrameState state = recordState(helper, frame);
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.HELMET)) && state.cell() == null,
                "after " + when + " the record has " + state.pieces().keySet() + " and the cell " + state.cell());
    }

    /** Il record ha una cella con quella carica. */
    private static void assertCell(GameTestHelper helper, ExoframeEntity frame, int stored) {
        EnergyCell cell = recordState(helper, frame).cell();
        helper.assertTrue(cell != null && cell.stored() == stored, "the record has the cell " + cell + ", expected "
                + stored + " RE");
    }

    /** Uno stato del giocatore con i pezzi Rottame nuovi degli slot dati. */
    private static FrameState withPieces(ServerPlayer owner, PieceSlot... slots) {
        FrameState state = ownedBy(owner);
        for (PieceSlot slot : slots) {
            state.mount(scrapPiece(slot));
        }
        return state;
    }

    /** Il record piazzato del telaio, che deve esserci. */
    private static PlacedFrame record(GameTestHelper helper, ExoframeEntity frame) {
        PlacedFrame record = Compat.frameState().placed(helper.getLevel().getServer(), frame.frameId()).orElse(null);
        helper.assertTrue(record != null, "frame " + frame.frameId() + " is not placed");
        return record;
    }

    /** I 36 posti dell'inventario principale del giocatore pieni di terra da 64. */
    private static void fillWithDirt(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < MAIN_INVENTORY_SLOTS; i++) {
            inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        }
    }

    /**
     * Quanti messaggi della mod (le chiavi {@code message.redstone_exoframes.}) sono arrivati al giocatore finto: gli
     * altri messaggi di sistema non contano, perche' l'ingresso di ogni giocatore finto, anche di un altro test, ne
     * manda uno a tutti.
     */
    private static int modMessages(ServerPlayer player) {
        String prefix = "message." + RedstoneExoframes.MOD_ID + ".";
        int count = 0;
        for (Component message : Compat.gameTests().messagesTo(player)) {
            if (message.getContents() instanceof TranslatableContents contents
                    && contents.getKey().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Il banco di un test: la stazione dei test posata con il fronte a nord, il giocatore A in sopravvivenza davanti,
     * i suoni di {@link FrameSounds} registrati nel riquadro della struttura, e cio' che il test aggiunge (altri
     * giocatori, telai). {@link #run} esegue il corpo del test: se il corpo non ha messo un ultimo passo
     * ({@link #last}) pulisce e chiude il test con successo subito; un passo intermedio ({@link #then}) o il corpo che
     * fallisce puliscono, e l'ultimo passo pulisce sempre. Pulire chiude i menu con il cursore vuoto (cio' che resta
     * sul cursore tornerebbe in un inventario che puo' essere pieno, o cadrebbe), svuota gli inventari dei giocatori,
     * toglie i telai, i loro record, gli oggetti a terra nella struttura e le due meta' della stazione.
     */
    private static final class Bench {
        private final GameTestHelper helper;
        private final BlockPos side;
        private final FrameSounds.Recording sounds;
        /** Il giocatore A: in sopravvivenza, in {@link #STAND}, rivolto a nord. */
        private final ServerPlayer a;
        private final List<ServerPlayer> players = new ArrayList<>();
        private final List<ExoframeEntity> frames = new ArrayList<>();
        private boolean lastScheduled;
        private boolean cleaned;

        Bench(GameTestHelper helper) {
            this.helper = helper;
            this.side = placeStation(helper, MAIN, Direction.NORTH);
            this.sounds = FrameSounds.record(helper.getLevel(), structureBox(helper));
            this.a = player(STAND);
        }

        /** Un giocatore finto in sopravvivenza con i piedi nel punto relativo alla struttura, rivolto a nord. */
        ServerPlayer player(Vec3 feet) {
            return join(Compat.gameTests().mockServerPlayer(helper), feet);
        }

        /**
         * Un operatore finto ({@code GameTests.operatorPlayer}: livello di permesso 2, e {@code isCreative()} che segue
         * la modalita' di gioco) in sopravvivenza, con i piedi nel punto relativo alla struttura, rivolto a nord.
         */
        ServerPlayer operator(Vec3 feet) {
            return join(Compat.gameTests().operatorPlayer(helper), feet);
        }

        private ServerPlayer join(ServerPlayer player, Vec3 feet) {
            players.add(player);
            player.setGameMode(GameType.SURVIVAL);
            standFacingNorth(helper, player, feet);
            return player;
        }

        /** Un telaio con lo stato dato sopra la meta' principale ({@code frameOnStation}). */
        ExoframeEntity frame(FrameState state) {
            return adopt(frameOnStation(helper, state, MAIN));
        }

        /** Un'entita' telaio che il test ha fatto da se': la pulizia toglie lei e il suo record. */
        ExoframeEntity adopt(ExoframeEntity frame) {
            frames.add(frame);
            return frame;
        }

        /** Il menu del giocatore sulla stazione dei test ({@code openStation}). */
        StationMenu open(ServerPlayer player) {
            return openStation(helper, player, MAIN);
        }

        /** I suoni registrati devono essere quelli attesi, in ordine. */
        void assertSounds(List<Sound> expected, String when) {
            List<Sound> heard = sounds.sounds();
            helper.assertTrue(heard.equals(expected), when + " the sounds are " + heard + ", expected " + expected);
        }

        /** Esegue il corpo del test, come dice {@link Bench}. */
        void run(Runnable body) {
            boolean ran = false;
            try {
                body.run();
                ran = true;
            } finally {
                if (!ran) {
                    cleanUp();
                }
            }
            if (!lastScheduled) {
                cleanUp();
                helper.succeed();
            }
        }

        /** Un passo intermedio del test, {@code delay} tick dopo il corpo: se fallisce, pulisce. */
        void then(int delay, Consumer<GameTestHelper> step) {
            helper.runAfterDelay(delay, () -> runInSequence(helper, h -> {
                boolean passed = false;
                try {
                    step.accept(h);
                    passed = true;
                } finally {
                    if (!passed) {
                        cleanUp();
                    }
                }
            }));
        }

        /**
         * L'ultimo passo del test, {@code delay} tick dopo il corpo: pulisce comunque e, se e' passato, chiude il test.
         */
        void last(int delay, Consumer<GameTestHelper> step) {
            lastScheduled = true;
            helper.runAfterDelay(delay, () -> runInSequence(helper, h -> {
                try {
                    step.accept(h);
                } finally {
                    cleanUp();
                }
                h.succeed();
            }));
        }

        /**
         * Chiude i menu dei giocatori con il cursore vuoto, toglie i telai, i loro record e le due meta' della
         * stazione: nella pulizia, o a meta' di un test che continua senza; ripeterlo non fa danni.
         */
        void removeStationAndFrames() {
            for (ServerPlayer player : players) {
                if (player.containerMenu != player.inventoryMenu) {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.closeContainer();
                }
            }
            MinecraftServer server = helper.getLevel().getServer();
            for (ExoframeEntity frame : frames) {
                removeFrame(server, frame);
            }
            helper.setBlock(MAIN, Blocks.AIR);
            helper.setBlock(side, Blocks.AIR);
        }

        /** Toglie tutto cio' che il test ha messo, una volta sola. */
        void cleanUp() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            sounds.close();
            removeStationAndFrames();
            for (ServerPlayer player : players) {
                player.getInventory().clearContent();
            }
            for (ItemEntity drop : itemsIn(helper)) {
                drop.discard();
            }
        }
    }
}
