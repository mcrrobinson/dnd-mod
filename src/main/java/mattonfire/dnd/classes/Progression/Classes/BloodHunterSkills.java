package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Blood Hunter tree. The class's x2 night / x0.5 day sword multiplier is
 * applied in LivingEntityMixin after these hooks run, so Sunshield and Blood
 * Moon scale day damage up here to land on their target multiplier.
 */
public class BloodHunterSkills extends ClassSkills {
    /**
     * The root special; fired by {@code PowerUpEffect} through {@code BloodHunterControl}. Short, unreliable
     * and close-range early on; today's 20 s, 30 blocks and a sure hit at full rank. Strong mobs lower the
     * success chance (see {@code BloodHunterControl.successChance}).
     */
    public static final Ranks BLOOD_CONTROL = Ranks.of("bloodhunter.blood_control")
            .seconds("Duration", 8, 12, 16, 20)
            .percent("Success", 60, 75, 90, 100)
            .amount("Range", "blocks", 15, 20, 25, 30);

    private static final int NIGHT_SWORD_KILL_BONUS_XP = 3;

    private static final int BLEED_TICKS = 40;
    private static final double BINDING_RANGE = 20.0;
    private static final int BINDING_TICKS = 80;
    private static final float HEMOCRAFT_HEAL = 1.0F;
    /** x1.5 here, then x0.5 in the mixin: 75% damage in the day. */
    private static final float SUNSHIELD_DAY_FACTOR = 1.5F;
    private static final int TRANSFORM_TICKS = 300;
    private static final int PREDATOR_RADIUS = 16;
    private static final int BLOOD_MOON_TICKS = 400;
    /** x4 here, then x0.5 in the mixin: night damage (x2) in the day. */
    private static final float BLOOD_MOON_DAY_FACTOR = 4.0F;
    private static final float BLOOD_MOON_LIFESTEAL = 0.2F;

