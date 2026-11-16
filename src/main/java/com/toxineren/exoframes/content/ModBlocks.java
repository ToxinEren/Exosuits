package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Blocchi della mod. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.BLOCK);

    /**
     * Il caricatore di celle (design §12.11): metallo, duro come una fornace (3,5), cade solo con l'attrezzo giusto,
     * fa luce 7 quando sta caricando ({@link ChargerBlock#LIT}).
     */
    public static final RegistrySupplier<ChargerBlock> CHARGER = BLOCKS.register("charger",
            () -> new ChargerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ChargerBlock.LIT) ? 7 : 0)));

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
    }
}
