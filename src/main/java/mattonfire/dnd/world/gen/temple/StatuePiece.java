package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * One of the courtyard's three dragon statues, carved from its ancestry's Dragonstone (scales in relief) with
 * Dragon Belly plates down its chest and neck, glowing Dragonstone Eyes, ivory Dragon Horns for its horns, claws,
 * fangs, spines and tail tip, and wing membranes of its colour's Dragonscale Tiles. Each sits up on its haunches
 * on a plinth of its colour's tiles with gilded corners, forelegs planted, tail curled round behind, its neck
 * curving up and forward so its open-jawed head looks out past the plinth towards the stair, wings raised and
 * spread. The shapes are voxel drawings in {@link StatueModels}.
 * <ul>
 * <li>Ember, in the middle of the courtyard: the great one, 18 blocks tall on a two-tier plinth, its wings
 * spread 13 blocks wide over the paths either side.</li>
 * <li>Frost and Storm, the guardians at the front corners: 15 blocks tall, wings 9 wide.</li>
 * </ul>
 * Each statue is its own piece, so "is this block part of a statue" is a box test (damaging one is a temple
 * grudge, see {@code TempleLaw}).
 */
public class StatuePiece extends TemplePiece {
    /** The plinths: the guardians' and the great Ember statue's. */
    static final int GUARDIAN_WIDTH = 5;
    static final int GUARDIAN_DEPTH = 7;
    static final int GREAT_WIDTH = 7;
    static final int GREAT_DEPTH = 9;
    /** How far back in its box a plinth starts: the head looks out over the gap. */
    static final int SETBACK = 2;
    private static final Model GUARDIAN = new Model(StatueModels.GUARDIAN);
    private static final Model GREAT = new Model(StatueModels.GREAT);
    private final DragonAncestry kind;

    public StatuePiece(BlockPos front, Direction facing, DragonAncestry kind, long seed) {
        super(DragonTempleStructures.STATUE, boxFrom(front, facing, model(kind).width, model(kind).height(), model(kind).depth),
                facing, seed);
        this.kind = kind;
    }

    public StatuePiece(NbtCompound nbt) {
        super(DragonTempleStructures.STATUE, nbt);
        DragonAncestry kind;
        try {
            kind = DragonAncestry.fromValue(nbt.getInt("Kind"));
        } catch (IllegalArgumentException e) {
            kind = DragonAncestry.EMBER;
        }
        this.kind = kind;
    }

    /** Whether {@code kind}'s statue is the great one in the middle. */
    static boolean great(DragonAncestry kind) {
        return kind == DragonAncestry.EMBER;
    }

    private static Model model(DragonAncestry kind) {
        return great(kind) ? GREAT : GUARDIAN;
    }

    static int plinthWidth(DragonAncestry kind) {
        return great(kind) ? GREAT_WIDTH : GUARDIAN_WIDTH;
    }

    static int plinthDepth(DragonAncestry kind) {
        return great(kind) ? GREAT_DEPTH : GUARDIAN_DEPTH;
    }

    public DragonAncestry kind() {
        return this.kind;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Kind", this.kind.getValue());
    }

    @Override
    protected void build(Builder b) {
        Model model = model(this.kind);
        for (int y = 0; y < model.height(); y++) {
            String[] rows = model.layers.get(y);
            for (int z = 0; z < rows.length; z++) {
                for (int x = 0; x < rows[z].length(); x++) {
                    BlockState state = this.block(rows[z].charAt(x));
                    if (state != null) {
                        b.set(x, y, z, state);
                    }
                }
            }
        }
    }

    /** The block a model character stands for, in this statue's colours, or null for air. */
    private BlockState block(char c) {
        BlockFamily stone = ModBlockFamilies.dragonstone(this.kind);
        BlockFamily belly = ModBlockFamilies.dragonBelly(this.kind);
        BlockFamily tiles = ModBlockFamilies.dragonscale(this.kind);
        Block horn = ModTempleBlocks.DRAGON_HORN;
        return switch (c) {
            case '#' -> stone.base().getDefaultState();
            case 'b' -> belly.base().getDefaultState();
            case '=' -> slab(stone, false);
            case '-' -> slab(stone, true);
            case '|' -> wall(stone);
            case 'N' -> stairs(stone, BACK, false);
            case 'S' -> stairs(stone, FRONT, false);
            case 'E' -> stairs(stone, Direction.EAST, false);
            case 'W' -> stairs(stone, Direction.WEST, false);
            case 'n' -> stairs(stone, BACK, true);
            case 's' -> stairs(stone, FRONT, true);
            case 'e' -> stairs(stone, Direction.EAST, true);
            case 'w' -> stairs(stone, Direction.WEST, true);
            case 'P' -> stairs(belly, BACK, false);
            case 'p' -> stairs(belly, BACK, true);
            case 't' -> tiles.base().getDefaultState();
            case 'T' -> stairs(tiles, FRONT, false);
            case 'O' -> facing(ModTempleBlocks.statueEye(this.kind), FRONT);
            case 'H' -> facing(horn, Direction.UP);
            case 'h' -> facing(horn, BACK);
            case 'k' -> facing(horn, FRONT);
            case 'v' -> facing(horn, Direction.DOWN);
            case 'L' -> facing(horn, Direction.WEST);
            case 'R' -> facing(horn, Direction.EAST);
            case 'x' -> tiles.base().getDefaultState();
            case 'g' -> Blocks.GILDED_BLACKSTONE.getDefaultState();
            case 'X' -> Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            case 'c' -> Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
            default -> null;
        };
    }

    /** A {@link StatueModels} drawing, parsed: its layers from the bottom up, each a row per z of a char per x. */
    private static final class Model {
        final List<String[]> layers = new ArrayList<>();
        final int width;
        final int depth;

        Model(String drawing) {
            List<String> rows = new ArrayList<>();
            for (String line : drawing.split("\n")) {
                String row = line.strip();
                if (row.equals("~")) {
                    this.layers.add(rows.toArray(new String[0]));
                    rows.clear();
                } else if (!row.isEmpty()) {
                    rows.add(row);
                }
            }
            this.layers.add(rows.toArray(new String[0]));
            this.width = this.layers.get(0)[0].length();
            this.depth = this.layers.get(0).length;
        }

        int height() {
            return this.layers.size();
        }
    }
}
