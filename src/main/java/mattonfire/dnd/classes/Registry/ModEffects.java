package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Effects.ArmorBuffEffect;
import mattonfire.dnd.classes.Effects.FreezeEffect;
import mattonfire.dnd.classes.Effects.InvulnerabilityEffect;
import mattonfire.dnd.classes.Effects.MobRepelEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEffects {
    public static StatusEffect FREEZE;
    public static StatusEffect INVULNERABILITY;
    public static StatusEffect MOB_REPEL;
    public static StatusEffect ARROW_STORM;
    public static StatusEffect ARMOR_BUFF;

    public static StatusEffect registerStatusEffect(String name, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier(DnDClasses.MOD_ID, name),
                effect);
    }

    public static void registerEffects() {
        FREEZE = registerStatusEffect("freeze", new FreezeEffect(StatusEffectCategory.HARMFUL, 3124687));
        INVULNERABILITY = registerStatusEffect("invulnerability",
                new InvulnerabilityEffect(StatusEffectCategory.BENEFICIAL, 0xFFD700));
        MOB_REPEL = registerStatusEffect("mob_repel", new MobRepelEffect(StatusEffectCategory.BENEFICIAL, 0x5555FF));
        ARROW_STORM = registerStatusEffect("arrow_storm",
                new mattonfire.dnd.classes.Effects.ArrowStorm(StatusEffectCategory.BENEFICIAL, 0x00FF00));
        ARMOR_BUFF = registerStatusEffect("armor_buff",
                new ArmorBuffEffect(StatusEffectCategory.BENEFICIAL, 0xB87333));
    }
}