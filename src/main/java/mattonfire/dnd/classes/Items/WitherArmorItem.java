package mattonfire.dnd.classes.Items;

import mattonfire.dnd.classes.DndCharacter;
import java.util.Set;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;
import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;

public class WitherArmorItem extends DndArmorItem {
    public WitherArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.WITHER, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(
                new StatusEffectInstance(StatusEffects.JUMP_BOOST, 239));
    }

    @Override
    public Set<DndCharacter> getMatchingClasses() {
        return Set.of(DndCharacter.WARLOCK, DndCharacter.NECROMANCER);
    }

    @Override
    public List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.STRENGTH, 239));
    }

}