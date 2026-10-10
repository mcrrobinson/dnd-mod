package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Paladin skills. Hellforged is checked in {@code PaladinNetherWeakness}, and
 * Aura of Protection in {@code PaladinAuraMixin} (it also protects other
 * players, whose damage hooks belong to their own class).
 *
 * Paladins only ignore potion effects, so the effects given here still apply.
 */
public class PaladinSkills extends ClassSkills {
    /**
     * The root special, Divine Judgment; fired by {@code PowerUpEffect}. A beam
     * of holy light hits the mob you look at, and a shockwave hits hostiles
     * around it. From rank III, extra beams strike the nearest other hostiles.
     */
    public static final String DIVINE_JUDGMENT_ID = "paladin.divine_judgment";
    public static final Ranks DIVINE_JUDGMENT = Ranks.of(DIVINE_JUDGMENT_ID)
            .amount("Damage", "", 8, 12, 16, 20)
            .amount("Shockwave", "blocks", 3, 4, 5, 6)
            .amount("Beams", "", 1, 1, 2, 3);

    public static final String HELLFORGED = "paladin.hellforged";
    private static final String AURA_OF_PROTECTION = "paladin.aura_of_protection";

    private static final int UNDEAD_KILL_BONUS_XP = 3;
    private static final int GROUP_KILL_BONUS_XP = 1;
    private static final double GROUP_RADIUS = 16;

    private static final float SMITE_BONUS = 4.0F;
    private static final int SACRED_WEAPON_TICKS = 15 * 20;
    private static final int SACRED_FIRE_SECONDS = 4;
    private static final double AURA_RADIUS = 8;
    private static final float AURA_PROTECTION = 0.85F;
    private static final int SHIELD_TICKS = 15 * 20;
    private static final int ANGEL_TICKS = 20 * 20;
    private static final double ANGEL_RADIUS = 10;
    private static final int ANGEL_FIRE_SECONDS = 3;

    private static final String CIRCLE_OF_HEALING = "paladin.circle_of_healing";
    /** Every player this close is fully healed. */
    private static final double HEAL_RADIUS = 10;
    /** Party members this close are fully healed and get Absorption. */
    private static final double PARTY_HEAL_RADIUS = 24;
    private static final int HEAL_ABSORPTION_TICKS = 30 * 20;

    private static final double JUDGMENT_RANGE = 30;
    /** Extra beams pick hostiles within this many blocks of the first target. */
    private static final double JUDGMENT_CHAIN_RADIUS = 12;
    /** Share of the beam's damage the shockwave deals. */
    private static final float SHOCKWAVE_SHARE = 0.4F;
    private static final float JUDGMENT_UNDEAD_MULTIPLIER = 1.5F;
    private static final int JUDGMENT_FIRE_SECONDS = 5;
    private static final double BEAM_HEIGHT = 16;

    private static final UUID SHIELD_KNOCKBACK_UUID = UUID.fromString("8d0e6f43-2b7a-4c11-9e55-3f7a1c2d9b01");

