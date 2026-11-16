package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.BlockItemBase;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Il tipo che gli oggetti della mod che posano un blocco e hanno un suggerimento estendono, uguale sulle due versioni
 * del gioco: come {@link CompatItem}, per un {@code BlockItem}. La firma di {@code Item.appendHoverText} cambia fra
 * 1.20.1 e 1.21.1: la chiude la base in {@code compat/impl/} (vedi docs/compat.md), e le sottoclassi aggiungono le loro
 * righe con {@code tooltip(stack, lines, advanced)}, ereditato e vuoto di base, senza vedere le firme delle due
 * versioni.
 */
public abstract class CompatBlockItem extends BlockItemBase {
    protected CompatBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
