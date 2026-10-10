package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.magic.Curse;
import net.minecraft.entity.player.PlayerEntity;

/** The Gluttony curse: hunger drains 50% faster. */
@Mixin(PlayerEntity.class)
public abstract class CurseExhaustionMixin {
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float dndclasses$gluttony(float exhaustion) {
        return Curse.modifyExhaustion((PlayerEntity) (Object) this, exhaustion);
    }
}
