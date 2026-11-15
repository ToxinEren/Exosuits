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
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ExoframeItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugEnergy;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FrameLog;
import com.toxineren.exoframes.frame.FramePlacement;
import com.toxineren.exoframes.frame.FrameRemoval;
import com.toxineren.exoframes.frame.FrameSight;
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
import net.minecraft.world.item.ItemStack;

/**
 * Comando {@code /exoframe} (design §5.5). Si riferisce al telaio che chi lo esegue indossa oppure, se non ne indossa,
 * a quello piazzato che guarda entro {@link FrameSight#SIGHT} blocchi ({@link FrameSight#inSight}); serve un
 * giocatore, non la console. Per tutti:
 * <ul>
 * <li>{@code /exoframe unlock} e {@code /exoframe lock}: sblocca o blocca il telaio per gli altri giocatori; solo il
 * proprietario o un operatore (livello di permesso 2), e un telaio senza proprietario solo un operatore. Il cambio
 * passa dal registro; gia' in quello stato riesce lo stesso.</li>
 * </ul>
 * Solo per gli operatori (livello 2; senza, i rami non esistono per quella sorgente):
 * <ul>
 * <li>{@code /exoframe remove}: elimina per sempre il telaio piazzato che guarda (mai uno indossato); quello che puo'
 * diventare oggetto resta a terra ai suoi piedi, vedi {@link FrameRemoval#remove}. La conferma va anche alla console e
 * agli altri operatori, come per i comandi di amministrazione del gioco.</li>
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
 * Il ramo {@code energy} legge e imposta la carica della cella del telaio indossato ({@link DebugEnergy}; design
 * §12.14), per portarla a {@code LOW} o {@code EMPTY} senza aspettare:
 * <ul>
 * <li>{@code /exoframe debug energy}: scrive carica, capacita' e stato dell'energia (normale, bassa, esaurita); il
 * risultato del comando e' la carica;</li>
 * <li>{@code /exoframe debug energy <re>}: porta la carica a {@code re} RE, limitata alla capacita' della cella,
 * passando dal registro con un cambio strutturale che sincronizza subito il proprietario.</li>
 * </ul>
 * Senza telaio indossato o senza cella i due rami lo dicono e non cambiano nulla; con il registro in sola lettura
 * {@code energy <re>} non scrive nulla in chat.
 */
