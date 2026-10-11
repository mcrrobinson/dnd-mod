package mattonfire.dnd.faction;

import net.minecraft.entity.player.PlayerEntity;

/**
 * A faction member that can hold a grudge against a player for something other than a hit (a goblin who saw
 * you open the camp chief's chest). {@link TierEffects#provoked} counts it as provoked, whatever the tier.
 */
public interface Provokable {
    boolean provokedBy(PlayerEntity player);
}
