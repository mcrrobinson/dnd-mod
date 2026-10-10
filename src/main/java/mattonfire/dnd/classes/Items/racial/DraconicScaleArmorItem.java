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

/** Draconic Scale (Dragonborn kin stock). Full set: Fire Resistance. Dragonborn also take no damage from their ancestry's element ({@link RacialGear#immuneTo}). */
public class DraconicScaleArmorItem extends DndArmorItem {
    public DraconicScaleArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.DRACONIC_SCALE, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 239));
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.DRAGONBORN);
    }

    @Override
    public List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of();
    }
}
