package mattonfire.dnd.classes.Effects;

import mattonfire.dnd.classes.Rest.RestState;
import mattonfire.dnd.classes.Rest.RestSync;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Well Rested, from a night (or an afternoon) in a rented tavern room: one temporary charge on top
 * of the max, spent first ({@code Charges.spend}). The charge is given by {@code TavernRooms} when
 * the rest ends; this effect only takes it away again when it runs out or is cleared.
 */
public class WellRestedEffect extends StatusEffect {
    /** 10 minutes. */
    public static final int DURATION = 12000;

    public WellRestedEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);
        if (entity instanceof ServerPlayerEntity player) {
            clearTempCharges(player);
        }
    }

    /** Drops any temporary charges the player has left. */
    public static void clearTempCharges(ServerPlayerEntity player) {
        RestState state = RestState.get(player);
        if (state.tempCharges > 0) {
            state.tempCharges = 0;
            state.save(player);
            RestSync.sync(player);
        }
    }
}
