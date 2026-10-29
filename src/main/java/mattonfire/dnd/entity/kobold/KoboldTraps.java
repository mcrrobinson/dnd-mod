package mattonfire.dnd.entity.kobold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * The kobold traps that need fixing (design 10, 4.2): every spent or disarmed trap with {@code Faction:
 * dndclasses:kobolds} outside a dungeon marks itself here once a second while its chunk is loaded, so a Trapmaker's
 * {@code KoboldRearmGoal} finds the nearest one without scanning blocks. Kept in memory only: a trap re-marks itself
 * when its chunk loads again.
 */
public final class KoboldTraps {
    private static final Map<RegistryKey<World>, Set<BlockPos>> SPENT = new HashMap<>();

    private KoboldTraps() {
    }

    public static synchronized void markSpent(ServerWorld world, BlockPos pos) {
        SPENT.computeIfAbsent(world.getRegistryKey(), k -> new HashSet<>()).add(pos.toImmutable());
    }

    public static synchronized void clear(ServerWorld world, BlockPos pos) {
        Set<BlockPos> set = SPENT.get(world.getRegistryKey());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * Spent or disarmed kobold traps within {@code range} blocks of {@code from} (horizontally; 8 up or down), nearest
     * first. Positions whose trap is gone or armed again are dropped.
     */
    public static synchronized List<TrapTriggerBlockEntity> needingRepair(ServerWorld world, BlockPos from, double range) {
        Set<BlockPos> set = SPENT.get(world.getRegistryKey());
        List<TrapTriggerBlockEntity> found = new ArrayList<>();
        if (set == null) {
            return found;
        }
        set.removeIf(pos -> {
            if (!world.isChunkLoaded(pos)) {
                return false;
            }
            return !(world.getBlockEntity(pos) instanceof TrapTriggerBlockEntity trap) || trap.isArmed() || !trap.isKoboldTrap();
        });
        for (BlockPos pos : set) {
            double dx = pos.getX() - from.getX();
            double dz = pos.getZ() - from.getZ();
            if (dx * dx + dz * dz <= range * range && Math.abs(pos.getY() - from.getY()) <= 8 && world.isChunkLoaded(pos)
                    && world.getBlockEntity(pos) instanceof TrapTriggerBlockEntity trap) {
                found.add(trap);
            }
        }
        found.sort((a, b) -> Double.compare(a.getPos().getSquaredDistance(from), b.getPos().getSquaredDistance(from)));
        return found;
    }

    /** A living kobold within {@code range} blocks of {@code pos}. */
    public static boolean koboldNear(ServerWorld world, BlockPos pos, double range) {
        return !world.getEntitiesByClass(LivingEntity.class, new Box(pos).expand(range),
                e -> e.isAlive() && Kobolds.isKobold(e)).isEmpty();
    }
}
