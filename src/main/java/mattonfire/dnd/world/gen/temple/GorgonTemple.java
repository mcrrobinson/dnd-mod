package mattonfire.dnd.world.gen.temple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.classes.Blocks.PetrifiedRemainsBlock;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import mattonfire.dnd.entity.boss.StructureBosses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * The Gorgon's Temple: one in {@value #ONE_IN} desert pyramids and jungle temples (chosen from the world seed and
 * the temple's position, so the same temple always is or isn't one) is the Medusa's lair. The temple mixins
 * ({@code DesertTempleGeneratorMixin}, {@code JungleTempleGeneratorMixin}) call {@link #decorate} as each chunk of
 * the temple generates; whatever lies in that chunk is placed: the Medusa (a Peaceful marker on Peaceful,
 * {@link StructureBosses#placeMedusa}), 3-6 statues of earlier visitors (indefinitely Petrified mobs facing her)
 * and a few Petrified Remains.
 */
public final class GorgonTemple {
    public static final int ONE_IN = 3;
    public static final int MIN_STATUES = 3;
    public static final int MAX_STATUES = 6;

    /** Turns a piece-local position into a world position (the piece's {@code offsetPos}). */
    @FunctionalInterface
    public interface Local {
        BlockPos at(int x, int y, int z);
    }

    /**
     * Where things go in one kind of temple, in piece-local coordinates.
     *
     * @param name     for the log ("desert pyramid")
     * @param boss     where the Medusa stands
     * @param faces    the point she faces (the entrance)
     * @param statues  statue spots, more than {@link #MAX_STATUES}; a seeded few are used
     * @param remains  Petrified Remains spots; a seeded two or three are used
     * @param husks    the statues can be husks (desert)
     */
    public record Layout(String name, int[] boss, int[] faces, int[][] statues, int[][] remains, boolean husks) {
    }

    public static final Layout DESERT_PYRAMID = new Layout("desert pyramid",
            new int[] { 10, 1, 10 }, new int[] { 10, 1, 0 },
            new int[][] { { 10, 1, 6 }, { 6, 1, 10 }, { 14, 1, 10 }, { 10, 1, 14 }, { 7, 1, 7 }, { 13, 1, 7 },
                    { 7, 1, 13 }, { 13, 1, 13 } },
            new int[][] { { 9, 1, 13 }, { 11, 1, 7 }, { 6, 1, 8 }, { 14, 1, 12 } },
            true);

    /** The upper floor (y 4-5), the jungle temple's biggest room; her spot is at the far end from the stairs. */
    public static final Layout JUNGLE_TEMPLE = new Layout("jungle temple",
            new int[] { 5, 4, 11 }, new int[] { 5, 4, 2 },
            new int[][] { { 3, 4, 3 }, { 8, 4, 3 }, { 3, 4, 7 }, { 8, 4, 7 }, { 3, 4, 11 }, { 8, 4, 11 },
                    { 3, 4, 9 }, { 8, 4, 9 } },
            new int[][] { { 6, 4, 3 }, { 2, 4, 5 }, { 9, 4, 10 } },
            false);

    private GorgonTemple() {
    }

    /** Whether the temple whose bounding box is {@code box} is a Gorgon's Temple in this world. */
    public static boolean isGorgonTemple(long seed, BlockBox box) {
        return seeded(seed, box, 0x6F726E).nextInt(ONE_IN) == 0;
    }

    private static Random seeded(long seed, BlockBox box, long salt) {
        return Random.create(seed ^ box.getMinX() * 341873128712L ^ box.getMinZ() * 132897987541L ^ salt);
    }

    /** Called at the end of the temple piece's {@code generate} for each chunk it overlaps. */
    public static void decorate(StructureWorldAccess world, BlockBox chunkBox, BlockBox pieceBox, Local local,
            Layout layout) {
        BlockPos boss = local.at(layout.boss()[0], layout.boss()[1], layout.boss()[2]).toImmutable();
        boolean gorgon = isGorgonTemple(world.getSeed(), pieceBox);
        if (chunkBox.contains(boss)) {
            DnDClasses.LOGGER.info("[GorgonTemple] {} at {}: {}", layout.name(), boss.toShortString(),
                    gorgon ? "Gorgon's Temple, Medusa placed" : "an ordinary temple");
        }
        if (!gorgon) {
            return;
        }
        BlockPos faces = local.at(layout.faces()[0], layout.faces()[1], layout.faces()[2]);
        if (chunkBox.contains(boss)) {
            StructureBosses.placeMedusa(world, boss, yawToward(boss, faces));
        }

        Random random = seeded(world.getSeed(), pieceBox, 0x5747A7);
        List<int[]> spots = new ArrayList<>(List.of(layout.statues()));
        Collections.shuffle(spots, new java.util.Random(random.nextLong()));
        int count = MIN_STATUES + random.nextInt(MAX_STATUES - MIN_STATUES + 1);
        for (int i = 0; i < Math.min(count, spots.size()); i++) {
            int[] s = spots.get(i);
            BlockPos pos = local.at(s[0], s[1], s[2]).toImmutable();
            int kind = random.nextInt(layout.husks() ? 4 : 3);
            if (chunkBox.contains(pos) && open(world, pos)) {
                placeStatue(world, pos, yawToward(pos, boss), kind, layout.husks());
            }
        }

        List<int[]> remains = new ArrayList<>(List.of(layout.remains()));
        Collections.shuffle(remains, new java.util.Random(random.nextLong()));
        int remainsCount = 2 + random.nextInt(2);
        for (int i = 0; i < Math.min(remainsCount, remains.size()); i++) {
            int[] r = remains.get(i);
            BlockPos pos = local.at(r[0], r[1], r[2]).toImmutable();
            if (chunkBox.contains(pos) && world.getBlockState(pos).isAir()) {
                Direction facing = Direction.fromRotation(yawToward(pos, boss));
                world.setBlockState(pos, RuinBlocks.PETRIFIED_REMAINS.getDefaultState()
                        .with(PetrifiedRemainsBlock.FACING, facing), 2);
            }
        }
    }

    private static boolean open(StructureWorldAccess world, BlockPos pos) {
        return world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir();
    }

    /** Minecraft yaw for looking from {@code from} toward {@code to}. */
    static float yawToward(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return MathHelper.wrapDegrees((float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
    }

    /**
     * An earlier visitor turned to stone: a zombie, a pillager, a zombie in iron armour or (desert) a husk, facing
     * her, indefinitely Petrified (persistent and silent: a statue; a pickaxe shatters it).
     */
    private static void placeStatue(StructureWorldAccess world, BlockPos pos, float yaw, int kind, boolean husks) {
        EntityType<? extends MobEntity> type = switch (kind) {
            case 1 -> EntityType.PILLAGER;
            case 3 -> husks ? EntityType.HUSK : EntityType.ZOMBIE;
            default -> EntityType.ZOMBIE;
        };
        MobEntity mob = type.create(world.toServerWorld());
        if (mob == null) {
            return;
        }
        mob.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        mob.setHeadYaw(yaw);
        mob.setBodyYaw(yaw);
        // No initialize(): a zombie's could make a baby on a chicken. Gear by hand.
        if (kind == 1) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (kind == 2) {
            mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            mob.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            mob.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        mob.addCommandTag("dndclasses.gorgon_statue");
        mob.setPersistent();
        Conditions.preset(mob, Condition.PETRIFIED);
        world.spawnEntity(mob);
    }
}
