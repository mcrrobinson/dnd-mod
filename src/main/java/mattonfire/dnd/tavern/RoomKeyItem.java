package mattonfire.dnd.tavern;

import java.util.List;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import mattonfire.dnd.world.GameTime;

/**
 * A key to a rented tavern room, bought from an innkeeper. It remembers the inn (its name and
 * bounding box) and the in-game day it was bought, and it's good until the next dawn: while it's
 * valid, any bed inside that inn lets its holder sleep for a long rest at any time of day
 * ({@link TavernRooms}). Sleeping out the rest uses the key up.
 * <p>
 * The innkeeper stamps the key when it appears in the trade screen's output slot; an unstamped key
 * (from the creative tab, or a {@code /give}) opens nothing.
 */
public class RoomKeyItem extends Item {
    private static final String INN = "Inn";
    private static final String BOX = "InnBox";
    private static final String DAY = "Day";

    public RoomKeyItem(Settings settings) {
        super(settings);
    }

    /** Writes the inn and the day of purchase on the key. */
    public static void stamp(ItemStack stack, String inn, BlockBox box, long day) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(INN, inn);
        nbt.putIntArray(BOX, new int[]{box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(),
                box.getMaxZ()});
        nbt.putLong(DAY, day);
    }

    /** The inn the key opens, or null if it hasn't been stamped. */
    @Nullable
    public static BlockBox box(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (!stack.isOf(Tavern.ROOM_KEY) || nbt == null) {
            return null;
        }
        int[] b = nbt.getIntArray(BOX);
        return b.length == 6 ? new BlockBox(b[0], b[1], b[2], b[3], b[4], b[5]) : null;
    }

    /** The in-game day the key was bought, or -1 if it hasn't been stamped. */
    public static long day(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(DAY) ? nbt.getLong(DAY) : -1;
    }

    /** True if the key was bought today: it runs out at the next dawn. */
    public static boolean isValid(ItemStack stack, long today) {
        return box(stack) != null && day(stack) == today;
    }

    /** True if the key is valid today and the bed is inside its inn. */
    public static boolean opens(ItemStack stack, BlockPos bed, long today) {
        BlockBox box = box(stack);
        return box != null && day(stack) == today && box.contains(bed);
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(INN)) {
            return Text.translatable(this.getTranslationKey() + ".named", nbt.getString(INN));
        }
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (box(stack) == null) {
            tooltip.add(Text.translatable("item.dndclasses.room_key.blank").formatted(Formatting.GRAY));
            return;
        }
        long today = world == null ? day(stack) : GameTime.day(world);
        if (day(stack) != today) {
            tooltip.add(Text.translatable("item.dndclasses.room_key.expired").formatted(Formatting.RED));
            return;
        }
        tooltip.add(Text.translatable("item.dndclasses.room_key.valid").formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("item.dndclasses.room_key.how").formatted(Formatting.GRAY));
    }
}
