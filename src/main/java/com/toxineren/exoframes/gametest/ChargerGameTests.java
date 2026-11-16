package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.content.ChargerBlock;
import com.toxineren.exoframes.content.ChargerBlockEntity;
import com.toxineren.exoframes.content.ChargerItem;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.energy.ChargerRule.Status;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco del caricatore (design §12.11 e §12.15). Le posizioni sono relative alla struttura del test: il
 * caricatore sta in {@link #POS}, e ogni test prima di chiudere rimette aria dove ha messo un blocco. Il clic e' quello
 * vero del gioco ({@code clickBlock}: prima il blocco, poi l'oggetto in mano), cosi' prova la base del clic di ogni
 * versione; salvataggio e lettura passano dagli aiuti {@code saveBlockEntity} e {@code loadBlockEntity} di
 * {@link Compat#gameTests()}, che fanno cio' che fa il chunk. Gli aiuti comuni stanno in {@link GameTestKit}.
 * <p>
 * I test della carica chiamano a mano {@link ChargerBlockEntity#serverTick} ({@code tick}) dentro un solo tick del
 * server, cosi' il ticker vero non si mette in mezzo e i numeri sono esatti; un solo test
 * ({@code chargerRealTickerChargesInRealTime}) lascia fare al ticker vero. Chi ha messo qualcosa in un caricatore lo
 * toglie con {@code removeCharger}: svuotato, poi aria.
 */
public final class ChargerGameTests {
    /** Dove sta il caricatore dei test, relativo alla struttura. */
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    /**
     * Le chiavi dei testi del caricatore che il codice tiene in costanti: stato nella barra delle azioni e righe del
     * suggerimento. {@code everyItemAndBlockHasANameInBothLanguages} le cerca nei due file di lingua.
     */
    private static final List<String> CHARGER_TEXT_KEYS =
            Stream.concat(Stream.of(ChargerBlock.STATUS_NO_CELL), ChargerItem.TOOLTIP_KEYS.stream()).toList();

    /** Le cinque chiavi del caricatore nel tag della sua block entity (docs/compat.md, "Schema NBT"). */
    private static final List<String> CHARGER_NBT_KEYS = List.of(NbtKeys.CHARGER_HAS_CELL,
            NbtKeys.CHARGER_CELL_ENERGY, NbtKeys.CHARGER_REDSTONE, NbtKeys.CHARGER_RESERVE,
            NbtKeys.CHARGER_CARRY_MILLI);

    /** I due file di lingua della mod, come risorse del jar. */
    private static final List<String> LANG_FILES = List.of("en_us.json", "it_it.json");

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public ChargerGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "chargerPlacedByItemFacesThePlayer", ChargerGameTests::chargerPlacedByItemFacesThePlayer);
        add(tests, "emptyHandClickTellsNoCell", ChargerGameTests::emptyHandClickTellsNoCell);
        add(tests, "chargerEntitySavesAndLoads", ChargerGameTests::chargerEntitySavesAndLoads);
        add(tests, "chargerBreaksCleanly", ChargerGameTests::chargerBreaksCleanly);
        add(tests, "chargerTooltipTeachesTheControls", ChargerGameTests::chargerTooltipTeachesTheControls);
        add(tests, "everyItemAndBlockHasANameInBothLanguages",
                ChargerGameTests::everyItemAndBlockHasANameInBothLanguages);
        add(tests, "chargerChargesTwoAndAHalfRePerTick", ChargerGameTests::chargerChargesTwoAndAHalfRePerTick);
        add(tests, "chargerStopsWhenFullOrOutOfRedstone", ChargerGameTests::chargerStopsWhenFullOrOutOfRedstone);
        add(tests, "litTogglesTwiceWithoutLosingTheCell", ChargerGameTests::litTogglesTwiceWithoutLosingTheCell);
        add(tests, "chargerRealTickerChargesInRealTime", ChargerGameTests::chargerRealTickerChargesInRealTime);
        add(tests, "chargerSavesAndLoadsItsContents", ChargerGameTests::chargerSavesAndLoadsItsContents);
        add(tests, "chargerLoadNeverThrowsAndNeverCutsACharge",
                ChargerGameTests::chargerLoadNeverThrowsAndNeverCutsACharge);
        add(tests, "comparatorReadsTheCharge", ChargerGameTests::comparatorReadsTheCharge);
        return tests;
    }

    /**
     * L'oggetto del caricatore, usato da un giocatore in sopravvivenza sopra un blocco, posa il blocco rivolto verso di
     * lui (come una fornace), vuoto e spento, con la sua block entity, e si consuma; il blocco fa luce 7 solo acceso e
     * vuole l'attrezzo giusto per cadere.
     */
    private static void chargerPlacedByItemFacesThePlayer(GameTestHelper helper) {
        BlockPos floor = new BlockPos(3, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, new Vec3(3.5, 2.0, 5.5));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHARGER.get()));
        helper.assertTrue(clickBlock(helper, p, floor, InteractionHand.MAIN_HAND).consumesAction(),
                "the charger item was not placed");

        BlockState state = stateAt(helper, POS);
        helper.assertTrue(state.is(ModBlocks.CHARGER.get()), "no charger above the clicked block: " + state);
        helper.assertTrue(state.getValue(ChargerBlock.FACING) == Direction.SOUTH && !state.getValue(ChargerBlock.CELL)
                && !state.getValue(ChargerBlock.LIT), "placed charger state " + state);
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(POS));
        helper.assertTrue(entity instanceof ChargerBlockEntity, "block entity of the placed charger: " + entity);
        helper.assertTrue(entity.onlyOpCanSetNbt(), "the charger entity lets any player set its NBT");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the placed item is still in hand: " + p.getMainHandItem());
        helper.assertTrue(state.getLightEmission() == 0, "unlit charger light " + state.getLightEmission());
        int lit = state.setValue(ChargerBlock.LIT, true).getLightEmission();
        helper.assertTrue(lit == 7, "lit charger light " + lit + ", not 7");
        helper.assertTrue(state.requiresCorrectToolForDrops(), "the charger drops without the right tool");

        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(floor, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Il primo passo del clic: a mani vuote, con la mano principale, il caricatore vuoto dice "nessuna cella" una
     * volta; con un altro oggetto in mano e con la mano secondaria il clic passa, senza messaggi e senza toccare
     * l'oggetto.
     */
    private static void emptyHandClickTellsNoCell(GameTestHelper helper) {
        charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, new Vec3(3.5, 2.0, 5.5));
        helper.assertTrue(p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty(),
                "the mock player holds something");

        helper.assertTrue(clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the empty-hand click was not consumed");
        helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1,
                "no-cell messages after the empty-hand click: " + messages(p, ChargerBlock.STATUS_NO_CELL));

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the click with a stick was consumed");
        helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1,
                "no-cell messages after the click with a stick: " + messages(p, ChargerBlock.STATUS_NO_CELL));
        helper.assertTrue(p.getMainHandItem().is(Items.STICK) && p.getMainHandItem().getCount() == 1,
                "the stick changed: " + p.getMainHandItem());

        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!clickBlock(helper, p, POS, InteractionHand.OFF_HAND).consumesAction(),
                "the off-hand click was consumed");
        helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1,
                "no-cell messages after the off-hand click: " + messages(p, ChargerBlock.STATUS_NO_CELL));

        helper.setBlock(POS, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Salvataggio e lettura come li fa il chunk: il tag porta l'id del tipo e la riserva; una block entity nuova letta
     * da quel tag prende il posto della prima con la riserva scritta; una riserva fuori dai limiti si legge limitata a
     * [0, 1 000 000].
     */
    private static void chargerEntitySavesAndLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        ChargerBlockEntity first = charger(helper, POS);
        CompoundTag saved = Compat.gameTests().saveBlockEntity(level, abs);
        helper.assertTrue(saved.getString("id").equals("redstone_exoframes:charger"),
                "saved block entity id '" + saved.getString("id") + "'");
        helper.assertTrue(saved.contains(NbtKeys.CHARGER_RESERVE) && saved.getInt(NbtKeys.CHARGER_RESERVE) == 0,
                "reserve of a new charger in the saved tag: " + saved);

        saved.putInt(NbtKeys.CHARGER_RESERVE, 321);
        BlockEntity loaded = Compat.gameTests().loadBlockEntity(level, abs, saved);
        helper.assertTrue(loaded instanceof ChargerBlockEntity, "loaded block entity: " + loaded);
        helper.assertTrue(loaded != first, "loading gave the same block entity back");
        helper.assertTrue(first.isRemoved(), "the replaced block entity is not removed");
        helper.assertTrue(level.getBlockEntity(abs) == loaded, "the level does not hold the loaded block entity");
        int reserve = ((ChargerBlockEntity) loaded).reserve();
        helper.assertTrue(reserve == 321, "loaded reserve " + reserve + ", not 321");
        int savedAgain = Compat.gameTests().saveBlockEntity(level, abs).getInt(NbtKeys.CHARGER_RESERVE);
        helper.assertTrue(savedAgain == 321, "reserve saved again " + savedAgain + ", not 321");

        saved.putInt(NbtKeys.CHARGER_RESERVE, -7);
        int negative = ((ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, saved)).reserve();
        helper.assertTrue(negative == 0, "reserve read from -7: " + negative + ", not 0");
        saved.putInt(NbtKeys.CHARGER_RESERVE, 2_000_000);
        int huge = ((ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, saved)).reserve();
        helper.assertTrue(huge == 1_000_000, "reserve read from 2000000: " + huge + ", not 1000000");

        helper.setBlock(POS, Blocks.AIR);
        helper.succeed();
    }

    /** Rotto senza bottino, il caricatore vuoto sparisce con la sua block entity e non lascia nulla a terra. */
    private static void chargerBreaksCleanly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        charger(helper, POS);
        helper.assertTrue(level.destroyBlock(abs, false), "destroyBlock refused the charger");
        helper.assertTrue(stateAt(helper, POS).isAir(), "the broken charger left " + stateAt(helper, POS));
        helper.assertTrue(level.getBlockEntity(abs) == null, "the broken charger left its block entity");
        helper.assertTrue(itemsIn(helper).isEmpty(), "the broken empty charger dropped " + itemsIn(helper));
        helper.succeed();
    }

    /** Il suggerimento dell'oggetto: cinque righe grigie, le chiavi di {@link ChargerItem#TOOLTIP_KEYS} in ordine. */
    private static void chargerTooltipTeachesTheControls(GameTestHelper helper) {
        List<Component> lines = new ArrayList<>();
        ModItems.CHARGER.get().tooltip(new ItemStack(ModItems.CHARGER.get()), lines, false);
        List<String> keys = new ArrayList<>();
        for (Component line : lines) {
            keys.add(line.getContents() instanceof TranslatableContents c ? c.getKey() : "?");
        }
        helper.assertTrue(keys.size() == 5 && keys.equals(ChargerItem.TOOLTIP_KEYS), "charger tooltip " + keys);
        TextColor gray = TextColor.fromLegacyFormat(ChatFormatting.GRAY);
        for (Component line : lines) {
            helper.assertTrue(gray.equals(line.getStyle().getColor()),
                    "tooltip line " + line + " has color " + line.getStyle().getColor());
        }
        helper.succeed();
    }

    /**
     * Ogni oggetto, blocco e tipo di entita' della mod ha il suo nome nei due file di lingua, letti dal jar; e cosi' le
     * chiavi dei testi del caricatore che il codice tiene in costanti ({@link #CHARGER_TEXT_KEYS}): una costante
     * scritta diversa dalla chiave del file mostrerebbe in gioco la chiave nuda, e nessun altro test lo vedrebbe.
     */
    private static void everyItemAndBlockHasANameInBothLanguages(GameTestHelper helper) {
        List<String> items = new ArrayList<>();
        List<String> blocks = new ArrayList<>();
        List<String> entities = new ArrayList<>();
        boolean chargerItem = false;
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (id.getNamespace().equals(RedstoneExoframes.MOD_ID)) {
                items.add(BuiltInRegistries.ITEM.get(id).getDescriptionId());
                chargerItem |= id.getPath().equals("charger");
            }
        }
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals(RedstoneExoframes.MOD_ID)) {
                blocks.add(BuiltInRegistries.BLOCK.get(id).getDescriptionId());
            }
        }
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (id.getNamespace().equals(RedstoneExoframes.MOD_ID)) {
                entities.add(BuiltInRegistries.ENTITY_TYPE.get(id).getDescriptionId());
            }
        }
        helper.assertTrue(items.size() >= 12 && chargerItem && blocks.size() >= 1 && entities.size() >= 1,
                "ids of the mod found: " + items.size() + " items (charger: " + chargerItem + "), " + blocks.size()
                        + " blocks, " + entities.size() + " entity types");

        TreeSet<String> wanted = new TreeSet<>(items);
        wanted.addAll(blocks);
        wanted.addAll(entities);
        wanted.addAll(CHARGER_TEXT_KEYS);
        List<String> missing = new ArrayList<>();
        for (String file : LANG_FILES) {
            Map<String, String> texts = readLang(helper, file);
            for (String key : wanted) {
                if (!texts.containsKey(key)) {
                    missing.add(file + ": " + key);
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "keys missing from the language files: " + missing);
        helper.succeed();
    }

    /**
     * Con i predefiniti (50 RE al secondo, 500 RE per polvere) la cella prende 2,5 RE per tick: 2 nei tick dispari, 3
     * nei pari, con il riporto in millesimi. La prima polvere si brucia al primo tick e dura 200 tick; la seconda si
     * brucia solo al tick 201. Lo stato del blocco dice cella presente e in carica.
     */
    private static void chargerChargesTwoAndAHalfRePerTick(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        c.setCell(cell(0));
        c.setDust(redstone(20));

        tick(helper, c, 1);
        assertContents(helper, c, "after 1 tick", 2, 19, 498);
        helper.assertTrue(c.carryMilli() == 500, "carry after 1 tick " + c.carryMilli() + ", not 500");
        assertCellAndLit(helper, POS, "after 1 tick", true, true);

        tick(helper, c, 1);
        assertContents(helper, c, "after 2 ticks", 5, 19, 495);

        tick(helper, c, 198);
        assertContents(helper, c, "after 200 ticks", 500, 19, 0);

        tick(helper, c, 1);
        assertContents(helper, c, "after 201 ticks", 502, 18, 498);

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * La carica si ferma da sola: a cella piena (la riserva resta per la cella dopo, e la luce si spegne al tick
     * seguente) e quando non c'e' piu' ne' polvere ne' riserva. Una cella vuota messa dopo consuma la riserva rimasta
     * senza altra polvere.
     */
    private static void chargerStopsWhenFullOrOutOfRedstone(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        c.setCell(cell(9_999));
        c.setDust(redstone(1));

        tick(helper, c, 1);
        assertContents(helper, c, "filling tick", 10_000, 0, 499);
        assertCellAndLit(helper, POS, "filling tick", true, true);

        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "tick after full", true, false);
        helper.assertTrue(c.status() == Status.FULL, "status of a full cell: " + c.status());
        assertContents(helper, c, "tick after full", 10_000, 0, 499);

        c.setCell(cell(0));
        tick(helper, c, 200);
        assertContents(helper, c, "empty cell on the reserve left, 200 ticks", 499, 0, 0);

        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "reserve spent", true, false);
        helper.assertTrue(c.status() == Status.NO_REDSTONE, "status with nothing to burn: " + c.status());
        assertContents(helper, c, "reserve spent", 499, 0, 0);

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * {@code lit} si accende e si spegne due volte e {@code cell} una: ogni volta il tick riscrive lo stato del blocco,
     * e la block entity resta la stessa, non rimossa, e nulla cade a terra. Un cambio di proprieta' non e' una rottura.
     */
    private static void litTogglesTwiceWithoutLosingTheCell(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);

        c.setCell(cell(0));
        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "cell, nothing to burn", true, false);
        assertSameEntityNothingDropped(helper, c, "cell, nothing to burn");

        c.setDust(redstone(1));
        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "cell and dust", true, true);
        assertSameEntityNothingDropped(helper, c, "cell and dust");

        c.setCell(cell(10_000));
        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "full cell", true, false);
        assertSameEntityNothingDropped(helper, c, "full cell");

        c.setCell(cell(0));
        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "empty cell on the reserve left", true, true);
        assertSameEntityNothingDropped(helper, c, "empty cell on the reserve left");

        c.setCell(ItemStack.EMPTY);
        tick(helper, c, 1);
        assertCellAndLit(helper, POS, "no cell", false, false);
        assertSameEntityNothingDropped(helper, c, "no cell");

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Il ticker vero del gioco ({@code ChargerBlock.getTicker}), senza chiamate a mano: dopo 40 tick del server la
     * cella ha circa 100 RE (un tick in piu' o in meno secondo l'ordine fra il tick del livello e quello dei test), una
     * polvere e' stata bruciata e lo stato del blocco dice cella presente e in carica.
     */
    private static void chargerRealTickerChargesInRealTime(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        c.setCell(cell(0));
        c.setDust(redstone(2));
        helper.runAfterDelay(40, () -> runInSequence(helper, h -> {
            int stored = energy(c.cell());
            h.assertTrue(stored >= 95 && stored <= 105, "charge after 40 real ticks: " + stored + ", not about 100");
            assertCellAndLit(h, POS, "after 40 real ticks", true, true);
            h.assertTrue(c.dust().getCount() == 1, "dust after 40 real ticks: " + c.dust().getCount() + ", not 1");
            removeCharger(h, POS);
            h.succeed();
        }));
    }

    /**
     * Il contenuto passa dal salvataggio del chunk: cinque chiavi, tutte numeri (nessuno stack nell'NBT), con i valori
     * di cella, polvere, riserva e riporto; la block entity letta da quel tag li ha uguali e riprende a caricare dal
     * riporto salvato (il tick dopo sposta 3 RE, non 2).
     */
    private static void chargerSavesAndLoadsItsContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        ChargerBlockEntity c = charger(helper, POS);
        c.setCell(cell(1_234));
        c.setDust(redstone(7));
        tick(helper, c, 1);
        assertContents(helper, c, "before saving", 1_236, 6, 498);
        helper.assertTrue(c.carryMilli() == 500, "carry before saving " + c.carryMilli() + ", not 500");

        CompoundTag saved = Compat.gameTests().saveBlockEntity(level, abs);
        helper.assertTrue(saved.getBoolean(NbtKeys.CHARGER_HAS_CELL)
                && saved.getInt(NbtKeys.CHARGER_CELL_ENERGY) == 1_236 && saved.getInt(NbtKeys.CHARGER_REDSTONE) == 6
                && saved.getInt(NbtKeys.CHARGER_RESERVE) == 498
                && saved.getInt(NbtKeys.CHARGER_CARRY_MILLI) == 500, "saved charger " + saved);
        for (String key : CHARGER_NBT_KEYS) {
            helper.assertTrue(saved.contains(key, Tag.TAG_ANY_NUMERIC), "saved key '" + key + "' is not a number: "
                    + saved);
        }

        ChargerBlockEntity loaded = (ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, saved);
        helper.assertTrue(loaded != c && !loaded.cell().isEmpty(), "loaded charger has no cell, or is the saved one");
        assertContents(helper, loaded, "loaded", 1_236, 6, 498);
        helper.assertTrue(loaded.carryMilli() == 500, "loaded carry " + loaded.carryMilli() + ", not 500");

        tick(helper, loaded, 1);
        assertContents(helper, loaded, "loaded, one tick later", 1_239, 6, 495);

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * La lettura non lancia mai e non taglia mai una carica: una cella con piu' della capacita' della config resta
     * com'e' (vale piena, e il tick non la tocca), gli altri valori si limitano, un valore mancante o del tipo
     * sbagliato vale zero. Nemmeno {@code setCell} taglia: mette nello slot uno stack nuovo con la stessa carica e non
     * tocca quello passato.
     */
    private static void chargerLoadNeverThrowsAndNeverCutsACharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        charger(helper, POS);
        CompoundTag empty = Compat.gameTests().saveBlockEntity(level, abs);

        CompoundTag over = empty.copy();
        over.putBoolean(NbtKeys.CHARGER_HAS_CELL, true);
        over.putInt(NbtKeys.CHARGER_CELL_ENERGY, 40_000);
        over.putInt(NbtKeys.CHARGER_REDSTONE, 500);
        over.putInt(NbtKeys.CHARGER_RESERVE, -7);
        over.putInt(NbtKeys.CHARGER_CARRY_MILLI, 5_000);
        ChargerBlockEntity a = (ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, over);
        assertContents(helper, a, "over the capacity", 40_000, 64, 0);
        helper.assertTrue(a.carryMilli() == 999, "carry read from 5000: " + a.carryMilli() + ", not 999");
        helper.assertTrue(a.status() == Status.FULL && a.comparatorOutput() == 15,
                "cell over the capacity: status " + a.status() + ", comparator " + a.comparatorOutput());
        tick(helper, a, 1);
        assertContents(helper, a, "over the capacity, one tick later", 40_000, 64, 0);

        CompoundTag broken = empty.copy();
        broken.putBoolean(NbtKeys.CHARGER_HAS_CELL, true);
        broken.putInt(NbtKeys.CHARGER_CELL_ENERGY, -5);
        broken.putString(NbtKeys.CHARGER_REDSTONE, "x");
        broken.putInt(NbtKeys.CHARGER_RESERVE, 2_000_000);
        ChargerBlockEntity b = (ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, broken);
        helper.assertTrue(!b.cell().isEmpty(), "the cell with a negative charge was not read");
        assertContents(helper, b, "negative charge, redstone as a string", 0, 0, 1_000_000);

        CompoundTag bare = new CompoundTag();
        for (String key : List.of("id", "x", "y", "z")) {
            bare.put(key, empty.get(key).copy());
        }
        ChargerBlockEntity c = (ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs, bare);
        helper.assertTrue(c.cell().isEmpty() && c.dust().isEmpty() && c.reserve() == 0 && c.carryMilli() == 0,
                "charger read from a bare tag: cell " + c.cell() + ", dust " + c.dust() + ", reserve " + c.reserve()
                        + ", carry " + c.carryMilli());

        // cell(40_000) non si puo' scrivere: una EnergyCell non tiene piu' della sua capacita'
        ItemStack passed = cell(0);
        Compat.itemData().setCellEnergy(passed, 40_000);
        c.setCell(passed);
        helper.assertTrue(energy(c.cell()) == 40_000, "setCell cut the charge to " + energy(c.cell()));
        helper.assertTrue(c.cell() != passed, "setCell kept the stack it was given");
        helper.assertTrue(energy(passed) == 40_000 && passed.getCount() == 1,
                "setCell changed the stack it was given: " + passed + " with " + energy(passed) + " RE");

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * L'uscita per il comparatore: 0 senza cella, altrimenti la carica in quindicesimi della capacita', per difetto (15
     * solo a cella piena).
     */
    private static void comparatorReadsTheCharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        ChargerBlockEntity c = charger(helper, POS);
        BlockState state = stateAt(helper, POS);
        helper.assertTrue(state.hasAnalogOutputSignal(), "the charger has no comparator output");
        helper.assertTrue(state.getAnalogOutputSignal(level, abs) == 0,
                "comparator output without a cell: " + state.getAnalogOutputSignal(level, abs));
        int[][] cases = {{666, 0}, {667, 1}, {5_000, 7}, {9_999, 14}, {10_000, 15}};
        for (int[] expected : cases) {
            c.setCell(cell(expected[0]));
            int output = state.getAnalogOutputSignal(level, abs);
            helper.assertTrue(output == expected[1], "comparator output with " + expected[0] + " RE: " + output
                    + ", not " + expected[1]);
        }

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Posa un caricatore nello stato predefinito nel punto relativo alla struttura e restituisce la sua block entity,
     * letta dal livello.
     */
    private static ChargerBlockEntity charger(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.CHARGER.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(entity instanceof ChargerBlockEntity, "block entity of a new charger: " + entity);
        return (ChargerBlockEntity) entity;
    }

    /** Lo stato del blocco nel punto relativo alla struttura. */
    private static BlockState stateAt(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockState(helper.absolutePos(relative));
    }

    /**
     * Chiama {@code times} volte il tick del server del caricatore, ogni volta con lo stato del blocco letto dal
     * livello, come fa il ticker del gioco.
     */
    private static void tick(GameTestHelper helper, ChargerBlockEntity c, int times) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = c.getBlockPos();
        for (int i = 0; i < times; i++) {
            ChargerBlockEntity.serverTick(level, pos, level.getBlockState(pos), c);
        }
    }

    /** Toglie il caricatore nel punto relativo alla struttura: prima svuotato (cella e polvere), poi aria. */
    private static void removeCharger(GameTestHelper helper, BlockPos relative) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof ChargerBlockEntity c) {
            c.setCell(ItemStack.EMPTY);
            c.setDust(ItemStack.EMPTY);
        }
        helper.setBlock(relative, Blocks.AIR);
    }

    /** Uno stack di {@code count} polveri di redstone. */
    private static ItemStack redstone(int count) {
        return new ItemStack(Items.REDSTONE, count);
    }

    /** Carica della cella, numero di polveri e riserva del caricatore sono quelli attesi. */
    private static void assertContents(GameTestHelper helper, ChargerBlockEntity c, String when, int energy, int dust,
            int reserve) {
        helper.assertTrue(energy(c.cell()) == energy && c.dust().getCount() == dust && c.reserve() == reserve,
                when + ": cell " + energy(c.cell()) + " RE, dust " + c.dust().getCount() + ", reserve " + c.reserve()
                        + "; expected " + energy + ", " + dust + ", " + reserve);
    }

    /** Le proprieta' {@code cell} e {@code lit} dello stato del blocco nel punto relativo sono quelle attese. */
    private static void assertCellAndLit(GameTestHelper helper, BlockPos relative, String when, boolean cell,
            boolean lit) {
        BlockState state = stateAt(helper, relative);
        helper.assertTrue(state.is(ModBlocks.CHARGER.get()) && state.getValue(ChargerBlock.CELL) == cell
                && state.getValue(ChargerBlock.LIT) == lit, when + ": block state " + state + ", expected cell="
                + cell + " lit=" + lit);
    }

    /** La block entity del livello e' ancora {@code c}, non rimossa, e nella struttura nulla e' a terra. */
    private static void assertSameEntityNothingDropped(GameTestHelper helper, ChargerBlockEntity c, String when) {
        helper.assertTrue(helper.getLevel().getBlockEntity(c.getBlockPos()) == c && !c.isRemoved(),
                when + ": the block entity was replaced or removed");
        helper.assertTrue(itemsIn(helper).isEmpty(), when + ": items on the ground " + itemsIn(helper));
    }

    /**
     * Un file di lingua della mod letto dal jar come mappa da chiave a testo: prima dal class loader della mod, poi
     * dalla classe; se nessuno dei due lo trova il test fallisce e dice quale file non ha letto.
     */
    private static Map<String, String> readLang(GameTestHelper helper, String file) {
        String path = "assets/" + RedstoneExoframes.MOD_ID + "/lang/" + file;
        InputStream in = RedstoneExoframes.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            in = RedstoneExoframes.class.getResourceAsStream("/" + path);
        }
        if (in == null) {
            helper.fail("language file not readable from the mod jar: " + path);
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Map<String, String> texts = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() { }.getType());
            helper.assertTrue(texts != null && !texts.isEmpty(), "language file empty: " + path);
            return texts;
        } catch (IOException e) {
            throw new IllegalStateException("language file not readable: " + path, e);
        }
    }
}
