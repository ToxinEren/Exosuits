package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.extensions.injected.InjectedItemPropertiesExtension;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/** Item del mod. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.ITEM);

    // arch$tab viene aggiunto a Item.Properties dal mixin di Architectury a runtime su tutti i loader, ma i jar di
    // piattaforma di Architectury non dichiarano l'iniezione per Loom: si chiama quindi attraverso l'interfaccia.
    public static final RegistrySupplier<Item> REDSTONE_CELL = ITEMS.register("redstone_cell",
            () -> new Item(((InjectedItemPropertiesExtension) new Item.Properties().stacksTo(1)).arch$tab(ModCreativeTabs.MAIN)));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
