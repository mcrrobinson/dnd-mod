package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * /dndhome [player]: which racial home the player stands in, the piece under their feet, and whether
 * it's their own home and hearth.
 * /dndhome pieces: every piece of that settlement with its box, to find rooms (e.g. a forge) by hand.
 */
public final class DndHomeCommand {
    private DndHomeCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndhome")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> info(context, context.getSource().getPlayerOrThrow()))
                .then(CommandManager.literal("pieces").executes(DndHomeCommand::pieces))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> info(context, EntityArgumentType.getPlayer(context, "player")))));
    }

    private static int info(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        RacialHomes.Visit visit = RacialHomes.homeAt(player.getWorld(), pos);
        if (visit == null) {
            context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " is in no racial home"), false);
            return 0;
        }
        StructurePiece piece = visit.pieceAt(pos);
        String pieceName = piece == null ? "none" : String.valueOf(Registries.STRUCTURE_PIECE.getId(piece.getType()));
        boolean own = visit.home().isHomeOf(RaceLifecycle.activeRaceOf(player));
        context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " is in "
                + visit.home().structure() + " (home of " + visit.home().race().id() + ", start chunk "
                + visit.start().getPos() + "), piece " + pieceName + ", own home " + own
                + ", hearth " + (own && visit.inHearth(pos))), false);
        return own ? 2 : 1;
    }

    private static int pieces(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        RacialHomes.Visit visit = RacialHomes.homeAt(player.getWorld(), player.getBlockPos());
        if (visit == null) {
            context.getSource().sendFeedback(Text.literal("[Home] Not in a racial home"), false);
            return 0;
        }
        for (StructurePiece piece : visit.start().getChildren()) {
            BlockBox box = piece.getBoundingBox();
            context.getSource().sendFeedback(Text.literal("[Home] piece " + Registries.STRUCTURE_PIECE.getId(piece.getType())
                    + " " + box.getMinX() + " " + box.getMinY() + " " + box.getMinZ()
                    + " to " + box.getMaxX() + " " + box.getMaxY() + " " + box.getMaxZ()), false);
        }
        return visit.start().getChildren().size();
    }
}
