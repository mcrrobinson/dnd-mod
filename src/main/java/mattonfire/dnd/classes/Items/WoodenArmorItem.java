package mattonfire.dnd.classes.Items;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;
import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;

public class WoodenArmorItem extends DndArmorItem {
    public WoodenArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.WOODEN, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(
                new StatusEffectInstance(StatusEffects.JUMP_BOOST, 239));
    }
}