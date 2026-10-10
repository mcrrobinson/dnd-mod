package mattonfire.dnd.client.renderer;

import java.util.function.Function;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ElfEntity;
import mattonfire.dnd.entity.ElfMerchantEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * The racial NPC model kit: the player model (slim Alex arms or classic Steve ones) with optional head
 * features, a per-entity scale, armour, and arm poses for held items and drawn bows. The settlement NPCs of
 * each race use it with their own skins; for now that's the elves, who get pointed ears ({@link #EARS}: a
 * 1x3x1 cuboid each side of the head, textured from the skin's spare corner at 56, 0).
 *
 * <p>Later races add their own head features here (orc tusks, tiefling horns, a dragonborn snout). The
 * races ticket's {@code RaceFeatures.renderHead} draws the same features on players; once both are in, the
 * ears can move there and this kit can call it.
 */
public class RacialHumanoidRenderer<T extends MobEntity> extends BipedEntityRenderer<T, RacialHumanoidRenderer.HumanoidModel<T>> {
    public static final EntityModelLayer EARS = new EntityModelLayer(new Identifier(DnDClasses.MOD_ID, "racial_ears"), "main");

    /** What one race's NPCs look like. */
    public record Look(boolean slim, boolean elfEars, float scale) {
    }

    public static final Look ELF = new Look(true, true, 1.0F);

    private final Function<T, Identifier> texture;
    private final Look look;

    public RacialHumanoidRenderer(EntityRendererFactory.Context context, Look look, Function<T, Identifier> texture) {
        super(context, new HumanoidModel<>(context.getPart(look.slim() ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER),
                look.slim()), 0.5F * look.scale());
        this.texture = texture;
        this.look = look;
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        if (look.elfEars()) {
            this.addFeature(new EarsFeature<>(this, context.getPart(EARS)));
        }
    }

    // ---- The elves ----

    private static final Identifier[] ELF_SKINS = new Identifier[ElfEntity.VARIANTS];
    private static final Identifier SPEAKER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/speaker.png");
    private static final Identifier FLETCHER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/fletcher.png");

    static {
        for (int i = 0; i < ELF_SKINS.length; i++) {
            ELF_SKINS[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/elf_" + i + ".png");
        }
    }

    /** Wood elves and wardens: one of the six skins (wardens wear their leathers over it). */
    public static <E extends ElfEntity> RacialHumanoidRenderer<E> elf(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ELF, elf -> ELF_SKINS[Math.floorMod(elf.getVariant(), ELF_SKINS.length)]);
    }

    public static RacialHumanoidRenderer<ElfMerchantEntity> elfMerchant(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ELF,
                elf -> elf.getType() == ModEntityTypes.ELF_SPEAKER ? SPEAKER_SKIN : FLETCHER_SKIN);
    }

    /** The ear cuboids, posed on the sides of the head (pivot at the head's pivot, the neck). */
    public static TexturedModelData earsModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("right_ear", ModelPartBuilder.create().uv(56, 0).cuboid(-1.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.of(-4.0F, -4.0F, 0.5F, -0.35F, 0.0F, -0.5F));
        root.addChild("left_ear", ModelPartBuilder.create().uv(56, 0).mirrored().cuboid(0.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.of(4.0F, -4.0F, 0.5F, -0.35F, 0.0F, 0.5F));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public Identifier getTexture(T entity) {
        return this.texture.apply(entity);
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        if (this.look.scale() != 1.0F) {
            matrices.scale(this.look.scale(), this.look.scale(), this.look.scale());
        }
    }

    /** The player model, with arm poses from what the mob holds (a bow drawn while it's being used). */
    public static class HumanoidModel<T extends MobEntity> extends PlayerEntityModel<T> {
        HumanoidModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            ItemStack held = entity.getMainHandStack();
            this.rightArmPose = held.isEmpty() ? ArmPose.EMPTY
                    : entity.isUsingItem() && held.getItem() instanceof BowItem ? ArmPose.BOW_AND_ARROW : ArmPose.ITEM;
            this.leftArmPose = ArmPose.EMPTY;
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        }
    }

    /** Draws the ears on the head, following its turn and tilt. */
    private static final class EarsFeature<T extends MobEntity> extends FeatureRenderer<T, HumanoidModel<T>> {
        private final ModelPart ears;

        EarsFeature(FeatureRendererContext<T, HumanoidModel<T>> context, ModelPart ears) {
            super(context);
            this.ears = ears;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle,
                           float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            matrices.push();
            this.getContextModel().head.rotate(matrices);
            VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(this.getTexture(entity)));
            this.ears.render(matrices, consumer, light, LivingEntityRenderer.getOverlay(entity, 0.0F));
            matrices.pop();
        }
    }
}
