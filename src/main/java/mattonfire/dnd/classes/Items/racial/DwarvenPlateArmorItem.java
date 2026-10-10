package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;
import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;
import mattonfire.dnd.classes.Race.DndRace;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

/** Dwarven Plate (Dwarf kin stock). Full set: Resistance I. Dwarves also get Haste I. */
public class DwarvenPlateArmorItem extends DndArmorItem {
    public DwarvenPlateArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.DWARVEN_PLATE, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.RESISTANCE, 239));
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.DWARF);
    }

    @Override
    public List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of(new StatusEffectInstance(StatusEffects.HASTE, 239));
    }
}
