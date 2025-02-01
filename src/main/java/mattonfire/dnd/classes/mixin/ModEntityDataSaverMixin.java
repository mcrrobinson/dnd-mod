package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

@Mixin(Entity.class)
public abstract class ModEntityDataSaverMixin implements IEntityDataSaver {
    private NbtCompound persistentData = new NbtCompound();
    private final static String DATA_KEY = "mattonfire.dnd.classes";

    @Override
    public NbtCompound getPersistentData() {
        if (persistentData.isEmpty()) {
            this.persistentData = new NbtCompound();
        }
        return persistentData;
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    protected void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        nbt.put(DATA_KEY, this.persistentData);
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    protected void injectReadMethod(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(DATA_KEY)) {
            this.persistentData = nbt.getCompound(DATA_KEY);
        }
    }
}
