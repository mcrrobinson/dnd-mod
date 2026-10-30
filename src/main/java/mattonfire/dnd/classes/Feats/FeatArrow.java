package mattonfire.dnd.classes.Feats;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.Vec3d;

/**
 * Sharpshooter's marks on an arrow ({@code PersistentProjectileHitMixin}, saved as NBT {@code dndFeat}): which
 * feats it was loosed with and where from, for Long Shot.
 */
public interface FeatArrow {
    /** Loosed by a Sharpshooter: Long Shot and past cover. */
    int SHARPSHOOTER = 1;
    /** Loosed with Steady Aim on (and fully drawn). */
    int STEADY = 2;

    int dnd$featFlags();

    void dnd$setFeatFlags(int flags);

    @Nullable
    Vec3d dnd$origin();

    void dnd$setOrigin(@Nullable Vec3d origin);
}
