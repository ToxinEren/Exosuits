package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.CellBar;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.CellSwap;
import com.toxineren.exoframes.frame.ClientRules;
import com.toxineren.exoframes.frame.EntryHints;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FrameSounds;
import com.toxineren.exoframes.frame.FrameSounds.Sound;
import com.toxineren.exoframes.frame.SuitTick;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco della cella e degli oggetti (design §12.10, §12.13): barre e suggerimenti, il cambio cella dall'interno
 * ({@link CellSwap#handle}, chiamato a mano con tempi {@code now(helper) + k}), lo scambio su un telaio piazzato e la
 * riga dei tasti all'ingresso. Tutti nello stesso tick. Barre e suggerimenti senza giocatori: gli stack si
 * costruiscono con {@code CellItem.fromCell} e {@code PieceItem.fromPiece} e si interrogano con i metodi di
 * {@link ItemStack}, che delegano all'oggetto. Sul server di test {@link ClientRules} non ha ricevuto
 * nulla, quindi la capacita' della cella e' quella della config locale: ogni test lo controlla per primo.
 * <p>
 * Il suggerimento vero, al passaggio del mouse (l'override di {@code appendHoverText} per versione), qui non si
 * raggiunge (la firma di {@code getTooltipLines} cambia fra le versioni): i test chiamano il metodo neutro
 * {@code tooltip}. Il resto lo prova il client vero. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class CellGameTests {
    /** I piedi del telaio dei test del cambio cella, relativi alla struttura. */
    private static final Vec3 FEET = new Vec3(2.5, 1.0, 2.5);

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public CellGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "cellBarFollowsTheCharge", CellGameTests::cellBarFollowsTheCharge);
        add(tests, "pieceBarAndTooltipFollowTheDurability", CellGameTests::pieceBarAndTooltipFollowTheDurability);
        add(tests, "tooltipsTeachTheControls", CellGameTests::tooltipsTeachTheControls);
        add(tests, "cellSwapWithoutAFrameChangesNothing", CellGameTests::cellSwapWithoutAFrameChangesNothing);
        add(tests, "cellSwapAfterExitChangesNothing", CellGameTests::cellSwapAfterExitChangesNothing);
        add(tests, "cellSwapAfterDeathChangesNothing", CellGameTests::cellSwapAfterDeathChangesNothing);
        add(tests, "cellSwapTakesTheFullestBetterCell", CellGameTests::cellSwapTakesTheFullestBetterCell);
        add(tests, "cellSwapWithoutABetterCellSaysSo", CellGameTests::cellSwapWithoutABetterCellSaysSo);
        add(tests, "cellSwapRespectsTheInterval", CellGameTests::cellSwapRespectsTheInterval);
        add(tests, "cellSwapRefusedWithAMenuOpen", CellGameTests::cellSwapRefusedWithAMenuOpen);
        add(tests, "placedFrameSwapsCells", CellGameTests::placedFrameSwapsCells);
        add(tests, "keysHintOncePerSession", CellGameTests::keysHintOncePerSession);
        add(tests, "cellSwapTakesTheOffhandCellAndRefusesASpectator",
                CellGameTests::cellSwapTakesTheOffhandCellAndRefusesASpectator);
        return tests;
    }

    /**
     * La barra della cella: si vede finche' la cella non e' piena, larga quanto la carica sulla capacita' della config,
     * rossa; una carica sopra la capacita' vale piena (nessuna barra, larghezza 13).
     */
    private static void cellBarFollowsTheCharge(GameTestHelper helper) {
        int capacity = localCapacity(helper);
        ItemStack half = CellItem.fromCell(new EnergyCell(capacity, 5_000));
        helper.assertTrue(half.isBarVisible(), "a half-charged cell shows no bar");
        helper.assertTrue(half.getBarWidth() == 7, "half-charged cell bar width " + half.getBarWidth() + ", not 7");
        helper.assertTrue(half.getBarColor() == CellBar.CELL_COLOR,
                "cell bar color " + Integer.toHexString(half.getBarColor()));
        ItemStack empty = CellItem.fromCell(new EnergyCell(capacity, 0));
        helper.assertTrue(empty.isBarVisible(), "an empty cell shows no bar");
        helper.assertTrue(empty.getBarWidth() == 0, "empty cell bar width " + empty.getBarWidth() + ", not 0");
        helper.assertTrue(!CellItem.charged().isBarVisible(), "a full cell shows a bar");
        ItemStack over = new ItemStack(ModItems.REDSTONE_CELL.get());
        Compat.itemData().setCellEnergy(over, 20_000);
        helper.assertTrue(!over.isBarVisible(), "a cell above the capacity shows a bar");
        helper.assertTrue(over.getBarWidth() == 13, "over-charged cell bar width " + over.getBarWidth() + ", not 13");
        helper.succeed();
    }

    /**
     * La barra e il suggerimento di un pezzo: dalla durata nei dati, con il colore del gioco base; un pezzo intero non
     * ha barra; uno stack senza dati (appena fabbricato) non ha barra e dice "nuovo", senza numeri.
     */
    private static void pieceBarAndTooltipFollowTheDurability(GameTestHelper helper) {
        localCapacity(helper);
        ItemStack worn = PieceItem.fromPiece(new Piece(PieceSlot.HELMET, Tier.SCRAP, 180, 90), List.of(),
                Appearance.DEFAULT);
        helper.assertTrue(worn.isBarVisible(), "a worn piece shows no bar");
        helper.assertTrue(worn.getBarWidth() == 7, "worn piece bar width " + worn.getBarWidth() + ", not 7");
        helper.assertTrue(worn.getBarColor() == Mth.hsvToRgb(0.5f / 3f, 1f, 1f),
                "worn piece bar color " + Integer.toHexString(worn.getBarColor()));
        List<Component> lines = new ArrayList<>();
        ModItems.SCRAP_HELMET.get().tooltip(worn, lines, false);
        helper.assertTrue(keys(lines).equals(List.of(PieceItem.TOOLTIP_DURABILITY)),
                "worn piece tooltip " + keys(lines));
        helper.assertTrue(args(lines, 0).equals(List.of(90, 180)), "worn piece tooltip arguments " + args(lines, 0));
        ItemStack whole = PieceItem.fromPiece(new Piece(PieceSlot.HELMET, Tier.SCRAP, 180, 180), List.of(),
                Appearance.DEFAULT);
        helper.assertTrue(!whole.isBarVisible(), "a piece at full durability shows a bar");
        ItemStack fresh = new ItemStack(ModItems.SCRAP_HELMET.get());
        helper.assertTrue(!fresh.isBarVisible(), "a piece without data shows a bar");
        lines.clear();
        ModItems.SCRAP_HELMET.get().tooltip(fresh, lines, false);
        helper.assertTrue(keys(lines).equals(List.of(PieceItem.TOOLTIP_NEW)), "fresh piece tooltip " + keys(lines));
        helper.assertTrue(args(lines, 0).isEmpty(), "fresh piece tooltip has numbers: " + args(lines, 0));
        helper.succeed();
    }

    /**
     * I suggerimenti insegnano i comandi: il telaio le quattro azioni, nell'ordine; la cella vuota il caricatore; la
     * cella carica la carica sulla capacita' della config, una carica sopra la capacita' tagliata.
     */
    private static void tooltipsTeachTheControls(GameTestHelper helper) {
        int capacity = localCapacity(helper);
        List<Component> lines = new ArrayList<>();
        ModItems.EXOFRAME.get().tooltip(new ItemStack(ModItems.EXOFRAME.get()), lines, false);
        helper.assertTrue(keys(lines).equals(List.of("tooltip.redstone_exoframes.exoframe.place",
                "tooltip.redstone_exoframes.exoframe.mount", "tooltip.redstone_exoframes.exoframe.enter",
                "tooltip.redstone_exoframes.exoframe.unmount")), "exoframe tooltip " + keys(lines));

        CellItem cell = ModItems.REDSTONE_CELL.get();
        lines.clear();
        cell.tooltip(CellItem.fromCell(new EnergyCell(capacity, 0)), lines, false);
        helper.assertTrue(keys(lines).equals(List.of(CellItem.TOOLTIP_EMPTY)), "empty cell tooltip " + keys(lines));

        lines.clear();
        cell.tooltip(CellItem.fromCell(new EnergyCell(capacity, 5_000)), lines, false);
        helper.assertTrue(keys(lines).equals(List.of(CellItem.TOOLTIP_CHARGE)), "cell tooltip " + keys(lines));
        helper.assertTrue(args(lines, 0).equals(List.of(5_000, capacity)), "cell tooltip arguments " + args(lines, 0));

        ItemStack over = new ItemStack(cell);
        Compat.itemData().setCellEnergy(over, 20_000);
        lines.clear();
        cell.tooltip(over, lines, false);
        helper.assertTrue(keys(lines).equals(List.of(CellItem.TOOLTIP_CHARGE)), "over-charged cell tooltip "
                + keys(lines));
        helper.assertTrue(args(lines, 0).equals(List.of(capacity, capacity)),
                "over-charged cell tooltip arguments " + args(lines, 0));
        helper.succeed();
    }

    /**
     * Un cambio cella senza telaio indossato (design §12.10): nessun record nasce, la cella resta nell'inventario e
     * nulla si dice. Il controllo dopo, con il telaio addosso, prova che la stessa chiamata il cambio lo fa davvero.
     */
    private static void cellSwapWithoutAFrameChangesNothing(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        FrameStateAccess frames = Compat.frameState();
        NonNullList<ItemStack> items = player.getInventory().items;
        long t0 = now(helper);
        items.set(0, cell(9_000));

        CellSwap.handle(player, t0);

        helper.assertTrue(frames.wornFrameId(player).isEmpty() && frames.get(player).isEmpty(),
                "a cell swap without a frame created a record");
        helper.assertTrue(energy(items.get(0)) == 9_000 && countInMainInventory(player, cellItem()) == 1,
                "a cell swap without a frame touched the inventory: " + items.get(0));
        helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 0, "a cell swap without a frame said something");

        wearScrap(helper, player, FEET, 1_000);
        CellSwap.handle(player, t0 + 1);
        helper.assertTrue(stored(player) == 9_000, "control: the worn frame did not swap its cell: " + stored(player));
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Un cambio cella arrivato dopo l'uscita (Review Focus 1): nessun record indossato rinasce, la cella resta
     * nell'inventario e il telaio piazzato tiene la sua.
     */
    private static void cellSwapAfterExitChangesNothing(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        FrameStateAccess frames = Compat.frameState();
        NonNullList<ItemStack> items = player.getInventory().items;
        long t0 = now(helper);
        UUID id = wearScrap(helper, player, FEET, 1_000);
        items.set(0, cell(9_000));
        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        CellSwap.handle(player, t0);

        helper.assertTrue(frames.wornFrameId(player).isEmpty() && frames.get(player).isEmpty(),
                "a cell swap after the exit created a worn record");
        helper.assertTrue(energy(items.get(0)) == 9_000, "a cell swap after the exit took the cell: " + items.get(0));
        int left = frames.placed(server, id).orElseThrow().state().cell().stored();
        helper.assertTrue(left == 1_000, "the placed frame's cell changed: " + left);
        removeFrame(server, (ExoframeEntity) helper.getLevel().getEntity(id));
        helper.succeed();
    }

    /**
     * Un cambio cella arrivato dopo la morte (Review Focus 1): nessun record nasce dal nulla (il gestore si ferma
     * senza un telaio indossato; {@code changeWorn} e' la seconda difesa), la cella resta nell'inventario e il telaio
     * lasciato alla morte tiene la sua.
     */
    private static void cellSwapAfterDeathChangesNothing(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        long t0 = now(helper);
        UUID id = wearScrap(helper, player, FEET, 1_000);

        player.kill();

        helper.assertTrue(player.isDeadOrDying(), "player did not die");
        helper.assertTrue(frames.wornFrameId(player).isEmpty(), "frame still worn after the death");
        helper.assertTrue(placedCharge(server, id) == 1_000, "the frame left at the death has no 1000 RE cell");
        NonNullList<ItemStack> items = player.getInventory().items; // quello di prima e' gia' caduto
        items.set(0, cell(9_000));

        CellSwap.handle(player, t0);

        helper.assertTrue(frames.get(player).isEmpty(), "a cell swap after the death created a record");
        helper.assertTrue(energy(items.get(0)) == 9_000, "a cell swap after the death took the cell: " + items.get(0));
        helper.assertTrue(placedCharge(server, id) == 1_000, "the frame left at the death changed its cell");
        deathDrops(level, player.position()).forEach(Entity::discard);
        removeFrame(server, (ExoframeEntity) level.getEntity(id));
        helper.succeed();
    }

    /**
     * Il cambio (design §12.10): entra la cella piu' carica fra quelle migliori dell'installata, a parita' quella
     * dell'inventario prima della mano secondaria; la vecchia torna nello stesso slot; le altre restano. Nulla si
     * perde e nulla si duplica (Review Focus 1: celle e RE contati prima e dopo); una sincronizzazione, un suono,
     * nessun messaggio.
     */
    private static void cellSwapTakesTheFullestBetterCell(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        Inventory inventory = player.getInventory();
        long t0 = now(helper);
        wearScrap(helper, player, FEET, 2_000);
        inventory.items.set(3, cell(3_000));
        inventory.items.set(20, cell(9_000));
        inventory.offhand.set(0, cell(9_000));
        long[] before = cellsAndRe(player);
        helper.assertTrue(Arrays.equals(before, new long[] {4, 23_000}), "premise: " + Arrays.toString(before));
        int synced = Compat.gameTests().syncCount(player);

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            CellSwap.handle(player, t0);
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CELL_SWAP)),
                    "the swap played " + sounds.sounds() + ", expected [CELL_SWAP]");
        }

        helper.assertTrue(stored(player) == 9_000, "the fullest better cell not swapped in: " + stored(player));
        ItemStack back = inventory.items.get(20);
        helper.assertTrue(back.is(cellItem()) && back.getCount() == 1 && energy(back) == 2_000,
                "the old cell is not back in slot 20: " + back);
        helper.assertTrue(energy(inventory.items.get(3)) == 3_000, "slot 3 changed: " + inventory.items.get(3));
        helper.assertTrue(energy(inventory.offhand.get(0)) == 9_000,
                "the off hand changed: " + inventory.offhand.get(0));
        long[] after = cellsAndRe(player);
        helper.assertTrue(Arrays.equals(before, after), "cells and RE before " + Arrays.toString(before) + ", after "
                + Arrays.toString(after));
        helper.assertTrue(Compat.gameTests().syncCount(player) == synced + 1,
                "syncs " + (Compat.gameTests().syncCount(player) - synced) + ", expected 1");
        helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 0 && messages(player, SuitTick.ENERGY_LOW) == 0,
                "the swap said something");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Nessuna cella con piu' energia dell'installata (una uguale non e' meglio): un messaggio nella barra delle azioni,
     * e nient'altro cambia, ne' suona, ne' si sincronizza.
     */
    private static void cellSwapWithoutABetterCellSaysSo(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        NonNullList<ItemStack> items = player.getInventory().items;
        long t0 = now(helper);
        wearScrap(helper, player, FEET, 5_000);
        items.set(0, cell(5_000));
        items.set(1, cell(4_000));
        items.set(2, new ItemStack(Items.DIRT));
        int synced = Compat.gameTests().syncCount(player);

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            CellSwap.handle(player, t0);
            helper.assertTrue(sounds.sounds().isEmpty(), "no swap played " + sounds.sounds());
        }

        helper.assertTrue(stored(player) == 5_000, "the worn cell changed: " + stored(player));
        helper.assertTrue(energy(items.get(0)) == 5_000 && energy(items.get(1)) == 4_000
                && items.get(2).is(Items.DIRT), "the inventory changed: " + items.subList(0, 3));
        helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 1, "the player was not told once");
        helper.assertTrue(Compat.gameTests().syncCount(player) == synced, "a refused swap synced");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * L'intervallo minimo sul server (design §12.10): dopo un cambio, i pacchetti prima di meta' dell'attesa
     * ({@code cellSwapSeconds * 10} tick) si scartano in silenzio; dal tick 20 si cambia di nuovo.
     */
    private static void cellSwapRespectsTheInterval(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        NonNullList<ItemStack> items = player.getInventory().items;
        long t0 = now(helper);
        wearScrap(helper, player, FEET, 1_000);
        items.set(0, cell(9_000));

        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            CellSwap.handle(player, t0);
            helper.assertTrue(stored(player) == 9_000 && energy(items.get(0)) == 1_000, "first swap not done");
            setStored(player, 500);
            CellSwap.handle(player, t0);
            helper.assertTrue(stored(player) == 500 && energy(items.get(0)) == 1_000, "a swap in the same tick done");
            CellSwap.handle(player, t0 + 19);
            helper.assertTrue(stored(player) == 500 && energy(items.get(0)) == 1_000, "a swap 19 ticks later done");
            CellSwap.handle(player, t0 + 20);
            helper.assertTrue(stored(player) == 1_000 && energy(items.get(0)) == 500, "a swap 20 ticks later refused");
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.CELL_SWAP, Sound.CELL_SWAP)),
                    "swaps played " + sounds.sounds() + ", expected two CELL_SWAP");
        }
        helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 0, "a refused swap said something");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Con un menu aperto il cambio si rifiuta in silenzio (design §12.10) e non segna l'intervallo: chiuso il menu, il
     * tick dopo il cambio si fa.
     */
    private static void cellSwapRefusedWithAMenuOpen(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        NonNullList<ItemStack> items = player.getInventory().items;
        long t0 = now(helper);
        wearScrap(helper, player, FEET, 1_000);
        items.set(0, cell(9_000));
        player.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.threeRows(id, inv),
                Component.literal("cell swap test")));
        try {
            helper.assertTrue(player.containerMenu != player.inventoryMenu, "premise: no menu open");
            CellSwap.handle(player, t0);
            helper.assertTrue(stored(player) == 1_000 && energy(items.get(0)) == 9_000,
                    "a swap with a menu open was done");
            helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 0, "a refused swap said something");
        } finally {
            player.closeContainer();
        }
        CellSwap.handle(player, t0 + 1);
        helper.assertTrue(stored(player) == 9_000, "the refusal with a menu open marked the interval");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * Una cella in mano su un telaio piazzato che ne ha gia' una (design §12.10): le due si scambiano, la vecchia
     * torna nell'inventario, suona il montaggio e non si dice "slot occupato". In creativa la cella in mano resta e la
     * vecchia arriva comunque, nel primo slot libero diverso da quello in mano.
     */
    private static void placedFrameSwapsCells(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        FrameState state = ownedBy(player);
        state.insertCell(new EnergyCell(capacity, 1_000));
        ExoframeEntity frame = placeFrame(helper, state, FEET, 0f);
        player.setItemInHand(InteractionHand.MAIN_HAND, cell(8_000));
        try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper))) {
            helper.assertTrue(click(frame, player, 0.0, 1.6, 0.0).consumesAction(), "cell click not consumed");
            helper.assertTrue(sounds.sounds().equals(List.of(Sound.MOUNT)),
                    "the swap played " + sounds.sounds() + ", expected [MOUNT]");
        }
        helper.assertTrue(recordState(helper, frame).cell().stored() == 8_000, "the held cell not swapped in");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the swapped cell left in hand");
        helper.assertTrue(countInMainInventory(player, cellItem()) == 1
                && energy(findInMainInventory(player, cellItem())) == 1_000, "the old cell not handed back");
        helper.assertTrue(messages(player, FrameInteractions.SLOT_TAKEN) == 0, "the player was told the slot is taken");

        ServerPlayer creative = Compat.gameTests().mockServerPlayer(helper);
        creative.setGameMode(GameType.CREATIVE);
        helper.assertTrue(creative.getAbilities().instabuild, "premise: the creative player cannot instabuild");
        FrameState creativeState = ownedBy(creative);
        creativeState.insertCell(new EnergyCell(capacity, 1_000));
        ExoframeEntity creativeFrame = placeFrame(helper, creativeState, new Vec3(5.5, 1.0, 2.5), 0f);
        creative.setItemInHand(InteractionHand.MAIN_HAND, cell(8_000));
        helper.assertTrue(click(creativeFrame, creative, 0.0, 1.6, 0.0).consumesAction(),
                "creative cell click not consumed");
        helper.assertTrue(recordState(helper, creativeFrame).cell().stored() == 8_000,
                "creative: the held cell not swapped in");
        ItemStack held = creative.getMainHandItem();
        helper.assertTrue(held.is(cellItem()) && held.getCount() == 1 && energy(held) == 8_000,
                "creative: the held cell was used up: " + held);
        helper.assertTrue(countInMainInventory(creative, cellItem()) == 2
                && energy(creative.getInventory().items.get(1)) == 1_000, "creative: the old cell not handed back");
        removeFrame(server, frame);
        removeFrame(server, creativeFrame);
        helper.succeed();
    }

    /**
     * La riga dei tasti in chat (design §12.13) al primo ingresso della sessione, con i due tasti come
     * {@code Component.keybind}; un altro ingresso nella stessa sessione non la ripete, dopo la fine della sessione
     * ({@code PlayerEvent.PLAYER_QUIT}) si ripete.
     */
    private static void keysHintOncePerSession(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        UUID id = wearScrap(helper, player, FEET, 5_000);
        helper.assertTrue(messages(player, EntryHints.KEYS_HINT) == 1, "the keys hint not shown once on entering");
        Component hint = Compat.gameTests().messagesTo(player).stream()
                .filter(m -> m.getContents() instanceof TranslatableContents c
                        && c.getKey().equals(EntryHints.KEYS_HINT))
                .findFirst().orElseThrow();
        Object[] args = ((TranslatableContents) hint.getContents()).getArgs();
        helper.assertTrue(args.length == 2 && keybind(args[0]).equals("key.redstone_exoframes.exit")
                && keybind(args[1]).equals("key.redstone_exoframes.cell_swap"),
                "the keys hint does not name the two keys: " + Arrays.toString(args));

        helper.assertTrue(FrameEntry.exit(player), "exit refused");
        helper.assertTrue(FrameEntry.enter(player, (ExoframeEntity) helper.getLevel().getEntity(id)), "enter refused");
        helper.assertTrue(messages(player, EntryHints.KEYS_HINT) == 1, "the keys hint repeated in the same session");

        PlayerEvent.PLAYER_QUIT.invoker().quit(player);
        helper.assertTrue(FrameEntry.exit(player), "exit refused after the session");
        helper.assertTrue(FrameEntry.enter(player, (ExoframeEntity) helper.getLevel().getEntity(id)),
                "enter refused after the session");
        helper.assertTrue(messages(player, EntryHints.KEYS_HINT) == 2, "the keys hint not shown in a new session");
        leaveTestFrame(player);
        helper.succeed();
    }

    /**
     * La sola cella migliore sta nella mano secondaria. Da spettatore il cambio si rifiuta in silenzio; tornato in
     * sopravvivenza il cambio la prende e la vecchia torna nello stesso slot, senza toccare gli slot dell'armatura
     * (la mano secondaria e' lo slot subito dopo). Celle e RE contati prima e dopo.
     */
    private static void cellSwapTakesTheOffhandCellAndRefusesASpectator(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        Inventory inventory = player.getInventory();
        long t0 = now(helper);
        wearScrap(helper, player, FEET, 1_000);
        inventory.offhand.set(0, cell(9_000));
        long[] before = cellsAndRe(player);
        helper.assertTrue(Arrays.equals(before, new long[] {2, 10_000}), "premise: " + Arrays.toString(before));

        player.setGameMode(GameType.SPECTATOR);
        CellSwap.handle(player, t0);

        helper.assertTrue(stored(player) == 1_000, "a spectator swapped the cell: " + stored(player));
        helper.assertTrue(energy(inventory.offhand.get(0)) == 9_000,
                "the off hand of a spectator changed: " + inventory.offhand.get(0));
        helper.assertTrue(messages(player, CellSwap.NO_BETTER_CELL) == 0, "a spectator's swap said something");

        player.setGameMode(GameType.SURVIVAL);
        CellSwap.handle(player, t0 + 1);

        helper.assertTrue(stored(player) == 9_000, "the off-hand cell not swapped in: " + stored(player));
        ItemStack back = inventory.offhand.get(0);
        helper.assertTrue(back.is(cellItem()) && back.getCount() == 1 && energy(back) == 1_000,
                "the old cell is not back in the off hand: " + back);
        assertPlaceholdersWorn(helper, player);
        long[] after = cellsAndRe(player);
        helper.assertTrue(Arrays.equals(before, after), "cells and RE before " + Arrays.toString(before) + ", after "
                + Arrays.toString(after));
        leaveTestFrame(player);
        helper.succeed();
    }

    /** Il nome del tasto di un argomento {@code Component.keybind}; "?" per altro. */
    private static String keybind(Object arg) {
        return arg instanceof Component c && c.getContents() instanceof KeybindContents k ? k.getName() : "?";
    }

    /** L'oggetto cella. */
    private static Item cellItem() {
        return ModItems.REDSTONE_CELL.get();
    }

    /** La carica della cella del telaio piazzato; -1 se il record o la cella mancano. */
    private static int placedCharge(MinecraftServer server, UUID frameId) {
        return Compat.frameState().placed(server, frameId).map(p -> p.state().cell())
                .map(EnergyCell::stored).orElse(-1);
    }

    /**
     * Celle del giocatore e somma dei loro RE: inventario principale, mano secondaria e cella del telaio indossato (se
     * c'e').
     */
    private static long[] cellsAndRe(ServerPlayer player) {
        long cells = 0;
        long re = 0;
        Inventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inventory.items);
        stacks.add(inventory.offhand.get(0));
        for (ItemStack stack : stacks) {
            if (stack.is(cellItem())) {
                cells += stack.getCount();
                re += (long) energy(stack) * stack.getCount();
            }
        }
        EnergyCell worn = Compat.frameState().get(player).map(FrameState::cell).orElse(null);
        if (worn != null) {
            cells++;
            re += worn.stored();
        }
        return new long[] {cells, re};
    }

    /**
     * La capacita' della cella della config locale, dopo aver controllato che {@link ClientRules} la usi (sul server di
     * test non ha ricevuto nulla).
     */
    private static int localCapacity(GameTestHelper helper) {
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        helper.assertTrue(ClientRules.cellCapacity() == capacity, "ClientRules does not fall back to the local config");
        return capacity;
    }

    /** Le chiavi di traduzione delle righe, nell'ordine; "?" per una riga che non e' traducibile. */
    private static List<String> keys(List<Component> lines) {
        List<String> keys = new ArrayList<>();
        for (Component line : lines) {
            keys.add(line.getContents() instanceof TranslatableContents c ? c.getKey() : "?");
        }
        return keys;
    }

    /** Gli argomenti della riga traducibile i-esima. */
    private static List<Object> args(List<Component> lines, int i) {
        return List.of(((TranslatableContents) lines.get(i).getContents()).getArgs());
    }
}
