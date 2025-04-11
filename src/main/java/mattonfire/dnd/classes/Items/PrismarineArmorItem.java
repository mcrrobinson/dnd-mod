package mattonfire.dnd.classes.Items;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.text.Text;

import java.util.List;

import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;
import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;


public class PrismarineArmorItem extends DndArmorItem {
    public PrismarineArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.PRISMARINE, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0, false, false));
    }
}