package mattonfire.dnd.world.underdark.drow;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Drow Outpost's barracks, facing the shrine (local +b): a low dome of Drowstone Bricks, {@value #RADIUS} blocks
 * round and 4 high inside at the middle, a Spidersilk Curtain in its door. Inside: four pale grey beds, spidersilk
 * carpet, a Faerie Fire Lantern hung from the crown, and a weapon rack of two armour stands holding iron swords. A mage
 * keeps to it.
 */
public class DrowBarracksPiece extends UnderdarkPiece {
    /** The dome's outer radius. */
    public static final int RADIUS = 5;
    /** The dome is squashed this much in height. */
    private static final double SQUASH = 1.25D;

    public DrowBarracksPiece(BlockPos center, Direction front, long seed) {
        super(DrowOutposts.BARRACKS, center, front, seed, -RADIUS - 1, -1, -RADIUS - 1, RADIUS + 1, RADIUS, RADIUS + 1);
    }

    public DrowBarracksPiece(NbtCompound nbt) {
        super(DrowOutposts.BARRACKS, nbt);
    }

    /** Where you stand inside, in the world. */
    public BlockPos inside() {
        return this.at(0, 0, 2);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        for (int a = -RADIUS - 1; a <= RADIUS + 1; a++) {
            for (int c = -RADIUS - 1; c <= RADIUS + 1; c++) {
                double flat = Math.sqrt(a * a + c * c);
                for (int y = -1; y <= RADIUS; y++) {
                    double d = Math.sqrt(a * a + c * c + (y * SQUASH) * (y * SQUASH));
                    if (y == -1) {
                        if (flat <= RADIUS + 0.5D) {
                            b.set(a, y, c, flat <= RADIUS - 0.6D ? bricks : polished);
                        }
                    } else if (d <= RADIUS - 0.6D) {
                        b.set(a, y, c, Blocks.AIR.getDefaultState());
                    } else if (d <= RADIUS + 0.5D) {
                        b.set(a, y, c, y == 1 ? polished : bricks);
                    }
                }
            }
        }
        // The door, with a curtain.
        for (int c = RADIUS - 2; c <= RADIUS + 1; c++) {
            b.clear(0, 0, c);
            b.clear(0, 1, c);
        }
        b.set(0, 0, RADIUS, UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());
        b.set(0, 1, RADIUS, UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());
        // Carpet, beds, lantern.
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                b.set(a, 0, c, UnderdarkBlocks.SPIDERSILK_CARPET);
            }
        }
        bed(b, -2, -2, Direction.NORTH);
        bed(b, 0, -2, Direction.NORTH);
        bed(b, 2, -2, Direction.NORTH);
        bed(b, -3, 1, Direction.WEST);
        b.hangingLantern(0, 3, 0, 1, UnderdarkBlocks.FAERIE_LANTERN);
        // The weapon rack.
        rack(b, 3, 0, 1);
        rack(b, 3, 0, -1);
        b.resident(0, 0, 1, Drow.DROW_MAGE, 5);
    }

    /** A pale grey bed with its foot at a/c and its head towards {@code head} (local). */
    private static void bed(Builder b, int a, int c, Direction head) {
        BlockState state = Blocks.LIGHT_GRAY_BED.getDefaultState().with(BedBlock.FACING, head);
        b.set(a, 0, c, state.with(BedBlock.PART, BedPart.FOOT));
        b.set(a + head.getOffsetX(), 0, c + head.getOffsetZ(), state.with(BedBlock.PART, BedPart.HEAD));
    }

    /** An armour stand with arms, an iron sword in hand and a black leather tunic, facing into the room. */
    private static void rack(Builder b, int a, int y, int c) {
        BlockPos pos = b.pos(a, y, c);
        if (!b.chunkBox.contains(pos)) {
            return;
        }
        ArmorStandEntity stand = new ArmorStandEntity(b.world.toServerWorld(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        NbtCompound arms = new NbtCompound();
        arms.putBoolean("ShowArms", true);
        stand.readCustomDataFromNbt(arms);
        stand.setYaw(b.dir(Direction.WEST).asRotation());
        stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ItemStack tunic = new ItemStack(Items.LEATHER_CHESTPLATE);
        ((DyeableItem) tunic.getItem()).setColor(tunic, 0x1E1A24);
        stand.equipStack(EquipmentSlot.CHEST, tunic);
        b.world.spawnEntity(stand);
    }
}
