package mattonfire.dnd.classes.Client.Render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Renders the non-GeckoLib armor sets ({@link DndArmorItem} with a {@link ModArmorMaterials}
 * material). Each set ships several 64x32 vanilla-layout layers under
 * {@code textures/models/armor/<set>/}: {@code body_*} for helmet, chestplate and boots, and
 * {@code leggings_*} for leggings. {@code lower}, {@code middle} and {@code upper} are drawn on
 * slightly bigger shells, so the layers stack instead of z-fighting. Optional {@code *_overlay}
 * layers are drawn on the same shell right after their base layer.
 */
@Environment(EnvType.CLIENT)
public final class LayeredArmorRenderer implements ArmorRenderer {

    private record Layer(Identifier texture, BipedEntityModel<LivingEntity> model) {
    }

    // Shell sizes. Vanilla uses 1.0 for helmet/chestplate/boots and 0.5 for leggings.
    private static final float[] BODY_DILATION = { 0.9f, 1.0f, 1.1f };
    private static final float[] LEGGINGS_DILATION = { 0.45f, 0.55f, 0.65f };
    private static final String[] LEVELS = { "lower", "middle", "upper" };

    /** Which layer files each set has (index into {@link #LEVELS}). */
    private static final Map<ModArmorMaterials, int[][]> LAYERS = new EnumMap<>(ModArmorMaterials.class);
    private static final Map<ModArmorMaterials, Boolean> HAS_OVERLAYS = new EnumMap<>(ModArmorMaterials.class);

    static {
        // { body levels }, { leggings levels }
        LAYERS.put(ModArmorMaterials.PRISMARINE, new int[][] { { 1, 2 }, { 1 } });
        LAYERS.put(ModArmorMaterials.HOLY, new int[][] { { 0, 1, 2 }, { 0, 1, 2 } });
        LAYERS.put(ModArmorMaterials.ROBE, new int[][] { { 0, 1 }, { 0, 1 } });
        LAYERS.put(ModArmorMaterials.STEAMPUNK, new int[][] { { 0, 1, 2 }, { 0, 1 } });
        LAYERS.put(ModArmorMaterials.WARRIOR, new int[][] { { 0, 1, 2 }, { 0, 1, 2 } });
        LAYERS.put(ModArmorMaterials.WITHER, new int[][] { { 1 }, { 1 } });
        LAYERS.put(ModArmorMaterials.WOODEN, new int[][] { { 1, 2 }, { 1, 2 } });
        HAS_OVERLAYS.put(ModArmorMaterials.ROBE, true);
    }

    private final List<Layer> bodyLayers = new ArrayList<>();
    private final List<Layer> leggingsLayers = new ArrayList<>();

    private LayeredArmorRenderer(ModArmorMaterials material) {
        int[][] levels = LAYERS.get(material);
        boolean overlays = HAS_OVERLAYS.getOrDefault(material, false);
        addLayers(bodyLayers, material, "body", levels[0], BODY_DILATION, overlays);
        addLayers(leggingsLayers, material, "leggings", levels[1], LEGGINGS_DILATION, overlays);
    }

    private static void addLayers(List<Layer> out, ModArmorMaterials material, String part, int[] levels,
            float[] dilations, boolean overlays) {
        for (int level : levels) {
            BipedEntityModel<LivingEntity> model = createModel(dilations[level]);
            String base = "textures/models/armor/" + material.getTextureFolder() + "/" + part + "_" + LEVELS[level];
            out.add(new Layer(new Identifier("dndclasses", base + ".png"), model));
            if (overlays) {
                out.add(new Layer(new Identifier("dndclasses", base + "_overlay.png"), model));
            }
        }
    }

    private static BipedEntityModel<LivingEntity> createModel(float dilation) {
        return new ArmorEntityModel<>(
                TexturedModelData.of(ArmorEntityModel.getModelData(new Dilation(dilation)), 64, 32).createModel());
    }

    /** Registers a renderer for every registered {@link DndArmorItem} that uses a {@link ModArmorMaterials}. */
    public static void registerAll() {
        Map<ModArmorMaterials, List<Item>> items = new EnumMap<>(ModArmorMaterials.class);
        for (Item item : Registries.ITEM) {
            if (item instanceof DndArmorItem armor && armor.getMaterial() instanceof ModArmorMaterials material) {
                items.computeIfAbsent(material, m -> new ArrayList<>()).add(item);
            }
        }
        items.forEach((material, list) -> ArmorRenderer.register(new LayeredArmorRenderer(material),
                list.toArray(new Item[0])));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
            LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        List<Layer> layers = slot == EquipmentSlot.LEGS ? leggingsLayers : bodyLayers;
        for (Layer layer : layers) {
            BipedEntityModel<LivingEntity> model = layer.model();
            contextModel.copyBipedStateTo(model);
            setVisible(model, slot);
            ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, layer.texture());
        }
    }

    // Same parts as vanilla ArmorFeatureRenderer.setVisible.
    private static void setVisible(BipedEntityModel<LivingEntity> model, EquipmentSlot slot) {
        model.setVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }
}
