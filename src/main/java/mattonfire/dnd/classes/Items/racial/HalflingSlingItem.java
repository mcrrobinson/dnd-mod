package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.lib.RacialGearItem;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.entity.SlingStoneEntity;
import net.minecraft.client.item.TooltipContext;
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
 * The Halfling Sling: right-click to sling one cobblestone from your inventory (4 damage, 0.8 s cooldown).
 * Creative players need no stones.
 */
public class HalflingSlingItem extends Item implements RacialGearItem {
    public static final int COOLDOWN_TICKS = 16;
    private static final float SPEED = 1.6F;

    public HalflingSlingItem(Settings settings) {
        super(settings);
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.HALFLING);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
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
            SlingStoneEntity stone = new SlingStoneEntity(world, user);
            stone.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, SPEED, 1.0F);
            world.spawnEntity(stone);
            if (!user.getAbilities().creativeMode) {
                ammo.decrement(1);
                sling.damage(1, user, p -> p.sendToolBreakStatus(hand));
            }
        }
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(sling, world.isClient());
    }

    /** The first cobblestone stack: offhand, then the inventory. */
    private static ItemStack findAmmo(PlayerEntity user) {
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

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        ArmorTooltips.appendRaces(getMatchingRaces(), tooltip);
        if (FAConfig.getValues().showDescriptions()) {
            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }
}
