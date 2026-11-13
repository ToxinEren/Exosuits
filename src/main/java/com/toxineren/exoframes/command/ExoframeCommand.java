package com.toxineren.exoframes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.frame.DebugWear;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando {@code /exoframe}, livello di permesso 2. Per ora solo il ramo di debug che veste chi lo esegue con i
 * segnaposto del telaio ({@link DebugWear}):
 * <ul>
 * <li>{@code /exoframe debug wear nudo}: telaio senza pezzi;</li>
 * <li>{@code /exoframe debug wear rottame [stato 0..3] [primario RRGGBB] [secondario RRGGBB]}: sei pezzi
 * {@link Tier#SCRAP} nella condizione data (0 intatto, 1 danneggiato, 2 critico, 3 rotto; 0 se omessa) e con i
 * colori dati in esadecimale senza {@code #} (non dipinti se omessi);</li>
 * <li>{@code /exoframe debug wear via}: toglie i segnaposto.</li>
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
                                        .executes(ExoframeCommand::remove)))));
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
