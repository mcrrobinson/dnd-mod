package mattonfire.dnd.entity.boss;

/**
 * A creature with 5e Legendary Resistance: a few times a day it can turn a failed save into a success
 * ({@link LegendaryResistance}). The Ancient dragons have it; other bosses can opt in by implementing this.
 */
public interface LegendaryCreature {
    LegendaryResistance legendaryResistance();
}
