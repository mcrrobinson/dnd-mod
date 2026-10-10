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
        setAttunedTo(stack, player, "");
    }

    /** Bonds the stack to a player, remembering their name for "Attuned to &lt;player&gt;"; null clears both. */
    public static void setAttunedTo(ItemStack stack, @Nullable UUID player, String playerName) {
        if (player == null) {
            NbtCompound nbt = get(stack);
            if (nbt != null) {
                nbt.remove("attunedTo");
                nbt.remove("attunedName");
            }
        } else {
            getOrCreate(stack).putUuid("attunedTo", player);
            getOrCreate(stack).putString("attunedName", playerName);
        }
    }

    /** The bonded player's name when they attuned, or "". */
    public static String attunedName(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt == null ? "" : nbt.getString("attunedName");
    }

    // --- Blessing of the Forge (Forge Domain Cleric): +1 until the holder's next long rest ---

    public static boolean isForgeBlessed(ItemStack stack) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.getBoolean("forgeBlessing");
    }

    public static void setForgeBlessed(ItemStack stack, boolean blessed) {
        if (blessed) {
            getOrCreate(stack).putBoolean("forgeBlessing", true);
        } else {
            NbtCompound nbt = get(stack);
            if (nbt != null)
                nbt.remove("forgeBlessing");
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

    /** The charges, or {@code full} for a stack that has never stored any (a fresh item starts full). */
    public static int charges(ItemStack stack, int full) {
        NbtCompound nbt = get(stack);
        return nbt != null && nbt.contains("charges", NbtElement.NUMBER_TYPE) ? nbt.getInt("charges") : full;
    }

    public static void setCharges(ItemStack stack, int charges) {
        getOrCreate(stack).putInt("charges", charges);
    }

    /**
     * Whether the item's magic is asleep on its own, whoever holds it: while it's unidentified. The checks
     * that need the holder (bond, class, anti-magic) are in {@link Attunement#isActive}.
     */
    public static boolean isDormant(ItemStack stack) {
        return !isIdentified(stack);
    }

    /** The +N shown in the name: the stored +N plus a Forge blessing's +1, at most +3. */
    public static int displayPlus(ItemStack stack) {
        int plus = Math.max(0, plus(stack)) + (isForgeBlessed(stack) ? 1 : 0);
        return Math.min(MagicGear.MAX_PLUS, plus);
    }

    /** The +N that actually applies: {@link #displayPlus}, or 0 while dormant. */
    public static int activePlus(ItemStack stack) {
        return isDormant(stack) ? 0 : displayPlus(stack);
    }
}
