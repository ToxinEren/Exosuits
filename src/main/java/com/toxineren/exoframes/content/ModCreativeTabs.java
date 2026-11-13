package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Tab creative del mod. L'icona e' un Supplier: l'item viene risolto solo quando la tab la chiede; gli item entrano nella tab con append. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeTabRegistry.create(Component.translatable("itemGroup.redstone_exoframes.main"),
                    () -> new ItemStack(ModItems.REDSTONE_CELL.get())));

    private ModCreativeTabs() {
    }

    @SuppressWarnings("unchecked") // append(DeferredSupplier, T...) con T generico non e' @SafeVarargs in Architectury
    public static void register() {
        TABS.register();
        CreativeTabRegistry.append(MAIN, ModItems.REDSTONE_CELL);
    }
}
