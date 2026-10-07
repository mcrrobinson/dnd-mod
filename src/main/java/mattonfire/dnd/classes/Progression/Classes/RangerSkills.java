package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class RangerSkills extends ClassSkills {
    private static final int BOW_KILL_BONUS_XP = 2;
    private static final int LONG_SHOT_BONUS_XP = 2;
    private static final double LONG_SHOT_DISTANCE = 20;

    private static final float SHARPSHOOTER_BONUS = 1.25F;
    private static final float MARKED_BONUS = 1.2F;
    private static final int MARK_TICKS = 200;

    private static final int VOLLEY_ARROWS = 5;
    private static final float VOLLEY_SPREAD_DEGREES = 10; // Between neighbouring arrows
    private static final float VOLLEY_SPEED = 3.0F;

    private static final int RAIN_RANGE = 30;
    private static final double RAIN_RADIUS = 6;
    private static final double RAIN_HEIGHT = 15;
    private static final int RAIN_TICKS = 60;
    private static final int RAIN_ARROWS_PER_TICK = 2;
    private static final float RAIN_SPEED = 2.0F;
    private static final double RAIN_ARROW_DAMAGE = 4.0;
    private static final String RAIN_TAG = "dndclasses.ranger_rain";

    /** A Rain of Arrows still falling. */
    private record Rain(UUID owner, ServerWorld world, Vec3d center, long endTime) {
    }

    private static final List<Rain> RAINS = new ArrayList<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.RANGER;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("ranger.arrow_storm", "Arrow Storm",
                        "For 15 seconds your bow needs no arrows and fires itself at full draw.", "minecraft:bow", 9,
                        0, 1, 3),
                // Hunter
                passive("ranger.sharpshooter", "Sharpshooter", "Your arrows deal 25% more damage.",
                        "minecraft:arrow", 1, 2, 3, "ranger.arrow_storm"),
                active("ranger.volley", "Volley", "Fire a fan of 5 arrows where you're looking.",
                        "minecraft:crossbow", 4, 1, 2, 2, "ranger.sharpshooter"),
                passive("ranger.hunters_mark", "Hunter's Mark",
                        "Your arrows make targets glow for 10 seconds, and glowing targets take 20% more damage from you.",
                        "minecraft:spectral_arrow", 1, 2, 1, "ranger.volley"),
                // Survivalist
                passive("ranger.fireproof", "Fireproof", "Lava no longer burns you extra hard.",
                        "minecraft:magma_cream", 1, 0, 3, "ranger.arrow_storm"),
                active("ranger.snare", "Snare", "Mobs within 6 blocks get Slowness V for 5 seconds.",
                        "minecraft:cobweb", 3, 1, 0, 2, "ranger.fireproof"),
                passive("ranger.natural_explorer", "Natural Explorer", "Move 10% faster.", "minecraft:leather_boots",
                        1, 0, 1, "ranger.snare"),
                active("ranger.rain_of_arrows", "Rain of Arrows",
                        "Arrows rain on a 6-block radius where you're looking (up to 30 blocks) for 3 seconds.",
                        "minecraft:tipped_arrow", 9, 2, 1, 0, "ranger.hunters_mark", "ranger.natural_explorer"));
    }

    @Override
    public void register() {
        ServerTickEvents.END_WORLD_TICK.register(RangerSkills::tickRains);
        // Drop rains (and their world references) when a world is closed.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RAINS.clear());
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(new AttributeBonus("ranger.natural_explorer", EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "ranger.volley" -> {
                float middle = (VOLLEY_ARROWS - 1) / 2.0F;
                for (int i = 0; i < VOLLEY_ARROWS; i++) {
                    ArrowEntity arrow = new ArrowEntity(world, player);
                    arrow.setVelocity(player, player.getPitch(),
                            player.getYaw() + (i - middle) * VOLLEY_SPREAD_DEGREES, 0, VOLLEY_SPEED, 1.0F);
                    arrow.setCritical(true);
                    arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                    world.spawnEntity(arrow);
                }
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
                        1.0F, 0.8F);
            }
            case "ranger.snare" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4), player);
                }
                effects(player, SoundEvents.BLOCK_GRASS_BREAK, ParticleTypes.ITEM_SLIME, 30);
            }
            case "ranger.rain_of_arrows" -> {
                HitResult hit = player.raycast(RAIN_RANGE, 1.0F, false);
                if (hit.getType() == HitResult.Type.MISS)
                    return false; // Nothing to aim at; no mana spent
                RAINS.add(new Rain(player.getUuid(), world, hit.getPos(), world.getTime() + RAIN_TICKS));
                world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS,
                        1.0F, 0.6F);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Drops this tick's arrows for every rain in the world. */
    private static void tickRains(ServerWorld world) {
        if (RAINS.isEmpty())
            return;
        long now = world.getTime();
        RAINS.removeIf(rain -> {
            if (rain.world() != world)
                return false;
            PlayerEntity owner = world.getPlayerByUuid(rain.owner());
            if (now >= rain.endTime() || owner == null)
                return true;
            for (int i = 0; i < RAIN_ARROWS_PER_TICK; i++) {
                double angle = world.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(world.random.nextDouble()) * RAIN_RADIUS;
                ArrowEntity arrow = new ArrowEntity(world, rain.center().x + Math.cos(angle) * r,
                        rain.center().y + RAIN_HEIGHT, rain.center().z + Math.sin(angle) * r);
                arrow.setOwner(owner);
                arrow.setVelocity(0, -1, 0, RAIN_SPEED, 2.0F);
                arrow.setDamage(RAIN_ARROW_DAMAGE / RAIN_SPEED); // Arrow damage scales with speed
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                arrow.addCommandTag(RAIN_TAG);
                world.spawnEntity(arrow);
            }
            return false;
        });
    }

    private static boolean isArrow(DamageSource source) {
        return source.getSource() instanceof PersistentProjectileEntity;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        if (!(killed instanceof Monster) || !isArrow(source))
            return 0;
        return player.distanceTo(killed) >= LONG_SHOT_DISTANCE ? BOW_KILL_BONUS_XP + LONG_SHOT_BONUS_XP
                : BOW_KILL_BONUS_XP;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        boolean arrow = isArrow(source);
        if (arrow && progress.hasPassive("ranger.sharpshooter")) {
            amount *= SHARPSHOOTER_BONUS;
        }
        if (progress.hasPassive("ranger.hunters_mark")) {
            // Checked before marking, so the hit that marks isn't boosted.
            if (target.hasStatusEffect(StatusEffects.GLOWING)) {
                amount *= MARKED_BONUS;
            }
            if (arrow) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, MARK_TICKS, 0), player);
            }
        }
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        // Your own Rain of Arrows doesn't hurt you.
        if (source.getSource() != null && source.getSource().getCommandTags().contains(RAIN_TAG)
                && source.getAttacker() == player) {
            return 0;
        }
        return amount;
    }
}
