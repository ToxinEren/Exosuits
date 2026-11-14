package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.content.PieceKind;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.ModuleType;
import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Le mani su un telaio piazzato (design §5.4): montare pezzi e cella, smontarli, raccogliere il telaio vuoto. Nessuna
 * interfaccia grafica: decide la mano principale e il punto cliccato. Il registro e' la verita' (§4.5): ogni cambio
 * passa da {@link FrameStateAccess#updatePlaced} o {@link FrameStateAccess#removePlaced}, e cio' che lascia la mano o
 * arriva al giocatore dipende dal loro esito; tutto quello che lo stato potrebbe rifiutare si controlla prima, cosi'
 * nessuna eccezione lascia il record cambiato a meta'.
 * <p>
 * Lati e zone sono quelli del telaio: con yaw 0 guarda a sud (+Z) e ha la destra verso -X, come un giocatore vanilla
 * (il renderer gira il modello come {@code LivingEntityRenderer}, e il pezzo del braccio destro sta sull'osso del
 * braccio destro). L'altezza del clic si misura dai piedi: gambe sotto {@value #LEGS_TOP}, busto e braccia fino a
 * {@value #HEAD_BOTTOM}, testa da li' in su; nella fascia di mezzo un braccio se il clic e' a piu' di
 * {@value #ARM_OFFSET} dal centro verso il suo lato, altrimenti il busto.
 */
public final class FrameInteractions {
    /** Messaggio sulla barra delle azioni a chi non puo' toccare il telaio. */
    public static final String LOCKED = "message." + RedstoneExoframes.MOD_ID + ".locked";
    /** Messaggio sulla barra delle azioni quando lo slot del pezzo o della cella e' gia' occupato. */
    public static final String SLOT_TAKEN = "message." + RedstoneExoframes.MOD_ID + ".slot_taken";

    /** Sotto questa altezza dai piedi il clic e' sulle gambe. */
    static final double LEGS_TOP = 1.1;
    /** Da questa altezza dai piedi il clic e' sulla testa. */
    static final double HEAD_BOTTOM = 2.2;
    /** Distanza laterale dal centro oltre la quale, nella fascia di mezzo, il clic e' su un braccio. */
    static final double ARM_OFFSET = 0.25;

    /** Che cosa chiede la mano principale. */
    private enum Action {
        /** Nulla: un altro oggetto, la mano secondaria o uno spettatore. */
        NONE,
        MOUNT_PIECE,
        INSERT_CELL,
        /** Mano vuota, in piedi: entrare nel telaio. */
        ENTER,
        /** Mano vuota, accovacciati: smontare. */
        UNMOUNT
    }

    private FrameInteractions() {
    }

    /**
     * Il clic destro di un giocatore sul telaio, sul server; {@code hit} e' relativo ai piedi dell'entita'. Regole, in
     * ordine: senza permesso ({@link FramePermissions#canUse}) solo il messaggio {@link #LOCKED}; un pezzo in mano si
     * monta; una cella in mano si inserisce; mano vuota in piedi: ingresso (per ora nulla); mano vuota accovacciati:
     * si smonta. La mano secondaria, uno spettatore, un altro oggetto in mano o un telaio senza record passano senza
     * effetto.
     */
    public static InteractionResult onInteract(ServerPlayer player, ExoframeEntity frame, Vec3 hit,
            InteractionHand hand) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        Objects.requireNonNull(hit, "hit must not be null");
        Objects.requireNonNull(hand, "hand must not be null");
        Action action = action(player, hand);
        if (action == Action.NONE) {
            return InteractionResult.PASS;
        }
        MinecraftServer server = player.getServer();
        Optional<PlacedFrame> placed = Compat.frameState().placed(server, frame.frameId());
        if (placed.isEmpty()) {
            return InteractionResult.PASS;
        }
        PlacedFrame record = placed.get();
        if (!FramePermissions.canUse(player, record.state())) {
            tell(player, LOCKED);
            return InteractionResult.CONSUME;
        }
        double right = rightOf(frame, hit);
        return switch (action) {
            case MOUNT_PIECE -> mountPiece(player, frame, record, right > 0);
            case INSERT_CELL -> insertCell(player, frame, record);
            case ENTER -> enter(player, frame, record);
            case UNMOUNT -> unmount(player, frame, record, hit.y, right);
            case NONE -> InteractionResult.PASS;
        };
    }

    /**
     * Il risultato del clic sul client, che non tocca nessuno stato: dice solo se il server fara' qualcosa con
     * quello che c'e' in mano, cosi' il gioco non passa all'altra mano ne' all'uso dell'oggetto.
     */
    public static InteractionResult clientResult(Player player, InteractionHand hand) {
        Action action = action(player, hand);
        return action == Action.NONE || action == Action.ENTER ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private static Action action(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
            return Action.NONE;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof PieceItem) {
            return Action.MOUNT_PIECE;
        }
        if (held.getItem() instanceof CellItem) {
            return Action.INSERT_CELL;
        }
        if (held.isEmpty()) {
            return player.isShiftKeyDown() ? Action.UNMOUNT : Action.ENTER;
        }
        return Action.NONE;
    }

    /**
     * Quanto il clic sta a destra del centro, sull'asse destro del telaio: con yaw 0 la destra e' -X, e in generale
     * (-cos yaw, 0, -sin yaw), come per un giocatore che guarda verso (-sin yaw, 0, cos yaw).
     */
    private static double rightOf(ExoframeEntity frame, Vec3 hit) {
        double yaw = Math.toRadians(frame.getYRot());
        return -hit.x * Math.cos(yaw) - hit.z * Math.sin(yaw);
    }

    /**
     * Monta il pezzo in mano nello slot del suo tipo; braccia e gambe sul lato cliccato se e' libero, altrimenti
     * sull'altro. Moduli e aspetto vengono dai dati dello stack e vanno nello slot. Un oggetto lascia la mano (non in
     * creativa) solo se il registro ha preso il pezzo.
     */
    private static InteractionResult mountPiece(ServerPlayer player, ExoframeEntity frame, PlacedFrame record,
            boolean rightSide) {
        ItemStack held = player.getMainHandItem();
        PieceItem item = (PieceItem) held.getItem();
        FrameState state = record.state();
        PieceSlot slot = null;
        for (PieceSlot candidate : clickedSideFirst(item.kind(), rightSide)) {
            if (!state.pieces().containsKey(candidate)) {
                slot = candidate;
                break;
            }
        }
        if (slot == null) {
            tell(player, SLOT_TAKEN);
            return InteractionResult.CONSUME;
        }
        PieceSlot target = slot;
        Piece piece = item.toPiece(held, target);
        Optional<PieceData> data = Compat.itemData().piece(held);
        List<ModuleType> modules = data.map(PieceData::modules).orElse(List.of());
        Appearance appearance = data.map(PieceData::appearance).orElse(Appearance.DEFAULT);
        // Lo slot e' libero e i moduli letti dallo stack sono distinti e non null: mount, restoreModules e
        // setAppearance non rifiutano.
        boolean mounted = Compat.frameState().updatePlaced(player.getServer(), record.frameId(), s -> {
            s.mount(piece);
            if (!modules.isEmpty()) {
                s.restoreModules(target, modules);
            }
            s.setAppearance(target, appearance);
        });
        if (!mounted) {
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        refreshSummary(player.getServer(), frame);
        return InteractionResult.SUCCESS;
    }

    /** Le posizioni di un tipo di pezzo, quella del lato cliccato per prima. */
    private static List<PieceSlot> clickedSideFirst(PieceKind kind, boolean rightSide) {
        return switch (kind) {
            case ARM -> rightSide ? List.of(PieceSlot.RIGHT_ARM, PieceSlot.LEFT_ARM)
                    : List.of(PieceSlot.LEFT_ARM, PieceSlot.RIGHT_ARM);
            case LEG -> rightSide ? List.of(PieceSlot.RIGHT_LEG, PieceSlot.LEFT_LEG)
                    : List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG);
            case HELMET, TORSO -> kind.slots();
        };
    }

    /** Inserisce la cella in mano se il telaio non ne ha una; altrimenti {@link #SLOT_TAKEN}. */
    private static InteractionResult insertCell(ServerPlayer player, ExoframeEntity frame, PlacedFrame record) {
        if (record.state().cell() != null) {
            tell(player, SLOT_TAKEN);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getMainHandItem();
        EnergyCell cell = ((CellItem) held.getItem()).toCell(held);
        if (!Compat.frameState().updatePlaced(player.getServer(), record.frameId(), s -> s.insertCell(cell))) {
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        refreshSummary(player.getServer(), frame);
        return InteractionResult.SUCCESS;
    }

    /** Mano vuota, in piedi: qui entrera' il giocatore nel telaio (prossimo passo); per ora nessun effetto. */
    private static InteractionResult enter(ServerPlayer player, ExoframeEntity frame, PlacedFrame record) {
        return InteractionResult.PASS;
    }

    /**
     * Mano vuota, accovacciati, in quest'ordine: l'armatura stivata che si legge, tutta; altrimenti il pezzo della
     * parte cliccata o, se lo slot e' vuoto, quello piu' vicino della stessa fascia; con la fascia vuota la cella;
     * senza cella, con il telaio del tutto vuoto, il telaio stesso torna oggetto. Altrimenti nulla.
     */
    private static InteractionResult unmount(ServerPlayer player, ExoframeEntity frame, PlacedFrame record,
            double height, double right) {
        MinecraftServer server = player.getServer();
        FrameStateAccess frames = Compat.frameState();
        if (record.hasStowed()) {
            List<StowedStack> stowed = frames.takeStowedOfPlaced(server, record.frameId());
            for (StowedStack entry : stowed) {
                Handouts.give(player, entry.stack());
            }
            return stowed.isEmpty() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }
        FrameState state = record.state();
        for (PieceSlot slot : bandNearestFirst(height, right)) {
            if (state.pieces().containsKey(slot)) {
                return unmountPiece(player, frame, record, slot);
            }
        }
        if (state.cell() != null) {
            ItemStack cell = CellItem.fromCell(state.cell());
            if (!frames.updatePlaced(server, record.frameId(), FrameState::removeCell)) {
                return InteractionResult.CONSUME;
            }
            Handouts.give(player, cell);
            refreshSummary(server, frame);
            return InteractionResult.SUCCESS;
        }
        if (state.pieces().isEmpty()) {
            if (!frames.removePlaced(server, record.frameId())) {
                return InteractionResult.CONSUME;
            }
            frame.discard();
            Handouts.give(player, new ItemStack(ModItems.EXOFRAME.get()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Le posizioni della fascia d'altezza del clic, la parte cliccata per prima e poi le altre dalla piu' vicina.
     */
    private static List<PieceSlot> bandNearestFirst(double height, double right) {
        boolean rightSide = right > 0;
        if (height >= HEAD_BOTTOM) {
            return List.of(PieceSlot.HELMET);
        }
        if (height < LEGS_TOP) {
            return rightSide ? List.of(PieceSlot.RIGHT_LEG, PieceSlot.LEFT_LEG)
                    : List.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG);
        }
        PieceSlot near = rightSide ? PieceSlot.RIGHT_ARM : PieceSlot.LEFT_ARM;
        PieceSlot far = rightSide ? PieceSlot.LEFT_ARM : PieceSlot.RIGHT_ARM;
        if (Math.abs(right) > ARM_OFFSET) {
            return List.of(near, PieceSlot.TORSO, far);
        }
        return List.of(PieceSlot.TORSO, near, far);
    }

    /**
     * Smonta il pezzo dello slot e lo da' al giocatore come oggetto con durabilita', moduli e aspetto dello slot.
     * L'oggetto si costruisce prima di cambiare il registro: se per il livello del pezzo non c'e' un oggetto il pezzo
     * resta montato.
     */
    private static InteractionResult unmountPiece(ServerPlayer player, ExoframeEntity frame, PlacedFrame record,
            PieceSlot slot) {
        FrameState state = record.state();
        Piece piece = state.pieces().get(slot);
        List<ModuleType> modules = List.copyOf(state.modules().getOrDefault(slot, List.of()));
        ItemStack stack;
        try {
            stack = PieceItem.fromPiece(piece, modules, state.appearance(slot));
        } catch (IllegalArgumentException e) {
            RedstoneExoframes.LOGGER.warn("Cannot unmount {} from frame {}: {}", slot, record.frameId(),
                    e.getMessage());
            return InteractionResult.CONSUME;
        }
        if (!Compat.frameState().updatePlaced(player.getServer(), record.frameId(), s -> s.unmount(slot))) {
            return InteractionResult.CONSUME;
        }
        Handouts.give(player, stack);
        refreshSummary(player.getServer(), frame);
        return InteractionResult.SUCCESS;
    }

    /** Il riassunto dell'entita' dallo stato del suo record, dopo un cambio. */
    private static void refreshSummary(MinecraftServer server, ExoframeEntity frame) {
        UUID frameId = frame.frameId();
        Compat.frameState().placed(server, frameId).ifPresent(p -> frame.setSummary(SuitSummary.of(p.state())));
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
