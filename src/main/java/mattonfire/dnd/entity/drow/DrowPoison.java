package mattonfire.dnd.entity.drow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SaveRules;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Drow Poison (design 9, section 4.2): what drow hand-crossbow bolts and the priestess's scourge carry.
 * <ul>
 * <li>A CON save ({@link DrowSaves}, DC 13, tagged {@code poison} so a Dwarf's resilience gives advantage). A success
 * gives the poison for half as long ({@value #SAVED_TICKS} ticks) and adds no stack. A failure gives it for
 * {@value #FAIL_TICKS} ticks, with the Poisoned condition for as long, and adds a stack.</li>
 * <li>The effect: Slowness II and Weakness I worth of attribute modifiers, and a darkened screen edge (client).</li>
 * <li>{@value #SLEEP_STACKS} stacks within {@value #STACK_WINDOW} ticks and you fall asleep for {@value #SLEEP_TICKS}
 * ticks: Incapacitated (condition key {@value #SLEEP_KEY}), Slowness VI and Mining Fatigue III (no negative Jump Boost: it turns into fall damage on landing, which would wake you). Taking
 * damage wakes you.</li>
 * <li>Immune: what can't be Poisoned (undead, constructs, a Petrified creature) and players immune to poison
 * ({@link SaveRules#immune}: the Rogue, the Alchemist's Iron Stomach, poison-immunity potions).</li>
 * </ul>
 */
public final class DrowPoison {
    public static final int FAIL_TICKS = 120;
    public static final int SAVED_TICKS = FAIL_TICKS / 2;
    public static final int STACK_WINDOW = 200;
    public static final int SLEEP_STACKS = 3;
    public static final int SLEEP_TICKS = 60;
    public static final String SLEEP_KEY = "drow_sleep";
    /** One save per attacker per this many ticks (one bolt, one scourge lash). */
    public static final int EXPOSURE_TICKS = 20;

    /** Target to the world times of its failed saves inside the stack window. */
    private static final Map<UUID, Deque<Long>> STACKS = new HashMap<>();

    private DrowPoison() {
    }

    /** The status effect: fixed Slowness II and Weakness I modifiers, whatever the amplifier. */
    public static final class Effect extends StatusEffect {
        public Effect() {
            super(StatusEffectCategory.HARMFUL, 0x2E3B1F);
            this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "6c2f1d5e-3b0a-4d8e-9a77-2a8d1f0c4b11",
                    -0.30D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            this.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "a4b1e3c2-77d5-4f0e-8c1d-5e9b2f6a3d22",
                    -4.0D, EntityAttributeModifier.Operation.ADDITION);
        }

        @Override
        public double adjustModifierAmount(int amplifier, EntityAttributeModifier modifier) {
            return modifier.getValue();
        }
    }

    /** Wakes sleepers when they're hurt. */
    static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0.0F && isAsleep(entity)) {
                DnDClasses.LOGGER.info("[Drow] {} hurt by {} ({}) while asleep", entity.getName().getString(),
                        source.getName(), amount);
                wake(entity);
            }
            return true;
        });
    }

    /** Whether {@code entity} is asleep from drow poison. */
    public static boolean isAsleep(LivingEntity entity) {
        for (var instance : Conditions.all(entity)) {
            if (instance.condition() == Condition.INCAPACITATED && SLEEP_KEY.equals(instance.key())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code source} poisons {@code target} (a bolt or a scourge hit that landed): the save, the effect, the stack
     * and, at {@value #SLEEP_STACKS} stacks, the sleep. Returns whether the save failed.
     */
    public static boolean afflict(LivingEntity target, @Nullable LivingEntity source, MobSaveInfo.Entry save) {
        if (target.getWorld().isClient || !target.isAlive() || immune(target, source)) {
            return false;
        }
        SaveResult result = SavingThrow.of(target, save).source(source)
                .exposure(source, "drow_poison", EXPOSURE_TICKS).tags("poison")
                .onSuccess(Text.translatable("save.dndclasses.effect.drow_poison.saved"))
                .onFailure(Text.translatable("save.dndclasses.effect.drow_poison.failed"))
                .roll();
        if (result.succeeded()) {
            give(target, source, SAVED_TICKS);
            return false;
        }
        give(target, source, FAIL_TICKS);
        Conditions.of(target, Condition.POISONED).source(source).key("drow_poison").ticks(FAIL_TICKS).apply();
        if (result.fresh()) {
            addStack(target, source);
        }
        return true;
    }

    private static boolean immune(LivingEntity target, @Nullable LivingEntity source) {
        if (target instanceof PlayerEntity player
                && (player.isCreative() || player.isSpectator()
                || SaveRules.immune(player, new StatusEffectInstance(StatusEffects.POISON, FAIL_TICKS)))) {
            return true;
        }
        return Conditions.isImmune(target, Condition.POISONED, source) || DrowAllies.isAlly(target);
    }

    private static void give(LivingEntity target, @Nullable LivingEntity source, int ticks) {
        target.addStatusEffect(new StatusEffectInstance(Drow.DROW_POISON, ticks, 0), source);
    }

    private static void addStack(LivingEntity target, @Nullable LivingEntity source) {
        long now = target.getWorld().getTime();
        Deque<Long> stacks = STACKS.computeIfAbsent(target.getUuid(), k -> new ArrayDeque<>());
        while (!stacks.isEmpty() && (now - stacks.peekFirst() > STACK_WINDOW || stacks.peekFirst() > now)) {
            stacks.pollFirst();
        }
        stacks.addLast(now);
        int count = stacks.size();
        DnDClasses.LOGGER.info("[Drow] {} drow poison stack {}/{}", target.getName().getString(), count, SLEEP_STACKS);
        if (count >= SLEEP_STACKS) {
            STACKS.remove(target.getUuid());
            sleep(target, source);
        } else if (target instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("message.dndclasses.drow_poison.stack", count, SLEEP_STACKS)
                    .formatted(Formatting.DARK_GREEN), true);
        }
    }

    /** Puts {@code target} to sleep for {@value #SLEEP_TICKS} ticks. */
    public static void sleep(LivingEntity target, @Nullable LivingEntity source) {
        Conditions.of(target, Condition.INCAPACITATED).source(source).key(SLEEP_KEY).ticks(SLEEP_TICKS).apply();
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLEEP_TICKS, 5, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, SLEEP_TICKS, 2, false, false, true));
        DnDClasses.LOGGER.info("[Drow] {} falls asleep from drow poison", target.getName().getString());
        if (target instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("message.dndclasses.drow_poison.sleep").formatted(Formatting.DARK_GREEN), true);
        }
    }

    /** Ends the sleep (taking damage). */
    public static void wake(LivingEntity target) {
        Conditions.removeKey(target, Condition.INCAPACITATED, SLEEP_KEY);
        removeIf(target, StatusEffects.SLOWNESS, 5);
        removeIf(target, StatusEffects.MINING_FATIGUE, 2);
        DnDClasses.LOGGER.info("[Drow] {} wakes up", target.getName().getString());
        if (target instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("message.dndclasses.drow_poison.wake").formatted(Formatting.GRAY), true);
        }
    }

    private static void removeIf(LivingEntity target, StatusEffect type, int amplifier) {
        StatusEffectInstance current = target.getStatusEffect(type);
        if (current != null && current.getAmplifier() == amplifier) {
            target.removeStatusEffect(type);
        }
    }

    /** Forgets a creature's stacks (it left or died). */
    static void forget(UUID uuid) {
        STACKS.remove(uuid);
    }
}
