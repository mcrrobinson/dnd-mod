package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

/**
 * The racial banner patterns (docs/items/banner-patterns.md): one per racial home or faction, each a
 * {@link BannerPattern} with a pattern item that unlocks it in the loom (through the
 * {@code dndclasses:pattern_item/<name>} banner pattern tag), and textures in {@code textures/entity/banner/} and
 * {@code textures/entity/shield/} (written by {@code tools/banner_pattern_textures.py}).
 *
 * <p>Structures hang banners with them through {@link #decorate}; the kin merchants sell the items
 * ({@code RacialBannerOffers}).
 */
public final class ModBannerPatterns {
    private static final List<Pattern> ALL = new ArrayList<>();

    /** Orc Strongholds: two crossed curved tusks under a handprint. */
    public static final Pattern BLOODTUSK = new Pattern("bloodtusk");
    /** Dwarven Fortresses: a hammer over an anvil in a hexagon. */
    public static final Pattern CLAN_ANVIL = new Pattern("clan_anvil");
    /** Elven Enclaves: a single leaf rising from a crescent moon. */
    public static final Pattern SYLVAN_LEAF = new Pattern("sylvan_leaf");
    /** Dragon Temples: three dragon heads meeting in a triangle. */
    public static final Pattern THREE_BREATHS = new Pattern("three_breaths");
    /** Brimstone Sanctums: a pentagram ring with horns. */
    public static final Pattern INFERNAL_PACT = new Pattern("infernal_pact");
    /** Cogsdeep: a cog with a magnifying glass. */
    public static final Pattern GNOME_COG = new Pattern("gnome_cog");
    /** Hobbit villages' Green Dragon inn: a curled green dragon. */
    public static final Pattern GREEN_DRAGON = new Pattern("green_dragon");
    /** Goblin camps and Warlords: a crude grinning goblin skull. */
    public static final Pattern GOBLIN_SKULL = new Pattern("goblin_skull");

    private ModBannerPatterns() {
    }

    public static List<Pattern> all() {
        return List.copyOf(ALL);
    }

    /** Registers every pattern and its item. Call once, from the mod initializer. */
    public static void register() {
        for (Pattern pattern : ALL) {
            Registry.register(Registries.BANNER_PATTERN, pattern.key, new BannerPattern(pattern.code()));
            pattern.item = Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, pattern.name + "_banner_pattern"),
                    new BannerPatternItem(pattern.tag, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
        }
    }

    /**
     * Sets the patterns of the banner at {@code pos} (a banner block placed just before), bottom layer first. Does
     * nothing if there's no banner there.
     */
    public static void decorate(WorldAccess world, BlockPos pos, Layer... layers) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof BannerBlockEntity banner)) {
            return;
        }
        NbtList patterns = new NbtList();
        for (Layer layer : layers) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Pattern", layer.code());
            entry.putInt("Color", layer.color().getId());
            patterns.add(entry);
        }
        NbtCompound nbt = new NbtCompound();
        nbt.put("Patterns", patterns);
        banner.readNbt(nbt);
        banner.markDirty();
    }

    /** One of the racial patterns. */
    public static final class Pattern {
        private final String name;
        private final RegistryKey<BannerPattern> key;
        private final TagKey<BannerPattern> tag;
        private Item item;

        private Pattern(String name) {
            this.name = name;
            this.key = RegistryKey.of(RegistryKeys.BANNER_PATTERN, new Identifier(DnDClasses.MOD_ID, name));
            this.tag = TagKey.of(RegistryKeys.BANNER_PATTERN, new Identifier(DnDClasses.MOD_ID, "pattern_item/" + name));
            ALL.add(this);
        }

        public String name() {
            return this.name;
        }

        /** The short id banners save in their {@code Patterns} NBT ({@code dnd_<name>}). */
        public String code() {
            return "dnd_" + this.name;
        }

        public RegistryKey<BannerPattern> key() {
            return this.key;
        }

        /** The pattern item that unlocks it in the loom. */
        public Item item() {
            return this.item;
        }

        /** This pattern in {@code color}, as a banner layer. */
        public Layer in(DyeColor color) {
            return new Layer(this.code(), color);
        }
    }

    /**
     * One layer of a banner: a pattern's short id (a racial one's {@link Pattern#code()}, or a vanilla one such as
     * {@code "bo"} for the border) in a colour.
     */
    public record Layer(String code, DyeColor color) {
    }
}
