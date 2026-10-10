package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import net.minecraft.util.Identifier;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RogueSkills extends ClassSkills {
    /** The root special; fired by {@code PowerUpEffect}. Short early on, today's 15 s at full rank. */
    public static final Ranks VANISH = Ranks.of("rogue.vanish")
            .seconds("Duration", 6, 9, 12, 15);

    /** Thief active: every projectile that would hit you misses for this long. */
    public static final Ranks DANGER_SENSE = Ranks.of("rogue.danger_sense")
            .seconds("Duration", 5, 6, 7, 8);

    /** Sideways push when Danger Sense dodges a projectile, in blocks per tick. */
    private static final double SIDESTEP_SPEED = 0.45;

    public static final String ASSASSIN = "rogue.assassin";
    public static final String THIEF = "rogue.thief";
    /** Assassinate: damage to a mob that isn't targeting you is multiplied by this. */
    public static final float ASSASSINATE_MULTIPLIER = 2.0F;
    /** Assassinate: how long after Vanish ends its guaranteed critical is still waiting. */
    public static final int ASSASSINATE_WINDOW_TICKS = 5 * 20;
    /** Fast Hands: added to Thieves' Tools (lockpicking) checks. */
    public static final int FAST_HANDS_BONUS = 3;

    /** Assassins with a Vanish critical waiting, and the server tick it lapses on. */
    private static final Map<UUID, Integer> ASSASSINATE_UNTIL = new HashMap<>();

    private static final int STEALTH_KILL_BONUS_XP = 4;
    private static final int SHADOWSTEP_RANGE = 12;

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.ROGUE;
    }

    @Override
    public List<String> subclassIds() {
        return List.of("rogue.assassin", "rogue.thief");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("rogue.vanish", "Vanish", "Turn invisible for a few seconds, longer with each rank.", "minecraft:fermented_spider_eye", 9,
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
                active("rogue.danger_sense", "Danger Sense",
                        "For a few seconds, arrows and other projectiles that would hit you miss as you sidestep.",
                        "minecraft:phantom_membrane", 4, 1, 1, 2, "rogue.smoke_bomb"),
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
    public void register() {
        // Fast Hands, on the sheet so the lockpick roll (and its HUD and log) shows it. Ignoring class
        // restrictions when attuning comes with the attunement card.
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "subclass/rogue_thief"), (player, c) -> {
            if (Progression.classOf(player) == DndCharacter.ROGUE && Progression.current(player).hasSubclass(THIEF)) {
                c.skillBonus(Skill.THIEVES_TOOLS, FAST_HANDS_BONUS, "Fast Hands");
            }
        });
        // Assassinate: the first melee swing out of Vanish is a critical.
        AttackRolls.registerAutoCrit((player, target) -> {
            Integer until = ASSASSINATE_UNTIL.get(player.getUuid());
            if (until == null || player.getServer() == null)
                return null;
            ASSASSINATE_UNTIL.remove(player.getUuid());
            if (player.getServer().getTicks() > until || Progression.classOf(player) != DndCharacter.ROGUE
                    || !Progression.current(player).hasSubclass(ASSASSIN))
                return null;
            return "Assassinate";
        });
    }

    /**
     * The root special, Vanish; called from the ROGUE case in {@code PowerUpEffect}. An Assassin's next
     * melee swing, while invisible or up to 5 s after, is a critical (Assassinate).
     */
    public static void vanish(PlayerEntity player) {
        int ticks = VANISH.ticks(player, "Duration");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, ticks, 0));
        if (player.getServer() != null && Progression.classOf(player) == DndCharacter.ROGUE
                && Progression.current(player).hasSubclass(ASSASSIN)) {
            ASSASSINATE_UNTIL.put(player.getUuid(), player.getServer().getTicks() + ticks + ASSASSINATE_WINDOW_TICKS);
        }
    }

    @Override
    public void forget(ServerPlayerEntity player) {
        int now = player.getServer().getTicks();
        ASSASSINATE_UNTIL.values().removeIf(t -> now > t);
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
            case "rogue.danger_sense" -> {
                player.addStatusEffect(new StatusEffectInstance(ModEffects.DANGER_SENSE,
                        DANGER_SENSE.ticks(player, "Duration"), 0, false, false, true));
                effects(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, ParticleTypes.CLOUD, 20);
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

    /**
     * Danger Sense dodged a projectile that was about to hit: a sideways nudge, a whoosh and a puff of
     * smoke where it would have landed. Called once per projectile from {@code ProjectileEntityMixin}.
     */
    public static void sidestep(PlayerEntity player, Entity projectile) {
        if (!(player.getWorld() instanceof ServerWorld world))
            return;
        Vec3d flight = projectile.getVelocity();
        Vec3d side = new Vec3d(-flight.z, 0, flight.x);
        if (side.lengthSquared() < 1.0E-6) {
            side = player.getRotationVec(1.0F).crossProduct(new Vec3d(0, 1, 0));
        }
        side = side.normalize().multiply(player.getRandom().nextBoolean() ? SIDESTEP_SPEED : -SIDESTEP_SPEED);
        player.addVelocity(side.x, 0.1, side.z);
        player.velocityModified = true;
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.8F, 1.6F);
        world.spawnParticles(ParticleTypes.CLOUD, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2,
                0.2, 0.2, 0.02);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 6,
                0.15, 0.15, 0.15, 0.01);
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
        // Assassinate: a mob that isn't after you never sees it coming.
        if (melee && progress.hasSubclass(ASSASSIN) && target instanceof MobEntity mob && mob.getTarget() != player) {
            amount *= ASSASSINATE_MULTIPLIER;
        }
        if (melee && progress.hasPassive("rogue.poisoned_blades")) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), player);
        }
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        // Projectiles already fly through a Danger Sense Rogue (ProjectileEntityMixin); this catches the rest.
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) && player.hasStatusEffect(ModEffects.DANGER_SENSE)) {
            return 0;
        }
        return source.isOf(DamageTypes.FALL) && progress.hasPassive("rogue.light_feet") ? amount * 0.5F : amount;
    }
}
