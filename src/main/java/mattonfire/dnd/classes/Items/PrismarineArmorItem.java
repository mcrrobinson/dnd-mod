package mattonfire.dnd.classes.Items;

import mattonfire.dnd.classes.DndCharacter;
import java.util.Set;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

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

    @Override
    public Set<DndCharacter> getMatchingClasses() {
        return Set.of(DndCharacter.DRUID);
    }

    @Override
    public List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 239));
    }
}