package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.CompatItem;
import com.toxineren.exoframes.core.energy.CellBar;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.frame.ClientRules;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cella di redstone come oggetto. Lo stack porta solo la carica ({@link Compat#itemData()}); la capacita' viene
 * sempre dalla config ({@code energy.cellCapacity}): una sola capacita' (design §12.10).
 * <p>
 * Barra e suggerimento si disegnano solo sul client e usano sempre la capacita' di {@link ClientRules#cellCapacity()}:
 * quella del server a cui il client e' connesso (senza server, la config locale). {@link #toCell} (sul server) usa la
 * config del server.
 */
public final class CellItem extends CompatItem {
    /** Riga del suggerimento di una cella con carica: carica e capacita' in RE. */
    public static final String TOOLTIP_CHARGE = "tooltip." + RedstoneExoframes.MOD_ID + ".cell.charge";
    /** Riga del suggerimento di una cella vuota: dove si carica. */
    public static final String TOOLTIP_EMPTY = "tooltip." + RedstoneExoframes.MOD_ID + ".cell.empty";

    public CellItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * La cella che lo stack rappresenta: capacita' dalla config, carica dai dati dello stack (0 se mancano), ridotta
     * alla capacita' se la supera (per esempio dopo che la config ha abbassato la capacita').
     */
    public EnergyCell toCell(ItemStack stack) {
        Objects.requireNonNull(stack, "stack must not be null");
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        return new EnergyCell(capacity, Math.min(Compat.itemData().cellEnergy(stack), capacity));
    }

    /**
     * Lo stack (uno) della cella con la carica di {@code cell}. La capacita' della cella non si salva: rileggendo lo
     * stack vale quella della config.
     */
    public static ItemStack fromCell(EnergyCell cell) {
        Objects.requireNonNull(cell, "cell must not be null");
        ItemStack stack = new ItemStack(ModItems.REDSTONE_CELL.get());
        Compat.itemData().setCellEnergy(stack, cell.stored());
        return stack;
    }

    /** Una cella piena alla capacita' della config: quella della scheda creativa. */
    public static ItemStack charged() {
        return fromCell(EnergyCell.full(RedstoneExoframes.config().energy.cellCapacity));
    }

    /** La barra si vede finche' la cella non e' piena (una carica sopra la capacita' vale piena). */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return CellBar.visible(Compat.itemData().cellEnergy(stack), ClientRules.cellCapacity());
    }

    /** La larghezza della barra, da 0 a {@link CellBar#MAX_WIDTH}. */
    @Override
    public int getBarWidth(ItemStack stack) {
        return CellBar.width(Compat.itemData().cellEnergy(stack), ClientRules.cellCapacity());
    }

    /** Rosso della redstone, {@link CellBar#CELL_COLOR}. */
    @Override
    public int getBarColor(ItemStack stack) {
        return CellBar.CELL_COLOR;
    }

    /**
     * Una riga grigia: {@link #TOOLTIP_EMPTY} senza carica, altrimenti {@link #TOOLTIP_CHARGE} con la carica (tagliata
     * alla capacita') e la capacita'.
     */
    @Override
    public void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
        int capacity = ClientRules.cellCapacity();
        int shown = Math.min(Compat.itemData().cellEnergy(stack), capacity);
        if (shown == 0) {
            lines.add(Component.translatable(TOOLTIP_EMPTY).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(TOOLTIP_CHARGE, shown, capacity).withStyle(ChatFormatting.GRAY));
        }
    }
}
