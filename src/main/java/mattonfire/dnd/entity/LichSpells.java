package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.entity.boss.BossMinions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link LichEntity}'s spellcasting: picks a spell (Frost Nova when cornered, Raise Dead while it
 * has room for more undead, Ray of Frost, Wither Bolt), winds it up, then casts it. Cooldowns shorten
 * in its desperate phase. Not saved: a Lich that reloads starts with every spell ready.
 */
final class LichSpells {
    enum Spell {
        WITHER_BOLT(8, 50, "cast"),
        RAY_OF_FROST(12, 90, "cast"),
        FROST_NOVA(10, 140, "nova"),
        RAISE_DEAD(24, 400, "summon");

        final int windUp;
        final int cooldown;
        final String animation;

        Spell(int windUp, int cooldown, String animation) {
            this.windUp = windUp;
            this.cooldown = cooldown;
            this.animation = animation;
        }
    }

    private static final double MINION_RANGE = 32.0D;
    private static final double NOVA_RADIUS = 5.0D;
    private static final double FROST_RANGE = 20.0D;

    private final LichEntity lich;
    private final long[] nextCast = new long[Spell.values().length];
    private Spell casting;
    private long castTime;

    LichSpells(LichEntity lich) {
        this.lich = lich;
    }

    boolean isCasting() {
        return this.casting != null;
    }

    /** Drops the spell it was winding up (it blinked away). */
    void interrupt() {
        this.casting = null;
    }

    /** Lets {@code spell} be cast straight away. */
    void readyNow(Spell spell, long now) {
        this.nextCast[spell.ordinal()] = now;
    }

