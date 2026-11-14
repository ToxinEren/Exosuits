package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.FrameLog;
import com.toxineren.exoframes.frame.FramePlacement;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * L'oggetto telaio (design, par. 5.4): usato su un blocco piazza un telaio vuoto (identita' nuova, chi lo usa come
 * proprietario, nulla di montato) al suolo sotto il lato cliccato, rivolto verso chi lo usa.
 */
public final class ExoframeItem extends Item {
    /** Messaggio sulla barra delle azioni quando il telaio li' non ci sta. */
    public static final String NO_ROOM = "message." + RedstoneExoframes.MOD_ID + ".no_room";
    /**
     * Quanti blocchi al massimo il telaio scende dal punto richiesto fino al suolo: oltre, l'oggetto rifiuta come se
     * non ci fosse spazio (usato sul fianco di un dirupo non manda il telaio in fondo). L'uscita e la morte non hanno
     * questo limite.
     */
    public static final double MAX_DROP = 3.0;

    public ExoframeItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Sul server piazza il telaio al suolo sotto {@code Vec3.atBottomCenterOf} del blocco accanto alla faccia
     * cliccata, con l'orientamento di chi lo usa girato di 180 gradi; toglie un oggetto dalla pila se chi lo usa non
     * e' in creativa. Se il telaio non ci sta (o sotto non c'e' suolo, o il suolo e' piu' di {@link #MAX_DROP} blocchi
     * sotto il punto, o il punto e' fuori dal bordo del mondo) la pila resta com'e', non nasce alcun record e chi lo usa
     * vede {@link #NO_ROOM} sulla barra delle azioni. Sul client dice solo che l'uso e' riuscito.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        FrameState state = new FrameState(RedstoneExoframes.config());
        if (player != null) {
            state.setOwner(player.getUUID());
        }
        Vec3 from = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        float yaw = Mth.wrapDegrees(context.getRotation() + 180f);
        Optional<ExoframeEntity> placed = FramePlacement.groundBelow(level, from)
                .filter(feet -> from.y - feet.y <= MAX_DROP)
                .flatMap(feet -> FramePlacement.place(level, state, feet, yaw));
        if (placed.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(NO_ROOM), true);
            }
            return InteractionResult.FAIL;
        }
        FrameLog.event(placed.get().frameId(), "placed", player, level, placed.get().position());
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
