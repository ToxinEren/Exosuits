package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.CompatBlock;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.station.StationFinder;
import com.toxineren.exoframes.station.StationMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * La stazione di manutenzione (design §5.1): due meta' affiancate in orizzontale, come un letto ma per il lungo davanti
 * a chi la posa. {@link #FACING} e' il lato del fronte, verso chi l'ha posata; {@link #PART} dice quale meta' e' il
 * blocco: la principale ({@link StationPart#MAIN}), dove il gioco posa il blocco, e la laterale
 * ({@link StationPart#SIDE}), alla destra di chi la posa ({@link #otherHalf}). Nessuna block entity: il blocco non
 * custodisce nulla, e il menu nasce dal clic sull'una o sull'altra meta' ({@link #onClick}), come quello del banco da
 * lavoro, su un telaio piazzato a portata della stazione.
 * <p>
 * Le due meta' nascono e spariscono insieme, e una stazione lascia a terra al piu' un oggetto, quello della principale
 * (la tabella di bottino fa cadere l'oggetto solo da {@code part=main}):
 * <ul>
 * <li><b>posa</b> ({@link #getStateForPlacement}, {@link #setPlacedBy}): la principale si posa solo se c'e' posto anche
 * per la laterale, che il server mette subito dopo;</li>
 * <li><b>una meta' rimasta sola</b> ({@link #updateShape}) diventa aria, e il gioco ne fa cadere il bottino: rotta con
 * il piccone la laterale, la stazione cade dalla principale; tolta una meta' in un altro modo (un comando, un'altra
 * mod), sparisce anche l'altra;</li>
 * <li><b>la laterale rotta in creativa o senza l'attrezzo giusto</b> ({@link #onPlayerWillDestroy}) toglie prima la
 * principale, senza bottino; dalla principale non serve nulla, come per il letto e la porta del gioco base, che
 * agiscono solo dalla meta' senza bottino;</li>
 * <li><b>i pistoni</b> non la spostano ({@code PushReaction.BLOCK} in {@link ModBlocks#STATION}): una meta' spinta via
 * la spezzerebbe.</li>
 * </ul>
 */
public final class StationBlock extends CompatBlock {
    /** Il lato del fronte, orizzontale: verso chi l'ha posata. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Quale meta' della stazione e' il blocco. */
    public static final EnumProperty<StationPart> PART = EnumProperty.create("part", StationPart.class);
    /**
     * Il messaggio sulla barra delle azioni quando nessun telaio e' a portata della stazione, con la distanza in
     * blocchi della config del server.
     */
    public static final String NO_FRAME = "message." + RedstoneExoframes.MOD_ID + ".station.no_frame";

    /** Stato predefinito: il fronte a nord, la meta' principale. */
    public StationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, StationPart.MAIN));
    }

    /**
     * La posizione dell'altra meta' della stazione di cui {@code state}, in {@code pos}, e' una meta': la laterale di
     * una principale sta verso {@code FACING.getCounterClockWise()}, alla destra di chi guarda il fronte; la principale
     * di una laterale verso {@code FACING.getClockWise()}.
     */
    public static BlockPos otherHalf(BlockPos pos, BlockState state) {
        return pos.relative(towardOtherHalf(state));
    }

    /** La posizione della meta' principale della stazione di cui {@code state}, in {@code pos}, e' una meta'. */
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == StationPart.MAIN ? pos : otherHalf(pos, state);
    }

    /** La direzione in cui sta l'altra meta', vista da quella di {@code state}. */
    private static Direction towardOtherHalf(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == StationPart.MAIN ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /**
     * Vero se {@code other} e' l'altra meta' della stessa stazione di {@code state}: lo stesso blocco, l'altra
     * {@link #PART} e lo stesso {@link #FACING}.
     */
    private boolean isOtherHalf(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(PART) != state.getValue(PART)
                && other.getValue(FACING) == state.getValue(FACING);
    }

    /**
     * La posa: lo stato della meta' principale, con il fronte verso chi posa, che il gioco mette dove posa il blocco.
     * Null, e nulla si posa (l'oggetto resta in mano), se la posizione della laterale ({@link #otherHalf}) non si puo'
     * sostituire, e' fuori dal bordo del mondo o ha entita' che impediscono di costruire: per la laterale gli stessi
     * controlli che l'oggetto fa per la posizione della principale.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState main = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        BlockPos sidePos = otherHalf(context.getClickedPos(), main);
        Level level = context.getLevel();
        Player player = context.getPlayer();
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        if (!level.getBlockState(sidePos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(sidePos)
                || !level.isUnobstructed(main.setValue(PART, StationPart.SIDE), sidePos, collision)) {
            return null;
        }
        return main;
    }

    /**
     * Dopo la posa della principale, sul server: la laterale accanto ({@link #otherHalf}) con i segni 3, poi gli
     * aggiornamenti dei vicini della principale, come fa il letto.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(otherHalf(pos, state), state.setValue(PART, StationPart.SIDE), Block.UPDATE_ALL);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    /**
     * Una meta' rimasta sola diventa aria: se il vicino nella direzione dell'altra meta' non e' piu' l'altra meta'
     * della stessa stazione (lo stesso blocco, l'altra {@link #PART}, lo stesso {@link #FACING}), lo stato nuovo e'
     * l'aria, e il gioco toglie il blocco facendone cadere il bottino (solo la principale ne ha). Un vicino che cambia
     * in un'altra direzione non conta: due stazioni accostate restano indipendenti. Pubblico, come su 1.20.1 (su 1.21.1
     * e' protetto): compila sulle due versioni.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == towardOtherHalf(state) && !isOtherHalf(state, neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Prima che un giocatore rompa una meta', sul server: se e' la laterale e il giocatore e' in creativa
     * ({@code instabuild}) o non ha l'attrezzo giusto per farla cadere, toglie prima la principale della stessa
     * stazione, senza bottino (segni 35) e con le particelle e il suono della rottura (evento 2001); poi il gioco rompe
     * la laterale, che non ha bottino. Senza questo la principale diventerebbe aria per {@link #updateShape}, con il
     * suo bottino, e una stazione cadrebbe anche in creativa o a mano vuota. In tutti gli altri casi nulla: con
     * l'attrezzo giusto, in sopravvivenza, la stazione deve cadere proprio dalla principale che diventa aria; rotta la
     * principale, che in creativa o senza attrezzo non fa cadere nulla, la laterale diventa aria senza bottino.
     * (Togliere per prima la laterale quando si rompe la principale farebbe invece diventare aria la principale per
     * {@link #updateShape}, con il bottino.)
     * <p>
     * Due limiti accettati, perche' e' la stessa regola delle porte del gioco base 1.21.1
     * ({@code DoorBlock.playerWillDestroy}: creativa, o {@code hasCorrectToolForDrops} a un argomento falso) e
     * correggerli vorrebbe una cucitura diversa per loader:
     * <ul>
     * <li>Su Forge 1.20.1 questa chiamata arriva dopo che la rottura ha consumato l'attrezzo (docs/compat.md, alla voce
     * {@code playerWillDestroy} di {@code CompatBlock}): se il piccone si rompe proprio sul colpo che rompe la
     * laterale, qui la mano e' gia' vuota e la stazione si perde, perche' la principale se ne va senza bottino e la
     * laterale non ne ha.</li>
     * <li>Su NeoForge {@code hasCorrectToolForDrops} a un argomento non manda {@code PlayerEvent.HarvestCheck}, che
     * NeoForge manda dalle chiamate con livello e posizione (la rottura lo chiede con {@code canHarvestBlock}, e la sua
     * porta con {@code hasCorrectToolForDrops} a tre argomenti): una mod che con quell'evento cambia che cosa si
     * raccoglie non e' sentita da questa regola, e la stazione allora puo' perdersi o cadere contro la decisione di
     * quella mod, ma mai due volte.</li>
     * </ul>
     */
    @Override
    public void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || state.getValue(PART) != StationPart.SIDE
                || (!player.getAbilities().instabuild && player.hasCorrectToolForDrops(state))) {
            return;
        }
        BlockPos mainPos = otherHalf(pos, state);
        BlockState main = level.getBlockState(mainPos);
        if (isOtherHalf(state, main)) {
            level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPos, Block.getId(main));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Gira il fronte con la struttura; le due meta' girano insieme e la laterale resta alla destra del fronte. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * Specchia il fronte, come il caricatore. Una struttura specchiata scambia pero' i lati delle due meta': la
     * laterale non sta piu' dove la principale la cerca, e le due meta' diventano aria (al piu' una stazione a terra,
     * dalla principale). Accettato: una stazione in una struttura specchiata si posa di nuovo.
     */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Il clic destro, sull'una o sull'altra meta': passa con la mano secondaria, per uno spettatore e per un giocatore
     * che non e' vivo. Altrimenti non usa l'oggetto in mano, qualunque sia; sul client riesce e basta. Sul server legge
     * prima il blocco vero nel posto della meta' principale ({@link #mainPos}): se non e' la principale di questa
     * stazione (lo stesso blocco, {@link #PART} principale, lo stesso {@link #FACING} della meta' cliccata), come per
     * una meta' laterale rimasta sola posata con un comando, non apre nulla e non dice nulla ({@code CONSUME}).
     * Altrimenti cerca il telaio per la principale, con il suo stato vero ({@link StationFinder#find}):
     * <ul>
     * <li>nessun telaio a portata: il messaggio {@link #NO_FRAME} sulla barra delle azioni, con la distanza della
     * config del server ({@link StationFinder#reachText}), e {@code CONSUME};</li>
     * <li>telai a portata ma nessuno che il giocatore possa usare: il messaggio del telaio bloccato
     * ({@link FrameInteractions#LOCKED}) e {@code CONSUME};</li>
     * <li>il telaio trovato: il menu della stazione su quel telaio ({@link StationMenu#open}) e {@code SUCCESS}.</li>
     * </ul>
     */
    @Override
    public InteractionResult onClick(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isAlive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos main = mainPos(pos, state);
        // La principale vera: una laterale rimasta sola non ha una stazione intorno, e il menu non si costruirebbe.
        BlockState mainState = level.getBlockState(main);
        if (!mainState.is(this) || mainState.getValue(PART) != StationPart.MAIN
                || mainState.getValue(FACING) != state.getValue(FACING)) {
            return InteractionResult.CONSUME;
        }
        StationFinder.Result found = StationFinder.find(serverPlayer, serverLevel, main, mainState);
        switch (found.outcome()) {
            case NONE_IN_REACH -> serverPlayer.displayClientMessage(Component.translatable(NO_FRAME,
                    StationFinder.reachText(RedstoneExoframes.config().station.reachBlocks)), true);
            case ALL_LOCKED -> serverPlayer.displayClientMessage(Component.translatable(FrameInteractions.LOCKED),
                    true);
            case FOUND -> {
                Optional<PlacedFrame> frame = found.frameId()
                        .flatMap(frameId -> Compat.frameState().placed(serverLevel.getServer(), frameId));
                // La ricerca ha appena letto il record, nello stesso tick: c'e' sempre.
                if (frame.isPresent()) {
                    StationMenu.open(serverPlayer, StationMenu.opening(serverPlayer, main, frame.get()));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.CONSUME;
    }
}
