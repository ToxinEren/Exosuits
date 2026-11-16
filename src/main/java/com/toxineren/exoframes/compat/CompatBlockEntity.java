package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.BlockEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Il tipo che le block entity della mod estendono, uguale sulle due versioni del gioco. Lettura e scrittura dell'NBT di
 * una block entity cambiano fra 1.20.1 e 1.21.1 (nome, visibilita' e il provider dei registri in piu'): le chiude la
 * base in {@code compat/impl/} (vedi docs/compat.md), e le sottoclassi leggono e scrivono i loro dati con
 * {@code readData(tag)} e {@code writeData(tag)}, senza vedere le firme delle due versioni.
 */
public abstract class CompatBlockEntity extends BlockEntityBase {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
