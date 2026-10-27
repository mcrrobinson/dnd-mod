package mattonfire.dnd.world.gen;

import mattonfire.dnd.classes.Blocks.PetrifiedRemainsBlock;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Creatures turned to stone, for the lairs of monsters that petrify: the Beholder's cavern and the Underdark's
 * Basilisk Dens. A statue is an armour stand in stone-grey leather, arms raised against the gaze, facing what caught
 * it; {@link #remains} is the kneeling Petrified Remains block.
 */
public final class PetrifiedStatues {
    /** The leather's dye: the grey of weathered stone. */
    public static final int STONE = 0x8A8A86;

    /** Who the statue was: their gear. */
    public enum Victim {
        /** A surface adventurer: sword, axe, bow or shield, a helmet half the time. */
        ADVENTURER,
        /** A drow: a sword or a hand crossbow, hooded (no helmet). */
        DROW,
        /** A duergar: always helmed, with a pick, an axe or a shield. */
        DUERGAR
    }

    private PetrifiedStatues() {
    }

    /**
     * A petrified adventurer as the Beholder's cavern places them: {@code weapon} 0-3 picks a sword, an axe, a bow or
     * a shield.
     */
    public static void adventurer(StructureWorldAccess world, BlockPos at, double faceX, double faceZ, int weapon,
                                  boolean helmet, float lean) {
        Item held = switch (weapon) {
            case 0 -> Items.STONE_SWORD;
            case 1 -> Items.STONE_AXE;
            case 2 -> Items.BOW;
            default -> Items.SHIELD;
        };
        ItemStack stack = new ItemStack(held);
        statue(world, at, faceX, faceZ, lean, helmet, weapon == 3 ? ItemStack.EMPTY : stack,
                weapon == 3 ? stack : ItemStack.EMPTY);
    }

    /**
     * A petrified {@code victim} at {@code at} if it's in {@code chunkBox}, its gear drawn from {@code random} (drawn
     * either way, so every chunk of a piece draws the same).
     */
    public static void statue(StructureWorldAccess world, BlockBox chunkBox, BlockPos at, double faceX, double faceZ,
                              Victim victim, Random random) {
        float lean = (random.nextFloat() - 0.5F) * 30.0F;
        int roll = random.nextInt(4);
        boolean helmet = random.nextBoolean();
        if (!chunkBox.contains(at)) {
            return;
        }
        switch (victim) {
            case ADVENTURER -> adventurer(world, at, faceX, faceZ, roll, helmet, lean);
            case DROW -> statue(world, at, faceX, faceZ, lean, false,
                    new ItemStack(roll < 2 ? Items.IRON_SWORD : Items.CROSSBOW), ItemStack.EMPTY);
            case DUERGAR -> statue(world, at, faceX, faceZ, lean, true,
                    new ItemStack(roll == 0 ? Items.IRON_PICKAXE : Items.IRON_AXE),
                    roll >= 2 ? new ItemStack(Items.SHIELD) : ItemStack.EMPTY);
        }
    }

    /**
     * An armour stand at {@code at} in stone-grey leather (a helmet if {@code helmet}), arms raised, leaning
     * {@code lean} degrees, turned to face the column {@code faceX}, {@code faceZ}, holding {@code main} and
     * {@code off}.
     */
    public static void statue(StructureWorldAccess world, BlockPos at, double faceX, double faceZ, float lean,
                              boolean helmet, ItemStack main, ItemStack off) {
        ArmorStandEntity stand = new ArmorStandEntity(world.toServerWorld(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        float yaw = (float) (MathHelper.atan2(faceZ - at.getZ(), faceX - at.getX()) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
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
        if (!main.isEmpty()) {
            stand.equipStack(EquipmentSlot.MAINHAND, main);
        }
        if (!off.isEmpty()) {
            stand.equipStack(EquipmentSlot.OFFHAND, off);
        }
        world.spawnEntity(stand);
    }

    /** Petrified Remains, kneeling, turned to face {@code facing}. */
    public static BlockState remains(Direction facing) {
        return RuinBlocks.PETRIFIED_REMAINS.getDefaultState().with(PetrifiedRemainsBlock.FACING, facing);
    }

    /** Petrified Remains at {@code from} turned to face the column {@code faceX}, {@code faceZ}. */
    public static BlockState remainsFacing(BlockPos from, int faceX, int faceZ) {
        int dx = faceX - from.getX();
        int dz = faceZ - from.getZ();
        Direction facing = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        return remains(facing);
    }

    /** {@code item} dyed stone grey, if it's dyeable. */
    public static ItemStack stone(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof DyeableItem dyeable) {
            dyeable.setColor(stack, STONE);
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
}
