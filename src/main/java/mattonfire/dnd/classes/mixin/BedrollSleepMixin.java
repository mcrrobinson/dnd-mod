package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Blocks.adventuring.BedrollBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

/** A sleeper lies on a bedroll, which is much lower than a bed ({@link BedrollBlock#SLEEP_DROP}). */
@Mixin(LivingEntity.class)
public abstract class BedrollSleepMixin {
    @Inject(method = "setPositionInBed", at = @At("TAIL"))
    private void dndclasses$lieOnBedroll(BlockPos pos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().getBlockState(pos).getBlock() instanceof BedrollBlock) {
            self.setPosition(self.getX(), self.getY() - BedrollBlock.SLEEP_DROP, self.getZ());
        }
    }
}
