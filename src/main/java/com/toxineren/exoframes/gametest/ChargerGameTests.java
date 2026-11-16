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
