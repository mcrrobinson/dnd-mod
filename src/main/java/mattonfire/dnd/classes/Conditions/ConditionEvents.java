package mattonfire.dnd.classes.Conditions;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;

/** Server-side events for conditions landing and ending. */
public final class ConditionEvents {
    /** After an instance lands (fresh = it wasn't there before from this source/key). */
    public static final Event<Applied> APPLIED = EventFactory.createArrayBacked(Applied.class,
            listeners -> (target, inst, fresh) -> {
                for (Applied listener : listeners) {
                    listener.applied(target, inst, fresh);
                }
            });

    /** After an instance ends. */
    public static final Event<Ended> ENDED = EventFactory.createArrayBacked(Ended.class,
            listeners -> (target, inst, reason) -> {
                for (Ended listener : listeners) {
                    listener.ended(target, inst, reason);
                }
            });

    public enum EndReason {
        EXPIRED, SAVED, REMOVED, SOURCE_GONE, ESCALATED, DEATH, DOWNED
    }

    @FunctionalInterface
    public interface Applied {
        void applied(LivingEntity target, ConditionInstance inst, boolean fresh);
    }

    @FunctionalInterface
    public interface Ended {
        void ended(LivingEntity target, ConditionInstance inst, EndReason reason);
    }

    private ConditionEvents() {
    }
}
