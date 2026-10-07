package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.Progression.Classes.AlchemistSkills;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

// Alchemist: XP for drinking potions, and Potent Brews.
@Mixin(PotionItem.class)
public class AlchemistPotionMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void dnd$alchemistDrinkXp(ItemStack stack, World world, LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            AlchemistSkills.onPotionUsed(player, stack);
        }
    }

    @WrapOperation(method = "finishUsing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z"))
    private boolean dnd$potentBrews(LivingEntity user, StatusEffectInstance effect, Operation<Boolean> original) {
        return original.call(user, AlchemistSkills.potentBrew(user, effect));
    }
}
