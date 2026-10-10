package mattonfire.dnd.dungeon;

import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.mixin.LootableContainerBlockEntityAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Class-check gates at run time. The gate itself does its own work (an obstacle block, plain blocks, an
 * iron door); this only puts it back when the dungeon resets ({@link DungeonState#resets()} moves on),
 * from the ward's tick: obstacles reseal (and mined blocks come back), rubble refills, the iron door
 * shuts, levers and other power next to it go, and an emptied key chest is restocked.
 */
public final class DungeonGates {
    /** The lever "key" chest of a {@link GateKind#LOCKED_DOOR}: locked like any dungeon chest. */
    public static final String KEY_ROOM = "gate_key";

    private DungeonGates() {
    }

    /** The key chest's table, {@code dndclasses:chests/dungeon/gate_key_t<tier>} (locked at the tier's DC). */
    public static Identifier keyTable(int tier) {
        return new Identifier(DnDClasses.MOD_ID, DungeonLoot.CHEST_PREFIX + KEY_ROOM + "_t" + MathHelper.clamp(tier, 1, 4));
    }

    /** The obstacle tier for a dungeon's Challenge tier (one step harder for a side vault). */
    public static Tier obstacleTier(int tier, boolean sideVault) {
        Tier[] tiers = {Tier.MEDIUM, Tier.MEDIUM, Tier.HARD, Tier.VERY_HARD};
        int index = MathHelper.clamp(tier, 1, 4) - 1 + (sideVault ? 1 : 0);
        return tiers[Math.min(index, tiers.length - 1)];
    }

    /** Rubble: gravel along the bottom (so it doesn't fall), cobblestone above. */
    public static BlockState rubble(boolean bottom, int i) {
        if (bottom) {
            return Blocks.GRAVEL.getDefaultState();
        }
        return (i % 3 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState();
    }

    public static void tick(ServerWorld world, DungeonWardBlockEntity ward, GateState gate, DungeonState dungeon) {
        if (gate.resetsSeen() == dungeon.resets()) {
            return;
        }
        for (BlockPos pos : gate.blocks()) {
            if (!world.isChunkLoaded(pos)) {
                return;
            }
        }
        rebuild(world, gate, dungeon.tier());
        gate.setResetsSeen(dungeon.resets());
        ward.markDirty();
        DnDClasses.LOGGER.info("[Gate] {} gate of room #{} rebuilt with the dungeon (reset {})", gate.kind().id(),
                ward.getRoomId(), dungeon.resets());
    }

    private static boolean occupied(ServerWorld world, BlockPos pos) {
        return !world.getEntitiesByClass(LivingEntity.class, new Box(pos), e -> true).isEmpty();
    }

    /** Puts the gate back as generated. */
    public static void rebuild(ServerWorld world, GateState gate, int tier) {
        ObstacleType type = gate.kind().obstacle();
        if (type != null) {
            Tier obstacleTier = Tier.byName(gate.tier(), Tier.MEDIUM);
            BlockPos origin = null;
            for (BlockPos pos : gate.blocks()) {
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof ObstacleBlock) {
                    origin = origin == null ? pos : origin;
                } else if (state.isAir() && !occupied(world, pos)) {
                    ObstaclePlacer.place(world, null, pos, type, obstacleTier, gate.kind().mainPathSafe(), gate.groupSeed());
                    origin = origin == null ? pos : origin;
                }
            }
            if (origin != null) {
                ObstacleGroups.seal(world, origin);
            }
            return;
        }
        if (gate.kind() == GateKind.RUBBLE) {
            int bottom = gate.blocks().stream().mapToInt(BlockPos::getY).min().orElse(0);
            int i = 0;
            for (BlockPos pos : gate.blocks()) {
                if (world.getBlockState(pos).isAir() && !occupied(world, pos)) {
                    world.setBlockState(pos, rubble(pos.getY() == bottom, i), Block.NOTIFY_ALL);
                }
                i++;
            }
            return;
        }
        // LOCKED_DOOR: take away whatever powers the door, shut it, restock the key chest.
        for (BlockPos pos : gate.blocks()) {
            unpower(world, pos);
        }
        BlockPos door = gate.door();
        if (door != null) {
            unpower(world, door);
            unpower(world, door.up());
            BlockState state = world.getBlockState(door);
            if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.OPEN)) {
                ((DoorBlock) state.getBlock()).setOpen(null, world, state, door, false);
            }
        }
        BlockPos chest = gate.keyChest();
        if (chest != null && world.isChunkLoaded(chest)) {
            if (!(world.getBlockState(chest).getBlock() instanceof ChestBlock)) {
                world.setBlockState(chest, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
            }
            // isEmpty() would roll a table still waiting, so only look inside chests already opened.
            if (world.getBlockEntity(chest) instanceof ChestBlockEntity be
                    && ((LootableContainerBlockEntityAccessor) be).dndclasses$getLootTableId() == null && be.isEmpty()) {
                be.setLootTable(keyTable(tier), world.getRandom().nextLong());
                be.markDirty();
            }
        }
    }

    /** Removes levers, buttons and other redstone sources next to {@code pos}. */
    private static void unpower(World world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            BlockState state = world.getBlockState(p);
            if (state.isOf(Blocks.LEVER) || state.isOf(Blocks.REDSTONE_TORCH) || state.isOf(Blocks.REDSTONE_WALL_TORCH)
                    || state.isOf(Blocks.REDSTONE_BLOCK) || state.isOf(Blocks.STONE_BUTTON)
                    || state.isOf(Blocks.STONE_PRESSURE_PLATE) || state.isOf(Blocks.REDSTONE_WIRE)) {
                world.breakBlock(p, true);
            }
        }
    }

    /** "arcane_seal (sealed)" style summary for /dungeon info. */
    public static String describe(World world, GateState gate) {
        String status;
        BlockPos first = gate.blocks().isEmpty() ? null : gate.blocks().get(0);
        if (first == null || !world.isChunkLoaded(first)) {
            status = "not loaded";
        } else if (gate.kind().obstacle() != null) {
            long sealed = gate.blocks().stream().filter(p -> ObstacleBlock.isSealed(world.getBlockState(p))).count();
            status = sealed > 0 ? "sealed (" + sealed + "/" + gate.blocks().size() + ")" : "open";
        } else if (gate.kind() == GateKind.RUBBLE) {
            long left = gate.blocks().stream().filter(p -> !world.getBlockState(p).isAir()).count();
            status = left > 0 ? left + "/" + gate.blocks().size() + " rubble left" : "dug through";
        } else {
            BlockState door = gate.door() == null ? Blocks.AIR.getDefaultState() : world.getBlockState(gate.door());
            status = door.getBlock() instanceof DoorBlock ? (door.get(DoorBlock.OPEN) ? "door open" : "door shut") : "door gone";
        }
        return gate.kind().id() + " (" + status + "; " + gate.kind().classes().stream().map(Enum::name).toList()
                + (gate.kind().bypass() ? " or anyone the hard way" : " only") + ")";
    }
}
