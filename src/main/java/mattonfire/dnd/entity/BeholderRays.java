package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mattonfire.dnd.classes.SkillChecks.SaveResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link BeholderEntity}'s eye rays: while its great eye is shut, its open eyestalks charge up
 * (aimed where the target stood) and fire one ray each, one or two at a time, preferring a different
 * ray from the last. An eye shut by a hit drops its charge. Not saved.
 */
final class BeholderRays {
    private static final int CHARGE_TICKS = 16;
    private static final int RAY_COOLDOWN = 30;
    private static final int ENRAGED_RAY_COOLDOWN = 18;
    private static final double RAY_RANGE = 32.0;

    private static final class Charge {
        final int stalk;
        final Vec3d aim;
        int ticks;

        Charge(int stalk, Vec3d aim, int ticks) {
            this.stalk = stalk;
            this.aim = aim;
            this.ticks = ticks;
        }
    }

    private final BeholderEntity beholder;
    private final List<Charge> charges = new ArrayList<>();
    private int rayCooldown = RAY_COOLDOWN;
    @Nullable
    private BeholderEntity.Ray lastRay;

    BeholderRays(BeholderEntity beholder) {
        this.beholder = beholder;
    }

    /** Drops eyestalk {@code stalk}'s charge (its eye was shut). */
    void cancel(int stalk) {
        this.charges.removeIf(charge -> charge.stalk == stalk);
    }

    /** Drops every charge. */
    void clear() {
        this.charges.clear();
    }

    /** Drops every charge and fires the first volley half a second after a switch to rays. */
    void restart() {
        this.charges.clear();
        this.rayCooldown = 10;
    }

