package mattonfire.dnd.classes.Downed;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PotionImmunity;
import mattonfire.dnd.classes.Progression.Classes.FighterSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.tavern.Tavern;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * Bringing a Downed player back with healing.
 * <ul>
 * <li><b>Heals:</b> a {@code LivingEntity.heal} on a Downed player revives them with {@code max(1, amount)} HP when
 * it comes from someone else: another player's active skill or power-up ({@link #asHealer}), an Instant Health
 * effect (splash and lingering potions, tipped arrows), or Regeneration put on after they went down (Druid
 * Regrowth, beacons). Every other heal is the player's own (natural regen, Second Wind, life drain, Hit Dice) and
 * is blocked. Potion-immune classes stay immune, so a splash potion doesn't revive a Paladin.</li>
 * <li><b>Feeding:</b> right-clicking a Downed player with a Potion of Healing, a golden apple, an enchanted golden
 * apple or a Mug of Ale uses it on them ({@link #feed}).</li>
 * </ul>
 * Other systems (magic items, scripted healers) that heal someone else should wrap the heal in
 * {@link #asHealer} or call {@link #healFrom}, or the heal is treated as the Downed player's own and blocked.
 */
public final class Revives {
    /** HP a golden apple (or helping a stable player up) stands you up with. */
    public static final float APPLE_HP = 4.0F;

    /** The player whose skill is running on this thread: heals on anyone else are outside heals. */
    private static final ThreadLocal<ServerPlayerEntity> HEALER = new ThreadLocal<>();
    /** Depth of outside heals on this thread (effects, {@link #healFrom}). */
    private static final ThreadLocal<int[]> OUTSIDE_HEAL = ThreadLocal.withInitial(() -> new int[1]);

    private Revives() {
    }

    public static void register() {
        // Class features on death saves
        DeathSaveModifier.EVENT.register(new DeathSaveModifier() {
            @Override
            public int bonus(ServerPlayerEntity player) {
                return PaladinSkills.auraDeathSaveBonus(player);
            }

            @Override
            public boolean rerollFailure(ServerPlayerEntity player) {
                return FighterSkills.indomitableReroll(player);
            }
        });
        DownedEvents.AFTER_DOWNED.register((player, source) -> FighterSkills.resetIndomitableReroll(player));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(entity instanceof PlayerEntity target) || !Downed.is(target) || Downed.is(player)
                    || !canFeed(player.getStackInHand(hand), target)) {
                return ActionResult.PASS;
            }
            // Client side: claim the click so the helper doesn't drink or eat the item themselves
            return player instanceof ServerPlayerEntity helper && target instanceof ServerPlayerEntity downed
                    ? feed(helper, downed, hand)
                    : ActionResult.SUCCESS;
        });
    }

    /** Runs a skill of {@code healer}'s: anything it heals (other than the healer) counts as an outside heal. */
    public static boolean asHealer(ServerPlayerEntity healer, BooleanSupplier action) {
        ServerPlayerEntity previous = HEALER.get();
        HEALER.set(healer);
        try {
            return action.getAsBoolean();
        } finally {
            HEALER.set(previous);
        }
    }

    /** Heals {@code target} on behalf of {@code healer}; revives them if they're Downed. */
    public static void healFrom(@Nullable ServerPlayerEntity healer, LivingEntity target, float amount) {
        OUTSIDE_HEAL.get()[0]++;
        try {
            target.heal(amount);
        } finally {
            OUTSIDE_HEAL.get()[0]--;
        }
    }

    /** Called by the Instant Health and Regeneration heal redirects in {@code DownedHealMixin}. */
    public static void effectHeal(LivingEntity target, float amount) {
        healFrom(null, target, amount);
    }

    /**
     * The {@code LivingEntity.heal} HEAD hook for a Downed player: an outside heal stands them up, anything else
     * is blocked. Either way vanilla's heal is cancelled.
     */
    public static void onHeal(ServerPlayerEntity player, float amount) {
        ServerPlayerEntity healer = HEALER.get();
        boolean outside = OUTSIDE_HEAL.get()[0] > 0 || healer != null && healer != player;
        if (!outside || amount <= 0.0F) {
            return;
        }
        DnDClasses.LOGGER.info("[Downed] {} revived by a heal of {}{}", player.getEntityName(), amount,
                healer != null && healer != player ? " from " + healer.getEntityName() : "");
        Downed.revive(player, Math.max(1.0F, amount));
    }

    /** Uses the helper's held healing item on a Downed player. */
    static ActionResult feed(ServerPlayerEntity helper, ServerPlayerEntity target, Hand hand) {
        ItemStack stack = helper.getStackInHand(hand);
        if (stack.isOf(Items.POTION) && heals(stack)) {
            if (PotionImmunity.isImmune(target, StatusEffects.INSTANT_HEALTH)) {
                helper.sendMessage(Text.literal(target.getEntityName() + " shrugs off potions.")
                        .formatted(Formatting.GRAY), true);
                return ActionResult.FAIL;
            }
            PotionImmunity.begin();
            try {
                for (StatusEffectInstance effect : PotionUtil.getPotionEffects(stack)) {
                    if (effect.getEffectType().isInstant()) {
                        effect.getEffectType().applyInstantEffect(helper, helper, target, effect.getAmplifier(), 1.0D);
                    } else {
                        target.addStatusEffect(new StatusEffectInstance(effect), helper);
                    }
                }
            } finally {
                PotionImmunity.end();
            }
            used(helper, target, hand, stack, new ItemStack(Items.GLASS_BOTTLE), "a Potion of Healing");
            return ActionResult.SUCCESS;
        }
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            Downed.revive(target, APPLE_HP);
            FoodComponent food = stack.getItem().getFoodComponent();
            if (food != null) {
                target.getHungerManager().eat(stack.getItem(), stack);
                for (Pair<StatusEffectInstance, Float> pair : food.getStatusEffects()) {
                    if (target.getRandom().nextFloat() < pair.getSecond()) {
                        target.addStatusEffect(new StatusEffectInstance(pair.getFirst()), helper);
                    }
                }
            }
            used(helper, target, hand, stack, ItemStack.EMPTY, stack.isOf(Items.GOLDEN_APPLE)
                    ? "a golden apple" : "an enchanted golden apple");
            return ActionResult.SUCCESS;
        }
        if (stack.isOf(Tavern.ALE)) {
            if (Downed.isStable(target)) {
                return ActionResult.FAIL;
            }
            Downed.stabilise(target);
            target.sendMessage(Text.literal("A stiff drink.").formatted(Formatting.GOLD), false);
            used(helper, target, hand, stack, ItemStack.EMPTY, "a Mug of Ale");
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    /** Whether the stack is something you can feed a Downed player (both sides). */
    public static boolean canFeed(ItemStack stack, PlayerEntity target) {
        return stack.isOf(Items.POTION) && heals(stack) || stack.isOf(Items.GOLDEN_APPLE)
                || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Tavern.ALE) && !Downed.isStable(target);
    }

    private static boolean heals(ItemStack potion) {
        for (StatusEffectInstance effect : PotionUtil.getPotionEffects(potion)) {
            if (effect.getEffectType() == StatusEffects.INSTANT_HEALTH) {
                return true;
            }
        }
        return false;
    }

    private static void used(ServerPlayerEntity helper, ServerPlayerEntity target, Hand hand, ItemStack stack,
            ItemStack leftover, String what) {
        DnDClasses.LOGGER.info("[Downed] {} fed {} {}", helper.getEntityName(), target.getEntityName(), what);
        helper.world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_GENERIC_DRINK,
                SoundCategory.PLAYERS, 0.8F, 1.0F);
        helper.sendMessage(Text.literal("You give " + target.getEntityName() + " " + what + ".")
                .formatted(Formatting.GREEN), true);
        if (helper.getAbilities().creativeMode) {
            return;
        }
        stack.decrement(1);
        if (!leftover.isEmpty()) {
            if (stack.isEmpty()) {
                helper.setStackInHand(hand, leftover);
            } else if (!helper.getInventory().insertStack(leftover)) {
                helper.dropItem(leftover, false);
            }
        }
    }
}
