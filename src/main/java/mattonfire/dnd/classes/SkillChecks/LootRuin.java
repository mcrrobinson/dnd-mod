package mattonfire.dnd.classes.SkillChecks;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

/**
 * Forcing a sealed container open ruins some of what's inside: a smashed locked chest ({@link Lockpicking}) or
 * a broken Alchemical Seal. Each stack is lost with chance {@link #RUIN_CHANCE}.
 */
public final class LootRuin {
    /** Chance that each stack is ruined when its container is forced open. */
    public static final float RUIN_CHANCE = 0.4f;

    private LootRuin() {
    }

    /** Rolls the container's loot (if it hasn't been) for {@code player}, then ruins some of it. Returns how many stacks were lost. */
    public static int forceOpen(LootableContainerBlockEntity container, PlayerEntity player) {
        container.checkLootInteraction(player);
        return ruin(container, player.getRandom());
    }

    /** Empties each non-empty slot with chance {@link #RUIN_CHANCE}. Returns how many stacks were lost. */
    public static int ruin(Inventory inventory, Random random) {
        int ruined = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && random.nextFloat() < RUIN_CHANCE) {
                inventory.setStack(i, ItemStack.EMPTY);
                ruined++;
            }
        }
        if (ruined > 0) {
            inventory.markDirty();
        }
        return ruined;
    }
}
