package mattonfire.dnd.world.gen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.GoblinWarriorEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The whole goblin camp, built round its centre in world coordinates. From the middle out: a
 * campfire in a stone fire pit ringed by log seats, three hide tents (one on each side but the
 * gate's; the chief's, opposite the gate, holds the loot chest), junk heaps and trophy poles in
 * the corners, and a crude, gappy palisade with a skull-topped gate. The clearing is levelled and
 * any trees in it cut down.
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
    /** Tents run from this far out from the fire... */
    private static final int TENT_FRONT = 4;
    /** ...to this far, and are this many blocks either side of their middle. */
    private static final int TENT_BACK = 8;
    private static final int TENT_HALF_WIDTH = 3;
    /** Chance of a Goblin Warlord leading the camp. */
    private static final float WARLORD_CHANCE = 0.3F;
    /** Goblins keep within this distance of the fire when they aren't fighting. */
    private static final int ROAM = 10;

    private static final Block[] HIDES = {Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.GRAY_WOOL, Blocks.LIME_WOOL, Blocks.BLACK_WOOL};

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
                set(world, chunkBox, post.up(y), log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
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
                    set(world, chunkBox, this.at(2 * sx, 1, 2 * sz), log(Blocks.STRIPPED_SPRUCE_LOG, axis));
                }
            }
        }

        // Tents on three sides; the chief's, facing the gate, holds the plunder.
        for (int i = 0; i < 4; i++) {
            Direction side = Direction.fromHorizontal(i);
            Block hide = HIDES[random.nextInt(HIDES.length)];
            Block patch = HIDES[random.nextInt(HIDES.length)];
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
                spots.add(new int[]{4 * sx, 4 * sz});
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
    }

    /** A hide tent, open towards the fire, with its back {@link #TENT_BACK} blocks out on {@code side}. */
    private void tent(StructureWorldAccess world, BlockBox chunkBox, Random random, Direction side, Block hide, Block patch, boolean chief) {
        for (int r = TENT_FRONT; r <= TENT_BACK; r++) {
            for (int p = -TENT_HALF_WIDTH; p <= TENT_HALF_WIDTH; p++) {
                // An A-frame: the roof is 4 high on the ridge, sloping down to 1 at the edges.
                int roof = TENT_HALF_WIDTH + 1 - Math.abs(p);
                boolean patched = random.nextFloat() < 0.12F;
                BlockState shell = (patched ? patch : hide).getDefaultState();
                set(world, chunkBox, this.at(side, r, p, roof), shell);
                for (int y = 1; y < roof; y++) {
                    set(world, chunkBox, this.at(side, r, p, y), r == TENT_BACK ? hide.getDefaultState() : Blocks.AIR.getDefaultState());
                }
                if (r < TENT_BACK && Math.abs(p) < TENT_HALF_WIDTH) {
                    set(world, chunkBox, this.at(side, r, p, 0), Blocks.COARSE_DIRT.getDefaultState());
                }
            }
        }
        // Ridge pole poking out of the front.
        set(world, chunkBox, this.at(side, TENT_FRONT - 1, 0, 1), Blocks.SPRUCE_FENCE.getDefaultState());
        set(world, chunkBox, this.at(side, TENT_FRONT - 1, 0, 2), Blocks.SPRUCE_FENCE.getDefaultState());
        set(world, chunkBox, this.at(side, TENT_FRONT - 1, 0, 3), Blocks.SPRUCE_FENCE.getDefaultState());

        Direction in = side.getOpposite();
        long lootSeed = random.nextLong();
        boolean barrel = random.nextFloat() < 0.6F;
        // A bed of straw at the back.
        set(world, chunkBox, this.at(side, TENT_BACK - 1, -2, 1), Blocks.HAY_BLOCK.getDefaultState());
        set(world, chunkBox, this.at(side, TENT_BACK - 2, -2, 1), Blocks.HAY_BLOCK.getDefaultState());
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
        } else if (barrel) {
            BlockPos at = this.at(side, TENT_BACK - 1, 2, 1);
            set(world, chunkBox, at, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP));
            setLoot(world, chunkBox, at, SUPPLIES, lootSeed);
        }
    }

    /** The corner between the tents at ({@code sx}, {@code sz}) = (+-1, +-1). */
    private void corner(StructureWorldAccess world, BlockBox chunkBox, Random random, int sx, int sz) {
        int kind = random.nextInt(4);
        float r = random.nextFloat();
        long lootSeed = random.nextLong();
        BlockState skull = skull(random);
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
                    set(world, chunkBox, this.at(a * sx, 1, 6 * sz), log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
                    set(world, chunkBox, this.at(a * sx, 1, 7 * sz), log(Blocks.OAK_LOG, Direction.Axis.Z));
                }
                set(world, chunkBox, this.at(6 * sx, 2, 6 * sz), log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
                set(world, chunkBox, this.at(7 * sx, 2, 7 * sz), Blocks.TORCH.getDefaultState());
            }
            case 2 -> {
                // A heap of bones round a skull.
                set(world, chunkBox, this.at(7 * sx, 1, 7 * sz), Blocks.BONE_BLOCK.getDefaultState());
                set(world, chunkBox, this.at(6 * sx, 1, 7 * sz), Blocks.BONE_BLOCK.getDefaultState());
                set(world, chunkBox, this.at(7 * sx, 2, 7 * sz), Blocks.BONE_BLOCK.getDefaultState());
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
        } else if (goblin instanceof GoblinWarriorEntity) {
            goblin.setPositionTarget(this.center.up(), ROAM);
        }
        goblin.setPersistent();
        world.spawnEntityAndPassengers(goblin);
    }

    /** A crude palisade post: logs, often with a stake or a torch on top, or a stretch of fence. */
    private static void palisade(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, float r, float r2, int height) {
        if (r2 < 0.25F) {
            for (int y = 1; y <= 2; y++) {
                set(world, chunkBox, pos.up(y), Blocks.SPRUCE_FENCE.getDefaultState());
            }
            return;
        }
        Block log = r2 < 0.4F ? Blocks.OAK_LOG : r2 < 0.55F ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
        for (int y = 1; y <= height; y++) {
            set(world, chunkBox, pos.up(y), log(log, Direction.Axis.Y));
        }
        if (r < 0.06F) {
            set(world, chunkBox, pos.up(height + 1), Blocks.TORCH.getDefaultState());
        } else if (r < 0.35F) {
            set(world, chunkBox, pos.up(height + 1), Blocks.SPRUCE_FENCE.getDefaultState());
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

    private static BlockState log(Block log, Direction.Axis axis) {
        return log.getDefaultState().with(PillarBlock.AXIS, axis);
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
            world.setBlockState(pos, state, 2);
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
            world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
            pos.move(Direction.DOWN);
        }
    }
}
