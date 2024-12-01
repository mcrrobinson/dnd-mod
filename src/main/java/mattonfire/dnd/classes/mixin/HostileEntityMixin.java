package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(MobEntity.class)
public class HostileEntityMixin {
    private LivingEntity target;

    @Inject(at = @At("RETURN"), method = "setTarget")
    public void setTarget(@Nullable LivingEntity target, CallbackInfo info) {
        if (target instanceof ServerPlayerEntity) {
            if (((PlayerEntityExt) (PlayerEntity) target).dndClassExist() == 2) {
                this.target = null;
            }
        }
    }
}
