package mattonfire.dnd.dm;

import java.util.List;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Duck interface on {@code ThreadedAnvilChunkStorage.EntityTracker} (see {@code DmEntityTrackerMixin}), so the
 * veil can re-check who sees a player right away instead of waiting for them to move.
 */
public interface DmTracked {
    void dnd$updateTrackedStatus(List<ServerPlayerEntity> players);
}
