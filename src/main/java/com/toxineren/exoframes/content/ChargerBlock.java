package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.CompatEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Il caricatore di celle (design §12.11): un cubo opaco con una block entity ({@link ChargerBlockEntity}), senza GUI e
 * senza registrazioni lato client. Lo stato del blocco dice da che parte guarda ({@link #FACING}, orizzontale: verso
 * chi lo piazza, come una fornace), se tiene una cella ({@link #CELL}) e se sta caricando ({@link #LIT}, luce 7).
 * <p>
 * In questo primo passo e' vuoto: il clic a mano vuota dice solo "nessuna cella", e prova la cucitura del clic sulle
 * due versioni.
 */
public final class ChargerBlock extends CompatEntityBlock {
    /** Il lato del fronte, orizzontale. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Vero se nel caricatore c'e' una cella. */
    public static final BooleanProperty CELL = BooleanProperty.create("cell");
    /** Vero mentre il caricatore sta caricando la cella. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Stato nella barra delle azioni: nel caricatore non c'e' una cella. */
    public static final String STATUS_NO_CELL = "message." + RedstoneExoframes.MOD_ID + ".charger.no_cell";
    /** Stato nella barra delle azioni: la cella e' piena. */
    public static final String STATUS_FULL = "message." + RedstoneExoframes.MOD_ID + ".charger.full";
    /** Stato nella barra delle azioni: ne' polvere ne' riserva. */
    public static final String STATUS_NO_REDSTONE = "message." + RedstoneExoframes.MOD_ID + ".charger.no_redstone";
    /** Stato nella barra delle azioni: in carica, con la percentuale come argomento. */
    public static final String STATUS_CHARGING = "message." + RedstoneExoframes.MOD_ID + ".charger.charging";

    public ChargerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, false)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(pos, state);
    }

    /** Il fronte guarda verso chi lo piazza, come una fornace. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL, LIT);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Il clic del primo passo. Passa (il gioco prova allora l'oggetto in mano) per la mano secondaria, per uno
     * spettatore, per un giocatore non vivo e con qualcosa nella mano principale. A mano vuota: sul client dice solo
     * che il clic e' riuscito, senza toccare nulla; sul server mostra {@link #STATUS_NO_CELL} nella barra delle azioni.
     */
    @Override
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isAlive()
                || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable(STATUS_NO_CELL), true);
        return InteractionResult.CONSUME;
    }
}
