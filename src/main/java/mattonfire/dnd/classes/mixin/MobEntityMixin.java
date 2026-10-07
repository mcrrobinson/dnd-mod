package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Misc.ClericHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

@Mixin(MobEntity.class)
public class MobEntityMixin {

    // Circle of ignoring mobs: a player with MOB_REPEL (Cleric or party member) can't be targeted, so any
    // attempt to target them clears the mob's target instead.
    @ModifyVariable(at = @At("HEAD"), method = "setTarget", argsOnly = true)
    private @Nullable LivingEntity ignoreRepellingCleric(@Nullable LivingEntity target) {
        if (ClericHandler.isRepellingMobs(target)) {
            return null;
        }
        return target;
    }

}