public final class ExoframeCommand {
    private static final String COMMANDS = "commands.redstone_exoframes.";
    private static final String LANG = COMMANDS + "debug.";
    private static final String NO_FRAME = COMMANDS + "no_frame";
    private static final String NO_FRAME_IN_SIGHT = COMMANDS + "no_frame_in_sight";
    private static final String NOT_OWNER = COMMANDS + "not_owner";
    /** Il livello di permesso degli operatori: rami {@code debug} e {@code remove}, telai altrui o senza padrone. */
    private static final int OPERATOR_LEVEL = 2;
    private static final String STATE = "stato";
    private static final String PRIMARY = "primario";
    private static final String SECONDARY = "secondario";
    private static final String ENERGY = "re";
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
                                        .executes(context -> place(context, true, "place.scrap"))))
                        .then(Commands.literal("energy")
                                .executes(ExoframeCommand::showEnergy)
                                .then(Commands.argument(ENERGY, IntegerArgumentType.integer(0))
                                        .executes(ExoframeCommand::setEnergy)))));
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
     * {@code energy} senza argomento: scrive carica, capacita' e stato dell'energia della cella indossata; il risultato
     * e' la carica, cosi' {@code execute store result} la legge. 0 e un errore senza telaio indossato o senza cella.
     */
    private static int showEnergy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        FrameStateAccess frames = Compat.frameState();
        Optional<FrameState> worn = frames.wornFrameId(player).isPresent() ? frames.get(player) : Optional.empty();
        if (worn.isEmpty()) {
            source.sendFailure(Component.translatable(LANG + "energy.not_wearing"));
            return 0;
        }
        EnergyCell cell = worn.get().cell();
        if (cell == null) {
            source.sendFailure(Component.translatable(LANG + "energy.no_cell"));
            return 0;
        }
        int stored = cell.stored();
        int capacity = cell.capacity();
        EnergyState state = RedstoneExoframes.rules().evaluate(worn.get()).energyState();
        source.sendSuccess(() -> Component.translatable(LANG + "energy.show", stored, capacity,
                Component.translatable("energy." + RedstoneExoframes.MOD_ID + "."
                        + state.name().toLowerCase(Locale.ROOT))), false);
        return stored;
    }

    /**
     * {@code energy <re>}: porta la carica della cella indossata a {@code re}, limitata alla capacita'
     * ({@link DebugEnergy#set}), e scrive la carica letta dopo. 0 e un errore senza telaio indossato o senza cella; 0
     * senza messaggio se il registro rifiuta il cambio (resta la riga del log del registro).
     */
    private static int setEnergy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        switch (DebugEnergy.set(player, IntegerArgumentType.getInteger(context, ENERGY))) {
            case DONE -> {
                int stored = DebugEnergy.cell(player).map(EnergyCell::stored).orElse(0);
                source.sendSuccess(() -> Component.translatable(LANG + "energy.set", stored), false);
                return 1;
            }
            case NOT_WEARING -> {
                source.sendFailure(Component.translatable(LANG + "energy.not_wearing"));
                return 0;
            }
            case NO_CELL -> {
                source.sendFailure(Component.translatable(LANG + "energy.no_cell"));
                return 0;
            }
            case REFUSED -> {
                return 0;
            }
            default -> throw new IllegalStateException("unknown outcome");
        }
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
                : FrameSight.inSight(player).flatMap(frame -> frames.placed(server, frame.frameId()));
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
     * Elimina per sempre il telaio piazzato che l'operatore guarda ({@link FrameRemoval#remove}: cio' che puo'
     * diventare oggetto resta a terra ai suoi piedi). Un pezzo del cui livello non c'e' un oggetto se ne va con il
     * telaio: lo nominano il messaggio e una riga di avviso nel log. 0 senza un telaio guardato o se il registro
     * rifiuta (allora non si dice nulla).
     */
    private static int removeFrame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = player.getServer();
        FrameStateAccess frames = Compat.frameState();
        Optional<ExoframeEntity> frame = FrameSight.inSight(player);
        Optional<PlacedFrame> record = frame.flatMap(f -> frames.placed(server, f.frameId()));
        if (record.isEmpty()) {
            source.sendFailure(Component.translatable(NO_FRAME_IN_SIGHT));
            return 0;
        }
        UUID frameId = record.get().frameId();
        // i livelli servono al messaggio: si leggono prima che i pezzi lascino il record
        Map<PieceSlot, Tier> tiers = new EnumMap<>(PieceSlot.class);
        record.get().state().pieces().forEach((slot, piece) -> tiers.put(slot, piece.tier()));
        FrameRemoval.Result result = FrameRemoval.remove(player, frame.get());
        if (!result.removed()) {
            return 0;
        }
        // come i comandi di amministrazione del gioco: la conferma va anche alla console e agli altri operatori
        if (result.deleted().isEmpty()) {
            source.sendSuccess(() -> Component.translatable(COMMANDS + "remove.done"), true);
        } else {
            List<String> deleted = new ArrayList<>();
            for (PieceSlot slot : result.deleted()) {
                deleted.add(tiers.get(slot).name().toLowerCase(Locale.ROOT) + " "
                        + slot.name().toLowerCase(Locale.ROOT));
            }
            String names = String.join(", ", deleted);
            RedstoneExoframes.LOGGER.warn("Frame {} removed by {}; pieces without an item deleted with it: {}",
                    frameId, player.getScoreboardName(), names);
            source.sendSuccess(() -> Component.translatable(COMMANDS + "remove.done_deleted", names), true);
        }
        return 1;
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
