package mattonfire.dnd.world.gen.dungeon;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;

/**
 * A dungeon's theme: its block palette, how deep it is, which tiers it can roll and the word lists
 * its name is made from (lang keys {@code dungeon.dndclasses.<theme>.place.<n>} and {@code .name.<n>}).
 *
 * The Crypt and the Goblin Warren are finished; the Dwarven Ruin is a placeholder that reuses the
 * Crypt palette until its own ticket gives it one (no structure JSON uses it yet).
 */
public enum DungeonTheme implements StringIdentifiable {
    CRYPT("crypt", Palette.CRYPT, 1, 3, 6, 16),
    GOBLIN_WARREN("goblin_warren", Palette.WARREN, 1, 4, 4, 8),
    // TODO(dungeons ticket 8): ruined dwarven palette, deep placement (y -30..0), dome boss room
    DWARVEN_RUIN("dwarven_ruin", Palette.CRYPT, 2, 4, 4, 8);

    public static final Codec<DungeonTheme> CODEC = StringIdentifiable.createCodec(DungeonTheme::values);

    private final String id;
    private final Palette palette;
    private final int minTier;
    /** The highest tier it rolls nearer than {@link DungeonPlanner#FAR_OUT} blocks from spawn; beyond that, IV. */
    private final int maxNearTier;
    private final int places;
    private final int names;

    DungeonTheme(String id, Palette palette, int minTier, int maxNearTier, int places, int names) {
        this.id = id;
        this.palette = palette;
        this.minTier = minTier;
        this.maxNearTier = maxNearTier;
        this.places = places;
        this.names = names;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String id() {
        return this.id;
    }

    public Palette palette() {
        return this.palette;
    }

    public int minTier() {
        return this.minTier;
    }

    public int maxNearTier() {
        return this.maxNearTier;
    }

    public int placeCount() {
        return this.places;
    }

    public int nameCount() {
        return this.names;
    }

    /** True for the Crypt: its boss is the Lich, whose phylactery waits in a reliquary off the boss room. */
    public boolean hasReliquary() {
        return this == CRYPT;
    }

    public String translationKey() {
        return "dungeon.dndclasses." + this.id;
    }

    public static DungeonTheme byId(String id) {
        for (DungeonTheme theme : values()) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return CRYPT;
    }

    /** The blocks a theme builds with. Every room asks the palette, so themes share their geometry. */
    public static class Palette {
        static final Palette CRYPT = new Palette();
        static final Palette WARREN = new WarrenPalette();

        /** The outer, waterproof skin of every room: keeps caves, aquifers and lava out. */
        public BlockState shell() {
            return Blocks.DEEPSLATE_TILES.getDefaultState();
        }

        /** The inner wall and ceiling. */
        public BlockState wall(Random random) {
            float r = random.nextFloat();
            return (r < 0.12F ? Blocks.MOSSY_STONE_BRICKS : r < 0.24F ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState();
        }

        public BlockState floor(Random random) {
            float r = random.nextFloat();
            return (r < 0.1F ? Blocks.ANDESITE : r < 0.2F ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_ANDESITE).getDefaultState();
        }

        /** The band along the foot of the walls and round doorways. */
        public BlockState trim() {
            return Blocks.POLISHED_DEEPSLATE.getDefaultState();
        }

        public BlockState accent() {
            return Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        }

        public Block pillar() {
            return Blocks.STONE_BRICKS;
        }

        /** What a hidden doorway is bricked up with: breakable by anyone. */
        public BlockState secretWall() {
            return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        }

        public BlockState light(boolean hanging) {
            return Blocks.SOUL_LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, hanging);
        }

        /** Props floors up over caves. */
        public BlockState foundation() {
            return Blocks.COBBLED_DEEPSLATE.getDefaultState();
        }

        /** Stonework where it shows at the surface (the entrance). */
        public BlockState surface(Random random) {
            return (random.nextFloat() < 0.5F ? Blocks.MOSSY_STONE_BRICKS : Blocks.MOSSY_COBBLESTONE).getDefaultState();
        }

        public BlockState surfaceWall() {
            return Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState();
        }

        /** The steps of the entrance's spiral stair. */
        public Block stairs() {
            return Blocks.STONE_BRICK_STAIRS;
        }
    }

    /**
     * The Goblin Warren: rough-hewn tunnels dug out of the rock and shored up with timber, the
     * palisade's spruce logs from {@code GoblinCampPiece}, coarse earth underfoot and plain lanterns.
     */
    static class WarrenPalette extends Palette {
        @Override
        public BlockState wall(Random random) {
            float r = random.nextFloat();
            return (r < 0.45F ? Blocks.COBBLESTONE : r < 0.65F ? Blocks.STONE : r < 0.8F ? Blocks.MOSSY_COBBLESTONE
                    : r < 0.92F ? Blocks.ANDESITE : Blocks.TUFF).getDefaultState();
        }

        @Override
        public BlockState floor(Random random) {
            float r = random.nextFloat();
            return (r < 0.45F ? Blocks.COARSE_DIRT : r < 0.65F ? Blocks.PACKED_MUD : r < 0.8F ? Blocks.ROOTED_DIRT
                    : r < 0.92F ? Blocks.COBBLESTONE : Blocks.DIRT).getDefaultState();
        }

        /** Timber shoring along the foot of the walls and round the doorways. */
        @Override
        public BlockState trim() {
            return Blocks.SPRUCE_LOG.getDefaultState();
        }

        @Override
        public BlockState accent() {
            return Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();
        }

        @Override
        public Block pillar() {
            return Blocks.SPRUCE_LOG;
        }

        /** A tunnel the goblins filled in with packed earth: plain to see once you look for it, quick to dig. */
        @Override
        public BlockState secretWall() {
            return Blocks.COARSE_DIRT.getDefaultState();
        }

        @Override
        public BlockState light(boolean hanging) {
            return Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, hanging);
        }

        @Override
        public BlockState foundation() {
            return Blocks.COBBLESTONE.getDefaultState();
        }

        @Override
        public BlockState surface(Random random) {
            float r = random.nextFloat();
            return (r < 0.4F ? Blocks.COBBLESTONE : r < 0.7F ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE).getDefaultState();
        }

        @Override
        public BlockState surfaceWall() {
            return Blocks.SPRUCE_FENCE.getDefaultState();
        }

        @Override
        public Block stairs() {
            return Blocks.COBBLESTONE_STAIRS;
        }
    }
}
