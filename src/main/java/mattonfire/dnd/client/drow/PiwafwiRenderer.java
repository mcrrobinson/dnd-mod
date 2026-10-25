package mattonfire.dnd.client.drow;

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
 * Draws the Piwafwi, the Cloak of the Drow, worn in the chest slot: a dark hooded cloak over body and arms, one
 * 64x32 vanilla-layout layer ({@code textures/models/armor/piwafwi/body.png}, from
 * {@code tools/drow_spire_textures.py}) on a shell a little bigger than a chestplate's.
 */
@Environment(EnvType.CLIENT)
public final class PiwafwiRenderer implements ArmorRenderer {
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/models/armor/piwafwi/body.png");

    private final BipedEntityModel<LivingEntity> model = new ArmorEntityModel<>(
            TexturedModelData.of(ArmorEntityModel.getModelData(new Dilation(1.05F)), 64, 32).createModel());

    private PiwafwiRenderer() {
    }

    static void register() {
        ArmorRenderer.register(new PiwafwiRenderer(), ModItems.PIWAFWI);
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
        this.model.head.visible = true;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, this.model, TEXTURE);
    }
}
