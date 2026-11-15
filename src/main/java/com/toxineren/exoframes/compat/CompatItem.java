package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.ItemBase;
import net.minecraft.world.item.Item;

/**
 * Il tipo che gli oggetti della mod con un suggerimento estendono, uguale sulle due versioni del gioco. La firma di
 * {@code Item.appendHoverText} cambia fra 1.20.1 e 1.21.1: la chiude la base in {@code compat/impl/} (vedi
 * docs/compat.md), e le sottoclassi aggiungono le loro righe con {@code tooltip(stack, lines, advanced)}, ereditato e
 * vuoto di base, senza vedere le firme delle due versioni.
 */
public abstract class CompatItem extends ItemBase {
    protected CompatItem(Item.Properties properties) {
        super(properties);
    }
}
