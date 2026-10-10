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

/**
 * Orcish Warplate (Half-Orc kin stock). Full set: Strength I while under half health. Half-Orcs also get
 * Resistance I (always). The Strength is a short effect, refreshed while the condition holds, so it ends
 * about 3 seconds after the wearer heals above half.
 */
public class OrcishWarplateArmorItem extends DndArmorItem {
    /** Strength while health is below this fraction of max health. */
    public static final float BLOODIED = 0.5F;
    private static final int BLOODIED_TICKS = 60;

    public OrcishWarplateArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.ORCISH_WARPLATE, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.STRENGTH, BLOODIED_TICKS));
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects(PlayerEntity wearer) {
        return wearer.getHealth() < wearer.getMaxHealth() * BLOODIED ? getFullSetEffects() : List.of();
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.HALFORC);
    }

    @Override
    public List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of(new StatusEffectInstance(StatusEffects.RESISTANCE, 239));
    }
}
