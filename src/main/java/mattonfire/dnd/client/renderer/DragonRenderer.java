package mattonfire.dnd.client.renderer;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.DragonPartLayout;
import mattonfire.dnd.entity.MultipartDragon;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders a multipart dragon and fits each hittable part to its bones' cubes as they are animated. */
public class DragonRenderer<T extends MobEntity & GeoAnimatable & MultipartDragon> extends GeoEntityRenderer<T> {
    private static final Matrix4f IDENTITY = new Matrix4f();

    public DragonRenderer(EntityRendererFactory.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public void preRender(MatrixStack poseStack, T animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        if (isReRender) {
            return;
        }
        // GeckoLib only records a bone's world matrix while it renders if asked to
        DragonPartLayout layout = animatable.getPartLayout();
        for (int i = 0; i < layout.size(); i++) {
            for (String boneName : layout.bones(i)) {
                model.getBone(boneName).ifPresent(bone -> bone.setTrackingMatrices(true));
            }
        }
    }

    @Override
    public void postRender(MatrixStack poseStack, T animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        if (isReRender) {
            return;
        }
        DragonPartLayout layout = animatable.getPartLayout();
        DragonPart[] parts = animatable.getParts();
        long time = animatable.world.getTime();
        // Anchor to where the model is drawn this frame, not the last tick's position
        Vec3d base = new Vec3d(
                MathHelper.lerp(partialTick, animatable.prevX, animatable.getX()),
                MathHelper.lerp(partialTick, animatable.prevY, animatable.getY()),
                MathHelper.lerp(partialTick, animatable.prevZ, animatable.getZ()));
        for (int i = 0; i < parts.length; i++) {
            List<Box> shapes = new ArrayList<>();
            for (String boneName : layout.bones(i)) {
                GeoBone bone = model.getBone(boneName).orElse(null);
                if (bone == null || bone.isHidden()) {
                    continue;
                }
                for (GeoCube cube : bone.getCubes()) {
                    addCube(shapes, bone, cube, base);
                }
            }
            parts[i].setRenderedShapes(shapes, time);
        }
    }

    /** Adds a cube as animated this frame, repeating the transforms GeckoLib renders it with. */
    private static void addCube(List<Box> shapes, GeoBone bone, GeoCube cube, Vec3d base) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f p = vertex.position();
                minX = Math.min(minX, p.x());
                minY = Math.min(minY, p.y());
                minZ = Math.min(minZ, p.z());
                maxX = Math.max(maxX, p.x());
                maxY = Math.max(maxY, p.y());
                maxZ = Math.max(maxZ, p.z());
            }
        }
        if (minX == Float.POSITIVE_INFINITY) {
            return;
        }
        // The bone's tracked matrix is taken at its pivot after its animated rotation, relative to the
        // entity; GeckoLib then moves back off the pivot and rotates the cube around its own pivot.
        // GeckoLib 4.2's RenderUtils.translateMatrix adds an identity matrix (not just the offset) to
        // the local/world space matrices, so take that back off.
        float cubePivotX = (float) cube.pivot().x / 16f, cubePivotY = (float) cube.pivot().y / 16f, cubePivotZ = (float) cube.pivot().z / 16f;
        Matrix4f matrix = new Matrix4f(bone.getLocalSpaceMatrix()).sub(IDENTITY)
                .translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f)
                .translate(cubePivotX, cubePivotY, cubePivotZ)
                .rotateZ((float) cube.rotation().z)
                .rotateY((float) cube.rotation().y)
                .rotateX((float) cube.rotation().x)
                .translate(-cubePivotX, -cubePivotY, -cubePivotZ);
        Vector3f o = matrix.transformPosition(minX, minY, minZ, new Vector3f());
        Vector3f x = matrix.transformPosition(maxX, minY, minZ, new Vector3f()).sub(o);
        Vector3f y = matrix.transformPosition(minX, maxY, minZ, new Vector3f()).sub(o);
        Vector3f z = matrix.transformPosition(minX, minY, maxZ, new Vector3f()).sub(o);
        DragonPart.addCube(shapes, base.add(o.x, o.y, o.z), new Vec3d(x.x, x.y, x.z), new Vec3d(y.x, y.y, y.z), new Vec3d(z.x, z.y, z.z));
    }
}