    /** Charges and fires its eye rays at {@code target} (while its great eye is shut). */
    void tick(LivingEntity target) {
        ServerWorld world = (ServerWorld) this.beholder.world;
        Iterator<Charge> it = this.charges.iterator();
        while (it.hasNext()) {
            Charge charge = it.next();
            Vec3d eye = this.beholder.stalkEye(charge.stalk);
            world.spawnParticles(rayOf(charge.stalk).dust, eye.x, eye.y, eye.z, 3, 0.15, 0.15, 0.15, 0.0);
            if (--charge.ticks <= 0) {
                it.remove();
                this.fireRay(world, charge);
            }
        }

        if (this.charges.isEmpty() && --this.rayCooldown <= 0 && this.beholder.canSee(target)) {
            int volley = this.beholder.isEnraged() ? 2 : 1;
            for (int i = 0; i < volley; i++) {
                int stalk = this.pickStalk();
                if (stalk < 0) {
                    break;
                }
                // Aimed where the target is now: it has CHARGE_TICKS to get out of the way.
                this.charges.add(new Charge(stalk, target.getBoundingBox().getCenter(), CHARGE_TICKS));
                this.lastRay = rayOf(stalk);
            }
            if (!this.charges.isEmpty()) {
                this.beholder.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.6F);
            }
            this.rayCooldown = this.beholder.isEnraged() ? ENRAGED_RAY_COOLDOWN : RAY_COOLDOWN;
        }
    }

    private static BeholderEntity.Ray rayOf(int stalk) {
        return BeholderEntity.Ray.values()[stalk / 2];
    }

    /** A random open eye, preferring a different ray from the last one; -1 if every eye is shut. */
    private int pickStalk() {
        List<Integer> open = new ArrayList<>();
        List<Integer> fresh = new ArrayList<>();
        for (int i = 0; i < BeholderEntity.STALKS; i++) {
            final int stalk = i;
            if (this.beholder.isEyeClosed(i) || this.charges.stream().anyMatch(c -> rayOf(c.stalk) == rayOf(stalk))) {
                continue;
            }
            open.add(i);
            if (rayOf(i) != this.lastRay) {
                fresh.add(i);
            }
        }
        List<Integer> pool = fresh.isEmpty() ? open : fresh;
        return pool.isEmpty() ? -1 : pool.get(this.beholder.getRandom().nextInt(pool.size()));
    }

    private void fireRay(ServerWorld world, Charge charge) {
        BeholderEntity.Ray ray = rayOf(charge.stalk);
        MobActions.log(this.beholder, "ray " + ray, this.beholder.getTarget());
        Vec3d from = this.beholder.stalkEye(charge.stalk);
        Vec3d dir = charge.aim.subtract(from);
        if (dir.lengthSquared() < 1.0E-4) {
            return;
        }
        Vec3d to = from.add(dir.normalize().multiply(RAY_RANGE));
        BlockHitResult block = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this.beholder));
        if (block.getType() != HitResult.Type.MISS) {
            to = block.getPos();
        }
        EntityHitResult hit = ProjectileUtil.getEntityCollision(world, this.beholder, from, to, new Box(from, to).expand(1.0),
                EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.and(e -> e instanceof LivingEntity && e.isAlive()
                        && !(e instanceof DragonPart) && e != this.beholder));
        if (hit != null) {
            to = hit.getPos();
        }

        double length = from.distanceTo(to);
        Vec3d step = to.subtract(from).normalize().multiply(0.35);
        Vec3d p = from;
        for (double d = 0; d < length; d += 0.35) {
            world.spawnParticles(ray.dust, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            p = p.add(step);
        }
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5F, 1.4F);

        if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
            this.applyRay(ray, victim);
        }
    }

    /**
     * A ray's effect, after the victim's save against it ({@link BossSaves}, DC 15, 16 enraged; one roll per
     * ray): DEX against slowing (the sting still lands), STR against levitation, CON against enervation (half
     * damage) and WIS against fear (just a moment's darkness).
     */
    private void applyRay(BeholderEntity.Ray ray, LivingEntity victim) {
        boolean enraged = this.beholder.isEnraged();
        String tag = "ray:" + ray.id;
        switch (ray) {
            case SLOW -> {
                SaveResult save = BossSaves.save(victim, this.beholder, BossSaves.SLOWING_RAY, enraged, tag, BossSaves.RAY_WINDOW)
                        .onFailure(Text.translatable("save.dndclasses.effect.slowing_ray.failure")).roll();
                victim.damage(this.beholder.world.getDamageSources().indirectMagic(this.beholder, this.beholder), 2.0F);
                if (save.failed()) {
                    victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2), this.beholder);
                }
            }
            case LEVITATION -> {
                SaveResult save = BossSaves.save(victim, this.beholder, BossSaves.TELEKINETIC_RAY, enraged, tag, BossSaves.RAY_WINDOW)
                        .onFailure(Text.translatable("save.dndclasses.effect.telekinetic_ray.failure")).roll();
                if (save.failed()) {
                    victim.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 50, 1), this.beholder);
                }
            }
            case DAMAGE -> {
                DamageSource source = this.beholder.world.getDamageSources().indirectMagic(this.beholder, this.beholder);
                float full = enraged ? 10.0F : 8.0F;
                if (DragonSaves.isUnaffected(victim, source)) {
                    victim.damage(source, full); // Nothing to save against
                } else {
                    victim.damage(source, BossSaves.roll(victim, this.beholder, BossSaves.ENERVATION_RAY, enraged, tag,
                            BossSaves.RAY_WINDOW).damage(full));
                }
            }
            case FEAR -> {
                SaveResult save = BossSaves.save(victim, this.beholder, BossSaves.FEAR_RAY, enraged, tag, BossSaves.RAY_WINDOW)
                        .tags("fear")
                        .onFailure(Text.translatable("save.dndclasses.effect.fear_ray.failure")).roll();
                if (save.failed()) {
                    mattonfire.dnd.classes.Conditions.Conditions.of(victim, mattonfire.dnd.classes.Conditions.Condition.FRIGHTENED).source(this.beholder)
                            .key("fear_ray").ticks(BossSaves.FEAR_RAY_TICKS)
                            .saveLabel(BossSaves.FEAR_RAY.labelKey())
                            .repeatSave(BossSaves.FEAR_RAY.ability(),
                                    enraged ? BossSaves.LAST_PHASE_DC : BossSaves.FEAR_RAY.dc(),
                                    BossSaves.FEAR_REPEAT_TICKS)
                            .saveTags("fear").apply();
                    victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0), this.beholder);
                } else {
                    victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20, 0), this.beholder);
                }
            }
        }
    }
}
