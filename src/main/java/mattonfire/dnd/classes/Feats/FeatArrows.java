package mattonfire.dnd.classes.Feats;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Sharpshooter on bows and crossbows ({@code BowItemMixin}, {@code CrossbowItemMixin}): arrows are marked when
 * loosed ({@link FeatArrow}); Steady Aim multiplies their damage and spread; Long Shot scales the hit by distance
 * ({@code PersistentProjectileHitMixin}); and marked arrows aren't blocked by shields ({@code ShieldArrowMixin}).
 */
public final class FeatArrows {
    public static final double STEADY_DAMAGE = 1.4;
    public static final float STEADY_SPREAD = 2.5F;
    public static final double LONG_SHOT_RANGE = 24.0;
    public static final float LONG_SHOT_DAMAGE = 1.25F;

    private FeatArrows() {
    }

    /**
     * Marks an arrow as it's loosed. {@code fullDraw}: a fully drawn bow or any crossbow shot, the only shots Steady
     * Aim works on. Returns the flags set.
     */
    public static int mark(PersistentProjectileEntity arrow, LivingEntity shooter, boolean fullDraw) {
        if (arrow == null || shooter.getWorld().isClient || !(shooter instanceof PlayerEntity player)
                || !Feats.has(player, Feat.SHARPSHOOTER) || !(arrow instanceof FeatArrow marked))
            return 0;
        int flags = FeatArrow.SHARPSHOOTER;
        if (fullDraw && Feats.toggleOn(player))
            flags |= FeatArrow.STEADY;
        marked.dnd$setFeatFlags(flags);
        marked.dnd$setOrigin(arrow.getPos());
        return flags;
    }

    public static boolean isSteady(Entity arrow) {
        return arrow instanceof FeatArrow marked && (marked.dnd$featFlags() & FeatArrow.STEADY) != 0;
    }

    /** Steady Aim's spread. */
    public static float divergence(Entity arrow, float divergence) {
        return isSteady(arrow) ? divergence * STEADY_SPREAD : divergence;
    }

    /** Steady Aim's damage, once the arrow's other bonuses are on (Power, +N). */
    public static void applySteadyDamage(PersistentProjectileEntity arrow) {
        if (isSteady(arrow)) {
            double before = arrow.getDamage();
            arrow.setDamage(before * STEADY_DAMAGE);
            DnDClasses.LOGGER.info("[Feats] Steady Aim: arrow damage {} -> {}", String.format("%.2f", before),
                    String.format("%.2f", arrow.getDamage()));
        }
    }

    /** Long Shot: x{@value #LONG_SHOT_DAMAGE} to a hit {@value #LONG_SHOT_RANGE}+ blocks from where it was loosed. */
    public static float longShot(PersistentProjectileEntity arrow, Entity hit, float amount) {
        if (!(arrow instanceof FeatArrow marked) || (marked.dnd$featFlags() & FeatArrow.SHARPSHOOTER) == 0)
            return amount;
        Vec3d origin = marked.dnd$origin();
        if (origin == null)
            return amount;
        double distance = origin.distanceTo(hit.getPos());
        if (distance < LONG_SHOT_RANGE) {
            DnDClasses.LOGGER.info("[Feats] Sharpshooter arrow hit {} at {} blocks: {}", hit.getName().getString(),
                    String.format("%.1f", distance), amount);
            return amount;
        }
        float scaled = amount * LONG_SHOT_DAMAGE;
        DnDClasses.LOGGER.info("[Feats] Sharpshooter Long Shot on {} at {} blocks: {} -> {}",
                hit.getName().getString(), String.format("%.1f", distance), amount, scaled);
        return scaled;
    }

    /** Past cover: a Sharpshooter's arrows aren't blocked by shields. */
    public static boolean ignoresShields(DamageSource source) {
        return source.getSource() instanceof FeatArrow marked
                && (marked.dnd$featFlags() & FeatArrow.SHARPSHOOTER) != 0;
    }
}
