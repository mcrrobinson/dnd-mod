package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.MinecraftClient;

/** Lets DevScript press the use and attack buttons without real mouse input. */
@Mixin(MinecraftClient.class)
public interface MinecraftClientInvoker {
    @Invoker("doItemUse")
    void invokeDoItemUse();

    @Invoker("doAttack")
    boolean invokeDoAttack();
}
