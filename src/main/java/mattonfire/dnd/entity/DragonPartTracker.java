package mattonfire.dnd.entity;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Implemented on World by WorldMixin: the multipart dragons currently loaded in that world. */
public interface DragonPartTracker {
    Set<MultipartDragon> dnd$getDragons();

    /** Hooked to the server/client entity load events. */
    static void onLoad(Entity entity, World world) {
        if (entity instanceof MultipartDragon dragon) {
            ((DragonPartTracker) world).dnd$getDragons().add(dragon);
        }
    }

    static void onUnload(Entity entity, World world) {
        if (entity instanceof MultipartDragon dragon) {
            ((DragonPartTracker) world).dnd$getDragons().remove(dragon);
        }
    }
}
