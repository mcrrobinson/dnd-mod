package mattonfire.dnd.magic;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * The magic state of one stack, kept in the sub-compound {@value #KEY} of its NBT (1.19.4 has no item
 * components). Every field is optional; a stack without the compound is identified, uncursed and +0.
 *
 * <ul>
 * <li>{@code tier}: overrides the registered tier (and gives vanilla +N gear one)</li>
 * <li>{@code identified}: false for random loot; the item shows "Unidentified &lt;type&gt;" and its magic is off</li>
 * <li>{@code curse}, {@code curseKnown}: the curse id and whether the holder knows (curses ticket)</li>
 * <li>{@code uuid}: this item's bond id; {@code attunedTo}: the bonded player (attunement ticket)</li>
 * <li>{@code plus}: the +1/+2/+3 of generic magic gear</li>
 * <li>{@code charges}: for wands and other charged items</li>
 * </ul>
 */
public final class MagicData {
    public static final String KEY = "dndclasses_magic";

    private MagicData() {
    }

    public static @Nullable NbtCompound get(ItemStack stack) {
        return stack.getSubNbt(KEY);
    }

    public static NbtCompound getOrCreate(ItemStack stack) {
        return stack.getOrCreateSubNbt(KEY);
    }

    public static boolean has(ItemStack stack) {
        return get(stack) != null;
    }

    // --- tier ---

    /** The tier stored on the stack, or null to use the registered one. */
    public static @Nullable MagicTier storedTier(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.contains("tier", NbtElement.STRING_TYPE) ? MagicTier.byId(nbt.getString("tier")) : null;
    }

    public static void setTier(ItemStack stack, MagicTier tier) {
        getOrCreate(stack).putString("tier", tier.id);
    }

    // --- identification ---

    public static boolean isIdentified(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt == null || !nbt.contains("identified") || nbt.getBoolean("identified");
    }

    public static void setIdentified(ItemStack stack, boolean identified) {
        getOrCreate(stack).putBoolean("identified", identified);
    }

    // --- curses (used by the curses ticket) ---

    /** The curse id, or "" for none. */
    public static String curse(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt == null ? "" : nbt.getString("curse");
    }

    public static void setCurse(ItemStack stack, String curse) {
        getOrCreate(stack).putString("curse", curse);
    }

    public static boolean isCurseKnown(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.getBoolean("curseKnown");
    }

    public static void setCurseKnown(ItemStack stack, boolean known) {
        getOrCreate(stack).putBoolean("curseKnown", known);
    }

    // --- bonds (used by the attunement ticket) ---

    public static @Nullable UUID uuid(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.containsUuid("uuid") ? nbt.getUuid("uuid") : null;
    }

    /** This item's bond id, created the first time it's asked for. */
    public static UUID ensureUuid(ItemStack stack) {
        UUID uuid = uuid(stack);
        if (uuid == null) {
            uuid = UUID.randomUUID();
            getOrCreate(stack).putUuid("uuid", uuid);
        }
        return uuid;
    }

    public static @Nullable UUID attunedTo(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.containsUuid("attunedTo") ? nbt.getUuid("attunedTo") : null;
    }

    public static void setAttunedTo(ItemStack stack, @Nullable UUID player) {
        if (player == null) {
            NbtCompound nbt = get(stack);
            if (nbt != null)
                nbt.remove("attunedTo");
        } else {
            getOrCreate(stack).putUuid("attunedTo", player);
        }
    }

    // --- +N and charges ---

    /** The stored +N, whether or not it's active. */
    public static int plus(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt == null ? 0 : nbt.getInt("plus");
    }

    public static void setPlus(ItemStack stack, int plus) {
        getOrCreate(stack).putInt("plus", plus);
    }

    public static int charges(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt == null ? 0 : nbt.getInt("charges");
    }

    public static void setCharges(ItemStack stack, int charges) {
        getOrCreate(stack).putInt("charges", charges);
    }

    /**
     * Whether the item's magic is asleep. Today that's while it's unidentified; the attunement ticket adds
     * the bond, class and anti-magic checks in {@code Attunement.isActive(player, stack)}.
     */
    public static boolean isDormant(ItemStack stack) {
        return !isIdentified(stack);
    }

    /** The +N that actually applies: 0 while dormant. */
    public static int activePlus(ItemStack stack) {
        return isDormant(stack) ? 0 : Math.max(0, plus(stack));
    }
}
