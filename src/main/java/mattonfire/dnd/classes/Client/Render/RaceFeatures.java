package mattonfire.dnd.classes.Client.Render;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceSize;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/**
 * Racial head features drawn on a biped head: elf ears, tiefling horns, half-orc tusks and a dragonborn
 * snout with frills. Small cuboids in head space, so they follow the head's turn and nod. Ears use the
 * skin's head-side pixels so they match the skin; the rest use one grey texture tinted per race (and per
 * ancestry for Dragonborn).
 * <p>
 * {@link #renderHead} is the shared entry point: {@link PlayerFeature} calls it for players, and NPC
 * renderers (settlement folk, later) can call it with their own head part. It's skipped while the head
 * slot holds anything, while invisible and while Identity gives the player a form.
 */
public final class RaceFeatures {
    public static final Identifier TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/entity/race/features.png");

    private static final float[] HORN = {0.62f, 0.3f, 0.34f};
    private static final float[] TUSK = {0.98f, 0.95f, 0.82f};
    private static final float[] EMBER = {0.78f, 0.22f, 0.16f};
    private static final float[] FROST = {0.86f, 0.93f, 1.0f};
    private static final float[] STORM = {0.28f, 0.46f, 0.92f};

    private static ModelPart ears;
    private static ModelPart horns;
    private static ModelPart tusks;
    private static ModelPart snout;

    private RaceFeatures() {
    }

    /**
     * Draws the race's head features. The matrix stack must be in the model's space (as inside a
     * feature renderer); {@code head} is the posed head part.
     */
    public static void renderHead(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            ModelPart head, DndRace race, DragonAncestry ancestry, Identifier skin) {
        if (race == DndRace.ELF || race == DndRace.HALFORC || race == DndRace.TIEFLING
                || race == DndRace.DRAGONBORN) {
            build();
        } else {
            return;
        }
        matrices.push();
        head.rotate(matrices);
        switch (race) {
            case ELF -> ears.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(skin)), light,
                    OverlayTexture.DEFAULT_UV);
            case TIEFLING -> draw(horns, matrices, vertexConsumers, light, HORN);
            case HALFORC -> draw(tusks, matrices, vertexConsumers, light, TUSK);
            case DRAGONBORN -> draw(snout, matrices, vertexConsumers, light, switch (ancestry) {
                case FROST -> FROST;
                case STORM -> STORM;
                default -> EMBER;
            });
            default -> {
            }
        }
        matrices.pop();
    }

    private static void draw(ModelPart part, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            float[] tint) {
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        part.render(matrices, buffer, light, OverlayTexture.DEFAULT_UV, tint[0], tint[1], tint[2], 1f);
    }

    private static synchronized void build() {
        if (ears != null) {
            return;
        }
        ears = buildEars();
        horns = buildHorns();
        tusks = buildTusks();
        snout = buildSnout();
    }

    // Head space: the head cube is x -4..4, y -8..0 (up is -y), z -4..4 (the face is at z -4). The hat
    // layer sits 0.5 outside it, so everything pokes out past that.

    /** Long pointed ears swept up and back, on the skin texture (64x64) at the head's side pixels. */
    private static ModelPart buildEars() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("right_ear", ModelPartBuilder.create().uv(0, 10).cuboid(-1f, -4f, -0.5f, 1f, 4f, 1f),
                ModelTransform.of(-4.2f, -3.5f, 0.5f, -0.45f, 0f, -0.55f));
        root.addChild("left_ear", ModelPartBuilder.create().uv(0, 10).mirrored().cuboid(0f, -4f, -0.5f, 1f, 4f, 1f),
                ModelTransform.of(4.2f, -3.5f, 0.5f, -0.45f, 0f, 0.55f));
        return TexturedModelData.of(data, 64, 64).createModel();
    }

    /**
     * Two ram horns curling from the temples: out to the side, then down and forward. They sit below a
     * hat brim (every class outfit has a hat), so they still show.
     */
    private static ModelPart buildHorns() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int side = -1; side <= 1; side += 2) {
            float out = side < 0 ? -3f : 0f;
            ModelPartData base = root.addChild(side < 0 ? "right_horn" : "left_horn",
                    ModelPartBuilder.create().uv(0, 0).cuboid(out, -2f, -1f, 3f, 2f, 2f),
                    ModelTransform.of(side * 4f, -6f, -0.5f, 0f, 0f, side * -0.35f));
            base.addChild("curl", ModelPartBuilder.create().uv(0, 8).cuboid(-0.75f, 0f, -0.75f, 1.5f, 3.5f, 1.5f),
                    ModelTransform.of(side * 2.4f, -1f, 0f, -0.45f, 0f, side * 0.25f));
        }
        return TexturedModelData.of(data, 32, 32).createModel();
    }

    /** Two lower tusks jutting up past the upper lip at the corners of the mouth. */
    private static ModelPart buildTusks() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("right_tusk", ModelPartBuilder.create().uv(0, 0).cuboid(-0.5f, -2.5f, -0.5f, 1f, 2.5f, 1f),
                ModelTransform.of(-2.5f, -1.5f, -4.6f, -0.2f, 0f, -0.2f));
        root.addChild("left_tusk", ModelPartBuilder.create().uv(0, 0).cuboid(-0.5f, -2.5f, -0.5f, 1f, 2.5f, 1f),
                ModelTransform.of(2.5f, -1.5f, -4.6f, -0.2f, 0f, 0.2f));
        return TexturedModelData.of(data, 32, 32).createModel();
    }

    /**
     * A blunt snout over the lower face with a nose ridge, cheek frills sweeping back from the jaw and a
     * row of spines down the back of the head.
     */
    private static ModelPart buildSnout() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("snout", ModelPartBuilder.create().uv(0, 0).cuboid(-2f, -3.5f, -7f, 4f, 3f, 3f),
                ModelTransform.NONE);
        root.addChild("nose_ridge", ModelPartBuilder.create().uv(0, 12).cuboid(-1f, -4.5f, -6.5f, 2f, 1f, 2.5f),
                ModelTransform.NONE);
        for (int side = -1; side <= 1; side += 2) {
            root.addChild(side < 0 ? "right_frill" : "left_frill", ModelPartBuilder.create().uv(16, 12)
                    .cuboid(-0.5f, -3f, 0f, 1f, 3f, 4f),
                    ModelTransform.of(side * 4.3f, -1f, -1f, 0.6f, side * -0.45f, 0f));
        }
        for (int i = 0; i < 3; i++) {
            root.addChild("spine_" + i, ModelPartBuilder.create().uv(16, 12).cuboid(-0.5f, -2f, 0f, 1f, 2f, 3f),
                    ModelTransform.of(0f, -8.2f + i * 2.6f, 3.6f + i * 0.3f, 0.5f, 0f, 0f));
        }
        return TexturedModelData.of(data, 32, 32).createModel();
    }

    /** Racial head features on the player model, for every player this client sees. */
    public static final class PlayerFeature
            extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
        public PlayerFeature(PlayerEntityRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                float animationProgress, float headYaw, float headPitch) {
            if (player.isInvisible() || !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    || !(player instanceof PlayerEntityExt ext)) {
                return;
            }
            DndRace race = ext.getBodyRace();
            if (race == DndRace.NONE || RaceSize.hasForm(player)) {
                return;
            }
            renderHead(matrices, vertexConsumers, light, getContextModel().head, race, ext.getDragonAncestry(),
                    player.getSkinTexture());
        }
    }
}
