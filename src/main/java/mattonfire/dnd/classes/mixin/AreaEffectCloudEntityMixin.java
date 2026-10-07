package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;

// Lingering potion clouds apply potion-sourced effects. Clouds without a potion
// (e.g. dragon's breath) are not treated as potions.
@Mixin(AreaEffectCloudEntity.class)
public class AreaEffectCloudEntityMixin {
    @Shadow
    private Potion potion;

    @WrapMethod(method = "tick")
    private void dnd$markPotionSource(Operation<Void> original) {
        if (this.potion == Potions.EMPTY) {
            original.call();
            return;
        }
        PotionImmunity.begin();
        try {
            original.call();
        } finally {
            PotionImmunity.end();
        }
    }
}
