package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.player.PlayerEntity;

import blue.endless.jankson.annotation.Nullable;

@Mixin(MobEntity.class)
public class HostileEntityMixin {
    private LivingEntity target;
    @Inject(
		at = @At("RETURN"), 
		method = "setTarget"
	)
    public void setTarget(@Nullable LivingEntity target, CallbackInfo info) {
        if(target != null && ((PlayerEntityExt)(PlayerEntity)target).dndClassExist()==2){
                this.target = null;
        }
     }
}

