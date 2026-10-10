package mattonfire.dnd.magic;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.text.Text;

/**
 * D&D magic item rarity. The tier sets the colour of the item's name everywhere it's shown (tooltips,
 * the held-item popup, chat hovers) and, for +N gear, the default bonus.
 */
public enum MagicTier {
    COMMON("common", 0xFFFFFF, 0),
    UNCOMMON("uncommon", 0x1EFF00, 1),
    RARE("rare", 0x0070DD, 2),
    VERY_RARE("very_rare", 0xA335EE, 3),
    LEGENDARY("legendary", 0xFF8000, 3);

    /** Lower-case id used in NBT, loot tables and commands. */
    public final String id;
    /** Name colour, 0xRRGGBB. */
    public final int color;
    /** The +N bonus a generic weapon or armor piece of this tier gets from loot. */
    public final int defaultPlus;

    MagicTier(String id, int color, int defaultPlus) {
        this.id = id;
        this.color = color;
        this.defaultPlus = defaultPlus;
    }

    public Text displayName() {
        return Text.translatable("magic.dndclasses.tier." + id);
    }

    /** Parses a tier id ("rare", "very_rare", also "veryrare" / "VERY-RARE"); null if unknown. */
    public static @Nullable MagicTier byId(@Nullable String id) {
        if (id == null)
            return null;
        String key = id.toLowerCase(Locale.ROOT).replace('-', '_');
        for (MagicTier tier : values()) {
            if (tier.id.equals(key) || tier.id.replace("_", "").equals(key))
                return tier;
        }
        return null;
    }
}
