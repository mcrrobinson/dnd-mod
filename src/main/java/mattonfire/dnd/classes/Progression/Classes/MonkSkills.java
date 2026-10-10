package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.MonkHandler;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

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

    /**
     * Flurry Rush, the power-up: blink to the mob in the crosshair and the
     * hostiles around it and hit each in turn, then blink back.
     */
    public static final Ranks FLURRY_RUSH = Ranks.of("monk.flurry_rush")
            .amount("Hits", "", 3, 5, 7, 10)
            .amount("Targets", "", 1, 2, 3, 5)
            .amount("Damage", "per hit", 3, 3, 4, 4);
    /** How far away the first target can be. */
    private static final double RUSH_RANGE = 16;
    /** Further targets are hostiles within this many blocks of the first. */
    private static final double RUSH_CHAIN_RADIUS = 8;
    private static final int RUSH_HIT_INTERVAL_TICKS = 3;
    /** Each blink lands this far round the target from the last one. */
    private static final double RUSH_ANGLE_STEP = Math.toRadians(137.5);

    /** A Flurry Rush in progress: the hits still to land, in order, and where to come back to. */
    private static final class Rush {
        final RegistryKey<World> world;
        final Vec3d start;
        final float yaw;
        final float pitch;
        final List<UUID> hits;
        final float damage;
        int next = 0;
        long nextTick;

        Rush(RegistryKey<World> world, Vec3d start, float yaw, float pitch, List<UUID> hits, float damage,
                long nextTick) {
            this.world = world;
            this.start = start;
            this.yaw = yaw;
            this.pitch = pitch;
            this.hits = hits;
            this.damage = damage;
            this.nextTick = nextTick;
        }
    }

    private static final Map<UUID, Rush> RUSHES = new HashMap<>();

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
    public List<String> subclassIds() {
        return List.of("monk.open_hand", "monk.drunken_master");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("monk.flurry_rush", "Flurry Rush",
                        "Blink between the mob you're looking at and hostiles near it, landing a rapid chain of hits, then blink back.",
                        "minecraft:blaze_rod", 9, 0, 1, 3),
                // Way of the Open Hand
                passive("monk.flurry_of_blows", "Flurry of Blows",
                        "Every 3rd hit in a row on the same target deals 50% more damage.", "minecraft:stick", 1, 2,
                        3, "monk.flurry_rush"),
                active("monk.stunning_strike", "Stunning Strike",
                        "Mobs within 4 blocks get Slowness IV and Weakness II for 4 seconds.", "minecraft:bell", 3, 1,
                        2, 2, "monk.flurry_of_blows"),
                passive("monk.deflect_missiles", "Deflect Missiles", "50% chance to ignore projectile damage.",
                        "minecraft:arrow", 1, 2, 1, "monk.stunning_strike"),
                // Way of the Drunken Master
                passive("monk.slow_fall", "Slow Fall", "Take 75% less fall damage.", "minecraft:feather", 1, 0, 3,
                        "monk.flurry_rush"),
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (RUSHES.isEmpty())
                return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Rush rush = RUSHES.get(player.getUuid());
                if (rush != null)
                    tickRush(player, rush);
            }
            // Players who left mid-rush.
            RUSHES.keySet().removeIf(id -> server.getPlayerManager().getPlayer(id) == null);
        });
    }

    /**
     * The root power-up, Flurry Rush; called from the MONK case in
     * {@code PowerUpEffect}.
     *
     * @return false if there's nothing in the crosshair, so no mana is spent
     */
    public static boolean flurryRush(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || RUSHES.containsKey(player.getUuid()))
            return false;
        ServerWorld world = (ServerWorld) player.getWorld();
        LivingEntity first = lookTarget(player, RUSH_RANGE);
        if (first == null) {
            serverPlayer.sendMessage(Text.literal("No target in sight").formatted(Formatting.GRAY), true);
            return false;
        }

        int maxTargets = FLURRY_RUSH.getInt(player, "Targets");
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(first);
        world.getEntitiesByClass(LivingEntity.class, first.getBoundingBox().expand(RUSH_CHAIN_RADIUS),
                e -> e != first && e instanceof Monster && e.isAlive())
                .stream()
                .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(first)))
                .limit(maxTargets - 1)
                .forEach(targets::add);

        // The hits go to each target in a row (so Flurry of Blows can combo), the first gets any extra.
        int hits = FLURRY_RUSH.getInt(player, "Hits");
        List<UUID> order = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            int count = hits / targets.size() + (i < hits % targets.size() ? 1 : 0);
            for (int j = 0; j < count; j++)
                order.add(targets.get(i).getUuid());
        }

        // Armor weakens the rush like any other Monk strike; unarmored is full damage.
        float damage = (float) (FLURRY_RUSH.get(player, "Damage")
                * MonkHandler.damageMultiplier(player.getArmor()) / MonkHandler.damageMultiplier(0));
        RUSHES.put(player.getUuid(), new Rush(world.getRegistryKey(), player.getPos(), player.getYaw(),
                player.getPitch(), order, damage, world.getTime()));

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR,
                SoundCategory.PLAYERS, 1.0F, 1.4F);
        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getBodyY(0.5), player.getZ(), 15, 0.3, 0.5,
                0.3, 0.05);
        tickRush(serverPlayer, RUSHES.get(player.getUuid()));
        return true;
    }

    private static void tickRush(ServerPlayerEntity player, Rush rush) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (!player.isAlive() || world.getRegistryKey() != rush.world) {
            RUSHES.remove(player.getUuid());
            return;
        }
        if (world.getTime() < rush.nextTick)
            return;
        rush.nextTick = world.getTime() + RUSH_HIT_INTERVAL_TICKS;

        // The next target still alive; dead ones lose their remaining hits.
        LivingEntity target = null;
        while (rush.next < rush.hits.size() && target == null) {
            if (world.getEntity(rush.hits.get(rush.next)) instanceof LivingEntity living && living.isAlive()
                    && living.squaredDistanceTo(rush.start) <= (RUSH_RANGE + RUSH_CHAIN_RADIUS)
                            * (RUSH_RANGE + RUSH_CHAIN_RADIUS)) {
                target = living;
            } else {
                rush.next++;
            }
        }
        if (target == null) {
            endRush(player, rush);
            return;
        }

        Vec3d from = player.getPos();
        blinkTo(player, target, rush.next);
        trail(world, from, player.getPos());

        player.swingHand(Hand.MAIN_HAND, true);
        target.timeUntilRegen = 0; // A hit every 3 ticks, faster than the usual half-second of invulnerability.
        target.damage(player.getDamageSources().playerAttack(player), rush.damage);
        player.fallDistance = 0;
        rush.next++;

        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0,
                0, 0);
        world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.4,
                0.3, 0.3);
        float pitch = 0.8F + 0.8F * rush.next / rush.hits.size();
        boolean last = rush.next >= rush.hits.size();
        world.playSound(null, target.getBlockPos(),
                last ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                SoundCategory.PLAYERS, 1.0F, pitch);
        if (last) {
            world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0,
                    0, 0, 0);
            rush.nextTick = world.getTime() + RUSH_HIT_INTERVAL_TICKS * 2L; // A beat before blinking back.
        }
    }

    /** Puts the player next to the target, facing it, on a different side each hit. */
    private static void blinkTo(ServerPlayerEntity player, LivingEntity target, int hit) {
        ServerWorld world = (ServerWorld) player.getWorld();
        double distance = target.getWidth() / 2 + player.getWidth() / 2 + 0.7;
        Vec3d spot = null;
        // Try a few sides round the target for one with room to stand.
        for (int attempt = 0; attempt < 6 && spot == null; attempt++) {
            double angle = (hit + attempt) * RUSH_ANGLE_STEP;
            Vec3d candidate = new Vec3d(target.getX() + Math.cos(angle) * distance, target.getY(),
                    target.getZ() + Math.sin(angle) * distance);
            if (world.isSpaceEmpty(player, player.getBoundingBox().offset(candidate.subtract(player.getPos()))))
                spot = candidate;
        }
        if (spot == null)
            spot = player.getPos(); // Boxed in: strike from where you are.
        Vec3d look = target.getPos().add(0, target.getHeight() / 2, 0).subtract(spot.add(0, player.getStandingEyeHeight(), 0));
        float yaw = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        player.networkHandler.requestTeleport(spot.x, spot.y, spot.z, yaw, pitch);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
    }

    private static void endRush(ServerPlayerEntity player, Rush rush) {
        RUSHES.remove(player.getUuid());
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d from = player.getPos();
        player.networkHandler.requestTeleport(rush.start.x, rush.start.y, rush.start.z, rush.yaw, rush.pitch);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0;
        trail(world, from, rush.start);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS,
                1.0F, 1.2F);
        world.spawnParticles(ParticleTypes.CLOUD, rush.start.x, rush.start.y + 1, rush.start.z, 15, 0.3, 0.5, 0.3,
                0.05);
    }

    /** A line of particles where the player blinked. */
    private static void trail(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d step = to.subtract(from);
        int points = (int) Math.min(step.length() * 3, 60);
        for (int i = 0; i <= points; i++) {
            Vec3d pos = from.add(step.multiply(points == 0 ? 0 : i / (double) points)).add(0, 1, 0);
            world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.05, 0.1, 0.05, 0);
        }
    }

    /** Whether the player is mid Flurry Rush. */
    public static boolean isRushing(PlayerEntity player) {
        return RUSHES.containsKey(player.getUuid());
    }

    /** The first living non-player, non-pet in the crosshair within range, not behind blocks. */
    private static LivingEntity lookTarget(PlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(range));
        HitResult block = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        if (block.getType() != HitResult.Type.MISS)
            end = block.getPos();
        Box area = player.getBoundingBox().stretch(end.subtract(eye)).expand(1.0);
        UUID owner = player.getUuid();
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, area,
                e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof Tameable t && owner.equals(t.getOwnerUuid())),
                eye.squaredDistanceTo(end));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
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
        if (RUSHES.containsKey(player.getUuid()) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return 0;
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

    @Override
    public void forget(ServerPlayerEntity player) {
        COMBOS.remove(player.getUuid());
        QUIVERING_PALM.remove(player.getUuid());
        RUSHES.remove(player.getUuid());
    }
}
