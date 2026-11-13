package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/** Item del mod. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> REDSTONE_CELL = ITEMS.register("redstone_cell",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Segnaposto dei quattro slot armatura vanilla occupati dal telaio indossato. Non disegnano nulla: il layer
    // armatura vanilla li salta (materiale "frame" senza layer su 1.21.1, texture trasparenti su 1.20.1) e la tuta
    // la disegna il suo layer di rendering. Fuori dalla scheda creativa; nella parte 3a si ottengono solo con /give o con
    // il comando di prova (/exoframe debug wear), e il blocco degli slot arriva con la parte 3b.
    public static final RegistrySupplier<Item> EXOFRAME_HELMET = ITEMS.register("exoframe_helmet",
            () -> Compat.armorMaterials().newPlaceholder(ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> EXOFRAME_TORSO = ITEMS.register("exoframe_torso",
            () -> Compat.armorMaterials().newPlaceholder(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> EXOFRAME_LEGS = ITEMS.register("exoframe_legs",
            () -> Compat.armorMaterials().newPlaceholder(ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> EXOFRAME_BOOTS = ITEMS.register("exoframe_boots",
            () -> Compat.armorMaterials().newPlaceholder(ArmorItem.Type.BOOTS, new Item.Properties()));

    // Animatable GeckoLib del renderer della tuta, disegnato con uno stack fittizio: non si indossa mai e resta fuori
    // dalla scheda creativa. Va registrato perche' il renderer di GeckoLib 4.9 interroga i tag dello stack.
    public static final RegistrySupplier<Item> SUIT_MODEL = ITEMS.register("suit_model",
            () -> Compat.suitAssets().newSuitModelItem(new Item.Properties()));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
