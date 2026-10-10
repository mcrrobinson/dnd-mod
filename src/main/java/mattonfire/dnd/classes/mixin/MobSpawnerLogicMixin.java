package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerLogic;

// A Cleric's consecrated altar keeps spawners near it quiet (DesecratedAltarType).
@Mixin(MobSpawnerLogic.class)
public class MobSpawnerLogicMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void dnd$consecratedQuiet(ServerWorld world, BlockPos pos, CallbackInfo ci) {
        if (ObstacleTypes.DESECRATED_ALTAR.suppresses(world, pos)) {
            ci.cancel();
        }
    }
}
