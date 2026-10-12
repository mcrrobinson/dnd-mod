package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Race.RacePassives;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Stonecunning's break speed and Second Breakfast ({@link RacePassives}). */
@Mixin(PlayerEntity.class)
public abstract class RacePassivesPlayerMixin {
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void dnd$stonecunning(BlockState state, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(RacePassives.breakSpeed((PlayerEntity) (Object) this, state, cir.getReturnValueF()));
    }

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void dnd$secondBreakfast(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        RacePassives.secondBreakfast((PlayerEntity) (Object) this, stack);
    }
}
