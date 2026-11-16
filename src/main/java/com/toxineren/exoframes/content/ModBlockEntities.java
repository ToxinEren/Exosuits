package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Tipi di block entity della mod. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    /**
     * La block entity del caricatore, valida solo per il blocco {@link ModBlocks#CHARGER} (su 1.21.1 il costruttore di
     * una block entity rifiuta uno stato il cui blocco non e' fra quelli del tipo). Senza tipo per il correttore di
     * dati ({@code build(null)}): i suoi dati sono numeri e booleani che nessuna versione converte.
     */
    public static final RegistrySupplier<BlockEntityType<ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITY_TYPES.register("charger",
                    () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, ModBlocks.CHARGER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITY_TYPES.register();
    }
}
