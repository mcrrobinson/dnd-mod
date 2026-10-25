package mattonfire.dnd.classes.Items;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.entity.BasiliskSaves;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Salve of Stone to Flesh (design 8, section 7.5), made from a Basilisk Eye. Use it on yourself, or right-click
 * another creature with it, to end Petrified and the turning-to-stone Restrained (key
 * {@value BasiliskSaves#PETRIFYING_KEY}, the Basilisk's and the Medusa's). On a statue mob it brings the mob back
 * to life (a hostile one turns on you). A petrified player can't use it themselves (they're incapacitated): an
 * ally has to. Needs no hold, unlike stabilising a Downed ally.
 */
public class SalveOfStoneToFleshItem extends Item {
    public SalveOfStoneToFleshItem(Settings settings) {
        super(settings.maxCount(16));
    }

    /** Whether the salve has anything to cure on {@code target}. Both sides (the client can't see the key). */
    public static boolean needsSalve(LivingEntity target) {
        if (Conditions.has(target, Condition.PETRIFIED)) {
            return true;
        }
        if (target.getWorld().isClient) {
            return Conditions.has(target, Condition.RESTRAINED);
        }
        return Conditions.all(target).stream().anyMatch(i -> i.condition() == Condition.RESTRAINED
                && BasiliskSaves.PETRIFYING_KEY.equals(i.key()));
    }

    /** Ends the petrification on {@code target}. Server side. Returns whether anything ended. */
    public static boolean cure(LivingEntity target, @Nullable PlayerEntity user) {
        boolean statue = Conditions.has(target, Condition.PETRIFIED) && !(target instanceof PlayerEntity)
                && Conditions.get(target, Condition.PETRIFIED) != null
                && Conditions.get(target, Condition.PETRIFIED).indefinite();
        boolean cured = Conditions.remove(target, Condition.PETRIFIED);
        cured |= Conditions.removeKey(target, Condition.RESTRAINED, BasiliskSaves.PETRIFYING_KEY);
        DnDClasses.LOGGER.info("[Salve] {} applies Salve of Stone to Flesh to {}: {}",
                user == null ? "nobody" : user.getEntityName(), target.getName().getString(),
                cured ? (statue ? "statue restored" : "cured") : "nothing to cure");
        if (!cured) {
            return false;
        }
        World world = target.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getBodyY(0.5D),
                    target.getZ(), 12, target.getWidth() / 2, target.getHeight() / 3, target.getWidth() / 2, 0.0D);
        }
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_HONEY_BLOCK_SLIDE,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        if (statue && user != null && target instanceof HostileEntity hostile && !user.isCreative()) {
            // Back from stone, and not grateful
            hostile.setTarget(user);
        }
        if (target instanceof PlayerEntity player) {
            player.sendMessage(Text.translatable("item.dndclasses.salve_of_stone_to_flesh.cured")
                    .formatted(Formatting.GREEN), true);
        }
        return true;
    }

    private static void consume(PlayerEntity user, ItemStack stack) {
        if (!user.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!needsSalve(user)) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.dndclasses.salve_of_stone_to_flesh.nothing")
                        .formatted(Formatting.GRAY), true);
            }
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient && cure(user, user)) {
            consume(user, stack);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dndclasses.salve_of_stone_to_flesh.tooltip").formatted(Formatting.GRAY));
    }

    /**
     * Right-clicking a creature with the salve applies it to that creature, before the creature's own interaction
     * (so a petrified villager doesn't open its trades instead). Register once at init.
     */
    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof SalveOfStoneToFleshItem) || player.isSpectator()
                    || !(entity instanceof LivingEntity target) || !needsSalve(target)) {
                return ActionResult.PASS;
            }
            if (!world.isClient && cure(target, player)) {
                consume(player, stack);
            }
            return ActionResult.SUCCESS;
        });
    }
}
