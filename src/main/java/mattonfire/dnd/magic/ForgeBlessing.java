package mattonfire.dnd.magic;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The Forge Domain Cleric's subclass feature, Blessing of the Forge: once per long rest, at an Attunement
 * Table's Magic Items tab, make the held weapon or a worn armor piece +1 ({@link MagicData#isForgeBlessed},
 * counted by {@link MagicData#activePlus}). A long rest ends the blessing on every item the resting player
 * carries and gives the Cleric the blessing back.
 */
public final class ForgeBlessing {
    public static final String FORGE_SUBCLASS = "cleric.forge";
    private static final String USED_KEY = "dndForgeBlessingUsed";

    private ForgeBlessing() {
    }

    public static boolean isForgeCleric(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.CLERIC
                && Progression.current(player).hasSubclass(FORGE_SUBCLASS);
    }

    /** 0 not a Forge Cleric, 1 ready, 2 used until the next long rest (for {@link AttunementSnapshot}). */
    public static int state(PlayerEntity player) {
        if (!isForgeCleric(player))
            return 0;
        return ((IEntityDataSaver) player).getPersistentData().getBoolean(USED_KEY) ? 2 : 1;
    }

    /** Whether the stack in inventory slot {@code slot} can take the blessing: a held weapon or worn armor. */
    public static boolean canBless(PlayerEntity player, int slot, ItemStack stack) {
        if (stack.isEmpty() || MagicData.isForgeBlessed(stack) || MagicData.plus(stack) >= MagicGear.MAX_PLUS)
            return false;
        return switch (MagicGear.gearOf(stack.getItem())) {
            case MELEE, RANGED -> slot == player.getInventory().selectedSlot || slot == PlayerInventory.OFF_HAND_SLOT;
            case ARMOR -> slot >= PlayerInventory.MAIN_SIZE && slot < PlayerInventory.MAIN_SIZE + 4;
            case NONE -> false;
        };
    }

    public static boolean bless(ServerPlayerEntity player, int slot) {
        if (!isForgeCleric(player))
            return false;
        if (!Progression.atAttunementTable(player)) {
            player.sendMessage(Text.literal("The Blessing of the Forge is given at an Attunement Table.")
                    .formatted(Formatting.RED), false);
            return false;
        }
        if (state(player) != 1) {
            player.sendMessage(Text.literal("You've used the Blessing of the Forge. It returns after a long rest.")
                    .formatted(Formatting.RED), false);
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.size())
            return false;
        ItemStack stack = inventory.getStack(slot);
        if (!canBless(player, slot, stack)) {
            player.sendMessage(Text.literal("Bless a weapon in your hand or a piece of armor you wear.")
                    .formatted(Formatting.RED), false);
            return false;
        }
        MagicData.setForgeBlessed(stack, true);
        ((IEntityDataSaver) player).getPersistentData().putBoolean(USED_KEY, true);
        Attunement.sync(player);
        player.sendMessage(Text.literal("The forge's blessing settles on ").formatted(Formatting.GOLD)
                .append(stack.toHoverableText()).append(Text.literal(" until your next long rest.")
                        .formatted(Formatting.GOLD)), false);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS,
                0.7F, 1.3F);
        return true;
    }

    /** A long rest ends the blessings on what this player carries, and gives a Forge Cleric theirs back. */
    public static void onLongRest(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (MagicData.isForgeBlessed(stack))
                MagicData.setForgeBlessed(stack, false);
        }
        ((IEntityDataSaver) player).getPersistentData().remove(USED_KEY);
        Attunement.sync(player);
    }
}
