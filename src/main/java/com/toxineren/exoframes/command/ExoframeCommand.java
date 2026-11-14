package com.toxineren.exoframes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ExoframeItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FrameLog;
import com.toxineren.exoframes.frame.FramePlacement;
import com.toxineren.exoframes.frame.Handouts;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Comando {@code /exoframe} (design §5.5). Si riferisce al telaio che chi lo esegue indossa oppure, se non ne indossa,
 * a quello piazzato che guarda entro {@link #SIGHT} blocchi (la prima entita' telaio sulla linea di vista dagli occhi,
 * non attraverso i blocchi); serve un giocatore, non la console. Per tutti:
 * <ul>
 * <li>{@code /exoframe unlock} e {@code /exoframe lock}: sblocca o blocca il telaio per gli altri giocatori; solo il
 * proprietario o un operatore (livello di permesso 2), e un telaio senza proprietario solo un operatore. Il cambio
 * passa dal registro; gia' in quello stato riesce lo stesso.</li>
 * </ul>
 * Solo per gli operatori (livello 2; senza, i rami non esistono per quella sorgente):
 * <ul>
 * <li>{@code /exoframe remove}: elimina per sempre il telaio piazzato che guarda (mai uno indossato); quello che puo'
 * diventare oggetto resta a terra ai suoi piedi, vedi {@link #removeFrame}. La conferma va anche alla console e agli
 * altri operatori, come per i comandi di amministrazione del gioco.</li>
 * </ul>
 * e i rami di debug: quello che mette addosso a chi lo esegue un telaio vero, con il suo record indossato nel
 * registro ({@link DebugWear}), e lo veste con i segnaposto:
 * <ul>
 * <li>{@code /exoframe debug wear nudo}: telaio senza pezzi;</li>
 * <li>{@code /exoframe debug wear rottame [stato 0..3] [primario RRGGBB] [secondario RRGGBB]}: sei pezzi
 * {@link Tier#SCRAP} nella condizione data (0 intatto, 1 danneggiato, 2 critico, 3 rotto; 0 se omessa) e con i
 * colori dati in esadecimale senza {@code #} (non dipinti se omessi);</li>
 * <li>{@code /exoframe debug wear via}: elimina il telaio che il giocatore indossa, comunque ci sia entrato (anche uno
 * entrato da un telaio piazzato, con i suoi pezzi e la sua cella): l'armatura stivata gli torna, il record si toglie
 * dal registro e i segnaposto se ne vanno.</li>
 * </ul>
 * {@code nudo} e {@code rottame} creano il record indossato se il giocatore non ne ha uno, altrimenti ne sostituiscono
 * pezzi e cella (quelli che c'erano si eliminano), sempre con una cella carica; l'armatura stivata non si tocca. Il
 * ramo {@code place} piazza un telaio nel mondo ({@link FramePlacement}) al suolo sotto i piedi di chi lo esegue, con il
 * suo orientamento e lui come proprietario (rifiutato, come l'oggetto telaio, se li' il telaio non ci sta o se il punto
 * e' fuori dal bordo del mondo):
 * <ul>
 * <li>{@code /exoframe debug place nudo}: telaio vuoto;</li>
 * <li>{@code /exoframe debug place rottame}: i sei pezzi Rottame e una cella carica, fatti con gli oggetti come se li
 * montasse un giocatore.</li>
 * </ul>
 */
public final class ExoframeCommand {
    private static final String COMMANDS = "commands.redstone_exoframes.";
    private static final String LANG = COMMANDS + "debug.";
    private static final String NO_FRAME = COMMANDS + "no_frame";
    private static final String NO_FRAME_IN_SIGHT = COMMANDS + "no_frame_in_sight";
    private static final String NOT_OWNER = COMMANDS + "not_owner";
    /** Il livello di permesso degli operatori: rami {@code debug} e {@code remove}, telai altrui o senza padrone. */
    private static final int OPERATOR_LEVEL = 2;
    /** Fin dove, in blocchi dagli occhi, un comando trova il telaio piazzato che il giocatore guarda. */
    private static final double SIGHT = 5.0;
    private static final String STATE = "stato";
    private static final String PRIMARY = "primario";
    private static final String SECONDARY = "secondario";
    private static final Pattern HEX_COLOR = Pattern.compile("[0-9A-Fa-f]{6}");
    private static final DynamicCommandExceptionType INVALID_COLOR = new DynamicCommandExceptionType(
            value -> Component.translatable(LANG + "invalid_color", value));

    private ExoframeCommand() {
    }

    /** Registra il comando presso Architectury: lo aggiunge a ogni dispatcher di comandi del server. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("exoframe")
                .then(Commands.literal("unlock")
                        .executes(context -> setUnlocked(context, true)))
                .then(Commands.literal("lock")
                        .executes(context -> setUnlocked(context, false)))
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(ExoframeCommand::removeFrame))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .then(Commands.literal("wear")
                                .then(Commands.literal("nudo")
                                        .executes(context -> wear(context, SuitSummary.BARE, "wear.bare")))
                                .then(scrapBranch())
                                .then(Commands.literal("via")
                                        .executes(ExoframeCommand::debugRemove)))
                        .then(Commands.literal("place")
                                .then(Commands.literal("nudo")
                                        .executes(context -> place(context, false, "place.bare")))
                                .then(Commands.literal("rottame")
                                        .executes(context -> place(context, true, "place.scrap"))))));
    }

    /** {@code rottame [stato] [primario] [secondario]}: ogni argomento opzionale e' ammesso solo dopo i precedenti. */
    private static LiteralArgumentBuilder<CommandSourceStack> scrapBranch() {
        return Commands.literal("rottame")
                .executes(context -> wearScrap(context, 0, null, null))
                .then(Commands.argument(STATE, IntegerArgumentType.integer(0, 3))
                        .executes(context -> wearScrap(context, state(context), null, null))
                        .then(Commands.argument(PRIMARY, StringArgumentType.word())
                                .executes(context -> wearScrap(context, state(context),
                                        StringArgumentType.getString(context, PRIMARY), null))
                                .then(Commands.argument(SECONDARY, StringArgumentType.word())
                                        .executes(context -> wearScrap(context, state(context),
                                                StringArgumentType.getString(context, PRIMARY),
                                                StringArgumentType.getString(context, SECONDARY))))));
    }

    private static int state(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, STATE);
    }

    /**
     * Sei pezzi {@link Tier#SCRAP}; {@code state} e' l'indice in {@link PieceCondition}, gia' limitato a 0..3
     * dall'argomento. I colori si leggono prima di toccare l'inventario: un colore malformato non cambia nulla.
     */
    private static int wearScrap(CommandContext<CommandSourceStack> context, int state, String primary,
            String secondary) throws CommandSyntaxException {
        Appearance appearance = new Appearance(parseColor(primary), parseColor(secondary), List.of());
        PieceLook look = new PieceLook(Tier.SCRAP, PieceCondition.values()[state], appearance);
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            pieces.put(slot, look);
        }
        return wear(context, new SuitSummary(pieces), "wear.scrap");
    }

    private static int wear(CommandContext<CommandSourceStack> context, SuitSummary summary, String feedback)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DebugWear.wear(player, summary);
        context.getSource().sendSuccess(() -> Component.translatable(LANG + feedback), false);
        return 1;
    }

    /**
     * {@code via}: con un telaio indossato lo elimina ({@link DebugWear#remove}) e lo dice; senza, toglie solo i
     * segnaposto e dice quanti. Il risultato e' il numero di segnaposto tolti.
     */
    private static int debugRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean wearing = Compat.frameState().wornFrameId(player).isPresent();
        int removed = DebugWear.remove(player);
        if (wearing) {
            context.getSource().sendSuccess(() -> Component.translatable(LANG + "wear.removed"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable(LANG + "wear.placeholders_removed",
                    removed), false);
        }
        return removed;
    }

    /**
     * Sblocca o blocca il telaio indossato o guardato: con {@link FrameStateAccess#mutate} (e poi la sincronizzazione
     * del giocatore) se indossato, con {@link FrameStateAccess#updatePlaced} se piazzato. 0 senza telaio, se chi esegue
     * non ne e' il proprietario e non e' un operatore, o se il registro rifiuta il cambio (allora non si dice nulla).
     */
    private static int setUnlocked(CommandContext<CommandSourceStack> context, boolean unlocked)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = player.getServer();
        FrameStateAccess frames = Compat.frameState();
        Optional<FrameState> worn = frames.get(player);
        Optional<PlacedFrame> placed = worn.isPresent() ? Optional.empty()
                : frameInSight(player).flatMap(frame -> frames.placed(server, frame.frameId()));
        Optional<FrameState> state = worn.isPresent() ? worn : placed.map(PlacedFrame::state);
        if (state.isEmpty()) {
            source.sendFailure(Component.translatable(NO_FRAME));
            return 0;
        }
        if (!player.getUUID().equals(state.get().owner()) && !source.hasPermission(OPERATOR_LEVEL)) {
            source.sendFailure(Component.translatable(NOT_OWNER));
            return 0;
        }
        boolean changed;
        if (worn.isPresent()) {
            changed = frames.mutate(player, s -> s.setUnlocked(unlocked));
            if (changed) {
                frames.syncTo(player);
            }
        } else {
            changed = frames.updatePlaced(server, placed.get().frameId(), s -> s.setUnlocked(unlocked));
        }
        if (!changed) {
            return 0;
        }
        String feedback = COMMANDS + (unlocked ? "unlock.done" : "lock.done");
        source.sendSuccess(() -> Component.translatable(feedback), false);
        return 1;
    }

    /**
     * Elimina per sempre il telaio piazzato che l'operatore guarda (design §5.4: solo dal registro). Prima ne esce
     * tutto cio' che puo' diventare oggetto, a terra fermo ai piedi del telaio ({@link Handouts#dropStill}), e ogni
     * oggetto arriva a terra solo dopo aver lasciato il record: ogni pezzo montato con i moduli e l'aspetto del suo
     * slot ({@link FrameInteractions#pieceStack}), la cella con la sua carica, l'armatura stivata che si legge. Poi il
     * record si toglie ({@link FrameStateAccess#removePlaced}: le voci stivate che non si leggono vanno fra quelle dei
     * telai tolti) e l'entita' sparisce. Un pezzo del cui livello non c'e' un oggetto se ne va con il telaio: lo
     * nominano il messaggio e una riga di avviso nel log. 0 senza un telaio guardato o se il registro rifiuta.
     */
    private static int removeFrame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = player.getServer();
        FrameStateAccess frames = Compat.frameState();
        Optional<ExoframeEntity> frame = frameInSight(player);
        Optional<PlacedFrame> record = frame.flatMap(f -> frames.placed(server, f.frameId()));
        if (record.isEmpty()) {
            source.sendFailure(Component.translatable(NO_FRAME_IN_SIGHT));
            return 0;
        }
        UUID frameId = record.get().frameId();
        FrameState state = record.get().state();
        Level level = frame.get().level();
        Vec3 feet = record.get().position();
        List<String> deleted = new ArrayList<>();
        for (PieceSlot slot : PieceSlot.values()) {
            Piece piece = state.pieces().get(slot);
            if (piece == null) {
                continue;
            }
            ItemStack stack;
            try {
                stack = FrameInteractions.pieceStack(state, slot);
            } catch (IllegalArgumentException e) {
                deleted.add(piece.tier().name().toLowerCase(Locale.ROOT) + " "
                        + slot.name().toLowerCase(Locale.ROOT));
                continue;
            }
            if (!frames.updatePlaced(server, frameId, s -> s.unmount(slot))) {
                return 0;
            }
            Handouts.dropStill(level, feet, stack);
        }
        if (state.cell() != null) {
            ItemStack cell = CellItem.fromCell(state.cell());
            if (!frames.updatePlaced(server, frameId, FrameState::removeCell)) {
                return 0;
            }
            Handouts.dropStill(level, feet, cell);
        }
        for (StowedStack stowed : frames.takeStowedOfPlaced(server, frameId)) {
            Handouts.dropStill(level, feet, stowed.stack());
        }
        if (!frames.removePlaced(server, frameId)) {
            return 0;
        }
        frame.get().discard();
        FrameLog.event(frameId, "removed", player, (ServerLevel) level, feet);
        // come i comandi di amministrazione del gioco: la conferma va anche alla console e agli altri operatori
        if (deleted.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(COMMANDS + "remove.done"), true);
        } else {
            String names = String.join(", ", deleted);
            RedstoneExoframes.LOGGER.warn("Frame {} removed by {}; pieces without an item deleted with it: {}",
                    frameId, player.getScoreboardName(), names);
            source.sendSuccess(() -> Component.translatable(COMMANDS + "remove.done_deleted", names), true);
        }
        return 1;
    }

    /**
     * Il telaio piazzato che il giocatore guarda entro {@link #SIGHT} blocchi: la prima entita' telaio sulla linea di
     * vista dagli occhi, che si ferma al primo blocco solido (un muro nasconde il telaio che ha dietro).
     */
    private static Optional<ExoframeEntity> frameInSight(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(SIGHT);
        Vec3 end = eyes.add(reach);
        HitResult block = player.level().clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB around = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eyes, end, around,
                entity -> entity instanceof ExoframeEntity && !entity.isRemoved(), eyes.distanceToSqr(end));
        return hit == null ? Optional.empty() : Optional.of((ExoframeEntity) hit.getEntity());
    }

    /**
     * Piazza un telaio con chi esegue come proprietario al suolo sotto i suoi piedi, con il suo orientamento; armato
     * ({@code scrap}) con un pezzo Rottame per slot e una cella carica. 0 e un errore se li' il telaio non ci sta.
     */
    private static int place(CommandContext<CommandSourceStack> context, boolean scrap, String feedback)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        FrameState state = new FrameState(RedstoneExoframes.config());
        state.setOwner(player.getUUID());
        if (scrap) {
            for (RegistrySupplier<PieceItem> supplier : ModItems.PIECES) {
                PieceItem item = supplier.get();
                for (PieceSlot slot : item.kind().slots()) {
                    state.mount(item.toPiece(new ItemStack(item), slot));
                }
            }
            state.insertCell(ModItems.REDSTONE_CELL.get().toCell(CellItem.charged()));
        }
        float yaw = player.getYRot();
        Optional<ExoframeEntity> placed = FramePlacement.groundBelow(level, player.position())
                .flatMap(feet -> FramePlacement.place(level, state, feet, yaw));
        if (placed.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(ExoframeItem.NO_ROOM));
            return 0;
        }
        FrameLog.event(placed.get().frameId(), "placed", player, level, placed.get().position());
        context.getSource().sendSuccess(() -> Component.translatable(LANG + feedback), false);
        return 1;
    }

    /** Un colore {@code RRGGBB} in esadecimale; {@link Appearance#UNPAINTED} se {@code value} e' null. */
    private static int parseColor(String value) throws CommandSyntaxException {
        if (value == null) {
            return Appearance.UNPAINTED;
        }
        if (!HEX_COLOR.matcher(value).matches()) {
            throw INVALID_COLOR.create(value);
        }
        return Integer.parseInt(value, 16);
    }
}
