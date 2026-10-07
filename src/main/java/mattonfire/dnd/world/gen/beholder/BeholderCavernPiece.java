package mattonfire.dnd.world.gen.beholder;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.BeholderEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.Thickness;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The Beholder's cavern: a rough dome hollowed out of the deepslate round its centre (the middle of
 * the floor). The floor is a great eye laid in stone, with a glowing froglight iris; petrified
 * adventurers stand where its gaze caught them, a hoard is heaped against the far wall, and the
 * Beholder floats over it all. A tunnel leads east to the stair (BeholderShaftPiece).
 *
 * <p>The walls are a two-block shell, so caves, water and lava around it can't break in. Built once
 * per chunk it overlaps: per-block choices come from a position hash and the rest from its own seed.
 */
public class BeholderCavernPiece extends StructurePiece {
    private static final Identifier HOARD = new Identifier(DnDClasses.MOD_ID, "chests/beholder_lair");

    /** Horizontal radius of the dome at the floor, and its height above the floor. */
    public static final int RADIUS = 16;
    private static final int HEIGHT = 12;
    /** Thickness of the wall shell, in blocks. */
    private static final int SHELL = 2;
    private static final int STATUES = 5;
    private static final int DRIPSTONES = 24;
    /** The Beholder hovers this far above the floor. */
    private static final int HOVER = 5;

    private final BlockPos center;
    private final long seed;

    public BeholderCavernPiece(BlockPos center, long seed) {
        super(BeholderLairStructures.CAVERN, 0, new BlockBox(center.getX() - RADIUS - SHELL - 1, center.getY() - 3,
                center.getZ() - RADIUS - SHELL - 1, center.getX() + RADIUS + SHELL + 1, center.getY() + HEIGHT + SHELL + 1,
                center.getZ() + RADIUS + SHELL + 1));
        this.center = center;
        this.seed = seed;
    }

