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
import com.toxineren.exoframes.frame.FrameSounds;
import com.toxineren.exoframes.frame.FrameSounds.Sound;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
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
 * <p>
 * I test delle tramogge posano tramogge vere e lasciano fare al gioco per qualche decina di tick: cosi' passano dal
 * codice delle tramogge di ogni loader. Prima di togliere una tramoggia la svuotano ({@code clearContent()}), perche'
 * una tramoggia tolta lascia cadere cio' che tiene.
 */
public final class ChargerGameTests {
    /** Dove sta il caricatore dei test, relativo alla struttura. */
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    /** Dove stanno i piedi del giocatore che clicca, relativo alla struttura: due blocchi a sud del caricatore. */
    private static final Vec3 STAND = new Vec3(3.5, 2.0, 5.5);

    /** Le quattro chiavi dello stato che il clic mostra nella barra delle azioni. */
    private static final List<String> STATUS_KEYS = List.of(ChargerBlock.STATUS_NO_CELL, ChargerBlock.STATUS_FULL,
            ChargerBlock.STATUS_NO_REDSTONE, ChargerBlock.STATUS_CHARGING);

    /**
     * Le chiavi dei testi del caricatore che il codice tiene in costanti: stato nella barra delle azioni e righe del
     * suggerimento. {@code everyItemAndBlockHasANameInBothLanguages} le cerca nei due file di lingua.
     */
    private static final List<String> CHARGER_TEXT_KEYS = Stream.concat(STATUS_KEYS.stream(),
            ChargerItem.TOOLTIP_KEYS.stream()).toList();

    /** Le cinque chiavi del caricatore nel tag della sua block entity (docs/compat.md, "Schema NBT"). */
    private static final List<String> CHARGER_NBT_KEYS = List.of(NbtKeys.CHARGER_HAS_CELL,
            NbtKeys.CHARGER_CELL_ENERGY, NbtKeys.CHARGER_REDSTONE, NbtKeys.CHARGER_RESERVE,
            NbtKeys.CHARGER_CARRY_MILLI);