    /** World time each player's timed buff ends at. */
    private static final Map<UUID, Long> SACRED_WEAPON = new HashMap<>();
    private static final Map<UUID, Long> DIVINE_SHIELD = new HashMap<>();
    private static final Map<UUID, Long> AVENGING_ANGEL = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.PALADIN;
    }

    @Override
    public List<String> subclassIds() {
        return List.of("paladin.devotion", "paladin.conquest");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active(DIVINE_JUDGMENT_ID, "Divine Judgment",
                        "A beam of holy light strikes the mob you look at and sets it alight; a shockwave hits hostiles around it. Undead take 50% more.",
                        "minecraft:lightning_rod", 9, 0, 1, 3),
                // Oath of Devotion
                passive("paladin.divine_smite", "Divine Smite", "Melee hits deal +4 damage to undead.",
                        "minecraft:golden_sword", 1, 2, 3, DIVINE_JUDGMENT_ID),
                active("paladin.sacred_weapon", "Sacred Weapon",
                        "Strength for 15 seconds, and your melee hits set targets alight.", "minecraft:blaze_rod", 4,
                        1, 2, 2, "paladin.divine_smite"),
                passive(AURA_OF_PROTECTION, "Aura of Protection",
                        "You and players within 8 blocks take 15% less damage.", "minecraft:beacon", 1, 2, 1,
                        "paladin.sacred_weapon"),
                // Oath of Conquest
                passive(HELLFORGED, "Hellforged", "The Nether no longer weakens you.", "minecraft:netherite_ingot",
                        1, 0, 3, DIVINE_JUDGMENT_ID),
                active("paladin.divine_shield", "Divine Shield",
                        "Absorption III and no knockback for 15 seconds.", "minecraft:shield", 5, 1, 0, 2,
                        HELLFORGED),
                passive("paladin.aura_of_courage", "Aura of Courage",
                        "You and players within 8 blocks can't be weakened or slowed.", "minecraft:golden_helmet", 1,
                        0, 1, "paladin.divine_shield"),
                active("paladin.avenging_angel", "Avenging Angel",
                        "Strength II, Regeneration II and Resistance II for 20 seconds; undead within 10 blocks burn.",
                        "minecraft:elytra", 9, 2, 1, 0, AURA_OF_PROTECTION, "paladin.aura_of_courage"),
                // Shared by both oaths
                active(CIRCLE_OF_HEALING, "Circle of Healing",
                        "Fully heals players within 10 blocks; party members within 24 blocks are healed too and get Absorption for 30 seconds.",
                        "minecraft:golden_apple", 7, 2, 1, 2, DIVINE_JUDGMENT_ID));
    }

    @Override
    public void register() {
        // Divine Shield's knockback resistance is an attribute, so it's dropped
        // here even if the player changed class meanwhile (no secondTick then).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (DIVINE_SHIELD.isEmpty() || server.getTicks() % 20 != 0)
                return;
            long now = server.getOverworld().getTime();
            DIVINE_SHIELD.entrySet().removeIf(entry -> {
                if (now < entry.getValue())
                    return false;
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    EntityAttributeInstance knockback = player
                            .getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                    if (knockback != null)
                        knockback.removeModifier(SHIELD_KNOCKBACK_UUID);
                }
                return true;
            });
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        long now = ((ServerWorld) player.getWorld()).getTime();
        switch (node.id()) {
            case "paladin.sacred_weapon" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, SACRED_WEAPON_TICKS, 0));
                SACRED_WEAPON.put(player.getUuid(), now + SACRED_WEAPON_TICKS);
                effects(player, SoundEvents.ITEM_FIRECHARGE_USE, ParticleTypes.FLAME, 30);
            }
            case "paladin.divine_shield" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, SHIELD_TICKS, 2));
                EntityAttributeInstance knockback = player
                        .getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                if (knockback != null && knockback.getModifier(SHIELD_KNOCKBACK_UUID) == null) {
                    knockback.addTemporaryModifier(new EntityAttributeModifier(SHIELD_KNOCKBACK_UUID,
                            "Divine Shield", 1.0, EntityAttributeModifier.Operation.ADDITION));
                }
                DIVINE_SHIELD.put(player.getUuid(), now + SHIELD_TICKS);
                effects(player, SoundEvents.ITEM_SHIELD_BLOCK, ParticleTypes.ENCHANTED_HIT, 30);
            }
            case "paladin.avenging_angel" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ANGEL_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ANGEL_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ANGEL_TICKS, 1));
                AVENGING_ANGEL.put(player.getUuid(), now + ANGEL_TICKS);
                burnUndead(player);
                effects(player, SoundEvents.BLOCK_BEACON_ACTIVATE, ParticleTypes.END_ROD, 40);
            }
            case CIRCLE_OF_HEALING -> circleOfHealing(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        int xp = isUndead(killed) ? UNDEAD_KILL_BONUS_XP : 0;
        if (killed instanceof Monster && playersNear(player, GROUP_RADIUS).size() > 1) {
            xp += GROUP_KILL_BONUS_XP; // Someone besides us is nearby
        }
        return xp;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (source.getSource() != player)
            return amount; // Melee only
        if (progress.hasPassive("paladin.divine_smite") && isUndead(target)) {
            amount += SMITE_BONUS;
        }
        if (running(SACRED_WEAPON, player)) {
            target.setOnFireFor(SACRED_FIRE_SECONDS);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("paladin.aura_of_courage")) {
            for (PlayerEntity ally : playersNear(player, AURA_RADIUS)) {
                ally.removeStatusEffect(StatusEffects.WEAKNESS);
                ally.removeStatusEffect(StatusEffects.SLOWNESS);
            }
        }
        if (running(AVENGING_ANGEL, player)) {
            burnUndead(player);
        }
    }

    /**
     * Aura of Protection, called from {@code PaladinAuraMixin}: damage to a
     * player near a Paladin with the aura (or to that Paladin) is cut by 15%.
     * Several auras don't stack.
     */
    public static float applyAuraOfProtection(LivingEntity target, float amount) {
        if (!(target instanceof PlayerEntity player) || target.getWorld().isClient)
            return amount;
        for (PlayerEntity paladin : playersNear(player, AURA_RADIUS)) {
            if (Progression.classOf(paladin) == DndCharacter.PALADIN
                    && Progression.hasPassive(paladin, AURA_OF_PROTECTION)) {
                return amount * AURA_PROTECTION;
            }
        }
        return amount;
    }

    /**
     * Circle of Healing: every player within 10 blocks is fully healed. The
     * Paladin and party members within 24 blocks are fully healed and get
     * Absorption I for 30 seconds.
     */
    private static void circleOfHealing(ServerPlayerEntity player) {
        for (PlayerEntity ally : playersNear(player, HEAL_RADIUS)) {
            ally.heal(ally.getMaxHealth());
        }
        List<ServerPlayerEntity> party = PartyManager.nearbyMembers(player, PARTY_HEAL_RADIUS);
        party.add(player);
        for (ServerPlayerEntity member : party) {
            member.heal(member.getMaxHealth());
            member.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, HEAL_ABSORPTION_TICKS, 0));
            ((ServerWorld) member.getWorld()).spawnParticles(ParticleTypes.HEART, member.getX(),
                    member.getY() + 1.2, member.getZ(), 5, 0.4, 0.4, 0.4, 0.0);
        }

        // A ring of light on the ground marking the 10-block circle.
        ServerWorld world = (ServerWorld) player.getWorld();
        int points = (int) (HEAL_RADIUS * 8);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX() + Math.cos(angle) * HEAL_RADIUS,
                    player.getY() + 0.3, player.getZ() + Math.sin(angle) * HEAL_RADIUS, 2, 0.1, 0.2, 0.1, 0.0);
        }
        world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 12, 1.5, 0.5,
                1.5, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS,
                1.5F, 1.0F);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                0.6F, 1.6F);
    }

    /**
     * Divine Judgment: strikes the mob the Paladin looks at (within 30 blocks,
     * not through walls), then the nearest other hostiles for extra beams.
     *
     * @return false if nothing is in sight, so the mana is kept
     */
    public static boolean divineJudgment(ServerPlayerEntity player) {
        LivingEntity target = judgmentTarget(player);
        if (target == null) {
            player.sendMessage(Text.literal("No target for Divine Judgment in sight."), true);
            return false;
        }
        float damage = (float) DIVINE_JUDGMENT.get(player, "Damage");
        double radius = DIVINE_JUDGMENT.get(player, "Shockwave");
        int beams = DIVINE_JUDGMENT.getInt(player, "Beams");

        List<LivingEntity> struck = new ArrayList<>();
        struck.add(target);
        if (beams > 1) {
            target.getWorld().getEntitiesByClass(LivingEntity.class,
                    target.getBoundingBox().expand(JUDGMENT_CHAIN_RADIUS),
                    e -> e != target && e instanceof Monster && e.isAlive())
                    .stream()
                    .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(target)))
                    .limit(beams - 1)
                    .forEach(struck::add);
        }

        Set<LivingEntity> hit = new HashSet<>(struck);
        for (LivingEntity mob : struck) {
            strike(player, mob, damage, radius, hit);
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    private static void strike(ServerPlayerEntity player, LivingEntity target, float damage, double radius,
            Set<LivingEntity> hit) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d at = target.getPos();

        // The beam: a column of light from the sky down onto the target.
        for (double dy = 0; dy < BEAM_HEIGHT; dy += 0.4) {
            world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + dy, at.z, 1, 0.08, 0.0, 0.08, 0.0);
        }
        world.spawnParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 30, 0.4, 0.6, 0.4, 0.3);
        // The shockwave ring.
        int points = (int) (radius * 12);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            world.spawnParticles(ParticleTypes.END_ROD, at.x + Math.cos(angle) * radius, at.y + 0.2,
                    at.z + Math.sin(angle) * radius, 1, 0, 0.05, 0, 0.01);
        }
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS,
                1.2F, 1.4F);
        world.playSound(null, target.getBlockPos(), SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.6F,
                1.6F);

        hurt(player, target, damage);
        target.setOnFireFor(JUDGMENT_FIRE_SECONDS);

        for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(radius),
                e -> e instanceof Monster && e.isAlive() && e.squaredDistanceTo(at) <= radius * radius)) {
            if (!hit.add(mob))
                continue; // Each mob is hit once, by its own beam or the first shockwave
            hurt(player, mob, damage * SHOCKWAVE_SHARE);
            if (isUndead(mob))
                mob.setOnFireFor(JUDGMENT_FIRE_SECONDS);
            double dx = mob.getX() - at.x;
            double dz = mob.getZ() - at.z;
            if (dx * dx + dz * dz > 1.0E-4)
                mob.takeKnockback(0.6, -dx, -dz);
        }
    }

    private static void hurt(ServerPlayerEntity player, LivingEntity mob, float amount) {
        if (isUndead(mob))
            amount *= JUDGMENT_UNDEAD_MULTIPLIER;
        // No source entity, only the attacker: code that treats source == player as a melee hit
        // (Divine Smite, Sacred Weapon, melee kill bonuses) must not count the beam.
        mob.damage(player.getWorld().getDamageSources().indirectMagic(null, player), amount);
    }

    /** The living thing the player looks at within range, not through walls, players or own pets. */
    private static LivingEntity judgmentTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        double range = JUDGMENT_RANGE;
        HitResult blockHit = player.raycast(JUDGMENT_RANGE, 1.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            range = blockHit.getPos().distanceTo(eye);
        }
        Vec3d end = eye.add(look.multiply(range));
        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0D);
        UUID owner = player.getUuid();
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                (Entity e) -> e instanceof LivingEntity && e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof ArmorStandEntity)
                        && !(e instanceof Tameable t && owner.equals(t.getOwnerUuid())),
                range * range);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private static boolean isUndead(LivingEntity entity) {
        return entity.getGroup() == EntityGroup.UNDEAD;
    }

    /** Players within the radius, including this one. */
    private static List<PlayerEntity> playersNear(PlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(PlayerEntity.class, player.getBoundingBox().expand(radius),
                p -> p.isAlive() && !p.isSpectator());
    }

    private static void burnUndead(ServerPlayerEntity player) {
        for (LivingEntity mob : player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(ANGEL_RADIUS), e -> e.isAlive() && isUndead(e))) {
            mob.setOnFireFor(ANGEL_FIRE_SECONDS);
        }
    }

    /** Whether the player's timed buff is still going; forgets it once over. */
    private static boolean running(Map<UUID, Long> buffs, PlayerEntity player) {
        Long until = buffs.get(player.getUuid());
        if (until == null)
            return false;
        if (player.getWorld().getTime() < until)
            return true;
        buffs.remove(player.getUuid());
        return false;
    }

    @Override
    public void forget(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        SACRED_WEAPON.values().removeIf(t -> now >= t);
        AVENGING_ANGEL.values().removeIf(t -> now >= t);
    }
}
