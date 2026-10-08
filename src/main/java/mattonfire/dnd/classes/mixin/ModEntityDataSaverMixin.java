package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

@Mixin(Entity.class)
public abstract class ModEntityDataSaverMixin implements IEntityDataSaver {
    @Unique
    private NbtCompound dnd$persistentData = new NbtCompound();
    @Unique
    private static final String DND$DATA_KEY = "mattonfire.dnd.classes";

    @Override
    public NbtCompound getPersistentData() {
        return dnd$persistentData;
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void dnd$writePersistentData(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        // Only players use it; don't write an empty tag into every entity in the world.
        if (!this.dnd$persistentData.isEmpty()) {
            nbt.put(DND$DATA_KEY, this.dnd$persistentData.copy());
        }
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void dnd$readPersistentData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(DND$DATA_KEY)) {
            this.dnd$persistentData = nbt.getCompound(DND$DATA_KEY);
        }
    }
}
