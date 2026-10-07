package mattonfire.dnd.classes.Progression;

import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Where class XP comes from, and the passive skills that hook into the game.
 */
public final class ProgressionEvents {
    // XP for kills.
    private static final int HOSTILE_KILL_XP = 2;
    private static final int BARBARIAN_MELEE_KILL_BONUS = 3;
    private static final int DRUID_ANIMAL_KILL_XP = 3;
    private static final int DRUID_FORM_KILL_BONUS = 4;
    private static final int ROGUE_STEALTH_KILL_BONUS = 4;

    /** Passives that are just an attribute bonus, kept up to date once a second. */
    private record AttributeBonus(String skill, EntityAttribute attribute, double amount,
            EntityAttributeModifier.Operation operation) {
        UUID uuid() {
            return UUID.nameUUIDFromBytes(("dndclasses:" + skill).getBytes());
        }
    }

    private static final List<AttributeBonus> ATTRIBUTE_BONUSES = List.of(
            new AttributeBonus("barbarian.thick_skin", EntityAttributes.GENERIC_ARMOR, 4,
                    EntityAttributeModifier.Operation.ADDITION),
            new AttributeBonus("barbarian.unstoppable", EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5,
                    EntityAttributeModifier.Operation.ADDITION),
            new AttributeBonus("rogue.fleet", EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE));

    private ProgressionEvents() {
    }

    static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ProgressionEvents::onKill);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0)
                return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                secondTick(player);
            }
        });
    }

    private static void onKill(LivingEntity entity, DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player) || entity == player)
            return;

        DndCharacter dndClass = Progression.classOf(player);
        boolean hostile = entity instanceof Monster;
        boolean melee = source.getSource() == player;
        int xp = hostile ? HOSTILE_KILL_XP : 0;

        switch (dndClass) {
            case BARBARIAN -> {
                if (hostile && melee)
                    xp += BARBARIAN_MELEE_KILL_BONUS;
            }
            case DRUID -> {
                if (entity instanceof AnimalEntity)
                    xp += DRUID_ANIMAL_KILL_XP;
                if (hostile && Druid.isTransformed(player))
                    xp += DRUID_FORM_KILL_BONUS;
            }
            case ROGUE -> {
                if (hostile && (player.isSneaking() || player.isInvisible()))
                    xp += ROGUE_STEALTH_KILL_BONUS;
            }
            default -> {
            }
        }
        Progression.addXp(player, xp);

        if (Progression.hasPassive(player, "barbarian.bloodlust")) {
            player.heal(2.0F);
        }
    }

    private static void secondTick(ServerPlayerEntity player) {
        ClassProgress progress = Progression.current(player);

        for (AttributeBonus bonus : ATTRIBUTE_BONUSES) {
            EntityAttributeInstance instance = player.getAttributeInstance(bonus.attribute());
            if (instance == null)
                continue;
            boolean wanted = progress.hasPassive(bonus.skill());
            boolean has = instance.getModifier(bonus.uuid()) != null;
            if (wanted && !has) {
                // Temporary so it isn't saved; it's re-added here after a relog.
                instance.addTemporaryModifier(new EntityAttributeModifier(bonus.uuid(), bonus.skill(),
                        bonus.amount(), bonus.operation()));
            } else if (!wanted && has) {
                instance.removeModifier(bonus.uuid());
            }
        }

        if (progress.hasPassive("barbarian.unstoppable")) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        }
        if (progress.hasPassive("druid.hardy_form") && Druid.isTransformed(player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
        }
    }

    /**
     * Applies damage passives. Called from {@code LivingEntityMixin} with the
     * amount about to be dealt (before armor).
     */
    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.getWorld().isClient)
            return amount;

        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player && attacker != target) {
            ClassProgress progress = Progression.current(player);
            boolean melee = source.getSource() == player;

            if (progress.hasPassive("barbarian.rage_fuelled") && player.getHealth() < player.getMaxHealth() / 2) {
                amount *= 1.3F;
            }
            if (melee && progress.hasPassive("rogue.backstab") && (player.isSneaking() || player.isInvisible())) {
                amount *= 1.5F;
            }
            if (melee && progress.hasPassive("rogue.poisoned_blades")) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), player);
            }
        }

        if (target instanceof PlayerEntity player && source.isOf(DamageTypes.FALL)
                && Progression.hasPassive(player, "rogue.light_feet")) {
            amount *= 0.5F;
        }
        return amount;
    }
}
