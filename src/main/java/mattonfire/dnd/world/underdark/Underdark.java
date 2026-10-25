package mattonfire.dnd.world.underdark;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

/**
 * The Underdark: a datapack dimension of huge caverns from y -256 to -65, "under the bedrock" (design 9; see
 * docs/systems/underdark.md). Its worldgen is data under {@code data/dndclasses/{dimension,dimension_type,worldgen}},
 * written by {@code tools/underdark_worldgen.py}.
 * <p>
 * Hooks for the tickets that come after the foundation:
 * <ul>
 * <li>{@link #IS_UNDERDARK} and the {@code HABITAT_*} biome tags, for spawn lines such as
 * {@code BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_TROLL), SpawnGroup.MONSTER, TROLL, w, 1, 1)}
 * (Troll, Basilisk, Mind Flayer and Rust Monster tickets; drow and duergar in T4 and T5).</li>
 * <li>{@link #isUnderdark(World)}, for spawn rules such as "only below y X in the Overworld, anywhere in the
 * Underdark".</li>
 * <li>{@link UnderdarkTravel#AFTER_TRAVEL}, fired after a player goes down or comes back up (hirelings follow).</li>
 * </ul>
 */
public final class Underdark {
    /** The Underdark world (and its dimension). */
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, id("underdark"));
    public static final RegistryKey<DimensionType> DIMENSION_TYPE = RegistryKey.of(RegistryKeys.DIMENSION_TYPE,
            id("underdark"));

    /** Lowest block (bedrock floor) and highest block (bedrock roof). */
    public static final int BOTTOM_Y = -256;
    public static final int TOP_Y = -65;

    public static final RegistryKey<Biome> FUNGAL_FOREST = biome("fungal_forest");
    public static final RegistryKey<Biome> MYCELIAL_CAVERNS = biome("mycelial_caverns");
    public static final RegistryKey<Biome> DARKLAKE = biome("darklake");
    public static final RegistryKey<Biome> FAERZRESS_HOLLOWS = biome("faerzress_hollows");
    public static final RegistryKey<Biome> GREY_REACHES = biome("grey_reaches");
    /** The five Underdark biomes. */
    public static final List<RegistryKey<Biome>> BIOMES = List.of(FUNGAL_FOREST, MYCELIAL_CAVERNS, DARKLAKE,
            FAERZRESS_HOLLOWS, GREY_REACHES);

    /** Every Underdark biome. */
    public static final TagKey<Biome> IS_UNDERDARK = biomeTag("is_underdark");
    /** Fungal Forest, Faerzress Hollows. */
    public static final TagKey<Biome> HABITAT_DROW = biomeTag("habitat/drow");
    /** Grey Reaches. */
    public static final TagKey<Biome> HABITAT_DUERGAR = biomeTag("habitat/duergar");
    /** Fungal Forest, Darklake (the Troll ticket may add Overworld biomes). */
    public static final TagKey<Biome> HABITAT_TROLL = biomeTag("habitat/troll");
    /** Grey Reaches, Faerzress Hollows. */
    public static final TagKey<Biome> HABITAT_BASILISK = biomeTag("habitat/basilisk");
    /** Faerzress Hollows. */
    public static final TagKey<Biome> HABITAT_MIND_FLAYER = biomeTag("habitat/mind_flayer");
    /** Grey Reaches, Mycelial Caverns. */
    public static final TagKey<Biome> HABITAT_RUST_MONSTER = biomeTag("habitat/rust_monster");

    private Underdark() {
    }

    /** Whether {@code world} is the Underdark. */
    public static boolean isUnderdark(World world) {
        return world != null && world.getRegistryKey() == WORLD;
    }

    /** The Underdark world, or null if this world doesn't have it (its data pack disabled). */
    public static ServerWorld world(MinecraftServer server) {
        return server.getWorld(WORLD);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }

    private static RegistryKey<Biome> biome(String path) {
        return RegistryKey.of(RegistryKeys.BIOME, id(path));
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.of(RegistryKeys.BIOME, id(path));
    }
}
