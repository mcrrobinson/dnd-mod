package mattonfire.dnd.dm.encounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Places an {@link Encounter} in the world: each mob on safe ground (solid floor, two free blocks, no fluid) within
 * the encounter's {@code spread} of the centre, persistent, facing the nearest player, with the given tags. Callers
 * (the DM toolkit, dungeon rooms) add their own tags for tracking.
 */
public final class EncounterSpawner {
    /** Mobs from {@code "loot": false} encounters: no drops or XP, and no class XP or bounty credit. */
    public static final String NO_LOOT_TAG = "dndclasses.no_loot";

    private static final int ATTEMPTS = 16;
    private static final int VERTICAL = 4;

    private EncounterSpawner() {
    }

    /** What a spawn placed: the mobs (passengers included) and any chests. */
    public record Result(List<Entity> entities, List<BlockPos> chests) {
    }

    public static Result spawn(ServerWorld world, Encounter encounter, BlockPos center, Collection<String> tags) {
        Random random = world.getRandom();
        List<Entity> entities = new ArrayList<>();
        for (Encounter.Spawn spawn : encounter.spawns()) {
            int count = spawn.roll(random);
            for (int i = 0; i < count; i++) {
                Entity entity = spawnOne(world, encounter, spawn, center, random);
                if (entity == null) {
                    continue;
                }
                entity.streamSelfAndPassengers().forEach(e -> {
                    tags.forEach(e::addCommandTag);
                    spawn.tags().forEach(e::addCommandTag);
                    if (!encounter.loot()) {
                        e.addCommandTag(NO_LOOT_TAG);
                    }
                    entities.add(e);
                });
            }
        }
        List<BlockPos> chests = new ArrayList<>();
        for (Encounter.Chest chest : encounter.chests()) {
            for (int i = 0; i < chest.count(); i++) {
                BlockPos pos = findChestSpot(world, center, encounter.spread(), random);
                if (pos == null) {
                    continue;
                }
                world.setBlockState(pos, Blocks.CHEST.getDefaultState()
                        .with(net.minecraft.block.ChestBlock.FACING, Direction.Type.HORIZONTAL.random(random)));
                if (world.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
                    blockEntity.setLootTable(chest.lootTable(), random.nextLong());
                }
                mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Encounters] {}: chest at {} with loot table {}",
                        encounter.id(), pos.toShortString(), chest.lootTable());
                chests.add(pos);
            }
        }
        return new Result(entities, chests);
    }

    @Nullable
    private static Entity spawnOne(ServerWorld world, Encounter encounter, Encounter.Spawn spawn, BlockPos center,
            Random random) {
        NbtCompound nbt = spawn.nbt().copy();
        nbt.putString("id", EntityType.getId(spawn.entity()).toString());
        Entity entity = EntityType.loadEntityWithPassengers(nbt, world, e -> e);
        if (entity == null) {
            return null;
        }
        BlockPos pos = findSpot(world, entity, center, encounter.spread(), random);
        entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        faceNearestPlayer(world, entity);
        if (entity instanceof MobEntity mob) {
            if (spawn.initialize()) {
                mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.COMMAND, null, null);
            }
            mob.setPersistent();
        }
        if (!world.spawnNewEntityAndPassengers(entity)) {
            return null;
        }
        return entity;
    }

    /** A random spot within {@code spread} with a solid floor, room for the mob and no fluid; else the centre. */
    private static BlockPos findSpot(ServerWorld world, Entity entity, BlockPos center, int spread, Random random) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int dx = spread == 0 ? 0 : random.nextInt(spread * 2 + 1) - spread;
            int dz = spread == 0 ? 0 : random.nextInt(spread * 2 + 1) - spread;
            for (int dy = VERTICAL; dy >= -VERTICAL; dy--) {
                BlockPos pos = center.add(dx, dy, dz);
                if (isSafe(world, entity, pos)) {
                    return pos;
                }
            }
        }
        return center;
    }

    private static boolean isSafe(ServerWorld world, Entity entity, BlockPos pos) {
        BlockState floor = world.getBlockState(pos.down());
        if (!floor.isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
            return false;
        }
        double height = Math.max(2.0D, entity.getHeight());
        double halfWidth = entity.getWidth() / 2.0D;
        Box box = new Box(pos.getX() + 0.5D - halfWidth, pos.getY(), pos.getZ() + 0.5D - halfWidth,
                pos.getX() + 0.5D + halfWidth, pos.getY() + height, pos.getZ() + 0.5D + halfWidth);
        return world.isSpaceEmpty(box) && !world.containsFluid(box);
    }

    @Nullable
    private static BlockPos findChestSpot(ServerWorld world, BlockPos center, int spread, Random random) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int dx = spread == 0 ? 0 : random.nextInt(spread * 2 + 1) - spread;
            int dz = spread == 0 ? 0 : random.nextInt(spread * 2 + 1) - spread;
            for (int dy = VERTICAL; dy >= -VERTICAL; dy--) {
                BlockPos pos = center.add(dx, dy, dz);
                if (world.getBlockState(pos).isAir() && world.getFluidState(pos).isEmpty()
                        && world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static void faceNearestPlayer(ServerWorld world, Entity entity) {
        PlayerEntity player = world.getClosestPlayer(entity.getX(), entity.getY(), entity.getZ(), 64.0D,
                p -> !p.isSpectator() && !mattonfire.dnd.dm.DungeonMaster.isVeiled(p));
        if (player == null) {
            return;
        }
        double dx = player.getX() - entity.getX();
        double dz = player.getZ() - entity.getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setBodyYaw(yaw);
    }

    /** True for a mob from a {@code "loot": false} encounter. */
    public static boolean isNoLoot(Entity entity) {
        return entity.getCommandTags().contains(NO_LOOT_TAG);
    }
}
