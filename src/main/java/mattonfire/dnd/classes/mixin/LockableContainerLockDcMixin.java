package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.SkillChecks.LockDcHolder;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** Stores a container's lock DC override ({@link LockDcHolder}), set by {@code /dm loot plant ... dc <n>}. */
@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerLockDcMixin implements LockDcHolder {
    @Unique
    private int dndclasses$lockDc;

    @Override
    public int dndclasses$getLockDc() {
        return this.dndclasses$lockDc;
    }

    @Override
    public void dndclasses$setLockDc(int dc) {
        this.dndclasses$lockDc = Math.max(0, dc);
        ((LockableContainerBlockEntity) (Object) this).markDirty();
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void dndclasses$readLockDc(NbtCompound nbt, CallbackInfo ci) {
        this.dndclasses$lockDc = nbt.contains(NBT_KEY, NbtElement.INT_TYPE) ? Math.max(0, nbt.getInt(NBT_KEY)) : 0;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void dndclasses$writeLockDc(NbtCompound nbt, CallbackInfo ci) {
        if (this.dndclasses$lockDc > 0) {
            nbt.putInt(NBT_KEY, this.dndclasses$lockDc);
        }
    }
}
