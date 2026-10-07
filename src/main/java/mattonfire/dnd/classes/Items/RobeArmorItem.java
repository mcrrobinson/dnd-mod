package mattonfire.dnd.classes.Items;
import mattonfire.dnd.classes.DndCharacter;
import java.util.Set;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;
import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;


public class RobeArmorItem extends DndArmorItem {
    public RobeArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.ROBE, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(
            new StatusEffectInstance(StatusEffects.JUMP_BOOST, 239)
            );
    }

    @Override
    public Set<DndCharacter> getMatchingClasses() {
        return Set.of(DndCharacter.MONK, DndCharacter.ALCHEMIST);
    }

    @Override
    public List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.SPEED, 239));
    }
}