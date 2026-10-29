package mattonfire.dnd.entity.dragon;

import java.util.List;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * An Ancient dragon's legendary actions (design 10, 5.9): {@value #POINTS} points, refilled every
 * {@value #REFILL_TICKS} ticks, spent between its normal attacks at most once every {@value #MIN_GAP} ticks.
 * <ul>
 * <li><b>Tail Attack</b> (1): a player behind it (outside its 120-degree front arc) within {@value #TAIL_RANGE}
 * blocks takes {@value #TAIL_DAMAGE} damage and is knocked away.</li>
 * <li><b>Wing Attack</b> (2): when two or more players are within {@value #WING_RANGE} blocks, one within
 * {@value #WING_CLOSE} or it has just landed: everything within {@value #WING_RANGE} blocks makes a DEX save
 * (DC {@value AncientSaves#WING_ATTACK_DC}). A failure: {@value #WING_DAMAGE} damage, a hard knockback and Prone
 * for {@value #WING_PRONE_TICKS} ticks; a success: half and a lighter knockback. Then it beats into the air.</li>
 * <li><b>Detect</b> (1): its target is out of sight; it turns to face them and they glow for 3 s.</li>
 * </ul>
 */
public final class LegendaryActions {
    public static final int POINTS = 3;
    public static final int REFILL_TICKS = 120;
    public static final int MIN_GAP = 20;
    public static final double TAIL_RANGE = 14.0;
    public static final float TAIL_DAMAGE = 12.0F;
    public static final double TAIL_KNOCKBACK = 1.6;
    public static final double WING_RANGE = 15.0;
    public static final double WING_CLOSE = 8.0;
    public static final float WING_DAMAGE = 10.0F;
    public static final double WING_KNOCKBACK = 2.0;
    public static final double WING_KNOCKBACK_SAVED = 0.8;
    public static final int WING_PRONE_TICKS = 40;
    /** How hard it beats up into the air after a wing attack (about 8 blocks). */
    public static final double WING_LIFT = 1.3;
    public static final int DETECT_GLOW_TICKS = 60;
    /** Half the front arc: players further round than this are behind it. */
    private static final double FRONT_HALF_ARC = 60.0;

    private int points = POINTS;
    private int refill;
    private int gap;

    public int points() {
        return this.points;
    }

    public void refill() {
        this.points = POINTS;
        this.refill = 0;
    }

    /** Call every tick while the fight is on. */
    public void tick() {
        if (this.points < POINTS && ++this.refill >= REFILL_TICKS) {
            this.refill();
        }
        if (this.gap > 0) {
            this.gap--;
        }
    }

    private boolean ready(int cost) {
        return this.gap <= 0 && this.points >= cost;
    }

    private void spend(int cost) {
        this.points -= cost;
        this.gap = MIN_GAP;
    }

    /** Spends points on the first action that fits; true if it used one. */
    public boolean tryUse(AncientDragonEntity dragon, boolean justLanded) {
        if (!(dragon.getWorld() instanceof ServerWorld world) || this.gap > 0) {
            return false;
        }
        List<ServerPlayerEntity> near = world.getPlayers(p -> isFoe(dragon, p)
                && p.squaredDistanceTo(dragon) <= WING_RANGE * WING_RANGE);
        boolean close = near.stream().anyMatch(p -> p.squaredDistanceTo(dragon) <= WING_CLOSE * WING_CLOSE);
        if (this.ready(2) && (near.size() >= 2 || close || (justLanded && !near.isEmpty()))) {
            this.spend(2);
            wingAttack(dragon, world);
            return true;
        }
        if (this.ready(1)) {
            for (ServerPlayerEntity player : world.getPlayers(p -> isFoe(dragon, p)
                    && p.squaredDistanceTo(dragon) <= TAIL_RANGE * TAIL_RANGE)) {
                if (behind(dragon, player)) {
                    this.spend(1);
                    tailAttack(dragon, world, player);
                    return true;
                }
            }
            LivingEntity target = dragon.getTarget();
            if (target != null && target.isAlive() && !dragon.getVisibilityCache().canSee(target)) {
                this.spend(1);
                detect(dragon, target);
                return true;
            }
        }
        return false;
    }

    private static boolean isFoe(AncientDragonEntity dragon, PlayerEntity player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !dragon.isAlly(player);
    }

    /** Outside the 120-degree arc in front of its body. */
    static boolean behind(AncientDragonEntity dragon, LivingEntity entity) {
        Vec3d facing = Vec3d.fromPolar(0, dragon.bodyYaw);
        Vec3d to = entity.getPos().subtract(dragon.getPos()).multiply(1, 0, 1);
        if (to.lengthSquared() < 1.0E-4) {
            return false;
        }
        double cos = facing.dotProduct(to.normalize());
        return cos < Math.cos(Math.toRadians(FRONT_HALF_ARC));
    }

    private static void tailAttack(AncientDragonEntity dragon, ServerWorld world, ServerPlayerEntity player) {
        MobActions.log(dragon, "tail", player);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE,
                2.0F, 0.5F);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getBodyY(0.5), player.getZ(), 3, 0.5,
                0.3, 0.5, 0.0);
        if (player.damage(dragon.getDamageSources().mobAttack(dragon), TAIL_DAMAGE)) {
            knock(dragon, player, TAIL_KNOCKBACK);
        }
    }

    private static void wingAttack(AncientDragonEntity dragon, ServerWorld world) {
        MobActions.log(dragon, "wing", dragon.getTarget());
        world.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), ModSounds.ANCIENT_WINGBEAT,
                SoundCategory.HOSTILE, 6.0F, 0.8F);
        for (int i = 0; i < 48; i++) {
            float a = i / 48.0F * MathHelper.TAU;
            world.spawnParticles(ParticleTypes.CLOUD, dragon.getX() + MathHelper.cos(a) * 3.0, dragon.getY() + 0.3,
                    dragon.getZ() + MathHelper.sin(a) * 3.0, 0, MathHelper.cos(a), 0.05, MathHelper.sin(a), 0.9);
        }
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class,
                dragon.getBoundingBox().expand(WING_RANGE), e -> e != dragon && e.isAlive() && !dragon.isAlly(e)
                        && e.squaredDistanceTo(dragon) <= WING_RANGE * WING_RANGE
                        && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())))) {
            SaveResult save = SavingThrow.of(victim, AncientSaves.WING_ATTACK).source(dragon)
                    .exposure(dragon, "wing", 10).tags("dragon")
                    .onSuccess(Text.translatable(AncientSaves.WING_ATTACK.effectKey()))
                    .onFailure(Text.translatable("save.dndclasses.effect.wing_attack.failure"))
                    .roll();
            if (victim.damage(dragon.getDamageSources().mobAttack(dragon), save.damage(WING_DAMAGE))) {
                knock(dragon, victim, save.succeeded() ? WING_KNOCKBACK_SAVED : WING_KNOCKBACK);
            }
            if (save.failed()) {
                Conditions.of(victim, Condition.PRONE).source(dragon).key("wing_attack").ticks(WING_PRONE_TICKS).apply();
            }
        }
        dragon.takeOff(WING_LIFT);
    }

    private static void detect(AncientDragonEntity dragon, LivingEntity target) {
        MobActions.log(dragon, "detect", target);
        dragon.getLookControl().lookAt(target, 180.0F, 90.0F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, DETECT_GLOW_TICKS, 0, false, false),
                dragon);
    }

    private static void knock(AncientDragonEntity dragon, LivingEntity victim, double strength) {
        Vec3d away = victim.getPos().subtract(dragon.getPos()).multiply(1, 0, 1);
        if (away.lengthSquared() < 1.0E-4) {
            away = new Vec3d(1, 0, 0);
        }
        away = away.normalize();
        victim.takeKnockback(strength, -away.x, -away.z);
        victim.velocityModified = true;
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("LegendaryActions", this.points);
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("LegendaryActions")) {
            this.points = MathHelper.clamp(nbt.getInt("LegendaryActions"), 0, POINTS);
        }
    }
}
