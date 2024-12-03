package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    private boolean isFrozen = false;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tickFreeze(CallbackInfo ci) {
        if (isFrozen) {
            ci.cancel(); // Skip ticking if frozen
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void saveFreezeState(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("Frozen", isFrozen);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void loadFreezeState(NbtCompound nbt, CallbackInfo ci) {
        isFrozen = nbt.getBoolean("Frozen");
    }

    public void setFrozen(boolean frozen) {
        this.isFrozen = frozen;
    }

    public boolean isFrozen() {
        return isFrozen;
    }
}
