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

/** Infernal Regalia (Tiefling kin stock). Full set: Fire Resistance. Tieflings also have Hellish Rebuke's cooldown halved ({@link RacialGear#rebukeCooldown}). */
public class InfernalRegaliaArmorItem extends DndArmorItem {
    public InfernalRegaliaArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.INFERNAL_REGALIA, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 239));
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.TIEFLING);
    }

    @Override
    public List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of();
    }
}
