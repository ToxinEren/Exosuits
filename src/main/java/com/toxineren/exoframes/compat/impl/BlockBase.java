package com.toxineren.exoframes.compat.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21 {
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
//?}

/**
 * Base dei blocchi della mod: chiude le differenze del clic destro su un blocco e di {@code playerWillDestroy} fra
 * 1.20.1 e 1.21.1 dietro metodi neutri senza versione. E' l'unica traduzione, per i blocchi senza block entity e per
 * quelli con una block entity ({@link EntityBlockBase} la estende). Il codice della mod non la estende direttamente:
 * estende {@code compat.CompatBlock} o {@code compat.CompatEntityBlock}.
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
 * <li>un giocatore che sta per rompere il blocco: {@code playerWillDestroy(Level, BlockPos, BlockState, Player)}
 * restituisce {@code void} su 1.20.1 e {@code BlockState} su 1.21.1, pubblico sulle due. L'override di ogni versione
 * chiama prima {@link #onPlayerWillDestroy}, poi {@code super} (le particelle della rottura, i piglin, l'evento di
 * gioco), e su 1.21.1 restituisce cio' che restituisce {@code super}.</li>
 * </ul>
 * Sulle due versioni il gioco non chiama il blocco quando il giocatore e' accovacciato e ha un oggetto in una delle
 * due mani: va diretto all'oggetto.
 */
public abstract class BlockBase extends Block {
    protected BlockBase(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Il clic destro di un giocatore sul blocco, una chiamata per mano, sui due lati; di base passa. */
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /**
     * Un giocatore sta per rompere il blocco ({@code state}, in {@code pos}), sui due lati, prima che il gioco lo tolga
     * e ne faccia cadere il bottino; di base nulla. Il gioco lo chiama da {@code ServerPlayerGameMode.destroyBlock} e,
     * sul client, da {@code MultiPlayerGameMode.destroyBlock}. L'attrezzo in mano: sul gioco base e su NeoForge la
     * chiamata arriva prima che la rottura lo consumi ({@code ItemStack.mineBlock}); su Forge 1.20.1 dopo, perche'
     * Forge la fa da {@code onDestroyedByPlayer}, quindi un attrezzo che si rompe su quel colpo e' gia' sparito dalla
     * mano.
     */
    public void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    }

    //? if >=1.21 {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onPlayerWillDestroy(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onPlayerWillDestroy(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

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
