package com.toxineren.exoframes.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.ModuleType;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.SuitSize;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
