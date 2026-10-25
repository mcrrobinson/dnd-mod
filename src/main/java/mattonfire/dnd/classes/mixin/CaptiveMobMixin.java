package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.world.underdark.Captives;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets any mob be a captive ({@link Captives}): the {@code Captive:1b} flag and, once freed, {@code CaptiveFreedAt},
 * saved with the mob, and the captive's tick (kneeling, or walking off).
 */
@Mixin(MobEntity.class)
public abstract class CaptiveMobMixin implements Captives.Holder {
    @Unique
    private boolean dndclasses$captive;
    @Unique
    private long dndclasses$freedAt;

    @Override
    public boolean dndclasses$isCaptive() {
        return this.dndclasses$captive;
    }

    @Override
    public void dndclasses$setCaptive(boolean captive) {
        this.dndclasses$captive = captive;
    }

    @Override
    public long dndclasses$freedAt() {
        return this.dndclasses$freedAt;
    }

    @Override
    public void dndclasses$setFreedAt(long time) {
        this.dndclasses$freedAt = time;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void dndclasses$writeCaptive(NbtCompound nbt, CallbackInfo ci) {
        if (this.dndclasses$captive) {
            nbt.putBoolean(Captives.NBT, true);
        }
        if (this.dndclasses$freedAt > 0L) {
            nbt.putLong(Captives.FREED_NBT, this.dndclasses$freedAt);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void dndclasses$readCaptive(NbtCompound nbt, CallbackInfo ci) {
        this.dndclasses$captive = nbt.getBoolean(Captives.NBT);
        this.dndclasses$freedAt = nbt.getLong(Captives.FREED_NBT);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dndclasses$tickCaptive(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!self.getWorld().isClient && (this.dndclasses$captive || this.dndclasses$freedAt > 0L) && self.isAlive()) {
            Captives.tick(self);
        }
    }
}
