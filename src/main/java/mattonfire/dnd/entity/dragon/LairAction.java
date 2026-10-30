package mattonfire.dnd.entity.dragon;

import java.util.List;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Something an Ancient dragon's lair does on its turn (design 10, 5.8): magma vents, a tremor, volcanic gas. The
 * dragon picks one every few seconds while a fight is on near its lair; each colour has its own set
 * ({@link ChromaticColor#lairActions()}). Effects that play out over time are scheduled on the dragon
 * ({@link AncientDragonEntity#schedule}).
 */
public interface LairAction {
    /** Short id, logged as {@code [MobAction] <name> lair_<id>}. */
    String id();

    /** Runs the action against the players in the lair (within {@link AncientDragonEntity#LAIR_RADIUS}). */
    void run(AncientDragonEntity dragon, ServerWorld world, List<ServerPlayerEntity> inLair);

    /** A scheduled part of a lair action, ticked by the dragon until it says it's done. */
    @FunctionalInterface
    interface Effect {
        /** One tick; true when finished. */
        boolean tick(ServerWorld world);
    }
}
