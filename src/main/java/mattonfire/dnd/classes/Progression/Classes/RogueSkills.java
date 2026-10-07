package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RogueSkills extends ClassSkills {
    private static final int STEALTH_KILL_BONUS_XP = 4;
    private static final int SHADOWSTEP_RANGE = 12;

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.ROGUE;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("rogue.vanish", "Vanish", "Invisibility for 15 seconds.", "minecraft:fermented_spider_eye", 9,
                        0, 1, 3),
                // Assassin
                passive("rogue.backstab", "Backstab", "Deal 50% more damage while sneaking or invisible.",
                        "minecraft:iron_sword", 1, 2, 3, "rogue.vanish"),
                active("rogue.shadowstep", "Shadowstep", "Teleport up to 12 blocks where you're looking.",
                        "minecraft:ender_pearl", 4, 1, 2, 2, "rogue.backstab"),
                passive("rogue.poisoned_blades", "Poisoned Blades", "Your hits poison.", "minecraft:spider_eye", 1, 2,
                        1, "rogue.shadowstep"),
                // Thief
                passive("rogue.light_feet", "Light Feet", "Take half fall damage.", "minecraft:feather", 1, 0, 3,
                        "rogue.vanish"),
                active("rogue.smoke_bomb", "Smoke Bomb",
                        "Blind and slow mobs within 6 blocks, and they lose track of you.", "minecraft:gunpowder", 3,
                        1, 0, 2, "rogue.light_feet"),
                passive("rogue.fleet", "Fleet", "Move 15% faster.", "minecraft:sugar", 1, 0, 1, "rogue.smoke_bomb"),
                active("rogue.death_mark", "Death Mark",
                        "Invisibility, Strength II and Speed II for 10 seconds.", "minecraft:wither_skeleton_skull", 9,
                        2, 1, 0, "rogue.poisoned_blades", "rogue.fleet"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(new AttributeBonus("rogue.fleet", EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "rogue.shadowstep" -> {
                Vec3d target = shadowstepTarget(player);
                if (target == null)
                    return false;
                effects(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, ParticleTypes.LARGE_SMOKE, 20);
                player.requestTeleport(target.x, target.y, target.z);
                player.fallDistance = 0;
                effects(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, ParticleTypes.PORTAL, 30);
            }
            case "rogue.smoke_bomb" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), player);
                    if (mob instanceof MobEntity m && m.getTarget() == player) {
                        m.setTarget(null);
                    }
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0));
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1,
                        player.getZ(), 40, 2.0, 1.0, 2.0, 0.01);
                effects(player, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, ParticleTypes.LARGE_SMOKE, 30);
            }
            case "rogue.death_mark" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
                effects(player, SoundEvents.ENTITY_WITHER_AMBIENT, ParticleTypes.SMOKE, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Where Shadowstep lands: the furthest free spot along the look direction, or null if none. */
    private static Vec3d shadowstepTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        HitResult hit = player.getWorld().raycast(new RaycastContext(eye, eye.add(look.multiply(SHADOWSTEP_RANGE)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double distance = hit.getPos().distanceTo(eye) - 0.5;

        Vec3d feetOffset = player.getPos().subtract(eye);
        for (double d = distance; d > 1.0; d -= 0.5) {
            Vec3d feet = eye.add(look.multiply(d)).add(feetOffset);
            Box box = player.getBoundingBox().offset(feet.subtract(player.getPos()));
            if (player.getWorld().isSpaceEmpty(player, box)) {
                return feet;
            }
        }
        return null;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return killed instanceof Monster && (player.isSneaking() || player.isInvisible()) ? STEALTH_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        boolean melee = source.getSource() == player;
        if (melee && progress.hasPassive("rogue.backstab") && (player.isSneaking() || player.isInvisible())) {
            amount *= 1.5F;
        }
        if (melee && progress.hasPassive("rogue.poisoned_blades")) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), player);
        }
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        return source.isOf(DamageTypes.FALL) && progress.hasPassive("rogue.light_feet") ? amount * 0.5F : amount;
    }
}
