package com.toxineren.exoframes.frame;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;

/**
 * Con la tuta non si dorme (design §12.3, domanda 5 di §8): il risveglio metterebbe un ingombro alto 2,97 in una
 * stanza bassa. Il clic destro su un letto si annulla sui due lati e per le due mani (sul client lo decide
 * {@link SuitSize#wearsSuit}, il segnaposto del torso, che vale senza pacchetti); il messaggio
 * {@link SuitTick#LEAVE_FRAME_FIRST} lo manda solo il server e solo per la mano principale (una volta per clic). Le
 * cavalcature sono nel passo 3 di {@link SuitTick}.
 */
public final class SuitRestrictions {
    private SuitRestrictions() {
    }

    /** Registra l'ascoltatore del clic destro su un blocco, sui due lati: lo chiama {@code RedstoneExoframes.init()}. */
    public static void register() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register(SuitRestrictions::onUseBlock);
    }

    /**
     * Clic destro di {@code player} con la mano {@code hand} sul blocco in {@code pos}: lo annulla se il giocatore
     * indossa la tuta e il blocco e' un letto, altrimenti lascia decidere al gioco. Pubblico per i test.
     */
    public static EventResult onUseBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (!SuitSize.wearsSuit(player) || !(player.level().getBlockState(pos).getBlock() instanceof BedBlock)) {
            return EventResult.pass();
        }
        if (!player.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.translatable(SuitTick.LEAVE_FRAME_FIRST), true);
        }
        return EventResult.interruptFalse();
    }
}
