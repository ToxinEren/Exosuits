package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.CellBar;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.frame.ClientRules;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Test in gioco delle barre e dei suggerimenti degli oggetti (design §12.10, §12.13). Tutti nello stesso tick, senza
 * giocatori: gli stack si costruiscono con {@code CellItem.fromCell} e {@code PieceItem.fromPiece} e si interrogano con
 * i metodi di {@link ItemStack}, che delegano all'oggetto. Sul server di test {@link ClientRules} non ha ricevuto
 * nulla, quindi la capacita' della cella e' quella della config locale: ogni test lo controlla per primo.
 * <p>
 * Il suggerimento vero, al passaggio del mouse (l'override di {@code appendHoverText} per versione), qui non si
 * raggiunge (la firma di {@code getTooltipLines} cambia fra le versioni): i test chiamano il metodo neutro
 * {@code tooltip}. Il resto lo prova il client vero. Gli aiuti comuni stanno in {@link GameTestKit}.
 */
public final class CellGameTests {

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public CellGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "cellBarFollowsTheCharge", CellGameTests::cellBarFollowsTheCharge);
        add(tests, "pieceBarAndTooltipFollowTheDurability", CellGameTests::pieceBarAndTooltipFollowTheDurability);
        add(tests, "tooltipsTeachTheControls", CellGameTests::tooltipsTeachTheControls);
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
