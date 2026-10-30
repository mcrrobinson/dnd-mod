package mattonfire.dnd.entity.dragon;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Hooks for the Ancient dragons' lairs (design 10, section 6), so the lair ticket can react without this one
 * knowing its structures:
 * <ul>
 * <li>{@link #SLAIN}: an Ancient died; mark its lair slain (it never respawns) and drop its markers.</li>
 * <li>{@link #TREMOR}: its Tremor lair action shook the lair; drop dust from the hoard hall's ceiling.</li>
 * </ul>
 */
public final class AncientDragonEvents {
    @FunctionalInterface
    public interface Slain {
        /** {@code lair} is the dragon's lair centre, or null for one summoned without a lair. */
        void onSlain(AncientDragonEntity dragon, ServerWorld world, @Nullable BlockPos lair);
    }

    @FunctionalInterface
    public interface Tremor {
        void onTremor(AncientDragonEntity dragon, ServerWorld world);
    }

    public static final Event<Slain> SLAIN = EventFactory.createArrayBacked(Slain.class,
            listeners -> (dragon, world, lair) -> {
                for (Slain listener : listeners) {
                    listener.onSlain(dragon, world, lair);
                }
            });

    public static final Event<Tremor> TREMOR = EventFactory.createArrayBacked(Tremor.class,
            listeners -> (dragon, world) -> {
                for (Tremor listener : listeners) {
                    listener.onTremor(dragon, world);
                }
            });

    private AncientDragonEvents() {
    }
}
