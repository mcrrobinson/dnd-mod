package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

/**
 * Fighter tree. Skill effects aren't potion-sourced, so the Fighter's potion
 * block (PotionImmunity) doesn't stop them.
 */
public class FighterSkills extends ClassSkills {
    private static final int MELEE_KILL_BONUS_XP = 2;
    private static final int CROWD_KILL_BONUS_XP = 3;
    private static final int CROWD_SIZE = 3;
    private static final double CROWD_RADIUS = 6;

    private static final float CRIT_BONUS = 1.25F;
    private static final int ACTION_SURGE_TICKS = 160;
    private static final float RIPOSTE_REFLECT = 0.5F;
    private static final int RIPOSTE_TICKS = 120;
    private static final float SECOND_WIND_THRESHOLD = 0.25F;
    private static final float SECOND_WIND_HEAL = 6.0F;
    private static final int SECOND_WIND_COOLDOWN_TICKS = 1200;
    private static final int INDOMITABLE_TICKS = 300;

    private static final UUID INDOMITABLE_KNOCKBACK = UUID
            .nameUUIDFromBytes("dndclasses:fighter.indomitable".getBytes());

    /** Server tick each player's buff or cooldown ends at. */
    private static final Map<UUID, Integer> RIPOSTE_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> SECOND_WIND_READY = new HashMap<>();
    private static final Map<UUID, Integer> INDOMITABLE_UNTIL = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.FIGHTER;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("fighter.super_regen", "Super Regeneration", "Regeneration V for 10 seconds.",
                        "minecraft:golden_apple", 9, 0, 1, 3),
                // Champion
                passive("fighter.improved_critical", "Improved Critical", "Critical hits deal 25% more damage.",
                        "minecraft:iron_sword", 1, 2, 3, "fighter.super_regen"),
                active("fighter.action_surge", "Action Surge", "Haste II, Speed II and Strength I for 8 seconds.",
                        "minecraft:sugar", 4, 1, 2, 2, "fighter.improved_critical"),
                passive("fighter.brawler", "Brawler", "+2 attack damage.", "minecraft:diamond_sword", 1, 2, 1,
                        "fighter.action_surge"),
                // Battle Master
                passive("fighter.defensive_style", "Defensive Style", "+2 armor and +2 armor toughness.",
                        "minecraft:shield", 1, 0, 3, "fighter.super_regen"),
                active("fighter.riposte", "Riposte",
                        "For 6 seconds, melee attackers take half the damage they deal to you.", "minecraft:cactus",
                        3, 1, 0, 2, "fighter.defensive_style"),
                passive("fighter.second_wind", "Second Wind",
                        "Dropping below 25% health heals 3 hearts (once a minute).",
                        "minecraft:glistering_melon_slice", 1, 0, 1, "fighter.riposte"),
                active("fighter.indomitable", "Indomitable",
                        "Resistance II, Strength II and no knockback for 15 seconds.",
                        "minecraft:netherite_chestplate", 9, 2, 1, 0, "fighter.brawler", "fighter.second_wind"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        // Both Defensive Style bonuses share a modifier UUID, which is fine as they're on different attributes.
        return List.of(
                new AttributeBonus("fighter.brawler", EntityAttributes.GENERIC_ATTACK_DAMAGE, 2,
                        EntityAttributeModifier.Operation.ADDITION),
                new AttributeBonus("fighter.defensive_style", EntityAttributes.GENERIC_ARMOR, 2,
                        EntityAttributeModifier.Operation.ADDITION),
                new AttributeBonus("fighter.defensive_style", EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2,
                        EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        int now = player.getServer().getTicks();
        switch (node.id()) {
            case "fighter.action_surge" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, ACTION_SURGE_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ACTION_SURGE_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ACTION_SURGE_TICKS, 0));
                effects(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ParticleTypes.CRIT, 30);
            }
            case "fighter.riposte" -> {
                RIPOSTE_UNTIL.put(player.getUuid(), now + RIPOSTE_TICKS);
                effects(player, SoundEvents.ITEM_SHIELD_BLOCK, ParticleTypes.ENCHANTED_HIT, 20);
            }
            case "fighter.indomitable" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, INDOMITABLE_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, INDOMITABLE_TICKS, 1));
                INDOMITABLE_UNTIL.put(player.getUuid(), now + INDOMITABLE_TICKS);
                setKnockbackImmune(player, true);
                effects(player, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, ParticleTypes.TOTEM_OF_UNDYING, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        if (!(killed instanceof Monster) || source.getSource() != player)
            return 0;
        // The killed mob is already dead so hostilesNear skips it; count it as well.
        boolean crowded = hostilesNear(player, CROWD_RADIUS).size() + 1 >= CROWD_SIZE;
        return MELEE_KILL_BONUS_XP + (crowded ? CROWD_KILL_BONUS_XP : 0);
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (source.getSource() == player && progress.hasPassive("fighter.improved_critical") && isCritical(player)) {
            amount *= CRIT_BONUS;
        }
        return amount;
    }

    /** Vanilla's critical hit check, minus the attack cooldown (already reset when the damage lands). */
    private static boolean isCritical(PlayerEntity player) {
        return player.fallDistance > 0 && !player.isOnGround() && !player.isClimbing() && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasVehicle() && !player.isSprinting();
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        Integer until = RIPOSTE_UNTIL.get(player.getUuid());
        if (until != null && player.getServer() != null && player.getServer().getTicks() < until
                && !source.isOf(DamageTypes.THORNS) && source.getAttacker() instanceof LivingEntity attacker
                && source.getSource() == attacker && attacker != player) {
            // Thorns damage, so two riposting Fighters don't bounce it back and forth.
            attacker.damage(player.getDamageSources().thorns(player), amount * RIPOSTE_REFLECT);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        UUID id = player.getUuid();
        int now = player.getServer().getTicks();

        if (progress.hasPassive("fighter.second_wind") && player.isAlive()
                && player.getHealth() < player.getMaxHealth() * SECOND_WIND_THRESHOLD
                && now >= SECOND_WIND_READY.getOrDefault(id, 0)) {
            player.heal(SECOND_WIND_HEAL);
            SECOND_WIND_READY.put(id, now + SECOND_WIND_COOLDOWN_TICKS);
            effects(player, SoundEvents.ENTITY_PLAYER_LEVELUP, ParticleTypes.HEART, 8);
        }

        Integer indomitable = INDOMITABLE_UNTIL.get(id);
        if (indomitable != null && now >= indomitable) {
            INDOMITABLE_UNTIL.remove(id);
            setKnockbackImmune(player, false);
        }
        RIPOSTE_UNTIL.values().removeIf(t -> now >= t);
    }

    /** Indomitable's full knockback resistance; a temporary modifier, so a relog also clears it. */
    private static void setKnockbackImmune(ServerPlayerEntity player, boolean on) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (instance == null)
            return;
        instance.removeModifier(INDOMITABLE_KNOCKBACK);
        if (on) {
            instance.addTemporaryModifier(new EntityAttributeModifier(INDOMITABLE_KNOCKBACK, "fighter.indomitable",
                    1.0, EntityAttributeModifier.Operation.ADDITION));
        }
    }
}
