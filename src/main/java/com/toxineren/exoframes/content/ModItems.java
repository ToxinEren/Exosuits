package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/** Item del mod. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> REDSTONE_CELL = ITEMS.register("redstone_cell",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
