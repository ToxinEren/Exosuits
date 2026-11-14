package com.toxineren.exoframes.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModEntities;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.content.PieceKind;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.ModuleType;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FramePlacement;
import com.toxineren.exoframes.frame.FrameRealign;
import com.toxineren.exoframes.frame.Handouts;
import com.toxineren.exoframes.frame.Placeholders;
import com.toxineren.exoframes.frame.SafeSpots;
import com.toxineren.exoframes.frame.SlotGuard;
import com.toxineren.exoframes.frame.SuitSize;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Test in gioco (GameTest vanilla) comuni alle quattro varianti, senza differenze di versione o loader: quelle stanno
 * dietro {@link Compat#gameTests()}. Si lanciano con {@code ./gradlew :<variante>:runGameTestServer} o con
 * {@code bash tools/run_gametests.sh}.
 *
 * <p>Per aggiungere un test: un metodo {@code private static void temaCaso(GameTestHelper helper)} che chiude con
 * {@code helper.succeed()}, e una riga {@code add(tests, "temaCaso", ExoframeGameTests::temaCaso);} in
 * {@link #generate()}. tools/run_gametests.sh conta queste righe per sapere quanti test aspettarsi; la lista e' la
 * stessa sulle quattro varianti (il corpo di un test puo' dipendere dalla versione, la lista no).
 *
 * <p>Tutti i test girano sullo stesso server, con lo stesso dato salvato e lo stesso livello, e cominciano nello
 * stesso tick. Percio' un test:
 * <ul>
 * <li>controlla solo i suoi giocatori finti e i suoi {@code frameId}, mai conteggi globali (quanti telai, quanti
 * giocatori, quante voci orfane in tutto);</li>
 * <li>sposta i suoi giocatori finti dentro la sua struttura prima di cercare qualcosa intorno a loro;</li>
 * <li>prima di {@code succeed()} toglie quello che ha creato: i record piazzati e indossati, le entita', gli oggetti a
 * terra, le voci orfane ({@code GameTests.forgetOrphaned});</li>
 * <li>rimette com'erano le regole del gioco e il bordo del mondo che cambia, anche se fallisce ({@code finally});</li>
 * <li>non dipende dal contatore dei tick del giocatore finto (che il server non fa avanzare): se serve, lo imposta;</li>
 * <li>non lascia avvisi nel log oltre a quelli che prova di proposito.</li>
 * </ul>
 *
 * <p>I test nascono da un {@link GameTestGenerator} e non dall'annotazione {@code @GameTest}, perche' i loader
 * risolvono in modo diverso il nome della struttura dell'annotazione; il costruttore a 7 argomenti di
 * {@link TestFunction} e' identico su 1.20.1 e 1.21.1. Nome del test e struttura portano entrambi il namespace della
 * mod: Forge 1.20.1 filtra i test generati sul namespace del nome, NeoForge su quello della struttura.
 */
public final class ExoframeGameTests {
    /** Struttura vuota 8x8x8 della mod: data/redstone_exoframes/structures/empty.nbt (1.20.1) e structure/ (1.21). */
    private static final String EMPTY_STRUCTURE = RedstoneExoframes.MOD_ID + ":empty";
    private static final int MAX_TICKS = 100;
    /** Versione dei dati di Minecraft 1.20.1. */
    private static final int DATA_VERSION_1_20_1 = 3465;
    private static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public ExoframeGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "wiringPlacesABlock", ExoframeGameTests::wiringPlacesABlock);
        add(tests, "frameStateOnMockPlayer", ExoframeGameTests::frameStateOnMockPlayer);
        add(tests, "itemDataRoundTripsOnARealStack", ExoframeGameTests::itemDataRoundTripsOnARealStack);
        add(tests, "placeholdersAreArmorItems", ExoframeGameTests::placeholdersAreArmorItems);
        add(tests, "debugWearPutsPlaceholdersAndSummary", ExoframeGameTests::debugWearPutsPlaceholdersAndSummary);
        add(tests, "debugWearRemoveClearsEverything", ExoframeGameTests::debugWearRemoveClearsEverything);
        add(tests, "debugWearWithStateAndColors", ExoframeGameTests::debugWearWithStateAndColors);
        add(tests, "debugWearHandlesTorsoWithoutValidSummary",
                ExoframeGameTests::debugWearHandlesTorsoWithoutValidSummary);
        add(tests, "debugWearDropsArmorWhenInventoryIsFull", ExoframeGameTests::debugWearDropsArmorWhenInventoryIsFull);
        add(tests, "suitModelItemIsRegistered", ExoframeGameTests::suitModelItemIsRegistered);
        add(tests, "suitGrowsBoxAndEyes", ExoframeGameTests::suitGrowsBoxAndEyes);
        add(tests, "suitCrawlsUnderLowCeiling", ExoframeGameTests::suitCrawlsUnderLowCeiling);
        add(tests, "registryPlaceEnterExitMovesOneRecord", ExoframeGameTests::registryPlaceEnterExitMovesOneRecord);
        add(tests, "stowedArmorSurvivesEnterAndExit", ExoframeGameTests::stowedArmorSurvivesEnterAndExit);
        add(tests, "stowedStackFromOlderVersionIsRead", ExoframeGameTests::stowedStackFromOlderVersionIsRead);
        add(tests, "summaryTagRoundTrips", ExoframeGameTests::summaryTagRoundTrips);
        add(tests, "exitPlacesTheFrameInTheGivenDimension", ExoframeGameTests::exitPlacesTheFrameInTheGivenDimension);
        add(tests, "pieceItemsKnowTheirKind", ExoframeGameTests::pieceItemsKnowTheirKind);
        add(tests, "pieceItemRoundTripsAPiece", ExoframeGameTests::pieceItemRoundTripsAPiece);
        add(tests, "cellItemCarriesItsCharge", ExoframeGameTests::cellItemCarriesItsCharge);
        add(tests, "frameEntityKeepsItsIdentityAndSummary", ExoframeGameTests::frameEntityKeepsItsIdentityAndSummary);
        add(tests, "frameEntityIgnoresDamageAndExplosions", ExoframeGameTests::frameEntityIgnoresDamageAndExplosions);
        add(tests, "frameEntityHasTheSuitBox", ExoframeGameTests::frameEntityHasTheSuitBox);
        add(tests, "placingCreatesRecordAndEntity", ExoframeGameTests::placingCreatesRecordAndEntity);
        add(tests, "placingRefusedWhereNoRoom", ExoframeGameTests::placingRefusedWhereNoRoom);
        add(tests, "placingRefusedWhereAnotherFrameStands", ExoframeGameTests::placingRefusedWhereAnotherFrameStands);
        add(tests, "entityWithoutRecordDiscardsItself", ExoframeGameTests::entityWithoutRecordDiscardsItself);
        add(tests, "entityOfRecordInAnotherDimensionDiscardsItself",
                ExoframeGameTests::entityOfRecordInAnotherDimensionDiscardsItself);
        add(tests, "missingEntityIsRecreated", ExoframeGameTests::missingEntityIsRecreated);
        add(tests, "movedEntityIsBroughtBack", ExoframeGameTests::movedEntityIsBroughtBack);
        add(tests, "realignLeavesUnloadedChunksAlone", ExoframeGameTests::realignLeavesUnloadedChunksAlone);
        add(tests, "groundBelowFindsTheFloor", ExoframeGameTests::groundBelowFindsTheFloor);
        add(tests, "exoframeItemPlacesAnEmptyFrame", ExoframeGameTests::exoframeItemPlacesAnEmptyFrame);
        add(tests, "exoframeItemRefusedUnderLowCeiling", ExoframeGameTests::exoframeItemRefusedUnderLowCeiling);
        add(tests, "debugPlaceBareAndScrap", ExoframeGameTests::debugPlaceBareAndScrap);
        add(tests, "frameEntityIgnoresPortals", ExoframeGameTests::frameEntityIgnoresPortals);
        add(tests, "mountPieceAndCellByHand", ExoframeGameTests::mountPieceAndCellByHand);
        add(tests, "secondArmGoesToTheFreeSide", ExoframeGameTests::secondArmGoesToTheFreeSide);
        add(tests, "unmountByHeightThenCellThenPickUp", ExoframeGameTests::unmountByHeightThenCellThenPickUp);
        add(tests, "fullInventoryOverflowStaysOnTheGround", ExoframeGameTests::fullInventoryOverflowStaysOnTheGround);
        add(tests, "unmountTakesTheClickedArmThenTheCell", ExoframeGameTests::unmountTakesTheClickedArmThenTheCell);
        add(tests, "strangerCannotTouchALockedFrame", ExoframeGameTests::strangerCannotTouchALockedFrame);
        add(tests, "unlockedFrameIsOpenToAll", ExoframeGameTests::unlockedFrameIsOpenToAll);
        add(tests, "unmountedPieceKeepsDurabilityAndColours",
                ExoframeGameTests::unmountedPieceKeepsDurabilityAndColours);
        add(tests, "enterStowsArmorAndWearsTheFrame", ExoframeGameTests::enterStowsArmorAndWearsTheFrame);
        add(tests, "enterRefusedWithoutAChargedCell", ExoframeGameTests::enterRefusedWithoutAChargedCell);
        add(tests, "enterRefusedUnderLowCeiling", ExoframeGameTests::enterRefusedUnderLowCeiling);
        add(tests, "secondEnterOnSameFrameFails", ExoframeGameTests::secondEnterOnSameFrameFails);
        add(tests, "ownerGetsBackArmorLeftInTheFrame", ExoframeGameTests::ownerGetsBackArmorLeftInTheFrame);
        add(tests, "strangerCannotEnterAFrameHoldingArmor", ExoframeGameTests::strangerCannotEnterAFrameHoldingArmor);
        add(tests, "exitReturnsArmorAndLeavesTheFrame", ExoframeGameTests::exitReturnsArmorAndLeavesTheFrame);
        add(tests, "exitWithFullInventoryDropsArmor", ExoframeGameTests::exitWithFullInventoryDropsArmor);
        add(tests, "exitInMidAirPutsTheFrameOnTheGround", ExoframeGameTests::exitInMidAirPutsTheFrameOnTheGround);
        add(tests, "exitWithoutAFrameDoesNothing", ExoframeGameTests::exitWithoutAFrameDoesNothing);
        add(tests, "enterExitAndEnterAgainKeepTheFrame", ExoframeGameTests::enterExitAndEnterAgainKeepTheFrame);
        add(tests, "deathLeavesTheFrameWithEverythingInside",
                ExoframeGameTests::deathLeavesTheFrameWithEverythingInside);
        add(tests, "deathInTheVoidUsesTheLastSafeSpot", ExoframeGameTests::deathInTheVoidUsesTheLastSafeSpot);
        add(tests, "deathWithKeepInventoryStillLeavesTheFrame",
                ExoframeGameTests::deathWithKeepInventoryStillLeavesTheFrame);
        add(tests, "exitWithoutGroundUsesTheLastSafeSpot", ExoframeGameTests::exitWithoutGroundUsesTheLastSafeSpot);
        add(tests, "slotGuardRestoresARemovedPlaceholder", ExoframeGameTests::slotGuardRestoresARemovedPlaceholder);
        add(tests, "slotGuardDeletesStrayCopies", ExoframeGameTests::slotGuardDeletesStrayCopies);
        add(tests, "slotGuardMovesForeignArmorOut", ExoframeGameTests::slotGuardMovesForeignArmorOut);
        add(tests, "placeholdersWithoutStateAreRemoved", ExoframeGameTests::placeholdersWithoutStateAreRemoved);
        add(tests, "rejoinRestoresPlaceholders", ExoframeGameTests::rejoinRestoresPlaceholders);
        add(tests, "summaryFollowsTheState", ExoframeGameTests::summaryFollowsTheState);
        add(tests, "slotGuardDeletesPlaceholdersInGridAndContainer",
                ExoframeGameTests::slotGuardDeletesPlaceholdersInGridAndContainer);
        add(tests, "thrownPlaceholderIsNotDropped", ExoframeGameTests::thrownPlaceholderIsNotDropped);
        add(tests, "debugWearViaGivesBackStowedArmor", ExoframeGameTests::debugWearViaGivesBackStowedArmor);
        add(tests, "enteringRemembersTheFrameSpotAsSafe", ExoframeGameTests::enteringRemembersTheFrameSpotAsSafe);
        add(tests, "safeSpotFollowsTheFloorDown", ExoframeGameTests::safeSpotFollowsTheFloorDown);
        add(tests, "deathClearsPlaceholdersFromTheCraftingGrid",
                ExoframeGameTests::deathClearsPlaceholdersFromTheCraftingGrid);
        add(tests, "ownerUnlocksAndStrangerCanUse", ExoframeGameTests::ownerUnlocksAndStrangerCanUse);
        add(tests, "strangerCannotUnlock", ExoframeGameTests::strangerCannotUnlock);
        add(tests, "lockAndUnlockTheWornFrame", ExoframeGameTests::lockAndUnlockTheWornFrame);
        add(tests, "wallHidesTheFrameFromTheCommand", ExoframeGameTests::wallHidesTheFrameFromTheCommand);
        add(tests, "removeDropsTheContents", ExoframeGameTests::removeDropsTheContents);
        add(tests, "debugNeedsOperator", ExoframeGameTests::debugNeedsOperator);
        add(tests, "registryFileIsReadBack", ExoframeGameTests::registryFileIsReadBack);
        add(tests, "quitSavesTheRegistryWithThePlayers", ExoframeGameTests::quitSavesTheRegistryWithThePlayers);
        add(tests, "exitStepsOutOfTheFrame", ExoframeGameTests::exitStepsOutOfTheFrame);
        add(tests, "exitInAShaftStaysInPlace", ExoframeGameTests::exitInAShaftStaysInPlace);
        add(tests, "exitStepsAroundFireAndDrops", ExoframeGameTests::exitStepsAroundFireAndDrops);
        add(tests, "worldBorderKeepsFramesInside", ExoframeGameTests::worldBorderKeepsFramesInside);
        add(tests, "exoframeItemRefusesADeepDrop", ExoframeGameTests::exoframeItemRefusesADeepDrop);
        add(tests, "deadPlayerCannotMountOrUnmount", ExoframeGameTests::deadPlayerCannotMountOrUnmount);
        add(tests, "spectatorCannotLeaveWithTheKey", ExoframeGameTests::spectatorCannotLeaveWithTheKey);
        add(tests, "guardLeavesAnotherPlayersPlaceholdersAlone",
                ExoframeGameTests::guardLeavesAnotherPlayersPlaceholdersAlone);
        add(tests, "guardScansTheRestOncePerSecondWithoutAFrame",
                ExoframeGameTests::guardScansTheRestOncePerSecondWithoutAFrame);
        add(tests, "realignRunsOnTheLevelTick", ExoframeGameTests::realignRunsOnTheLevelTick);
        return tests;
    }

    /** Piazza un blocco nella struttura vuota e verifica che ci sia: prova del cablaggio. */
    private static void wiringPlacesABlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.REDSTONE_BLOCK);
        helper.assertBlockPresent(Blocks.REDSTONE_BLOCK, pos);
        helper.succeed();
    }

    /** Stato del telaio per giocatore su un giocatore finto: update, poi get. */
    private static void frameStateOnMockPlayer(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        Compat.frameState().update(player, state -> state.setReinforced(true));
        Optional<FrameState> state = Compat.frameState().get(player);
        helper.assertTrue(state.isPresent(), "frame state missing after update");
        helper.assertTrue(state.get().reinforced(), "frame state not reinforced after update");
        Compat.frameState().clear(player);
        helper.succeed();
    }

    /**
     * Andata e ritorno dei dati degli item su uno stack vero (il percorso di scrittura che i test unitari non
     * raggiungono): un pezzo con aspetto non predefinito, poi il riassunto visivo, che non deve cancellare il pezzo.
     */
    private static void itemDataRoundTripsOnARealStack(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.REDSTONE_CELL.get());
        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of(new Decal("minecraft:creeper", "red")));
        PieceData piece = new PieceData(Tier.ARMORED, 120, 400, List.of(ModuleType.NIGHT_VISION), painted);
        Compat.itemData().setPiece(stack, piece);
        helper.assertTrue(Optional.of(piece).equals(Compat.itemData().piece(stack)),
                "piece data changed on round trip: " + Compat.itemData().piece(stack));

        SuitSummary summary = new SuitSummary(Map.of(
                PieceSlot.TORSO, new PieceLook(Tier.ARMORED, PieceCondition.DAMAGED, painted),
                PieceSlot.HELMET, new PieceLook(Tier.SCRAP, PieceCondition.BROKEN, Appearance.DEFAULT)));
        Compat.itemData().setSuitSummary(stack, summary);
        helper.assertTrue(Optional.of(summary).equals(Compat.itemData().suitSummary(stack)),
                "suit summary changed on round trip: " + Compat.itemData().suitSummary(stack));
        helper.assertTrue(Optional.of(piece).equals(Compat.itemData().piece(stack)),
                "writing the suit summary changed the piece data: " + Compat.itemData().piece(stack));
        helper.succeed();
    }

    /**
     * I quattro segnaposto degli slot armatura sono nel registro degli item con il loro id, sono {@link ArmorItem}
     * del tipo atteso e hanno la pila a 1. Si cercano per id e non dai campi di ModItems, cosi' il test parla dei
     * nomi che vedono i salvataggi e i comandi.
     */
    private static void placeholdersAreArmorItems(GameTestHelper helper) {
        Map<String, ArmorItem.Type> expected = Map.of(
                "exoframe_helmet", ArmorItem.Type.HELMET,
                "exoframe_torso", ArmorItem.Type.CHESTPLATE,
                "exoframe_legs", ArmorItem.Type.LEGGINGS,
                "exoframe_boots", ArmorItem.Type.BOOTS);
        expected.forEach((path, type) -> {
            ResourceLocation id = Ids.of(path);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "item not registered: " + id);
            Item item = BuiltInRegistries.ITEM.get(id);
            helper.assertTrue(item instanceof ArmorItem, "item is not an ArmorItem: " + id);
            helper.assertTrue(((ArmorItem) item).getType() == type,
                    "wrong armor type for " + id + ": " + ((ArmorItem) item).getType());
            int max = new ItemStack(item).getMaxStackSize();
            helper.assertTrue(max == 1, "max stack size of " + id + " is " + max + ", expected 1");
        });
        helper.succeed();
    }

    /**
     * L'item modello della tuta (l'animatable del renderer GeckoLib) e' nel registro con il suo id, e' un
     * {@link GeoItem} e non e' un'armatura: non si indossa mai. Che il server di test parta con l'item registrato
     * prova anche che la classe dell'item si carica senza classi client.
     */
    private static void suitModelItemIsRegistered(GameTestHelper helper) {
        ResourceLocation id = Ids.of("suit_model");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "item not registered: " + id);
        Item item = BuiltInRegistries.ITEM.get(id);
        helper.assertTrue(item == ModItems.SUIT_MODEL.get(), "registry entry " + id + " is not ModItems.SUIT_MODEL");
        helper.assertTrue(item instanceof GeoItem, "suit model item is not a GeoItem: " + item.getClass().getName());
        helper.assertTrue(!(item instanceof ArmorItem), "suit model item must not be armor");
        helper.succeed();
    }

    /**
     * {@link DebugWear#wear} su un giocatore con un elmo di ferro: il giocatore ha un record indossato vero, con lui
     * come proprietario, una cella carica e uno stato che da' il riassunto chiesto (sei pezzi SCRAP INTACT non
     * dipinti); i quattro slot armatura hanno i segnaposto, il torso porta il riassunto e l'elmo di ferro e' finito
     * nell'inventario, dove resta dopo {@link DebugWear#remove}, che toglie anche il record.
     */
    private static void debugWearPutsPlaceholdersAndSummary(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            pieces.put(slot, new PieceLook(Tier.SCRAP, PieceCondition.INTACT, Appearance.DEFAULT));
        }
        SuitSummary summary = new SuitSummary(pieces);

        DebugWear.wear(player, summary);

        FrameState state = Compat.frameState().get(player).orElse(null);
        helper.assertTrue(state != null && SuitSummary.of(state).equals(summary)
                && player.getUUID().equals(state.owner()) && state.cell() != null && !state.cell().isEmpty(),
                "worn record missing or not matching the summary: " + state);
        assertPlaceholdersWorn(helper, player);
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(Optional.of(summary).equals(written), "wrong suit summary on the torso: " + written);
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 1,
                "iron helmet not moved to the inventory");

        DebugWear.remove(player);
        helper.assertTrue(countPlaceholders(player) == 0,
                countPlaceholders(player) + " placeholders left after remove");
        helper.assertTrue(Compat.frameState().wornFrameId(player).isEmpty(), "worn record left after remove");
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 1,
                "iron helmet not in the inventory after remove");
        helper.succeed();
    }

    /**
     * {@link DebugWear#remove} toglie il record indossato e i segnaposto dagli slot armatura e dal resto
     * dell'inventario. Un secondo {@code wear} sostituisce i segnaposto gia' indossati senza metterli nell'inventario
     * e tiene lo stesso record, con i pezzi nuovi.
     */
    private static void debugWearRemoveClearsEverything(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        DebugWear.wear(player, SuitSummary.BARE);
        UUID frameId = frames.wornFrameId(player).orElse(null);
        Map<PieceSlot, PieceLook> helmet = Map.of(PieceSlot.HELMET,
                new PieceLook(Tier.SCRAP, PieceCondition.DAMAGED, Appearance.DEFAULT));
        DebugWear.wear(player, new SuitSummary(helmet));
        helper.assertTrue(countPlaceholders(player) == 4,
                "wearing twice left " + countPlaceholders(player) + " placeholders, expected 4");
        helper.assertTrue(frameId != null && Optional.of(frameId).equals(frames.wornFrameId(player))
                && SuitSummary.of(frames.get(player).orElseThrow()).equals(new SuitSummary(helmet)),
                "second wear did not replace the pieces of the same record: " + frames.get(player));
        player.getInventory().items.set(5, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_TORSO.get()));

        DebugWear.remove(player);

        helper.assertTrue(countPlaceholders(player) == 0,
                countPlaceholders(player) + " placeholders left after remove");
        helper.assertTrue(frames.wornFrameId(player).isEmpty(), "worn record left after remove");
        helper.assertTrue(Compat.gameTests().orphanedStowed(helper.getLevel().getServer(), frameId).isEmpty(),
                "remove left orphaned entries");
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            helper.assertTrue(player.getItemBySlot(slot).isEmpty(), "armor slot not empty after remove: " + slot);
        }
        helper.succeed();
    }

    /**
     * Il comando {@code /exoframe debug wear rottame 2 <primario> <secondario>}: un record indossato con sei pezzi
     * SCRAP CRITICAL con i colori dati, e lo stesso riassunto sul torso. Prima un colore malformato fa fallire il
     * comando senza cambiare nulla (nessun record). Alla fine {@code via} toglie il record.
     */
    private static void debugWearWithStateAndColors(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();

        boolean failed = false;
        try {
            dispatcher.execute("exoframe debug wear rottame 2 12345G 00ff00", source);
        } catch (CommandSyntaxException e) {
            failed = true;
        }
        helper.assertTrue(failed, "a malformed color did not fail the command");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                && Compat.frameState().wornFrameId(player).isEmpty(), "a failed command changed something");

        runCommand(dispatcher, source, "exoframe debug wear rottame 2 Ab12Ef 00ff00");
        assertPlaceholdersWorn(helper, player);
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(written.isPresent(), "no suit summary on the torso");
        helper.assertTrue(written.get().pieces().keySet().equals(EnumSet.allOf(PieceSlot.class)),
                "summary does not cover the six slots: " + written.get());
        Appearance painted = new Appearance(0xAB12EF, 0x00FF00, List.of());
        written.get().pieces().forEach((slot, look) -> helper.assertTrue(
                look.equals(new PieceLook(Tier.SCRAP, PieceCondition.CRITICAL, painted)),
                "wrong look for " + slot + ": " + look));
        helper.assertTrue(countInMainInventory(player, Items.IRON_CHESTPLATE) == 1,
                "iron chestplate not moved to the inventory");
        Optional<SuitSummary> recorded = Compat.frameState().get(player).map(SuitSummary::of);
        helper.assertTrue(recorded.equals(written), "record state gives " + recorded + ", torso " + written);
        runCommand(dispatcher, source, "exoframe debug wear via");
        helper.assertTrue(Compat.frameState().wornFrameId(player).isEmpty() && countPlaceholders(player) == 0,
                "via left the record or placeholders");
        helper.succeed();
    }

    /**
     * Segnaposto del torso con riassunto assente o malformato, come da {@code /give}, altre mod o dati vecchi: la
     * lettura non lancia (vuota senza riassunto o con {@code visual} non compound, telaio nudo se le voci sono tutte
     * sconosciute), {@code via} li toglie ovunque anche senza un record, {@code nudo} sostituisce quello indossato e
     * crea il record nudo, e {@code via} lo toglie.
     */
    private static void debugWearHandlesTorsoWithoutValidSummary(GameTestHelper helper) {
        ItemStack bare = new ItemStack(ModItems.EXOFRAME_TORSO.get());
        helper.assertTrue(Compat.itemData().suitSummary(bare).isEmpty(),
                "summary read from a torso without data: " + Compat.itemData().suitSummary(bare));

        ItemStack notCompound = new ItemStack(ModItems.EXOFRAME_TORSO.get());
        CompoundTag stringVisual = new CompoundTag();
        stringVisual.putString(NbtKeys.VISUAL, "garbage");
        Compat.gameTests().writeRawItemData(notCompound, stringVisual);
        helper.assertTrue(Compat.itemData().suitSummary(notCompound).isEmpty(),
                "summary read from a non-compound visual: " + Compat.itemData().suitSummary(notCompound));

        ItemStack unknownEntries = new ItemStack(ModItems.EXOFRAME_TORSO.get());
        CompoundTag unknownSlot = new CompoundTag();
        unknownSlot.putString(NbtKeys.TIER, Tier.SCRAP.id());
        CompoundTag unknownTier = new CompoundTag();
        unknownTier.putString(NbtKeys.TIER, "mythic");
        CompoundTag pieces = new CompoundTag();
        pieces.put("left_wing", unknownSlot);
        pieces.put("helmet", unknownTier);
        CompoundTag visual = new CompoundTag();
        visual.put(NbtKeys.PIECES, pieces);
        CompoundTag root = new CompoundTag();
        root.put(NbtKeys.VISUAL, visual);
        Compat.gameTests().writeRawItemData(unknownEntries, root);
        helper.assertTrue(Optional.of(SuitSummary.BARE).equals(Compat.itemData().suitSummary(unknownEntries)),
                "unknown entries not skipped: " + Compat.itemData().suitSummary(unknownEntries));

        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.CHEST, bare);
        player.getInventory().items.set(3, notCompound);
        player.getInventory().offhand.set(0, unknownEntries);
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();

        runCommand(dispatcher, source, "exoframe debug wear via");
        helper.assertTrue(countPlaceholders(player) == 0,
                countPlaceholders(player) + " placeholders left after via");

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        runCommand(dispatcher, source, "exoframe debug wear nudo");
        assertPlaceholdersWorn(helper, player);
        helper.assertTrue(countPlaceholders(player) == 4,
                countPlaceholders(player) + " placeholders after nudo, expected the 4 worn ones");
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(Optional.of(SuitSummary.BARE).equals(written), "wrong summary after nudo: " + written);
        helper.assertTrue(Compat.frameState().get(player).map(SuitSummary::of).equals(Optional.of(SuitSummary.BARE)),
                "no bare worn record after nudo");
        runCommand(dispatcher, source, "exoframe debug wear via");
        helper.assertTrue(Compat.frameState().wornFrameId(player).isEmpty() && countPlaceholders(player) == 0,
                "via left the record or placeholders");
        helper.succeed();
    }

    /**
     * Con l'inventario principale pieno l'armatura spostata da {@link DebugWear#wear} finisce a terra accanto al
     * giocatore, intera, e nulla dell'inventario va perso.
     */
    private static void debugWearDropsArmorWhenInventoryIsFull(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        int slots = player.getInventory().items.size();
        for (int i = 0; i < slots; i++) {
            player.getInventory().items.set(i, new ItemStack(Items.DIRT, 64));
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));

        DebugWear.wear(player, SuitSummary.BARE);

        assertPlaceholdersWorn(helper, player);
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == slots * 64, "inventory contents changed");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(4), entity -> entity.getItem().is(Items.IRON_HELMET));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                "expected one dropped iron helmet next to the player, found " + dropped.size());
        helper.assertTrue(Compat.frameState().wornFrameId(player).isPresent(), "no worn record after wear");
        dropped.forEach(Entity::discard);
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Grandezza reale (design, par. 4.8): {@link DebugWear#wear} porta subito ingombro e occhi a quelli della tuta
     * (0,8 x 2,97, occhi 1,62 * 1,65 e, accovacciati, 1,27 * 1,65), con il riquadro ricostruito e quello di culling
     * allargato ai lati; accovacciata 0,8 x 2,475 e a nuoto 0,8 x 0,99 con gli occhi 1,65 volte quelli vanilla, sonno e
     * morte come senza tuta; {@link DebugWear#remove} li riporta a quelli vanilla. Un segnaposto messo nello slot senza
     * passare da DebugWear cambia la grandezza solo al tick ({@link SuitSize#tick}), e cosi' toglierlo.
     */
    private static void suitGrowsBoxAndEyes(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        SuitSize.tick(player);
        assertSize(helper, player, "without the suit", 0.6f, 1.8f, 1.62f, 1.27f);
        helper.assertTrue(SuitSize.cullingBox(player, player.getBoundingBox()).equals(player.getBoundingBox()),
                "culling box widened without the suit");
        String vanillaSleeping = sizeInPose(player, Pose.SLEEPING);
        String vanillaDying = sizeInPose(player, Pose.DYING);

        DebugWear.wear(player, SuitSummary.BARE);
        assertSize(helper, player, "after wear", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f, 1.27f * 1.65f);
        AABB box = player.getBoundingBox();
        AABB standing = SuitSize.standingBox(player.position());
        helper.assertTrue(near(box, standing), "bounding box not rebuilt after wear: " + box + ", expected " + standing);
        helper.assertTrue(near(SuitSize.cullingBox(player, box), box.inflate(0.5, 0, 0.5)),
                "culling box not widened by 0.5 on the sides: " + SuitSize.cullingBox(player, box));
        SuitSize.tick(player);
        assertSize(helper, player, "after wear and a tick", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f, 1.27f * 1.65f);
        assertPoseSize(helper, player, Pose.CROUCHING, 0.8f, 1.5f * 1.65f, 1.27f * 1.65f);
        assertPoseSize(helper, player, Pose.SWIMMING, 0.8f, 0.6f * 1.65f, 0.4f * 1.65f);
        helper.assertTrue(sizeInPose(player, Pose.SLEEPING).equals(vanillaSleeping),
                "sleeping size changed by the suit: " + sizeInPose(player, Pose.SLEEPING) + ", vanilla "
                        + vanillaSleeping);
        helper.assertTrue(sizeInPose(player, Pose.DYING).equals(vanillaDying),
                "dying size changed by the suit: " + sizeInPose(player, Pose.DYING) + ", vanilla " + vanillaDying);

        DebugWear.remove(player);
        assertSize(helper, player, "after remove", 0.6f, 1.8f, 1.62f, 1.27f);
        helper.assertTrue(near((float) player.getBoundingBox().getYsize(), 1.8f),
                "bounding box not rebuilt after remove: " + player.getBoundingBox());
        SuitSize.tick(player);

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        SuitSize.tick(player);
        assertSize(helper, player, "torso placed in the slot, after a tick", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f,
                1.27f * 1.65f);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        SuitSize.tick(player);
        assertSize(helper, player, "torso taken from the slot, after a tick", 0.6f, 1.8f, 1.62f, 1.27f);
        helper.succeed();
    }

    /**
     * In uno spazio alto 2 la tuta non sta ne' in piedi (2,97) ne' accovacciata (2,475): il gioco la mette da solo a
     * carponi (0,8 x 0,99). Il giocatore ha un record indossato e il segnaposto del torso va nello slot senza
     * DebugWear, che ricalcolerebbe da solo: qui il ricalcolo deve arrivare dal tick del giocatore (la grandezza in
     * TickEvent.PLAYER_PRE, l'autoriparazione degli slot in PLAYER_POST). I giocatori finti non li fa girare il server.
     */
    private static void suitCrawlsUnderLowCeiling(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
        Vec3 feet = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        player.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        player.doTick();
        helper.assertTrue(player.getPose() == Pose.STANDING, "player without the suit not standing: " + size(player));

        Compat.frameState().update(player, state -> { });
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        player.doTick();
        player.doTick();
        helper.assertTrue(player.getPose() == Pose.SWIMMING, "suit under a ceiling at 2 not crawling: " + size(player));
        helper.assertTrue(near(player.getBbHeight(), 0.6f * 1.65f) && near(player.getBbWidth(), 0.8f),
                "wrong crawling box: " + size(player));
        helper.assertTrue(near((float) player.getBoundingBox().getYsize(), 0.6f * 1.65f),
                "crawling bounding box not rebuilt: " + player.getBoundingBox());
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Registro dei telai: lo stesso {@code frameId} e' prima piazzato, poi indossato, poi piazzato altrove, e non sta
     * mai in due posti; la modifica di un piazzato cambia lo stato che il giocatore poi indossa (lo stesso oggetto).
     */
    private static void registryPlaceEnterExitMovesOneRecord(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        FrameState state = new FrameState(RedstoneExoframes.config());
        Vec3 first = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
        UUID frameId = frames.place(level, state, first, 90f);
        helper.assertTrue(frameId != null, "place returned no frame id");

        PlacedFrame placed = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(placed != null, "placed frame not found after place");
        helper.assertTrue(placed.frameId().equals(frameId) && placed.state() == state
                && placed.dimension().equals(level.dimension()) && placed.position().equals(first)
                && placed.yaw() == 90f && !placed.hasStowed(), "placed frame differs from what was placed: " + placed);
        helper.assertTrue(placedIds(level).contains(frameId), "placed frame missing from placedIn");
        helper.assertTrue(frames.updatePlaced(server, frameId, s -> s.setReinforced(true)), "updatePlaced refused");
        helper.assertTrue(frames.placed(server, frameId).orElseThrow().state().reinforced(), "update not applied");

        helper.assertTrue(frames.enter(player, frameId), "enter refused");
        helper.assertTrue(frames.placed(server, frameId).isEmpty(), "frame still placed after enter");
        helper.assertTrue(!placedIds(level).contains(frameId), "frame still in placedIn after enter");
        helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(player)), "worn frame id differs after enter");
        helper.assertTrue(frames.get(player).orElse(null) == state, "worn state is not the placed state");
        helper.assertTrue(!frames.enter(player, frameId), "second enter of the same frame accepted");
        helper.assertTrue(!frames.updatePlaced(server, frameId, s -> s.setUnlocked(true)), "worn frame updated as placed");
        helper.assertTrue(!frames.removePlaced(server, frameId), "worn frame removed as placed");

        Vec3 second = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 4)));
        helper.assertTrue(Optional.of(frameId).equals(frames.exit(player, level, second, -45f)), "exit lost the id");
        helper.assertTrue(frames.wornFrameId(player).isEmpty() && frames.get(player).isEmpty(),
                "frame still worn after exit");
        placed = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(placed != null && placed.state() == state && placed.position().equals(second)
                && placed.yaw() == -45f && placed.dimension().equals(level.dimension()),
                "frame not placed at the exit spot: " + placed);
        helper.assertTrue(frames.exit(player, level, first, 0f).isEmpty(), "exit without a worn frame moved something");

        helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
        helper.assertTrue(frames.placed(server, frameId).isEmpty() && !placedIds(level).contains(frameId),
                "frame still placed after removePlaced");
        helper.succeed();
    }

    /**
     * Un elmo di ferro danneggiato stivato nel telaio indossato resta nel record attraverso uscita e nuovo ingresso, e
     * torna identico, con il suo slot, sia dal telaio indossato sia da quello piazzato; ogni presa svuota la lista.
     */
    private static void stowedArmorSurvivesEnterAndExit(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        UUID frameId = frames.place(level, new FrameState(RedstoneExoframes.config()), spot, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "could not enter the placed frame");

        frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, helmet.copy()),
                new StowedStack(EquipmentSlot.FEET, ItemStack.EMPTY)));
        helper.assertTrue(frames.exit(player, level, spot, 0f).isPresent(), "exit failed");
        helper.assertTrue(frames.placed(server, frameId).orElseThrow().hasStowed(), "placed frame lost its armor");
        helper.assertTrue(frames.takeStowed(player).isEmpty(), "took stowed armor from a frame not worn");

        helper.assertTrue(frames.enter(player, frameId), "second enter failed");
        assertOneStowedHelmet(helper, frames.takeStowed(player), helmet, "worn frame");
        helper.assertTrue(frames.takeStowed(player).isEmpty(), "takeStowed did not empty the list");

        frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, helmet.copy())));
        helper.assertTrue(frames.exit(player, level, spot, 0f).isPresent(), "second exit failed");
        assertOneStowedHelmet(helper, frames.takeStowedOfPlaced(server, frameId), helmet, "placed frame");
        helper.assertTrue(frames.takeStowedOfPlaced(server, frameId).isEmpty(),
                "takeStowedOfPlaced did not empty the list");
        helper.assertTrue(!frames.placed(server, frameId).orElseThrow().hasStowed(), "placed frame still has armor");

        helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
        helper.succeed();
    }

    /**
     * Una voce dell'armatura stivata scritta a mano nel formato di 1.20.1 ({@code Count}, {@code tag.Damage}) con
     * {@code data_version} 3465 si legge come elmo di ferro con danno 5: su 1.21.1 passando dal correttore di dati, su
     * 1.20.1 perche' e' il formato corrente, e li' il test fa anche un giro completo di scrittura e lettura. Poi una
     * voce illeggibile (item sconosciuto): le prese la saltano e resta nel record.
     */
    private static void stowedStackFromOlderVersionIsRead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        int current = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
        UUID frameId = frames.place(level, new FrameState(RedstoneExoframes.config()), spot, 0f);
        helper.assertTrue(frameId != null, "place returned no frame id");

        CompoundTag damage = new CompoundTag();
        damage.putInt("Damage", 5);
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:iron_helmet");
        item.putByte("Count", (byte) 1);
        item.put("tag", damage);
        helper.assertTrue(Compat.gameTests().addRawStowed(server, frameId, stowedEntry(DATA_VERSION_1_20_1, item)),
                "raw stowed entry not added");
        ItemStack expected = new ItemStack(Items.IRON_HELMET);
        expected.setDamageValue(5);
        assertOneStowedHelmet(helper, frames.takeStowedOfPlaced(server, frameId), expected, "1.20.1 entry");

        if (current == DATA_VERSION_1_20_1) {
            // 1.20.1: il formato scritto a mano e' quello corrente; giro completo di scrittura e lettura
            ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
            helper.assertTrue(frames.enter(player, frameId), "enter refused");
            frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, expected.copy())));
            helper.assertTrue(frames.exit(player, level, spot, 0f).isPresent(), "exit failed");
            assertOneStowedHelmet(helper, frames.takeStowedOfPlaced(server, frameId), expected, "round trip");
        } else {
            helper.assertTrue(current > DATA_VERSION_1_20_1, "unexpected data version " + current);
        }

        CompoundTag unknown = new CompoundTag();
        unknown.putString("id", RedstoneExoframes.MOD_ID + ":no_such_item");
        unknown.putByte("Count", (byte) 1);
        unknown.putInt("count", 1);
        helper.assertTrue(Compat.gameTests().addRawStowed(server, frameId, stowedEntry(current, unknown)),
                "raw stowed entry not added");
        helper.assertTrue(!frames.placed(server, frameId).orElseThrow().hasStowed(),
                "hasStowed counts an unreadable entry");
        CompoundTag unreadable = stowedEntry(current, unknown);
        helper.assertTrue(frames.takeStowedOfPlaced(server, frameId).isEmpty(), "unreadable entry was taken");
        helper.assertTrue(frames.takeStowedOfPlaced(server, frameId).isEmpty(), "unreadable entry was taken later");
        // nulla da restituire: il telaio si puo' smontare, e la voce non si perde
        helper.assertTrue(!frames.placed(server, frameId).orElseThrow().hasStowed(),
                "a frame with only unreadable stowed entries claims armor to give back");

        helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
        helper.assertTrue(List.of(unreadable).equals(Compat.gameTests().orphanedStowed(server, frameId)),
                "unreadable entry not kept among the orphaned ones of its frame: "
                        + Compat.gameTests().orphanedStowed(server, frameId));
        Compat.gameTests().forgetOrphaned(server, frameId);
        helper.succeed();
    }

    /**
     * L'uscita mette il telaio nella dimensione del livello dato, anche se non e' quella del giocatore (il Nether del
     * server di test): {@code placedIn} lo trova li' e non nel livello del giocatore.
     */
    private static void exitPlacesTheFrameInTheGivenDimension(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether != level, "test server has no separate Nether level");
        FrameStateAccess frames = Compat.frameState();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 5)));
        UUID frameId = frames.place(level, new FrameState(RedstoneExoframes.config()), spot, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "could not enter the placed frame");

        Vec3 netherSpot = new Vec3(8.5, 70.0, -3.5);
        helper.assertTrue(Optional.of(frameId).equals(frames.exit(player, nether, netherSpot, 30f)), "exit failed");
        PlacedFrame placed = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(placed != null && placed.dimension().equals(Level.NETHER)
                && placed.position().equals(netherSpot) && placed.yaw() == 30f,
                "frame not placed in the Nether at the exit spot: " + placed);
        helper.assertTrue(placedIds(nether).contains(frameId), "frame missing from the Nether's placed frames");
        helper.assertTrue(!placedIds(level).contains(frameId), "frame listed in the player's level");

        helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
        helper.succeed();
    }

    /** Il riassunto visivo scritto come tag si rilegge uguale; un tag senza riassunto da' vuoto. */
    private static void summaryTagRoundTrips(GameTestHelper helper) {
        FrameStateAccess frames = Compat.frameState();
        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of(new Decal("minecraft:creeper", "red")));
        SuitSummary summary = new SuitSummary(Map.of(
                PieceSlot.TORSO, new PieceLook(Tier.ARMORED, PieceCondition.DAMAGED, painted),
                PieceSlot.HELMET, new PieceLook(Tier.SCRAP, PieceCondition.BROKEN, Appearance.DEFAULT)));
        helper.assertTrue(Optional.of(summary).equals(frames.readSummaryTag(frames.writeSummaryTag(summary))),
                "suit summary changed on round trip");
        helper.assertTrue(Optional.of(SuitSummary.BARE).equals(
                frames.readSummaryTag(frames.writeSummaryTag(SuitSummary.BARE))), "bare summary changed on round trip");
        helper.assertTrue(frames.readSummaryTag(new CompoundTag()).isEmpty(), "summary read from an empty tag");
        helper.succeed();
    }

    /**
     * I quattro pezzi Rottame sono nel registro con il loro id, sono {@link PieceItem} del tipo e del livello attesi,
     * coincidono con i campi di ModItems e hanno la pila a 1; braccio e gamba valgono per i due lati.
     */
    private static void pieceItemsKnowTheirKind(GameTestHelper helper) {
        Map<String, PieceKind> expected = Map.of(
                "scrap_helmet", PieceKind.HELMET,
                "scrap_torso", PieceKind.TORSO,
                "scrap_arm", PieceKind.ARM,
                "scrap_leg", PieceKind.LEG);
        Map<String, Item> fields = Map.of(
                "scrap_helmet", ModItems.SCRAP_HELMET.get(),
                "scrap_torso", ModItems.SCRAP_TORSO.get(),
                "scrap_arm", ModItems.SCRAP_ARM.get(),
                "scrap_leg", ModItems.SCRAP_LEG.get());
        expected.forEach((path, kind) -> {
            ResourceLocation id = Ids.of(path);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "item not registered: " + id);
            Item item = BuiltInRegistries.ITEM.get(id);
            helper.assertTrue(item == fields.get(path), "registry entry " + id + " is not the ModItems field");
            helper.assertTrue(item instanceof PieceItem, "item is not a PieceItem: " + id);
            PieceItem piece = (PieceItem) item;
            helper.assertTrue(piece.kind() == kind, "wrong kind for " + id + ": " + piece.kind());
            helper.assertTrue(piece.tier() == Tier.SCRAP, "wrong tier for " + id + ": " + piece.tier());
            int max = new ItemStack(item).getMaxStackSize();
            helper.assertTrue(max == 1, "max stack size of " + id + " is " + max + ", expected 1");
        });
        helper.assertTrue(PieceKind.ARM.slots().equals(List.of(PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM)),
                "arm slots: " + PieceKind.ARM.slots());
        helper.assertTrue(PieceKind.LEG.slots().equals(List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG)),
                "leg slots: " + PieceKind.LEG.slots());
        helper.succeed();
    }

    /**
     * Un pezzo danneggiato diventa lo stack dell'oggetto giusto e torna pezzo con la stessa durabilita' (anche
     * montato sull'altro lato), con moduli e aspetto nei dati dello stack; uno stack senza dati e' un pezzo nuovo;
     * dati di un altro livello prendono il livello dell'oggetto, con la durabilita' ridotta al nuovo massimo.
     */
    private static void pieceItemRoundTripsAPiece(GameTestHelper helper) {
        int scrapMax = RedstoneExoframes.config().tiers.maxDurability.get(Tier.SCRAP);
        Piece damaged = new Piece(PieceSlot.LEFT_ARM, Tier.SCRAP, scrapMax, scrapMax);
        damaged.damage(37);
        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of(new Decal("minecraft:creeper", "red")));
        ItemStack stack = PieceItem.fromPiece(damaged, List.of(ModuleType.NIGHT_VISION), painted);
        helper.assertTrue(stack.getItem() == ModItems.SCRAP_ARM.get(), "wrong item for an arm: " + stack.getItem());
        helper.assertTrue(stack.getCount() == 1, "stack count " + stack.getCount());
        PieceData data = Compat.itemData().piece(stack).orElse(null);
        helper.assertTrue(data != null && data.modules().equals(List.of(ModuleType.NIGHT_VISION))
                && data.appearance().equals(painted), "modules or appearance not on the stack: " + data);

        PieceItem arm = ModItems.SCRAP_ARM.get();
        Piece back = arm.toPiece(stack, PieceSlot.RIGHT_ARM);
        helper.assertTrue(back.slot() == PieceSlot.RIGHT_ARM && back.tier() == Tier.SCRAP
                        && back.durability() == damaged.durability() && back.maxDurability() == scrapMax,
                "piece changed on round trip: " + back.slot() + " " + back.tier() + " " + back.durability() + "/"
                        + back.maxDurability());

        PieceItem helmet = ModItems.SCRAP_HELMET.get();
        Piece fresh = helmet.toPiece(new ItemStack(helmet), PieceSlot.HELMET);
        helper.assertTrue(fresh.tier() == Tier.SCRAP && fresh.durability() == scrapMax
                        && fresh.maxDurability() == scrapMax,
                "bare stack is not a fresh piece: " + fresh.durability() + "/" + fresh.maxDurability());

        ItemStack foreign = new ItemStack(ModItems.SCRAP_LEG.get());
        Compat.itemData().setPiece(foreign,
                new PieceData(Tier.ARMORED, scrapMax + 50, scrapMax + 100, List.of(), Appearance.DEFAULT));
        Piece clamped = ModItems.SCRAP_LEG.get().toPiece(foreign, PieceSlot.LEFT_LEG);
        helper.assertTrue(clamped.tier() == Tier.SCRAP && clamped.durability() == scrapMax
                        && clamped.maxDurability() == scrapMax,
                "foreign tier not clamped: " + clamped.tier() + " " + clamped.durability() + "/"
                        + clamped.maxDurability());
        helper.succeed();
    }

    /**
     * Una cella da 4.000 diventa lo stack della cella e torna cella da 4.000 con la capacita' della config; uno
     * stack senza dati e' una cella vuota; {@link CellItem#charged()} e' piena alla capacita' della config.
     */
    private static void cellItemCarriesItsCharge(GameTestHelper helper) {
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        CellItem item = ModItems.REDSTONE_CELL.get();
        ItemStack stack = CellItem.fromCell(new EnergyCell(capacity, 4_000));
        helper.assertTrue(stack.getItem() == item, "wrong item for a cell: " + stack.getItem());
        EnergyCell back = item.toCell(stack);
        helper.assertTrue(back.stored() == 4_000 && back.capacity() == capacity,
                "cell changed on round trip: " + back.stored() + "/" + back.capacity());

        EnergyCell empty = item.toCell(new ItemStack(item));
        helper.assertTrue(empty.stored() == 0 && empty.capacity() == capacity,
                "bare cell stack is not empty: " + empty.stored() + "/" + empty.capacity());

        ItemStack charged = CellItem.charged();
        helper.assertTrue(charged.getItem() == item, "charged() is not a cell: " + charged.getItem());
        EnergyCell full = item.toCell(charged);
        helper.assertTrue(full.stored() == capacity && full.capacity() == capacity,
                "charged() is not full: " + full.stored() + "/" + full.capacity());
        helper.succeed();
    }

    /**
     * Un'entita' telaio con l'UUID di un record piazzato, creata un blocco piu' in la' e girata altrove, dopo il primo
     * tick sul server c'e' ancora, con la stessa identita', e sta nel punto e con l'orientamento del record; il suo
     * riassunto e' quello dello stato del record, e un riassunto impostato si rilegge uguale.
     */
    private static void frameEntityKeepsItsIdentityAndSummary(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        int scrapMax = RedstoneExoframes.config().tiers.maxDurability.get(Tier.SCRAP);
        FrameState state = new FrameState(RedstoneExoframes.config());
        state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, scrapMax, scrapMax));
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        UUID frameId = frames.place(level, state, spot, 45f);
        helper.assertTrue(frameId != null, "place returned no frame id");
        ExoframeEntity frame = spawnFrame(level, frameId, spot.add(1.0, 0.0, 0.0), -90f);
        helper.assertTrue(frame.frameId().equals(frameId), "frameId differs from the UUID: " + frame.frameId());
        helper.assertTrue(frame.position().distanceTo(spot) > 0.5 && frame.getYRot() == -90f,
                "entity spawned at the record's spot already: " + frame.position() + " yaw " + frame.getYRot());

        // Il primo tick lo da' il test stesso, subito: le verifiche del riallineamento di altri test, nello stesso tick
        // del server, porterebbero l'entita' al suo punto anche con un primo tick rotto.
        frame.tick();
        helper.assertTrue(!frame.isRemoved(), "entity with a placed record removed itself");
        helper.assertTrue(level.getEntity(frameId) == frame, "entity not found in the level by its frame id");
        helper.assertTrue(frame.frameId().equals(frameId), "frame id changed: " + frame.frameId());
        helper.assertTrue(frame.position().distanceTo(spot) < 1e-6 && frame.getYRot() == 45f,
                "entity not moved to the record's spot: " + frame.position() + " yaw " + frame.getYRot()
                        + ", expected " + spot + " yaw 45");
        helper.assertTrue(frame.summary().equals(SuitSummary.of(state)),
                "summary is not the record's: " + frame.summary());

        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of());
        SuitSummary summary = new SuitSummary(Map.of(
                PieceSlot.TORSO, new PieceLook(Tier.ARMORED, PieceCondition.DAMAGED, painted),
                PieceSlot.LEFT_LEG, new PieceLook(Tier.SCRAP, PieceCondition.BROKEN, Appearance.DEFAULT)));
        frame.setSummary(summary);
        helper.assertTrue(frame.summary().equals(summary), "summary changed when read back: " + frame.summary());
        frame.setSummary(SuitSummary.BARE);
        helper.assertTrue(frame.summary().equals(SuitSummary.BARE), "bare summary not read back");

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!frame.isRemoved() && level.getEntity(frameId) == frame,
                    "entity with a placed record gone after later ticks");
            helper.assertTrue(frame.position().distanceTo(spot) < 1e-6 && frame.getYRot() == 45f,
                    "entity left the record's spot after later ticks: " + frame.position() + " yaw "
                            + frame.getYRot());
            helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
            frame.discard();
            helper.succeed();
        });
    }

    /**
     * L'entita' telaio non prende danni: {@code hurt} e' falso con danno generico, da uccisione generica e da un
     * attacco del giocatore; un'esplosione accanto non la toglie, non la sposta e non le da' spinta.
     */
    private static void frameEntityIgnoresDamageAndExplosions(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
        UUID frameId = frames.place(level, new FrameState(RedstoneExoframes.config()), spot, 0f);
        helper.assertTrue(frameId != null, "place returned no frame id");
        ExoframeEntity frame = spawnFrame(level, frameId, spot, 0f);

        helper.assertTrue(!frame.hurt(level.damageSources().generic(), 1000f), "hurt by generic damage");
        helper.assertTrue(!frame.hurt(level.damageSources().genericKill(), Float.MAX_VALUE), "hurt by a generic kill");
        helper.assertTrue(!frame.hurt(level.damageSources().playerAttack(player), 1000f), "hurt by a player attack");
        // potenza 1 a un blocco: i test condividono il server, e un'esplosione piu' forte arriva alle strutture vicine
        level.explode(null, spot.x + 1.0, spot.y + 1.0, spot.z, 1f, Level.ExplosionInteraction.NONE);
        helper.assertTrue(frame.isAlive() && !frame.isRemoved(), "entity removed by an explosion");
        helper.assertTrue(level.getEntity(frameId) == frame, "entity gone from the level after an explosion");
        helper.assertTrue(frame.position().equals(spot), "explosion moved the entity: " + frame.position());
        helper.assertTrue(frame.getDeltaMovement().equals(Vec3.ZERO),
                "explosion pushed the entity: " + frame.getDeltaMovement());

        helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
        frame.discard();
        helper.succeed();
    }

    /**
     * Il tipo dell'entita' telaio e' registrato con il suo id, nella categoria MISC, immune al fuoco, con raggio di
     * tracciamento 10; l'entita' ha il riquadro della tuta in piedi (0,8 x 2,97), il riquadro di culling allargato di
     * 1 per lato in orizzontale, si clicca, non si spinge e non blocca il movimento.
     */
    private static void frameEntityHasTheSuitBox(GameTestHelper helper) {
        EntityType<ExoframeEntity> type = ModEntities.EXOFRAME.get();
        helper.assertTrue(type == BuiltInRegistries.ENTITY_TYPE.get(Ids.of("exoframe")),
                "entity type not registered as exoframe");
        helper.assertTrue(type.getCategory() == MobCategory.MISC && type.fireImmune()
                && type.clientTrackingRange() == 10, "wrong entity type properties");
        helper.assertTrue(near(type.getWidth(), 0.8f) && near(type.getHeight(), 2.97f),
                "wrong entity type size: " + type.getWidth() + " x " + type.getHeight());

        ExoframeEntity frame = new ExoframeEntity(type, helper.getLevel());
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        frame.setPos(feet.x, feet.y, feet.z);
        helper.assertTrue(near(frame.getBbWidth(), 0.8f) && near(frame.getBbHeight(), 2.97f),
                "wrong entity size: " + frame.getBbWidth() + " x " + frame.getBbHeight());
        helper.assertTrue(near(frame.getBoundingBox(), SuitSize.standingBox(feet)),
                "bounding box is not the suit's: " + frame.getBoundingBox());
        helper.assertTrue(near(frame.getBoundingBoxForCulling(), frame.getBoundingBox().inflate(1.0, 0.0, 1.0)),
                "culling box not widened by 1 on the sides: " + frame.getBoundingBoxForCulling());
        helper.assertTrue(frame.isPickable(), "frame entity not pickable");
        helper.assertTrue(!frame.isPushable() && !frame.isPushedByFluid(), "frame entity pushable");
        helper.assertTrue(!frame.canBeCollidedWith(), "frame entity blocks movement");
        helper.assertTrue(frame.isNoGravity(), "frame entity has gravity");
        helper.assertTrue(frame.summary().equals(SuitSummary.BARE), "new frame entity not bare: " + frame.summary());
        helper.succeed();
    }

    /**
     * {@link FramePlacement#place} su un pavimento libero crea il record piazzato (stato, dimensione, punto,
     * orientamento) e l'entita' con il suo UUID, nel punto, con l'orientamento e con il riassunto dello stato; al
     * primo tick l'entita' trova il suo record e resta.
     */
    private static void placingCreatesRecordAndEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        int scrapMax = RedstoneExoframes.config().tiers.maxDurability.get(Tier.SCRAP);
        FrameState state = new FrameState(RedstoneExoframes.config());
        state.mount(new Piece(PieceSlot.HELMET, Tier.SCRAP, scrapMax, scrapMax));

        Optional<ExoframeEntity> placed = FramePlacement.place(level, state, feet, 30f);
        helper.assertTrue(placed.isPresent(), "place refused on a free floor");
        ExoframeEntity frame = placed.get();
        UUID frameId = frame.frameId();
        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.state() == state && record.dimension().equals(level.dimension())
                && record.position().equals(feet) && record.yaw() == 30f,
                "record differs from the placement: " + record);
        helper.assertTrue(level.getEntity(frameId) == frame, "entity not in the level under the frame id");
        helper.assertTrue(frame.position().equals(feet) && frame.getYRot() == 30f,
                "entity not at the record's spot: " + frame.position() + " yaw " + frame.getYRot());
        helper.assertTrue(frame.summary().equals(SuitSummary.of(state)),
                "summary is not the state's: " + frame.summary());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!frame.isRemoved() && level.getEntity(frameId) == frame,
                    "placed entity removed itself at its first tick");
            helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
            frame.discard();
            helper.succeed();
        });
    }

    /** Con un soffitto a 2 blocchi sopra il punto la tuta in piedi (2,97) non ci sta: niente record, niente entita'. */
    private static void placingRefusedWhereNoRoom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));

        Optional<ExoframeEntity> placed =
                FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), feet, 0f);
        helper.assertTrue(placed.isEmpty(), "place accepted under a ceiling at 2");
        assertNothingPlacedAt(helper, feet);
        helper.succeed();
    }

    /** Dove c'e' gia' un telaio (anche spostato di mezzo blocco) non se ne piazza un altro. */
    private static void placingRefusedWhereAnotherFrameStands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        ExoframeEntity first = FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), feet, 0f)
                .orElse(null);
        helper.assertTrue(first != null, "first place refused");

        Vec3 beside = feet.add(0.5, 0.0, 0.0);
        Optional<ExoframeEntity> second =
                FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), beside, 0f);
        helper.assertTrue(second.isEmpty(), "second frame placed inside the first one");
        helper.assertTrue(Compat.frameState().placedIn(level).stream().noneMatch(p -> p.position().equals(beside)),
                "a record was placed at " + beside);
        helper.assertTrue(level.getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(beside))
                .equals(List.of(first)), "another frame entity stands beside the first one");

        helper.assertTrue(Compat.frameState().removePlaced(server, first.frameId()), "removePlaced refused");
        first.discard();
        helper.succeed();
    }

    /** Un'entita' telaio con un UUID che il registro non conosce sparisce al primo tick. */
    private static void entityWithoutRecordDiscardsItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID frameId = UUID.randomUUID();
        ExoframeEntity frame = spawnFrame(level, frameId, helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)), 0f);
        helper.assertTrue(level.getEntity(frameId) == frame, "entity not added to the level");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(frame.isRemoved() && level.getEntity(frameId) == null,
                    "entity without a record still in the level after its first tick");
            helper.succeed();
        });
    }

    /**
     * Un'entita' telaio il cui record e' piazzato in un'altra dimensione (il Nether del server di test) sparisce al
     * primo tick, e il record resta dov'e'.
     */
    private static void entityOfRecordInAnotherDimensionDiscardsItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether != level, "test server has no separate Nether level");
        FrameStateAccess frames = Compat.frameState();
        Vec3 netherSpot = new Vec3(8.5, 70.0, -3.5);
        UUID frameId = frames.place(nether, new FrameState(RedstoneExoframes.config()), netherSpot, 0f);
        helper.assertTrue(frameId != null, "place returned no frame id");
        ExoframeEntity frame = spawnFrame(level, frameId, helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)), 0f);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(frame.isRemoved() && level.getEntity(frameId) == null,
                    "entity of a record in another dimension still in the level after its first tick");
            PlacedFrame record = frames.placed(server, frameId).orElse(null);
            helper.assertTrue(record != null && record.dimension().equals(Level.NETHER)
                    && record.position().equals(netherSpot), "the record moved or went away: " + record);
            helper.assertTrue(frames.removePlaced(server, frameId), "removePlaced refused");
            helper.succeed();
        });
    }

    /**
     * Un'entita' eliminata con {@code discard()} torna alla seconda verifica consecutiva (la prima la ricorda
     * soltanto): una sola, con lo stesso UUID, nel punto e con l'orientamento del record.
     */
    private static void missingEntityIsRecreated(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        ExoframeEntity frame = FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), feet, -60f)
                .orElse(null);
        helper.assertTrue(frame != null, "place refused");
        UUID frameId = frame.frameId();
        frame.discard();
        helper.assertTrue(level.getEntity(frameId) == null, "discarded entity still in the level");

        FrameRealign.checkNow(level);
        helper.assertTrue(level.getEntity(frameId) == null, "entity recreated at the first check");
        int recreated = FrameRealign.checkNow(level);
        Entity back = level.getEntity(frameId);
        helper.assertTrue(recreated >= 1 && back instanceof ExoframeEntity && back != frame,
                "entity not recreated at the second check (recreated " + recreated + ", found " + back + ")");
        List<ExoframeEntity> copies = level.getEntitiesOfClass(ExoframeEntity.class,
                SuitSize.standingBox(feet).inflate(2.0), entity -> entity.getUUID().equals(frameId));
        helper.assertTrue(copies.size() == 1, copies.size() + " entities with the frame id, expected 1");
        helper.assertTrue(back.position().equals(feet) && back.getYRot() == -60f,
                "recreated entity not at the record's spot: " + back.position() + " yaw " + back.getYRot());

        helper.assertTrue(Compat.frameState().removePlaced(server, frameId), "removePlaced refused");
        back.discard();
        helper.succeed();
    }

    /**
     * Il riallineamento passa dal tick del livello ({@code TickEvent.SERVER_LEVEL_POST}, l'evento a cui e'
     * registrato), non da una chiamata diretta: l'evento sollevato qui {@code 2 * INTERVAL_TICKS} volte di fila (due
     * verifiche periodiche, qualunque fosse il contatore del livello) ricrea l'entita' tolta, nel punto del record.
     * Prima, sollevato una volta sola, non la ricrea.
     */
    private static void realignRunsOnTheLevelTick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        ExoframeEntity frame = FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), feet, 45f)
                .orElse(null);
        helper.assertTrue(frame != null, "place refused");
        UUID frameId = frame.frameId();
        frame.discard();

        TickEvent.SERVER_LEVEL_POST.invoker().tick(level);
        helper.assertTrue(level.getEntity(frameId) == null, "entity recreated after one level tick");
        for (int i = 1; i < 2 * FrameRealign.INTERVAL_TICKS; i++) {
            TickEvent.SERVER_LEVEL_POST.invoker().tick(level);
        }
        Entity back = level.getEntity(frameId);
        helper.assertTrue(back instanceof ExoframeEntity && back.position().equals(feet) && back.getYRot() == 45f,
                "entity not recreated by the periodic check: " + back);
        removeFrame(server, (ExoframeEntity) back);
        helper.succeed();
    }

    /** Un'entita' spostata (come da un {@code /tp}) torna al punto e all'orientamento del record a una verifica. */
    private static void movedEntityIsBroughtBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        ExoframeEntity frame = FramePlacement.place(level, new FrameState(RedstoneExoframes.config()), feet, 15f)
                .orElse(null);
        helper.assertTrue(frame != null, "place refused");
        frame.moveTo(feet.x + 2.0, feet.y + 1.0, feet.z, 100f, 0f);

        FrameRealign.checkNow(level);
        helper.assertTrue(level.getEntity(frame.frameId()) == frame, "the moved entity was replaced");
        helper.assertTrue(frame.position().equals(feet) && frame.getYRot() == 15f,
                "moved entity not brought back: " + frame.position() + " yaw " + frame.getYRot());

        helper.assertTrue(Compat.frameState().removePlaced(server, frame.frameId()), "removePlaced refused");
        frame.discard();
        helper.succeed();
    }

    /**
     * Un record piazzato a 10.000 blocchi, in un chunk senza entita' caricate: le verifiche non creano nulla, nemmeno
     * un'entita' nascosta (il livello la terrebbe in una sezione non visibile, che {@code getEntity} non vede): una
     * sonda con la stessa identita' si aggiunge al livello solo se l'identita' non c'e' ancora, e poi si toglie.
     */
    private static void realignLeavesUnloadedChunksAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        Vec3 far = new Vec3(10_000.5, 64.0, 10_000.5);
        long chunk = ChunkPos.asLong(BlockPos.containing(far));
        helper.assertTrue(!level.areEntitiesLoaded(chunk), "the far chunk has its entities loaded");
        UUID frameId = Compat.frameState().place(level, new FrameState(RedstoneExoframes.config()), far, 0f);
        helper.assertTrue(frameId != null, "place returned no frame id");

        FrameRealign.checkNow(level);
        FrameRealign.checkNow(level);
        helper.assertTrue(level.getEntity(frameId) == null, "entity created in a chunk without loaded entities");
        helper.assertTrue(!level.areEntitiesLoaded(chunk), "the checks loaded the far chunk");
        ExoframeEntity probe = new ExoframeEntity(ModEntities.EXOFRAME.get(), level);
        probe.setUUID(frameId);
        probe.moveTo(far.x, far.y, far.z, 0f, 0f);
        boolean added = level.addFreshEntity(probe);
        probe.discard();
        helper.assertTrue(added, "the level already knows the frame id: a hidden entity was created");
        helper.assertTrue(!level.areEntitiesLoaded(chunk), "the probe loaded the far chunk");

        helper.assertTrue(Compat.frameState().removePlaced(server, frameId), "removePlaced refused");
        helper.succeed();
    }

    /**
     * {@link FramePlacement#groundBelow}: il primo appoggio sotto il punto, con la forma di collisione vera (sopra una
     * lastra a meta' blocco), il punto stesso se ci appoggia gia', il fondo sotto l'acqua (i fluidi non sono suolo);
     * vuoto dove sotto non c'e' nulla fino al fondo del mondo (una colonna senza blocchi caricati).
     */
    private static void groundBelowFindsTheFloor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE_SLAB);
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 5), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 3, 5), Blocks.WATER);

        assertGround(helper, new Vec3(2.5, 6.0, 2.5), new Vec3(2.5, 2.0, 2.5), "above stone");
        assertGround(helper, new Vec3(2.5, 2.0, 2.5), new Vec3(2.5, 2.0, 2.5), "already resting on stone");
        assertGround(helper, new Vec3(5.5, 4.0, 2.5), new Vec3(5.5, 1.5, 2.5), "above a bottom slab");
        assertGround(helper, new Vec3(2.5, 5.0, 5.5), new Vec3(2.5, 2.0, 5.5), "above water");
        Optional<Vec3> none = FramePlacement.groundBelow(level, new Vec3(10_000.5, 100.0, 10_000.5));
        helper.assertTrue(none.isEmpty(), "ground found in a column without blocks: " + none);
        helper.succeed();
    }

    /**
     * L'oggetto telaio usato sulla faccia superiore di un blocco, con il suo {@code useOn}: un record con il giocatore
     * come proprietario e nulla di montato, l'entita' in piedi sul blocco cliccato e rivolta verso il giocatore; in
     * sopravvivenza l'oggetto si consuma, in creativa resta.
     */
    private static void exoframeItemPlacesAnEmptyFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
        player.setYRot(170f);

        player.setGameMode(GameType.SURVIVAL);
        ExoframeEntity survival = useExoframeItem(helper, player, new BlockPos(2, 1, 3), true);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "item not consumed in survival: "
                + player.getMainHandItem());
        PlacedFrame record = Compat.frameState().placed(server, survival.frameId()).orElse(null);
        helper.assertTrue(record != null && player.getUUID().equals(record.state().owner())
                && record.state().pieces().isEmpty() && record.state().cell() == null,
                "record is not an empty frame owned by the player: " + record);
        helper.assertTrue(survival.getYRot() == Mth.wrapDegrees(170f + 180f),
                "frame not facing the player: yaw " + survival.getYRot());

        player.setGameMode(GameType.CREATIVE);
        ExoframeEntity creative = useExoframeItem(helper, player, new BlockPos(5, 1, 3), true);
        helper.assertTrue(player.getMainHandItem().is(ModItems.EXOFRAME.get())
                && player.getMainHandItem().getCount() == 1, "item consumed in creative: " + player.getMainHandItem());

        for (ExoframeEntity frame : List.of(survival, creative)) {
            helper.assertTrue(Compat.frameState().removePlaced(server, frame.frameId()), "removePlaced refused");
            frame.discard();
        }
        helper.succeed();
    }

    /** Sotto un soffitto basso l'oggetto telaio non piazza nulla e resta in mano, intero. */
    private static void exoframeItemRefusedUnderLowCeiling(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        player.setGameMode(GameType.SURVIVAL);
        useExoframeItem(helper, player, new BlockPos(3, 1, 3), false);
        helper.assertTrue(player.getMainHandItem().is(ModItems.EXOFRAME.get())
                && player.getMainHandItem().getCount() == 1, "refused item not left untouched: "
                + player.getMainHandItem());
        assertNothingPlacedAt(helper, helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)));
        helper.succeed();
    }

    /**
     * {@code /exoframe debug place nudo} e {@code rottame}: il telaio al suolo sotto i piedi di chi esegue, con il suo
     * orientamento e lui come proprietario; nudo senza nulla, rottame con i sei pezzi Rottame e una cella carica. Dove
     * c'e' gia' un telaio il comando non piazza nulla.
     */
    private static void debugPlaceBareAndScrap(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);

        Vec3 bareFeet = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.moveTo(bareFeet.x, bareFeet.y, bareFeet.z, 90f, 0f);
        runCommand(dispatcher, player.createCommandSourceStack().withPermission(2).withSuppressedOutput(),
                "exoframe debug place nudo");
        ExoframeEntity bare = frameAt(helper, bareFeet);
        PlacedFrame bareRecord = Compat.frameState().placed(server, bare.frameId()).orElseThrow();
        helper.assertTrue(player.getUUID().equals(bareRecord.state().owner()) && bareRecord.state().pieces().isEmpty()
                && bareRecord.state().cell() == null && bareRecord.yaw() == 90f,
                "nudo did not place an empty frame owned by the executor: " + bareRecord);

        int again = 0;
        try {
            again = dispatcher.execute("exoframe debug place nudo",
                    player.createCommandSourceStack().withPermission(2).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            again = 0;
        }
        helper.assertTrue(again == 0, "a second frame placed where the first one stands");
        helper.assertTrue(level.getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(bareFeet)).size() == 1,
                "more than one frame where the first one stands");

        Vec3 scrapFeet = helper.absoluteVec(new Vec3(5.5, 2.0, 5.5));
        player.moveTo(scrapFeet.x, scrapFeet.y + 2.0, scrapFeet.z, -30f, 0f);
        runCommand(dispatcher, player.createCommandSourceStack().withPermission(2).withSuppressedOutput(),
                "exoframe debug place rottame");
        ExoframeEntity scrap = frameAt(helper, scrapFeet);
        FrameState scrapState = Compat.frameState().placed(server, scrap.frameId()).orElseThrow().state();
        helper.assertTrue(player.getUUID().equals(scrapState.owner()), "rottame not owned by the executor");
        helper.assertTrue(scrapState.pieces().keySet().equals(EnumSet.allOf(PieceSlot.class))
                && scrapState.pieces().values().stream().allMatch(piece -> piece.tier() == Tier.SCRAP),
                "rottame does not have the six scrap pieces: " + scrapState.pieces().keySet());
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        helper.assertTrue(scrapState.cell() != null && scrapState.cell().stored() == capacity,
                "rottame has no charged cell: " + scrapState.cell());

        for (ExoframeEntity frame : List.of(bare, scrap)) {
            helper.assertTrue(Compat.frameState().removePlaced(server, frame.frameId()), "removePlaced refused");
            frame.discard();
        }
        helper.succeed();
    }

    /**
     * Il telaio non usa portali: i controlli che i blocchi portale del Nether e dell'End e il portale di passaggio
     * dell'End fanno sull'entita' nella versione del gioco ({@link GameTests#portalsAccept}) la rifiutano, e accettano
     * un oggetto a terra (controllo). Il viaggio vero non si prova: genererebbe l'altra dimensione.
     */
    private static void frameEntityIgnoresPortals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether != level, "test server has no separate Nether level");
        Vec3 spot = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        ExoframeEntity frame = new ExoframeEntity(ModEntities.EXOFRAME.get(), level);
        frame.setPos(spot.x, spot.y, spot.z);
        ItemEntity control = new ItemEntity(level, spot.x, spot.y, spot.z, new ItemStack(Items.STONE));
        helper.assertTrue(Compat.gameTests().portalsAccept(control, nether), "portals refuse a dropped item");
        helper.assertTrue(!Compat.gameTests().portalsAccept(frame, nether), "portals accept the frame entity");
        helper.succeed();
    }

    /**
     * Montare a mano (design §5.4) su un telaio con yaw 0, che guarda a sud (+Z) e ha la destra verso -X: l'elmo va
     * nel suo slot, il braccio cliccato a -X va sul braccio destro, la cella entra; ogni oggetto lascia la mano (in
     * sopravvivenza), lo stato del record li ha e il riassunto dell'entita' pure. Una seconda cella trova lo slot
     * occupato e resta in mano; la mano secondaria e un oggetto qualsiasi passano senza effetto.
     */
    private static void mountPieceAndCellByHand(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), new Vec3(3.5, 1.0, 3.5), 0f);
        FrameState state = recordState(helper, frame);

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCRAP_HELMET.get()));
        InteractionResult offHand = frame.interactAt(player, new Vec3(0.0, 2.5, 0.0), InteractionHand.OFF_HAND);
        helper.assertTrue(offHand == InteractionResult.PASS && state.pieces().isEmpty()
                && player.getOffhandItem().getCount() == 1, "off hand click acted: " + offHand);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(click(frame, player, 0.0, 1.6, 0.0) == InteractionResult.PASS
                && player.getMainHandItem().is(Items.STICK), "a stick did something to the frame");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_HELMET.get()));
        helper.assertTrue(click(frame, player, 0.0, 2.5, 0.0).consumesAction(), "helmet click not consumed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_ARM.get()));
        helper.assertTrue(click(frame, player, -0.35, 1.6, 0.0).consumesAction(), "arm click not consumed");
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.HELMET, PieceSlot.RIGHT_ARM)),
                "expected helmet and right arm, found " + state.pieces().keySet());
        helper.assertTrue(player.getMainHandItem().isEmpty(), "mounted piece left in hand");

        player.setItemInHand(InteractionHand.MAIN_HAND, CellItem.fromCell(new EnergyCell(capacity, 4_000)));
        helper.assertTrue(click(frame, player, 0.0, 1.6, 0.0).consumesAction(), "cell click not consumed");
        helper.assertTrue(state.cell() != null && state.cell().stored() == 4_000, "cell not inserted: " + state.cell());
        helper.assertTrue(player.getMainHandItem().isEmpty(), "inserted cell left in hand");

        player.setItemInHand(InteractionHand.MAIN_HAND, CellItem.fromCell(new EnergyCell(capacity, 1_000)));
        click(frame, player, 0.0, 1.6, 0.0);
        helper.assertTrue(state.cell().stored() == 4_000, "second cell replaced the first: " + state.cell());
        helper.assertTrue(player.getMainHandItem().is(ModItems.REDSTONE_CELL.get())
                && player.getMainHandItem().getCount() == 1, "second cell taken from the hand");

        helper.assertTrue(frame.summary().equals(SuitSummary.of(state))
                && frame.summary().pieces().keySet().equals(EnumSet.of(PieceSlot.HELMET, PieceSlot.RIGHT_ARM)),
                "entity summary not updated: " + frame.summary());
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Su un telaio con yaw 90 (guarda a ovest, -X, e ha la destra verso -Z) un braccio cliccato a +Z va sul braccio
     * sinistro, un secondo braccio cliccato sullo stesso lato va sul destro, libero; un terzo trova tutto occupato e
     * resta in mano. Una gamba cliccata a -Z va sulla gamba destra.
     */
    private static void secondArmGoesToTheFreeSide(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), new Vec3(3.5, 1.0, 3.5), 90f);
        FrameState state = recordState(helper, frame);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_ARM.get(), 3));
        click(frame, player, 0.0, 1.6, 0.35);
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.LEFT_ARM)),
                "first arm not on the left: " + state.pieces().keySet());
        click(frame, player, 0.0, 1.6, 0.35);
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM)),
                "second arm not on the free right side: " + state.pieces().keySet());
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "hand holds " + player.getMainHandItem());
        click(frame, player, 0.0, 1.6, -0.35);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && state.pieces().size() == 2,
                "third arm taken from the hand with both arms mounted");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_LEG.get()));
        click(frame, player, 0.0, 0.5, -0.2);
        helper.assertTrue(state.pieces().containsKey(PieceSlot.RIGHT_LEG)
                && !state.pieces().containsKey(PieceSlot.LEFT_LEG), "leg clicked at -Z not on the right");
        helper.assertTrue(frame.summary().equals(SuitSummary.of(state)), "entity summary not updated");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Accovacciati a mano vuota: prima torna tutta l'armatura stivata; poi la testa cliccata in alto; in basso a
     * destra la gamba destra non c'e' e viene la sinistra, la piu' vicina della fascia; con le gambe vuote la cella;
     * infine il telaio vuoto torna oggetto e record ed entita' spariscono.
     */
    private static void unmountByHeightThenCellThenPickUp(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        FrameState state = ownedBy(player);
        state.mount(scrapPiece(PieceSlot.HELMET));
        state.mount(scrapPiece(PieceSlot.LEFT_LEG));
        state.insertCell(new EnergyCell(capacity, 2_500));
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        UUID frameId = frames.place(level, state, feet, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)),
                new StowedStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS))));
        helper.assertTrue(frames.exit(player, level, feet, 0f).isPresent(), "exit refused");
        ExoframeEntity frame = spawnFrame(level, frameId, feet, 0f);
        player.setShiftKeyDown(true);

        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 1
                && countInMainInventory(player, Items.IRON_BOOTS) == 1, "stowed armor not given back");
        helper.assertTrue(state.pieces().containsKey(PieceSlot.HELMET)
                && !frames.placed(server, frameId).orElseThrow().hasStowed(), "stowed armor not taken first");

        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(countInMainInventory(player, ModItems.SCRAP_HELMET.get()) == 1
                && !state.pieces().containsKey(PieceSlot.HELMET), "helmet not unmounted from the head band");
        helper.assertTrue(frame.summary().equals(SuitSummary.of(state)), "summary not updated after the helmet");

        click(frame, player, -0.2, 0.5, 0.0);
        helper.assertTrue(countInMainInventory(player, ModItems.SCRAP_LEG.get()) == 1 && state.pieces().isEmpty(),
                "left leg not unmounted by a click on the empty right leg");
        helper.assertTrue(state.cell() != null, "cell taken before the legs");

        click(frame, player, -0.2, 0.5, 0.0);
        helper.assertTrue(state.cell() == null, "cell not removed with the band empty");
        ItemStack cell = findInMainInventory(player, ModItems.REDSTONE_CELL.get());
        helper.assertTrue(ModItems.REDSTONE_CELL.get().toCell(cell).stored() == 2_500, "cell charge changed");
        helper.assertTrue(frames.placed(server, frameId).isPresent() && !frame.isRemoved(), "frame picked up early");

        click(frame, player, 0.0, 1.6, 0.0);
        helper.assertTrue(countInMainInventory(player, ModItems.EXOFRAME.get()) == 1, "empty frame not given back");
        helper.assertTrue(frames.placed(server, frameId).isEmpty(), "record still placed after the pick up");
        helper.assertTrue(frame.isRemoved() && level.getEntity(frameId) == null, "entity still in the level");
        helper.succeed();
    }

    /**
     * Con l'inventario pieno nulla si perde (smontare vuole la mano vuota, quindi un solo oggetto entra sempre nella
     * mano). Con tutti gli altri slot pieni, in creativa: l'armatura stivata riempie la mano e il resto (stivali
     * consumati) cade ai piedi del giocatore, fermo, con il ritardo di raccolta di un oggetto lanciato; il gioco
     * prova a farglielo raccogliere ({@code playerTouch}, che in creativa con l'inventario pieno lo cancellerebbe) e
     * gli stivali restano a terra, uguali. Poi, in sopravvivenza, l'elmo smontato va nella mano con la sua
     * durabilita', e un pezzo dato a un inventario del tutto pieno cade ai piedi con i suoi dati. Qualche tick dopo,
     * con il giocatore di nuovo in creativa che fa il suo tick, entrambi sono ancora a terra.
     */
    private static void fullInventoryOverflowStaysOnTheGround(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 standing = helper.absoluteVec(new Vec3(1.5, 1.0, 3.5));
        player.moveTo(standing.x, standing.y, standing.z, 0f, 0f);
        List<ItemStack> items = player.getInventory().items;
        int selected = player.getInventory().selected;
        for (int i = 0; i < items.size(); i++) {
            if (i != selected) {
                items.set(i, new ItemStack(Items.DIRT, 64));
            }
        }
        FrameState state = ownedBy(player);
        Piece helmet = scrapPiece(PieceSlot.HELMET);
        helmet.damage(10);
        state.mount(helmet);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        UUID frameId = frames.place(level, state, feet, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        boots.setDamageValue(7);
        frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)),
                new StowedStack(EquipmentSlot.FEET, boots.copy())));
        helper.assertTrue(frames.exit(player, level, feet, 0f).isPresent(), "exit refused");
        ExoframeEntity frame = spawnFrame(level, frameId, feet, 0f);
        player.setShiftKeyDown(true);

        player.setGameMode(GameType.CREATIVE);
        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_HELMET), "first stowed stack not in the free hand");
        ItemEntity droppedBoots = assertDroppedAtFeet(helper, player, Items.IRON_BOOTS);
        droppedBoots.playerTouch(player);
        assertStillOnTheGround(helper, droppedBoots, boots, "creative pick up in the tick of the hand-out");

        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(state.pieces().isEmpty(), "helmet still mounted");
        PieceData inHand = Compat.itemData().piece(player.getMainHandItem()).orElse(null);
        helper.assertTrue(player.getMainHandItem().is(ModItems.SCRAP_HELMET.get()) && inHand != null
                && inHand.durability() == helmet.durability(), "unmounted helmet not in the hand: " + inHand);

        ItemStack leg = PieceItem.fromPiece(scrapPiece(PieceSlot.LEFT_LEG), List.of(ModuleType.NIGHT_VISION),
                new Appearance(0x00FF00, Appearance.UNPAINTED, List.of()));
        Handouts.give(player, leg);
        ItemEntity droppedLeg = assertDroppedAtFeet(helper, player, ModItems.SCRAP_LEG.get());
        assertStillOnTheGround(helper, droppedLeg, leg, "leg given to a full inventory");
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == (items.size() - 1) * 64,
                "inventory contents changed");
        removeFrame(server, frame);

        player.setGameMode(GameType.CREATIVE);
        helper.runAfterDelay(5, () -> {
            player.doTick();
            droppedBoots.playerTouch(player);
            droppedLeg.playerTouch(player);
            assertStillOnTheGround(helper, droppedBoots, boots, "boots after 5 ticks beside a creative player");
            assertStillOnTheGround(helper, droppedLeg, leg, "leg after 5 ticks beside a creative player");
            droppedBoots.discard();
            droppedLeg.discard();
            helper.succeed();
        });
    }

    /**
     * Accovacciati, su un telaio con yaw 0 e le due braccia montate: un clic a -X (la destra del telaio) nella fascia
     * di mezzo toglie il braccio destro e lascia il sinistro; un clic sulla testa, fascia vuota con altri pezzi ancora
     * montati, toglie la cella e lascia i pezzi; un altro clic li', senza cella, non fa nulla.
     */
    private static void unmountTakesTheClickedArmThenTheCell(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState state = ownedBy(player);
        state.mount(scrapPiece(PieceSlot.LEFT_ARM));
        state.mount(scrapPiece(PieceSlot.RIGHT_ARM));
        state.mount(scrapPiece(PieceSlot.LEFT_LEG));
        state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 300));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(3.5, 1.0, 3.5), 0f);
        player.setShiftKeyDown(true);

        click(frame, player, -0.4, 1.6, 0.0);
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.LEFT_ARM, PieceSlot.LEFT_LEG)),
                "click on the right side did not take the right arm: " + state.pieces().keySet());
        helper.assertTrue(countInMainInventory(player, ModItems.SCRAP_ARM.get()) == 1, "right arm not given");

        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(state.cell() == null && countInMainInventory(player, ModItems.REDSTONE_CELL.get()) == 1,
                "empty head band did not take the cell");
        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.LEFT_ARM, PieceSlot.LEFT_LEG)),
                "taking the cell changed the pieces: " + state.pieces().keySet());

        click(frame, player, 0.0, 2.5, 0.0);
        helper.assertTrue(state.pieces().size() == 2 && !frame.isRemoved()
                && Compat.frameState().placed(server, frame.frameId()).isPresent(),
                "a click on an empty band with no cell changed the frame");
        helper.assertTrue(countInMainInventory(player, ModItems.SCRAP_ARM.get()) == 1
                && countInMainInventory(player, ModItems.EXOFRAME.get()) == 0, "a click on nothing gave something");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Un telaio con proprietario, bloccato, con la regola di default {@code ownerOnlyFrames}: un altro giocatore (in
     * sopravvivenza, senza permessi) non monta, non smonta e non raccoglie nulla, e non riceve nulla.
     */
    private static void strangerCannotTouchALockedFrame(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        stranger.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(RedstoneExoframes.config().server.ownerOnlyFrames, "ownerOnlyFrames is not the default");
        FrameState state = ownedBy(owner);
        state.mount(scrapPiece(PieceSlot.TORSO));
        state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 100));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(2.5, 1.0, 2.5), 0f);
        ExoframeEntity empty = placeFrame(helper, ownedBy(owner), new Vec3(5.5, 1.0, 5.5), 0f);

        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_HELMET.get()));
        click(frame, stranger, 0.0, 2.5, 0.0);
        helper.assertTrue(stranger.getMainHandItem().getCount() == 1 && !state.pieces().containsKey(PieceSlot.HELMET),
                "stranger mounted a helmet");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        stranger.setShiftKeyDown(true);
        click(frame, stranger, 0.0, 1.6, 0.0);
        click(frame, stranger, 0.0, 0.5, 0.0);
        click(empty, stranger, 0.0, 1.6, 0.0);

        helper.assertTrue(state.pieces().keySet().equals(EnumSet.of(PieceSlot.TORSO)) && state.cell() != null,
                "stranger changed the frame: " + state.pieces().keySet() + " cell " + state.cell());
        helper.assertTrue(Compat.frameState().placed(server, empty.frameId()).isPresent() && !empty.isRemoved(),
                "stranger picked up a locked frame");
        helper.assertTrue(stranger.getInventory().isEmpty(), "stranger was given something");
        removeFrame(server, frame);
        removeFrame(server, empty);
        helper.succeed();
    }

    /** Un telaio sbloccato dal proprietario, e uno senza proprietario, li monta e li smonta chiunque. */
    private static void unlockedFrameIsOpenToAll(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        stranger.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        FrameState unlocked = ownedBy(owner);
        unlocked.setUnlocked(true);
        ExoframeEntity frame = placeFrame(helper, unlocked, new Vec3(2.5, 1.0, 2.5), 0f);
        ExoframeEntity ownerless = placeFrame(helper, new FrameState(RedstoneExoframes.config()),
                new Vec3(5.5, 1.0, 5.5), 0f);

        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_HELMET.get()));
        click(frame, stranger, 0.0, 2.5, 0.0);
        helper.assertTrue(unlocked.pieces().containsKey(PieceSlot.HELMET),
                "stranger could not mount on an unlocked frame");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_TORSO.get()));
        click(ownerless, stranger, 0.0, 1.6, 0.0);
        FrameState ownerlessState = recordState(helper, ownerless);
        helper.assertTrue(ownerlessState.pieces().containsKey(PieceSlot.TORSO),
                "stranger could not mount on a frame without owner");

        stranger.setShiftKeyDown(true);
        click(frame, stranger, 0.0, 2.5, 0.0);
        click(ownerless, stranger, 0.0, 1.6, 0.0);
        helper.assertTrue(unlocked.pieces().isEmpty() && ownerlessState.pieces().isEmpty(),
                "stranger could not unmount");
        helper.assertTrue(countInMainInventory(stranger, ModItems.SCRAP_HELMET.get()) == 1
                && countInMainInventory(stranger, ModItems.SCRAP_TORSO.get()) == 1, "unmounted pieces not given");
        removeFrame(server, frame);
        removeFrame(server, ownerless);
        helper.succeed();
    }

    /**
     * Un braccio consumato, con un modulo e dipinto, montato a mano porta nello slot del telaio durabilita', modulo e
     * aspetto (che il riassunto mostra); smontato torna lo stesso oggetto con gli stessi dati.
     */
    private static void unmountedPieceKeepsDurabilityAndColours(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        Piece worn = scrapPiece(PieceSlot.LEFT_ARM);
        worn.damage(23);
        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of(new Decal("minecraft:creeper", "red")));
        ItemStack original = PieceItem.fromPiece(worn, List.of(ModuleType.NIGHT_VISION), painted);
        PieceData before = Compat.itemData().piece(original).orElseThrow();
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), new Vec3(3.5, 1.0, 3.5), 0f);
        FrameState state = recordState(helper, frame);

        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        click(frame, player, -0.4, 1.6, 0.0);
        Piece mounted = state.pieces().get(PieceSlot.RIGHT_ARM);
        helper.assertTrue(mounted != null && mounted.durability() == worn.durability(),
                "arm not mounted with its durability: " + mounted);
        helper.assertTrue(state.modules().get(PieceSlot.RIGHT_ARM).equals(List.of(ModuleType.NIGHT_VISION)),
                "module not in the slot: " + state.modules());
        helper.assertTrue(state.appearance(PieceSlot.RIGHT_ARM).equals(painted),
                "appearance not in the slot: " + state.appearance(PieceSlot.RIGHT_ARM));
        helper.assertTrue(frame.summary().pieces().get(PieceSlot.RIGHT_ARM).appearance().equals(painted),
                "summary does not show the colours");

        player.setShiftKeyDown(true);
        click(frame, player, -0.4, 1.6, 0.0);
        ItemStack back = findInMainInventory(player, ModItems.SCRAP_ARM.get());
        helper.assertTrue(state.pieces().isEmpty() && state.modules().isEmpty(), "arm or modules left on the frame");
        PieceData after = Compat.itemData().piece(back).orElse(null);
        helper.assertTrue(before.equals(after), "piece data changed: " + before + " -> " + after);
        helper.assertTrue(ItemStack.matches(original, back), "unmounted stack differs from the mounted one");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Ingresso (design §5.5) con un clic a mano vuota, in piedi: l'elmo di ferro indossato finisce nel record con il
     * suo slot, i quattro segnaposto sono negli slot e il torso porta il riassunto dello stato; il record e'
     * indossato e non piu' piazzato, l'entita' e' sparita, il giocatore sta nel punto e con lo yaw del telaio (il
     * pitch resta il suo) e ha subito la grandezza della tuta (0,8 x 2,97). Sul client il clic consuma. Gli slot
     * armatura cambiano in silenzio: nessun evento EQUIP (e quindi nessun suono), mentre un elmo indossato con
     * {@code setItemSlot}, per controllo, ne emette uno.
     */
    private static void enterStowsArmorAndWearsTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 standing = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.doTick(); // dopo il primo tick onEquipItem non tace piu' per firstTick
        player.moveTo(standing.x, standing.y, standing.z, 0f, 20f);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(4.5, 1.0, 4.5), 30f);
        UUID frameId = frame.frameId();
        Vec3 spot = frame.position();

        try (GameTests.EquipEventCounter equips = Compat.gameTests().countEquipEvents(level,
                BlockPos.containing(spot))) {
            player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
            helper.assertTrue(equips.count() == 1, "control: wearing a helmet emitted " + equips.count()
                    + " equip events, expected 1");
            helper.assertTrue(FrameInteractions.clientResult(player, InteractionHand.MAIN_HAND).consumesAction(),
                    "client does not consume the empty hand click");
            helper.assertTrue(click(frame, player, 0.0, 1.6, 0.0).consumesAction(), "enter click not consumed");
            helper.assertTrue(equips.count() == 1, "entering emitted " + (equips.count() - 1) + " equip events");
        }

        helper.assertTrue(frames.placed(server, frameId).isEmpty(), "frame still placed after enter");
        helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(player)), "frame not worn after enter");
        helper.assertTrue(frames.get(player).orElse(null) == state, "worn state is not the placed state");
        assertPlaceholdersWorn(helper, player);
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(Optional.of(SuitSummary.of(state)).equals(written), "wrong summary on the torso: " + written);
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 0, "helmet left in the inventory");
        helper.assertTrue(frame.isRemoved() && level.getEntity(frameId) == null, "frame entity still in the level");
        helper.assertTrue(player.position().distanceTo(spot) < 1e-6 && player.getYRot() == 30f
                && player.getXRot() == 20f, "player not moved into the frame: " + player.position() + " yaw "
                + player.getYRot() + " pitch " + player.getXRot() + ", frame " + spot);
        assertSize(helper, player, "after enter", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f, 1.27f * 1.65f);
        helper.assertTrue(near(player.getBoundingBox(), SuitSize.standingBox(spot)),
                "bounding box not rebuilt after enter: " + player.getBoundingBox());

        assertOneStowedHelmet(helper, frames.takeStowed(player), helmet, "entered frame");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Senza cella, e con una cella scarica, l'ingresso si rifiuta e non tocca nulla: record piazzato, entita',
     * armatura del giocatore, nessun record indossato.
     */
    private static void enterRefusedWithoutAChargedCell(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        FrameState state = ownedBy(player);
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(3.5, 1.0, 3.5), 0f);

        helper.assertTrue(!FrameEntry.enter(player, frame), "entered a frame without a cell");
        assertNotEntered(helper, player, frame, "without a cell");
        EnergyCell empty = new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, 0);
        helper.assertTrue(Compat.frameState().updatePlaced(server, frame.frameId(), s -> s.insertCell(empty)),
                "updatePlaced refused");
        helper.assertTrue(!FrameEntry.enter(player, frame), "entered a frame with an empty cell");
        assertNotEntered(helper, player, frame, "with an empty cell");
        helper.assertTrue(state.cell() != null && state.cell().isEmpty(), "the empty cell changed");
        removeFrame(server, frame);
        helper.succeed();
    }

    /** Con un soffitto che taglia il riquadro della tuta in piedi nel punto del telaio l'ingresso si rifiuta. */
    private static void enterRefusedUnderLowCeiling(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        ExoframeEntity frame = placeFrame(helper, charged(ownedBy(player)), new Vec3(3.5, 1.0, 3.5), 0f);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);

        helper.assertTrue(!FrameEntry.enter(player, frame), "entered a frame under a ceiling at 2");
        assertNotEntered(helper, player, frame, "under a low ceiling");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Due giocatori entrano nello stesso telaio nello stesso tick: il primo e' dentro, il secondo resta com'era (la
     * sua armatura, nessun segnaposto, nessun record indossato, la sua posizione) e il record e' uno solo.
     */
    private static void secondEnterOnSameFrameFails(GameTestHelper helper) {
        ServerPlayer first = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer second = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 standing = helper.absoluteVec(new Vec3(1.5, 1.0, 6.5));
        second.moveTo(standing.x, standing.y, standing.z, 0f, 0f);
        second.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        ExoframeEntity frame = placeFrame(helper, charged(new FrameState(RedstoneExoframes.config())),
                new Vec3(4.5, 1.0, 4.5), 0f);
        UUID frameId = frame.frameId();

        boolean firstIn = FrameEntry.enter(first, frame);
        boolean secondIn = FrameEntry.enter(second, frame);

        helper.assertTrue(firstIn && !secondIn, "expected only the first in, got " + firstIn + " and " + secondIn);
        helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(first)), "first player not inside");
        helper.assertTrue(frames.wornFrameId(second).isEmpty() && frames.get(second).isEmpty(),
                "second player has a worn record");
        helper.assertTrue(frames.placed(server, frameId).isEmpty(), "frame still placed");
        helper.assertTrue(second.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                && countPlaceholders(second) == 0, "second player's armor changed");
        helper.assertTrue(second.position().equals(standing), "second player moved to " + second.position());
        helper.assertTrue(frames.takeStowed(first).isEmpty(), "first player had armor to stow");
        leaveTestFrame(first);
        helper.succeed();
    }

    /**
     * Il proprietario muore nel telaio con l'inventario pieno (con {@code keepInventory}, cosi' resta pieno: senza, e'
     * pieno di nuovo quando ha raccolto le sue cose) e gli stivali di ferro stivati restano nel telaio; poi entra: gli
     * stivali restano nel record (entrare non consegna nulla), e uscendo tornano nel loro slot, interi. In nessun
     * momento c'e' qualcosa a terra, e l'inventario resta quello che era.
     */
    private static void ownerGetsBackArmorLeftInTheFrame(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameRules.BooleanValue keepInventory = level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean before = keepInventory.get();
        keepInventory.set(true, server);
        try {
            ItemStack boots = new ItemStack(Items.IRON_BOOTS);
            boots.setDamageValue(5);
            owner.setItemSlot(EquipmentSlot.FEET, boots.copy());
            UUID frameId = wearFrameAt(helper, owner, charged(ownedBy(owner)), new Vec3(4.5, 2.0, 4.5));
            Vec3 feet = owner.position();
            List<ItemStack> items = owner.getInventory().items;
            for (int i = 0; i < items.size(); i++) {
                items.set(i, new ItemStack(Items.DIRT, 64));
            }

            owner.kill();
            helper.assertTrue(owner.isDeadOrDying(), "owner did not die");
            helper.assertTrue(deathDrops(level, feet).isEmpty(), "something on the ground after the death: "
                    + deathDrops(level, feet));
            helper.assertTrue(owner.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                    && countInMainInventory(owner, Items.IRON_BOOTS) == 0, "a second copy of the boots on the owner");
            owner.setHealth(owner.getMaxHealth()); // di nuovo vivo, come dopo la rinascita

            helper.assertTrue(level.getEntity(frameId) instanceof ExoframeEntity frame
                    && FrameEntry.enter(owner, frame), "owner could not enter the frame left by the death");
            helper.assertTrue(deathDrops(level, feet).isEmpty(), "entering put something on the ground: "
                    + deathDrops(level, feet));
            helper.assertTrue(countInMainInventory(owner, Items.IRON_BOOTS) == 0
                    && countInMainInventory(owner, Items.DIRT) == items.size() * 64, "entering handed armor out");
            assertPlaceholdersWorn(helper, owner);

            helper.assertTrue(FrameEntry.exit(owner), "exit refused");
            ItemStack worn = owner.getItemBySlot(EquipmentSlot.FEET);
            helper.assertTrue(ItemStack.matches(boots, worn) && worn.getDamageValue() == 5,
                    "boots not back in their slot: " + worn);
            helper.assertTrue(countInMainInventory(owner, Items.DIRT) == items.size() * 64
                    && countInMainInventory(owner, Items.IRON_BOOTS) == 0, "inventory changed on the way out");
            helper.assertTrue(deathDrops(level, feet).isEmpty(), "leaving put something on the ground: "
                    + deathDrops(level, feet));
            removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        } finally {
            keepInventory.set(before, server);
        }
        helper.succeed();
    }

    /**
     * Chi non e' il proprietario non entra in un telaio che contiene ancora un'armatura leggibile: su un telaio
     * sbloccato l'ingresso si rifiuta, gli stivali del proprietario restano nel record, nulla e' a terra e nulla e'
     * arrivato a lui. Dopo averli tirati fuori con un clic accovacciato (che li da' a lui, come smontare) entra. Un
     * telaio senza proprietario con un'armatura dentro rifiuta chiunque.
     */
    private static void strangerCannotEnterAFrameHoldingArmor(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        FrameStateAccess frames = Compat.frameState();
        FrameState state = charged(ownedBy(owner));
        state.setUnlocked(true);
        ExoframeEntity frame = frameWithStowedBoots(helper, owner, state, new Vec3(2.5, 1.0, 2.5));
        ExoframeEntity ownerless = frameWithStowedBoots(helper, owner,
                charged(new FrameState(RedstoneExoframes.config())), new Vec3(5.5, 1.0, 5.5));

        helper.assertTrue(!FrameEntry.enter(stranger, frame), "stranger entered a frame holding the owner's armor");
        helper.assertTrue(frames.placed(server, frame.frameId()).orElseThrow().hasStowed() && !frame.isRemoved()
                && frames.wornFrameId(stranger).isEmpty(), "refused entry changed the frame or the stranger");
        helper.assertTrue(countInMainInventory(stranger, Items.IRON_BOOTS) == 0
                && countInMainInventory(owner, Items.IRON_BOOTS) == 0, "boots given to a player");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                SuitSize.standingBox(frame.position()).inflate(4.0), e -> e.getItem().is(Items.IRON_BOOTS)).isEmpty(),
                "boots put on the ground");
        helper.assertTrue(!FrameEntry.enter(stranger, ownerless), "stranger entered an ownerless frame with armor");
        helper.assertTrue(!FrameEntry.enter(owner, ownerless), "player entered an ownerless frame with armor");
        helper.assertTrue(frames.placed(server, ownerless.frameId()).orElseThrow().hasStowed(),
                "refused entry took the armor of the ownerless frame");

        stranger.setShiftKeyDown(true);
        helper.assertTrue(click(frame, stranger, 0.0, 1.6, 0.0).consumesAction(), "take out click not consumed");
        stranger.setShiftKeyDown(false);
        helper.assertTrue(countInMainInventory(stranger, Items.IRON_BOOTS) == 1, "boots not taken out");
        helper.assertTrue(FrameEntry.enter(stranger, frame), "stranger could not enter the emptied frame");
        helper.assertTrue(frames.takeStowed(stranger).isEmpty(), "unexpected armor in the record");
        leaveTestFrame(stranger);
        frames.takeStowedOfPlaced(server, ownerless.frameId());
        removeFrame(server, ownerless);
        helper.succeed();
    }

    /**
     * L'uscita (design, par. 5.5): l'elmo stivato torna in testa con la sua durabilita', in silenzio (nessun evento
     * EQUIP); i segnaposto spariscono dagli slot armatura, dall'inventario principale, dalla mano secondaria e dal
     * cursore; il record e' piazzato ai piedi del giocatore con il suo yaw riportato a [-180, 180), l'entita' c'e', nel
     * punto e con il riassunto dello stato; il giocatore ha subito la grandezza vanilla e fa un passo fuori dal
     * telaio, a un blocco dal suo centro (qui non c'e' pavimento davanti ne' ai lati, e il passo va dietro); nessun
     * messaggio sul telaio altrove.
     */
    private static void exitReturnsArmorAndLeavesTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        player.doTick(); // dopo il primo tick onEquipItem non tace piu' per firstTick
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(4.5, 2.0, 4.5), 30f);
        UUID frameId = frame.frameId();
        helper.assertTrue(FrameEntry.enter(player, frame), "enter refused");
        player.setYRot(200f);
        player.getInventory().items.set(5, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_BOOTS.get()));
        player.containerMenu.setCarried(new ItemStack(ModItems.EXOFRAME_HELMET.get()));
        Vec3 feet = player.position();

        try (GameTests.EquipEventCounter equips = Compat.gameTests().countEquipEvents(level,
                BlockPos.containing(feet))) {
            helper.assertTrue(FrameEntry.exit(player), "exit refused");
            helper.assertTrue(equips.count() == 0, "leaving emitted " + equips.count() + " equip events");
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(ItemStack.matches(helmet, head) && head.getDamageValue() == 5,
                "helmet not back on the head: " + head);
        helper.assertTrue(countPlaceholders(player) == 0
                && !Placeholders.isPlaceholder(player.containerMenu.getCarried()),
                "placeholders left on the player");
        helper.assertTrue(frames.wornFrameId(player).isEmpty() && frames.get(player).isEmpty(),
                "frame still worn after exit");
        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.state() == state && record.position().equals(feet)
                && record.yaw() == -160f && record.dimension().equals(level.dimension()) && !record.hasStowed(),
                "frame not placed at the player's feet: " + record + ", feet " + feet);
        helper.assertTrue(level.getEntity(frameId) instanceof ExoframeEntity view && !view.isRemoved()
                && view.position().equals(feet) && view.getYRot() == -160f
                && view.summary().equals(SuitSummary.of(state)), "frame entity missing or wrong: "
                + level.getEntity(frameId));
        // yaw 200 guarda a nord-est: davanti, a sinistra e a destra non c'e' pavimento, dietro (verso sud) si'
        double yaw = Math.toRadians(200.0);
        Vec3 behind = feet.add(Math.sin(yaw), 0.0, -Math.cos(yaw));
        helper.assertTrue(player.position().distanceTo(behind) < 1e-6 && player.getYRot() == 200f,
                "player did not step out behind: " + player.position() + " yaw " + player.getYRot() + ", expected "
                        + behind);
        assertSize(helper, player, "after exit", 0.6f, 1.8f, 1.62f, 1.27f);
        helper.assertTrue(elsewhereMessages(player) == 0, "told that the frame went elsewhere");
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Uscire con lo slot della testa occupato da un elmo vero (al posto del segnaposto) e l'inventario pieno: l'elmo
     * del giocatore resta in testa, l'elmo stivato e' a terra ai piedi, intero, e ci resta (anche accanto a un
     * giocatore in creativa che fa il suo tick); l'inventario non cambia.
     */
    private static void exitWithFullInventoryDropsArmor(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 2.0, 5.5));
        player.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        player.setGameMode(GameType.CREATIVE);
        FrameState state = charged(ownedBy(player));
        UUID frameId = frames.place(level, state, feet, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        ItemStack stowed = new ItemStack(Items.IRON_HELMET);
        stowed.setDamageValue(5);
        frames.stow(player, List.of(new StowedStack(EquipmentSlot.HEAD, stowed.copy())));
        FrameEntry.dress(player, SuitSummary.of(state));
        player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), new ItemStack(Items.GOLDEN_HELMET));
        List<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            items.set(i, new ItemStack(Items.DIRT, 64));
        }

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET),
                "the player's own helmet left the head: " + player.getItemBySlot(EquipmentSlot.HEAD));
        helper.assertTrue(countPlaceholders(player) == 0, "placeholders left on the player");
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == items.size() * 64, "inventory contents changed");
        ItemEntity drop = assertDroppedAt(helper, feet, Items.IRON_HELMET);
        assertStillOnTheGround(helper, drop, stowed, "stowed helmet given to a full inventory");
        helper.assertTrue(frames.placed(server, frameId).isPresent(), "frame not placed after exit");
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));

        helper.runAfterDelay(5, () -> {
            player.doTick();
            drop.playerTouch(player);
            assertStillOnTheGround(helper, drop, stowed, "after 5 ticks beside a creative player");
            drop.discard();
            helper.succeed();
        });
    }

    /**
     * Uscire a mezz'aria: il record e l'entita' nascono sul suolo sotto il giocatore, con il suo yaw; il giocatore
     * resta dov'e'.
     */
    private static void exitInMidAirPutsTheFrameOnTheGround(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        Vec3 ground = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        Vec3 air = helper.absoluteVec(new Vec3(3.5, 5.0, 3.5));
        FrameState state = charged(ownedBy(player));
        UUID frameId = frames.place(level, state, ground, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        FrameEntry.dress(player, SuitSummary.of(state));
        player.moveTo(air.x, air.y, air.z, 45f, 0f);

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.position().equals(ground) && record.yaw() == 45f,
                "frame not placed on the ground below: " + record + ", ground " + ground);
        helper.assertTrue(level.getEntity(frameId) instanceof ExoframeEntity view && view.position().equals(ground),
                "frame entity not on the ground: " + level.getEntity(frameId));
        helper.assertTrue(player.position().equals(air), "player moved to " + player.position());
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Chi non indossa un telaio e chiede di uscire non cambia nulla: l'elmo resta in testa, il segnaposto che ha
     * nell'inventario resta li', nessun telaio nasce ai suoi piedi.
     */
    private static void exitWithoutAFrameDoesNothing(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
        player.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.getInventory().items.set(3, new ItemStack(ModItems.EXOFRAME_TORSO.get()));

        helper.assertTrue(!FrameEntry.exit(player), "exit without a frame reported true");

        helper.assertTrue(Compat.frameState().wornFrameId(player).isEmpty(), "player has a worn record");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                && player.getInventory().items.get(3).is(ModItems.EXOFRAME_TORSO.get()), "player's inventory changed");
        assertNothingPlacedAt(helper, feet);
        helper.succeed();
    }

    /**
     * Entrare, uscire ed entrare di nuovo: e' sempre lo stesso telaio (stessa identita', stessi pezzi, stessa cella
     * con la stessa carica), e fra le due volte l'armatura del giocatore e' di nuovo su di lui, nei suoi slot.
     */
    private static void enterExitAndEnterAgainKeepTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        player.setItemSlot(EquipmentSlot.FEET, boots.copy());
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        state.mount(scrapPiece(PieceSlot.HELMET));
        EnergyCell cell = state.cell();
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(5.5, 2.0, 2.5), 0f);
        UUID frameId = frame.frameId();

        helper.assertTrue(FrameEntry.enter(player, frame), "first enter refused");
        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        helper.assertTrue(ItemStack.matches(helmet, player.getItemBySlot(EquipmentSlot.HEAD))
                && ItemStack.matches(boots, player.getItemBySlot(EquipmentSlot.FEET))
                && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty(), "own armor not back in its slots");
        helper.assertTrue(countPlaceholders(player) == 0, "placeholders left on the player");
        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.state() == state && !record.hasStowed(),
                "frame not placed again with the same state: " + record);
        helper.assertTrue(level.getEntity(frameId) instanceof ExoframeEntity, "no frame entity after exit");

        helper.assertTrue(FrameEntry.enter(player, (ExoframeEntity) level.getEntity(frameId)), "second enter refused");
        helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(player)), "another frame worn");
        FrameState worn = frames.get(player).orElseThrow();
        helper.assertTrue(worn == state && worn.pieces().keySet().equals(EnumSet.of(PieceSlot.TORSO, PieceSlot.HELMET))
                && worn.cell() == cell && cell.stored() == cell.capacity() / 2,
                "pieces or cell changed: " + worn.pieces().keySet() + ", " + worn.cell());
        assertPlaceholdersWorn(helper, player);
        List<StowedStack> stowed = frames.takeStowed(player);
        helper.assertTrue(stowed.size() == 2, "expected helmet and boots stowed again, got " + stowed);
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * La morte nel telaio (design, par. 5.5), passando dalla morte del gioco ({@code kill}): il record e' piazzato ai
     * piedi del giocatore con lo stesso stato (pezzi, cella con la sua carica) e con l'elmo stivato dentro, intero;
     * l'entita' c'e', nel punto; nessun segnaposto resta al giocatore (slot, inventario, cursore) ne' e' a terra,
     * mentre la terra dell'inventario cade come sempre (le cadute vengono dopo l'evento). Prima, un giocatore senza
     * vita che chiede di uscire (il pacchetto del tasto arrivato dopo la morte) non cambia nulla: la morte la gestisce
     * solo l'evento.
     */
    private static void deathLeavesTheFrameWithEverythingInside(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        state.mount(scrapPiece(PieceSlot.HELMET));
        EnergyCell cell = state.cell();
        UUID frameId = wearFrameAt(helper, player, state, new Vec3(3.5, 2.0, 3.5));
        player.getInventory().items.set(4, new ItemStack(Items.DIRT, 3));
        player.getInventory().items.set(5, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        player.containerMenu.setCarried(new ItemStack(ModItems.EXOFRAME_HELMET.get()));
        Vec3 feet = player.position();

        float health = player.getHealth();
        player.setHealth(0f);
        helper.assertTrue(!FrameEntry.exit(player), "a player without health left the frame");
        helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(player)) && countPlaceholders(player) == 5
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EXOFRAME_HELMET.get())
                && frames.placed(server, frameId).isEmpty(), "exit of a player without health changed something");
        player.setHealth(health);

        player.kill();

        helper.assertTrue(player.isDeadOrDying(), "player did not die");
        helper.assertTrue(frames.wornFrameId(player).isEmpty() && frames.get(player).isEmpty(),
                "frame still worn after the death");
        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.state() == state && record.position().equals(feet)
                && record.dimension().equals(level.dimension()) && record.hasStowed()
                && state.pieces().keySet().equals(EnumSet.of(PieceSlot.TORSO, PieceSlot.HELMET))
                && state.cell() == cell && cell.stored() == cell.capacity() / 2,
                "frame not left where the player died, with everything inside: " + record + ", feet " + feet);
        Entity view = level.getEntity(frameId);
        helper.assertTrue(view instanceof ExoframeEntity && !view.isRemoved() && view.position().equals(feet),
                "frame entity missing or not at the feet: " + view);
        helper.assertTrue(countPlaceholders(player) == 0
                && !Placeholders.isPlaceholder(player.containerMenu.getCarried()),
                "placeholders left on the dead player");
        List<ItemEntity> drops = deathDrops(level, feet);
        helper.assertTrue(drops.stream().noneMatch(drop -> Placeholders.isPlaceholder(drop.getItem())),
                "a placeholder was dropped: " + drops);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.DIRT)),
                "the inventory did not drop (dirt not on the ground): " + drops);
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.IRON_HELMET)),
                "a second copy of the stowed helmet was dropped: " + drops);
        assertOneStowedHelmet(helper, frames.takeStowedOfPlaced(server, frameId), helmet, "frame left by the death");
        drops.forEach(Entity::discard);
        removeFrame(server, (ExoframeEntity) view);
        helper.succeed();
    }

    /**
     * La posizione sicura si ricorda per chi indossa il telaio ed e' a terra (qui un blocco accanto a quello dove e'
     * entrato), non in aria; morendo sotto il limite inferiore del mondo il telaio va li', con la sua entita', e non
     * nel punto di morte.
     */
    private static void deathInTheVoidUsesTheLastSafeSpot(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(5.5, 2.0, 2.5));
        Vec3 safe = rememberSafeSpot(helper, player, new Vec3(4.5, 2.0, 2.5));
        Vec3 below = new Vec3(safe.x, level.getMinBuildHeight() - 70.0, safe.z);
        player.setOnGround(false);
        player.moveTo(below.x, below.y, below.z, 0f, 0f);
        player.tickCount = 2 * SafeSpots.INTERVAL_TICKS;
        SafeSpots.tick(player);
        helper.assertTrue(SafeSpots.last(player).equals(Optional.of(new SafeSpots.Spot(level.dimension(), safe))),
                "safe spot changed while in the air: " + SafeSpots.last(player));

        player.kill();

        helper.assertTrue(player.isDeadOrDying(), "player did not die");
        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.position().equals(safe)
                && record.dimension().equals(level.dimension()), "frame not at the last safe spot: " + record
                + ", safe " + safe);
        Entity view = level.getEntity(frameId);
        helper.assertTrue(view instanceof ExoframeEntity && view.position().equals(safe),
                "frame entity not at the last safe spot: " + view);
        helper.assertTrue(countPlaceholders(player) == 0, "placeholders left on the dead player");
        deathDrops(level, below).forEach(Entity::discard);
        removeFrame(server, (ExoframeEntity) view);
        helper.succeed();
    }

    /**
     * Con {@code keepInventory} la morte lascia il telaio come sempre: nessun segnaposto resta nell'inventario che
     * passa al giocatore rinato (lo copia {@code restoreFrom}), il resto dell'inventario resta e nulla cade. La regola
     * torna com'era.
     */
    private static void deathWithKeepInventoryStillLeavesTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        GameRules.BooleanValue keepInventory = level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean before = keepInventory.get();
        keepInventory.set(true, server);
        try {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(2.5, 2.0, 6.5));
            player.getInventory().items.set(4, new ItemStack(Items.DIRT, 3));
            player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_BOOTS.get()));
            Vec3 feet = player.position();

            player.kill();

            helper.assertTrue(player.isDeadOrDying(), "player did not die");
            helper.assertTrue(countPlaceholders(player) == 0, "placeholders kept in the inventory");
            helper.assertTrue(countInMainInventory(player, Items.DIRT) == 3, "the kept inventory changed");
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                    && countInMainInventory(player, Items.IRON_BOOTS) == 0,
                    "a second copy of the stowed boots kept by the player");
            PlacedFrame record = frames.placed(server, frameId).orElse(null);
            helper.assertTrue(record != null && record.position().equals(feet) && record.hasStowed(),
                    "frame not left at the feet with the boots inside: " + record);
            Entity view = level.getEntity(frameId);
            helper.assertTrue(view instanceof ExoframeEntity && view.position().equals(feet),
                    "frame entity missing: " + view);
            List<ItemEntity> drops = deathDrops(level, feet);
            helper.assertTrue(drops.isEmpty(), "items dropped with keepInventory: " + drops);
            List<StowedStack> stowed = frames.takeStowedOfPlaced(server, frameId);
            helper.assertTrue(stowed.size() == 1 && stowed.get(0).stack().is(Items.IRON_BOOTS),
                    "expected the boots in the frame, got " + stowed);
            removeFrame(server, (ExoframeEntity) view);
        } finally {
            keepInventory.set(before, server);
        }
        helper.succeed();
    }

    /**
     * Uscire volontariamente dove sotto non c'e' suolo (sotto il limite inferiore del mondo): il telaio va all'ultima
     * posizione sicura (un blocco accanto a quello dove il giocatore e' entrato), con la sua entita'; il giocatore
     * resta dov'e' e un messaggio gli dice dove e' il telaio.
     */
    private static void exitWithoutGroundUsesTheLastSafeSpot(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(6.5, 2.0, 5.5));
        Vec3 safe = rememberSafeSpot(helper, player, new Vec3(5.5, 2.0, 5.5));
        Vec3 below = new Vec3(safe.x, level.getMinBuildHeight() - 10.0, safe.z);
        player.setOnGround(false);
        player.moveTo(below.x, below.y, below.z, 0f, 0f);

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        PlacedFrame record = frames.placed(server, frameId).orElse(null);
        helper.assertTrue(record != null && record.position().equals(safe)
                && record.dimension().equals(level.dimension()), "frame not at the last safe spot: " + record
                + ", safe " + safe);
        Entity view = level.getEntity(frameId);
        helper.assertTrue(view instanceof ExoframeEntity && view.position().equals(safe),
                "frame entity not at the last safe spot: " + view);
        helper.assertTrue(player.position().equals(below) && countPlaceholders(player) == 0,
                "player moved or kept placeholders: " + player.position());
        helper.assertTrue(elsewhereMessages(player) == 1, "player not told once where the frame went: "
                + Compat.gameTests().messagesTo(player));
        removeFrame(server, (ExoframeEntity) view);
        helper.succeed();
    }

    /**
     * Un segnaposto tolto dal suo slot (sul cursore, come dopo un clic nello slot) torna nello slot al tick, e la
     * copia sul cursore sparisce; senza il segnaposto del torso il giocatore ha la grandezza vanilla, e il tick che lo
     * rimette gli rida' subito quella della tuta, senza aspettare il tick della grandezza.
     */
    private static void slotGuardRestoresARemovedPlaceholder(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), ItemStack.EMPTY);
        player.containerMenu.setCarried(new ItemStack(ModItems.EXOFRAME_HELMET.get()));
        player.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), ItemStack.EMPTY);
        player.refreshDimensions();
        assertSize(helper, player, "torso taken away", 0.6f, 1.8f, 1.62f, 1.27f);

        SlotGuard.tick(player);

        assertPlaceholdersWorn(helper, player);
        helper.assertTrue(countPlaceholders(player) == 4 && player.containerMenu.getCarried().isEmpty(),
                "placeholders not back in their slots only: " + countPlaceholders(player) + ", cursor "
                        + player.containerMenu.getCarried());
        helper.assertTrue(Optional.of(SuitSummary.BARE).equals(
                Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST))), "torso without summary");
        assertSize(helper, player, "after the guard's tick", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f, 1.27f * 1.65f);
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Copie dei segnaposto nell'inventario principale, nella mano secondaria e sul cursore di chi indossa il telaio:
     * il tick le elimina e lascia i quattro negli slot armatura, gli stessi stack (non riscritti).
     */
    private static void slotGuardDeletesStrayCopies(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        SlotGuard.tick(player);
        List<ItemStack> worn = List.copyOf(player.getInventory().armor);
        player.getInventory().items.set(5, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        player.getInventory().items.set(7, new ItemStack(Items.DIRT, 2));
        player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        player.containerMenu.setCarried(new ItemStack(ModItems.EXOFRAME_BOOTS.get()));

        SlotGuard.tick(player);

        helper.assertTrue(countPlaceholders(player) == 4 && player.containerMenu.getCarried().isEmpty(),
                countPlaceholders(player) + " placeholders left, cursor " + player.containerMenu.getCarried());
        for (int i = 0; i < worn.size(); i++) {
            helper.assertTrue(player.getInventory().armor.get(i) == worn.get(i), "armor slot " + i + " rewritten");
        }
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == 2, "a real item was touched");
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Un elmo di ferro messo nello slot della testa di chi indossa il telaio: al tick torna nell'inventario, intero, e
     * nello slot c'e' di nuovo il segnaposto.
     */
    private static void slotGuardMovesForeignArmorOut(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), helmet.copy());

        SlotGuard.tick(player);

        assertPlaceholdersWorn(helper, player);
        ItemStack back = findInMainInventory(player, Items.IRON_HELMET);
        helper.assertTrue(ItemStack.matches(helmet, back) && countInMainInventory(player, Items.IRON_HELMET) == 1,
                "iron helmet not back in the inventory: " + back);
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Segnaposto senza un record indossato (come dopo un rientro, o da {@code /give}): il tick li elimina tutti, dagli
     * slot armatura, dall'inventario, dalla mano secondaria e dal cursore, e il giocatore torna subito alla grandezza
     * vanilla. Un oggetto vero resta.
     */
    private static void placeholdersWithoutStateAreRemoved(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            player.setItemSlot(slot, placeholderFor(slot));
        }
        player.getInventory().items.set(2, new ItemStack(ModItems.EXOFRAME_HELMET.get()));
        player.getInventory().items.set(3, new ItemStack(Items.DIRT, 4));
        player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        player.containerMenu.setCarried(new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        SuitSize.tick(player);
        assertSize(helper, player, "with a torso placeholder", 0.8f, 1.8f * 1.65f, 1.62f * 1.65f, 1.27f * 1.65f);
        player.tickCount = SlotGuard.FULL_SCAN_TICKS; // il tick che guarda tutto l'inventario

        SlotGuard.tick(player);

        helper.assertTrue(countPlaceholders(player) == 0 && player.containerMenu.getCarried().isEmpty(),
                countPlaceholders(player) + " placeholders left, cursor " + player.containerMenu.getCarried());
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == 4, "a real item was touched");
        assertSize(helper, player, "after the guard's tick", 0.6f, 1.8f, 1.62f, 1.27f);
        helper.succeed();
    }

    /**
     * Un record indossato e nessun segnaposto, come dopo un rientro nel gioco: un tick del giocatore, che passa
     * dall'evento del gioco a cui l'autoriparazione e' registrata ({@code TickEvent.PLAYER_POST}), rimette i quattro
     * segnaposto con il riassunto dello stato sul torso; il tick dopo non riscrive nulla.
     */
    private static void rejoinRestoresPlaceholders(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        frames.update(player, state -> {
            state.mount(scrapPiece(PieceSlot.TORSO));
            state.mount(scrapPiece(PieceSlot.LEFT_ARM));
            state.setAppearance(PieceSlot.TORSO, new Appearance(0x112233, Appearance.UNPAINTED, List.of()));
        });
        SuitSummary expected = SuitSummary.of(frames.get(player).orElseThrow());

        TickEvent.PLAYER_POST.invoker().tick(player);

        assertPlaceholdersWorn(helper, player);
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(Optional.of(expected).equals(written), "wrong summary after the tick: " + written);
        helper.assertTrue(countPlaceholders(player) == 4, countPlaceholders(player) + " placeholders, expected 4");
        List<ItemStack> worn = List.copyOf(player.getInventory().armor);
        SlotGuard.tick(player);
        for (int i = 0; i < worn.size(); i++) {
            helper.assertTrue(player.getInventory().armor.get(i) == worn.get(i), "armor slot " + i + " rewritten");
        }
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Un pezzo si rompe nello stato del telaio indossato: al tick dopo il riassunto sul torso lo dice.
     */
    private static void summaryFollowsTheState(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            pieces.put(slot, new PieceLook(Tier.SCRAP, PieceCondition.INTACT, Appearance.DEFAULT));
        }
        DebugWear.wear(player, new SuitSummary(pieces));
        SlotGuard.tick(player);

        frames.mutate(player, state -> {
            Piece helmet = state.pieces().get(PieceSlot.HELMET);
            helmet.damage(helmet.durability());
        });
        SlotGuard.tick(player);

        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(written.isPresent()
                && written.get().pieces().get(PieceSlot.HELMET).condition() == PieceCondition.BROKEN
                && written.get().pieces().get(PieceSlot.TORSO).condition() == PieceCondition.INTACT,
                "summary on the torso does not follow the state: " + written);
        assertPlaceholdersWorn(helper, player);
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Segnaposto nella griglia 2x2 di fabbricazione e in una cassa aperta (spostati li' con un clic accovacciato):
     * il tick li elimina mentre la cassa e' ancora aperta; i quattro negli slot armatura restano, e cosi' gli oggetti
     * veri.
     */
    private static void slotGuardDeletesPlaceholdersInGridAndContainer(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        player.inventoryMenu.getCraftSlots().setItem(1, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(4, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        chest.setItem(5, new ItemStack(Items.DIRT, 3));
        player.containerMenu = ChestMenu.threeRows(1, player.getInventory(), chest);
        try {
            SlotGuard.tick(player);

            helper.assertTrue(player.inventoryMenu.getCraftSlots().getItem(1).isEmpty(),
                    "placeholder left in the crafting grid");
            helper.assertTrue(chest.getItem(4).isEmpty() && chest.getItem(5).is(Items.DIRT)
                    && chest.getItem(5).getCount() == 3, "chest contents wrong: " + chest);
            assertPlaceholdersWorn(helper, player);
            helper.assertTrue(countPlaceholders(player) == 4, countPlaceholders(player) + " placeholders");
        } finally {
            player.containerMenu = player.inventoryMenu;
        }
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Un segnaposto lanciato (qui tolto dallo slot della testa e poi lanciato, come con il tasto Q sullo slot): nessun
     * oggetto a terra porta un segnaposto e nessuna copia torna al giocatore; il tick rimette quello dello slot.
     */
    private static void thrownPlaceholderIsNotDropped(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        DebugWear.wear(player, SuitSummary.BARE);
        ItemStack thrown = player.getInventory().armor.get(EquipmentSlot.HEAD.getIndex());
        player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), ItemStack.EMPTY);

        player.drop(thrown, true);

        List<ItemEntity> near = deathDrops(level, player.position());
        helper.assertTrue(near.stream().noneMatch(drop -> Placeholders.isPlaceholder(drop.getItem())),
                "a thrown placeholder is on the ground: " + near);
        helper.assertTrue(countPlaceholders(player) == 3 && player.containerMenu.getCarried().isEmpty(),
                "a copy of the thrown placeholder came back: " + countPlaceholders(player));
        SlotGuard.tick(player);
        assertPlaceholdersWorn(helper, player);
        helper.assertTrue(countPlaceholders(player) == 4, countPlaceholders(player) + " placeholders after the tick");
        near.forEach(Entity::discard);
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * {@code /exoframe debug wear via} su chi e' entrato davvero in un telaio con un elmo di ferro: l'elmo torna in
     * testa, il record indossato non c'e' piu' e nulla resta fra le voci dei telai tolti; nessun segnaposto.
     */
    private static void debugWearViaGivesBackStowedArmor(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 5.5));
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EXOFRAME_HELMET.get()),
                "helmet not stowed on entering");
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();

        runCommand(server.getCommands().getDispatcher(), source, "exoframe debug wear via");

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(ItemStack.matches(helmet, head), "helmet not back on the head: " + head);
        helper.assertTrue(Compat.frameState().wornFrameId(player).isEmpty()
                && Compat.frameState().placed(server, frameId).isEmpty(), "frame still in the registry");
        helper.assertTrue(Compat.gameTests().orphanedStowed(server, frameId).isEmpty(),
                "via left orphaned entries: " + Compat.gameTests().orphanedStowed(server, frameId));
        helper.assertTrue(countPlaceholders(player) == 0, "placeholders left after via");
        helper.succeed();
    }

    /** Entrando nel telaio il suo punto diventa la prima posizione sicura del giocatore, prima di ogni tick. */
    private static void enteringRemembersTheFrameSpotAsSafe(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        Vec3 spot = helper.absoluteVec(new Vec3(1.5, 2.0, 6.5));
        wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(1.5, 2.0, 6.5));
        helper.assertTrue(SafeSpots.last(player).equals(Optional.of(new SafeSpots.Spot(level.dimension(), spot))),
                "frame spot not remembered as safe: " + SafeSpots.last(player) + ", expected " + spot);
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Il pavimento sotto l'ultima posizione sicura e' stato tolto (il chunk e' caricato): uscendo sopra il vuoto il
     * telaio va sul suolo che c'e' ora sotto quella posizione, non in aria.
     */
    private static void safeSpotFollowsTheFloorDown(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(6.5, 2.0, 2.5));
        Vec3 safe = rememberSafeSpot(helper, player, new Vec3(5.5, 2.0, 2.5));
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(5, 0, 2), Blocks.STONE);
        player.setOnGround(false);
        player.moveTo(safe.x, level.getMinBuildHeight() - 10.0, safe.z, 0f, 0f);

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        PlacedFrame record = Compat.frameState().placed(server, frameId).orElse(null);
        Vec3 floor = safe.add(0.0, -1.0, 0.0);
        helper.assertTrue(record != null && record.position().distanceTo(floor) < 1e-6,
                "frame not on the floor below the safe spot: " + record + ", expected " + floor);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Un segnaposto nella griglia 2x2 di fabbricazione di chi muore nel telaio: la morte lo toglie, e quando il gioco
     * toglie il giocatore morto (al respawn, {@code removePlayerImmediately}: la griglia si svuota a terra) non cade
     * nulla e il segnaposto non e' in nessun inventario.
     */
    private static void deathClearsPlaceholdersFromTheCraftingGrid(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(4.5, 2.0, 1.5));
        player.inventoryMenu.getCraftSlots().setItem(2, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        Vec3 feet = player.position();

        player.kill();

        helper.assertTrue(player.isDeadOrDying(), "player did not die");
        helper.assertTrue(player.inventoryMenu.getCraftSlots().getItem(2).isEmpty(),
                "placeholder still in the crafting grid after the death");
        level.removePlayerImmediately(player, Entity.RemovalReason.KILLED);
        List<ItemEntity> drops = deathDrops(level, feet);
        helper.assertTrue(drops.stream().noneMatch(drop -> Placeholders.isPlaceholder(drop.getItem())),
                "a placeholder was dropped: " + drops);
        helper.assertTrue(countPlaceholders(player) == 0 && !Placeholders.isPlaceholder(
                player.containerMenu.getCarried()), "placeholders left on the dead player");
        drops.forEach(Entity::discard);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il proprietario, senza permessi da operatore, guarda il suo telaio piazzato: {@code /exoframe unlock} lo
     * sblocca e un estraneo in sopravvivenza ci monta un elmo; {@code /exoframe lock} lo blocca (due volte: anche gia'
     * bloccato riesce) e l'estraneo non lo smonta piu'.
     */
    private static void ownerUnlocksAndStrangerCanUse(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        stranger.setGameMode(GameType.SURVIVAL);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity frame = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 2.5), 0f);
        FrameState state = recordState(helper, frame);
        standFacingNorth(helper, owner, new Vec3(2.5, 1.0, 5.5));

        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1, "owner could not unlock");
        helper.assertTrue(state.unlocked(), "frame not unlocked");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_HELMET.get()));
        click(frame, stranger, 0.0, 2.5, 0.0);
        helper.assertTrue(state.pieces().containsKey(PieceSlot.HELMET),
                "stranger could not mount on the unlocked frame");

        helper.assertTrue(exoframeCommand(helper, owner, 0, "lock") == 1, "owner could not lock");
        helper.assertTrue(exoframeCommand(helper, owner, 0, "lock") == 1, "locking a locked frame failed");
        helper.assertTrue(!state.unlocked(), "frame not locked");
        stranger.setShiftKeyDown(true);
        click(frame, stranger, 0.0, 2.5, 0.0);
        helper.assertTrue(state.pieces().containsKey(PieceSlot.HELMET) && stranger.getInventory().isEmpty(),
                "stranger unmounted from the locked frame");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Chi non e' il proprietario, senza permessi da operatore, non sblocca il telaio altrui e nessuno senza quei
     * permessi sblocca un telaio senza proprietario; un operatore sblocca l'uno e l'altro.
     */
    private static void strangerCannotUnlock(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity owned = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 2.5), 0f);
        ExoframeEntity ownerless = placeFrame(helper, new FrameState(RedstoneExoframes.config()),
                new Vec3(5.5, 1.0, 2.5), 0f);

        standFacingNorth(helper, stranger, new Vec3(2.5, 1.0, 5.5));
        helper.assertTrue(exoframeCommand(helper, stranger, 0, "unlock") == 0, "stranger unlocked the frame");
        helper.assertTrue(!recordState(helper, owned).unlocked(), "frame unlocked by a stranger");
        standFacingNorth(helper, owner, new Vec3(5.5, 1.0, 5.5));
        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 0, "player unlocked an ownerless frame");
        helper.assertTrue(!recordState(helper, ownerless).unlocked(), "ownerless frame unlocked");

        helper.assertTrue(exoframeCommand(helper, owner, 2, "unlock") == 1, "operator could not unlock");
        helper.assertTrue(recordState(helper, ownerless).unlocked(), "ownerless frame not unlocked by an operator");
        helper.assertTrue(exoframeCommand(helper, stranger, 2, "unlock") == 1, "operator could not unlock");
        helper.assertTrue(recordState(helper, owned).unlocked(), "frame not unlocked by an operator");
        removeFrame(server, owned);
        removeFrame(server, ownerless);
        helper.succeed();
    }

    /**
     * Dentro il suo telaio il proprietario lo sblocca e lo blocca senza guardare nulla; il telaio piazzato che ha
     * davanti non cambia. {@code /exoframe remove} non tocca mai il telaio indossato.
     */
    private static void lockAndUnlockTheWornFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        FrameStateAccess frames = Compat.frameState();
        ExoframeEntity other = placeFrame(helper, ownedBy(player), new Vec3(2.5, 1.0, 2.5), 0f);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(2.5, 1.0, 5.5));
        player.moveTo(player.getX(), player.getY(), player.getZ(), 180f, 0f);

        helper.assertTrue(exoframeCommand(helper, player, 0, "unlock") == 1, "could not unlock the worn frame");
        helper.assertTrue(frames.get(player).orElseThrow().unlocked(), "worn frame not unlocked");
        helper.assertTrue(!recordState(helper, other).unlocked(), "the placed frame in sight was unlocked");
        helper.assertTrue(exoframeCommand(helper, player, 0, "lock") == 1, "could not lock the worn frame");
        helper.assertTrue(!frames.get(player).orElseThrow().unlocked(), "worn frame not locked");

        helper.assertTrue(exoframeCommand(helper, player, 2, "remove") == 1, "remove did not take the frame in sight");
        helper.assertTrue(frames.wornFrameId(player).equals(Optional.of(frameId)), "remove touched the worn frame");
        helper.assertTrue(frames.placed(server, other.frameId()).isEmpty() && other.isRemoved(),
                "remove did not remove the frame in sight");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Un muro fra il giocatore e il telaio lo nasconde al comando: ne' {@code unlock} ne' {@code remove} lo trovano;
     * tolto il muro, {@code unlock} lo trova.
     */
    private static void wallHidesTheFrameFromTheCommand(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity frame = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 2.5), 0f);
        standFacingNorth(helper, owner, new Vec3(2.5, 1.0, 5.5));
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 4), Blocks.STONE);
        }

        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 0, "unlocked a frame behind a wall");
        helper.assertTrue(exoframeCommand(helper, owner, 2, "remove") == 0, "removed a frame behind a wall");
        helper.assertTrue(!recordState(helper, frame).unlocked() && !frame.isRemoved(), "frame behind a wall changed");

        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 4), Blocks.AIR);
        }
        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1, "could not unlock without the wall");
        helper.assertTrue(recordState(helper, frame).unlocked(), "frame not unlocked without the wall");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * {@code /exoframe remove} sul telaio altrui che l'operatore guarda: record ed entita' spariscono e ai piedi del
     * telaio restano, fermi, il braccio consumato con modulo e colori, l'elmo, la cella con la sua carica e gli
     * stivali stivati; nulla fra l'armatura dei telai tolti.
     */
    private static void removeDropsTheContents(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer operator = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        FrameState state = ownedBy(owner);
        Piece arm = scrapPiece(PieceSlot.LEFT_ARM);
        arm.damage(23);
        Appearance painted = new Appearance(0x123456, 0xABCDEF, List.of(new Decal("minecraft:creeper", "red")));
        state.mount(arm);
        state.restoreModules(PieceSlot.LEFT_ARM, List.of(ModuleType.NIGHT_VISION));
        state.setAppearance(PieceSlot.LEFT_ARM, painted);
        state.mount(scrapPiece(PieceSlot.HELMET));
        state.insertCell(new EnergyCell(capacity, capacity / 3));
        List<ItemStack> expected = List.of(
                PieceItem.fromPiece(arm, List.of(ModuleType.NIGHT_VISION), painted),
                PieceItem.fromPiece(scrapPiece(PieceSlot.HELMET), List.of(), Appearance.DEFAULT),
                CellItem.fromCell(new EnergyCell(capacity, capacity / 3)),
                new ItemStack(Items.IRON_BOOTS));
        ExoframeEntity frame = frameWithStowedBoots(helper, owner, state, new Vec3(2.5, 1.0, 2.5));
        UUID frameId = frame.frameId();
        Vec3 feet = frame.position();
        standFacingNorth(helper, operator, new Vec3(2.5, 1.0, 5.5));

        helper.assertTrue(exoframeCommand(helper, operator, 2, "remove") == 1, "remove failed");

        helper.assertTrue(Compat.frameState().placed(server, frameId).isEmpty() && frame.isRemoved(),
                "record or entity left after remove");
        helper.assertTrue(Compat.gameTests().orphanedStowed(server, frameId).isEmpty(),
                "remove left orphaned entries: " + Compat.gameTests().orphanedStowed(server, frameId));
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(feet, feet).inflate(1.0));
        helper.assertTrue(drops.size() == expected.size(), "dropped " + drops.size() + " stacks, expected "
                + expected.size() + ": " + drops.stream().map(ItemEntity::getItem).toList());
        for (ItemStack want : expected) {
            ItemEntity drop = drops.stream().filter(d -> ItemStack.matches(want, d.getItem())).findFirst()
                    .orElse(null);
            helper.assertTrue(drop != null, "nothing on the ground matches " + want + ": "
                    + drops.stream().map(ItemEntity::getItem).toList());
            helper.assertTrue(drop.position().distanceTo(feet) < 1e-6 && drop.getDeltaMovement().equals(Vec3.ZERO),
                    want + " not lying still at the frame's feet: " + drop.position() + ", feet " + feet);
        }
        drops.forEach(Entity::discard);
        helper.succeed();
    }

    /**
     * Senza livello di permesso 2 {@code debug} e {@code remove} non esistono per la sorgente (errore di sintassi),
     * mentre {@code unlock} si' (senza un telaio non fa nulla); con il livello 2 {@code debug} si esegue.
     */
    private static void debugNeedsOperator(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack player0 = player.createCommandSourceStack().withPermission(0).withSuppressedOutput();
        for (String command : List.of("exoframe debug wear via", "exoframe remove")) {
            boolean unknown;
            try {
                dispatcher.execute(command, player0);
                unknown = false;
            } catch (CommandSyntaxException e) {
                unknown = true;
            }
            helper.assertTrue(unknown, command + " ran without permission level 2");
        }
        standFacingNorth(helper, player, new Vec3(2.5, 1.0, 5.5));
        helper.assertTrue(exoframeCommand(helper, player, 0, "unlock") == 0, "unlock without a frame succeeded");
        runCommand(dispatcher, player.createCommandSourceStack().withPermission(2).withSuppressedOutput(),
                "exoframe debug wear via");
        helper.succeed();
    }

    /**
     * Il registro scritto dal gioco si rilegge (design §4.5), sulle quattro varianti: un telaio piazzato con pezzi,
     * cella e due voci stivate (gli stivali, leggibili, e una voce illeggibile) e il gruppo orfano di un telaio tolto;
     * il salvataggio del gioco ({@code DimensionDataStorage.save}) lo scrive nel file del mondo, e un secondo
     * {@code DimensionDataStorage} sulla stessa cartella lo legge con la factory della mod: uguale a quello in memoria.
     * Poi, su una copia del file in una cartella temporanea (mai sul file del mondo): il file troncato con una copia
     * precedente buona si legge dalla copia; troncati entrambi, il registro resta in sola lettura e il file troncato
     * resta dov'e', intero, con una sua copia messa da parte. Alla fine il test toglie i suoi telai e le sue voci
     * orfane.
     */
    private static void registryFileIsReadBack(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        GameTests tests = Compat.gameTests();
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        state.mount(scrapPiece(PieceSlot.LEFT_ARM));
        ExoframeEntity frame = frameWithStowedBoots(helper, player, state, new Vec3(3.5, 1.0, 3.5));
        UUID frameId = frame.frameId();
        CompoundTag unknown = new CompoundTag();
        unknown.putString("id", RedstoneExoframes.MOD_ID + ":no_such_item");
        unknown.putByte("Count", (byte) 1);
        unknown.putInt("count", 1);
        int current = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        helper.assertTrue(tests.addRawStowed(server, frameId, stowedEntry(current, unknown)), "raw entry not added");
        UUID gone = frames.place(level, new FrameState(RedstoneExoframes.config()),
                helper.absoluteVec(new Vec3(6.5, 1.0, 6.5)), 0f);
        helper.assertTrue(gone != null && tests.addRawStowed(server, gone, stowedEntry(current, unknown))
                && frames.removePlaced(server, gone), "orphaned group not created");

        Path file = tests.saveRegistry(server);
        CompoundTag live = tests.registryContents(server);
        helper.assertTrue(Files.isRegularFile(file), "registry file not written: " + file);
        Optional<CompoundTag> read = tests.readRegistry(server, file.getParent());
        helper.assertTrue(read.isPresent() && read.get().equals(live), "registry read back differs from memory");
        CompoundTag entry = read.get().getCompound(NbtKeys.PLACED).getCompound(frameId.toString());
        helper.assertTrue(entry.getList(NbtKeys.STOWED, Tag.TAG_COMPOUND).size() == 2
                        && entry.getCompound(NbtKeys.STATE).getCompound(NbtKeys.PIECES).size() == 2
                        && entry.getCompound(NbtKeys.STATE).contains(NbtKeys.CELL),
                "the test's frame not read back with pieces, cell and two stowed entries: " + entry);
        helper.assertTrue(read.get().getList(NbtKeys.ORPHANED, Tag.TAG_COMPOUND).stream()
                .anyMatch(group -> gone.equals(NbtUtils.loadUUID(((CompoundTag) group).get(NbtKeys.FRAME)))),
                "orphaned group not read back");

        Path copy = null;
        try {
            copy = Files.createTempDirectory("exoframes-registry-test");
            Path damaged = copy.resolve(file.getFileName());
            // la copia precedente sta accanto con "_old" in coda, come il gioco fa per i file dei giocatori
            Path previous = damaged.resolveSibling(damaged.getFileName() + "_old");
            byte[] whole = Files.readAllBytes(file);
            Files.write(previous, whole);
            Files.write(damaged, Arrays.copyOf(whole, whole.length / 2));
            Optional<CompoundTag> fromPrevious = tests.readRegistry(server, copy);
            helper.assertTrue(fromPrevious.isPresent() && fromPrevious.get().equals(live),
                    "truncated file with a good previous copy not read from the copy");

            byte[] half = Arrays.copyOf(whole, whole.length / 2);
            Files.write(damaged, half);
            Files.write(previous, Arrays.copyOf(whole, whole.length / 3));
            helper.assertTrue(tests.readRegistry(server, copy).isEmpty(),
                    "registry with both files truncated is not read-only");
            helper.assertTrue(Arrays.equals(half, Files.readAllBytes(damaged)), "the unreadable file was changed");
            try (Stream<Path> kept = Files.list(copy)) {
                helper.assertTrue(kept.anyMatch(p -> p.getFileName().toString().startsWith(
                        damaged.getFileName() + ".unreadable-")), "no copy of the unreadable file kept aside");
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not prepare the copy of the registry file", e);
        } finally {
            deleteTree(copy);
        }

        helper.assertTrue(frames.takeStowedOfPlaced(server, frameId).size() == 1, "boots not taken back");
        removeFrame(server, frame);
        tests.forgetOrphaned(server, frameId);
        tests.forgetOrphaned(server, gone);
        helper.assertTrue(tests.orphanedStowed(server, frameId).isEmpty()
                && tests.orphanedStowed(server, gone).isEmpty(), "orphaned entries left by the test");
        helper.succeed();
    }

    /**
     * Un giocatore che lascia il server mentre il registro ha un cambio strutturale non ancora sul disco (qui un telaio
     * piazzato): l'evento dell'uscita ({@code PlayerEvent.PLAYER_QUIT}) salva i giocatori e il registro insieme, il
     * file del giocatore c'e' e il registro sul disco ha il telaio; dopo, nessun cambio in sospeso.
     */
    private static void quitSavesTheRegistryWithThePlayers(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        GameTests tests = Compat.gameTests();
        Path playerFile = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
        helper.assertTrue(!Files.exists(playerFile), "the new player already has a file");
        ExoframeEntity frame = placeFrame(helper, ownedBy(player), new Vec3(2.5, 1.0, 2.5), 0f);
        helper.assertTrue(tests.registryChangeUnsaved(server), "placing a frame is not an unsaved change");

        PlayerEvent.PLAYER_QUIT.invoker().quit(player);

        helper.assertTrue(!tests.registryChangeUnsaved(server), "the registry was not saved when the player left");
        helper.assertTrue(Files.isRegularFile(playerFile), "the player's file was not saved: " + playerFile);
        Optional<CompoundTag> onDisk = tests.readRegistry(server,
                server.getWorldPath(LevelResource.ROOT).resolve("data").normalize());
        helper.assertTrue(onDisk.isPresent() && onDisk.get().getCompound(NbtKeys.PLACED)
                .contains(frame.frameId().toString()), "the placed frame is not in the registry on disk");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Il passo fuori (design, par. 5.5): uscito su un pavimento aperto, il giocatore e' a un blocco davanti al centro
     * del telaio, nella direzione in cui guarda, con il suo yaw e il suo pitch; i suoi occhi non stanno nel riquadro
     * del telaio, e guardando avanti la linea di vista non prende il telaio ma il blocco che ha davanti.
     */
    private static void exitStepsOutOfTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos target = new BlockPos(3, 3, 6);
        helper.setBlock(target, Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        player.moveTo(spot.x, spot.y, spot.z, 0f, 0f); // guarda a sud, +Z, verso il blocco

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        Vec3 front = spot.add(0.0, 0.0, 1.0);
        helper.assertTrue(player.position().distanceTo(front) < 1e-6 && player.getYRot() == 0f
                && player.getXRot() == 0f, "player not one step in front: " + player.position() + ", expected "
                + front);
        ExoframeEntity frame = (ExoframeEntity) level.getEntity(frameId);
        helper.assertTrue(frame != null && frame.position().equals(spot), "frame not left where the player stood");
        Vec3 eyes = player.getEyePosition();
        helper.assertTrue(!frame.getBoundingBox().contains(eyes), "eyes still inside the frame's box: " + eyes);
        Vec3 end = eyes.add(player.getLookAngle().scale(4.5));
        HitResult block = level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                player));
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eyes, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(4.5)).inflate(1.0),
                e -> e instanceof ExoframeEntity, eyes.distanceToSqr(end));
        helper.assertTrue(entity == null, "the frame is still in the line of sight: " + entity);
        helper.assertTrue(block instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(helper.absolutePos(target)), "the block in front cannot be targeted: "
                + block);
        removeFrame(server, frame);
        helper.succeed();
    }

    /** In un pozzo largo un blocco non c'e' un posto accanto al telaio: il giocatore esce e resta dov'e'. */
    private static void exitInAShaftStaysInPlace(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        for (int y = 2; y <= 4; y++) {
            for (BlockPos wall : List.of(new BlockPos(2, y, 3), new BlockPos(4, y, 3), new BlockPos(3, y, 2),
                    new BlockPos(3, y, 4))) {
                helper.setBlock(wall, Blocks.STONE);
            }
        }
        Vec3 spot = player.position();

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        helper.assertTrue(player.position().equals(spot), "player moved out of the shaft to " + player.position());
        helper.assertTrue(Compat.frameState().placed(server, frameId).isPresent(), "frame not placed");
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il posto davanti ha fuoco, quello a sinistra non ha pavimento entro un blocco, quello a destra e' buono: il passo
     * va a destra. Poi, rientrato, con la lava davanti e il posto a destra senza spazio (un muro): il passo va dietro.
     */
    private static void exitStepsAroundFireAndDrops(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        // giocatore in (3.5, 2, 3.5) rivolto a sud (+Z): davanti (3, 2, 4), sinistra (4, 2, 3), destra (2, 2, 3)
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.FIRE);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        player.moveTo(spot.x, spot.y, spot.z, 0f, 10f);

        helper.assertTrue(FrameEntry.exit(player), "exit refused");
        Vec3 right = spot.add(-1.0, 0.0, 0.0);
        helper.assertTrue(player.position().distanceTo(right) < 1e-6 && player.getXRot() == 10f,
                "player did not step to the right: " + player.position() + ", expected " + right);
        ExoframeEntity frame = (ExoframeEntity) level.getEntity(frameId);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.LAVA);
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        player.moveTo(spot.x, spot.y, spot.z, 0f, 10f);
        helper.assertTrue(FrameEntry.enter(player, frame), "second enter refused");
        player.moveTo(spot.x, spot.y, spot.z, 0f, 10f);

        helper.assertTrue(FrameEntry.exit(player), "second exit refused");
        Vec3 behind = spot.add(0.0, 0.0, -1.0);
        helper.assertTrue(player.position().distanceTo(behind) < 1e-6, "player did not step behind: "
                + player.position() + ", expected " + behind);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.AIR);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il bordo del mondo (stretto qui intorno alla struttura del test e poi rimesso com'era): l'oggetto telaio non
     * piazza fuori dal bordo e resta in mano, intero, e {@code FramePlacement.place} rifiuta; chi esce dal telaio fuori
     * dal bordo non lo lascia li' ma all'ultima posizione sicura, dentro, e un messaggio gli dice dove.
     */
    private static void worldBorderKeepsFramesInside(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        WorldBorder border = level.getWorldBorder();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(1.5, 2.0, 1.5));
        Vec3 inside = player.position();
        try {
            // il bordo copre i blocchi da 0 a 3 della struttura; (6, 6) resta fuori
            Vec3 corner = helper.absoluteVec(new Vec3(0.0, 0.0, 0.0));
            border.setCenter(corner.x + 2.0, corner.z + 2.0);
            border.setSize(4.0);
            Vec3 outside = helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
            helper.assertTrue(!border.isWithinBounds(BlockPos.containing(outside))
                    && border.isWithinBounds(BlockPos.containing(inside)), "test border not where expected");

            helper.assertTrue(FramePlacement.place(level, ownedBy(player), outside, 0f).isEmpty(),
                    "a frame was placed outside the world border");
            player.setGameMode(GameType.SURVIVAL);
            useExoframeItem(helper, player, new BlockPos(6, 1, 6), false);
            helper.assertTrue(player.getMainHandItem().is(ModItems.EXOFRAME.get())
                    && player.getMainHandItem().getCount() == 1, "refused item not left untouched");
            assertNothingPlacedAt(helper, outside);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            player.moveTo(outside.x, outside.y, outside.z, 0f, 0f);
            helper.assertTrue(FrameEntry.exit(player), "exit refused");
            PlacedFrame record = Compat.frameState().placed(server, frameId).orElse(null);
            helper.assertTrue(record != null && record.position().equals(inside),
                    "frame not left at the last safe spot inside the border: " + record + ", expected " + inside);
            helper.assertTrue(elsewhereMessages(player) == 1, "player not told where the frame went");
        } finally {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
            player.setGameMode(GameType.CREATIVE);
        }
        Entity view = level.getEntity(frameId);
        if (view instanceof ExoframeEntity frame) {
            removeFrame(server, frame);
        } else {
            Compat.frameState().removePlaced(server, frameId);
        }
        helper.succeed();
    }

    /**
     * L'oggetto telaio usato sul fianco di un blocco: con il suolo tre blocchi sotto il punto richiesto piazza li'; con
     * il suolo quattro blocchi sotto rifiuta come se non ci fosse spazio e resta in mano, intero.
     */
    private static void exoframeItemRefusesADeepDrop(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        player.setGameMode(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(1, 4, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 5, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 0, 5), Blocks.STONE);

        ExoframeEntity placed = useExoframeItemOnSide(helper, player, new BlockPos(1, 4, 2));
        Vec3 threeBelow = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        helper.assertTrue(placed != null && placed.position().equals(threeBelow),
                "frame not placed three blocks down: " + (placed == null ? null : placed.position()));
        helper.assertTrue(useExoframeItemOnSide(helper, player, new BlockPos(4, 5, 5)) == null,
                "frame placed four blocks down");
        helper.assertTrue(player.getMainHandItem().is(ModItems.EXOFRAME.get())
                && player.getMainHandItem().getCount() == 1, "refused item not left untouched");
        assertNothingPlacedAt(helper, helper.absoluteVec(new Vec3(5.5, 1.0, 5.5)));
        player.setGameMode(GameType.CREATIVE);
        removeFrame(server, placed);
        helper.succeed();
    }

    /**
     * Un giocatore morto (un pacchetto arrivato dopo la morte) non monta e non smonta: il pezzo resta in mano, quello
     * montato resta sul telaio, e il clic passa.
     */
    private static void deadPlayerCannotMountOrUnmount(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        player.setGameMode(GameType.SURVIVAL);
        FrameState state = ownedBy(player);
        state.mount(scrapPiece(PieceSlot.HELMET));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(3.5, 1.0, 3.5), 0f);
        float health = player.getHealth();
        player.setHealth(0f);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRAP_TORSO.get()));
            helper.assertTrue(!click(frame, player, 0.0, 1.6, 0.0).consumesAction(), "a dead player's click acted");
            helper.assertTrue(!state.pieces().containsKey(PieceSlot.TORSO)
                    && player.getMainHandItem().is(ModItems.SCRAP_TORSO.get()), "a dead player mounted a piece");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            helper.assertTrue(!click(frame, player, 0.0, 2.5, 0.0).consumesAction(), "a dead player's click acted");
            helper.assertTrue(state.pieces().containsKey(PieceSlot.HELMET)
                    && countInMainInventory(player, ModItems.SCRAP_HELMET.get()) == 0,
                    "a dead player unmounted a piece");
        } finally {
            player.setShiftKeyDown(false);
            player.setHealth(health);
            player.setGameMode(GameType.CREATIVE);
        }
        removeFrame(server, frame);
        helper.succeed();
    }

    /** Uno spettatore che indossa un telaio non ne esce con il tasto: il telaio resta indossato, e nulla nasce. */
    private static void spectatorCannotLeaveWithTheKey(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(5.5, 2.0, 2.5));
        Vec3 spot = player.position();
        player.setGameMode(GameType.SPECTATOR);
        try {
            helper.assertTrue(player.isSpectator(), "the mock player is not a spectator");
            helper.assertTrue(!FrameEntry.exit(player), "a spectator left the frame");
            helper.assertTrue(Optional.of(frameId).equals(frames.wornFrameId(player)), "frame no longer worn");
            assertNothingPlacedAt(helper, spot);
        } finally {
            player.setGameMode(GameType.CREATIVE);
        }
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Un giocatore senza telaio apre un menu che mostra gli slot armatura di chi ne indossa uno (come fa una mod che
     * apre l'inventario altrui) e uno slot di un contenitore con un segnaposto: il tick dell'autoriparazione di chi
     * guarda toglie il segnaposto del contenitore e lascia quelli dell'altro giocatore, gli stessi stack, al loro posto.
     */
    private static void guardLeavesAnotherPlayersPlaceholdersAlone(GameTestHelper helper) {
        ServerPlayer wearer = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer viewer = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(wearer, SuitSummary.BARE);
        List<ItemStack> worn = List.copyOf(wearer.getInventory().armor);
        SimpleContainer box = new SimpleContainer(1);
        box.setItem(0, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 1) {
            {
                for (int i = 0; i < 4; i++) {
                    addSlot(new Slot(wearer.getInventory(), Inventory.INVENTORY_SIZE + i, 0, 0));
                }
                addSlot(new Slot(box, 0, 0, 0));
            }

            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        viewer.containerMenu = menu;
        viewer.tickCount = SlotGuard.FULL_SCAN_TICKS; // il tick guarda tutto, menu aperto compreso
        try {
            SlotGuard.tick(viewer);
        } finally {
            viewer.containerMenu = viewer.inventoryMenu;
        }
        helper.assertTrue(box.getItem(0).isEmpty(), "placeholder left in the container slot of the menu");
        for (int i = 0; i < worn.size(); i++) {
            helper.assertTrue(wearer.getInventory().armor.get(i) == worn.get(i),
                    "the viewer's guard touched the wearer's armor slot " + i);
        }
        assertPlaceholdersWorn(helper, wearer);
        DebugWear.remove(wearer);
        helper.succeed();
    }

    /**
     * Senza un telaio indossato il tick toglie a ogni tick i segnaposto dagli slot armatura e quelli nel resto
     * dell'inventario solo quando il contatore dei tick e' un multiplo di {@link SlotGuard#FULL_SCAN_TICKS}: con 7
     * il segnaposto dello slot della testa sparisce e quello nell'inventario resta, con 20 sparisce anche quello.
     */
    private static void guardScansTheRestOncePerSecondWithoutAFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), placeholderFor(EquipmentSlot.HEAD));
        player.getInventory().items.set(4, new ItemStack(ModItems.EXOFRAME_LEGS.get()));

        player.tickCount = 7;
        SlotGuard.tick(player);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "armor placeholder left");
        helper.assertTrue(player.getInventory().items.get(4).is(ModItems.EXOFRAME_LEGS.get()),
                "the inventory was scanned outside the once-a-second tick");

        player.tickCount = 2 * SlotGuard.FULL_SCAN_TICKS;
        SlotGuard.tick(player);
        helper.assertTrue(countPlaceholders(player) == 0, countPlaceholders(player) + " placeholders left");
        helper.succeed();
    }

    /** Quante volte il giocatore finto ha avuto il messaggio del telaio lasciato altrove ({@code ELSEWHERE}). */
    private static int elsewhereMessages(ServerPlayer player) {
        int count = 0;
        for (Component message : Compat.gameTests().messagesTo(player)) {
            if (message.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals(FrameEntry.ELSEWHERE)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Usa l'oggetto telaio (uno, in mano) sulla faccia est del blocco dato, relativo alla struttura; l'unico telaio
     * piazzato nella colonna del blocco accanto, o null se l'uso e' stato rifiutato.
     */
    private static ExoframeEntity useExoframeItemOnSide(GameTestHelper helper, ServerPlayer player,
            BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXOFRAME.get()));
        BlockPos clicked = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0.5, 0.0, 0.0), Direction.EAST,
                clicked, false);
        InteractionResult result = ModItems.EXOFRAME.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit));
        if (!result.consumesAction()) {
            return null;
        }
        BlockPos column = clicked.east();
        List<ExoframeEntity> found = helper.getLevel().getEntitiesOfClass(ExoframeEntity.class,
                new AABB(column.getX(), column.getY() - 8, column.getZ(), column.getX() + 1, column.getY() + 1,
                        column.getZ() + 1));
        helper.assertTrue(found.size() == 1, found.size() + " frames below " + column + ", expected 1");
        return found.get(0);
    }

    /** Cancella la cartella temporanea di un test con tutto quello che contiene; nulla se e' null. */
    private static void deleteTree(Path folder) {
        if (folder == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not delete " + folder, e);
        }
    }

    /** Il segnaposto dello slot, come lo darebbe {@code /give}: senza riassunto. */
    private static ItemStack placeholderFor(EquipmentSlot slot) {
        return new ItemStack(switch (slot) {
            case HEAD -> ModItems.EXOFRAME_HELMET.get();
            case CHEST -> ModItems.EXOFRAME_TORSO.get();
            case LEGS -> ModItems.EXOFRAME_LEGS.get();
            default -> ModItems.EXOFRAME_BOOTS.get();
        });
    }

    /**
     * Mette il giocatore dentro un telaio con lo stato dato, in piedi su un blocco di pietra posato sotto il punto
     * relativo alla struttura, con un clic come un giocatore vero ({@link FrameEntry#enter}); da' l'identita' del
     * telaio.
     */
    private static UUID wearFrameAt(GameTestHelper helper, ServerPlayer player, FrameState state, Vec3 relativeFeet) {
        helper.setBlock(BlockPos.containing(relativeFeet).below(), Blocks.STONE);
        ExoframeEntity frame = placeFrame(helper, state, relativeFeet, 0f);
        helper.assertTrue(FrameEntry.enter(player, frame), "enter refused");
        return frame.frameId();
    }

    /**
     * Il giocatore, dentro il telaio, cammina fino al punto relativo alla struttura (un blocco di pietra posato sotto,
     * diverso da quello dove e' entrato, che e' gia' la sua posizione sicura) e ci sta a terra; poi fa un tick del
     * giocatore passando dall'evento del gioco ({@code TickEvent.PLAYER_POST}, quello a cui {@link SafeSpots} e'
     * registrato), con il contatore dei tick impostato su un multiplo dell'intervallo: la posizione si ricorda. Da' il
     * punto assoluto.
     */
    private static Vec3 rememberSafeSpot(GameTestHelper helper, ServerPlayer player, Vec3 relativeFeet) {
        helper.setBlock(BlockPos.containing(relativeFeet).below(), Blocks.STONE);
        Vec3 spot = helper.absoluteVec(relativeFeet);
        helper.assertTrue(!SafeSpots.last(player).map(SafeSpots.Spot::position).equals(Optional.of(spot)),
                "the new spot is already the safe spot");
        player.moveTo(spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setOnGround(true);
        player.tickCount = SafeSpots.INTERVAL_TICKS;
        TickEvent.PLAYER_POST.invoker().tick(player);
        helper.assertTrue(SafeSpots.last(player).equals(Optional.of(new SafeSpots.Spot(helper.getLevel().dimension(),
                spot))), "safe spot not remembered: " + SafeSpots.last(player) + ", expected " + spot);
        return spot;
    }

    /** Gli oggetti a terra entro 2,5 blocchi da {@code center}, come li lascia nel suo tick una morte in quel punto. */
    private static List<ItemEntity> deathDrops(ServerLevel level, Vec3 center) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(2.5));
    }

    /**
     * Un telaio piazzato con lo stato dato e un paio di stivali di ferro stivati, come dopo una morte: il giocatore
     * entra dal registro, stiva gli stivali ed esce; poi l'entita' nel punto relativo alla struttura.
     */
    private static ExoframeEntity frameWithStowedBoots(GameTestHelper helper, ServerPlayer player, FrameState state,
            Vec3 relativeFeet) {
        ServerLevel level = helper.getLevel();
        FrameStateAccess frames = Compat.frameState();
        Vec3 feet = helper.absoluteVec(relativeFeet);
        UUID frameId = frames.place(level, state, feet, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        frames.stow(player, List.of(new StowedStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS))));
        helper.assertTrue(frames.exit(player, level, feet, 0f).isPresent(), "exit refused");
        return spawnFrame(level, frameId, feet, 0f);
    }

    /** Nulla e' cambiato dopo un ingresso rifiutato: record piazzato, entita', elmo di ferro, nessun segnaposto. */
    private static void assertNotEntered(GameTestHelper helper, ServerPlayer player, ExoframeEntity frame,
            String when) {
        FrameStateAccess frames = Compat.frameState();
        helper.assertTrue(frames.placed(helper.getLevel().getServer(), frame.frameId()).isPresent()
                && !frame.isRemoved(), when + ": frame no longer placed");
        helper.assertTrue(frames.wornFrameId(player).isEmpty(), when + ": player has a worn record");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                && countPlaceholders(player) == 0, when + ": player's armor changed");
    }

    /**
     * Toglie il record indossato del test e i segnaposto ({@link DebugWear#remove}); l'armatura ancora stivata torna
     * al giocatore.
     */
    private static void leaveTestFrame(ServerPlayer player) {
        DebugWear.remove(player);
    }

    /** Lo stato con una cella carica a meta'. */
    private static FrameState charged(FrameState state) {
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        state.insertCell(new EnergyCell(capacity, capacity / 2));
        return state;
    }

    /**
     * Usa l'oggetto telaio (uno, in mano) sulla faccia superiore del blocco dato con il suo {@code useOn}; se
     * {@code expectPlaced} restituisce l'unico telaio in piedi sul blocco, altrimenti verifica il rifiuto e da' null.
     */
    private static ExoframeEntity useExoframeItem(GameTestHelper helper, ServerPlayer player, BlockPos relative,
            boolean expectPlaced) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXOFRAME.get()));
        BlockPos clicked = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0.0, 0.5, 0.0), Direction.UP, clicked,
                false);
        InteractionResult result = ModItems.EXOFRAME.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit));
        if (!expectPlaced) {
            helper.assertTrue(!result.consumesAction(), "refused use reported " + result);
            return null;
        }
        helper.assertTrue(result.consumesAction(), "use of the exoframe item reported " + result);
        return frameAt(helper, Vec3.atBottomCenterOf(clicked.above()));
    }

    /** L'unico telaio in piedi esattamente in {@code feet} (riquadro della tuta). */
    private static ExoframeEntity frameAt(GameTestHelper helper, Vec3 feet) {
        List<ExoframeEntity> found = helper.getLevel().getEntitiesOfClass(ExoframeEntity.class,
                SuitSize.standingBox(feet).inflate(0.5));
        helper.assertTrue(found.size() == 1, found.size() + " frames near " + feet + ", expected 1");
        ExoframeEntity frame = found.get(0);
        helper.assertTrue(frame.position().equals(feet), "frame at " + frame.position() + ", expected " + feet);
        return frame;
    }

    /** Nessun record piazzato in {@code feet} e nessun telaio nel riquadro della tuta li'. */
    private static void assertNothingPlacedAt(GameTestHelper helper, Vec3 feet) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Compat.frameState().placedIn(level).stream().noneMatch(p -> p.position().equals(feet)),
                "a record was placed at " + feet);
        helper.assertTrue(level.getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(feet)).isEmpty(),
                "a frame entity stands at " + feet);
    }

    /** Verifica il suolo trovato da {@link FramePlacement#groundBelow} partendo da un punto relativo alla struttura. */
    private static void assertGround(GameTestHelper helper, Vec3 from, Vec3 expected, String where) {
        Optional<Vec3> ground = FramePlacement.groundBelow(helper.getLevel(), helper.absoluteVec(from));
        Vec3 want = helper.absoluteVec(expected);
        helper.assertTrue(ground.isPresent() && ground.get().distanceTo(want) < 1e-6,
                where + ": ground " + ground + ", expected " + want);
    }

    /** Crea l'entita' telaio con l'identita' data nel punto e con l'orientamento dati e la aggiunge al livello. */
    private static ExoframeEntity spawnFrame(ServerLevel level, UUID frameId, Vec3 feet, float yaw) {
        ExoframeEntity frame = new ExoframeEntity(ModEntities.EXOFRAME.get(), level);
        frame.setUUID(frameId);
        frame.moveTo(feet.x, feet.y, feet.z, yaw, 0f);
        level.addFreshEntity(frame);
        return frame;
    }

    /** Piazza nel registro un telaio con lo stato dato e ne aggiunge l'entita' nel punto relativo alla struttura. */
    private static ExoframeEntity placeFrame(GameTestHelper helper, FrameState state, Vec3 relativeFeet, float yaw) {
        Vec3 feet = helper.absoluteVec(relativeFeet);
        UUID frameId = Compat.frameState().place(helper.getLevel(), state, feet, yaw);
        helper.assertTrue(frameId != null, "place returned no frame id");
        return spawnFrame(helper.getLevel(), frameId, feet, yaw);
    }

    /** Lo stato vivo del record piazzato del telaio. */
    private static FrameState recordState(GameTestHelper helper, ExoframeEntity frame) {
        PlacedFrame record = Compat.frameState().placed(helper.getLevel().getServer(), frame.frameId()).orElse(null);
        helper.assertTrue(record != null, "frame " + frame.frameId() + " is not placed");
        return record.state();
    }

    /** Toglie il record piazzato del telaio, se c'e' ancora, e l'entita'. */
    private static void removeFrame(MinecraftServer server, ExoframeEntity frame) {
        Compat.frameState().removePlaced(server, frame.frameId());
        frame.discard();
    }

    /**
     * L'unico oggetto del tipo dato a terra vicino al giocatore: uno stack da uno, ai suoi piedi e fermo (non
     * lanciato). Chi chiama lo toglie dal mondo.
     */
    private static ItemEntity assertDroppedAtFeet(GameTestHelper helper, ServerPlayer player, Item item) {
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(2.0), entity -> entity.getItem().is(item));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                dropped.size() + " dropped " + item + ", expected one stack of 1");
        ItemEntity drop = dropped.get(0);
        helper.assertTrue(drop.position().distanceTo(player.position()) < 1e-6
                && drop.getDeltaMovement().equals(Vec3.ZERO), "dropped " + item + " not lying at the feet: "
                + drop.position() + " moving " + drop.getDeltaMovement() + ", feet " + player.position());
        return drop;
    }

    /**
     * L'unico oggetto del tipo dato a terra vicino a {@code feet}: uno stack da uno, fermo (non lanciato), in
     * {@code feet}. Chi chiama lo toglie dal mondo.
     */
    private static ItemEntity assertDroppedAt(GameTestHelper helper, Vec3 feet, Item item) {
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(feet, feet).inflate(2.0), entity -> entity.getItem().is(item));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                dropped.size() + " dropped " + item + ", expected one stack of 1");
        ItemEntity drop = dropped.get(0);
        helper.assertTrue(drop.position().distanceTo(feet) < 1e-6 && drop.getDeltaMovement().equals(Vec3.ZERO),
                "dropped " + item + " not lying at " + feet + ": " + drop.position() + " moving "
                        + drop.getDeltaMovement());
        return drop;
    }

    /** L'oggetto a terra c'e' ancora, con uno stack uguale a quello atteso (dati compresi). */
    private static void assertStillOnTheGround(GameTestHelper helper, ItemEntity drop, ItemStack expected,
            String when) {
        helper.assertTrue(!drop.isRemoved() && helper.getLevel().getEntity(drop.getUUID()) == drop,
                "dropped " + expected + " gone (" + when + ")");
        helper.assertTrue(ItemStack.matches(expected, drop.getItem()),
                "dropped stack changed (" + when + "): " + drop.getItem() + ", expected " + expected);
    }

    /** Uno stato vuoto con il giocatore come proprietario. */
    private static FrameState ownedBy(ServerPlayer player) {
        FrameState state = new FrameState(RedstoneExoframes.config());
        state.setOwner(player.getUUID());
        return state;
    }

    /** Un pezzo Rottame nuovo nello slot. */
    private static Piece scrapPiece(PieceSlot slot) {
        return Piece.fresh(slot, Tier.SCRAP, RedstoneExoframes.config().tiers);
    }

    /** Un clic destro con la mano principale sul telaio, nel punto dato relativo ai suoi piedi. */
    private static InteractionResult click(ExoframeEntity frame, ServerPlayer player, double x, double y, double z) {
        return frame.interactAt(player, new Vec3(x, y, z), InteractionHand.MAIN_HAND);
    }

    /** L'unico stack del tipo dato nell'inventario principale; vuoto se non c'e'. */
    private static ItemStack findInMainInventory(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Le identita' dei telai piazzati nel livello. */
    private static List<UUID> placedIds(ServerLevel level) {
        return Compat.frameState().placedIn(level).stream().map(PlacedFrame::frameId).toList();
    }

    /** Una voce dell'armatura stivata scritta a mano: slot della testa, versione dei dati e stack dati. */
    private static CompoundTag stowedEntry(int dataVersion, CompoundTag item) {
        CompoundTag entry = new CompoundTag();
        entry.putString(NbtKeys.SLOT, "head");
        entry.putInt(NbtKeys.DATA_VERSION, dataVersion);
        entry.put(NbtKeys.ITEM, item);
        return entry;
    }

    /** Verifica che la lista abbia una sola voce, nello slot della testa, uguale allo stack atteso. */
    private static void assertOneStowedHelmet(GameTestHelper helper, List<StowedStack> taken, ItemStack expected,
            String where) {
        helper.assertTrue(taken.size() == 1, where + ": expected one stowed stack, got " + taken);
        StowedStack stowed = taken.get(0);
        helper.assertTrue(stowed.slot() == EquipmentSlot.HEAD, where + ": stowed slot is " + stowed.slot());
        helper.assertTrue(ItemStack.matches(expected, stowed.stack()) && stowed.stack().getDamageValue() == 5,
                where + ": stowed stack " + stowed.stack() + " differs from " + expected);
    }

    /** Verifica larghezza, altezza, occhi e occhi da accovacciato del giocatore. */
    private static void assertSize(GameTestHelper helper, ServerPlayer player, String when, float width,
            float height, float eyes, float crouchingEyes) {
        helper.assertTrue(near(player.getBbWidth(), width) && near(player.getBbHeight(), height)
                && near(player.getEyeHeight(), eyes) && near(player.getEyeHeight(Pose.CROUCHING), crouchingEyes),
                "wrong size " + when + ": " + size(player) + ", expected w=" + width + " h=" + height + " eye="
                        + eyes + " crouchEye=" + crouchingEyes);
    }

    /**
     * Mette il giocatore nella posa (il cambio di posa ricalcola le dimensioni), verifica larghezza, altezza del
     * riquadro e occhi, e lo rimette in piedi.
     */
    private static void assertPoseSize(GameTestHelper helper, ServerPlayer player, Pose pose, float width,
            float height, float eyes) {
        player.setPose(pose);
        String size = size(player);
        boolean ok = near(player.getBbWidth(), width) && near(player.getBbHeight(), height)
                && near((float) player.getBoundingBox().getYsize(), height) && near(player.getEyeHeight(), eyes);
        player.setPose(Pose.STANDING);
        helper.assertTrue(ok, "wrong size in pose " + pose + " with the suit: " + size + ", expected w=" + width
                + " h=" + height + " eye=" + eyes);
    }

    /** Ingombro e occhi del giocatore nella posa data, letti mettendolo in quella posa; poi lo rimette in piedi. */
    private static String sizeInPose(ServerPlayer player, Pose pose) {
        player.setPose(pose);
        String size = String.format(Locale.ROOT, "pose=%s w=%.4f h=%.4f box=%.4f eye=%.4f", player.getPose(),
                player.getBbWidth(), player.getBbHeight(), player.getBoundingBox().getYsize(), player.getEyeHeight());
        player.setPose(Pose.STANDING);
        return size;
    }

    /** Posa, ingombro e occhi del giocatore, per i messaggi dei test. */
    private static String size(ServerPlayer player) {
        return String.format(Locale.ROOT, "pose=%s w=%.4f h=%.4f eye=%.4f crouchEye=%.4f", player.getPose(),
                player.getBbWidth(), player.getBbHeight(), player.getEyeHeight(),
                player.getEyeHeight(Pose.CROUCHING));
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1e-3f;
    }

    private static boolean near(AABB a, AABB b) {
        return Math.abs(a.minX - b.minX) < 1e-3 && Math.abs(a.minY - b.minY) < 1e-3 && Math.abs(a.minZ - b.minZ) < 1e-3
                && Math.abs(a.maxX - b.maxX) < 1e-3 && Math.abs(a.maxY - b.maxY) < 1e-3
                && Math.abs(a.maxZ - b.maxZ) < 1e-3;
    }

    /** Esegue un comando che deve riuscire; un errore di sintassi o di esecuzione fa fallire il test. */
    private static void runCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source,
            String command) {
        try {
            dispatcher.execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("command failed: " + command + ": " + e.getMessage(), e);
        }
    }

    /** Il giocatore in piedi nel punto relativo alla struttura, rivolto a nord (z decrescenti), in orizzontale. */
    private static void standFacingNorth(GameTestHelper helper, ServerPlayer player, Vec3 relativeFeet) {
        Vec3 feet = helper.absoluteVec(relativeFeet);
        player.moveTo(feet.x, feet.y, feet.z, 180f, 0f);
    }

    /**
     * Esegue {@code /exoframe <sub>} come il giocatore con il livello di permesso dato e ne restituisce il risultato;
     * un errore di sintassi fa fallire il test.
     */
    private static int exoframeCommand(GameTestHelper helper, ServerPlayer player, int permission, String sub) {
        String command = "exoframe " + sub;
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command,
                    player.createCommandSourceStack().withPermission(permission).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("command failed: " + command + ": " + e.getMessage(), e);
        }
    }

    /** Verifica che i quattro slot armatura abbiano ciascuno il proprio segnaposto. */
    private static void assertPlaceholdersWorn(GameTestHelper helper, ServerPlayer player) {
        Map<EquipmentSlot, Item> expected = Map.of(
                EquipmentSlot.HEAD, ModItems.EXOFRAME_HELMET.get(),
                EquipmentSlot.CHEST, ModItems.EXOFRAME_TORSO.get(),
                EquipmentSlot.LEGS, ModItems.EXOFRAME_LEGS.get(),
                EquipmentSlot.FEET, ModItems.EXOFRAME_BOOTS.get());
        expected.forEach((slot, item) -> helper.assertTrue(player.getItemBySlot(slot).is(item),
                "slot " + slot + " holds " + player.getItemBySlot(slot) + ", expected " + item));
    }

    /** Quanti segnaposto ha il giocatore in tutto l'inventario (slot armatura e mano secondaria compresi). */
    private static int countPlaceholders(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Placeholders.isPlaceholder(player.getInventory().getItem(i))) {
                count += player.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    /** Quanti item del tipo dato ci sono nell'inventario principale (senza slot armatura e mano secondaria). */
    private static int countInMainInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Aggiunge un test obbligatorio nella struttura vuota. Il nome {@code temaCaso} diventa
     * {@code redstone_exoframes:tema_caso}: Forge lo legge come ResourceLocation, che non ammette maiuscole.
     */
    private static void add(List<TestFunction> tests, String name, Consumer<GameTestHelper> test) {
        String id = RedstoneExoframes.MOD_ID + ":" + name.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        tests.add(new TestFunction(RedstoneExoframes.MOD_ID, id, EMPTY_STRUCTURE, MAX_TICKS, 0L, true, test));
    }
}
