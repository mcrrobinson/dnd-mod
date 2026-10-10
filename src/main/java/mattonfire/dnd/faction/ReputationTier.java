package mattonfire.dnd.faction;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** The six standings, from Hostile to Exalted. Shared by server and client. */
public enum ReputationTier {
    HOSTILE("hostile", -1000, -500, Formatting.DARK_RED),
    UNFRIENDLY("unfriendly", -499, -100, Formatting.RED),
    NEUTRAL("neutral", -99, 99, Formatting.GRAY),
    FRIENDLY("friendly", 100, 399, Formatting.GREEN),
    HONORED("honored", 400, 749, Formatting.AQUA),
    EXALTED("exalted", 750, 1000, Formatting.GOLD);

    public final String id;
    public final int min;
    public final int max;
    public final Formatting color;

    ReputationTier(String id, int min, int max, Formatting color) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.color = color;
    }

    public static ReputationTier of(int value) {
        for (ReputationTier tier : values()) {
            if (value <= tier.max) {
                return tier;
            }
        }
        return EXALTED;
    }

    public boolean atLeast(ReputationTier other) {
        return this.ordinal() >= other.ordinal();
    }

    public MutableText displayName() {
        return Text.translatable("faction.dndclasses.tier." + this.id).formatted(this.color);
    }
}
