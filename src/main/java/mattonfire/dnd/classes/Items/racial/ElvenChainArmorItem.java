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

/** Elven Chain (Elf kin stock). Full set: Speed I. Elves also make no sculk vibrations while sneaking ({@link RacialGear#silencesVibrations}). */
public class ElvenChainArmorItem extends DndArmorItem {
    public ElvenChainArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.ELVEN_CHAIN, type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.SPEED, 239));
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.ELF);
    }

    @Override
    public List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of();
    }
}
