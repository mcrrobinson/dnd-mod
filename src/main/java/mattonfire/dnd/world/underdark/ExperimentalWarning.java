package mattonfire.dnd.world.underdark;

import com.mojang.serialization.Lifecycle;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

/**
 * Whether a world is "experimental" only because of the mod's own data-pack worldgen (the Underdark's dimension,
 * biomes and features), so the client can skip vanilla's experimental-settings warning for it
 * ({@code IntegratedServerLoaderMixin}).
 */
public final class ExperimentalWarning {
    private ExperimentalWarning() {
    }

    /** True if every registry entry that isn't stable has a {@code dndclasses} id. Logs the first one that isn't. */
    public static boolean onlyOurs(DynamicRegistryManager registries) {
        return registries.streamAllRegistries().allMatch(entry -> onlyOurs(entry.value()));
    }

    private static <T> boolean onlyOurs(Registry<T> registry) {
        for (Map.Entry<RegistryKey<T>, T> entry : registry.getEntrySet()) {
            if (entry.getKey().getValue().getNamespace().equals(DnDClasses.MOD_ID)) {
                continue;
            }
            if (registry.getEntryLifecycle(entry.getValue()) != Lifecycle.stable()) {
                DnDClasses.LOGGER.info("[Underdark] world is experimental because of {} (not ours): keeping the warning",
                        entry.getKey().getValue());
                return false;
            }
        }
        return true;
    }
}
