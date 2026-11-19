package com.toxineren.exoframes.content;

import com.toxineren.exoframes.compat.CompatBlock;
import com.toxineren.exoframes.station.StationMenu;
import com.toxineren.exoframes.station.StationOpening;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * La stazione di manutenzione (design §5.1). Per ora un blocco solo, segnaposto, senza proprieta' di stato: il fronte
 * del suo modello guarda a nord. Nessuna block entity: il blocco non custodisce nulla, e il menu nasce dal clic
 * ({@link #onClick}), come quello del banco da lavoro.
 */
public final class StationBlock extends CompatBlock {
    public StationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Il clic destro: passa con la mano secondaria, per uno spettatore e per un giocatore che non e' vivo. Altrimenti
     * riesce senza usare l'oggetto in mano, qualunque sia: sul client cosi' e basta, sul server dopo aver aperto il
     * menu della stazione ({@link StationMenu#open}).
     */
    @Override
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isAlive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            StationMenu.open(serverPlayer, new StationOpening(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
