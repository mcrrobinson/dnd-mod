package mattonfire.dnd.classes.Effects;

import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * Standing in a Beholder's anti-magic cone. While it lasts class specials don't work (the power-up,
 * the Warlock's fireballs, the Monk's double jump), and the magical buffs class powers give are
 * dispelled when it lands.
 */
public class AntiMagicEffect extends StatusEffect {
    public AntiMagicEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    /** Whether {@code entity}'s class specials are blocked right now (works on both sides: effects are synced to their owner). */
    public static boolean isSuppressed(LivingEntity entity) {
        return ModEffects.ANTI_MAGIC != null && entity.hasStatusEffect(ModEffects.ANTI_MAGIC);
    }

    /** Server side: checks a special the player is trying to use, telling them why it fizzled. */
    public static boolean blocks(PlayerEntity player) {
        if (!isSuppressed(player)) {
            return false;
        }
        player.sendMessage(Text.translatable("effect.dndclasses.anti_magic.blocked"), true);
        return true;
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onApplied(entity, attributes, amplifier);
        if (entity.world.isClient) {
            return;
        }
        for (StatusEffect magic : new StatusEffect[] {ModEffects.ARROW_STORM, ModEffects.MOB_REPEL,
                ModEffects.ARMOR_BUFF, ModEffects.INVULNERABILITY}) {
            if (magic != null && entity.hasStatusEffect(magic)) {
                entity.removeStatusEffect(magic);
            }
        }
    }
}
