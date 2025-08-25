package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    // use a generic feature renderer signature to match the real method; return void (no value needed)
    @Invoker("addFeature")
    boolean invokeAddFeature(FeatureRenderer<?, ?> feature);
}