package mattonfire.dnd.classes.Items.lib;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Utility class for handling cape/braid rotations, arm visibility, etc.
 */
public final class FARenderUtils {

    /**
     * Applies a "cape-like" rotation to the provided {@code bone} by replicating
     * the logic from the
     * vanilla CapeFeatureRenderer in 1.19.4.
     */
    public static void applyCapeRotation(AbstractClientPlayerEntity player, GeoBone bone, float tickDelta) {
        // These fields are the 1.19.4 Yarn names for the player's cape position.
        double d0 = MathHelper.lerp(tickDelta, player.prevCapeX, player.capeX)
                - MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double d1 = MathHelper.lerp(tickDelta, player.prevCapeY, player.capeY)
                - MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double d2 = MathHelper.lerp(tickDelta, player.prevCapeZ, player.capeZ)
                - MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        // Body yaw interpolation in Yarn for 1.19.4
        float bodyYaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * tickDelta;
        double sinYaw = MathHelper.sin(bodyYaw * ((float) Math.PI / 180F));
        double cosYaw = -MathHelper.cos(bodyYaw * ((float) Math.PI / 180F));

        float f1 = (float) d1 * 10.0F;
        f1 = MathHelper.clamp(f1, -6.0F, 32.0F);

        float f2 = (float) (d0 * sinYaw + d2 * cosYaw) * 100.0F;
        f2 = MathHelper.clamp(f2, 0.0F, 150.0F);

        float f3 = (float) (d0 * cosYaw - d2 * sinYaw) * 100.0F;
        f3 = MathHelper.clamp(f3, -20.0F, 20.0F);
        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        // Stride/bob from vanilla
        // prevStrideDistance + (strideDistance - prevStrideDistance) * tickDelta
        float stride = player.prevStrideDistance
                + (player.strideDistance - player.prevStrideDistance) * tickDelta;

        // The "walkDist" equivalent in 1.19.4 is the player's horizontalSpeed fields
        // (limb usage).
        float horizontalSpeed = player.prevHorizontalSpeed
                + (player.horizontalSpeed - player.prevHorizontalSpeed) * tickDelta;

        // Add the bobbing
        f1 += MathHelper.sin(horizontalSpeed * 6.0F) * 32.0F * stride;

        // The crouch check is isInSneakingPose() in Yarn
        if (player.isInSneakingPose()) {
            f1 += 25.0F;
        }

        bone.updateRotation(
                (float) -Math.toRadians(6.0F + f2 / 2.0F + f1),
                (float) Math.toRadians(f3 / 2.0F),
                (float) Math.toRadians(f3 / 2.0F));
    }

    /**
     * Lets you bend the "cape" bone in sync with the player’s front legs.
     */
    public static <T extends Item & GeoItem> void setFrontLegCapeAngle(GeoArmorRenderer<T> renderer, GeoBone bone) {
        if (renderer.getLeftLegBone() == null || renderer.getRightLegBone() == null) {
            return;
        }

        @SuppressWarnings("null")
        float legRot = Math.min(renderer.getLeftLegBone().getRotX(), renderer.getRightLegBone().getRotX());
        bone.setRotX((legRot > 0 ? 0 : legRot) * -1.2f);
    }

    /**
     * Same logic as {@link #applyCapeRotation}, but for a braid bone. Additional
     * pitch checks to limit rotation.
     */
    public static void applyBraidRotation(AbstractClientPlayerEntity player, GeoBone braid, float tickDelta) {
        applyCapeRotation(player, braid, tickDelta);

        // For pitch, use getPitch() in 1.19.4 Yarn
        float pitch = player.getPitch();
        if (pitch > 60) {
            braid.setRotX(0);
        } else if (pitch < -35) {
            braid.setRotX(30);
        }
    }

    /**
     * Toggle arm & sleeve visibility on the PlayerEntityModel.
     */
    public static <T extends LivingEntity> void setArmsVisibility(PlayerEntityModel<T> model, boolean visible) {
        model.rightArm.visible = visible;
        model.rightSleeve.visible = visible;
        model.leftArm.visible = visible;
        model.leftSleeve.visible = visible;
    }
}