    /** Players with Blood Moon up, and the server tick it ends on. */
    private static final Map<UUID, Integer> BLOOD_MOON = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.BLOODHUNTER;
    }

    @Override
    public List<String> subclassIds() {
        return List.of("bloodhunter.profane_soul", "bloodhunter.lycan");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("bloodhunter.blood_control", "Blood Control",
                        "Take control of the mob you're looking at for a while. It can resist, more often if it's strong; "
                                + "a failure still costs the mana and a heart.", "minecraft:lead", 9,
                        0, 1, 3),
                // Order of the Profane Soul
                passive("bloodhunter.crimson_rite", "Crimson Rite", "Sword hits make targets bleed (Wither).",
                        "minecraft:redstone", 1, 2, 3, "bloodhunter.blood_control"),
                active("bloodhunter.curse_of_binding", "Curse of Binding",
                        "The mob you're looking at (20 blocks) can't move for 4 seconds.", "minecraft:chain", 3, 1, 2,
                        2, "bloodhunter.crimson_rite"),
                passive("bloodhunter.hemocraft", "Hemocraft", "Heal half a heart per sword hit at night.",
                        "minecraft:glistering_melon_slice", 1, 2, 1, "bloodhunter.curse_of_binding"),
                // Order of the Lycan
                passive("bloodhunter.sunshield", "Sunshield", "Swords deal 75% damage in the day instead of 50%.",
                        "minecraft:sunflower", 1, 0, 3, "bloodhunter.blood_control"),
                active("bloodhunter.hybrid_transformation", "Hybrid Transformation",
                        "Strength II, Speed and Jump Boost II for 15 seconds.", "minecraft:bone", 6, 1, 0, 2,
                        "bloodhunter.sunshield"),
                passive("bloodhunter.predator", "Predator",
                        "At night, see in the dark and hostiles within 16 blocks glow.", "minecraft:spyglass", 1, 0, 1,
                        "bloodhunter.hybrid_transformation"),
                active("bloodhunter.blood_moon", "Blood Moon",
                        "For 20 seconds, swords deal night damage in the day and you heal 20% of the damage you deal.",
                        "minecraft:redstone_block", 9, 2, 1, 0, "bloodhunter.hemocraft", "bloodhunter.predator"));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        switch (node.id()) {
            case "bloodhunter.curse_of_binding" -> {
                LivingEntity target = lookedAtMob(player, BINDING_RANGE);
                if (target == null)
                    return false;
                target.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, BINDING_TICKS, 0), player);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, BINDING_TICKS, 0), player);
                ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(),
                        target.getBodyY(0.5), target.getZ(), 15, 0.4, 0.5, 0.4, 0.1);
                effects(player, SoundEvents.BLOCK_CHAIN_PLACE, ParticleTypes.CRIMSON_SPORE, 20);
            }
            case "bloodhunter.hybrid_transformation" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, TRANSFORM_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, TRANSFORM_TICKS, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, TRANSFORM_TICKS, 1));
                effects(player, SoundEvents.ENTITY_WOLF_HOWL, ParticleTypes.ANGRY_VILLAGER, 20);
            }
            case "bloodhunter.blood_moon" -> {
                BLOOD_MOON.put(player.getUuid(), player.getServer().getTicks() + BLOOD_MOON_TICKS);
                player.sendMessage(Text.of("The Blood Moon rises."), true);
                effects(player, SoundEvents.ENTITY_WITHER_SPAWN, ParticleTypes.CRIMSON_SPORE, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** The mob in the crosshair within range, not through walls; bosses are immune. */
    private static LivingEntity lookedAtMob(ServerPlayerEntity player, double maxRange) {
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        double range = maxRange;
        HitResult blockHit = player.raycast(maxRange, 1.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            range = blockHit.getPos().distanceTo(eye);
        }
        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, eye.add(look.multiply(range)), box,
                e -> e instanceof MobEntity && e.isAlive() && !(e instanceof EnderDragonEntity)
                        && !(e instanceof WitherEntity),
                range * range);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Same night window as LivingEntityMixin's sword multiplier. */
    private static boolean isNight(PlayerEntity player) {
        long time = player.getWorld().getTimeOfDay() % 24000;
        return time >= 13000 && time <= 23000;
    }

    private static boolean bloodMoonActive(PlayerEntity player) {
        Integer end = BLOOD_MOON.get(player.getUuid());
        if (end == null || player.getServer() == null)
            return false;
        if (player.getServer().getTicks() >= end) {
            BLOOD_MOON.remove(player.getUuid());
            return false;
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        boolean swordKill = source.getSource() == player && player.getMainHandStack().getItem() instanceof SwordItem;
        return killed instanceof Monster && swordKill && isNight(player) ? NIGHT_SWORD_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        // The mixin's multiplier checks only the held item, not the damage type.
        boolean sword = player.getMainHandStack().getItem() instanceof SwordItem;
        boolean melee = sword && source.getSource() == player;
        boolean night = isNight(player);
        boolean bloodMoon = bloodMoonActive(player);

        if (melee && progress.hasPassive("bloodhunter.crimson_rite")) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, BLEED_TICKS, 0), player);
        }
        if (melee && night && progress.hasPassive("bloodhunter.hemocraft")) {
            player.heal(HEMOCRAFT_HEAL);
        }

        if (sword && !night) {
            if (bloodMoon) {
                amount *= BLOOD_MOON_DAY_FACTOR;
            } else if (progress.hasPassive("bloodhunter.sunshield")) {
                amount *= SUNSHIELD_DAY_FACTOR;
            }
        }

        if (bloodMoon) {
            // What actually lands once the mixin's multiplier is applied.
            float dealt = sword ? amount * (night ? 2.0F : 0.5F) : amount;
            player.heal(Math.min(dealt, target.getHealth()) * BLOOD_MOON_LIFESTEAL);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (bloodMoonActive(player)) {
            ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.CRIMSON_SPORE, player.getX(),
                    player.getY() + 1, player.getZ(), 10, 0.5, 0.6, 0.5, 0.02);
        }
        if (progress.hasPassive("bloodhunter.predator") && isNight(player)) {
            StatusEffectInstance vision = player.getStatusEffect(StatusEffects.NIGHT_VISION);
            // Refresh before 10s left, when the screen starts flickering.
            if (vision == null || vision.getDuration() < 220) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false));
            }
            for (LivingEntity mob : hostilesNear(player, PREDATOR_RADIUS)) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false), player);
            }
        }
    }

    @Override
    public void forget(ServerPlayerEntity player) {
        int now = player.getServer().getTicks();
        BLOOD_MOON.values().removeIf(t -> now >= t);
    }
}
