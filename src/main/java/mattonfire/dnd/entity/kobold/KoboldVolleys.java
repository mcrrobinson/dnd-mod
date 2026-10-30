package mattonfire.dnd.entity.kobold;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;

/**
 * Sling volleys (design 10, 3.3). When a slinger is ready and sees its target it calls a volley: a sharp chirp, and
 * every other slinger within {@value #CALL_RANGE} blocks that sees the same target and is within
 * {@value #READY_WINDOW} ticks of being ready slings at it {@value #MAX_DELAY} ticks later at most. They all restart
 * their cooldown as they shoot. Three slingers do a 9-damage burst, 11.25 with Pack Tactics.
 */
public final class KoboldVolleys {
    public static final double CALL_RANGE = 12.0D;
    public static final int READY_WINDOW = 15;
    public static final int MAX_DELAY = 6;

    private KoboldVolleys() {
    }

    /** {@code caller} is ready and sees {@code target}: it shoots now and the others join in. */
    public static void call(KoboldSlingerEntity caller, LivingEntity target) {
        List<KoboldSlingerEntity> joining = caller.world.getEntitiesByClass(KoboldSlingerEntity.class,
                caller.getBoundingBox().expand(CALL_RANGE),
                other -> other != caller && other.isAlive() && other.squaredDistanceTo(caller) <= CALL_RANGE * CALL_RANGE
                        && other.cooldownLeft() <= READY_WINDOW && other.isReadyToJoin()
                        && other.getVisibilityCache().canSee(target)
                        && other.scaredOf() == null);
        if (!joining.isEmpty()) {
            caller.playSound(SoundEvents.ENTITY_FOX_SCREECH, 1.2F, 1.8F);
        }
        caller.sling(target);
        for (KoboldSlingerEntity other : joining) {
            other.scheduleShot(target, other.getRandom().nextInt(MAX_DELAY + 1));
        }
        if (!joining.isEmpty()) {
            MobActions.log(caller, "volley (" + (joining.size() + 1) + " slingers)", target);
            DnDClasses.LOGGER.info("[Kobold] {} calls a volley at {} ({} slingers, t={})", caller.getName().getString(),
                    target.getName().getString(), joining.size() + 1, caller.world.getTime());
        }
    }
}
