package mattonfire.dnd.classes;

import net.minecraft.entity.LivingEntity;

public final class BloodhunterIdentityData {
    public final long expiryTick;
    public final LivingEntity entity;

    public BloodhunterIdentityData(long expiryTick, LivingEntity entity) {
        this.expiryTick = expiryTick;
        this.entity = entity;
    }
}
