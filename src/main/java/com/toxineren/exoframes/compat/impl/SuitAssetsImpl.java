package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.SuitAssets;
import net.minecraft.world.item.Item;

/** Implementazione di {@link SuitAssets}: le differenze di GeckoLib stanno in {@link SuitModelItem}. */
public final class SuitAssetsImpl implements SuitAssets {
    @Override
    public Item newSuitModelItem(Item.Properties props) {
        return new SuitModelItem(props);
    }
}
