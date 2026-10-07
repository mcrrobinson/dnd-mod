package mattonfire.dnd.entity;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;

/** A dragon whose model is bigger than its hitbox, so it exposes extra hittable DragonParts. */
public interface MultipartDragon {
    DragonPart[] getParts();

    DragonPartLayout getPartLayout();

    /** A hit on one of its parts; by default the dragon just takes the damage. Override to react to which part was hit. */
    default boolean damagePart(DragonPart part, DamageSource source, float amount) {
        return ((MobEntity) this).damage(source, amount);
    }
}
