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
import com.toxineren.exoframes.frame.SuitSize;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Test in gioco (GameTest vanilla) comuni alle quattro varianti, senza differenze di versione o loader: quelle stanno
 * dietro {@link Compat#gameTests()}. Si lanciano con {@code ./gradlew :<variante>:runGameTestServer} o con
 * {@code bash tools/run_gametests.sh}.
 *
 * <p>Per aggiungere un test: un metodo {@code private static void temaCaso(GameTestHelper helper)} che chiude con
 * {@code helper.succeed()}, e una riga {@code add(tests, "temaCaso", ExoframeGameTests::temaCaso);} in
 * {@link #generate()}. tools/run_gametests.sh conta queste righe per sapere quanti test aspettarsi.
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
        add(tests, "strangerEnteringDropsTheOwnersArmor", ExoframeGameTests::strangerEnteringDropsTheOwnersArmor);
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
     * {@link DebugWear#wear} su un giocatore con un elmo di ferro: i quattro slot armatura hanno i segnaposto, il
     * torso porta il riassunto (sei pezzi SCRAP INTACT non dipinti) e l'elmo di ferro e' finito nell'inventario, dove
     * resta dopo {@link DebugWear#remove}.
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

        assertPlaceholdersWorn(helper, player);
        Optional<SuitSummary> written = Compat.itemData().suitSummary(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(Optional.of(summary).equals(written), "wrong suit summary on the torso: " + written);
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 1,
                "iron helmet not moved to the inventory");

        DebugWear.remove(player);
        helper.assertTrue(countPlaceholders(player) == 0,
                countPlaceholders(player) + " placeholders left after remove");
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 1,
                "iron helmet not in the inventory after remove");
        helper.succeed();
    }

    /**
     * {@link DebugWear#remove} toglie i segnaposto dagli slot armatura e dal resto dell'inventario. Un secondo
     * {@code wear} sostituisce i segnaposto gia' indossati senza metterli nell'inventario.
     */
    private static void debugWearRemoveClearsEverything(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        DebugWear.wear(player, SuitSummary.BARE);
        helper.assertTrue(countPlaceholders(player) == 4,
                "wearing twice left " + countPlaceholders(player) + " placeholders, expected 4");
        player.getInventory().items.set(5, new ItemStack(ModItems.EXOFRAME_LEGS.get()));
        player.getInventory().offhand.set(0, new ItemStack(ModItems.EXOFRAME_TORSO.get()));

        DebugWear.remove(player);

        helper.assertTrue(countPlaceholders(player) == 0,
                countPlaceholders(player) + " placeholders left after remove");
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            helper.assertTrue(player.getItemBySlot(slot).isEmpty(), "armor slot not empty after remove: " + slot);
        }
        helper.succeed();
    }

    /**
     * Il comando {@code /exoframe debug wear rottame 2 <primario> <secondario>}: sei pezzi SCRAP CRITICAL con i
     * colori dati. Prima un colore malformato fa fallire il comando senza cambiare nulla.
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
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                "a failed command changed the armor slots");

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
        helper.succeed();
    }

    /**
     * Segnaposto del torso con riassunto assente o malformato, come da {@code /give}, altre mod o dati vecchi: la
     * lettura non lancia (vuota senza riassunto o con {@code visual} non compound, telaio nudo se le voci sono tutte
     * sconosciute), {@code via} li toglie ovunque e {@code nudo} sostituisce quello indossato.
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
     * carponi (0,8 x 0,99). Il segnaposto va nello slot senza DebugWear, che ricalcolerebbe da solo: qui il ricalcolo
     * deve arrivare dal tick del giocatore (TickEvent.PLAYER_PRE). I giocatori finti non li fa girare il server.
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

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.EXOFRAME_TORSO.get()));
        player.doTick();
        player.doTick();
        helper.assertTrue(player.getPose() == Pose.SWIMMING, "suit under a ceiling at 2 not crawling: " + size(player));
        helper.assertTrue(near(player.getBbHeight(), 0.6f * 1.65f) && near(player.getBbWidth(), 0.8f),
                "wrong crawling box: " + size(player));
        helper.assertTrue(near((float) player.getBoundingBox().getYsize(), 0.6f * 1.65f),
                "crawling bounding box not rebuilt: " + player.getBoundingBox());
        helper.succeed();
    }

    /** Verifica larghezza, altezza, occhi e occhi da accovacciato del giocatore. */
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

    /** Un record piazzato a 10.000 blocchi, in un chunk senza entita' caricate: le verifiche non creano nulla. */
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
     * pitch resta il suo) e ha subito la grandezza della tuta (0,8 x 2,97). Sul client il clic consuma.
     */
    private static void enterStowsArmorAndWearsTheFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Vec3 standing = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.moveTo(standing.x, standing.y, standing.z, 0f, 20f);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        FrameState state = charged(ownedBy(player));
        state.mount(scrapPiece(PieceSlot.TORSO));
        ExoframeEntity frame = placeFrame(helper, state, new Vec3(4.5, 1.0, 4.5), 30f);
        UUID frameId = frame.frameId();
        Vec3 spot = frame.position();

        helper.assertTrue(FrameInteractions.clientResult(player, InteractionHand.MAIN_HAND).consumesAction(),
                "client does not consume the empty hand click");
        helper.assertTrue(click(frame, player, 0.0, 1.6, 0.0).consumesAction(), "enter click not consumed");

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
     * Il proprietario che entra nel telaio lasciato con la sua armatura dentro (dopo una morte) la riprende
     * nell'inventario; nel record resta solo quella che indossava entrando.
     */
    private static void ownerGetsBackArmorLeftInTheFrame(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        ExoframeEntity frame = frameWithStowedBoots(helper, owner, charged(ownedBy(owner)));
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setDamageValue(5);
        owner.setItemSlot(EquipmentSlot.HEAD, helmet.copy());

        helper.assertTrue(FrameEntry.enter(owner, frame), "owner could not enter");

        helper.assertTrue(countInMainInventory(owner, Items.IRON_BOOTS) == 1, "boots left in the frame not given back");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                SuitSize.standingBox(frame.position()).inflate(2.0), e -> e.getItem().is(Items.IRON_BOOTS)).isEmpty(),
                "boots dropped instead of given");
        assertOneStowedHelmet(helper, frames.takeStowed(owner), helmet, "owner's frame");
        leaveTestFrame(owner);
        helper.succeed();
    }

    /**
     * Un altro giocatore che entra in un telaio sbloccato con l'armatura del proprietario dentro non la prende: cade
     * a terra nel punto del telaio, ferma, e nel record resta solo la sua.
     */
    private static void strangerEnteringDropsTheOwnersArmor(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer stranger = Compat.gameTests().mockServerPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        FrameState state = charged(ownedBy(owner));
        state.setUnlocked(true);
        ExoframeEntity frame = frameWithStowedBoots(helper, owner, state);
        Vec3 spot = frame.position();

        helper.assertTrue(FrameEntry.enter(stranger, frame), "stranger could not enter an unlocked frame");

        helper.assertTrue(countInMainInventory(stranger, Items.IRON_BOOTS) == 0
                && countInMainInventory(owner, Items.IRON_BOOTS) == 0, "boots given to a player");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                SuitSize.standingBox(spot).inflate(2.0), e -> e.getItem().is(Items.IRON_BOOTS));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                dropped.size() + " dropped boots, expected one stack of 1");
        ItemEntity boots = dropped.get(0);
        helper.assertTrue(boots.position().distanceTo(spot) < 1e-6 && boots.getDeltaMovement().equals(Vec3.ZERO),
                "boots not lying at the frame: " + boots.position() + " moving " + boots.getDeltaMovement());
        boots.discard();
        helper.assertTrue(frames.takeStowed(stranger).isEmpty(), "owner's boots still in the record");
        leaveTestFrame(stranger);
        helper.succeed();
    }

    /**
     * Un telaio piazzato con lo stato dato e un paio di stivali di ferro stivati, come dopo una morte: il giocatore
     * entra dal registro, stiva gli stivali ed esce; poi l'entita' nel punto.
     */
    private static ExoframeEntity frameWithStowedBoots(GameTestHelper helper, ServerPlayer player, FrameState state) {
        ServerLevel level = helper.getLevel();
        FrameStateAccess frames = Compat.frameState();
        Vec3 feet = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
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

    /** Toglie il record indossato del test (l'armatura stivata la riprende prima chi chiama) e i segnaposto. */
    private static void leaveTestFrame(ServerPlayer player) {
        Compat.frameState().clear(player);
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
            if (DebugWear.isPlaceholder(player.getInventory().getItem(i))) {
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
