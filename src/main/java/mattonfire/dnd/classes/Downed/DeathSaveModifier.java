package mattonfire.dnd.classes.Downed;

import mattonfire.dnd.classes.Abilities.Advantage;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Bonuses and advantage on death saves and Last Stand rolls, for auras (Paladin), racial traits and magic
 * items. The bonuses of every listener are summed and capped at +{@value DeathSaves#MAX_BONUS}. Advantage
 * and disadvantage from different listeners cancel, as in 5e.
 */
@FunctionalInterface
public interface DeathSaveModifier {
    Event<DeathSaveModifier> EVENT = EventFactory.createArrayBacked(DeathSaveModifier.class, listeners ->
            new DeathSaveModifier() {
                @Override
                public int bonus(ServerPlayerEntity player) {
                    int total = 0;
                    for (DeathSaveModifier listener : listeners) {
                        total += listener.bonus(player);
                    }
                    return total;
                }

                @Override
                public Advantage mode(ServerPlayerEntity player) {
                    int adv = 0;
                    int dis = 0;
                    for (DeathSaveModifier listener : listeners) {
                        Advantage mode = listener.mode(player);
                        if (mode == Advantage.ADVANTAGE) {
                            adv++;
                        } else if (mode == Advantage.DISADVANTAGE) {
                            dis++;
                        }
                    }
                    return Advantage.resolve(adv, dis);
                }
            });

    /** A flat bonus (or penalty) to the player's next death save. */
    int bonus(ServerPlayerEntity player);

    /** Advantage or disadvantage on the player's next death save (e.g. a party wipe at a boss). */
    default Advantage mode(ServerPlayerEntity player) {
        return Advantage.NORMAL;
    }
}
