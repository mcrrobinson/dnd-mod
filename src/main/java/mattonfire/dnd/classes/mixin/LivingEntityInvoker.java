package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/** Lets a Downed player bleeding out still be saved by a held Totem of Undying. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("tryUseTotem")
    boolean dnd$tryUseTotem(DamageSource source);
}
