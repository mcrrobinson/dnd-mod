package mattonfire.dnd.classes.Feats;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * What a feat does, with the same hook shape as {@code ClassSkills} and {@code MagicEffect}. Registered per feat in
 * {@link FeatEffects}, and called from {@code ProgressionEvents} after the class and magic item hooks, only for the
 * feats of the player's current class. Every hook is optional. Attribute bonuses (Tough, Mobile) are kept by
 * {@link Feats#refresh}.
 */
public interface FeatEffect {
    /** Damage the player deals, before armor. */
    default float modifyDealtDamage(ServerPlayerEntity player, LivingEntity target, DamageSource source,
            float amount) {
        return amount;
    }

    /** Damage the player takes, before armor. */
    default float modifyTakenDamage(PlayerEntity player, DamageSource source, float amount) {
        return amount;
    }

    /** Once a second. */
    default void secondTick(ServerPlayerEntity player) {
    }

    /** Every {@value FeatEffects#FAST_TICKS} ticks, for quick reactions (opportunity bolt, first to act). */
    default void fastTick(ServerPlayerEntity player) {
    }

    /** The player killed something. */
    default void onKill(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
    }
}
