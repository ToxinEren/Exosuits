package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ChargerBlock;
import com.toxineren.exoframes.content.ChargerBlockEntity;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.ChargerRule.Status;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.CellSwap;
import com.toxineren.exoframes.frame.EntryHints;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.Handouts;
import com.toxineren.exoframes.frame.SuitTick;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * La prova in mondo "dall'inizio alla fine" di design §12.15, quella che chiude il "fatta quando" della parte 3c: una
 * partita intera di un giocatore in sopravvivenza che parte dalle sole materie prime, in un test solo. I passi:
 * <ol>
 * <li><b>fabbricare</b> al banco da lavoro, con le forme di design §12.12, un telaio, i sei pezzi Rottame, due celle e
 * un caricatore: 34 lingotti di ferro, 14 di rame, 2 blocchi di redstone, 2 quarzi e 4 polveri di redstone, e delle 12
 * polveri ne restano 8;</li>
 * <li><b>piazzare il telaio</b> e provare a entrarci: senza una cella carica non si entra;</li>
 * <li><b>caricare</b> le due celle al caricatore: la prima con 2 polveri (400 tick, 1 000 RE), la seconda con 4 (800
 * tick, 2 000 RE);</li>
 * <li><b>armare</b> il telaio con i sei pezzi e la cella da 1 000 RE, ed <b>entrare</b>;</li>
 * <li><b>consumare</b>: il tick di preparazione e venti passi da 0,2 blocchi tolgono 8 RE (992), e sotto il 10 % la
 * tuta lo dice;</li>
 * <li><b>prendere danno</b>: un colpo di cactus da 8 lascia elmo, braccia e gambe a 179 e il torso a 178;</li>
 * <li><b>cambiare cella</b> dall'interno: quella da 2 000 RE entra, quella da 992 torna nell'inventario;</li>
 * <li><b>uscire e ricaricare</b> la cella da 992 RE con le ultime 2 polveri: 1 992 RE;</li>
 * <li><b>contare</b>: due celle in tutto (2 000 RE nel telaio piazzato, 1 992 in mano), nessuna polvere, nulla a
 * terra, e 3 992 RE = 8 polveri x 500 - 8 consumati. Nulla si e' perso e nulla si e' duplicato.</li>
 * </ol>
 * Nessuna scorciatoia di debug: niente {@code setStored}, {@code wearScrap}, {@code DebugWear}. Tutto cio' che il
 * giocatore tiene nasce da una ricetta, e ogni gesto e' quello del gioco: il clic vero sui blocchi
 * ({@code clickBlock}), il clic sul telaio ({@code click}), l'oggetto telaio usato sul pavimento, il tick dell'energia,
 * il cambio cella e l'uscita chiamati come li chiamano l'evento del tick e i pacchetti dei tasti. Il solo tempo
 * accorciato e' quello del caricatore, il cui tick si chiama a mano ({@code tick}) come in {@code ChargerGameTests}:
 * tutta la partita sta in un tick del server.
 * <p>
 * Regola di tutto il test: fra un passo e l'altro la mano principale e' vuota, e nulla entra in mano se non con
 * {@code hold}, {@code holdCell}, un clic o {@code useExoframeItem}. Le materie prime si mettono negli slot da 9 a 13
 * e mai con {@code Inventory.add}, che riempirebbe per primo lo slot in mano (lo 0). Un fallimento dice in testa al
 * messaggio a quale passo e' arrivata la partita. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class SurvivalGameTests {
    /** Lato del pavimento di pietra, da 1 a questo numero lungo x e z, a quota 0 della struttura. */
    private static final int FLOOR_SIDE = 6;

    /** Dove sta in piedi il giocatore all'inizio, relativo alla struttura. */
    private static final Vec3 START = new Vec3(1.5, 1.0, 1.5);

    /** Il blocco del pavimento su cui il giocatore usa l'oggetto telaio. */
    private static final BlockPos FRAME_FLOOR = new BlockPos(2, 0, 3);

    /** I piedi del telaio appena piazzato, sopra {@link #FRAME_FLOOR}: da qui parte la camminata. */
    private static final Vec3 FRAME_FEET = new Vec3(2.5, 1.0, 3.5);

    /** Dove finisce la camminata e resta il telaio dopo l'uscita: sopra il pavimento, lontano dal caricatore. */
    private static final Vec3 EXIT_FEET = new Vec3(6.5, 1.0, 3.5);

    /** Il blocco del pavimento su cui il giocatore posa il caricatore. */
    private static final BlockPos CHARGER_FLOOR = new BlockPos(6, 0, 6);

    /** Dove sta il caricatore posato, sopra {@link #CHARGER_FLOOR}. */
    private static final BlockPos CHARGER_POS = CHARGER_FLOOR.above();

    /** Il primo slot delle materie prime: fuori dalla barra rapida, lontano dallo slot in mano. */
    private static final int FIRST_RAW_SLOT = 9;

    /** I punti del clic sul telaio, relativi ai suoi piedi: elmo, torso (anche cella e ingresso), braccia, gambe. */
    private static final Vec3 HELMET_POINT = new Vec3(0.0, 2.5, 0.0);
    private static final Vec3 TORSO_POINT = new Vec3(0.0, 1.6, 0.0);
    private static final Vec3 ARM_POINT = new Vec3(-0.35, 1.6, 0.0);
    private static final Vec3 LEG_POINT = new Vec3(-0.35, 0.5, 0.0);

    /** I tick che il caricatore mette a bruciare una polvere con i predefiniti: 500 RE a 2,5 RE per tick. */
    private static final int TICKS_PER_DUST = 200;

    /** I passi della camminata e la loro lunghezza in blocchi: un secondo di cammino, da x 2,5 a x 6,5. */
    private static final int WALK_STEPS = 20;
    private static final double WALK_STEP = 0.2;

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public SurvivalGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "survivalFromCraftingToRecharging", SurvivalGameTests::survivalFromCraftingToRecharging);
        return tests;
    }

    /**
     * La partita intera, un passo dopo l'altro (vedi il commento della classe). Ogni fallimento, anche un'eccezione
     * che non viene da un'asserzione, esce con il nome del passo in testa al messaggio.
     */
    private static void survivalFromCraftingToRecharging(GameTestHelper helper) {
        String step = "preparation";
        try {
            ServerPlayer p = prepare(helper);
            step = "step 1 (crafting)";
            craftEverything(helper, p);
            step = "step 2 (no entry without a charged cell)";
            ExoframeEntity frame = placeTheFrameAndGetRefused(helper, p);
            step = "step 3 (charging)";
            ChargerBlockEntity charger = placeTheChargerAndChargeTwoCells(helper, p);
            step = "step 4 (arming and entering)";
            UUID frameId = armAndEnter(helper, p, frame);
            step = "step 5 (draining)";
            long t0 = walkAndDrain(helper, p);
            step = "step 6 (taking damage)";
            takeACactusHit(helper, p);
            step = "step 7 (swapping the cell)";
            swapTheCell(helper, p, t0);
            step = "step 8 (leaving and recharging)";
            ExoframeEntity left = leaveAndRecharge(helper, p, charger, frameId);
            step = "step 9 (nothing lost, nothing duplicated)";
            countEverything(helper, p, charger, left);
            step = "cleanup";
            removeFrame(helper.getLevel().getServer(), left);
            charger.setCell(ItemStack.EMPTY);
            charger.setDust(ItemStack.EMPTY);
            helper.setBlock(CHARGER_POS, Blocks.AIR);
            p.getInventory().clearContent();
        } catch (GameTestAssertException e) {
            throw atStep(step, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw atStep(step, e.toString(), e);
        }
        helper.succeed();
    }

    /**
     * Preparazione: il pavimento di pietra, il giocatore in sopravvivenza in piedi in {@link #START} rivolto a sud
     * (verso il pavimento), e nell'inventario solo le materie prime, ciascuna nel suo slot da {@link #FIRST_RAW_SLOT}
     * in poi. Premesse: lo slot in mano e' lo 0, la mano e' vuota e l'inventario era vuoto.
     */
    private static ServerPlayer prepare(GameTestHelper helper) {
        for (int x = 1; x <= FLOOR_SIDE; x++) {
            for (int z = 1; z <= FLOOR_SIDE; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        Vec3 feet = helper.absoluteVec(START);
        p.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        Inventory inventory = p.getInventory();
        helper.assertTrue(inventory.selected == 0 && p.getMainHandItem().isEmpty() && inventory.isEmpty(),
                "premise: a new player has slot " + inventory.selected + " selected, holds " + p.getMainHandItem()
                        + " and its inventory is " + (inventory.isEmpty() ? "empty" : "not empty"));
        List<ItemStack> raw = List.of(new ItemStack(Items.IRON_INGOT, 34), new ItemStack(Items.COPPER_INGOT, 14),
                new ItemStack(Items.REDSTONE_BLOCK, 2), new ItemStack(Items.REDSTONE, 12),
                new ItemStack(Items.QUARTZ, 2));
        for (int i = 0; i < raw.size(); i++) {
            inventory.setItem(FIRST_RAW_SLOT + i, raw.get(i));
        }
        helper.assertTrue(p.getMainHandItem().isEmpty(), "premise: the raw materials reached the hand: "
                + p.getMainHandItem());
        return p;
    }

    /**
     * Passo 1: i dieci oggetti dalle forme di design §12.12. Alla fine il ferro, il rame, i blocchi di redstone e i
     * quarzi sono finiti, restano 8 polveri, i dieci oggetti stanno nell'inventario (le due celle vuote), nulla e'
     * finito in mano e nulla e' a terra.
     */
    private static void craftEverything(GameTestHelper helper, ServerPlayer p) {
        craftFromInventory(helper, p, ModItems.EXOFRAME.get(), "ICI", "IRI", "I_I");
        craftFromInventory(helper, p, ModItems.SCRAP_HELMET.get(), "ICI", "I_I");
        craftFromInventory(helper, p, ModItems.SCRAP_TORSO.get(), "I_I", "ICI", "III");
        craftFromInventory(helper, p, ModItems.SCRAP_ARM.get(), "CI", "_I", "_I");
        craftFromInventory(helper, p, ModItems.SCRAP_ARM.get(), "CI", "_I", "_I");
        craftFromInventory(helper, p, ModItems.SCRAP_LEG.get(), "I_", "I_", "IC");
        craftFromInventory(helper, p, ModItems.SCRAP_LEG.get(), "I_", "I_", "IC");
        ItemStack cellA = craftFromInventory(helper, p, ModItems.REDSTONE_CELL.get(), "_C_", "rQr", "_C_");
        ItemStack cellB = craftFromInventory(helper, p, ModItems.REDSTONE_CELL.get(), "_C_", "rQr", "_C_");
        craftFromInventory(helper, p, ModItems.CHARGER.get(), "ICI", "CRC", "III");

        helper.assertTrue(countInMainInventory(p, Items.IRON_INGOT) == 0
                && countInMainInventory(p, Items.COPPER_INGOT) == 0
                && countInMainInventory(p, Items.REDSTONE_BLOCK) == 0 && countInMainInventory(p, Items.QUARTZ) == 0
                && countInMainInventory(p, Items.REDSTONE) == 8, "raw materials left: iron "
                + countInMainInventory(p, Items.IRON_INGOT) + ", copper " + countInMainInventory(p, Items.COPPER_INGOT)
                + ", redstone blocks " + countInMainInventory(p, Items.REDSTONE_BLOCK) + ", quartz "
                + countInMainInventory(p, Items.QUARTZ) + ", redstone dust "
                + countInMainInventory(p, Items.REDSTONE) + "; expected 0, 0, 0, 0 and 8");
        helper.assertTrue(countInMainInventory(p, ModItems.EXOFRAME.get()) == 1
                && countInMainInventory(p, ModItems.SCRAP_HELMET.get()) == 1
                && countInMainInventory(p, ModItems.SCRAP_TORSO.get()) == 1
                && countInMainInventory(p, ModItems.SCRAP_ARM.get()) == 2
                && countInMainInventory(p, ModItems.SCRAP_LEG.get()) == 2
                && countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 2
                && countInMainInventory(p, ModItems.CHARGER.get()) == 1,
                "the ten crafted items are not all in the inventory: " + p.getInventory().items);
        helper.assertTrue(energy(cellA) == 0 && energy(cellB) == 0, "the crafted cells hold " + energy(cellA)
                + " and " + energy(cellB) + " RE, not 0");
        for (ItemStack stack : p.getInventory().items) {
            helper.assertTrue(!stack.is(ModItems.REDSTONE_CELL.get()) || energy(stack) == 0,
                    "a crafted cell in the inventory holds " + energy(stack) + " RE, not 0");
        }
        helper.assertTrue(p.getMainHandItem().isEmpty(), "a crafted item ended up in hand: " + p.getMainHandItem());
        helper.assertTrue(itemsIn(helper).isEmpty(), "a crafted item fell on the ground: " + itemsIn(helper));
    }

    /**
     * Passo 2: il telaio fabbricato lascia l'inventario e si usa sul pavimento ({@code useExoframeItem} ne mette in
     * mano uno nuovo al suo posto: per questo prima la mano deve essere vuota); in sopravvivenza si consuma. Il telaio
     * e' nudo: il clic a mano vuota non fa entrare, e dice che manca una cella carica.
     */
    private static ExoframeEntity placeTheFrameAndGetRefused(GameTestHelper helper, ServerPlayer p) {
        int slot = slotOutsideTheHand(p, stack -> stack.is(ModItems.EXOFRAME.get()));
        helper.assertTrue(slot >= 0, "the crafted exoframe is not in the inventory");
        p.getInventory().removeItem(slot, 1);
        helper.assertTrue(countInMainInventory(p, ModItems.EXOFRAME.get()) == 0 && p.getMainHandItem().isEmpty(),
                "before the use: " + countInMainInventory(p, ModItems.EXOFRAME.get())
                        + " exoframes left in the inventory, hand " + p.getMainHandItem());

        ExoframeEntity frame = useExoframeItem(helper, p, FRAME_FLOOR, true);
        helper.assertTrue(frame.position().equals(helper.absoluteVec(FRAME_FEET)), "the frame stands at "
                + frame.position() + ", not at " + helper.absoluteVec(FRAME_FEET));
        helper.assertTrue(p.getMainHandItem().isEmpty() && countInMainInventory(p, ModItems.EXOFRAME.get()) == 0,
                "the used exoframe item was not consumed: hand " + p.getMainHandItem());

        clickAt(frame, p, TORSO_POINT);
        helper.assertTrue(messages(p, FrameEntry.NO_CELL) == 1, "no-cell messages after the click on the bare frame: "
                + messages(p, FrameEntry.NO_CELL));
        helper.assertTrue(Compat.frameState().wornFrameId(p).isEmpty(), "the player entered a frame with no cell");
        return frame;
    }

    /**
     * Passo 3: il caricatore fabbricato si posa con il clic vero; poi le due celle, una alla volta
     * ({@link #chargeACell}): la prima con 2 polveri fino a 1 000 RE, la seconda con 4 fino a 2 000 RE, ciascuna
     * rimessa nell'inventario. Restano 2 polveri.
     */
    private static ChargerBlockEntity placeTheChargerAndChargeTwoCells(GameTestHelper helper, ServerPlayer p) {
        hold(helper, p, ModItems.CHARGER.get(), 1);
        InteractionResult placed = clickBlock(helper, p, CHARGER_FLOOR, InteractionHand.MAIN_HAND);
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(CHARGER_POS));
        helper.assertTrue(placed.consumesAction() && entity instanceof ChargerBlockEntity,
                "the charger item was not placed: the click gave " + placed + ", block entity " + entity);
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the placed charger is still in hand: "
                + p.getMainHandItem());
        ChargerBlockEntity charger = (ChargerBlockEntity) entity;

        chargeACell(helper, p, charger, 0, 2, 1_000);
        stash(p);
        // ora la cella vuota e' una sola
        chargeACell(helper, p, charger, 0, 4, 2_000);
        stash(p);
        helper.assertTrue(p.getMainHandItem().isEmpty() && countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 2
                && countInMainInventory(p, Items.REDSTONE) == 2, "after the two charges: hand " + p.getMainHandItem()
                + ", " + countInMainInventory(p, ModItems.REDSTONE_CELL.get()) + " cells and "
                + countInMainInventory(p, Items.REDSTONE) + " redstone dust in the inventory; expected 2 and 2");
        return charger;
    }

    /**
     * Passo 4: i sei pezzi montati con un clic ciascuno (il secondo braccio e la seconda gamba, cliccati sullo stesso
     * lato, vanno su quello libero), poi la cella da 1 000 RE, poi il clic a mano vuota: il giocatore e' dentro, e al
     * primo ingresso la chat nomina i tasti. Restituisce l'identita' del telaio.
     */
    private static UUID armAndEnter(GameTestHelper helper, ServerPlayer p, ExoframeEntity frame) {
        mount(helper, p, frame, ModItems.SCRAP_HELMET.get(), HELMET_POINT);
        mount(helper, p, frame, ModItems.SCRAP_TORSO.get(), TORSO_POINT);
        mount(helper, p, frame, ModItems.SCRAP_ARM.get(), ARM_POINT);
        mount(helper, p, frame, ModItems.SCRAP_ARM.get(), ARM_POINT);
        mount(helper, p, frame, ModItems.SCRAP_LEG.get(), LEG_POINT);
        mount(helper, p, frame, ModItems.SCRAP_LEG.get(), LEG_POINT);
        helper.assertTrue(recordState(helper, frame).pieces().size() == 6, "pieces mounted on the frame: "
                + recordState(helper, frame).pieces().keySet() + ", expected the six slots");

        holdCell(helper, p, 1_000);
        clickAt(frame, p, TORSO_POINT);
        EnergyCell mounted = recordState(helper, frame).cell();
        helper.assertTrue(mounted != null && mounted.stored() == 1_000 && p.getMainHandItem().isEmpty(),
                "after the click with the 1000 RE cell: cell in the frame "
                        + (mounted == null ? "none" : mounted.stored() + " RE") + ", hand " + p.getMainHandItem());

        UUID frameId = frame.frameId();
        clickAt(frame, p, TORSO_POINT);
        helper.assertTrue(Compat.frameState().wornFrameId(p).equals(Optional.of(frameId)),
                "the player did not enter the armed frame: worn " + Compat.frameState().wornFrameId(p));
        helper.assertTrue(messages(p, EntryHints.KEYS_HINT) == 1, "keys hints after the first entry: "
                + messages(p, EntryHints.KEYS_HINT));
        return frameId;
    }

    /**
     * Passo 5: il tick di preparazione (a 1 000 RE su 10 000 l'energia e' ancora normale: nessun avviso) e poi
     * {@value #WALK_STEPS} passi da 0,2 blocchi lungo x, uno per tick, da {@link #FRAME_FEET} a {@link #EXIT_FEET}:
     * 8 RE in meno, e un solo avviso di energia bassa. Restituisce il tick della preparazione.
     */
    private static long walkAndDrain(GameTestHelper helper, ServerPlayer p) {
        Vec3 start = p.position();
        helper.assertTrue(start.equals(helper.absoluteVec(FRAME_FEET)), "the player in the frame stands at " + start
                + ", not where the frame stood");
        long t0 = now(helper);
        SuitTick.tick(p, t0);
        helper.assertTrue(stored(p) == 1_000 && messages(p, SuitTick.ENERGY_LOW) == 0,
                "after the preparation tick: " + stored(p) + " RE and " + messages(p, SuitTick.ENERGY_LOW)
                        + " energy-low messages; expected 1000 and 0");
        for (int i = 1; i <= WALK_STEPS; i++) {
            p.setPos(start.x + WALK_STEP * i, start.y, start.z);
            SuitTick.tick(p, t0 + i);
        }
        helper.assertTrue(p.position().distanceTo(helper.absoluteVec(EXIT_FEET)) < 1e-6, "the walk ended at "
                + p.position() + ", not at " + helper.absoluteVec(EXIT_FEET));
        helper.assertTrue(stored(p) == 992, stored(p) + " RE after the walk, expected 992");
        helper.assertTrue(messages(p, SuitTick.ENERGY_LOW) == 1, "energy-low messages after the walk: "
                + messages(p, SuitTick.ENERGY_LOW));
        return t0;
    }

    /**
     * Passo 6: un colpo di cactus da 8 arriva, consuma i sei pezzi nuovi secondo la loro parte (torso 2 punti, gli
     * altri 1) e toglie vita al giocatore.
     */
    private static void takeACactusHit(GameTestHelper helper, ServerPlayer p) {
        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "premise: the player is already hurt: "
                + p.getHealth() + " of " + p.getMaxHealth());
        helper.assertTrue(p.hurt(p.damageSources().cactus(), 8.0F), "the cactus hit was refused");
        for (PieceSlot slot : PieceSlot.values()) {
            int expected = slot == PieceSlot.TORSO ? 178 : 179;
            Piece piece = worn(p, slot);
            helper.assertTrue(piece != null && piece.durability() == expected, slot + " after the hit: "
                    + (piece == null ? "missing" : "durability " + piece.durability()) + ", expected " + expected);
        }
        helper.assertTrue(p.getHealth() < p.getMaxHealth(), "the hit took no health: " + p.getHealth());
    }

    /**
     * Passo 7: il cambio cella dall'interno prende la cella da 2 000 RE e lascia nell'inventario quella da 992; il
     * tick seguente non toglie un RE intero e non ripete l'avviso (un miglioramento tace).
     */
    private static void swapTheCell(GameTestHelper helper, ServerPlayer p, long t0) {
        CellSwap.handle(p, t0 + 30);
        helper.assertTrue(stored(p) == 2_000, stored(p) + " RE in the frame after the swap, expected 2000");
        ItemStack old = findInMainInventory(p, ModItems.REDSTONE_CELL.get());
        helper.assertTrue(countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 1 && energy(old) == 992
                && p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty(), "after the swap: "
                + countInMainInventory(p, ModItems.REDSTONE_CELL.get()) + " cells in the inventory, the first with "
                + energy(old) + " RE, hand " + p.getMainHandItem() + ", off hand " + p.getOffhandItem()
                + "; expected one cell with 992 RE and empty hands");

        SuitTick.tick(p, t0 + 31);
        helper.assertTrue(stored(p) == 2_000 && messages(p, SuitTick.ENERGY_LOW) == 1,
                "after the tick that follows the swap: " + stored(p) + " RE and "
                        + messages(p, SuitTick.ENERGY_LOW) + " energy-low messages; expected 2000 and 1");
    }

    /**
     * Passo 8: l'uscita lascia il telaio in piedi in {@link #EXIT_FEET}, con la sua entita', i sei pezzi e la cella da
     * 2 000 RE; la cella da 992 RE torna al caricatore con le ultime 2 polveri e ne esce con 1 992 RE, in mano.
     * Restituisce l'entita' del telaio lasciato.
     */
    private static ExoframeEntity leaveAndRecharge(GameTestHelper helper, ServerPlayer p, ChargerBlockEntity charger,
            UUID frameId) {
        helper.assertTrue(FrameEntry.exit(p), "the exit was refused");
        helper.assertTrue(Compat.frameState().wornFrameId(p).isEmpty(), "the player still wears a frame");
        PlacedFrame record = Compat.frameState().placed(helper.getLevel().getServer(), frameId).orElse(null);
        helper.assertTrue(record != null, "the frame left by the player is not placed");
        helper.assertTrue(record.position().distanceTo(helper.absoluteVec(EXIT_FEET)) < 1e-6, "the frame rests at "
                + record.position() + ", not at " + helper.absoluteVec(EXIT_FEET));
        EnergyCell cell = record.state().cell();
        helper.assertTrue(cell != null && cell.stored() == 2_000 && record.state().pieces().size() == 6,
                "the placed frame holds " + (cell == null ? "no cell" : cell.stored() + " RE") + " and the pieces "
                        + record.state().pieces().keySet() + "; expected 2000 RE and the six slots");
        ExoframeEntity left = frameAt(helper, record.position());
        helper.assertTrue(left.frameId().equals(frameId), "the frame standing where the player left is another one");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the exit put something in hand: " + p.getMainHandItem());

        chargeACell(helper, p, charger, 992, 2, 1_992);
        return left;
    }

    /**
     * Passo 9: i conti finali. Due celle in tutto, una nel telaio piazzato (2 000 RE) e una in mano (1 992 RE), che e'
     * anche il solo stack rimasto in tutto l'inventario; il caricatore e' vuoto, senza polvere, riserva ne' riporto;
     * nulla a terra. 3 992 RE = 8 polveri x 500 RE - 8 RE consumati camminando.
     */
    private static void countEverything(GameTestHelper helper, ServerPlayer p, ChargerBlockEntity charger,
            ExoframeEntity left) {
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(countInMainInventory(p, ModItems.REDSTONE_CELL.get()) == 1
                && held.is(ModItems.REDSTONE_CELL.get()) && held.getCount() == 1 && p.getOffhandItem().isEmpty()
                && charger.cell().isEmpty(), "cells outside the frame: "
                + countInMainInventory(p, ModItems.REDSTONE_CELL.get()) + " in the inventory, hand " + held
                + ", off hand " + p.getOffhandItem() + ", charger " + charger.cell()
                + "; expected only the one in hand");
        helper.assertTrue(countInMainInventory(p, Items.REDSTONE) == 0 && charger.dust().isEmpty()
                && charger.reserve() == 0 && charger.carryMilli() == 0, "redstone left: "
                + countInMainInventory(p, Items.REDSTONE) + " dust in the inventory, " + charger.dust().getCount()
                + " in the charger, reserve " + charger.reserve() + " RE, carry " + charger.carryMilli()
                + " thousandths; expected nothing");
        Inventory inventory = p.getInventory();
        int stacks = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                stacks++;
            }
        }
        helper.assertTrue(stacks == 1, "stacks left in the whole inventory: " + stacks
                + ", expected only the cell in hand; main inventory " + inventory.items);

        EnergyCell inFrame = recordState(helper, left).cell();
        helper.assertTrue(inFrame != null && inFrame.stored() == 2_000 && energy(held) == 1_992,
                "energy at the end: " + (inFrame == null ? "no cell" : inFrame.stored() + " RE") + " in the frame, "
                        + energy(held) + " RE in hand; expected 2000 and 1992");
        int total = inFrame.stored() + energy(held);
        helper.assertTrue(total == 8 * 500 - 8, total + " RE in all, expected 3992: 8 dust of 500 RE less 8 drained");
        helper.assertTrue(itemsIn(helper).isEmpty(), "items on the ground at the end: "
                + itemsIn(helper).stream().map(e -> e.getItem()).toList());
    }

    /**
     * Fabbrica un oggetto al banco da lavoro con cio' che il giocatore ha: per ogni casella non vuota della griglia
     * delle righe date ({@code craftingGrid}) toglie dall'inventario un oggetto di quel tipo, e fallisce se manca; la
     * griglia deve dare {@code expected}, uno. Il risultato va al giocatore con {@link Handouts#give} (il primo slot
     * libero che non e' quello in mano) e torna a chi chiama: lo stack nell'inventario ne e' una copia.
     */
    private static ItemStack craftFromInventory(GameTestHelper helper, ServerPlayer p, Item expected,
            String... rows) {
        List<ItemStack> grid = craftingGrid(rows);
        for (ItemStack ingredient : grid) {
            if (ingredient.isEmpty()) {
                continue;
            }
            int slot = slotOutsideTheHand(p, stack -> stack.is(ingredient.getItem()));
            helper.assertTrue(slot >= 0, "missing ingredient: no " + ingredient.getItem()
                    + " left in the inventory to craft " + expected);
            p.getInventory().removeItem(slot, 1);
        }
        ItemStack result = Compat.gameTests().craft(helper.getLevel(), CRAFTING_GRID_SIDE, CRAFTING_GRID_SIDE, grid);
        helper.assertTrue(result.is(expected) && result.getCount() == 1, "the shape " + List.of(rows) + " crafts "
                + result + ", not one " + expected);
        Handouts.give(p, result);
        return result;
    }

    /**
     * Prende in mano {@code count} oggetti di quel tipo da uno slot dell'inventario principale che non e' quello in
     * mano. La mano principale deve essere vuota; fallisce se nessuno slot ne ha abbastanza.
     */
    private static void hold(GameTestHelper helper, ServerPlayer p, Item item, int count) {
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the hand already holds " + p.getMainHandItem()
                + " before taking " + count + " " + item);
        int slot = slotOutsideTheHand(p, stack -> stack.is(item) && stack.getCount() >= count);
        helper.assertTrue(slot >= 0, "no stack of " + count + " " + item + " in the inventory");
        p.setItemInHand(InteractionHand.MAIN_HAND, p.getInventory().removeItem(slot, count));
    }

    /**
     * Prende in mano la cella con quella carica da uno slot dell'inventario principale che non e' quello in mano. La
     * mano principale deve essere vuota; fallisce se non la trova.
     */
    private static void holdCell(GameTestHelper helper, ServerPlayer p, int stored) {
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the hand already holds " + p.getMainHandItem()
                + " before taking the cell with " + stored + " RE");
        int slot = slotOutsideTheHand(p, stack -> stack.is(ModItems.REDSTONE_CELL.get())
                && energy(stack) == stored);
        helper.assertTrue(slot >= 0, "no cell with " + stored + " RE in the inventory");
        p.setItemInHand(InteractionHand.MAIN_HAND, p.getInventory().removeItem(slot, 1));
    }

    /**
     * Rimette nell'inventario cio' che il giocatore ha in mano: prima svuota la mano, poi riconsegna lo stack con
     * {@link Handouts#give}, che cosi' sceglie uno slot diverso da quello in mano.
     */
    private static void stash(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Handouts.give(p, held);
    }

    /**
     * Chiama {@code times} volte il tick del server del caricatore, ogni volta con lo stato del blocco letto dal
     * livello, come fa il ticker del gioco.
     */
    private static void tick(GameTestHelper helper, ChargerBlockEntity charger, int times) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = charger.getBlockPos();
        for (int i = 0; i < times; i++) {
            ChargerBlockEntity.serverTick(level, pos, level.getBlockState(pos), charger);
        }
    }

    /**
     * Carica al caricatore la cella dell'inventario che ha {@code from} RE con {@code dust} polveri, fino a
     * {@code to} RE, tutto con i clic veri: la cella in mano e il clic (dentro, e la barra delle azioni dice "niente
     * redstone"), le polveri in mano e il clic (dentro, la mano vuota, il caricatore in carica), i tick che servono a
     * bruciarle ({@value #TICKS_PER_DUST} per polvere: la cella ha {@code to} RE, niente polvere ne' riserva), il clic
     * a mano vuota (la cella con {@code to} RE in mano, il caricatore senza cella). La cella resta in mano.
     */
    private static void chargeACell(GameTestHelper helper, ServerPlayer p, ChargerBlockEntity charger, int from,
            int dust, int to) {
        String what = "the cell from " + from + " to " + to + " RE";
        int noRedstone = messages(p, ChargerBlock.STATUS_NO_REDSTONE);
        holdCell(helper, p, from);
        clickBlock(helper, p, CHARGER_POS, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().isEmpty() && !charger.cell().isEmpty() && energy(charger.cell()) == from,
                what + ", after the click with the cell: hand " + p.getMainHandItem() + ", charger " + charger.cell()
                        + " with " + energy(charger.cell()) + " RE");
        helper.assertTrue(messages(p, ChargerBlock.STATUS_NO_REDSTONE) == noRedstone + 1,
                what + ": the click with the cell did not tell 'no redstone'");

        hold(helper, p, Items.REDSTONE, dust);
        clickBlock(helper, p, CHARGER_POS, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().isEmpty() && charger.dust().getCount() == dust
                && charger.status() == Status.CHARGING, what + ", after the click with " + dust + " dust: hand "
                + p.getMainHandItem() + ", dust in the charger " + charger.dust().getCount() + ", status "
                + charger.status());

        tick(helper, charger, dust * TICKS_PER_DUST);
        helper.assertTrue(energy(charger.cell()) == to && charger.dust().isEmpty() && charger.reserve() == 0
                && charger.status() == Status.NO_REDSTONE, what + ", after " + dust * TICKS_PER_DUST + " ticks: "
                + energy(charger.cell()) + " RE, dust " + charger.dust().getCount() + ", reserve "
                + charger.reserve() + ", status " + charger.status());

        clickBlock(helper, p, CHARGER_POS, InteractionHand.MAIN_HAND);
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(held.is(ModItems.REDSTONE_CELL.get()) && held.getCount() == 1 && energy(held) == to
                && charger.cell().isEmpty(), what + ", after the click with an empty hand: hand " + held + " with "
                + energy(held) + " RE, charger " + charger.cell());
    }

    /**
     * Monta sul telaio un pezzo dell'inventario: in mano ({@code hold}) e un clic nel punto dato. Il clic deve
     * riuscire e la mano tornare vuota (in sopravvivenza il pezzo montato si consuma).
     */
    private static void mount(GameTestHelper helper, ServerPlayer p, ExoframeEntity frame, Item piece, Vec3 point) {
        hold(helper, p, piece, 1);
        InteractionResult result = clickAt(frame, p, point);
        helper.assertTrue(result == InteractionResult.SUCCESS && p.getMainHandItem().isEmpty(), "mounting " + piece
                + " at " + point + " gave " + result + ", hand " + p.getMainHandItem());
    }

    /** Un clic destro con la mano principale sul telaio, nel punto dato relativo ai suoi piedi. */
    private static InteractionResult clickAt(ExoframeEntity frame, ServerPlayer p, Vec3 point) {
        return click(frame, p, point.x, point.y, point.z);
    }

    /**
     * Il primo slot dell'inventario principale, diverso da quello in mano, il cui stack soddisfa la condizione; -1 se
     * non c'e'.
     */
    private static int slotOutsideTheHand(ServerPlayer p, Predicate<ItemStack> wanted) {
        Inventory inventory = p.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot != inventory.selected && wanted.test(inventory.items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Il fallimento del test con il nome del passo in testa al messaggio e la traccia dell'eccezione d'origine.
     * Senza causa: nel log il gioco scrive il messaggio della causa piu' profonda, e il nome del passo sparirebbe.
     */
    private static GameTestAssertException atStep(String step, String message, RuntimeException origin) {
        GameTestAssertException failure = new GameTestAssertException(step + ": " + message);
        failure.setStackTrace(origin.getStackTrace());
        return failure;
    }
}
