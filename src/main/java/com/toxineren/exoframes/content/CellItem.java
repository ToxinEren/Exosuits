package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.core.energy.EnergyCell;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cella di redstone come oggetto. Lo stack porta solo la carica ({@link Compat#itemData()}); la capacita' viene
 * sempre dalla config ({@code energy.cellCapacity}).
 */
public final class CellItem extends Item {

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
}
