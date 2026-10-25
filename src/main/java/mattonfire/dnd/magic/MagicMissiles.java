package mattonfire.dnd.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Magic Missile, shared by the Wand of Magic Missiles and the Drow Mage: glowing darts that curve onto the target
 * and always hit (no attack roll), dealt as one hit of magic damage (which ignores armour).
 */
public final class MagicMissiles {
    private MagicMissiles() {
    }

    /** {@code darts} darts from {@code caster} to {@code target}, {@code dartDamage} each. Returns whether it hurt. */
    public static boolean fire(ServerWorld world, LivingEntity caster, LivingEntity target, int darts, float dartDamage) {
        Vec3d from = caster.getEyePos().add(caster.getRotationVec(1.0F).multiply(0.6)).add(0, -0.2, 0);
        Vec3d to = target.getBoundingBox().getCenter();
        Vec3d dir = to.subtract(from);
        Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
        Vec3d up = side.crossProduct(dir).normalize();
        double bow = Math.min(2.0, dir.length() * 0.2);
        Vec3d[] bends = { side.multiply(bow), side.multiply(-bow), up.multiply(bow), up.multiply(-bow),
                side.add(up).multiply(bow * 0.7) };
        int steps = Math.max(8, (int) (dir.length() * 3));
        for (int d = 0; d < darts; d++) {
            Vec3d bend = bends[d % bends.length];
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3d p = from.add(dir.multiply(t)).add(bend.multiply(4 * t * (1 - t)));
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                if (i % 3 == 0)
                    world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        SoundCategory category = caster instanceof PlayerEntity ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, category, 0.8F, 1.6F);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, category, 1.0F, 1.2F);
        return target.damage(world.getDamageSources().indirectMagic(caster, caster), dartDamage * darts);
    }
}
