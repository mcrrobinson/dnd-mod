package mattonfire.dnd.entity.hireling;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.InnkeeperEntity;
import mattonfire.dnd.world.GameTime;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The adventurers looking for work at an inn: {@value #CANDIDATES} a day, each a different role, picked from a
 * seed of the inn's position and the day. The innkeeper checks every {@value #CHECK_TICKS} ticks: on a new day
 * yesterday's unhired candidates leave and today's arrive inside its {@link InnkeeperEntity#innBox()}; candidates
 * that wandered out are put back.
 */
public final class HirelingRoster {
    public static final int CANDIDATES = 3;
    public static final int CHECK_TICKS = 100;
    private static final String DAY_KEY = "dndHirelingRosterDay";

    private HirelingRoster() {
    }

    /** Called from the innkeeper's tick. */
    public static void tick(InnkeeperEntity innkeeper) {
        if (innkeeper.age % CHECK_TICKS != 0 || !(innkeeper.world instanceof ServerWorld world) || !innkeeper.isAlive()) {
            return;
        }
        long day = GameTime.day(world);
        NbtCompound data = ((IEntityDataSaver) innkeeper).getPersistentData();
        if (!data.contains(DAY_KEY) || data.getLong(DAY_KEY) != day) {
            reroll(innkeeper, day);
            return;
        }
        BlockBox box = innkeeper.innBox();
        for (HirelingEntity candidate : candidates(innkeeper)) {
            if (!box.contains(candidate.getBlockPos())) {
                BlockPos spot = spot(innkeeper, world.getRandom());
                candidate.refreshPositionAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, candidate.getYaw(), 0.0F);
                candidate.getNavigation().stop();
            }
        }
    }

    /** This innkeeper's unhired candidates that are loaded. */
    public static List<HirelingEntity> candidates(InnkeeperEntity innkeeper) {
        Box area = Box.from(innkeeper.innBox()).expand(32.0D);
        return innkeeper.world.getEntitiesByClass(HirelingEntity.class, area,
                hireling -> hireling.isForHire() && innkeeper.getUuid().equals(hireling.rosterInn()));
    }

    /** Sends the current candidates away and calls today's. */
    public static List<HirelingEntity> reroll(InnkeeperEntity innkeeper, long day) {
        ServerWorld world = (ServerWorld) innkeeper.world;
        for (HirelingEntity old : candidates(innkeeper)) {
            old.discard();
        }
        BlockBox box = innkeeper.innBox();
        Random random = Random.create(box.getCenter().asLong() * 31L + day * 1_000_003L);
        List<HirelingRole> roles = new ArrayList<>(List.of(HirelingRole.values()));
        List<HirelingEntity> spawned = new ArrayList<>();
        for (int i = 0; i < CANDIDATES && !roles.isEmpty(); i++) {
            HirelingRole role = roles.remove(random.nextInt(roles.size()));
            HirelingRace race = HirelingRace.pick(random);
            HirelingEntity candidate = Hirelings.HIRELING.create(world);
            if (candidate == null) {
                continue;
            }
            candidate.setup(role, race, random.nextInt(race.skins), HirelingNames.pick(race, random));
            BlockPos spot = spot(innkeeper, random);
            candidate.refreshPositionAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            candidate.makeCandidate(innkeeper.getUuid(), day, spot);
            world.spawnEntity(candidate);
            spawned.add(candidate);
        }
        ((IEntityDataSaver) innkeeper).getPersistentData().putLong(DAY_KEY, day);
        DnDClasses.LOGGER.info("[Hireling] {} roster for day {}: {}", innkeeper.innName(), day,
                spawned.stream().map(h -> h.hirelingName() + " (" + h.role().id() + ")").toList());
        return spawned;
    }

    /**
     * A free spot to stand inside the inn, near the innkeeper's floor level and not on top of it; the innkeeper's
     * own position if none is found.
     */
    static BlockPos spot(InnkeeperEntity innkeeper, Random random) {
        BlockBox box = innkeeper.innBox();
        BlockPos base = innkeeper.getBlockPos();
        for (int attempt = 0; attempt < 120; attempt++) {
            int x = box.getMinX() + random.nextInt(box.getBlockCountX());
            int z = box.getMinZ() + random.nextInt(box.getBlockCountZ());
            for (int dy = 0; dy <= 4; dy++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    BlockPos pos = new BlockPos(x, base.getY() + dy * sign, z);
                    if (box.contains(pos) && standable(innkeeper, pos) && !pos.isWithinDistance(base, 2.0D)) {
                        return pos;
                    }
                }
            }
        }
        return base;
    }

    private static boolean standable(InnkeeperEntity innkeeper, BlockPos pos) {
        var world = innkeeper.world;
        BlockState below = world.getBlockState(pos.down());
        return below.isSideSolidFullSquare(world, pos.down(), Direction.UP)
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }
}
