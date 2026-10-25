package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Client.ConditionClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;

/**
 * Frightened: the movement input loses any part that points toward what frightens you while it's within 32 blocks
 * and in sight ({@link ConditionClient#clampFrightened}).
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void dnd$frightenedClamp(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        ConditionClient.clampFrightened((Input) (Object) this);
    }
}
