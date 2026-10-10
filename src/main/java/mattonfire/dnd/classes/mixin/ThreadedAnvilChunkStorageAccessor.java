package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
    /** Entity id to its (package-private) EntityTracker, which implements {@code DmTracked}. */
    @Accessor("entityTrackers")
    Int2ObjectMap<?> dnd$getEntityTrackers();
}
