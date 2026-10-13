package mattonfire.dnd.dungeon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.entity.boss.BossBars;
import mattonfire.dnd.entity.GelatinousCubeEntity;
import mattonfire.dnd.entity.MimicEntity;
import mattonfire.dnd.entity.OwlbearEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A dungeon's mid-boss: an ordinary mob from the theme's pool made tougher, named, and given a
 * white boss bar. The mob's own class doesn't change: the room's ward ticks the bar
 * ({@link #tick}), so the Gelatinous Cube, Owlbear and Mimic stay as they are.
 *
 * Max health is x3 for one player, +0.3 of the base per extra player (4 players: x3.9); damage
 * +25%, knockback resistance +0.3. A Gelatinous Cube champion (the Crypt's Ossuary Cube) splits
 * once at half health, shedding ordinary cubes ({@link #splitIfHurt}). A chained champion (the
 * Goblin Warren's Grimclaw, an Owlbear) appears held in its chains with no AI and breaks free
 * {@link #CHAIN_TICKS} later, or as soon as it's hurt ({@link #tickChained}); an Owlbear champion
 * charges every 4 s instead of 6 below half health ({@link OwlbearEntity#FURY_TAG}).
 */
public final class DungeonChampion {
    public static final String TAG = "dndclasses.dungeon_champion";
    public static final double HEALTH_SOLO = 3.0;
    public static final double HEALTH_PER_EXTRA_PLAYER = 0.3;
    public static final double DAMAGE_BONUS = 0.25;
    public static final double KNOCKBACK_RESISTANCE = 0.3;
    public static final double BAR_RANGE = 32.0;
    /**
     * The Ossuary Cube splits once, at this share of its health, shedding {@link #SPLIT_COUNT} plain
     * cubes ({@link #SPLIT_COUNT_SOLO} for a party of one: two more 4-threat cubes on top of the
     * champion swamp a lone player).
     */
    public static final float SPLIT_AT = 0.5F;
    public static final int SPLIT_COUNT = 2;
    public static final int SPLIT_COUNT_SOLO = 1;
    private static final String SPLIT_TAG = "dndclasses.dungeon_champion_split";
    /** A chained champion strains at its chains this long before it breaks free (unless it's hurt first). */
    public static final int CHAIN_TICKS = 60;
    public static final String CHAINED_TAG = "dndclasses.dungeon_champion_chained";
    /** When each chained champion breaks free (server thread only; a champion missing here after a restart breaks free at once). */
    private static final Map<UUID, Long> CHAINED_UNTIL = new HashMap<>();

    private static final UUID HEALTH_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a01");
    private static final UUID DAMAGE_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a02");
    private static final UUID KNOCKBACK_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a03");

    /** Live bars by champion (server thread only; rebuilt from the tag after a restart). */
    private static final Map<UUID, ServerBossBar> BARS = new HashMap<>();

    private DungeonChampion() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BARS.clear();
            CHAINED_UNTIL.clear();
        });
    }

    public static double healthMult(int partySize) {
        return HEALTH_SOLO + HEALTH_PER_EXTRA_PLAYER * (MathHelper.clamp(partySize, 1, EncounterBuilder.MAX_PARTY) - 1);
    }

    /** Makes {@code mob} the room's champion for a party of {@code partySize}. */
    public static void make(ServerWorld world, MobEntity mob, DungeonEncounterPools.Champion champion, int partySize) {
        EntityAttributeInstance maxHealth = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (champion.baseHealth() > 0.0D && maxHealth != null) {
            maxHealth.setBaseValue(champion.baseHealth());
        }
        add(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, healthMult(partySize) - 1.0, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_ID, DAMAGE_BONUS, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, KNOCKBACK_RESISTANCE, EntityAttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Text.translatable(champion.nameKey()));
        mob.setCustomNameVisible(false);
        mob.addCommandTag(TAG);
        if (mob instanceof OwlbearEntity) {
            mob.addCommandTag(OwlbearEntity.FURY_TAG);
        }
        if (champion.chained()) {
            mob.setAiDisabled(true);
            mob.addCommandTag(CHAINED_TAG);
            CHAINED_UNTIL.put(mob.getUuid(), world.getTime() + CHAIN_TICKS);
            world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 2.0F, 0.6F);
        }
        if (mob instanceof GelatinousCubeEntity cube && champion.absorbedLoot() != null && champion.absorbedStacks() > 0) {
            // The Ossuary Cube starts with a few things already inside it, which it drops when it dies.
            LootTable table = world.getServer().getLootManager().getTable(champion.absorbedLoot());
            List<ItemStack> stacks = table.generateLoot(new LootContext.Builder(world).random(world.getRandom())
                    .parameter(LootContextParameters.ORIGIN, mob.getPos()).build(LootContextTypes.CHEST));
            for (int i = 0; i < Math.min(champion.absorbedStacks(), stacks.size()); i++) {
                cube.absorb(stacks.get(i));
            }
        }
    }

    private static void add(LivingEntity mob, net.minecraft.entity.attribute.EntityAttribute attribute, UUID id, double amount,
                            EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = mob.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, "dndclasses.champion", amount, operation));
        }
    }

    public static boolean isChampion(Entity entity) {
        return entity.getCommandTags().contains(TAG);
    }

    /**
     * A chained champion breaks free once its time is up or as soon as it's hurt: its AI comes back,
     * it roars, the chains clatter, and it turns on the nearest player. Until then its chains rattle.
     * Returns true if it broke free now.
     */
    public static boolean tickChained(ServerWorld world, LivingEntity champion) {
        if (!champion.getCommandTags().contains(CHAINED_TAG) || !(champion instanceof MobEntity mob)) {
            return false;
        }
        long until = CHAINED_UNTIL.getOrDefault(champion.getUuid(), 0L);
        boolean hurt = champion.getHealth() < champion.getMaxHealth() || champion.hurtTime > 0;
        if (!hurt && world.getTime() < until) {
            if (world.random.nextInt(3) == 0) {
                world.playSound(null, champion.getBlockPos(), SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.HOSTILE, 1.5F, 0.5F);
            }
            return false;
        }
        CHAINED_UNTIL.remove(champion.getUuid());
        champion.removeScoreboardTag(CHAINED_TAG);
        mob.setAiDisabled(false);
        world.playSound(null, champion.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 2.0F, 0.6F);
        world.playSound(null, champion.getBlockPos(), SoundEvents.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 2.5F, 0.5F);
        world.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, champion.getX(), champion.getBodyY(0.5D), champion.getZ(),
                30, 0.8D, 0.6D, 0.8D, 0.2D);
        if (champion instanceof OwlbearEntity owlbear) {
            owlbear.triggerAnim("attack_controller", OwlbearEntity.ROAR);
        }
        net.minecraft.entity.player.PlayerEntity target = world.getClosestPlayer(champion.getX(), champion.getY(), champion.getZ(),
                BAR_RANGE, net.minecraft.predicate.entity.EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
        if (target != null) {
            mob.setTarget(target);
        }
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Dungeon] champion {} broke its chains ({})",
                champion.getType().getUntranslatedName(), hurt ? "hurt" : "time");
        return true;
    }

    /** Tag for the Ruin's Mimic Matriarch, hiding among the gallery's chests ({@link #tickMatriarch}). */
    public static final String MATRIARCH_TAG = "dndclasses.dungeon_champion_matriarch";
    /** A player this close to the sleeping Matriarch wakes it. */
    public static final double MATRIARCH_WAKE_RANGE = 2.5D;
    /** Mimics of the Matriarch's room this close to it wake with it. */
    private static final double MATRIARCH_KIN_RANGE = 24.0D;

    /**
     * The Mimic Matriarch: while it sleeps (a chest among chests) it wakes when a player comes within
     * {@link #MATRIARCH_WAKE_RANGE} blocks, or when nothing else of its room is still fighting (the
     * room's other monsters are dead and only sleeping mimics are left). Once it's awake it wakes every
     * mimic of its room. Returns true while it's still asleep (its bar stays hidden).
     */
    public static boolean tickMatriarch(ServerWorld world, LivingEntity champion) {
        if (!champion.getCommandTags().contains(MATRIARCH_TAG) || !(champion instanceof MimicEntity matriarch)) {
            return false;
        }
        String roomTag = null;
        for (String tag : champion.getCommandTags()) {
            if (tag.startsWith(EncounterBuilder.ROOM_TAG_PREFIX)) {
                roomTag = tag;
            }
        }
        final String room = roomTag;
        net.minecraft.util.math.Box near = champion.getBoundingBox().expand(MATRIARCH_KIN_RANGE);
        List<MobEntity> roomMobs = room == null ? List.of() : world.getEntitiesByClass(MobEntity.class, near,
                mob -> mob.isAlive() && mob != champion && mob.getCommandTags().contains(room));
        if (matriarch.isDormant()) {
            net.minecraft.entity.player.PlayerEntity close = world.getClosestPlayer(champion.getX(), champion.getY(), champion.getZ(),
                    MATRIARCH_WAKE_RANGE, net.minecraft.predicate.entity.EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
            boolean alone = roomMobs.stream().allMatch(mob -> mob instanceof MimicEntity m && m.isDormant());
            if (close == null && !alone) {
                return true;
            }
            net.minecraft.entity.player.PlayerEntity target = close != null ? close : world.getClosestPlayer(champion.getX(),
                    champion.getY(), champion.getZ(), BAR_RANGE, net.minecraft.predicate.entity.EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
            matriarch.rouse(target);
            mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Dungeon] the Mimic Matriarch woke ({})", close != null ? "a player came close" : "its room was otherwise clear");
        }
        int woken = 0;
        for (MobEntity mob : roomMobs) {
            if (mob instanceof MimicEntity mimic && mimic.isDormant()) {
                mimic.rouse(matriarch.getTarget());
                woken++;
            }
        }
        if (woken > 0) {
            world.playSound(null, champion.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.HOSTILE, 2.0F, 0.5F);
            mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Dungeon] the Mimic Matriarch woke {} mimic(s)", woken);
        }
        return false;
    }

    /** Updates the champion's bar: its health, and the players within {@link #BAR_RANGE} blocks. */
    public static void tick(ServerWorld world, LivingEntity champion) {
        tickChained(world, champion);
        if (tickMatriarch(world, champion)) {
            // Still pretending to be a chest: no bar to give it away
            ServerBossBar hidden = BARS.get(champion.getUuid());
            if (hidden != null) {
                hidden.clearPlayers();
            }
            return;
        }
        ServerBossBar bar = BARS.computeIfAbsent(champion.getUuid(), id -> {
            ServerBossBar b = new ServerBossBar(champion.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.NOTCHED_10);
            b.setDarkenSky(false);
            return b;
        });
        bar.setPercent(MathHelper.clamp(champion.getHealth() / champion.getMaxHealth(), 0.0F, 1.0F));
        Vec3d pos = champion.getPos();
        BossBars.syncViewers(bar, world, player -> !player.isSpectator() && player.squaredDistanceTo(pos) <= BAR_RANGE * BAR_RANGE);
    }

    /**
     * The Ossuary Cube's split: the first time a Gelatinous Cube champion is at or below
     * {@link #SPLIT_AT} of its health, {@link #splitCount} ordinary cubes (no champion buffs, no
     * absorbed loot) burst out of it beside it, tagged with {@code tags} so the room follows them.
     * Called from the room's ward with the champion's other ticking; returns the new cubes.
     */
    public static int splitCount(int partySize) {
        return partySize <= 1 ? SPLIT_COUNT_SOLO : SPLIT_COUNT;
    }

    public static List<Entity> splitIfHurt(ServerWorld world, LivingEntity champion, List<String> tags, int partySize) {
        if (!(champion instanceof GelatinousCubeEntity) || !champion.isAlive() || champion.getCommandTags().contains(SPLIT_TAG)
                || champion.getHealth() > champion.getMaxHealth() * SPLIT_AT) {
            return List.of();
        }
        champion.addCommandTag(SPLIT_TAG);
        List<Entity> spawned = new java.util.ArrayList<>();
        mattonfire.dnd.dm.encounter.Encounter.Spawn spawn = new mattonfire.dnd.dm.encounter.Encounter.Spawn(champion.getType(),
                1, 1, 1.0F, new net.minecraft.nbt.NbtCompound(), true, List.of());
        for (int i = 0; i < splitCount(partySize); i++) {
            Entity cube = EncounterBuilder.spawnOne(world, spawn, champion.getBlockPos(), tags);
            if (cube != null) {
                spawned.add(cube);
            }
        }
        world.playSound(null, champion.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_SLIME_SQUISH,
                net.minecraft.sound.SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.spawnParticles(net.minecraft.particle.ParticleTypes.ITEM_SLIME, champion.getX(), champion.getBodyY(0.5D),
                champion.getZ(), 40, 0.8D, 0.6D, 0.8D, 0.1D);
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Dungeon] champion {} split at {}/{} health into {} cube(s)",
                champion.getType().getUntranslatedName(), champion.getHealth(), champion.getMaxHealth(), spawned.size());
        return spawned;
    }

    /** Takes the champion's bar down (it died, or the fight was abandoned). */
    public static void removeBar(UUID champion) {
        CHAINED_UNTIL.remove(champion);
        ServerBossBar bar = BARS.remove(champion);
        if (bar != null) {
            bar.clearPlayers();
        }
    }
}
