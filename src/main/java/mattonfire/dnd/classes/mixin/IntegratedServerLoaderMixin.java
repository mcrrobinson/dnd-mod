package mattonfire.dnd.classes.mixin;

import com.mojang.serialization.Lifecycle;
import mattonfire.dnd.world.underdark.ExperimentalWarning;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.SaveProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Opening a world whose dimensions include the mod's own datapack dimension (the Underdark) would show vanilla's
 * "Worlds using Experimental Settings are not supported" warning every time, and stall the DevScript auto-join. When
 * the only non-stable registry entries are the mod's own ({@link ExperimentalWarning#onlyOurs}), the world counts as
 * stable; anything else experimental (another data pack's dimension, a flat or debug world) still warns.
 */
@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {
    @Unique
    private SaveLoader dnd$saveLoader;

    @Redirect(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;ZZ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/SaveLoader;saveProperties()Lnet/minecraft/world/SaveProperties;"))
    private SaveProperties dnd$rememberSaveLoader(SaveLoader loader) {
        this.dnd$saveLoader = loader;
        return loader.saveProperties();
    }

    @Redirect(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;ZZ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/SaveProperties;getLifecycle()Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle dnd$ignoreOwnDimension(SaveProperties properties) {
        Lifecycle lifecycle = properties.getLifecycle();
        if (lifecycle != Lifecycle.stable() && this.dnd$saveLoader != null && ExperimentalWarning
                .onlyOurs(this.dnd$saveLoader.combinedDynamicRegistries().getCombinedRegistryManager())) {
            return Lifecycle.stable();
        }
        return lifecycle;
    }
}
