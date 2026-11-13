package com.toxineren.exoframes.gametest;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

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
     * Aggiunge un test obbligatorio nella struttura vuota. Il nome {@code temaCaso} diventa
     * {@code redstone_exoframes:tema_caso}: Forge lo legge come ResourceLocation, che non ammette maiuscole.
     */
    private static void add(List<TestFunction> tests, String name, Consumer<GameTestHelper> test) {
        String id = RedstoneExoframes.MOD_ID + ":" + name.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        tests.add(new TestFunction(RedstoneExoframes.MOD_ID, id, EMPTY_STRUCTURE, MAX_TICKS, 0L, true, test));
    }
}
