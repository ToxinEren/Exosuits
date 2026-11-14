package com.toxineren.exoframes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.toxineren.exoframes.RedstoneExoframes;
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
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FramePlacement;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Comando {@code /exoframe}, livello di permesso 2. Per ora solo rami di debug: quello che veste chi lo esegue con i
 * segnaposto del telaio ({@link DebugWear}):
 * <ul>
 * <li>{@code /exoframe debug wear nudo}: telaio senza pezzi;</li>
 * <li>{@code /exoframe debug wear rottame [stato 0..3] [primario RRGGBB] [secondario RRGGBB]}: sei pezzi
 * {@link Tier#SCRAP} nella condizione data (0 intatto, 1 danneggiato, 2 critico, 3 rotto; 0 se omessa) e con i
 * colori dati in esadecimale senza {@code #} (non dipinti se omessi);</li>
 * <li>{@code /exoframe debug wear via}: toglie i segnaposto.</li>
 * </ul>
 * e il ramo che piazza un telaio nel mondo ({@link FramePlacement}) al suolo sotto i piedi di chi lo esegue, con il suo
 * orientamento e lui come proprietario (rifiutato, come l'oggetto telaio, se li' il telaio non ci sta):
 * <ul>
 * <li>{@code /exoframe debug place nudo}: telaio vuoto;</li>
 * <li>{@code /exoframe debug place rottame}: i sei pezzi Rottame e una cella carica, fatti con gli oggetti come se li
 * montasse un giocatore.</li>
 * </ul>
 */
public final class ExoframeCommand {
    private static final String LANG = "commands.redstone_exoframes.debug.";
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
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("wear")
                                .then(Commands.literal("nudo")
                                        .executes(context -> wear(context, SuitSummary.BARE, "wear.bare")))
                                .then(scrapBranch())
                                .then(Commands.literal("via")
                                        .executes(ExoframeCommand::remove)))
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

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int removed = DebugWear.remove(player);
        context.getSource().sendSuccess(() -> Component.translatable(LANG + "wear.removed", removed), false);
        return removed;
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
        boolean placed = FramePlacement.groundBelow(level, player.position())
                .flatMap(feet -> FramePlacement.place(level, state, feet, yaw))
                .isPresent();
        if (!placed) {
            context.getSource().sendFailure(Component.translatable(ExoframeItem.NO_ROOM));
            return 0;
        }
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
