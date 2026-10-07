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
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class MonkSkills extends ClassSkills {
    private static final int UNARMED_KILL_BONUS_XP = 3;

    private static final int FLURRY_HITS = 3;
    private static final float FLURRY_MULTIPLIER = 1.5F;
    /** A combo breaks if the next hit takes longer than this. */
    private static final long FLURRY_WINDOW_TICKS = 60;

    private static final double STUN_RADIUS = 4;
    private static final int STUN_TICKS = 80;

    private static final float DEFLECT_CHANCE = 0.5F;
    private static final float FALL_DAMAGE_MULTIPLIER = 0.25F;

    /** Launch speed; air drag brings it to a stop after roughly 8 blocks. */
    private static final double DASH_SPEED = 1.4;
    private static final double DASH_LIFT = 0.25;

    private static final UUID UNARMORED_SPEED_ID = UUID
            .nameUUIDFromBytes("dndclasses:monk.unarmored_movement".getBytes());
    private static final double UNARMORED_SPEED_BONUS = 0.15;
    /** How often the chestplate check runs. */
    private static final int UNARMORED_CHECK_TICKS = 5;

    private static final float QUIVERING_PALM_DAMAGE = 20.0F;
    private static final long QUIVERING_PALM_TICKS = 200;

    private static final int KI_SURGE_TICKS = 300;

    /** Flurry of Blows combo per player. */
    private record Combo(UUID target, int hits, long lastHit) {
    }

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    /** World time each player's Quivering Palm charge runs out. */
    private static final Map<UUID, Long> QUIVERING_PALM = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.MONK;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("monk.ki_surge", "Ki Surge", "Speed II, Haste II and Jump Boost II for 15 seconds.",
                        "minecraft:sugar", 9, 0, 1, 3),
                // Open Hand
                passive("monk.flurry_of_blows", "Flurry of Blows",
                        "Every 3rd hit in a row on the same target deals 50% more damage.", "minecraft:stick", 1, 2,
                        3, "monk.ki_surge"),
                active("monk.stunning_strike", "Stunning Strike",
                        "Mobs within 4 blocks get Slowness IV and Weakness II for 4 seconds.", "minecraft:bell", 3, 1,
                        2, 2, "monk.flurry_of_blows"),
                passive("monk.deflect_missiles", "Deflect Missiles", "50% chance to ignore projectile damage.",
                        "minecraft:arrow", 1, 2, 1, "monk.stunning_strike"),
                // Way of the Wind
                passive("monk.slow_fall", "Slow Fall", "Take 75% less fall damage.", "minecraft:feather", 1, 0, 3,
                        "monk.ki_surge"),
                active("monk.step_of_the_wind", "Step of the Wind", "Dash about 8 blocks the way you're looking.",
                        "minecraft:phantom_membrane", 3, 1, 0, 2, "monk.slow_fall"),
                passive("monk.unarmored_movement", "Unarmored Movement",
                        "Move 15% faster while not wearing a chestplate.", "minecraft:leather_boots", 1, 0, 1,
                        "monk.step_of_the_wind"),
                active("monk.quivering_palm", "Quivering Palm",
                        "Your next melee hit within 10 seconds deals 20 extra damage.", "minecraft:echo_shard", 9, 2,
                        1, 0, "monk.deflect_missiles", "monk.unarmored_movement"));
    }

    @Override
    public void register() {
        // Unarmored Movement depends on the chestplate, so it can't be a plain AttributeBonus.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UNARMORED_CHECK_TICKS != 0)
                return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                updateUnarmoredMovement(player);
            }
        });
    }

    /** The root power-up; called from {@code MonkPowerUpMixin} since PowerUpEffect has no monk case. */
    public static void kiSurge(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, KI_SURGE_TICKS, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, KI_SURGE_TICKS, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, KI_SURGE_TICKS, 1));
        if (player instanceof ServerPlayerEntity serverPlayer) {
            effects(serverPlayer, SoundEvents.BLOCK_BEACON_POWER_SELECT, ParticleTypes.END_ROD, 20);
        }
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "monk.stunning_strike" -> {
                for (LivingEntity mob : hostilesNear(player, STUN_RADIUS)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, STUN_TICKS, 3), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, STUN_TICKS, 1), player);
                    world.spawnParticles(ParticleTypes.CRIT, mob.getX(), mob.getEyeY(), mob.getZ(), 10, 0.3, 0.3,
                            0.3, 0.1);
                }
                effects(player, SoundEvents.BLOCK_BELL_USE, ParticleTypes.SWEEP_ATTACK, 6);
            }
            case "monk.step_of_the_wind" -> {
                Vec3d dash = player.getRotationVec(1.0F).multiply(DASH_SPEED);
                player.setVelocity(dash.x, Math.max(dash.y, 0) + DASH_LIFT, dash.z);
                player.velocityModified = true;
                player.fallDistance = 0;
                effects(player, SoundEvents.ENTITY_PHANTOM_FLAP, ParticleTypes.CLOUD, 20);
            }
            case "monk.quivering_palm" -> {
                QUIVERING_PALM.put(player.getUuid(), world.getTime() + QUIVERING_PALM_TICKS);
                effects(player, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, ParticleTypes.SCULK_SOUL, 20);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return killed instanceof Monster && isMonkStrike(player, source) ? UNARMED_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (!isMonkStrike(player, source))
            return amount;
        ServerWorld world = (ServerWorld) player.getWorld();
        long now = world.getTime();

        if (progress.hasPassive("monk.flurry_of_blows")) {
            Combo combo = COMBOS.get(player.getUuid());
            boolean continues = combo != null && combo.target().equals(target.getUuid())
                    && now - combo.lastHit() <= FLURRY_WINDOW_TICKS;
            int hits = continues ? combo.hits() + 1 : 1;
            if (hits >= FLURRY_HITS) {
                amount *= FLURRY_MULTIPLIER;
                hits = 0;
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(),
                        12, 0.3, 0.4, 0.3, 0.2);
            }
            COMBOS.put(player.getUuid(), new Combo(target.getUuid(), hits, now));
        }

        // Quivering Palm: one charged hit, spent on the next strike.
        Long palmUntil = QUIVERING_PALM.remove(player.getUuid());
        if (palmUntil != null && now <= palmUntil) {
            amount += QUIVERING_PALM_DAMAGE;
            world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0,
                    0, 0, 0);
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS,
                    1.0F, 1.2F);
        }
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        if (source.isOf(DamageTypes.FALL) && progress.hasPassive("monk.slow_fall")) {
            return amount * FALL_DAMAGE_MULTIPLIER;
        }
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) && progress.hasPassive("monk.deflect_missiles")
                && player.getRandom().nextFloat() < DEFLECT_CHANCE) {
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK,
                    SoundCategory.PLAYERS, 1.0F, 1.4F);
            return 0;
        }
        return amount;
    }

    /** A melee hit with the Monk Staff or a bare fist. */
    private static boolean isMonkStrike(PlayerEntity player, DamageSource source) {
        if (!source.isOf(DamageTypes.PLAYER_ATTACK) || source.getSource() != player)
            return false;
        return player.getMainHandStack().isEmpty() || player.getMainHandStack().isOf(ModItems.MONK_STAFF);
    }

    /** Adds or removes the Unarmored Movement speed bonus; also clears it after a class change. */
    private static void updateUnarmoredMovement(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null)
            return;
        boolean has = speed.getModifier(UNARMORED_SPEED_ID) != null;
        boolean wanted = Progression.classOf(player) == DndCharacter.MONK
                && player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                && Progression.hasPassive(player, "monk.unarmored_movement");
        if (wanted && !has) {
            speed.addTemporaryModifier(new EntityAttributeModifier(UNARMORED_SPEED_ID, "monk.unarmored_movement",
                    UNARMORED_SPEED_BONUS, EntityAttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!wanted && has) {
            speed.removeModifier(UNARMORED_SPEED_ID);
        }
    }
}
