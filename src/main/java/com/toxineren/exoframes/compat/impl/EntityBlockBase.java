package com.toxineren.exoframes.compat.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21 {
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
//?}

/**
 * Base dei blocchi della mod con una block entity: chiude le differenze del clic destro su un blocco fra 1.20.1 e
 * 1.21.1 dietro un metodo neutro senza versione. Il codice della mod non la estende direttamente: estende
 * {@code compat.CompatEntityBlock}. Non estende {@code BaseEntityBlock}, che su 1.21.1 vuole un {@code codec()}
 * astratto e rende il blocco invisibile di base: la forma di disegno resta quella di {@link Block}, un modello.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>clic destro: {@code use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} su 1.20.1, che il
 * gioco chiama una volta per mano; su 1.21.1 {@code useItemOn(ItemStack, BlockState, Level, BlockPos, Player,
 * InteractionHand, BlockHitResult)}, che il gioco chiama una volta per mano anche a mano vuota, e
 * {@code useWithoutItem(...)}, a cui arriva solo per la mano principale dopo un
 * {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} e che qui non si ridefinisce. Le sottoclassi rispondono in
 * {@link #onClick}; su 1.21.1 il suo risultato diventa un {@code ItemInteractionResult}: {@code SUCCESS} e
 * {@code SUCCESS_NO_ITEM_USED} danno {@code SUCCESS}, {@code CONSUME} da' {@code CONSUME}, {@code CONSUME_PARTIAL}
 * da' {@code CONSUME_PARTIAL}, {@code FAIL} da' {@code FAIL}, {@code PASS} da'
 * {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} (il gioco prova allora {@code useWithoutItem}, che passa, e poi l'oggetto
 * in mano).</li>
 * </ul>
 * Sulle due versioni il gioco non chiama il blocco quando il giocatore e' accovacciato e ha un oggetto in una delle
 * due mani: va diretto all'oggetto. {@code newBlockEntity(BlockPos, BlockState)} di {@link EntityBlock} ha la stessa
 * firma sulle due versioni e resta alle sottoclassi.
 */
public abstract class EntityBlockBase extends Block implements EntityBlock {
    protected EntityBlockBase(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Il clic destro di un giocatore sul blocco, una chiamata per mano, sui due lati; di base passa. */
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    //? if >=1.21 {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return switch (onClick(state, level, pos, player, hand, hit)) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return onClick(state, level, pos, player, hand, hit);
    }
    *///?}
}
