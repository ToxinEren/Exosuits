package com.toxineren.exoframes.content;

import com.toxineren.exoframes.compat.CompatBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** L'oggetto che posa la stazione di manutenzione (design §5.1): un oggetto di blocco, con il nome del blocco. */
public final class StationItem extends CompatBlockItem {
    public StationItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
