package mattonfire.dnd.classes.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Client.Render.GeoPlayerFeature;
import mattonfire.dnd.classes.Client.Render.RaceFeatures;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceSize;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCtor(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        PlayerEntityRenderer renderer = (PlayerEntityRenderer) (Object) this;
        ((LivingEntityRendererAccessor) (Object) renderer).invokeAddFeature(new GeoPlayerFeature(renderer, context));
        ((LivingEntityRendererAccessor) (Object) renderer).invokeAddFeature(new RaceFeatures.PlayerFeature(renderer));
    }

    /**
     * Racial body size ({@link RaceSize}): scales the whole third-person model, armour and features
     * included, about the feet. Every client draws every player at their size; the first-person hand
     * (renderArm) never comes through here.
     */
    @Inject(method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("TAIL"))
    private void dnd$raceScale(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta,
            CallbackInfo ci) {
        RaceInfo body = RaceSize.body(player);
        if (body != null) {
            float width = (float) body.modelWidth();
            matrices.scale(width, (float) body.scale(), width);
        }
    }
}
