package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.enemiesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class BarbarianSkills extends ClassSkills {
    /**
     * The root special; fired by {@code PowerUpEffect}. Strength I for 8 s early on, Strength III for
     * 12 s at full rank, a little under Titan (Strength III, Resistance and Regeneration for 20 s).
     */
    public static final Ranks RAGE = Ranks.of("barbarian.rage")
            .value("Strength", 1, 2, 2, 3)
            .seconds("Duration", 8, 10, 12, 12);

    private static final int MELEE_KILL_BONUS_XP = 3;

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.BARBARIAN;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("barbarian.rage", "Rage", "Strength for a few seconds, stronger with each rank.", "minecraft:blaze_powder", 9, 0, 1, 3),
                // Berserker
                passive("barbarian.bloodlust", "Bloodlust", "Heal a heart every time you kill something.",
                        "minecraft:redstone", 1, 2, 3, "barbarian.rage"),
                active("barbarian.war_cry", "War Cry",
                        "Mobs within 8 blocks get Weakness and Slowness for 6 seconds.", "minecraft:goat_horn", 3, 1,
                        2, 2, "barbarian.bloodlust"),
                passive("barbarian.rage_fuelled", "Fuelled by Rage", "Deal 30% more damage below half health.",
                        "minecraft:fire_charge", 1, 2, 1, "barbarian.war_cry"),
                // Juggernaut
                passive("barbarian.thick_skin", "Thick Skin", "+4 armor.", "minecraft:leather_chestplate", 1, 0, 3,
                        "barbarian.rage"),
                active("barbarian.ground_slam", "Ground Slam",
                        "Slam the ground, hurting and throwing back mobs within 5 blocks.", "minecraft:anvil", 5, 1,
                        0, 2, "barbarian.thick_skin"),
                passive("barbarian.unstoppable", "Unstoppable", "Half knockback, and you can't be slowed.",
                        "minecraft:iron_chestplate", 1, 0, 1, "barbarian.ground_slam"),
                active("barbarian.titan", "Titan", "Strength III, Resistance and Regeneration for 20 seconds.",
                        "minecraft:netherite_axe", 9, 2, 1, 0, "barbarian.rage_fuelled", "barbarian.unstoppable"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(
                new AttributeBonus("barbarian.thick_skin", EntityAttributes.GENERIC_ARMOR, 4,
                        EntityAttributeModifier.Operation.ADDITION),
                new AttributeBonus("barbarian.unstoppable", EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5,
                        EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "barbarian.war_cry" -> {
                for (LivingEntity mob : hostilesNear(player, 8)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), player);
                }
                effects(player, SoundEvents.ENTITY_RAVAGER_ROAR, ParticleTypes.ANGRY_VILLAGER, 20);
            }
            case "barbarian.ground_slam" -> {
                for (LivingEntity mob : enemiesNear(player, 5)) {
                    mob.damage(world.getDamageSources().playerAttack(player), 6.0F);
                    Vec3d push = mob.getPos().subtract(player.getPos()).multiply(1, 0, 1).normalize();
                    mob.takeKnockback(1.5, -push.x, -push.z);
                    mob.addVelocity(0, 0.4, 0);
                    mob.velocityModified = true;
                }
                world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 6, 2.0,
                        0.2, 2.0, 0);
                effects(player, SoundEvents.ENTITY_GENERIC_EXPLODE, ParticleTypes.CLOUD, 30);
            }
            case "barbarian.titan" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 400, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 0));
                effects(player, SoundEvents.ENTITY_IRON_GOLEM_REPAIR, ParticleTypes.CRIT, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return killed instanceof Monster && source.getSource() == player ? MELEE_KILL_BONUS_XP : 0;
    }

    @Override
    public void onKill(ServerPlayerEntity player, ClassProgress progress, LivingEntity killed, DamageSource source) {
        if (progress.hasPassive("barbarian.bloodlust")) {
            player.heal(2.0F);
        }
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (progress.hasPassive("barbarian.rage_fuelled") && player.getHealth() < player.getMaxHealth() / 2) {
            return amount * 1.3F;
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("barbarian.unstoppable")) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        }
    }
}
