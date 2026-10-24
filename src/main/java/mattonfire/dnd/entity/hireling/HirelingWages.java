package mattonfire.dnd.entity.hireling;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Hireling prices and gold. Everything is counted in gold nuggets: an ingot is {@value #INGOT}, a block
 * {@value #BLOCK}. Wages follow the hobbits' reputation like the innkeeper's prices (rounded up):
 * Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%.
 */
public final class HirelingWages {
    public static final int INGOT = 9;
    public static final int BLOCK = 81;
    /** The purse holds at most this many days' wages. */
    public static final int MAX_PURSE_DAYS = 7;

    private HirelingWages() {
    }

    /** {@code base} nuggets for a customer of {@code tier}. */
    public static int adjust(int base, ReputationTier tier) {
        double factor = switch (tier) {
            case UNFRIENDLY -> 1.5D;
            case FRIENDLY -> 0.9D;
            case HONORED -> 0.75D;
            case EXALTED -> 0.6D;
            default -> 1.0D;
        };
        return Math.max(1, (int) Math.ceil(base * factor - 1.0E-6D));
    }

    public static ReputationTier hobbitTier(PlayerEntity player) {
        return TierEffects.tierWith(player, TierEffects.HOBBITS);
    }

    /** A day's wage for this role, for this player. */
    public static int wage(HirelingRole role, PlayerEntity player) {
        return adjust(role.wage, hobbitTier(player));
    }

    /** The hire fee: {@link HirelingRole#FEE_DAYS} days' wages. */
    public static int fee(HirelingRole role, PlayerEntity player) {
        return wage(role, player) * HirelingRole.FEE_DAYS;
    }

    /** "54 gold nuggets (6 ingots)" style: the ingot count only when it comes out even. */
    public static String describe(int nuggets) {
        String text = nuggets + " gold nugget" + (nuggets == 1 ? "" : "s");
        if (nuggets >= INGOT && nuggets % INGOT == 0) {
            int ingots = nuggets / INGOT;
            text += " (" + ingots + " ingot" + (ingots == 1 ? "" : "s") + ")";
        }
        return text;
    }

    public static int valueOf(ItemStack stack) {
        return valueOf(stack.getItem()) * stack.getCount();
    }

    public static int valueOf(Item item) {
        if (item == Items.GOLD_NUGGET) {
            return 1;
        }
        if (item == Items.GOLD_INGOT) {
            return INGOT;
        }
        if (item == Items.GOLD_BLOCK) {
            return BLOCK;
        }
        return 0;
    }

    public static boolean isGold(ItemStack stack) {
        return valueOf(stack.getItem()) > 0;
    }

    /** All the gold in the player's inventory, in nuggets. */
    public static int count(PlayerEntity player) {
        if (player.getAbilities().creativeMode) {
            return Integer.MAX_VALUE / 2;
        }
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            total += valueOf(inventory.getStack(i));
        }
        return total;
    }

    /**
     * Takes {@code nuggets} worth of gold: nuggets first, then ingots, then blocks, with change given back in
     * nuggets. Returns false (and takes nothing) if the player hasn't enough. Creative players pay nothing.
     */
    public static boolean take(PlayerEntity player, int nuggets) {
        if (player.getAbilities().creativeMode) {
            return true;
        }
        if (count(player) < nuggets) {
            return false;
        }
        int owed = nuggets;
        owed = takeItems(player, Items.GOLD_NUGGET, 1, owed);
        owed = takeItems(player, Items.GOLD_INGOT, INGOT, owed);
        owed = takeItems(player, Items.GOLD_BLOCK, BLOCK, owed);
        if (owed < 0) {
            giveNuggets(player, -owed);
        }
        player.getInventory().markDirty();
        return true;
    }

    /** Takes items of one kind while anything is owed; returns what's still owed (negative: change due). */
    private static int takeItems(PlayerEntity player, Item item, int value, int owed) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size() && owed > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isOf(item)) {
                continue;
            }
            int needed = (owed + value - 1) / value;
            int taken = Math.min(needed, stack.getCount());
            stack.decrement(taken);
            owed -= taken * value;
        }
        return owed;
    }

    public static void giveNuggets(PlayerEntity player, int nuggets) {
        while (nuggets > 0) {
            int count = Math.min(64, nuggets);
            player.getInventory().offerOrDrop(new ItemStack(Items.GOLD_NUGGET, count));
            nuggets -= count;
        }
    }
}
