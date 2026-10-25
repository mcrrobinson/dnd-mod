package mattonfire.dnd.world.underdark;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.structure.Structure;

/**
 * Spawn rules the Underdark settlements share (design 9, 8): their folk are placed by the pieces (persistent), and
 * the structure's {@code spawn_overrides} top them up over time, only inside the settlement and only up to a cap of
 * their kind nearby. Used from {@code ModSpawns} spawn restrictions ({@code canDrowSpawn}; the duergar's next).
 */
public final class UnderdarkGarrisons {
    /** How far the cap counts. */
    public static final double CAP_RADIUS = 48.0D;

    private UnderdarkGarrisons() {
    }

    /** Whether {@code pos} lies inside one of the pieces of a {@code structure} (none outside the Underdark). */
    public static boolean inside(ServerWorldAccess world, BlockPos pos, RegistryKey<Structure> structure) {
        if (!Underdark.isUnderdark(world.toServerWorld())) {
            return false;
        }
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos, structure);
        return start != null && start.hasChildren();
    }

    /** Whether fewer than {@code cap} entities of {@code kin} are within {@value #CAP_RADIUS} blocks of {@code pos}. */
    public static boolean underCap(ServerWorldAccess world, BlockPos pos, Class<? extends Entity> kin, int cap) {
        return world.getEntitiesByClass(kin, new Box(pos).expand(CAP_RADIUS), Entity::isAlive).size() < cap;
    }
}