    /** Winds up and casts its spells at {@code target}: once a wind-up is over, the spell goes off. */
    void tick(ServerWorld world, @Nullable LivingEntity target, long now) {
        if (this.casting != null) {
            this.lich.getNavigation().stop();
            if (target != null) {
                this.lich.getLookControl().lookAt(target, 30.0F, 30.0F);
            }
            world.spawnParticles(this.casting == Spell.WITHER_BOLT || this.casting == Spell.RAISE_DEAD
                            ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SNOWFLAKE,
                    this.lich.getX(), this.lich.getBodyY(0.9D), this.lich.getZ(), 2, 0.4D, 0.4D, 0.4D, 0.02D);
            if (now >= this.castTime) {
                Spell spell = this.casting;
                this.casting = null;
                this.cast(world, spell, target);
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        Spell spell = this.chooseSpell(world, target, now);
        if (spell != null) {
            this.casting = spell;
            this.castTime = now + spell.windUp;
            this.nextCast[spell.ordinal()] = now + this.cooldown(spell);
            this.lich.triggerAnim("action", spell.animation);
            this.lich.playSound(spell == Spell.RAISE_DEAD ? SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON
                    : SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 2.0F, 0.7F);
        }
    }

    private int cooldown(Spell spell) {
        int cooldown = spell.cooldown;
        if (spell == Spell.RAISE_DEAD && this.lich.getBossFight().getPhase() >= 1) {
            cooldown = 300;
        }
        return this.lich.isDesperate() ? cooldown * 3 / 5 : cooldown;
    }

    /** Ticks between the entity scans behind the nova and Raise Dead. */
    private static final int SCAN_INTERVAL = 10;
    private long nextScanTime;

    @Nullable
    private Spell chooseSpell(ServerWorld world, LivingEntity target, long now) {
        double distance = this.lich.distanceTo(target);
        boolean canSee = this.lich.getVisibilityCache().canSee(target);
        // The nova and Raise Dead need entity scans: only look every SCAN_INTERVAL ticks
        boolean scan = now >= this.nextScanTime;
        if (scan) {
            this.nextScanTime = now + SCAN_INTERVAL;
        }
        if (scan && this.ready(Spell.FROST_NOVA, now) && !this.enemiesWithin(NOVA_RADIUS - 1.0D).isEmpty()) {
            return Spell.FROST_NOVA;
        }
        if (scan && this.ready(Spell.RAISE_DEAD, now) && this.minions(world).size() < this.maxMinions()) {
            return Spell.RAISE_DEAD;
        }
        if (canSee && this.ready(Spell.RAY_OF_FROST, now) && distance <= FROST_RANGE) {
            return Spell.RAY_OF_FROST;
        }
        if (canSee && this.ready(Spell.WITHER_BOLT, now) && distance <= 32.0D) {
            return Spell.WITHER_BOLT;
        }
        return null;
    }

    private boolean ready(Spell spell, long now) {
        return now >= this.nextCast[spell.ordinal()];
    }

    // ---------------------------------------------------------------- spells

    private void cast(ServerWorld world, Spell spell, @Nullable LivingEntity target) {
        MobActions.log(this.lich, "cast " + spell, target);
        switch (spell) {
            case WITHER_BOLT -> this.castWitherBolt(world, target);
            case RAY_OF_FROST -> this.castRayOfFrost(world, target);
            case FROST_NOVA -> this.castFrostNova(world);
            case RAISE_DEAD -> this.castRaiseDead(world, target);
        }
    }

    private Vec3d staffTip() {
        float yaw = this.lich.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        // The staff is held to the Lich's right, a little forward.
        double side = -0.4D;
        double forward = 0.3D;
        return new Vec3d(this.lich.getX() - MathHelper.sin(yaw) * forward + MathHelper.cos(yaw) * side,
                this.lich.getY() + 2.2D,
                this.lich.getZ() + MathHelper.cos(yaw) * forward + MathHelper.sin(yaw) * side);
    }

    private void castWitherBolt(ServerWorld world, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3d tip = this.staffTip();
        int bolts = this.lich.isDesperate() ? 3 : 1;
        for (int i = 0; i < bolts; i++) {
            double spread = (i - (bolts - 1) / 2.0D) * 1.5D;
            double dx = target.getX() - tip.x + spread * MathHelper.cos(this.lich.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
            double dy = target.getBodyY(0.5D) - tip.y;
            double dz = target.getZ() - tip.z + spread * MathHelper.sin(this.lich.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
            WitherSkullEntity skull = new LichBoltEntity(world, this.lich, dx, dy, dz);
            skull.setOwner(this.lich);
            skull.setPos(tip.x, tip.y, tip.z);
            if (this.lich.isDesperate() && i == 1) {
                skull.setCharged(true);
            }
            world.spawnEntity(skull);
        }
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.HOSTILE, 1.0F, 0.8F);
    }

    private void castRayOfFrost(ServerWorld world, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || !this.lich.getVisibilityCache().canSee(target)) {
            return;
        }
        Vec3d tip = this.staffTip();
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.5D), target.getZ());
        Vec3d step = end.subtract(tip);
        int points = Math.max(4, (int) (step.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3d p = tip.add(step.multiply(i / (double) points));
            world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, end.x, end.y, end.z, 12, 0.3D, 0.4D, 0.3D, 0.05D);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE,
                SoundCategory.HOSTILE, 1.5F, 0.8F);
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE, 0.8F, 1.6F);
        this.chill(target, 7.0F, 100, 2);
    }

    private void castFrostNova(ServerWorld world) {
        for (int i = 0; i < 48; i++) {
            float angle = i / 48.0F * MathHelper.TAU;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, this.lich.getX(), this.lich.getY() + 0.3D, this.lich.getZ(), 0,
                    MathHelper.cos(angle), 0.05D, MathHelper.sin(angle), 0.5D);
        }
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, this.lich.getX(), this.lich.getBodyY(0.5D), this.lich.getZ(),
                40, 1.5D, 0.6D, 1.5D, 0.1D);
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.BLOCK_POWDER_SNOW_BREAK,
                SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE, 1.5F, 0.6F);
        boolean lastPhase = this.lich.isLastPhase();
        for (LivingEntity victim : this.enemiesWithin(NOVA_RADIUS)) {
            // CON save (one per nova): half damage and half the slow, but the shove lands either way
            SaveResult save = BossSaves.roll(victim, this.lich, BossSaves.FROST_NOVA, lastPhase, "nova", BossSaves.NOVA_WINDOW);
            this.chill(victim, save.damage(8.0F), save.duration(120), 3);
            victim.takeKnockback(1.6D, this.lich.getX() - victim.getX(), this.lich.getZ() - victim.getZ());
            victim.velocityModified = true;
        }
    }

    /** Frost damage plus slowness and freezing (the powder snow frost overlay and its chip damage). */
    private void chill(LivingEntity victim, float damage, int slowTicks, int slowAmplifier) {
        victim.damage(this.lich.getDamageSources().indirectMagic(this.lich, this.lich), damage);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, slowAmplifier), this.lich);
        if (victim.canFreeze()) {
            victim.setFrozenTicks(Math.max(victim.getFrozenTicks(), victim.getMinFreezeDamageTicks() + slowTicks * 2));
        }
    }

    private List<LivingEntity> enemiesWithin(double radius) {
        return this.lich.world.getEntitiesByClass(LivingEntity.class, this.lich.getBoundingBox().expand(radius),
                e -> e.isAlive() && !LichEntity.isLichSide(e) && this.lich.squaredDistanceTo(e) <= radius * radius
                        && (e instanceof PlayerEntity player ? !player.isCreative() && !player.isSpectator()
                        : e == this.lich.getTarget() || e instanceof IronGolemEntity));
    }

    private int maxMinions() {
        return switch (this.lich.getBossFight().getPhase()) {
            case 0 -> 4;
            case 1 -> 6;
            default -> 8;
        };
    }

    /** Its raised dead within range. */
    List<MobEntity> minions(ServerWorld world) {
        return world.getEntitiesByClass(MobEntity.class, this.lich.getBoundingBox().expand(MINION_RANGE),
                e -> e.isAlive() && e.getCommandTags().contains(LichEntity.MINION_TAG));
    }

    private void castRaiseDead(ServerWorld world, @Nullable LivingEntity target) {
        int wave = 2 + this.lich.getBossFight().getPhase();
        // A Cleric consecrated the library's Desecrated Altar: the dead answer weakly
        boolean consecrated = ObstacleTypes.DESECRATED_ALTAR.consecratedNear(world, this.lich.getBlockPos(), LichEntity.CONSECRATED_RADIUS);
        if (consecrated) {
            DnDClasses.LOGGER.info("[Obstacle] Lich at {} raises {} dead at half health (consecrated altar)",
                    this.lich.getBlockPos().toShortString(), wave);
        }
        for (int i = 0; i < wave; i++) {
            EntityType<? extends MobEntity> type = this.pickMinionType(i);
            MobEntity minion = type.create(world);
            if (minion == null || !this.lich.placeNear(minion, this.lich.getBlockPos(), 2.0D, 4.0D)) {
                continue;
            }
            minion.initialize(world, world.getLocalDifficulty(minion.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            minion.addCommandTag(LichEntity.MINION_TAG);
            BossMinions.mark(minion);
            if (consecrated) {
                minion.setHealth(minion.getMaxHealth() * LichEntity.CONSECRATED_MINION_HEALTH);
            }
            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }
            world.spawnEntityAndPassengers(minion);
            world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 0.2D, minion.getZ(),
                    15, 0.4D, 0.2D, 0.4D, 0.03D);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    10, 0.3D, 0.6D, 0.3D, 0.01D);
        }
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
                SoundCategory.HOSTILE, 0.6F, 0.5F);
    }

    private EntityType<? extends MobEntity> pickMinionType(int index) {
        if (this.lich.getBossFight().getPhase() >= 1 && this.lich.getRandom().nextInt(3) == 0) {
            return EntityType.WITHER_SKELETON;
        }
        boolean archer = index % 2 == 1;
        if (archer) {
            return this.lich.getBossFight().getPhase() >= 1 && this.lich.getRandom().nextBoolean() ? EntityType.STRAY : EntityType.SKELETON;
        }
        return this.lich.getRandom().nextInt(4) == 0 ? EntityType.HUSK : EntityType.ZOMBIE;
    }
}
