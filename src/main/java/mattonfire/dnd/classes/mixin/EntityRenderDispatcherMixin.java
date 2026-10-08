package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.client.renderer.ServerPartDebug;
import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.MultipartDragon;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/** Draws dragon part hitboxes with F3+B, like vanilla does for ender dragon parts. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "renderHitbox", at = @At("TAIL"))
    private static void dnd$renderDragonPartHitboxes(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof MultipartDragon dragon)) {
            return;
        }
        // The matrix is at the entity's interpolated position, which rendered parts are anchored to too
        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
        for (DragonPart part : dragon.getParts()) {
            for (Box shape : part.getShapes()) {
                WorldRenderer.drawBox(matrices, vertices, shape.offset(-x, -y, -z), 0.25F, 1.0F, 0.0F, 1.0F);
            }
        }
        // Dev check: the integrated server's shapes for the same dragon, in red
        if (ServerPartDebug.enabled && ServerPartDebug.serverCopy(entity) instanceof MultipartDragon serverDragon) {
            for (DragonPart part : serverDragon.getParts()) {
                for (Box shape : part.getShapes()) {
                    WorldRenderer.drawBox(matrices, vertices, shape.offset(-x, -y, -z), 1.0F, 0.15F, 0.15F, 1.0F);
                }
            }
        }
    }
}
