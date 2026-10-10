package mattonfire.dnd.classes.Rest;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Hooks for other systems (items that recharge on a rest, quests, rest-forbidden zones, the downed state). */
public final class RestEvents {
    private RestEvents() {
    }

    /** After a rest's benefits are applied. */
    public static final Event<AfterRest> AFTER_REST = EventFactory.createArrayBacked(AfterRest.class,
            listeners -> (player, kind, source) -> {
                for (AfterRest listener : listeners) {
                    listener.afterRest(player, kind, source);
                }
            });

    /**
     * Asked by {@link Rests#canShortRest} and {@link Rests#canLongRest} before a
     * rest starts. The first listener to return a reason refuses it.
     */
    public static final Event<AllowRest> ALLOW_REST = EventFactory.createArrayBacked(AllowRest.class,
            listeners -> (player, kind) -> {
                for (AllowRest listener : listeners) {
                    Text reason = listener.refuse(player, kind);
                    if (reason != null) {
                        return reason;
                    }
                }
                return null;
            });

    @FunctionalInterface
    public interface AfterRest {
        void afterRest(ServerPlayerEntity player, RestKind kind, RestSource source);
    }

    @FunctionalInterface
    public interface AllowRest {
        /** @return why the player can't rest, or null to allow it */
        Text refuse(ServerPlayerEntity player, RestKind kind);
    }
}
