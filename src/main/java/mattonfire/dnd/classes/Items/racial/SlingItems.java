package mattonfire.dnd.classes.Items.racial;

import mattonfire.dnd.entity.SlingStoneEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * What the slings share (the Halfling Sling, the Kobold Sling): one cobblestone from the inventory per shot,
 * thrown as a {@link SlingStoneEntity}. Creative players need no stones.
 */
public final class SlingItems {
    public static final float SPEED = 1.6F;

    private SlingItems() {
    }

    /** Slings a stone doing {@code damage}, then puts the sling on a {@code cooldown}-tick cooldown. */
    public static TypedActionResult<ItemStack> sling(World world, PlayerEntity user, Hand hand, Item item, float damage,
            int cooldown) {
        ItemStack sling = user.getStackInHand(hand);
        ItemStack ammo = findAmmo(user);
        if (ammo.isEmpty() && !user.getAbilities().creativeMode) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.dndclasses.halfling_sling.no_ammo").formatted(Formatting.GRAY),
                        true);
            }
            return TypedActionResult.fail(sling);
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.PLAYERS, 0.6F, 0.6F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClient) {
            SlingStoneEntity stone = new SlingStoneEntity(world, user, damage);
            stone.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, SPEED, 1.0F);
            world.spawnEntity(stone);
            if (!user.getAbilities().creativeMode) {
                ammo.decrement(1);
                sling.damage(1, user, p -> p.sendToolBreakStatus(hand));
            }
        }
        user.getItemCooldownManager().set(item, cooldown);
        user.incrementStat(Stats.USED.getOrCreateStat(item));
        return TypedActionResult.success(sling, world.isClient());
    }

    /** The first cobblestone stack: offhand, then the inventory. */
    public static ItemStack findAmmo(PlayerEntity user) {
        if (user.getOffHandStack().isOf(Items.COBBLESTONE)) {
            return user.getOffHandStack();
        }
        for (int i = 0; i < user.getInventory().size(); i++) {
            ItemStack stack = user.getInventory().getStack(i);
            if (stack.isOf(Items.COBBLESTONE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
