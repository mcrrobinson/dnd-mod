package mattonfire.dnd.classes;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEffects {
    public static StatusEffect FREEZE;

    public static StatusEffect registerStatusEffect(String name) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier(DnDClasses.MOD_ID, name),
                new FreezeEffect(StatusEffectCategory.HARMFUL, 3124687));
    }

    public static void registerEffects() {
        FREEZE = registerStatusEffect("freeze");
    }
}