    public BeholderCavernPiece(NbtCompound nbt) {
        super(BeholderLairStructures.CAVERN, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.seed = nbt.getLong("Seed");
    }

    /** Where the Beholder hovers when it's not fighting. */
    public BlockPos getHome() {
        return this.center.up(HOVER);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putLong("Seed", this.seed);
    }

    /**
     * How far (dx, dy, dz) from the floor centre is through the dome: below 1 is inside. Below the
     * floor it's a cylinder, so the floor reaches right out to the walls.
     */
    private double shape(int dx, int dy, int dz) {
        double horizontal = (dx * dx + dz * dz) / (double) (RADIUS * RADIUS);
        double vertical = dy > 0 ? (dy * dy) / (double) (HEIGHT * HEIGHT) : 0.0;
        double rough = (BeholderLairStructures.noise(this.seed, dx >> 1, dy >> 1, dz >> 1) - 0.5) * 0.14;
        return Math.sqrt(horizontal + vertical) - rough;
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int cx = this.center.getX();
        int y0 = this.center.getY();
        int cz = this.center.getZ();
        double innerWall = 1.0 + 1.0 / RADIUS;
        double outerWall = 1.0 + (double) SHELL / RADIUS;

        BlockBox box = this.getBoundingBox();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = Math.max(box.getMinX(), chunkBox.getMinX()); x <= Math.min(box.getMaxX(), chunkBox.getMaxX()); x++) {
            for (int z = Math.max(box.getMinZ(), chunkBox.getMinZ()); z <= Math.min(box.getMaxZ(), chunkBox.getMaxZ()); z++) {
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    int dx = x - cx;
                    int dy = y - y0;
                    int dz = z - cz;
                    pos.set(x, y, z);
                    double s = this.shape(dx, dy, dz);
                    float n = BeholderLairStructures.noise(this.seed, x, y, z);
                    if (dy > 0 && s < 1.0) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    } else if (dy == 0 && s < 1.0) {
                        world.setBlockState(pos, floor(dx, dz, n), 2);
                    } else if (dy >= 0 && s < innerWall) {
                        world.setBlockState(pos, wall(dy, n), 2);
                    } else if (s < outerWall) {
                        // The rest of the shell, and under the floor: only fill gaps (caves, water, lava)
                        BlockState state = world.getBlockState(pos);
                        if (state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable()) {
                            world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        this.tunnel(world, chunkBox, cx, y0, cz);

        // Everything below draws from the seed in the same order in every chunk.
        Random random = Random.create(this.seed);
        for (int i = 0; i < DRIPSTONES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(random.nextDouble()) * (RADIUS - 3);
            int length = 1 + random.nextInt(3);
            this.dripstone(world, chunkBox, cx + (int) Math.round(Math.cos(angle) * r), cz + (int) Math.round(Math.sin(angle) * r), length);
        }

        int hx = cx - RADIUS + 4;

        // Adventurers turned to stone, all facing the middle, shields up against the gaze.
        double turn = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < STATUES; i++) {
            double angle = turn + Math.PI * 2.0 * i / STATUES + (random.nextDouble() - 0.5) * 0.6;
            double r = 7.5 + random.nextDouble() * 4.0;
            int sx = cx + (int) Math.round(Math.cos(angle) * r);
            int sz = cz + (int) Math.round(Math.sin(angle) * r);
            int weapon = random.nextInt(4);
            boolean helmet = random.nextBoolean();
            float lean = (random.nextFloat() - 0.5F) * 30.0F;
            BlockPos at = new BlockPos(sx, y0 + 1, sz);
            if (chunkBox.contains(at) && Math.abs(sx - hx) + Math.abs(sz - cz) > 5) {
                this.statue(world, at, cx, cz, weapon, helmet, lean);
            }
        }

        // The hoard, against the west wall (the tunnel is east).
        long lootSeed = random.nextLong();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                float r = random.nextFloat();
                if (dx * dx + dz * dz > 8 || r > 0.55F || (dx == 0 && dz == 0)) {
                    continue;
                }
                Block heap = r < 0.08F ? Blocks.GOLD_BLOCK : r < 0.2F ? Blocks.RAW_GOLD_BLOCK : r < 0.3F ? Blocks.BONE_BLOCK
                        : r < 0.38F ? Blocks.AMETHYST_BLOCK : r < 0.45F ? Blocks.COBWEB : Blocks.SKELETON_SKULL;
                set(world, chunkBox, new BlockPos(hx + dx, y0 + 1, cz + dz), heap.getDefaultState());
            }
        }
        BlockPos chest = new BlockPos(hx, y0 + 1, cz);
        set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST));
        if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(HOARD, lootSeed);
        }

        // And the eye tyrant itself.
        BlockPos home = this.getHome();
        if (chunkBox.contains(home)) {
            BeholderEntity beholder = ModEntityTypes.BEHOLDER.create(world.toServerWorld());
            if (beholder != null) {
                beholder.refreshPositionAndAngles(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                beholder.initialize(world, world.getLocalDifficulty(home), SpawnReason.STRUCTURE, null, null);
                beholder.setHome(home);
                beholder.setPersistent();
                world.spawnEntityAndPassengers(beholder);
            }
        }
    }

    /** The floor: a great eye of calcite and blackstone round a glowing iris, in cracked deepslate tiles. */
    private static BlockState floor(int dx, int dz, float n) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.6) {
            return Blocks.OBSIDIAN.getDefaultState();
        }
        if (d < 4.2) {
            return (n < 0.85F ? Blocks.VERDANT_FROGLIGHT : Blocks.MOSS_BLOCK).getDefaultState();
        }
        double almond = (dx / 10.0) * (dx / 10.0) + (dz / 5.5) * (dz / 5.5);
        if (almond < 1.0) {
            return (n < 0.07F ? Blocks.RED_TERRACOTTA : Blocks.CALCITE).getDefaultState();
        }
        if ((dx / 11.0) * (dx / 11.0) + (dz / 6.5) * (dz / 6.5) < 1.0) {
            return Blocks.POLISHED_BLACKSTONE.getDefaultState();
        }
        return (n < 0.35F ? Blocks.POLISHED_DEEPSLATE : n < 0.65F ? Blocks.DEEPSLATE_TILES
                : n < 0.85F ? Blocks.CRACKED_DEEPSLATE_TILES : Blocks.TUFF).getDefaultState();
    }

    /** The inside of the dome: dark rock veined with crying obsidian and amethyst, the odd shroomlight up high. */
    private static BlockState wall(int dy, float n) {
        if (dy > HEIGHT / 2 && n < 0.02F) {
            return Blocks.SHROOMLIGHT.getDefaultState();
        }
        if (n < 0.05F) {
            return Blocks.CRYING_OBSIDIAN.getDefaultState();
        }
        if (n < 0.08F) {
            return Blocks.AMETHYST_BLOCK.getDefaultState();
        }
        return (n < 0.35F ? Blocks.DEEPSLATE : n < 0.6F ? Blocks.TUFF : n < 0.8F ? Blocks.COBBLED_DEEPSLATE
                : Blocks.SMOOTH_BASALT).getDefaultState();
    }

    /** A three-wide tunnel east from the cavern wall to the stair, with soul lanterns at its mouth. */
    private void tunnel(StructureWorldAccess world, BlockBox chunkBox, int cx, int y0, int cz) {
        int from = cx + RADIUS - 4;
        int to = cx + BeholderShaftPiece.OFFSET - 2;
        for (int x = from; x <= to; x++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    BlockPos pos = new BlockPos(x, y0 + dy, cz + dz);
                    boolean inside = Math.abs(dz) <= 1 && dy >= 1 && dy <= 3;
                    if (inside) {
                        set(world, chunkBox, pos, Blocks.AIR.getDefaultState());
                    } else if (dy == 0 && Math.abs(dz) <= 1) {
                        set(world, chunkBox, pos, Blocks.DEEPSLATE_TILES.getDefaultState());
                    } else if (this.shape(x - cx, dy, dz) >= 1.0) {
                        // Walls and roof, only where they're not open cavern
                        set(world, chunkBox, pos, Blocks.DEEPSLATE_BRICKS.getDefaultState());
                    }
                }
            }
        }
        for (int dz : new int[] {-1, 1}) {
            set(world, chunkBox, new BlockPos(to, y0 + 3, cz + dz),
                    Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        }
    }

    /** Hangs a dripstone of {@code length} from the dome's ceiling over (x, z). */
    private void dripstone(StructureWorldAccess world, BlockBox chunkBox, int x, int z, int length) {
        int cx = this.center.getX();
        int y0 = this.center.getY();
        int cz = this.center.getZ();
        int top = -1;
        for (int dy = HEIGHT + 1; dy > 2; dy--) {
            if (this.shape(x - cx, dy, z - cz) < 1.0) {
                top = dy;
                break;
            }
        }
        if (top < 0) {
            return;
        }
        for (int i = 0; i < length && top - i > 3; i++) {
            Thickness thickness = i == length - 1 ? Thickness.TIP : i == 0 && length > 2 ? Thickness.BASE : Thickness.FRUSTUM;
            set(world, chunkBox, new BlockPos(x, y0 + top - i, z), Blocks.POINTED_DRIPSTONE.getDefaultState()
                    .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.DOWN)
                    .with(PointedDripstoneBlock.THICKNESS, thickness));
        }
    }

    /** A petrified adventurer: an armour stand in stone-grey leather, arms raised, facing the middle. */
    private void statue(StructureWorldAccess world, BlockPos at, int cx, int cz, int weapon, boolean helmet, float lean) {
        ArmorStandEntity stand = new ArmorStandEntity(world.toServerWorld(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        float yaw = (float) (MathHelper.atan2(cz - at.getZ(), cx - at.getX()) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        stand.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
        stand.setHeadYaw(yaw);
        stand.setBodyYaw(yaw);

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("ShowArms", true);
        nbt.putBoolean("NoBasePlate", true);
        NbtCompound pose = new NbtCompound();
        pose.put("Head", rotation(-20.0F, lean, 0.0F));
        pose.put("Body", rotation(0.0F, lean * 0.3F, 0.0F));
        pose.put("RightArm", rotation(-100.0F, -10.0F, 0.0F));
        pose.put("LeftArm", rotation(-110.0F, 25.0F, 0.0F));
        pose.put("RightLeg", rotation(-12.0F, 0.0F, 0.0F));
        pose.put("LeftLeg", rotation(10.0F, 0.0F, 0.0F));
        nbt.put("Pose", pose);
        stand.readCustomDataFromNbt(nbt);

        stand.equipStack(EquipmentSlot.CHEST, stone(Items.LEATHER_CHESTPLATE));
        stand.equipStack(EquipmentSlot.LEGS, stone(Items.LEATHER_LEGGINGS));
        stand.equipStack(EquipmentSlot.FEET, stone(Items.LEATHER_BOOTS));
        if (helmet) {
            stand.equipStack(EquipmentSlot.HEAD, stone(Items.LEATHER_HELMET));
        }
        Item held = switch (weapon) {
            case 0 -> Items.STONE_SWORD;
            case 1 -> Items.STONE_AXE;
            case 2 -> Items.BOW;
            default -> Items.SHIELD;
        };
        stand.equipStack(weapon == 3 ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, new ItemStack(held));
        world.spawnEntity(stand);
    }

    private static ItemStack stone(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof DyeableItem dyeable) {
            dyeable.setColor(stack, 0x8A8A86);
        }
        return stack;
    }

    private static NbtList rotation(float x, float y, float z) {
        NbtList list = new NbtList();
        list.add(NbtFloat.of(x));
        list.add(NbtFloat.of(y));
        list.add(NbtFloat.of(z));
        return list;
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, 2);
        }
    }
}
