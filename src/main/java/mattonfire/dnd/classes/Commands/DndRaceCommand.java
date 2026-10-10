package mattonfire.dnd.classes.Commands;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.classes.ClassLifecycle;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /dndrace get <player>
 * /dndrace set <player> <race> [ancestry]
 * /dndrace list
 *
 * Operator control of races. Setting "none" clears the race and reopens the race picker.
 */
public final class DndRaceCommand {
    private static final DynamicCommandExceptionType UNKNOWN_RACE = new DynamicCommandExceptionType(
            name -> Text.literal("Unknown race: " + name));
    private static final DynamicCommandExceptionType UNKNOWN_ANCESTRY = new DynamicCommandExceptionType(
            name -> Text.literal("Unknown ancestry: " + name + " (ember, frost or storm)"));
    private static final SimpleCommandExceptionType NEEDS_ANCESTRY = new SimpleCommandExceptionType(
            Text.literal("Dragonborn need an ancestry: ember, frost or storm"));

    private DndRaceCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndrace")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndRaceCommand::get)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("race", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(DndRace.values()).map(DndRace::id), builder))
                                        .executes(context -> set(context, null))
                                        .then(CommandManager.argument("ancestry", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        Arrays.stream(DragonAncestry.values())
                                                                .filter(a -> a != DragonAncestry.NONE)
                                                                .map(DragonAncestry::id),
                                                        builder))
                                                .executes(context -> set(context,
                                                        StringArgumentType.getString(context, "ancestry")))))))
                .then(CommandManager.literal("list").executes(DndRaceCommand::list)));
    }

    private static String describe(ServerPlayerEntity player) {
        DndRace race = RaceLifecycle.raceOf(player);
        DragonAncestry ancestry = RaceLifecycle.ancestryOf(player);
        return race.id() + (ancestry == DragonAncestry.NONE ? "" : " " + ancestry.id());
    }

    private static int get(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String off = RaceLifecycle.enabled(player) ? "" : " (dndRaces is off, so it has no effect)";
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + " is " + describe(player) + off),
                false);
        return RaceLifecycle.raceOf(player).getValue();
    }

    private static int set(CommandContext<ServerCommandSource> context, String ancestryName)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String raceName = StringArgumentType.getString(context, "race");
        DndRace race = DndRace.byId(raceName);
        if (race == null) {
            throw UNKNOWN_RACE.create(raceName);
        }
        DragonAncestry ancestry = DragonAncestry.NONE;
        if (ancestryName != null) {
            ancestry = DragonAncestry.byId(ancestryName);
            if (ancestry == null || ancestry == DragonAncestry.NONE) {
                throw UNKNOWN_ANCESTRY.create(ancestryName);
            }
        }
        if (race.hasAncestry() && ancestry == DragonAncestry.NONE) {
            throw NEEDS_ANCESTRY.create();
        }
        RaceLifecycle.change(player, race, ancestry);
        context.getSource().sendFeedback(
                Text.literal("Set " + player.getEntityName() + "'s race to " + describe(player)), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        var players = context.getSource().getServer().getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
            RaceInfo info = RaceInfo.get(RaceLifecycle.raceOf(player));
            String race = info == null ? "none" : RaceLifecycle.displayName(info, RaceLifecycle.ancestryOf(player));
            context.getSource().sendFeedback(Text.literal(player.getEntityName() + ": " + race + ", "
                    + ClassLifecycle.classOf(player).name().toLowerCase(Locale.ROOT)), false);
        }
        return players.size();
    }
}