    /**
     * Gli stessi cinque nomi come stanno sul disco, scritti alla lettera e non con le costanti di {@link NbtKeys}: una
     * costante a cui cambia la stringa farebbe leggere vuoto ogni caricatore gia' salvato, e deve far fallire un test.
     */
    private static final List<String> CHARGER_NAMES_ON_DISK = List.of("has_cell", "cell_energy", "redstone",
            "reserve", "carry_milli");

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
        add(tests, "cellClickInsertsIntoAnEmptyCharger", ChargerGameTests::cellClickInsertsIntoAnEmptyCharger);
        add(tests, "cellClickSwapsWithTheCellInside", ChargerGameTests::cellClickSwapsWithTheCellInside);
        add(tests, "dustClickMovesWhatFits", ChargerGameTests::dustClickMovesWhatFits);
        add(tests, "emptyHandTakesTheCellThenTheDust", ChargerGameTests::emptyHandTakesTheCellThenTheDust);
        add(tests, "sneakingWithEmptyHandsOnlyTellsTheStatus",
                ChargerGameTests::sneakingWithEmptyHandsOnlyTellsTheStatus);
        add(tests, "otherItemsAndTheOffHandPass", ChargerGameTests::otherItemsAndTheOffHandPass);
        add(tests, "creativeHandIsNotConsumed", ChargerGameTests::creativeHandIsNotConsumed);
        add(tests, "twoPlayersOneCell", ChargerGameTests::twoPlayersOneCell);
        add(tests, "hopperAboveInsertsOneCell", ChargerGameTests::hopperAboveInsertsOneCell);
        add(tests, "hopperAtTheSideAddsDustToANonEmptySlot",
                ChargerGameTests::hopperAtTheSideAddsDustToANonEmptySlot);
        add(tests, "hopperBelowTakesOnlyAFullCell", ChargerGameTests::hopperBelowTakesOnlyAFullCell);
        add(tests, "fullHopperBelowNeitherLosesNorDuplicatesTheCell",
                ChargerGameTests::fullHopperBelowNeitherLosesNorDuplicatesTheCell);
        add(tests, "facesAcceptOnlyWhatTheyShould", ChargerGameTests::facesAcceptOnlyWhatTheyShould);
        add(tests, "breakingDropsTheCellAndTheDust", ChargerGameTests::breakingDropsTheCellAndTheDust);
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
     * A mani vuote, con la mano principale, il caricatore vuoto dice "nessuna cella" una volta; con un altro oggetto in
     * mano e con la mano secondaria il clic passa, senza messaggi e senza toccare l'oggetto.
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
     * riporto salvato (il tick dopo sposta 3 RE, non 2). Le cinque chiavi si guardano anche con i nomi che hanno sul
     * disco, scritti alla lettera, e oltre a {@code id}, {@code x}, {@code y}, {@code z} il tag non ne ha altre.
     * <p>
     * Prima ancora, un cambio di contenuto segna il chunk come da salvare ({@code setChanged()}): senza quel segno una
     * cella messa in un caricatore che non sta caricando si perde quando il chunk si scarica. Il test toglie il segno
     * e lo ritrova messo dopo {@code setCell} e dopo {@code setDust}, tutto dentro lo stesso tick: il chunk non resta
     * mai senza.
     */
    private static void chargerSavesAndLoadsItsContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        ChargerBlockEntity c = charger(helper, POS);
        LevelChunk chunk = level.getChunkAt(abs);
        chunk.setUnsaved(false);
        c.setCell(cell(1_234));
        helper.assertTrue(chunk.isUnsaved(), "setCell did not mark the chunk for saving");
        chunk.setUnsaved(false);
        c.setDust(redstone(7));
        helper.assertTrue(chunk.isUnsaved(), "setDust did not mark the chunk for saving");
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
        helper.assertTrue(saved.getBoolean("has_cell") && saved.getInt("cell_energy") == 1_236
                && saved.getInt("redstone") == 6 && saved.getInt("reserve") == 498
                && saved.getInt("carry_milli") == 500, "saved charger read by its on-disk names: " + saved);
        TreeSet<String> own = new TreeSet<>(saved.getAllKeys());
        own.removeAll(List.of("id", "x", "y", "z"));
        helper.assertTrue(own.equals(new TreeSet<>(CHARGER_NAMES_ON_DISK)), "keys of a saved charger besides id, x, y"
                + " and z: " + own + ", expected " + new TreeSet<>(CHARGER_NAMES_ON_DISK));

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
     * Con una cella in mano il clic la mette nel caricatore vuoto: la mano si svuota, il caricatore tiene la carica,
     * suona l'inserimento e la barra delle azioni dice lo stato ("niente redstone"). Una cella con piu' della capacita'
     * della config, entrando a mano, si taglia alla capacita' (design §12.10): e' il solo ingresso che taglia.
     */
    private static void cellClickInsertsIntoAnEmptyCharger(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, cell(3_000));

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            InteractionResult result = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.SUCCESS, "the click with a cell gave " + result);
            helper.assertTrue(p.getMainHandItem().isEmpty(), "the inserted cell is still in hand: "
                    + p.getMainHandItem());
            helper.assertTrue(!c.cell().isEmpty() && energy(c.cell()) == 3_000,
                    "cell in the charger: " + c.cell() + " with " + energy(c.cell()) + " RE, not 3000");
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_REDSTONE) == 1 && statusMessages(p) == 1,
                    "status messages after the insert: " + statusMessages(p) + ", no-redstone "
                            + messages(p, ChargerBlock.STATUS_NO_REDSTONE));
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CHARGER_INSERT)),
                    "the insert played " + sounds.sounds() + ", expected [CHARGER_INSERT]");
        }

        BlockPos second = new BlockPos(5, 2, 3);
        ChargerBlockEntity c2 = charger(helper, second);
        // cell(40_000) non si puo' scrivere: una EnergyCell non tiene piu' della sua capacita'
        ItemStack big = cell(0);
        Compat.itemData().setCellEnergy(big, 40_000);
        p.setItemInHand(InteractionHand.MAIN_HAND, big);
        helper.assertTrue(clickBlock(helper, p, second, InteractionHand.MAIN_HAND).consumesAction(),
                "the click with the cell over the capacity was not consumed");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the cell over the capacity is still in hand: "
                + p.getMainHandItem());
        helper.assertTrue(!c2.cell().isEmpty() && energy(c2.cell()) == 10_000,
                "cell entered by hand with 40000 RE: " + c2.cell() + " with " + energy(c2.cell()) + " RE, not 10000");
        helper.assertTrue(messages(p, ChargerBlock.STATUS_FULL) == 1,
                "full messages after the second insert: " + messages(p, ChargerBlock.STATUS_FULL));

        removeCharger(helper, POS);
        removeCharger(helper, second);
        helper.succeed();
    }

    /**
     * Con una cella in mano su un caricatore che ne ha gia' una il clic le scambia: quella di dentro va in mano,
     * quella in mano va dentro, suona l'inserimento. Due celle e 8 500 RE prima e dopo, nessuna altrove.
     */
    private static void cellClickSwapsWithTheCellInside(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        c.setCell(cell(8_000));
        p.setItemInHand(InteractionHand.MAIN_HAND, cell(500));

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            InteractionResult result = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.SUCCESS, "the swapping click gave " + result);
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CHARGER_INSERT)),
                    "the swap played " + sounds.sounds() + ", expected [CHARGER_INSERT]");
        }
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(held.is(ModItems.REDSTONE_CELL.get()) && held.getCount() == 1 && energy(held) == 8_000,
                "in hand after the swap: " + held + " with " + energy(held) + " RE, not one cell with 8000");
        helper.assertTrue(!c.cell().isEmpty() && c.cell().getCount() == 1 && energy(c.cell()) == 500,
                "in the charger after the swap: " + c.cell() + " with " + energy(c.cell()) + " RE, not 500");
        helper.assertTrue(countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 1 && p.getOffhandItem().isEmpty()
                && itemsIn(helper).isEmpty(), "cells of the player after the swap: "
                + countInMainInventory(p, ModItems.REDSTONE_CELL.get()) + " in the inventory, off hand "
                + p.getOffhandItem() + ", on the ground " + itemsIn(helper));
        helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_REDSTONE) == 1 && statusMessages(p) == 1,
                "status messages after the swap: " + statusMessages(p));

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Con la polvere in mano il clic ne sposta quante ne stanno nello slot (fino a 64), senza suono, e dice lo stato;
     * a slot pieno un altro clic non sposta nulla e dice ancora lo stato.
     */
    private static void dustClickMovesWhatFits(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        c.setDust(redstone(60));
        p.setItemInHand(InteractionHand.MAIN_HAND, redstone(10));

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            InteractionResult first = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(first == InteractionResult.SUCCESS, "the click that moved dust gave " + first);
            assertDust(helper, c, p, "after the first click", 64, 6);
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1 && statusMessages(p) == 1,
                    "status messages after the first click: " + statusMessages(p));

            InteractionResult second = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(second == InteractionResult.CONSUME, "the click that moved nothing gave " + second);
            assertDust(helper, c, p, "after the second click", 64, 6);
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 2 && statusMessages(p) == 2,
                    "status messages after the second click: " + statusMessages(p));
            helper.assertTrue(sounds.sounds().isEmpty(), "the dust clicks played " + sounds.sounds());
        }

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * A mano vuota, in piedi, il clic prende la cella (con la sua carica, e suona il ritiro); solo quando la cella non
     * c'e' piu' prende la polvere, tutta, senza suono.
     */
    private static void emptyHandTakesTheCellThenTheDust(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        c.setCell(cell(10_000));
        c.setDust(redstone(5));

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            InteractionResult first = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(first == InteractionResult.SUCCESS, "the click that took the cell gave " + first);
            ItemStack held = p.getMainHandItem();
            helper.assertTrue(held.is(ModItems.REDSTONE_CELL.get()) && held.getCount() == 1
                    && energy(held) == 10_000, "in hand after the first click: " + held + " with " + energy(held)
                    + " RE, not one cell with 10000");
            helper.assertTrue(c.cell().isEmpty() && c.dust().getCount() == 5,
                    "charger after the first click: cell " + c.cell() + ", dust " + c.dust().getCount());
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CHARGER_TAKE)),
                    "taking the cell played " + sounds.sounds() + ", expected [CHARGER_TAKE]");
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1 && statusMessages(p) == 1,
                    "status messages after the first click: " + statusMessages(p));

            p.getInventory().items.set(5, held);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            InteractionResult second = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(second == InteractionResult.SUCCESS, "the click that took the dust gave " + second);
            helper.assertTrue(p.getMainHandItem().is(Items.REDSTONE) && p.getMainHandItem().getCount() == 5,
                    "in hand after the second click: " + p.getMainHandItem() + ", not 5 redstone");
            helper.assertTrue(c.dust().isEmpty() && c.cell().isEmpty(),
                    "charger after the second click: cell " + c.cell() + ", dust " + c.dust());
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CHARGER_TAKE)),
                    "taking the dust played a sound: " + sounds.sounds());
            helper.assertTrue(countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 1
                    && energy(p.getInventory().items.get(5)) == 10_000, "the taken cell left slot 5 or changed");
        }

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Accovacciati a mani vuote il clic non prende nulla: dice solo lo stato, uno dei quattro secondo il contenuto, con
     * la percentuale per difetto quando e' in carica.
     */
    private static void sneakingWithEmptyHandsOnlyTellsTheStatus(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        c.setCell(cell(2_500));
        c.setDust(redstone(3));
        p.setShiftKeyDown(true);

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            InteractionResult result = clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.CONSUME, "the sneaking click gave " + result);
            helper.assertTrue(energy(c.cell()) == 2_500 && c.cell().getCount() == 1 && c.dust().getCount() == 3
                    && p.getMainHandItem().isEmpty(), "the sneaking click moved something: cell " + c.cell()
                    + " with " + energy(c.cell()) + " RE, dust " + c.dust().getCount() + ", in hand "
                    + p.getMainHandItem());
            helper.assertTrue(messages(p, ChargerBlock.STATUS_CHARGING) == 1 && statusMessages(p) == 1,
                    "status messages while charging: " + statusMessages(p));
            helper.assertTrue(Integer.valueOf(25).equals(lastArg(p, ChargerBlock.STATUS_CHARGING)),
                    "charging percent " + lastArg(p, ChargerBlock.STATUS_CHARGING) + ", not 25");

            c.setCell(cell(10_000));
            clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(p, ChargerBlock.STATUS_FULL) == 1 && statusMessages(p) == 2,
                    "status messages with a full cell: " + statusMessages(p));

            c.setCell(cell(100));
            c.setDust(ItemStack.EMPTY);
            clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_REDSTONE) == 1 && statusMessages(p) == 3,
                    "status messages without redstone: " + statusMessages(p));
            helper.assertTrue(energy(c.cell()) == 100 && p.getMainHandItem().isEmpty(),
                    "the sneaking click took the cell: " + c.cell() + ", in hand " + p.getMainHandItem());

            c.setCell(ItemStack.EMPTY);
            clickBlock(helper, p, POS, InteractionHand.MAIN_HAND);
            helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_CELL) == 1 && statusMessages(p) == 4,
                    "status messages without a cell: " + statusMessages(p));
            helper.assertTrue(sounds.sounds().isEmpty(), "the sneaking clicks played " + sounds.sounds());
        }

        p.setShiftKeyDown(false);
        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Dove il design dice "passa" nulla si muove e nulla si dice: con un altro oggetto nella mano principale, con la
     * mano secondaria (anche se tiene una cella), da spettatore.
     */
    private static void otherItemsAndTheOffHandPass(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        c.setCell(cell(1_000));

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the click with a stick was consumed");
        helper.assertTrue(p.getMainHandItem().is(Items.STICK) && p.getMainHandItem().getCount() == 1,
                "the stick changed: " + p.getMainHandItem());
        assertUntouched(helper, c, p, "after the click with a stick");

        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, cell(5_000));
        helper.assertTrue(!clickBlock(helper, p, POS, InteractionHand.OFF_HAND).consumesAction(),
                "the off-hand click with a cell was consumed");
        helper.assertTrue(p.getMainHandItem().isEmpty() && p.getOffhandItem().getCount() == 1
                && energy(p.getOffhandItem()) == 5_000, "hands after the off-hand click: main " + p.getMainHandItem()
                + ", off " + p.getOffhandItem() + " with " + energy(p.getOffhandItem()) + " RE");
        assertUntouched(helper, c, p, "after the off-hand click");

        // da spettatore il gioco non arriva al blocco: qui si chiama il metodo neutro del clic
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        p.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(p.isSpectator(), "premise: the mock player is not a spectator");
        BlockPos abs = helper.absolutePos(POS);
        InteractionResult result = ModBlocks.CHARGER.get().onClick(stateAt(helper, POS), helper.getLevel(), abs, p,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(result == InteractionResult.PASS, "the click of a spectator gave " + result);
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the spectator took " + p.getMainHandItem());
        assertUntouched(helper, c, p, "after the click of a spectator");

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * In creativa ({@code instabuild}) l'oggetto in mano non si consuma: la cella entra e resta anche in mano, nello
     * scambio la cella tolta arriva comunque nell'inventario, e la polvere entra senza calare in mano.
     */
    private static void creativeHandIsNotConsumed(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(p.getAbilities().instabuild, "premise: the creative mock player has no instabuild");
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, cell(4_000));

        helper.assertTrue(clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the creative click with a cell was not consumed");
        helper.assertTrue(!c.cell().isEmpty() && energy(c.cell()) == 4_000,
                "cell in the charger after the creative insert: " + c.cell() + " with " + energy(c.cell()) + " RE");
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(held.is(ModItems.REDSTONE_CELL.get()) && held.getCount() == 1 && energy(held) == 4_000,
                "creative hand after the insert: " + held + " with " + energy(held) + " RE");
        helper.assertTrue(held != c.cell(), "the charger holds the very stack in the creative hand");

        helper.assertTrue(clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the creative swapping click was not consumed");
        helper.assertTrue(countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 2,
                "cells of the creative player after the swap: "
                        + countInMainInventory(p, ModItems.REDSTONE_CELL.get()) + ", not 2");
        helper.assertTrue(p.getMainHandItem().is(ModItems.REDSTONE_CELL.get()) && energy(p.getMainHandItem()) == 4_000,
                "creative hand after the swap: " + p.getMainHandItem());
        helper.assertTrue(!c.cell().isEmpty() && c.cell().getCount() == 1 && energy(c.cell()) == 4_000
                && itemsIn(helper).isEmpty(), "after the creative swap: charger " + c.cell() + ", on the ground "
                + itemsIn(helper));

        p.setItemInHand(InteractionHand.MAIN_HAND, redstone(10));
        helper.assertTrue(c.dust().isEmpty(), "premise: the charger already holds dust");
        helper.assertTrue(clickBlock(helper, p, POS, InteractionHand.MAIN_HAND).consumesAction(),
                "the creative click with dust was not consumed");
        assertDust(helper, c, p, "after the creative click with dust", 10, 10);

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Due giocatori, un caricatore: la cella che uno mette la prende l'altro, e al primo non resta nulla da prendere.
     * Dopo ogni clic la cella e' una sola, nel caricatore o in una mano.
     */
    private static void twoPlayersOneCell(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        ServerPlayer a = Compat.gameTests().survivalPlayer(helper);
        ServerPlayer b = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, a, STAND);
        standFacingNorth(helper, b, new Vec3(2.5, 2.0, 5.5));
        a.setItemInHand(InteractionHand.MAIN_HAND, cell(1_000));
        assertOneCell(helper, c, a, b, "premise");

        clickBlock(helper, a, POS, InteractionHand.MAIN_HAND);
        helper.assertTrue(!c.cell().isEmpty() && energy(c.cell()) == 1_000 && a.getMainHandItem().isEmpty(),
                "after the click of a: charger " + c.cell() + ", hand of a " + a.getMainHandItem());
        assertOneCell(helper, c, a, b, "after the click of a");

        clickBlock(helper, b, POS, InteractionHand.MAIN_HAND);
        helper.assertTrue(c.cell().isEmpty() && b.getMainHandItem().is(ModItems.REDSTONE_CELL.get())
                && energy(b.getMainHandItem()) == 1_000, "after the click of b: charger " + c.cell()
                + ", hand of b " + b.getMainHandItem());
        assertOneCell(helper, c, a, b, "after the click of b");

        clickBlock(helper, a, POS, InteractionHand.MAIN_HAND);
        helper.assertTrue(a.getMainHandItem().isEmpty() && c.cell().isEmpty(),
                "a took something from the empty charger: " + a.getMainHandItem());
        helper.assertTrue(messages(a, ChargerBlock.STATUS_NO_CELL) == 1,
                "no-cell messages of a: " + messages(a, ChargerBlock.STATUS_NO_CELL));
        assertOneCell(helper, c, a, b, "after the second click of a");

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * Una tramoggia sopra il caricatore con due celle in coda: ne entra una sola, con la sua carica, e l'altra resta
     * nella tramoggia. Le celle restano due.
     */
    private static void hopperAboveInsertsOneCell(GameTestHelper helper) {
        Item cellItem = ModItems.REDSTONE_CELL.get();
        ChargerBlockEntity c = charger(helper, POS);
        HopperBlockEntity hopper = hopper(helper, POS.above(), Direction.DOWN);
        hopper.setItem(0, cell(100));
        hopper.setItem(1, cell(200));
        helper.runAfterDelay(20, () -> runInSequence(helper, h -> {
            h.assertTrue(c.cell().is(cellItem) && c.cell().getCount() == 1 && energy(c.cell()) == 100,
                    "cell in the charger after 20 ticks: " + c.cell() + " with " + energy(c.cell())
                            + " RE, not one cell with 100");
            h.assertTrue(count(hopper, cellItem) == 1 && hopper.getItem(1).is(cellItem)
                    && energy(hopper.getItem(1)) == 200, "cells left in the hopper: " + count(hopper, cellItem)
                    + ", second slot " + hopper.getItem(1) + " with " + energy(hopper.getItem(1))
                    + " RE; expected one cell with 200");
            h.assertTrue(count(c, cellItem) + count(hopper, cellItem) == 2 && itemsIn(h).isEmpty(),
                    "cells in the charger " + count(c, cellItem) + ", in the hopper " + count(hopper, cellItem)
                            + ", on the ground " + itemsIn(h) + "; expected two cells in all");
            hopper.clearContent();
            h.setBlock(POS.above(), Blocks.AIR);
            removeCharger(h, POS);
            h.succeed();
        }));
    }

    /**
     * Una tramoggia di lato porta la polvere nello slot 1 anche quando non e' vuoto (lo stack cresce sul posto), e
     * nessuna polvere si perde; la pietra e la cella della tramoggia non entrano da un lato.
     */
    private static void hopperAtTheSideAddsDustToANonEmptySlot(GameTestHelper helper) {
        Item cellItem = ModItems.REDSTONE_CELL.get();
        ChargerBlockEntity c = charger(helper, POS);
        c.setDust(redstone(3));
        HopperBlockEntity hopper = hopper(helper, POS.west(), Direction.EAST);
        hopper.setItem(0, redstone(5));
        hopper.setItem(1, new ItemStack(Items.COBBLESTONE));
        hopper.setItem(2, cell(300));
        helper.runAfterDelay(30, () -> runInSequence(helper, h -> {
            int inside = c.dust().getCount();
            int left = count(hopper, Items.REDSTONE);
            h.assertTrue(inside > 3, "dust in the charger after 30 ticks: " + inside + ", not more than 3");
            h.assertTrue(inside + left == 8, "dust in the charger " + inside + " and in the hopper " + left
                    + ", not 8 in all");
            h.assertTrue(c.cell().isEmpty(), "a cell entered from the side: " + c.cell());
            h.assertTrue(count(hopper, Items.COBBLESTONE) == 1 && count(hopper, cellItem) == 1
                    && energy(hopper.getItem(2)) == 300, "the hopper lost its cobblestone or its cell: cobblestone "
                    + count(hopper, Items.COBBLESTONE) + ", cells " + count(hopper, cellItem) + ", third slot "
                    + hopper.getItem(2));
            h.assertTrue(itemsIn(h).isEmpty(), "items on the ground: " + itemsIn(h));
            hopper.clearContent();
            h.setBlock(POS.west(), Blocks.AIR);
            removeCharger(h, POS);
            h.succeed();
        }));
    }

    /**
     * Una tramoggia sotto il caricatore prende la cella solo quando e' piena: a 9 999 RE resta dentro, a 10 000 scende.
     * La polvere da sotto non esce mai, e senza cella lo stato del blocco dice {@code cell} falso. Le tre attese sono
     * fissate all'inizio, a 20, 40 e 60 tick.
     */
    private static void hopperBelowTakesOnlyAFullCell(GameTestHelper helper) {
        Item cellItem = ModItems.REDSTONE_CELL.get();
        BlockPos above = POS.above();
        HopperBlockEntity hopper = hopper(helper, POS, Direction.DOWN);
        ChargerBlockEntity c = charger(helper, above);
        c.setCell(cell(9_999));
        helper.runAfterDelay(20, () -> runInSequence(helper, h -> {
            h.assertTrue(c.cell().is(cellItem) && c.cell().getCount() == 1 && energy(c.cell()) == 9_999,
                    "cell one RE short of full: charger " + c.cell() + " with " + energy(c.cell()) + " RE");
            h.assertTrue(hopper.isEmpty(), "the hopper took a cell that is not full: " + count(hopper, cellItem));
            c.setCell(cell(10_000));
        }));
        helper.runAfterDelay(40, () -> runInSequence(helper, h -> {
            h.assertTrue(c.cell().isEmpty(), "the full cell is still in the charger: " + c.cell());
            h.assertTrue(count(hopper, cellItem) == 1 && hopper.getItem(0).is(cellItem)
                    && energy(hopper.getItem(0)) == 10_000, "full cell in the hopper: " + count(hopper, cellItem)
                    + " cells, first slot " + hopper.getItem(0) + " with " + energy(hopper.getItem(0)) + " RE");
            c.setDust(redstone(5));
        }));
        helper.runAfterDelay(60, () -> runInSequence(helper, h -> {
            h.assertTrue(c.dust().getCount() == 5 && count(hopper, Items.REDSTONE) == 0,
                    "dust in the charger " + c.dust().getCount() + ", in the hopper "
                            + count(hopper, Items.REDSTONE) + "; expected 5 and 0");
            h.assertTrue(count(hopper, cellItem) == 1 && c.cell().isEmpty() && itemsIn(h).isEmpty(),
                    "cells in the hopper " + count(hopper, cellItem) + ", charger " + c.cell()
                            + ", on the ground " + itemsIn(h));
            BlockState state = stateAt(h, above);
            h.assertTrue(state.is(ModBlocks.CHARGER.get()) && !state.getValue(ChargerBlock.CELL),
                    "block state without the cell: " + state);
            hopper.clearContent();
            h.setBlock(POS, Blocks.AIR);
            removeCharger(h, above);
            h.succeed();
        }));
    }

    /**
     * Una tramoggia sotto che non ha posto per la cella (cinque slot con una pietra ciascuno: nessuno libero, nulla con
     * cui unirla, ma non piena di pile intere, quindi tira) a ogni tick la toglie e la rimette: la cella resta una,
     * con la sua carica, e nulla finisce nella tramoggia o a terra. Il secondo caricatore tiene una cella letta dal
     * chunk con piu' della capacita' della config: il rimettere non la taglia.
     */
    private static void fullHopperBelowNeitherLosesNorDuplicatesTheCell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item cellItem = ModItems.REDSTONE_CELL.get();
        BlockPos above = POS.above();
        BlockPos hopperPos2 = new BlockPos(5, 2, 3);
        BlockPos chargerPos2 = new BlockPos(5, 3, 3);

        HopperBlockEntity hopper = hopper(helper, POS, Direction.DOWN);
        HopperBlockEntity hopper2 = hopper(helper, hopperPos2, Direction.DOWN);
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            hopper.setItem(slot, new ItemStack(Items.COBBLESTONE));
            hopper2.setItem(slot, new ItemStack(Items.COBBLESTONE));
        }

        ChargerBlockEntity c = charger(helper, above);
        c.setCell(cell(10_000));
        ItemStack before = c.cell();

        charger(helper, chargerPos2);
        BlockPos abs2 = helper.absolutePos(chargerPos2);
        CompoundTag saved = Compat.gameTests().saveBlockEntity(level, abs2);
        saved.putBoolean(NbtKeys.CHARGER_HAS_CELL, true);
        saved.putInt(NbtKeys.CHARGER_CELL_ENERGY, 40_000);
        ChargerBlockEntity c2 = (ChargerBlockEntity) Compat.gameTests().loadBlockEntity(level, abs2, saved);
        ItemStack before2 = c2.cell();
        helper.assertTrue(energy(before) == 10_000 && energy(before2) == 40_000, "premise: cells with "
                + energy(before) + " and " + energy(before2) + " RE, not 10000 and 40000");

        helper.runAfterDelay(20, () -> runInSequence(helper, h -> {
            h.assertTrue(c.cell() != before, "the hopper never took the full cell out and put it back");
            h.assertTrue(c.cell().is(cellItem) && c.cell().getCount() == 1 && energy(c.cell()) == 10_000,
                    "cell put back by the hopper: " + c.cell() + " with " + energy(c.cell()) + " RE");
            h.assertTrue(count(hopper, Items.COBBLESTONE) == 5 && count(hopper, cellItem) == 0,
                    "hopper with no room: cobblestone " + count(hopper, Items.COBBLESTONE) + ", cells "
                            + count(hopper, cellItem));

            h.assertTrue(c2.cell() != before2, "the second hopper never took the cell out and put it back");
            h.assertTrue(c2.cell().is(cellItem) && c2.cell().getCount() == 1 && energy(c2.cell()) == 40_000,
                    "cell above the capacity put back by the hopper: " + c2.cell() + " with " + energy(c2.cell())
                            + " RE, not 40000");
            h.assertTrue(count(hopper2, Items.COBBLESTONE) == 5 && count(hopper2, cellItem) == 0,
                    "second hopper with no room: cobblestone " + count(hopper2, Items.COBBLESTONE) + ", cells "
                            + count(hopper2, cellItem));

            h.assertTrue(itemsIn(h).isEmpty(), "items on the ground: " + itemsIn(h));
            hopper.clearContent();
            hopper2.clearContent();
            h.setBlock(POS, Blocks.AIR);
            h.setBlock(hopperPos2, Blocks.AIR);
            removeCharger(h, above);
            removeCharger(h, chargerPos2);
            h.succeed();
        }));
    }

    /**
     * Facce e slot, senza tick: sopra e sotto lo slot 0, ai lati lo slot 1. Entra solo una cella da sopra nello slot 0
     * vuoto e solo polvere di redstone dai lati nello slot 1, finche' lo slot ne tiene meno di 64 (con 63 entra, con
     * 64 no); esce solo da sotto la cella piena.
     */
    private static void facesAcceptOnlyWhatTheyShould(GameTestHelper helper) {
        ChargerBlockEntity c = charger(helper, POS);
        List<Direction> sides = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        ItemStack dust = redstone(1);

        helper.assertTrue(Arrays.equals(c.getSlotsForFace(Direction.UP), new int[] {0})
                && Arrays.equals(c.getSlotsForFace(Direction.DOWN), new int[] {0}), "slots from above "
                + Arrays.toString(c.getSlotsForFace(Direction.UP)) + ", from below "
                + Arrays.toString(c.getSlotsForFace(Direction.DOWN)) + "; expected [0] and [0]");
        for (Direction side : sides) {
            helper.assertTrue(Arrays.equals(c.getSlotsForFace(side), new int[] {1}), "slots from " + side + ": "
                    + Arrays.toString(c.getSlotsForFace(side)) + ", not [1]");
            helper.assertTrue(c.canPlaceItemThroughFace(1, dust, side), "dust refused from " + side);
        }

        helper.assertTrue(c.canPlaceItemThroughFace(0, cell(5), Direction.UP),
                "a cell refused from above by an empty charger");
        helper.assertTrue(!c.canPlaceItemThroughFace(0, cell(5), Direction.NORTH), "a cell accepted from a side");
        helper.assertTrue(!c.canPlaceItemThroughFace(0, cell(5), Direction.DOWN), "a cell accepted from below");
        helper.assertTrue(!c.canPlaceItemThroughFace(0, dust, Direction.UP), "dust accepted in the cell slot");
        helper.assertTrue(!c.canPlaceItemThroughFace(1, dust, Direction.UP), "dust accepted from above");
        helper.assertTrue(!c.canPlaceItemThroughFace(1, dust, Direction.DOWN), "dust accepted from below");
        helper.assertTrue(!c.canPlaceItemThroughFace(1, cell(5), Direction.NORTH), "a cell accepted in the dust slot");
        helper.assertTrue(!c.canPlaceItemThroughFace(1, new ItemStack(Items.STICK), Direction.NORTH),
                "a stick accepted in the dust slot");
        ItemStack twoCells = cell(5);
        twoCells.setCount(2);
        helper.assertTrue(c.canPlaceItem(0, cell(5)) && !c.canPlaceItem(0, twoCells),
                "one cell must fit the empty cell slot, a stack of two must not");

        c.setCell(cell(9_999));
        helper.assertTrue(!c.canPlaceItemThroughFace(0, cell(5), Direction.UP),
                "a second cell accepted from above");
        helper.assertTrue(!c.canTakeItemThroughFace(0, c.cell(), Direction.DOWN),
                "a cell with 9999 RE can be taken from below");
        c.setCell(cell(10_000));
        helper.assertTrue(c.canTakeItemThroughFace(0, c.cell(), Direction.DOWN),
                "a full cell cannot be taken from below");
        helper.assertTrue(!c.canTakeItemThroughFace(0, c.cell(), Direction.UP), "a full cell can be taken from above");
        c.setDust(redstone(5));
        helper.assertTrue(!c.canTakeItemThroughFace(1, c.dust(), Direction.DOWN), "dust can be taken from below");
        helper.assertTrue(!c.canTakeItemThroughFace(1, c.dust(), Direction.NORTH), "dust can be taken from a side");

        c.setDust(redstone(63));
        helper.assertTrue(c.canPlaceItem(1, dust), "dust refused by a dust slot that holds 63");
        for (Direction side : sides) {
            helper.assertTrue(c.canPlaceItemThroughFace(1, dust, side), "dust refused from " + side
                    + " with 63 inside");
        }
        c.setDust(redstone(64));
        helper.assertTrue(!c.canPlaceItem(1, dust), "dust accepted by a dust slot that holds 64");
        for (Direction side : sides) {
            helper.assertTrue(!c.canPlaceItemThroughFace(1, dust, side), "dust accepted from " + side
                    + " with 64 inside");
        }

        removeCharger(helper, POS);
        helper.succeed();
    }

    /**
     * La rottura fa cadere la cella, con la sua carica, e la polvere, una volta sola, al centro del blocco e ferme;
     * la riserva non si rimborsa e senza bottino il blocco non cade. Un caricatore svuotato prima di sparire, come fa
     * {@code /setblock}, non lascia nulla.
     */
    private static void breakingDropsTheCellAndTheDust(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        Vec3 center = Vec3.atCenterOf(abs);
        ChargerBlockEntity c = charger(helper, POS);
        c.setCell(cell(4_321));
        c.setDust(redstone(9));
        tick(helper, c, 1);
        assertContents(helper, c, "before breaking", 4_323, 8, 498);

        helper.assertTrue(level.destroyBlock(abs, false), "destroyBlock refused the charger");
        helper.assertTrue(stateAt(helper, POS).isAir() && level.getBlockEntity(abs) == null,
                "the broken charger left " + stateAt(helper, POS) + " and " + level.getBlockEntity(abs));
        List<ItemEntity> dropped = itemsIn(helper);
        helper.assertTrue(dropped.size() == 2, "the broken charger dropped " + dropped.size() + " stacks, not 2: "
                + dropped);
        int cells = 0;
        int dusts = 0;
        for (ItemEntity drop : dropped) {
            ItemStack stack = drop.getItem();
            if (stack.is(ModItems.REDSTONE_CELL.get())) {
                helper.assertTrue(stack.getCount() == 1 && energy(stack) == 4_323, "dropped cell " + stack + " with "
                        + energy(stack) + " RE, not one cell with 4323");
                cells++;
            } else {
                helper.assertTrue(stack.is(Items.REDSTONE) && stack.getCount() == 8, "dropped " + stack
                        + ", not 8 redstone dust");
                dusts++;
            }
            helper.assertTrue(drop.position().distanceTo(center) < 1e-6
                    && drop.getDeltaMovement().equals(Vec3.ZERO), "dropped " + stack + " not lying at the centre "
                    + center + ": " + drop.position() + " moving " + drop.getDeltaMovement());
        }
        helper.assertTrue(cells == 1 && dusts == 1, "dropped " + cells + " cell stacks and " + dusts
                + " dust stacks, not one each");
        dropped.forEach(ItemEntity::discard);

        ChargerBlockEntity second = charger(helper, POS);
        second.setCell(cell(1_000));
        second.setDust(redstone(3));
        Clearable.tryClear(level.getBlockEntity(abs));
        helper.assertTrue(second.cell().isEmpty() && second.dust().isEmpty(), "a cleared charger still holds "
                + second.cell() + " and " + second.dust());
        helper.setBlock(POS, Blocks.AIR);
        helper.assertTrue(itemsIn(helper).isEmpty(), "a cleared charger dropped " + itemsIn(helper));
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

    /**
     * Posa una tramoggia rivolta verso {@code facing} (in basso o un lato) nel punto relativo alla struttura e
     * restituisce la sua block entity, letta dal livello.
     */
    private static HopperBlockEntity hopper(GameTestHelper helper, BlockPos relative, Direction facing) {
        helper.setBlock(relative, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing));
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(entity instanceof HopperBlockEntity, "block entity of a new hopper: " + entity);
        return (HopperBlockEntity) entity;
    }

    /** Quanti oggetti di quel tipo tiene il contenitore, sommati su tutti gli slot. */
    private static int count(Container c, Item item) {
        int total = 0;
        for (int slot = 0; slot < c.getContainerSize(); slot++) {
            ItemStack stack = c.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
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

    /** Quanti messaggi di stato del caricatore ({@link #STATUS_KEYS}) ha avuto il giocatore finto, in tutto. */
    private static int statusMessages(ServerPlayer p) {
        int count = 0;
        for (String key : STATUS_KEYS) {
            count += messages(p, key);
        }
        return count;
    }

    /**
     * Il primo argomento dell'ultimo messaggio con quella chiave arrivato al giocatore finto; null se non ne ha avuti o
     * se l'ultimo non ha argomenti.
     */
    private static Object lastArg(ServerPlayer p, String key) {
        Object arg = null;
        for (Component message : Compat.gameTests().messagesTo(p)) {
            if (message.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)) {
                arg = contents.getArgs().length > 0 ? contents.getArgs()[0] : null;
            }
        }
        return arg;
    }

    /** Le polveri nel caricatore e quelle nella mano principale del giocatore sono quelle attese. */
    private static void assertDust(GameTestHelper helper, ChargerBlockEntity c, ServerPlayer p, String when,
            int inCharger, int inHand) {
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(c.dust().getCount() == inCharger && held.is(Items.REDSTONE) && held.getCount() == inHand,
                when + ": dust in the charger " + c.dust().getCount() + ", in hand " + held + "; expected "
                        + inCharger + " and " + inHand);
    }

    /**
     * Il clic e' passato senza toccare nulla: il caricatore tiene ancora la sua cella da 1 000 RE e nessuna polvere, il
     * giocatore non ha avuto nessun messaggio di stato e nulla e' a terra.
     */
    private static void assertUntouched(GameTestHelper helper, ChargerBlockEntity c, ServerPlayer p, String when) {
        helper.assertTrue(!c.cell().isEmpty() && c.cell().getCount() == 1 && energy(c.cell()) == 1_000
                && c.dust().isEmpty(), when + ": charger cell " + c.cell() + " with " + energy(c.cell())
                + " RE, dust " + c.dust());
        helper.assertTrue(statusMessages(p) == 0, when + ": status messages " + statusMessages(p));
        helper.assertTrue(itemsIn(helper).isEmpty(), when + ": items on the ground " + itemsIn(helper));
    }

    /**
     * Fra il caricatore, i due giocatori (inventario principale e mano secondaria) e il terreno della struttura c'e'
     * una cella sola.
     */
    private static void assertOneCell(GameTestHelper helper, ChargerBlockEntity c, ServerPlayer a, ServerPlayer b,
            String when) {
        int cells = c.cell().isEmpty() ? 0 : c.cell().getCount();
        for (ServerPlayer player : List.of(a, b)) {
            cells += countInMainInventory(player, ModItems.REDSTONE_CELL.get());
            if (player.getOffhandItem().is(ModItems.REDSTONE_CELL.get())) {
                cells += player.getOffhandItem().getCount();
            }
        }
        for (ItemEntity dropped : itemsIn(helper)) {
            if (dropped.getItem().is(ModItems.REDSTONE_CELL.get())) {
                cells += dropped.getItem().getCount();
            }
        }
        helper.assertTrue(cells == 1, when + ": " + cells + " cells among the charger and the two players, not 1");
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
