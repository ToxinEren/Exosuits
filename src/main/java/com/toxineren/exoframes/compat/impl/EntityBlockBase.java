package com.toxineren.exoframes.compat.impl;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Base dei blocchi della mod con una block entity. Il codice della mod non la estende direttamente: estende
 * {@code compat.CompatEntityBlock}. Non estende {@code BaseEntityBlock}, che su 1.21.1 vuole un {@code codec()}
 * astratto e rende il blocco invisibile di base: la forma di disegno resta quella di {@link Block}, un modello.
 * <p>
 * Il clic destro, che cambia fra 1.20.1 e 1.21.1, lo traduce {@link BlockBase}, che questa classe estende: le
 * sottoclassi rispondono con lo stesso metodo neutro {@code onClick} dei blocchi senza block entity, e la traduzione e'
 * una sola. {@code newBlockEntity(BlockPos, BlockState)} di {@link EntityBlock} ha la stessa firma sulle due versioni e
 * resta alle sottoclassi.
 */
public abstract class EntityBlockBase extends BlockBase implements EntityBlock {
    protected EntityBlockBase(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
