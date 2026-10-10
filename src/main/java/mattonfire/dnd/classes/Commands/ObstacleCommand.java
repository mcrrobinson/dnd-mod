package mattonfire.dnd.classes.Commands;

import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleText;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * /dndobstacle place &lt;from&gt; &lt;to&gt; &lt;type&gt; &lt;tier&gt; [critical]   fill a box with one sealed obstacle group
 * /dndobstacle info &lt;pos&gt;                                  type, tier, state and group of an obstacle
 * /dndobstacle open|reset &lt;pos&gt;                            open or reseal its whole group
 * /dndobstacle settier &lt;pos&gt; &lt;tier&gt;                       retier its whole group
 * /dndobstacle reseal &lt;pos&gt; &lt;ticks&gt;                       reseal the group this long after it opens (0 = never)
 * /dndobstacle list [radius]                              obstacle groups near you (default 64 blocks)
 *
 * Op level 2. The DM toolkit's seam for obstacles.
 */
public final class ObstacleCommand {
    private static final SimpleCommandExceptionType NOT_OBSTACLE = new SimpleCommandExceptionType(
            Text.literal("That isn't an obstacle block"));
    private static final DynamicCommandExceptionType UNKNOWN_TYPE = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown obstacle type: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_TIER = new DynamicCommandExceptionType(
            name -> Text.literal("Unknown tier: " + name + " (easy, medium, hard, very_hard)"));
    private static final SimpleCommandExceptionType TOO_BIG = new SimpleCommandExceptionType(
            Text.literal("An obstacle group can have at most " + ObstacleGroups.MAX_BLOCKS + " blocks"));

    private ObstacleCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndobstacle")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("place")
                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("type", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                                        ObstacleTypes.all().stream().map(ObstacleType::id), builder))
                                                .then(tierArgument()
                                                        .executes(context -> place(context, false))
                                                        .then(CommandManager.argument("critical", BoolArgumentType.bool())
                                                                .executes(context -> place(context,
                                                                        BoolArgumentType.getBool(context, "critical")))))))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(ObstacleCommand::info)))
                .then(CommandManager.literal("open")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> setState(context, true))))
                .then(CommandManager.literal("reset")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> setState(context, false))))
                .then(CommandManager.literal("settier")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(tierArgument().executes(ObstacleCommand::setTier))))
                .then(CommandManager.literal("reseal")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(ObstacleCommand::reseal))))
                .then(CommandManager.literal("list")
                        .executes(context -> list(context, 64))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 512))
                                .executes(context -> list(context, IntegerArgumentType.getInteger(context, "radius"))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> tierArgument() {
        return CommandManager.argument("tier", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        Arrays.stream(Tier.values()).map(Tier::asString), builder));
    }

    private static Tier tier(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "tier");
        Tier tier = Tier.byName(name, null);
        if (tier == null) {
            throw UNKNOWN_TIER.create(name);
        }
        return tier;
    }

    private static ObstacleBlockEntity obstacleAt(CommandContext<ServerCommandSource> context, BlockPos pos)
            throws CommandSyntaxException {
        if (context.getSource().getWorld().getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
            return be;
        }
        throw NOT_OBSTACLE.create();
    }

    private static int place(CommandContext<ServerCommandSource> context, boolean critical) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos from = BlockPosArgumentType.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getLoadedBlockPos(context, "to");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "type");
        ObstacleType type = ObstacleTypes.byId(id);
        if (type == null) {
            throw UNKNOWN_TYPE.create(id);
        }
        Tier tier = tier(context);
        BlockBox box = BlockBox.create(from, to);
        if (box.getBlockCountX() * box.getBlockCountY() * box.getBlockCountZ() > ObstacleGroups.MAX_BLOCKS) {
            throw TOO_BIG.create();
        }
        int placed = ObstaclePlacer.box(world, null, from, to, type, tier, critical, world.getRandom().nextLong());
        context.getSource().sendFeedback(Text.literal("Placed " + placed + " block" + (placed == 1 ? "" : "s") + " of ")
                .append(ObstacleText.name(type, tier)).append(critical ? ", critical" : ""), true);
        return placed;
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        ObstacleBlockEntity be = obstacleAt(context, pos);
        BlockState state = world.getBlockState(pos);
        ObstacleType type = ((ObstacleBlock) state.getBlock()).type();
        int size = ObstacleGroups.group(world, pos).size();
        context.getSource().sendFeedback(ObstacleText.name(type, be.tier())
                .append(": " + type.id() + ", " + state.get(ObstacleBlock.STATE).asString()
                        + ", DC " + be.tier().dc + ", group of " + size
                        + (be.critical() ? ", critical" : "")
                        + (be.resealTicks() > 0 ? ", reseals after " + be.resealTicks() + " ticks" : "")
                        + ". " + ObstacleText.solverList(type) + " can attempt it"), false);
        return size;
    }

    private static int setState(CommandContext<ServerCommandSource> context, boolean open) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        obstacleAt(context, pos);
        int changed = open ? ObstacleGroups.open(world, pos) : ObstacleGroups.seal(world, pos);
        context.getSource().sendFeedback(Text.literal((open ? "Opened " : "Resealed ") + changed + " obstacle block"
                + (changed == 1 ? "" : "s")), true);
        return changed;
    }

    private static int setTier(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        obstacleAt(context, pos);
        Tier tier = tier(context);
        int size = ObstacleGroups.setTier(world, pos, tier);
        context.getSource().sendFeedback(Text.literal("Set " + size + " obstacle block" + (size == 1 ? "" : "s")
                + " to ").append(Text.translatable(tier.translationKey())).append(" (DC " + tier.dc + ")"), true);
        return size;
    }

    private static int reseal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        obstacleAt(context, pos);
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        int size = ObstacleGroups.setResealTicks(world, pos, ticks);
        context.getSource().sendFeedback(Text.literal(ticks == 0 ? "The obstacle no longer reseals"
                : "The obstacle reseals " + ticks + " ticks after opening"), true);
        return size;
    }

    private static int list(CommandContext<ServerCommandSource> context, int radius) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        List<BlockPos> positions = ObstacleIndex.get(world).within(center, radius);
        // One line per group, from the first block of it we come to
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        int groups = 0;
        for (BlockPos pos : positions) {
            if (seen.contains(pos) || !(world.getBlockEntity(pos) instanceof ObstacleBlockEntity be)
                    || !(world.getBlockState(pos).getBlock() instanceof ObstacleBlock block)) {
                continue;
            }
            List<BlockPos> group = ObstacleGroups.group(world, pos);
            seen.addAll(group);
            groups++;
            source.sendFeedback(ObstacleText.name(block.type(), be.tier()).append(" at " + pos.toShortString()
                    + ": " + world.getBlockState(pos).get(ObstacleBlock.STATE).asString()
                    + ", " + group.size() + " block" + (group.size() == 1 ? "" : "s")), false);
        }
        if (groups == 0) {
            source.sendFeedback(Text.literal("No obstacles within " + radius + " blocks"), false);
        }
        return groups;
    }
}
