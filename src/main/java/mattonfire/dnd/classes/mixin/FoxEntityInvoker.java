package mattonfire.dnd.classes.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.passive.FoxEntity;

/** Lets a Bard's fox companion trust its Bard, so it doesn't run away from them. */
@Mixin(FoxEntity.class)
public interface FoxEntityInvoker {
    @Invoker("addTrustedUuid")
    void invokeAddTrustedUuid(UUID uuid);
}
