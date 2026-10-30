package mattonfire.dnd.client.renderer;

import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Draws the Red Dragon Scale Mail in the chest slot: one 64x32 vanilla-layout layer
 * ({@code textures/models/armor/red_dragon_scale/body.png}, from tools/boss_item_textures.py) on a chestplate shell.
 */
@Environment(EnvType.CLIENT)
public final class RedDragonScaleMailRenderer implements ArmorRenderer {
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/models/armor/red_dragon_scale/body.png");

    private final BipedEntityModel<LivingEntity> model = new ArmorEntityModel<>(
            TexturedModelData.of(ArmorEntityModel.getModelData(new Dilation(1.0F)), 64, 32).createModel());

    private RedDragonScaleMailRenderer() {
    }

    public static void register() {
        ArmorRenderer.register(new RedDragonScaleMailRenderer(), ModItems.RED_DRAGON_SCALE_MAIL);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, LivingEntity entity,
            EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        contextModel.copyBipedStateTo(this.model);
        this.model.setVisible(false);
        this.model.body.visible = true;
        this.model.rightArm.visible = true;
        this.model.leftArm.visible = true;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, this.model, TEXTURE);
    }
}
