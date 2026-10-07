package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Progression.Classes.ArtificerSkills;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

// Artificer Tinkerer: tools and weapons wear half as often.
@Mixin(ItemStack.class)
public class ArtificerItemStackMixin {
    @ModifyVariable(method = "damage(ILnet/minecraft/util/math/random/Random;Lnet/minecraft/server/network/ServerPlayerEntity;)Z", at = @At("HEAD"), argsOnly = true)
    private int dnd$tinkererWear(int amount, int originalAmount, Random random, ServerPlayerEntity player) {
        return ArtificerSkills.tinkererWear(player, (ItemStack) (Object) this, amount);
    }
}
