package mattonfire.dnd.classes.Commands;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /dndclass get <player>
 * /dndclass set <player> <class>
 *
 * Changes a player's class without them having to die. Setting "none" clears the
 * class and reopens the class picker on their client.
 */
public class DndClassCommand {
    private static final DynamicCommandExceptionType UNKNOWN_CLASS = new DynamicCommandExceptionType(
            name -> Text.literal("Unknown class: " + name));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndclass")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::get)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("class", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(DndCharacter.values()).map(DndClassCommand::name),
                                                builder))
                                        .executes(DndClassCommand::set)))));
    }

    private static int get(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        DndCharacter dndClass = ((PlayerEntityExt) player).getDndClass();
        if (dndClass == null) {
            dndClass = DndCharacter.NONE;
        }
        String className = name(dndClass);
        context.getSource().sendFeedback(
                Text.literal(player.getEntityName() + " is " + className), false);
        return dndClass.getValue();
    }

    private static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String className = StringArgumentType.getString(context, "class");
        DndCharacter dndClass = Arrays.stream(DndCharacter.values())
                .filter(c -> name(c).equalsIgnoreCase(className))
                .findFirst()
                .orElseThrow(() -> UNKNOWN_CLASS.create(className));

        DnDClasses.applyClass(player, dndClass);
        context.getSource().sendFeedback(
                Text.literal("Set " + player.getEntityName() + "'s class to " + name(dndClass)), true);
        return 1;
    }

    private static String name(DndCharacter dndClass) {
        return dndClass.name().toLowerCase(Locale.ROOT);
    }
}
