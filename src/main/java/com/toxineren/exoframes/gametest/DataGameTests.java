package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.content.ChargerBlockEntity;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dei dati per versione (design §12.12): le sette ricette, i loro avanzamenti di sblocco, la tabella di
 * bottino e il tag del caricatore, che {@code tools/gen_data.py} scrive in due alberi (uno per versione del gioco) e
 * che il gioco ignora in silenzio se stanno nella cartella con il nome dell'altra versione. Qui si chiede al server
 * che cosa ha caricato davvero, sulle quattro varianti: le ricette e gli avanzamenti passano dagli aiuti
 * {@code recipeResult}, {@code craft} e {@code hasAdvancement} di {@link Compat#gameTests()} (il gestore delle ricette,
 * il contenitore del banco da lavoro e il gestore degli avanzamenti hanno tipi diversi sulle due versioni); lo sblocco
 * e la rottura sono quelli veri del gioco (il criterio {@code inventory_changed} e
 * {@code ServerPlayerGameMode.destroyBlock}). La tabella {@link #RECIPES} ripete a mano le forme di design §12.12: non
 * le legge dai file, cosi' un file sbagliato non si conferma da solo. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class DataGameTests {
    /** Le sette ricette della mod: oggetto, categoria e forma (design §12.12), tutte con un risultato da uno. */
    private static final List<RecipeCase> RECIPES = List.of(
            new RecipeCase("exoframe", "equipment", ModItems.EXOFRAME, "ICI", "IRI", "I_I"),
            new RecipeCase("scrap_helmet", "equipment", ModItems.SCRAP_HELMET, "ICI", "I_I"),
            new RecipeCase("scrap_torso", "equipment", ModItems.SCRAP_TORSO, "I_I", "ICI", "III"),
            new RecipeCase("scrap_arm", "equipment", ModItems.SCRAP_ARM, "CI", "_I", "_I"),
            new RecipeCase("scrap_leg", "equipment", ModItems.SCRAP_LEG, "I_", "I_", "IC"),
            new RecipeCase("redstone_cell", "redstone", ModItems.REDSTONE_CELL, "_C_", "rQr", "_C_"),
            new RecipeCase("charger", "redstone", ModItems.CHARGER, "ICI", "CRC", "III"));

    /** Dove stanno i piedi del giocatore che rompe i caricatori, relativo alla struttura. */
    private static final Vec3 STAND = new Vec3(3.5, 2.0, 5.5);

    /**
     * Una ricetta della mod: {@code item} e' il percorso dell'oggetto e l'id della ricetta
     * ({@code redstone_exoframes:<item>}), {@code category} la cartella del suo avanzamento di sblocco
     * ({@code recipes/<category>/<item>}), {@code result} l'oggetto atteso, {@code rows} le righe della forma con le
     * lettere di {@code GameTestKit.craftingGrid}.
     */
    private record RecipeCase(String item, String category, Supplier<? extends Item> result, String... rows) {
        /** L'id della ricetta. */
        ResourceLocation recipeId() {
            return Ids.of(item);
        }

        /** L'id dell'avanzamento che la sblocca. */
        ResourceLocation unlockId() {
            return Ids.of("recipes/" + category + "/" + item);
        }
    }

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public DataGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "everyRecipeIsFoundByKey", DataGameTests::everyRecipeIsFoundByKey);
        add(tests, "everyRecipeCraftsFromItsShape", DataGameTests::everyRecipeCraftsFromItsShape);
        add(tests, "everyRecipeHasItsUnlockAdvancement", DataGameTests::everyRecipeHasItsUnlockAdvancement);
        add(tests, "aCopperIngotUnlocksTheSevenRecipes", DataGameTests::aCopperIngotUnlocksTheSevenRecipes);
        add(tests, "chargerNeedsAPickaxeToDropItself", DataGameTests::chargerNeedsAPickaxeToDropItself);
        return tests;
    }

    /**
     * Il gestore delle ricette del server ha le sette ricette sotto la loro chiave, ciascuna con un risultato da uno
     * dell'oggetto giusto; la cella esce vuota e senza dati. Una chiave che non c'e' da' vuoto (controllo).
     */
    private static void everyRecipeIsFoundByKey(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (RecipeCase recipe : RECIPES) {
            Optional<ItemStack> found = Compat.gameTests().recipeResult(server, recipe.recipeId());
            helper.assertTrue(found.isPresent(), "no recipe with the key " + recipe.recipeId());
            ItemStack result = found.get();
            helper.assertTrue(result.is(recipe.result().get()) && result.getCount() == 1,
                    "recipe " + recipe.recipeId() + " gives " + result + ", not one " + recipe.item());
        }
        ItemStack cell = Compat.gameTests().recipeResult(server, Ids.of("redstone_cell")).orElseThrow();
        helper.assertTrue(Compat.itemData().cellEnergy(cell) == 0 && !Compat.itemData().hasData(cell),
                "the crafted cell is not empty and bare: " + Compat.itemData().cellEnergy(cell) + " RE, data "
                        + Compat.itemData().hasData(cell));
        helper.assertTrue(Compat.gameTests().recipeResult(server, Ids.of("no_such_recipe")).isEmpty(),
                "a key with no recipe gave a result");
        helper.succeed();
    }

    /**
     * Il banco da lavoro da' ciascuno dei sette oggetti, uno, dalla sua forma. Controlli: la forma del telaio con un
     * lingotto d'oro al posto del rame e la griglia vuota non danno nulla.
     */
    private static void everyRecipeCraftsFromItsShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (RecipeCase recipe : RECIPES) {
            ItemStack crafted = Compat.gameTests().craft(level, 3, 3, craftingGrid(recipe.rows()));
            helper.assertTrue(crafted.is(recipe.result().get()) && crafted.getCount() == 1,
                    "the shape of " + recipe.item() + " crafts " + crafted + ", not one " + recipe.item());
        }

        List<ItemStack> withGold = craftingGrid("ICI", "IRI", "I_I");
        helper.assertTrue(withGold.get(1).is(Items.COPPER_INGOT), "premise: slot 1 of the exoframe shape holds "
                + withGold.get(1) + ", not the copper ingot");
        withGold.set(1, new ItemStack(Items.GOLD_INGOT));
        ItemStack fromGold = Compat.gameTests().craft(level, 3, 3, withGold);
        helper.assertTrue(fromGold.isEmpty(), "the exoframe shape with gold for copper crafts " + fromGold);

        ItemStack fromNothing = Compat.gameTests().craft(level, 3, 3, craftingGrid("___", "___", "___"));
        helper.assertTrue(fromNothing.isEmpty(), "an empty grid crafts " + fromNothing);
        helper.succeed();
    }

    /**
     * Il server ha i sette avanzamenti di sblocco, ciascuno nella cartella della categoria della sua ricetta.
     * Controllo: il telaio non ne ha uno nella categoria {@code misc}.
     */
    private static void everyRecipeHasItsUnlockAdvancement(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (RecipeCase recipe : RECIPES) {
            helper.assertTrue(Compat.gameTests().hasAdvancement(server, recipe.unlockId()),
                    "no unlock advancement " + recipe.unlockId());
        }
        helper.assertTrue(!Compat.gameTests().hasAdvancement(server, Ids.of("recipes/misc/exoframe")),
                "an advancement that nobody wrote is there: recipes/misc/exoframe");
        helper.succeed();
    }

    /**
     * Un giocatore in sopravvivenza che non conosce le ricette le ha tutte e sette nel libro appena il gioco vede un
     * lingotto di rame nel suo inventario (il criterio {@code inventory_changed} dei sette avanzamenti).
     */
    private static void aCopperIngotUnlocksTheSevenRecipes(GameTestHelper helper) {
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        for (RecipeCase recipe : RECIPES) {
            helper.assertTrue(!p.getRecipeBook().contains(recipe.recipeId()),
                    "premise: a new player already knows " + recipe.recipeId());
        }

        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        helper.assertTrue(p.getInventory().add(copper.copy()), "the copper ingot did not enter the inventory");
        CriteriaTriggers.INVENTORY_CHANGED.trigger(p, p.getInventory(), copper);

        for (RecipeCase recipe : RECIPES) {
            helper.assertTrue(p.getRecipeBook().contains(recipe.recipeId()),
                    "a copper ingot did not unlock " + recipe.recipeId());
        }
        p.getInventory().clearContent();
        helper.succeed();
    }

    /**
     * Il caricatore sta nel tag dei blocchi da piccone accanto alla pietra (il file della mod si e' aggiunto a quello
     * del gioco, non lo ha sostituito) e in nessun altro: vuole il piccone per cadere. Rotto da un giocatore in
     * sopravvivenza con un piccone di legno lascia se' stesso (la tabella di bottino), la cella con la sua carica e la
     * polvere; rotto a mano vuota sparisce e non lascia nulla.
     */
    private static void chargerNeedsAPickaxeToDropItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState state = ModBlocks.CHARGER.get().defaultBlockState();
        helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "the charger is not in mineable/pickaxe");
        helper.assertTrue(Blocks.STONE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "stone is no longer in mineable/pickaxe: the mod's tag file replaced the game's");
        helper.assertTrue(!state.is(BlockTags.MINEABLE_WITH_AXE), "the charger is in mineable/axe");

        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        helper.assertTrue(p.getMainHandItem().isEmpty(), "premise: the new player holds " + p.getMainHandItem());
        helper.assertTrue(!p.hasCorrectToolForDrops(state), "an empty hand is a correct tool for the charger");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        helper.assertTrue(p.hasCorrectToolForDrops(state), "a wooden pickaxe is not a correct tool for the charger");

        BlockPos first = new BlockPos(3, 2, 3);
        ChargerBlockEntity charger = charger(helper, first);
        charger.setCell(cell(1_000));
        charger.setDust(new ItemStack(Items.REDSTONE, 3));
        helper.assertTrue(p.gameMode.destroyBlock(helper.absolutePos(first)), "the pickaxe did not break the charger");
        helper.assertTrue(level.getBlockState(helper.absolutePos(first)).isAir(), "the broken charger left "
                + level.getBlockState(helper.absolutePos(first)));
        List<ItemEntity> dropped = itemsIn(helper);
        int chargers = 0;
        int cells = 0;
        int dusts = 0;
        for (ItemEntity drop : dropped) {
            ItemStack stack = drop.getItem();
            if (stack.is(ModItems.CHARGER.get()) && stack.getCount() == 1) {
                chargers++;
            } else if (stack.is(ModItems.REDSTONE_CELL.get()) && stack.getCount() == 1 && energy(stack) == 1_000) {
                cells++;
            } else if (stack.is(Items.REDSTONE) && stack.getCount() == 3) {
                dusts++;
            }
        }
        helper.assertTrue(dropped.size() == 3 && chargers == 1 && cells == 1 && dusts == 1,
                "the charger broken with a pickaxe dropped " + dropped.stream().map(ItemEntity::getItem).toList()
                        + ", not one charger, one cell with 1000 RE and 3 redstone dust");
        dropped.forEach(ItemEntity::discard);

        BlockPos second = new BlockPos(5, 2, 3);
        charger(helper, second);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(p.gameMode.destroyBlock(helper.absolutePos(second)),
                "the empty hand did not break the charger");
        helper.assertTrue(level.getBlockState(helper.absolutePos(second)).isAir(), "the broken charger left "
                + level.getBlockState(helper.absolutePos(second)));
        helper.assertTrue(itemsIn(helper).isEmpty(), "the charger broken without a pickaxe dropped "
                + itemsIn(helper).stream().map(ItemEntity::getItem).toList());
        helper.succeed();
    }

    /**
     * Posa un caricatore nel punto relativo alla struttura (stato predefinito: vuoto e spento) e restituisce la sua
     * block entity, letta dal livello.
     */
    private static ChargerBlockEntity charger(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.CHARGER.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(entity instanceof ChargerBlockEntity, "block entity of a new charger: " + entity);
        return (ChargerBlockEntity) entity;
    }
}
