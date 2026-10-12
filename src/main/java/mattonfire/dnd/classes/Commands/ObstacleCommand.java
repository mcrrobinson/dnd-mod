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

import mattonfire.dnd.classes.Obstacles.CryptSleepers;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleText;
import mattonfire.dnd.classes.Obstacles.ObstacleTierProcessor;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Obstacles.VanillaTraps;
import mattonfire.dnd.classes.Obstacles.types.WinchType;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.mob.MobEntity;
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
 * /dndobstacle link &lt;winch&gt; &lt;target&gt;                      connect a winch to its portcullis or chasm bridge
 * /dndobstacle list [radius]                              obstacle groups near you (default 64 blocks)
 * /dndobstacle sleep|wake [radius]                        lay undead near you down as crypt sleepers, or wake them (default 8)
 * /dndobstacle template &lt;template&gt; &lt;pos&gt; [&lt;tier&gt;|depth &lt;n&gt;]  place a structure template through the obstacle_tier processor
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
    private static final SimpleCommandExceptionType NOT_WINCH = new SimpleCommandExceptionType(
            Text.literal("That isn't a winch"));
    private static final DynamicCommandExceptionType WRONG_TARGET = new DynamicCommandExceptionType(
            name -> Text.literal("This winch works a ").append((Text) name).append(", and that isn't one"));
    private static final DynamicCommandExceptionType UNKNOWN_TEMPLATE = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown structure template: " + id));
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
                .then(CommandManager.literal("link")
                        .then(CommandManager.argument("winch", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("target", BlockPosArgumentType.blockPos())
                                        .executes(ObstacleCommand::link))))
                .then(CommandManager.literal("sleep")
                        .executes(context -> sleepers(context, 8, true))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 64))
                                .executes(context -> sleepers(context,
                                        IntegerArgumentType.getInteger(context, "radius"), true))))
                .then(CommandManager.literal("wake")
                        .executes(context -> sleepers(context, 8, false))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 64))
                                .executes(context -> sleepers(context,
                                        IntegerArgumentType.getInteger(context, "radius"), false))))
                .then(CommandManager.literal("traps")
                        .executes(context -> traps(context, false))
                        .then(CommandManager.literal("tp")
                                .executes(context -> traps(context, true))))
                .then(CommandManager.literal("template")
                        .then(CommandManager.argument("template", IdentifierArgumentType.identifier())
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(context -> template(context,
                                                new ObstacleTierProcessor(null, null, null), "as saved"))
                                        .then(CommandManager.literal("depth")
                                                .then(CommandManager.argument("depth", IntegerArgumentType.integer(0, 64))
                                                        .executes(context -> template(context,
                                                                ObstacleTierProcessor.ofDepth(
                                                                        IntegerArgumentType.getInteger(context, "depth")),
                                                                "depth " + IntegerArgumentType.getInteger(context, "depth")))))
                                        .then(tierArgument().executes(context -> template(context,
                                                ObstacleTierProcessor.ofTier(tier(context)), tier(context).asString()))))))
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

    /** Places a structure template through the {@code dndclasses:obstacle_tier} processor, as a dungeon would. */
    private static int template(CommandContext<ServerCommandSource> context, ObstacleTierProcessor processor,
            String how) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "template");
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        java.util.Optional<net.minecraft.structure.StructureTemplate> template = world.getStructureTemplateManager()
                .getTemplate(id);
        if (template.isEmpty()) {
            throw UNKNOWN_TEMPLATE.create(id);
        }
        net.minecraft.structure.StructurePlacementData data = new net.minecraft.structure.StructurePlacementData()
                .addProcessor(processor);
        template.get().place(world, pos, pos, data, world.getRandom(), net.minecraft.block.Block.NOTIFY_LISTENERS);
        context.getSource().sendFeedback(Text.literal("Placed template " + id + " at " + pos.toShortString()
                + " through dndclasses:obstacle_tier (" + how + ")"), true);
        return 1;
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
        int changed = open ? ObstacleGroups.open(world, pos) : ObstacleGroups.reset(world, pos);
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

    private static int link(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        BlockPos winch = BlockPosArgumentType.getLoadedBlockPos(context, "winch");
        BlockPos target = BlockPosArgumentType.getLoadedBlockPos(context, "target");
        obstacleAt(context, winch);
        if (!(((ObstacleBlock) world.getBlockState(winch).getBlock()).type() instanceof WinchType type)) {
            throw NOT_WINCH.create();
        }
        if (!type.link(world, winch, target)) {
            throw WRONG_TARGET.create(Text.translatable(type.targetType().translationKey()));
        }
        context.getSource().sendFeedback(Text.literal("Linked the ").append(Text.translatable(type.translationKey()))
                .append(" to " + target.toShortString()), true);
        return 1;
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

    /** How far {@code /dndobstacle traps} looks for vanilla temple traps. */
    private static final int TRAPS_RADIUS = 256;

    /**
     * Lists the vanilla temple traps (desert pyramid plates, jungle temple tripwires) within
     * {@value #TRAPS_RADIUS} blocks, or teleports the player to a safe spot beside the nearest trigger.
     */
    private static int traps(CommandContext<ServerCommandSource> context, boolean teleport)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        net.minecraft.util.math.Vec3d at = source.getPosition();
        BlockPos nearest = null;
        double best = (double) TRAPS_RADIUS * TRAPS_RADIUS;
        int count = 0;
        for (VanillaTraps.Trap trap : VanillaTraps.get(world).all()) {
            BlockPos closest = null;
            double closestSq = Double.MAX_VALUE;
            for (BlockPos trigger : trap.triggers()) {
                double d = trigger.toCenterPos().squaredDistanceTo(at);
                if (d < closestSq) {
                    closestSq = d;
                    closest = trigger;
                }
            }
            if (closest == null || closestSq > (double) TRAPS_RADIUS * TRAPS_RADIUS) {
                continue;
            }
            count++;
            if (!teleport) {
                source.sendFeedback(Text.translatable(trap.kind.translationKey()).append(" at "
                        + closest.toShortString() + ": " + trap.triggers().size() + " trigger blocks, "
                        + (trap.disarmed() ? "disarmed" : "armed")), false);
            }
            if (closestSq < best) {
                best = closestSq;
                nearest = closest;
            }
        }
        if (nearest == null) {
            source.sendFeedback(Text.literal("No temple traps within " + TRAPS_RADIUS + " blocks"), false);
            return 0;
        }
        if (teleport) {
            BlockPos spot = standingSpot(world, nearest);
            source.getPlayerOrThrow().teleport(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    source.getPlayerOrThrow().getYaw(), source.getPlayerOrThrow().getPitch());
            source.sendFeedback(Text.literal("Teleported beside the trap trigger at " + nearest.toShortString()
                    + " (standing at " + spot.toShortString() + ")"), false);
        }
        return count;
    }

    /** An air spot with solid ground, beside but not on {@code trigger} (diagonals first). */
    private static BlockPos standingSpot(ServerWorld world, BlockPos trigger) {
        int[][] offsets = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
        for (int dy = 0; dy <= 1; dy++) {
            for (int[] o : offsets) {
                BlockPos p = trigger.add(o[0], dy, o[1]);
                if (world.getBlockState(p).isAir() && world.getBlockState(p.up()).isAir()
                        && world.getBlockState(p.down()).isSolidBlock(world, p.down())) {
                    return p;
                }
            }
        }
        return trigger.up();
    }

    /** Lays every undead mob within {@code radius} down as a crypt sleeper, or wakes the sleepers there. */
    private static int sleepers(CommandContext<ServerCommandSource> context, int radius, boolean sleep) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(source.getPosition(), source.getPosition())
                .expand(radius);
        int count = 0;
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, m -> m.isAlive()
                && (sleep ? m.getGroup() == EntityGroup.UNDEAD && !CryptSleepers.isSleeper(m) : CryptSleepers.isSleeper(m)))) {
            if (sleep) {
                CryptSleepers.sleep(mob);
            } else {
                CryptSleepers.wake(world, mob, null);
            }
            count++;
        }
        int total = count;
        source.sendFeedback(Text.literal((sleep ? "Laid down " : "Woke ") + total + " crypt sleeper"
                + (total == 1 ? "" : "s") + " within " + radius + " blocks"), true);
        return total;
    }
}
