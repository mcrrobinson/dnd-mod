package mattonfire.dnd.world.gen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.GoblinBlocks;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.GoblinWarriorEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The whole goblin camp, built round its centre in world coordinates. From the middle out: a
 * campfire in a stone fire pit ringed by log seats, three hide or canvas tents with sloping roofs, ridge
 * poles, guy ropes and door flaps (one on each side but the gate's; the chief's, opposite the gate and the
 * biggest, has a porch, a plank floor, the War Order Board and the loot chest), junk piles and trophy poles in the corners, and a crude, gappy palisade of logs, war-marked
 * posts and rope-and-bone fence with a skull-topped gate. The clearing is levelled and
 * any trees in it cut down. Two wolves are chained to a Guardian Beast post in front of the chief's
 * tent: a Druid or a Bard calms them, anyone else fights them (the camp's one critical obstacle).
 *
 * Built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * random is drawn whether or not the block it's for is in this chunk.
 */
public class GoblinCampPiece extends StructurePiece {
    private static final Identifier CHIEF_CHEST = new Identifier(DnDClasses.MOD_ID, "chests/goblin_camp");
    private static final Identifier SUPPLIES = new Identifier(DnDClasses.MOD_ID, "chests/goblin_camp_supplies");

    /** Radius of the palisade. */
    private static final int RADIUS = 11;
    /** How far out the ground is levelled and cleared. */
    private static final int CLEAR = 14;
    /** How high above the floor the clearing is cut, trees and all. */
    private static final int HEADROOM = 16;
    /** How far down the floor gets built up over dips. */
    private static final int MAX_FOUNDATION = 8;
    /** Tents run from this far out from the fire (the chief's; the others this or one more)... */
    private static final int TENT_FRONT = 4;
    /** ...to their back wall, this far out. */
    private static final int TENT_BACK = 8;
    /** The old box tents' half-width: they drew a patch roll per block of a 5 x 7 tent. */
    private static final int TENT_HALF_WIDTH = 3;
    /** Chance of a Goblin Warlord leading the camp. */
    private static final float WARLORD_CHANCE = 0.3F;
    /** Wolves chained to the Guardian Beast post in front of the chief's tent. */
    private static final int GUARD_WOLVES = 2;

    /**
     * The old tent hides, still drawn by index so camps generated before the current tents match across chunk
     * borders: an even draw gives a Stretched Hide tent, an odd one Tent Canvas, and a Patchwork Hide patch draw
     * gives the walls patches of it.
     */
    private static final Block[] HIDES = {Blocks.BROWN_WOOL, GoblinBlocks.PATCHWORK_HIDE, GoblinBlocks.PATCHWORK_HIDE,
            GoblinBlocks.PATCHWORK_HIDE, Blocks.BLACK_WOOL};

    private final BlockPos center;
    private final long seed;

    public GoblinCampPiece(BlockPos center, long seed) {
        super(GoblinCampStructures.CAMP, 0, new BlockBox(center.getX() - CLEAR, center.getY(), center.getZ() - CLEAR,
                center.getX() + CLEAR, center.getY() + HEADROOM, center.getZ() + CLEAR));
        this.center = center;
        this.seed = seed;
    }

    public GoblinCampPiece(NbtCompound nbt) {
        super(GoblinCampStructures.CAMP, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.seed = nbt.getLong("Seed");
    }

    /** The camp's middle, at ground level; the fire is one block up. */
    public BlockPos getCenter() {
        return this.center;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Random random = Random.create(this.seed);
        Direction gate = Direction.fromHorizontal(random.nextInt(4));
        Direction chief = gate.getOpposite();

        // Level, clear and floor the ground, and put the palisade up round it.
        for (int dx = -CLEAR; dx <= CLEAR; dx++) {
            for (int dz = -CLEAR; dz <= CLEAR; dz++) {
                float r = random.nextFloat();
                float r2 = random.nextFloat();
                int extra = random.nextInt(2);
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > CLEAR + 0.5D) {
                    continue;
                }
                BlockPos pos = this.at(dx, 0, dz);
                for (int y = 1; y <= HEADROOM; y++) {
                    set(world, chunkBox, pos.up(y), Blocks.AIR.getDefaultState());
                }
                foundation(world, chunkBox, pos.down());
                set(world, chunkBox, pos, floor(d, r, onGateRoad(gate, dx, dz)));
                if (d > RADIUS - 0.5D && d <= RADIUS + 0.5D && !onGateRoad(gate, dx, dz) && r2 >= 0.1F) {
                    palisade(world, chunkBox, pos, r, r2, 2 + extra);
                }
            }
        }

        // Gate posts, taller than the rest and each topped with a skull.
        for (int side = -1; side <= 1; side += 2) {
            BlockPos post = this.at(gate, RADIUS, 2 * side, 0);
            for (int y = 1; y <= 3; y++) {
                Block log = y == 2 ? GoblinBlocks.GLYPH_DAUBED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
                set(world, chunkBox, post.up(y), PieceBlocks.pillar(log, Direction.Axis.Y));
            }
            set(world, chunkBox, post.up(4), skull(random));
        }

        // Fire pit and log seats.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float r = random.nextFloat();
                set(world, chunkBox, this.at(dx, 0, dz), (r < 0.4F ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState());
            }
        }
        set(world, chunkBox, this.at(0, 1, 0), Blocks.CAMPFIRE.getDefaultState());
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                boolean seat = random.nextFloat() < 0.75F;
                Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
                if (seat) {
                    set(world, chunkBox, this.at(2 * sx, 1, 2 * sz), PieceBlocks.pillar(Blocks.STRIPPED_SPRUCE_LOG, axis));
                }
            }
        }

        // Tents on three sides; the chief's, facing the gate, holds the plunder.
        for (int i = 0; i < 4; i++) {
            Direction side = Direction.fromHorizontal(i);
            int hide = random.nextInt(HIDES.length);
            int patch = random.nextInt(HIDES.length);
            if (side != gate) {
                this.tent(world, chunkBox, random, side, hide, patch, side == chief);
            }
        }

        // Something in each corner: trophy poles, woodpiles, bones, a crude workshop.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                this.corner(world, chunkBox, random, sx, sz);
            }
        }

        // And the goblins themselves.
        List<int[]> spots = new ArrayList<>();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                spots.add(new int[]{3 * sx, 3 * sz});
            }
        }
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (side != chief) {
                spots.add(new int[]{2 * side.getOffsetX(), 2 * side.getOffsetZ()});
            }
            if (side != gate && side != chief) {
                // Lounging in their tents.
                spots.add(new int[]{6 * side.getOffsetX(), 6 * side.getOffsetZ()});
            }
        }
        Collections.shuffle(spots, new java.util.Random(random.nextLong()));
        int warriors = 3 + random.nextInt(3);
        boolean warlord = random.nextFloat() < WARLORD_CHANCE;
        for (int i = 0; i < warriors && i < spots.size(); i++) {
            float yaw = random.nextFloat() * 360.0F;
            this.spawn(world, chunkBox, ModEntityTypes.GOBLIN_WARRIOR.create(world.toServerWorld()),
                    this.at(spots.get(i)[0], 1, spots.get(i)[1]), yaw);
        }
        if (warlord) {
            // In front of its own tent, looking out over the camp to the gate.
            this.spawn(world, chunkBox, ModEntityTypes.GOBLIN_WARLORD.create(world.toServerWorld()),
                    this.at(chief, 2, 0, 1), gate.asRotation());
        }

        // The chief's guard wolves, chained to a post beside the tent's ridge pole. Last, and drawing no randoms,
        // so camps generated before it was added still match across chunk borders.
        StructureObstacles.guardianPost(world, chunkBox, this.at(chief, TENT_FRONT - 1, 2, 1), EntityType.WOLF,
                List.of(this.at(chief, TENT_FRONT - 2, 2, 1), this.at(chief, TENT_FRONT - 1, 3, 1),
                        this.at(chief, TENT_FRONT - 2, 1, 1)),
                GUARD_WOLVES, Tier.EASY, true);

        // Banners, drawing no randoms either: the goblin skull on the gate posts, hung facing out, and the
        // Warlord's standard on the end of the chief's ridge pole when one leads the camp.
        ModBannerPatterns.Layer skull = ModBannerPatterns.GOBLIN_SKULL.in(DyeColor.LIME);
        for (int side = -1; side <= 1; side += 2) {
            BlockPos banner = this.at(gate, RADIUS + 1, 2 * side, 3);
            set(world, chunkBox, banner, Blocks.BLACK_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, gate));
            if (chunkBox.contains(banner)) {
                ModBannerPatterns.decorate(world, banner, skull);
            }
        }
        if (warlord) {
            BlockPos standard = this.at(chief, TENT_FRONT - 2, 0, CHIEF_TENT.ridge() + 1);
            set(world, chunkBox, standard, Blocks.BLACK_BANNER.getDefaultState()
                    .with(BannerBlock.ROTATION, RotationPropertyHelper.fromDirection(gate)));
            if (chunkBox.contains(standard)) {
                ModBannerPatterns.decorate(world, standard, new ModBannerPatterns.Layer("bo", DyeColor.RED), skull);
            }
        }
    }

    /** How a tent's roof is built across one column, {@code across} blocks from the ridge. */
    private enum Roof {
        /** A full block of cloth: a side wall. */
        WALL,
        /** Stairs, high side towards the ridge: the steep upper roof. */
        STAIRS,
        /** A top slab: where the cloth sags, half a block lower than a stair's step. */
        TOP_SLAB,
        /** A bottom slab: the eave, pulled out over the side wall. */
        BOTTOM_SLAB
    }

    /**
     * A tent's cross-section. The ridge pole is a log {@code ridge} blocks up, capped with a slab of cloth; the
     * roof falls away from it steeply (stairs) and then sags (slabs) out to a low side wall at {@code halfWidth}.
     *
     * @param columns for each column out from the ridge (1 to halfWidth), its roof pieces as {height, Roof}
     */
    private record TentShape(int halfWidth, int ridge, int[][][] columns) {
        /** The lowest roof piece of column {@code p} (0 is the ridge pole): everything below it is inside. */
        int lowest(int p) {
            if (p == 0) {
                return this.ridge;
            }
            int low = Integer.MAX_VALUE;
            for (int[] piece : this.columns[p - 1]) {
                low = Math.min(low, piece[0]);
            }
            return low;
        }
    }

    private static int[] piece(int y, Roof roof) {
        return new int[]{y, roof.ordinal()};
    }

    /** The common tent, 7 wide: a steep ridge sagging out to a 1-high wall. */
    private static final TentShape A_FRAME = new TentShape(3, 3, new int[][][]{
            {piece(3, Roof.STAIRS)},
            {piece(2, Roof.TOP_SLAB)},
            {piece(1, Roof.WALL), piece(2, Roof.BOTTOM_SLAB)}});
    /** A wall tent, 7 wide: 2-high walls under a steep roof, roomier inside. */
    private static final TentShape WALL_TENT = new TentShape(3, 4, new int[][][]{
            {piece(4, Roof.STAIRS)},
            {piece(3, Roof.STAIRS)},
            {piece(1, Roof.WALL), piece(2, Roof.WALL)}});
    /** The chief's tent, 9 wide: a tall ridge, then stairs, a sag and the eave. */
    private static final TentShape CHIEF_TENT = new TentShape(4, 4, new int[][][]{
            {piece(4, Roof.STAIRS)},
            {piece(3, Roof.STAIRS)},
            {piece(2, Roof.TOP_SLAB)},
            {piece(1, Roof.WALL), piece(2, Roof.BOTTOM_SLAB)}});

    /**
     * A tent with its back wall {@link #TENT_BACK} blocks out on {@code side} and its door flaps open towards the
     * fire: a ridge pole poking out of both ends, held up at the front by a pole, guy ropes to stakes either side,
     * and patches of Patchwork Hide on the walls. The chief's is the biggest, of hide, with a porch over its door.
     * The others are an A-frame or a wall tent, of hide or canvas, 4 or 5 deep.
     */
    private void tent(StructureWorldAccess world, BlockBox chunkBox, Random random, Direction side, int hideIndex,
                      int patchIndex, boolean chief) {
        // The randoms the old box tents drew, in the same order, so the rest of a camp still matches camps
        // generated before these tents. The tent's own shape comes from its own random.
        float[] patchRolls = new float[(TENT_BACK - TENT_FRONT + 1) * (2 * TENT_HALF_WIDTH + 1)];
        for (int i = 0; i < patchRolls.length; i++) {
            patchRolls[i] = random.nextFloat();
        }
        long lootSeed = random.nextLong();
        boolean barrel = random.nextFloat() < 0.6F;
        Random own = Random.create(this.seed ^ (0x7E47L * (side.getHorizontal() + 1)));

        BlockFamily cloth = chief || hideIndex % 2 == 0 ? ModBlockFamilies.STRETCHED_HIDE : ModBlockFamilies.TENT_CANVAS;
        // Patchwork patches only on the tents whose old patch roll was Patchwork Hide.
        float patchChance = HIDES[patchIndex] == GoblinBlocks.PATCHWORK_HIDE ? 0.14F : 0.0F;
        TentShape shape = chief ? CHIEF_TENT : own.nextBoolean() ? A_FRAME : WALL_TENT;
        int front = chief ? TENT_FRONT : TENT_FRONT + own.nextInt(2);
        int back = TENT_BACK;
        int w = shape.halfWidth();
        Direction left = side.rotateYCounterclockwise();
        Direction in = side.getOpposite();
        // The chief's porch: the roof carried one block further out over the door.
        int roofFront = chief ? front - 1 : front;

        for (int r = roofFront; r <= back; r++) {
            boolean porch = r < front;
            boolean gable = r == front || r == back;
            // Ridge pole and its cap of cloth.
            set(world, chunkBox, this.at(side, r, 0, shape.ridge()), PieceBlocks.pillar(Blocks.SPRUCE_LOG, side.getAxis()));
            set(world, chunkBox, this.at(side, r, 0, shape.ridge() + 1), PieceBlocks.slab(cloth, false));
            for (int p = -w; p <= w; p++) {
                int a = Math.abs(p);
                float roll = patchRolls[Math.floorMod(r * 7 + p, patchRolls.length)];
                if (a > 0) {
                    for (int[] piece : shape.columns()[a - 1]) {
                        Roof roof = Roof.values()[piece[1]];
                        if (porch && roof == Roof.WALL) {
                            continue;
                        }
                        BlockState state = switch (roof) {
                            case WALL -> (roll < patchChance ? GoblinBlocks.PATCHWORK_HIDE : cloth.base()).getDefaultState();
                            // High side towards the ridge.
                            case STAIRS -> PieceBlocks.stairs(cloth, p > 0 ? left.getOpposite() : left, false);
                            case TOP_SLAB -> gable ? cloth.base().getDefaultState() : PieceBlocks.slab(cloth, true);
                            case BOTTOM_SLAB -> PieceBlocks.slab(cloth, false);
                        };
                        set(world, chunkBox, this.at(side, r, p, piece[0]), state);
                    }
                }
                // The end walls, below the roof; the front one has a doorway 3 wide and 2 high.
                for (int y = 1; y < shape.lowest(a); y++) {
                    boolean doorway = r == front && a <= 1 && y <= 2;
                    BlockState fill = gable && !doorway
                            ? (roll < patchChance ? GoblinBlocks.PATCHWORK_HIDE : cloth.base()).getDefaultState()
                            : Blocks.AIR.getDefaultState();
                    set(world, chunkBox, this.at(side, r, p, y), fill);
                }
                if (!porch && r < back && a < w) {
                    // The chief has a floor of planks; everyone else sleeps on the dirt.
                    set(world, chunkBox, this.at(side, r, p, 0), (chief ? ModBlockFamilies.SCRAP_LASHED_PLANKS.base()
                            : Blocks.COARSE_DIRT).getDefaultState());
                }
            }
        }
        // The ridge pole pokes out of the back, and is held up at the front by an upright pole lashed to its end
        // (the chief's stands under the porch, and the ridge pokes out past it for the Warlord's standard).
        set(world, chunkBox, this.at(side, back + 1, 0, shape.ridge()), PieceBlocks.pillar(Blocks.SPRUCE_LOG, side.getAxis()));
        int poleAt = chief ? roofFront : front - 1;
        for (int y = 1; y <= (chief ? shape.ridge() - 1 : shape.ridge()); y++) {
            set(world, chunkBox, this.at(side, poleAt, 0, y), fence(Blocks.SPRUCE_FENCE, side, y == shape.ridge()));
        }
        if (chief) {
            set(world, chunkBox, this.at(side, roofFront - 1, 0, shape.ridge()), PieceBlocks.pillar(Blocks.SPRUCE_LOG, side.getAxis()));
            // Poles under the porch's eaves.
            set(world, chunkBox, this.at(side, roofFront, w, 1), Blocks.SPRUCE_FENCE.getDefaultState());
            set(world, chunkBox, this.at(side, roofFront, -w, 1), Blocks.SPRUCE_FENCE.getDefaultState());
        }
        // Door flaps, tied back against the sides of the doorway.
        for (int y = 1; y <= 2; y++) {
            set(world, chunkBox, this.at(side, front, 1, y), PieceBlocks.trapdoor(cloth.trapdoor(), left.getOpposite(), false)
                    .with(TrapdoorBlock.OPEN, true));
            set(world, chunkBox, this.at(side, front, -1, y), PieceBlocks.trapdoor(cloth.trapdoor(), left, false)
                    .with(TrapdoorBlock.OPEN, true));
        }
        // Guy ropes: stakes either side at the front and back corners, and behind the back wall.
        for (int s = -1; s <= 1; s += 2) {
            Direction toTent = s > 0 ? left.getOpposite() : left;
            stake(world, chunkBox, this.at(side, front, s * (w + 1), 1), toTent);
            stake(world, chunkBox, this.at(side, back, s * (w + 1), 1), toTent);
            stake(world, chunkBox, this.at(side, back + 1, s, 1), in);
        }

        // A grubby bedroll at the back, its head to the back wall.
        set(world, chunkBox, this.at(side, TENT_BACK - 1, -2, 1), AdventuringBlocks.BROWN_BEDROLL.head(side));
        set(world, chunkBox, this.at(side, TENT_BACK - 2, -2, 1), AdventuringBlocks.BROWN_BEDROLL.foot(side));
        if (chief) {
            for (int r = TENT_FRONT + 1; r <= TENT_BACK - 2; r++) {
                for (int p = -1; p <= 1; p++) {
                    set(world, chunkBox, this.at(side, r, p, 1), Blocks.RED_CARPET.getDefaultState());
                }
            }
            BlockPos chest = this.at(side, TENT_BACK - 1, 0, 1);
            set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, in));
            setLoot(world, chunkBox, chest, CHIEF_CHEST, lootSeed);
            set(world, chunkBox, this.at(side, TENT_BACK - 1, 2, 1), Blocks.GOLD_BLOCK.getDefaultState());
            // The Warlord's war orders, hung on the back wall over the chest.
            set(world, chunkBox, this.at(side, TENT_BACK - 1, 0, 2),
                    GoblinBlocks.WAR_ORDER_BOARD.getDefaultState().with(HorizontalFacingBlock.FACING, in));
            // A lantern hung from the ridge pole.
            set(world, chunkBox, this.at(side, TENT_FRONT + 1, 0, shape.ridge() - 1), PieceBlocks.lantern(true));
        } else if (barrel) {
            BlockPos at = this.at(side, TENT_BACK - 1, 2, 1);
            set(world, chunkBox, at, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP));
            setLoot(world, chunkBox, at, SUPPLIES, lootSeed);
        }
    }

    /** A fence post, joined to the block on its {@code dir} side if {@code joined}. */
    private static BlockState fence(Block fence, Direction dir, boolean joined) {
        return fence.getDefaultState().with(connection(dir), joined);
    }

    private static BooleanProperty connection(Direction dir) {
        return switch (dir) {
            case NORTH -> HorizontalConnectingBlock.NORTH;
            case SOUTH -> HorizontalConnectingBlock.SOUTH;
            case EAST -> HorizontalConnectingBlock.EAST;
            default -> HorizontalConnectingBlock.WEST;
        };
    }

    /** A Tent Stake with its guy rope running {@code toTent}. */
    private static void stake(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, Direction toTent) {
        set(world, chunkBox, pos, GoblinBlocks.TENT_STAKE.getDefaultState().with(connection(toTent), true));
    }

    /** The corner between the tents at ({@code sx}, {@code sz}) = (+-1, +-1). */
    private void corner(StructureWorldAccess world, BlockBox chunkBox, Random random, int sx, int sz) {
        int kind = random.nextInt(4);
        float r = random.nextFloat();
        long lootSeed = random.nextLong();
        BlockState skull = skull(random);
        BlockState junk = GoblinBlocks.GOBLIN_JUNK_PILE.getDefaultState();
        if (kind != 2) {
            // A heap of junk at the corner's edge (the bone heap has its own).
            set(world, chunkBox, this.at(7 * sx, 1, 5 * sz), junk);
        }
        switch (kind) {
            case 0 -> {
                // Trophy poles hung with skulls.
                for (int y = 1; y <= 2; y++) {
                    set(world, chunkBox, this.at(7 * sx, y, 6 * sz), Blocks.SPRUCE_FENCE.getDefaultState());
                    set(world, chunkBox, this.at(6 * sx, y, 7 * sz), Blocks.SPRUCE_FENCE.getDefaultState());
                }
                set(world, chunkBox, this.at(7 * sx, 3, 6 * sz), skull);
                set(world, chunkBox, this.at(6 * sx, 3, 7 * sz), r < 0.5F ? skull : Blocks.CARVED_PUMPKIN.getDefaultState());
            }
            case 1 -> {
                // A woodpile.
                for (int a = 6; a <= 7; a++) {
                    set(world, chunkBox, this.at(a * sx, 1, 6 * sz), PieceBlocks.pillar(Blocks.SPRUCE_LOG, Direction.Axis.Z));
                    set(world, chunkBox, this.at(a * sx, 1, 7 * sz), PieceBlocks.pillar(Blocks.OAK_LOG, Direction.Axis.Z));
                }
                set(world, chunkBox, this.at(6 * sx, 2, 6 * sz), PieceBlocks.pillar(Blocks.SPRUCE_LOG, Direction.Axis.Z));
                set(world, chunkBox, this.at(7 * sx, 2, 7 * sz), Blocks.TORCH.getDefaultState());
            }
            case 2 -> {
                // A hoard of junk and bones round a skull.
                set(world, chunkBox, this.at(7 * sx, 1, 7 * sz), Blocks.BONE_BLOCK.getDefaultState());
                set(world, chunkBox, this.at(6 * sx, 1, 7 * sz), junk);
                set(world, chunkBox, this.at(7 * sx, 2, 7 * sz), junk);
                set(world, chunkBox, this.at(7 * sx, 1, 6 * sz), skull);
                set(world, chunkBox, this.at(6 * sx, 1, 6 * sz), Blocks.COBWEB.getDefaultState());
            }
            default -> {
                // A crude workshop: a crafting table, a cauldron and a barrel of supplies.
                set(world, chunkBox, this.at(7 * sx, 1, 6 * sz), Blocks.CRAFTING_TABLE.getDefaultState());
                set(world, chunkBox, this.at(6 * sx, 1, 7 * sz), (r < 0.5F ? Blocks.WATER_CAULDRON : Blocks.CAULDRON).getDefaultState());
                BlockPos barrel = this.at(7 * sx, 1, 7 * sz);
                set(world, chunkBox, barrel, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP));
                setLoot(world, chunkBox, barrel, SUPPLIES, lootSeed);
            }
        }
    }

    private void spawn(StructureWorldAccess world, BlockBox chunkBox, HostileEntity goblin, BlockPos at, float yaw) {
        if (goblin == null || !chunkBox.contains(at)) {
            return;
        }
        goblin.refreshPositionAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, yaw, 0.0F);
        goblin.setHeadYaw(yaw);
        goblin.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null, null);
        if (goblin instanceof GoblinWarlordEntity warlord) {
            warlord.setGuardPos(this.center.up());
        } else if (goblin instanceof GoblinWarriorEntity warrior) {
            warrior.setCampCenter(this.center.up());
        }
        goblin.setPersistent();
        world.spawnEntityAndPassengers(goblin);
    }

    /**
     * A crude palisade post: logs (some spruce ones daubed with a war mark), often with a rope-and-bone
     * stake or a torch on top, or a stretch of rope-and-bone fence.
     */
    private static void palisade(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, float r, float r2, int height) {
        BlockState fence = GoblinBlocks.ROPE_AND_BONE_FENCE.getDefaultState();
        if (r2 < 0.25F) {
            for (int y = 1; y <= 2; y++) {
                set(world, chunkBox, pos.up(y), fence);
            }
            return;
        }
        Block log = r2 < 0.4F ? Blocks.OAK_LOG : r2 < 0.55F ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
        boolean daubed = r2 >= 0.8F;
        for (int y = 1; y <= height; y++) {
            Block block = daubed && y == 2 ? GoblinBlocks.GLYPH_DAUBED_SPRUCE_LOG : log;
            set(world, chunkBox, pos.up(y), PieceBlocks.pillar(block, Direction.Axis.Y));
        }
        if (r < 0.06F) {
            set(world, chunkBox, pos.up(height + 1), Blocks.TORCH.getDefaultState());
        } else if (r < 0.35F) {
            set(world, chunkBox, pos.up(height + 1), fence);
        }
    }

    /** The trampled floor at distance d from the fire. */
    private static BlockState floor(double d, float r, boolean road) {
        if (d > RADIUS + 0.5D) {
            return (road && r < 0.7F ? Blocks.DIRT_PATH : r < 0.12F ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).getDefaultState();
        }
        if (road && r < 0.6F) {
            return Blocks.DIRT_PATH.getDefaultState();
        }
        return (r < 0.35F ? Blocks.COARSE_DIRT : r < 0.45F ? Blocks.DIRT_PATH : r < 0.53F ? Blocks.PODZOL
                : r < 0.57F ? Blocks.GRAVEL : Blocks.GRASS_BLOCK).getDefaultState();
    }

    /** The three-wide track running out of the gate. */
    private static boolean onGateRoad(Direction gate, int dx, int dz) {
        int along = dx * gate.getOffsetX() + dz * gate.getOffsetZ();
        int across = Math.abs(dx * gate.getOffsetZ() - dz * gate.getOffsetX());
        return along > TENT_BACK && across <= 1;
    }

    private static BlockState skull(Random random) {
        Block skull = random.nextFloat() < 0.15F ? Blocks.ZOMBIE_HEAD : Blocks.SKELETON_SKULL;
        return skull.getDefaultState().with(SkullBlock.ROTATION, random.nextInt(16));
    }

    private BlockPos at(int dx, int dy, int dz) {
        return this.center.add(dx, dy, dz);
    }

    /** {@code out} blocks from the fire towards {@code side}, {@code across} to its left, {@code up} above the floor. */
    private BlockPos at(Direction side, int out, int across, int up) {
        Direction left = side.rotateYCounterclockwise();
        return this.center.add(side.getOffsetX() * out + left.getOffsetX() * across, up,
                side.getOffsetZ() * out + left.getOffsetZ() * across);
    }

    private static void setLoot(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, Identifier table, long seed) {
        if (chunkBox.contains(pos) && world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(table, seed);
        }
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
            // The palisade's rope-and-bone fence joins up with its neighbours once the chunk is done.
            if (state.isOf(GoblinBlocks.ROPE_AND_BONE_FENCE)) {
                world.getChunk(pos).markBlockForPostProcessing(pos);
            }
        }
    }

    /** Fills dirt down from {@code top} wherever there's air, plants or liquid, until it meets the ground. */
    private static void foundation(StructureWorldAccess world, BlockBox chunkBox, BlockPos top) {
        if (!chunkBox.contains(top)) {
            return;
        }
        BlockPos.Mutable pos = top.mutableCopy();
        for (int i = 0; i < MAX_FOUNDATION && pos.getY() > world.getBottomY(); i++) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                return;
            }
            world.setBlockState(pos, Blocks.DIRT.getDefaultState(), PieceBlocks.UPDATE);
            pos.move(Direction.DOWN);
        }
    }
}
