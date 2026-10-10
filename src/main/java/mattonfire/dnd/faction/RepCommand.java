package mattonfire.dnd.faction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * /rep                                    your standing with every faction
 * /rep &lt;player&gt;                           someone else's (op)
 * /rep &lt;player&gt; &lt;faction&gt; set|add &lt;n&gt;     change it (op)
 */
public final class RepCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown faction " + id));

    private RepCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rep")
                .executes(context -> show(context.getSource(), context.getSource().getPlayerOrThrow()))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> show(context.getSource(), EntityArgumentType.getPlayer(context, "player")))
                        .then(CommandManager.argument("faction", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                        Factions.all().stream().map(Faction::id), builder))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("value",
                                                        IntegerArgumentType.integer(Reputation.MIN, Reputation.MAX))
                                                .executes(context -> change(context, true))))
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("value",
                                                        IntegerArgumentType.integer(-2 * Reputation.MAX, 2 * Reputation.MAX))
                                                .executes(context -> change(context, false)))))));
    }

    private static int show(ServerCommandSource source, ServerPlayerEntity player) {
        if (Factions.all().isEmpty()) {
            source.sendFeedback(Text.literal("No factions loaded"), false);
            return 0;
        }
        source.sendFeedback(Text.literal("Reputation of ").append(player.getDisplayName()).append(":"), false);
        for (Faction faction : Factions.all()) {
            source.sendFeedback(line(player, faction), false);
        }
        return Factions.all().size();
    }

    private static Text line(ServerPlayerEntity player, Faction faction) {
        int value = Reputation.get(player, faction);
        return Text.literal("  ").append(faction.displayName())
                .append(Text.literal(" [" + faction.id() + "]: " + value + " "))
                .append(Text.literal("(").append(ReputationTier.of(value).displayName()).append(")"));
    }

    private static int change(CommandContext<ServerCommandSource> context, boolean set) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "faction");
        Faction faction = Factions.get(id);
        if (faction == null) {
            throw UNKNOWN.create(id);
        }
        int value = IntegerArgumentType.getInteger(context, "value");
        Reputation.set(player, faction, set ? value : Reputation.get(player, faction) + value);
        context.getSource().sendFeedback(Text.literal("Set ").append(player.getDisplayName()).append("'s ")
                .append(line(player, faction).copy()), true);
        return Reputation.get(player, faction);
    }
}
