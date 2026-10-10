package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.dungeon.DungeonCombat;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * /dungeon info          the dungeon you're in or standing over: name, theme, tier, rooms and their states
 * /dungeon clear         mark it cleared (boss defeated, every room open); fires the CLEARED event
 * /dungeon reset         repopulate it now: every room untouched, the boss available again (coffer claims stay)
 * /dungeon tier <1-4>    change its challenge tier
 * /dungeon trigger <room> start that room's fight now, for your party (you needn't be in the room)
 * /dungeon cutaway       (testing) remove everything from 3 blocks above its floor up to the sky,
 *                        so its layout can be screenshotted from above. Destructive.
 *
 * Op level 2. Use /place structure dndclasses:crypt and /locate structure #dndclasses:dungeons to find one.
 */
public class DungeonCommand {
    private static final SimpleCommandExceptionType NONE = new SimpleCommandExceptionType(
            Text.literal("No dungeon here: stand in one, or on the surface above it"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dungeon")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("info").executes(DungeonCommand::info))
                .then(CommandManager.literal("clear").executes(DungeonCommand::clear))
                .then(CommandManager.literal("reset").executes(DungeonCommand::reset))
                .then(CommandManager.literal("tier")
                        .then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 4)).executes(DungeonCommand::tier)))
                .then(CommandManager.literal("trigger")
                        .then(CommandManager.argument("room", IntegerArgumentType.integer(0)).executes(DungeonCommand::trigger)))
                .then(CommandManager.literal("cutaway").executes(DungeonCommand::cutaway)));
    }

    private static DungeonState find(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return DungeonRegistry.find(source.getWorld(), BlockPos.ofFloored(source.getPosition())).orElseThrow(NONE::create);
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        DungeonState d = find(context);
        source.sendFeedback(Text.empty().append(d.name()).append(" - ").append(Text.translatable(d.theme().translationKey()))
                .append(", ").append(d.tierText()), false);
        BlockBox b = d.bounds();
        source.sendFeedback(Text.literal("Entrance " + d.entrance().toShortString() + ", floor y=" + d.floorY()
                + ", bounds " + b.getMinX() + " " + b.getMinY() + " " + b.getMinZ() + " to " + b.getMaxX() + " " + b.getMaxY() + " " + b.getMaxZ()), false);
        source.sendFeedback(Text.literal((d.isCleared() ? "Cleared" : "Not cleared") + ", cleared " + d.clears() + " time(s)"
                + (d.clearedAt() >= 0 ? ", last at tick " + d.clearedAt() : "")), false);
        long repopulates = DungeonRegistry.repopulatesIn(source.getWorld(), d);
        source.sendFeedback(Text.literal("Hoard Coffer: " + d.cofferClaims() + " player(s) have had a roll"
                + (repopulates >= 0 ? "; repopulates in " + repopulates + " ticks, once empty for "
                + DungeonRegistry.REPOPULATE_EMPTY_TICKS : "")), false);
        source.sendFeedback(Text.literal(d.rooms().size() + " rooms:"), false);
        ServerWorld world = source.getWorld();
        for (DungeonState.Room room : d.rooms()) {
            BlockPos c = room.box().getCenter();
            String fight = "";
            if (room.state() == mattonfire.dnd.dungeon.RoomState.ACTIVE) {
                DungeonWardBlockEntity ward = DungeonCombat.ward(world, d, room);
                fight = ward == null ? " (ward not loaded)" : " (" + ward.trackedMobs() + " mob(s) left, " + ward.getSeals().size() + " seal block(s))";
            }
            source.sendFeedback(Text.literal("  #" + room.id() + " " + room.role().label() + ": " + room.state() + fight
                    + " at " + c.getX() + " " + d.floorY() + " " + c.getZ()), false);
        }
        // Soundness check over the loaded rooms: no water or lava inside, and every room but the
        // entrance roofed over by the ground
        int fluids = 0;
        int exposed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (DungeonState.Room room : d.rooms()) {
            BlockBox r = room.box();
            for (int x = r.getMinX() + 2; x <= r.getMaxX() - 2; x++) {
                for (int z = r.getMinZ() + 2; z <= r.getMaxZ() - 2; z++) {
                    for (int y = d.floorY() + 1; y <= Math.min(r.getMaxY() - 2, d.floorY() + 12); y++) {
                        if (!world.getFluidState(pos.set(x, y, z)).isEmpty()) {
                            fluids++;
                        }
                    }
                }
            }
            if (room.role() != mattonfire.dnd.dungeon.RoomRole.ENTRANCE
                    && world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, r.getCenter().getX(), r.getCenter().getZ()) <= r.getMaxY() + 1) {
                exposed++;
            }
        }
        source.sendFeedback(Text.literal("Check: " + fluids + " water/lava blocks inside rooms, "
                + exposed + " rooms (besides the entrance) open to the surface"), false);
        return d.rooms().size();
    }

    private static int clear(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        DungeonState d = find(context);
        DungeonRegistry.clear(context.getSource().getWorld(), d);
        context.getSource().sendFeedback(Text.literal("Cleared ").append(d.name()), true);
        return 1;
    }

    private static int reset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        DungeonState d = find(context);
        DungeonRegistry.repopulate(context.getSource().getWorld(), d);
        context.getSource().sendFeedback(Text.literal("Reset ").append(d.name()), true);
        return 1;
    }

    private static int tier(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        DungeonState d = find(context);
        d.setTier(IntegerArgumentType.getInteger(context, "tier"));
        context.getSource().sendFeedback(Text.empty().append(d.name()).append(" is now ").append(d.tierText()), true);
        return d.tier();
    }

    private static int trigger(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        DungeonState d = find(context);
        int id = IntegerArgumentType.getInteger(context, "room");
        DungeonState.Room room = d.room(id);
        if (room == null) {
            throw new SimpleCommandExceptionType(Text.literal("No room #" + id)).create();
        }
        if (!room.role().combat()) {
            throw new SimpleCommandExceptionType(Text.literal("Room #" + id + " (" + room.role().label() + ") has no fight")).create();
        }
        DungeonWardBlockEntity ward = DungeonCombat.ward(source.getWorld(), d, room);
        if (ward == null) {
            throw new SimpleCommandExceptionType(Text.literal("Room #" + id + "'s ward isn't loaded: go closer")).create();
        }
        if (source.getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            throw new SimpleCommandExceptionType(Text.literal("Nothing spawns on Peaceful")).create();
        }
        int count = ward.start(source.getWorld(), d, room, source.getPlayerOrThrow());
        source.sendFeedback(Text.literal("Room #" + id + " " + room.role().label() + ": spawned " + count + " mob(s), seals "
                + ward.getSeals().size() + " block(s)"), true);
        return count;
    }

    private static int cutaway(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        DungeonState d = find(context);
        ServerWorld world = context.getSource().getWorld();
        BlockBox b = d.bounds();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int removed = 0;
        for (int x = b.getMinX(); x <= b.getMaxX(); x++) {
            for (int z = b.getMinZ(); z <= b.getMaxZ(); z++) {
                int top = Math.max(b.getMaxY(), world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z));
                for (int y = d.floorY() + 3; y <= top; y++) {
                    pos.set(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                        removed++;
                    }
                }
            }
        }
        int count = removed;
        context.getSource().sendFeedback(Text.literal("Cut away " + count + " blocks above " + (d.floorY() + 2)), true);
        return count;
    }
}
