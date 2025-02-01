package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements PlayerEntityExt {
    boolean dropEntireStack;
    private DndCharacter dndClass;

    public PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
        // TODO Auto-generated constructor stub
    }

    public void setDndClass(DndCharacter dndClass) {
        this.dndClass = dndClass;
        System.out.println(this.dndClass);
    }

    public DndCharacter getDndClass() {
        return this.dndClass;
    }

    public int addProgress(DndCharacter character, int amount) {
        IEntityDataSaver player = (IEntityDataSaver) (Object) this;
        NbtCompound nbt = player.getPersistentData();
        int progress = nbt.getInt(character.toString());
        progress += amount;
        nbt.putInt(character.toString(), progress);
        return progress;
    }

    public int getProgress(DndCharacter character) {
        IEntityDataSaver player = (IEntityDataSaver) (Object) this;
        NbtCompound nbt = player.getPersistentData();
        return nbt.getInt(character.toString());
    }

    // Some creatures can't swim... here is that. Probably a better way of doing
    // it.
    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (((PlayerEntityExt) player).getDndClass() == DndCharacter.DRUID) {
            if (isSubmergedIn(FluidTags.WATER) && !player.isCreative() &&
                    !player.getAbilities().flying) {
                this.setVelocity(0.0D, -0.5, 0.0D);
            }
        }
    }

}