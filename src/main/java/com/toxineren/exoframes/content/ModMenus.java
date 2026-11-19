package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.station.StationMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Tipi di menu della mod. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.MENU);

    /**
     * Il menu della stazione di manutenzione (design §5.2): un tipo "esteso" di Architectury, che all'apertura porta al
     * client i dati extra; la fabbrica e' il costruttore del client di {@link StationMenu}, che li legge subito. La
     * schermata si registra sul client, quando il tipo c'e' (vedi docs/compat.md, "Menu e schermate").
     */
    public static final RegistrySupplier<MenuType<StationMenu>> STATION = MENUS.register("station",
            () -> MenuRegistry.ofExtended(StationMenu::new));

    private ModMenus() {
    }

    public static void register() {
        MENUS.register();
    }
}
