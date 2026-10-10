package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class WarlockSkills extends ClassSkills {
    /**
     * The root special; fired by {@code PowerUpEffect} and ticked by {@code Warlock}.
     * Short and close early on, longer than the old 20 s / 5 blocks at full rank.
     */
    public static final Ranks FIRE_BREATH = Ranks.of("warlock.fire_breath")
            .seconds("Duration", 8, 12, 16, 20)
            .amount("Reach", "blocks", 3, 4, 5, 7)
            .amount("Damage", "", 1, 2, 2, 3)
            .seconds("Burn", 2, 2, 4, 4);

    /** Read by {@code Warlock} for the fireball cooldown. */
    public static final String INFERNAL_FIREBALLS = "warlock.infernal_fireballs";

    public static final String FIEND = "warlock.fiend";
    public static final String GREAT_OLD_ONE = "warlock.great_old_one";
    /** Dark One's Own Luck: at most one reroll this often. */
    public static final int DARK_ONES_LUCK_COOLDOWN_TICKS = 2 * 60 * 20;
    /** Entropic Ward: at most one projectile turned aside this often. */
    public static final int ENTROPIC_WARD_COOLDOWN_TICKS = 60 * 20;

    /** Server tick each player's Dark One's Own Luck / Entropic Ward is ready again. */
    private static final Map<UUID, Integer> LUCK_READY = new HashMap<>();
    private static final Map<UUID, Integer> WARD_READY = new HashMap<>();

    private static final int FIRE_KILL_BONUS_XP = 2;
    private static final int NETHER_KILL_BONUS_XP = 1;

    private static final float HELLFIRE_MULTIPLIER = 1.25F;
    private static final double ELDRITCH_BLAST_RANGE = 20;
    private static final float ELDRITCH_BLAST_DAMAGE = 8.0F;
    private static final double HEX_RANGE = 20;
    private static final int HEX_TICKS = 15 * 20;
    private static final float HEX_MULTIPLIER = 1.3F;
    private static final float RAIN_WARD_MULTIPLIER = 0.5F;
    private static final float BLESSING_ABSORPTION = 4.0F; // 2 hearts
    private static final float BLESSING_MAX_ABSORPTION = 8.0F; // 4 hearts
    private static final double HELLGATE_RADIUS = 6;
    private static final int HELLGATE_TICKS = 10 * 20;
    private static final float HELLGATE_DAMAGE = 2.0F; // Per second
    private static final int HELLGATE_BURN_SECONDS = 3;

    /** Hexed mob -> who hexed it and when it wears off. */
    private static final Map<UUID, Hex> HEXES = new HashMap<>();
    private static final List<Hellgate> HELLGATES = new ArrayList<>();

    private record Hex(UUID owner, long endTime) {
    }

    private record Hellgate(UUID owner, World world, Vec3d center, long endTime) {
    }

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.WARLOCK;
    }

    @Override
    public List<String> subclassIds() {
        return List.of("warlock.fiend", "warlock.great_old_one");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("warlock.fire_breath", "Fire Breath", "Breathe a beam of fire that burns and hurts the mobs in front of you.", "minecraft:blaze_powder",
                        9, 0, 1, 3),
                // Fiend
                passive("warlock.hellfire", "Hellfire", "Burning targets take 25% more damage from you.",
                        "minecraft:fire_charge", 1, 2, 3, "warlock.fire_breath"),
                active("warlock.eldritch_blast", "Eldritch Blast",
                        "An instant beam that deals 8 damage to the first mob within 20 blocks.",
                        "minecraft:ender_eye", 3, 1, 2, 2, "warlock.hellfire"),
                passive(INFERNAL_FIREBALLS, "Infernal Fireballs", "Your fireball cooldown is halved.",
                        "minecraft:blaze_rod", 1, 2, 1, "warlock.eldritch_blast"),
                // Great Old One
                passive("warlock.rain_ward", "Rain Ward", "Water and rain hurt you half as much.",
                        "minecraft:turtle_helmet", 1, 0, 3, "warlock.fire_breath"),
                active("warlock.hex", "Hex",
                        "The mob you're looking at (up to 20 blocks) gets Weakness II, Slowness II and Glowing, "
                                + "and takes 30% more damage from you for 15 seconds.",
                        "minecraft:fermented_spider_eye", 4, 1, 0, 2, "warlock.rain_ward"),
                passive("warlock.dark_ones_blessing", "Eldritch Hunger",
                        "Gain 2 absorption hearts per kill, up to 4.", "minecraft:golden_apple", 1, 0, 1,
                        "warlock.hex"),
                active("warlock.hellgate", "Hellgate",
                        "A 6-block ring of fire burns mobs inside it for 10 seconds; Strength I for you.",
                        "minecraft:netherrack", 9, 2, 1, 0, INFERNAL_FIREBALLS, "warlock.dark_ones_blessing"));
    }

    @Override
    public void register() {
        // Dark One's Own Luck: a failed d20 roll of a Fiend Warlock's is rolled again, once every 2 minutes.
        D20.registerReroll((player, kind, outcome) -> {
            if (!(player instanceof ServerPlayerEntity warlock) || Progression.classOf(player) != DndCharacter.WARLOCK
                    || !Progression.current(player).hasSubclass(FIEND))
                return null;
            int now = warlock.getServer().getTicks();
            if (now < LUCK_READY.getOrDefault(warlock.getUuid(), 0))
                return null;
            LUCK_READY.put(warlock.getUuid(), now + DARK_ONES_LUCK_COOLDOWN_TICKS);
            warlock.sendMessage(Text.literal("Dark One's Own Luck: you roll again").formatted(Formatting.DARK_RED),
                    false);
            warlock.getWorld().playSound(null, warlock.getBlockPos(), SoundEvents.ENTITY_BLAZE_AMBIENT,
                    SoundCategory.PLAYERS, 0.6F, 0.7F);
            return "Dark One's Own Luck";
        });
        // Don't carry hexes or fire rings (and their worlds) into the next singleplayer session
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HEXES.clear();
            HELLGATES.clear();
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            long now = world.getTime();
            if (!HEXES.isEmpty() && now % 20 == 0) {
                HEXES.values().removeIf(hex -> now >= hex.endTime());
            }
            HELLGATES.removeIf(gate -> {
                if (gate.world() != world)
                    return false; // Ticked by its own world
                if (now >= gate.endTime())
                    return true;
                if (now % 5 == 0)
                    ringParticles(world, gate.center());
                if (now % 20 == 0)
                    burnInside(world, gate);
                return false;
            });
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "warlock.eldritch_blast" -> {
                LivingEntity target = lookTarget(player, ELDRITCH_BLAST_RANGE);
                if (target == null)
                    return false;
                beamParticles(world, player.getEyePos(), target.getBoundingBox().getCenter());
                target.damage(world.getDamageSources().indirectMagic(player, player), ELDRITCH_BLAST_DAMAGE);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                        SoundCategory.PLAYERS, 1.0F, 1.4F);
            }
            case "warlock.hex" -> {
                LivingEntity target = lookTarget(player, HEX_RANGE);
                if (target == null)
                    return false;
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, HEX_TICKS, 1), player);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, HEX_TICKS, 1), player);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, HEX_TICKS, 0), player);
                HEXES.put(target.getUuid(), new Hex(player.getUuid(), world.getTime() + HEX_TICKS));
                world.spawnParticles(ParticleTypes.WITCH, target.getX(), target.getBodyY(0.5), target.getZ(), 30,
                        0.4, 0.6, 0.4, 0.05);
                world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO,
                        SoundCategory.PLAYERS, 1.0F, 0.8F);
            }
            case "warlock.hellgate" -> {
                HELLGATES.add(new Hellgate(player.getUuid(), world, player.getPos(),
                        world.getTime() + HELLGATE_TICKS));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, HELLGATE_TICKS, 0));
                ringParticles(world, player.getPos());
                effects(player, SoundEvents.ENTITY_BLAZE_SHOOT, ParticleTypes.LAVA, 20);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        int xp = source.isIn(DamageTypeTags.IS_FIRE) ? FIRE_KILL_BONUS_XP : 0;
        if (player.getWorld().getRegistryKey() == World.NETHER)
            xp += NETHER_KILL_BONUS_XP;
        return xp;
    }

    @Override
    public void onKill(ServerPlayerEntity player, ClassProgress progress, LivingEntity killed, DamageSource source) {
        HEXES.remove(killed.getUuid());
        if (progress.hasPassive("warlock.dark_ones_blessing")) {
            float absorption = player.getAbsorptionAmount();
            if (absorption < BLESSING_MAX_ABSORPTION)
                player.setAbsorptionAmount(Math.min(BLESSING_MAX_ABSORPTION, absorption + BLESSING_ABSORPTION));
        }
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (progress.hasPassive("warlock.hellfire") && target.isOnFire())
            amount *= HELLFIRE_MULTIPLIER;
        Hex hex = HEXES.get(target.getUuid());
        if (hex != null && hex.owner().equals(player.getUuid()) && player.getWorld().getTime() < hex.endTime())
            amount *= HEX_MULTIPLIER;
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        if (progress.hasPassive("warlock.rain_ward") && source.isOf(ModDamageTypes.WARLOCK_WET_DAMAGE_SOURCE))
            return amount * RAIN_WARD_MULTIPLIER;
        return amount;
    }

    /**
     * Entropic Ward: whether a Great Old One Warlock turns aside the projectile about to hit them, using
     * up the ward for 60 seconds. Called from {@code ProjectileEntityMixin} once per projectile that would
     * really hit (server only); the projectile then can't hit the player at all.
     */
    public static boolean entropicWard(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity warlock) || Progression.classOf(player) != DndCharacter.WARLOCK
                || !Progression.current(player).hasSubclass(GREAT_OLD_ONE))
            return false;
        int now = warlock.getServer().getTicks();
        if (now < WARD_READY.getOrDefault(warlock.getUuid(), 0))
            return false;
        WARD_READY.put(warlock.getUuid(), now + ENTROPIC_WARD_COOLDOWN_TICKS);
        warlock.sendMessage(Text.literal("Entropic Ward turns the shot aside").formatted(Formatting.DARK_PURPLE),
                true);
        DnDClasses.LOGGER.debug("[Subclass] Entropic Ward: {} dodged a projectile", warlock.getEntityName());
        return true;
    }

    @Override
    public void forget(ServerPlayerEntity player) {
        int now = player.getServer().getTicks();
        LUCK_READY.values().removeIf(t -> now >= t);
        WARD_READY.values().removeIf(t -> now >= t);
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

    private static void beamParticles(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d step = to.subtract(from);
        int points = (int) (step.length() * 3);
        for (int i = 1; i <= points; i++) {
            Vec3d pos = from.add(step.multiply(i / (double) points));
            world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void ringParticles(ServerWorld world, Vec3d center) {
        int points = 48;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            world.spawnParticles(ParticleTypes.FLAME, center.x + Math.cos(angle) * HELLGATE_RADIUS, center.y + 0.1,
                    center.z + Math.sin(angle) * HELLGATE_RADIUS, 2, 0.05, 0.3, 0.05, 0.01);
        }
    }

    /** Sets everything inside the ring alight, with fire damage credited to the caster. */
    private static void burnInside(ServerWorld world, Hellgate gate) {
        Entity owner = world.getEntity(gate.owner());
        DamageSource source = new DamageSource(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.IN_FIRE), owner);
        Vec3d c = gate.center();
        Box box = new Box(c, c).expand(HELLGATE_RADIUS, 3, HELLGATE_RADIUS);
        for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof Tameable t && gate.owner().equals(t.getOwnerUuid()))
                        && e.squaredDistanceTo(c.x, e.getY(), c.z) <= HELLGATE_RADIUS * HELLGATE_RADIUS)) {
            mob.setOnFireFor(HELLGATE_BURN_SECONDS);
            mob.damage(source, HELLGATE_DAMAGE);
        }
    }
}
