package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.Feats.FeatArrow;
import mattonfire.dnd.classes.Feats.FeatArrows;
import mattonfire.dnd.classes.Feats.Feats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

/** Sharpshooter's arrow marks ({@link FeatArrow}), saved with the arrow, and Long Shot's damage on a hit. */
@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileHitMixin implements FeatArrow {
    @Unique
    private int dnd$featFlags;
    @Unique
    @Nullable
    private Vec3d dnd$origin;

    @Override
    public int dnd$featFlags() {
        return dnd$featFlags;
    }

    @Override
    public void dnd$setFeatFlags(int flags) {
        dnd$featFlags = flags;
    }

    @Override
    @Nullable
    public Vec3d dnd$origin() {
        return dnd$origin;
    }

    @Override
    public void dnd$setOrigin(@Nullable Vec3d origin) {
        dnd$origin = origin;
    }

    @WrapOperation(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean dnd$longShot(Entity hit, DamageSource source, float amount, Operation<Boolean> original) {
        return original.call(hit, source, FeatArrows.longShot((PersistentProjectileEntity) (Object) this, hit, amount));
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void dnd$writeFeat(NbtCompound nbt, CallbackInfo ci) {
        if (dnd$featFlags == 0)
            return;
        NbtCompound feat = new NbtCompound();
        feat.putInt("flags", dnd$featFlags);
        if (dnd$origin != null) {
            feat.putDouble("x", dnd$origin.x);
            feat.putDouble("y", dnd$origin.y);
            feat.putDouble("z", dnd$origin.z);
        }
        nbt.put(Feats.DATA_KEY, feat);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void dnd$readFeat(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains(Feats.DATA_KEY))
            return;
        NbtCompound feat = nbt.getCompound(Feats.DATA_KEY);
        dnd$featFlags = feat.getInt("flags");
        dnd$origin = feat.contains("x") ? new Vec3d(feat.getDouble("x"), feat.getDouble("y"), feat.getDouble("z"))
                : null;
    }
}
