package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.spawnSummon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Goals.FollowSummonerGoal;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Necromancer tree. Summons join the same scoreboard ally team as the root's
 * (named after the player's UUID), which is also how kills by summons are
 * traced back to their necromancer.
 */
public class NecromancerSkills extends ClassSkills {
    /** Tag on every Necromancer summon (special and skills), used to tidy up the ally team. */
    public static final String SUMMON_TAG = "dndclasses.necromancer_summon";

    /**
     * The root special; fired by {@code PowerUpEffect.spawnUndead}. Each rank raises more and
     * stronger undead (see {@link #raisedUndead}) for longer, and the last adds a Bone Wyvern.
     */
    public static final Ranks RAISE_DEAD = Ranks.of("necromancer.raise_dead")
            .amount("Undead", "", 2, 3, 4, 5, 5)
            .text("Kinds", "zombie, skeleton", "zombie, skeleton, spider", "husk, stray, spider, zombie",
                    "2 wither skeletons, husk, stray, spider", "2 wither skeletons, husk, stray, spider, Bone Wyvern")
            .seconds("Duration", 10, 12, 14, 16, 20)
            .levels(3, 5, 7, 10);

    /** Rank of Raise Dead that adds a Bone Wyvern (one at a time). */
    public static final int BONE_WYVERN_RANK = 5;

    /** The undead Raise Dead raises at a rank, in order; as long as the "Undead" count. */
    public static List<EntityType<? extends HostileEntity>> raisedUndead(int rank) {
        return switch (rank) {
            case 1 -> List.of(EntityType.ZOMBIE, EntityType.SKELETON);
            case 2 -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER);
            case 3 -> List.of(EntityType.HUSK, EntityType.STRAY, EntityType.SPIDER, EntityType.ZOMBIE);
            default -> List.of(EntityType.WITHER_SKELETON, EntityType.WITHER_SKELETON, EntityType.HUSK,
                    EntityType.STRAY, EntityType.SPIDER);
        };
    }

    private static final int SUMMON_KILL_XP = 3;
    private static final int WITHER_KILL_XP = 1;

    private static final int SUMMON_LIFETIME_TICKS = 1200;
    private static final int ARCHER_COUNT = 2;
    private static final int ARMY_SIZE = 6; // Alternating zombies and skeletons
    private static final double GRAVE_PACT_RANGE = 48;

    private static final float LIFE_DRAIN_FRACTION = 0.1F;

    private static final int CLOUD_TICKS = 120;
    private static final double CLOUD_RADIUS = 4;
    private static final int CLOUD_WITHER_TICKS = 60;

    // The class's own wither-on-hit is Wither I for 5 ticks, too short to ever deal damage.
    private static final int EMBRACE_WITHER_TICKS = 60;

    /** A wither cloud left by Wither Cloud. */
    private record Cloud(RegistryKey<World> world, Vec3d center, UUID owner, long endTime) {
    }

    private static final List<Cloud> CLOUDS = new ArrayList<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.NECROMANCER;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("necromancer.raise_dead", "Raise Dead",
                        "Undead rise to fight for you. More and stronger ones with each rank, up to a Bone Wyvern.",
                        "minecraft:rotten_flesh", 9, 0, 1, 3),
                // Bone
                passive("necromancer.bone_armor", "Bone Armor", "+3 armor.", "minecraft:bone_block", 1, 2, 3,
                        "necromancer.raise_dead"),
                active("necromancer.skeletal_archers", "Skeletal Archers",
                        "Two skeleton archers fight for you for 60 seconds.", "minecraft:bow", 5, 1, 2, 2,
                        "necromancer.bone_armor"),
                passive("necromancer.grave_pact", "Grave Pact", "Your summons get Strength and Resistance.",
                        "minecraft:skeleton_skull", 1, 2, 1, "necromancer.skeletal_archers"),
                // Blight
                passive("necromancer.life_drain", "Life Drain", "Heal 10% of the melee damage you deal.",
                        "minecraft:fermented_spider_eye", 1, 0, 3, "necromancer.raise_dead"),
                active("necromancer.wither_cloud", "Wither Cloud",
                        "Leave a wither cloud for 6 seconds that withers mobs but not you.",
                        "minecraft:wither_rose", 4, 1, 0, 2, "necromancer.life_drain"),
                passive("necromancer.deaths_embrace", "Death's Embrace",
                        "Your hits apply Wither II for 3 seconds.", "minecraft:wither_skeleton_skull", 1, 0, 1,
                        "necromancer.wither_cloud"),
                active("necromancer.army_of_the_dead", "Army of the Dead",
                        "Six undead fight for you for 60 seconds.", "minecraft:zombie_head", 9, 2, 1, 0,
                        "necromancer.grave_pact", "necromancer.deaths_embrace"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(new AttributeBonus("necromancer.bone_armor", EntityAttributes.GENERIC_ARMOR, 3,
                EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public void register() {
        // Death's Embrace: a longer, stronger wither on hit (the stronger effect wins over the base one).
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof LivingEntity target
                    && Progression.classOf(player) == DndCharacter.NECROMANCER
                    && Progression.hasPassive(player, "necromancer.deaths_embrace")) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, EMBRACE_WITHER_TICKS, 1),
                        player);
            }
            return ActionResult.PASS;
        });

        // XP the base kill XP doesn't give: kills by a summon, or by wither after you hit the mob.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Monster))
                return;
            ServerPlayerEntity summoner = summonerOf(source.getAttacker());
            if (summoner != null) {
                Progression.addXp(summoner, SUMMON_KILL_XP);
            } else if (source.isOf(DamageTypes.WITHER)
                    && entity.getPrimeAdversary() instanceof ServerPlayerEntity player
                    && Progression.classOf(player) == DndCharacter.NECROMANCER) {
                Progression.addXp(player, WITHER_KILL_XP);
            }
        });

        // Take dead or vanished summons off the ally team so the scoreboard doesn't fill up.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(SUMMON_TAG) && entity.getRemovalReason() != null
                    && entity.getRemovalReason().shouldDestroy()) {
                Scoreboard scoreboard = world.getScoreboard();
                Team team = scoreboard.getPlayerTeam(entity.getUuidAsString());
                scoreboard.clearPlayerTeam(entity.getUuidAsString());
                // The last summon gone: drop the team too, it's made again on the next summon.
                if (team != null && team.getPlayerList().isEmpty()) {
                    scoreboard.removeTeam(team);
                }
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(NecromancerSkills::tickClouds);
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "necromancer.skeletal_archers" -> {
                for (int i = 0; i < ARCHER_COUNT; i++) {
                    summon(player, EntityType.SKELETON.create(world), Items.BOW, i * Math.PI);
                }
                effects(player, SoundEvents.ENTITY_SKELETON_AMBIENT, ParticleTypes.SOUL, 20);
            }
            case "necromancer.wither_cloud" -> {
                CLOUDS.add(new Cloud(world.getRegistryKey(), player.getPos(), player.getUuid(),
                        world.getTime() + CLOUD_TICKS));
                effects(player, SoundEvents.ENTITY_WITHER_SHOOT, ParticleTypes.LARGE_SMOKE, 30);
            }
            case "necromancer.army_of_the_dead" -> {
                for (int i = 0; i < ARMY_SIZE; i++) {
                    double angle = i * Math.PI * 2 / ARMY_SIZE;
                    if (i % 2 == 0) {
                        summon(player, EntityType.ZOMBIE.create(world), Items.IRON_SWORD, angle);
                    } else {
                        summon(player, EntityType.SKELETON.create(world), Items.BOW, angle);
                    }
                }
                effects(player, SoundEvents.ENTITY_WITHER_SPAWN, ParticleTypes.SOUL, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return killed.hasStatusEffect(StatusEffects.WITHER) ? WITHER_KILL_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (progress.hasPassive("necromancer.life_drain") && source.getSource() == player) {
            player.heal(amount * LIFE_DRAIN_FRACTION);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (!progress.hasPassive("necromancer.grave_pact"))
            return;
        // Refreshed every second, so it also covers the root's summons and stops when unequipped.
        String team = player.getUuidAsString();
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(GRAVE_PACT_RANGE),
                mob -> mob.getScoreboardTeam() != null && team.equals(mob.getScoreboardTeam().getName()))) {
            gravePact(mob);
        }
    }

    private static void gravePact(LivingEntity mob) {
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0));
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0));
    }

    /** Sets up an undead ally like the root's summons, but timed by {@code spawnSummon}. */
    private static void summon(ServerPlayerEntity player, HostileEntity mob, Item weapon, double angle) {
        if (mob == null)
            return;
        ServerWorld world = (ServerWorld) player.getWorld();
        mob.refreshPositionAndAngles(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                player.getZ() + Math.sin(angle) * 1.5, world.random.nextFloat() * 360F, 0F);
        // Skeletons pick bow or melee attacks when their main hand changes.
        mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0);
        mob.setPersistent();
        // Don't burn in daylight.
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, SUMMON_LIFETIME_TICKS, 0));

        MobEntityAccessor accessor = (MobEntityAccessor) mob;
        accessor.getTargetSelector().add(2, new ActiveTargetGoal<LivingEntity>(mob, LivingEntity.class, 10, true,
                false, entity -> entity instanceof Monster));
        accessor.getGoalSelector().add(3, new FollowSummonerGoal(mob, player, 1.2D, 5.0F, 10.0F));

        world.getScoreboard().addPlayerToTeam(mob.getUuidAsString(), allyTeam(world, player));
        mob.addCommandTag(SUMMON_TAG);
        if (Progression.hasPassive(player, "necromancer.grave_pact")) {
            gravePact(mob);
        }
        spawnSummon(world, mob, SUMMON_LIFETIME_TICKS);
        world.spawnParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
    }

    /** The scoreboard team the root's summons use, named after the player's UUID. */
    public static Team allyTeam(ServerWorld world, PlayerEntity player) {
        Scoreboard scoreboard = world.getScoreboard();
        Team team = scoreboard.getTeam(player.getUuidAsString());
        return team != null ? team : scoreboard.addTeam(player.getUuidAsString());
    }

    /** The online necromancer whose ally team this entity is on, if any. */
    private static ServerPlayerEntity summonerOf(Entity attacker) {
        if (!(attacker instanceof MobEntity mob) || mob.getScoreboardTeam() == null
                || !(mob.getWorld() instanceof ServerWorld world))
            return null;
        UUID owner;
        try {
            owner = UUID.fromString(mob.getScoreboardTeam().getName());
        } catch (IllegalArgumentException e) {
            return null; // Some other team
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(owner);
        return player != null && Progression.classOf(player) == DndCharacter.NECROMANCER ? player : null;
    }

    private static void tickClouds(ServerWorld world) {
        if (CLOUDS.isEmpty())
            return;
        long now = world.getTime();
        CLOUDS.removeIf(cloud -> {
            if (cloud.world() != world.getRegistryKey())
                return false;
            if (now >= cloud.endTime())
                return true;
            Vec3d c = cloud.center();
            if (now % 4 == 0) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, 12, CLOUD_RADIUS / 2, 0.3,
                        CLOUD_RADIUS / 2, 0.01);
                world.spawnParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.3, c.z, 4, CLOUD_RADIUS / 2, 0.2,
                        CLOUD_RADIUS / 2, 0.01);
            }
            if (now % 20 == 0) {
                // Only hostile mobs, and never your own summons.
                Entity owner = world.getEntity(cloud.owner());
                String team = cloud.owner().toString();
                Box box = new Box(c, c).expand(CLOUD_RADIUS, 2, CLOUD_RADIUS);
                for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class, box,
                        e -> e instanceof Monster && e.isAlive()
                                && (e.getScoreboardTeam() == null || !team.equals(e.getScoreboardTeam().getName()))
                                && e.squaredDistanceTo(c) <= CLOUD_RADIUS * CLOUD_RADIUS)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, CLOUD_WITHER_TICKS, 1),
                            owner);
                }
                world.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_SOUL_SAND_STEP, SoundCategory.PLAYERS, 0.6F,
                        0.6F);
            }
            return false;
        });
    }
}
