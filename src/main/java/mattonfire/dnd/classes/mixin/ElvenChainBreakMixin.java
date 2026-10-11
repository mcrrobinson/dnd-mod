package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Items.racial.RacialGear;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;

/** Elven Chain: marks a sneaking Elf's block break so the drops it spawns stay silent ({@link RacialGear#blockBreakStarted}). */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ElvenChainBreakMixin {
    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void dnd$elvenChainBreakStart(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        RacialGear.blockBreakStarted(this.player);
    }

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void dnd$elvenChainBreakEnd(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        RacialGear.blockBreakEnded(this.player);
    }
}
