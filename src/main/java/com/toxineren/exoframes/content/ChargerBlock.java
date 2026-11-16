package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.CompatEntityBlock;
import com.toxineren.exoframes.frame.FrameSounds;
import com.toxineren.exoframes.frame.Handouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Il caricatore di celle (design §12.11): un cubo opaco con una block entity ({@link ChargerBlockEntity}), senza GUI e
 * senza registrazioni lato client. Lo stato del blocco dice da che parte guarda ({@link #FACING}, orizzontale: verso
 * chi lo piazza, come una fornace), se tiene una cella ({@link #CELL}) e se sta caricando ({@link #LIT}, luce 7).
 * <p>
 * Sul server la block entity fa un tick di carica a ogni tick del gioco ({@link #getTicker}) ed e' lei, in quel tick, a
 * scrivere {@link #CELL} e {@link #LIT}; un comparatore accanto legge la carica della cella, da 0 a 15.
 * <p>
 * Senza GUI, il caricatore si usa con il clic destro ({@link #onClick}): una cella in mano entra o si scambia con
 * quella dentro, la polvere di redstone in mano lo alimenta, la mano vuota prende la cella e poi la polvere, e ogni
 * clic dice lo stato nella barra delle azioni. Le tramogge passano dalla block entity, che e' un contenitore del gioco
 * base; rotto, il caricatore lascia a terra cella e polvere ({@link #onRemove}).
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

    /**
     * Il ticker della block entity: nessuno sul client (cio' che si vede sta nello stato del blocco), sul server
     * {@link ChargerBlockEntity#serverTick} per il tipo del caricatore.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHARGER.get()) {
            return null;
        }
        return (l, p, s, be) -> ChargerBlockEntity.serverTick(l, p, s, (ChargerBlockEntity) be);
    }

    /** Il caricatore ha un'uscita per il comparatore. */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * L'uscita per il comparatore: la carica della cella da 0 a 15 ({@link ChargerBlockEntity#comparatorOutput}), 0 se
     * la block entity manca.
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChargerBlockEntity charger ? charger.comparatorOutput() : 0;
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
     * Quando il caricatore sparisce davvero (rotto, esploso, sostituito da un altro blocco) la cella, con la sua
     * carica, e la polvere cadono a terra ({@link ChargerBlockEntity#dropContents}) e i comparatori vicini vengono
     * avvisati. Il gioco chiama {@code onRemove} anche a ogni cambio di proprieta' dello stato ({@link #LIT},
     * {@link #CELL}: il blocco resta lo stesso), e li' non deve cadere nulla: si agisce solo se il blocco nuovo e' un
     * altro. Poi sempre {@code super}, che toglie la block entity.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            charger.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Il clic destro sul caricatore (design §12.11), una sola azione per clic, tutta in questa chiamata sul thread del
     * server: una cella sta nel caricatore o in un inventario, mai in tutti e due.
     * <p>
     * <b>Passa</b> (il gioco prova allora l'oggetto in mano), senza toccare nulla e senza messaggi: la mano
     * secondaria, uno spettatore, un giocatore non vivo, e nella mano principale qualcosa che non e' ne' una cella ne'
     * polvere di redstone. Sul client, per il resto, dice solo che il clic e' riuscito: tutto avviene sul server.
     * <p>
     * <b>Sul server</b>, secondo cio' che tiene la mano principale:
     * <ul>
     * <li><b>una cella:</b> entra nel caricatore; se dentro ce n'era gia' una le due si scambiano, e la vecchia va
     * nella mano se e' rimasta vuota, altrimenti nell'inventario con {@link Handouts#give} (a terra, ferma, se non ha
     * posto). Suona {@code CHARGER_INSERT}. La carica della cella che entra si taglia qui alla capacita' della config
     * (design §12.10): e' l'ingresso a mano, il solo che taglia; {@link ChargerBlockEntity#setCell}, da cui passano
     * anche le tramogge, non taglia mai;</li>
     * <li><b>polvere di redstone:</b> ne entrano quante ne stanno, fino a {@link ChargerBlockEntity#MAX_DUST} nel
     * caricatore; nessun suono;</li>
     * <li><b>nulla, in piedi:</b> la cella va in mano, e suona {@code CHARGER_TAKE}; se la cella non c'e', va in mano
     * tutta la polvere, senza suono; se non c'e' nemmeno quella, nulla;</li>
     * <li><b>nulla, accovacciati:</b> nulla si muove. (Accovacciati con un oggetto in una delle due mani il gioco non
     * chiama il blocco.)</li>
     * </ul>
     * In creativa ({@code instabuild}) cio' che e' in mano non si consuma: la cella o la polvere entrano e restano
     * anche in mano, e la cella tolta da uno scambio arriva comunque al giocatore. Chiunque puo' prendere la cella di
     * chiunque, come da ogni contenitore del gioco base (design §8, domanda 11): il deposito protetto e' un telaio
     * bloccato.
     * <p>
     * Dopo l'azione, sempre, lo stato nella barra delle azioni: {@link #STATUS_NO_CELL}, {@link #STATUS_FULL},
     * {@link #STATUS_NO_REDSTONE} o {@link #STATUS_CHARGING} con la percentuale, in quest'ordine di precedenza. Lo
     * stato del blocco ({@link #CELL}, {@link #LIT}) lo riscrive il tick seguente della block entity.
     *
     * @return {@code PASS} nei casi che passano e, sul server, se in quel punto non c'e' la block entity del
     *         caricatore; {@code SUCCESS} sul client; sul server {@code SUCCESS} se il contenuto e' cambiato,
     *         {@code CONSUME} se il clic ha solo detto lo stato
     */
    @Override
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isAlive()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getMainHandItem();
        boolean holdsCell = held.getItem() instanceof CellItem;
        boolean holdsDust = held.is(Items.REDSTONE);
        if (!held.isEmpty() && !holdsCell && !holdsDust) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ChargerBlockEntity charger)
                || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        boolean changed;
        if (holdsCell) {
            insertCell(charger, serverPlayer, held);
            FrameSounds.play(serverLevel, Vec3.atCenterOf(pos), FrameSounds.Sound.CHARGER_INSERT);
            changed = true;
        } else if (holdsDust) {
            changed = addDust(charger, serverPlayer, held);
        } else if (player.isShiftKeyDown()) {
            changed = false;
        } else if (!charger.cell().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, charger.cell());
            charger.setCell(ItemStack.EMPTY);
            FrameSounds.play(serverLevel, Vec3.atCenterOf(pos), FrameSounds.Sound.CHARGER_TAKE);
            changed = true;
        } else if (!charger.dust().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, charger.dust());
            charger.setDust(ItemStack.EMPTY);
            changed = true;
        } else {
            changed = false;
        }

        player.displayClientMessage(statusLine(charger), true);
        return changed ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * La cella in mano entra nel caricatore con la carica tagliata alla capacita' della config; quella che c'era
     * (se c'era) torna al giocatore: nella mano principale se ora e' vuota, altrimenti con {@link Handouts#give}. In
     * creativa la mano tiene la sua cella.
     */
    private static void insertCell(ChargerBlockEntity charger, ServerPlayer player, ItemStack held) {
        ItemStack old = charger.cell();
        charger.setCell(CellItem.fromCell(((CellItem) held.getItem()).toCell(held)));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        if (old.isEmpty()) {
            return;
        }
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, old);
        } else {
            Handouts.give(player, old);
        }
    }

    /**
     * Sposta dalla mano al caricatore le polveri che ci stanno; in creativa la mano non cala. Falso se non ce ne sta
     * nessuna.
     */
    private static boolean addDust(ChargerBlockEntity charger, ServerPlayer player, ItemStack held) {
        int inside = charger.dust().getCount();
        int moved = Math.min(held.getCount(), ChargerBlockEntity.MAX_DUST - inside);
        if (moved <= 0) {
            return false;
        }
        charger.setDust(new ItemStack(Items.REDSTONE, inside + moved));
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
        }
        return true;
    }

    /** La riga dello stato per la barra delle azioni, secondo {@link ChargerBlockEntity#status}. */
    private static Component statusLine(ChargerBlockEntity charger) {
        return switch (charger.status()) {
            case NO_CELL -> Component.translatable(STATUS_NO_CELL);
            case FULL -> Component.translatable(STATUS_FULL);
            case NO_REDSTONE -> Component.translatable(STATUS_NO_REDSTONE);
            case CHARGING -> Component.translatable(STATUS_CHARGING, charger.chargePercent());
        };
    }
}
