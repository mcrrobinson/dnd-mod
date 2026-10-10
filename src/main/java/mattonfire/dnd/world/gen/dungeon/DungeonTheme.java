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
 * Only the Crypt is finished; the Goblin Warren and Dwarven Ruin are placeholders that reuse the
 * Crypt palette until their own tickets give them one (no structure JSON uses them yet).
 */
public enum DungeonTheme implements StringIdentifiable {
    CRYPT("crypt", Palette.CRYPT, 1, 3, 4, 8),
    // TODO(dungeons ticket 7): warren palette, cave-mouth entrance, goblin pools
    GOBLIN_WARREN("goblin_warren", Palette.CRYPT, 1, 4, 4, 8),
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
    }
}
