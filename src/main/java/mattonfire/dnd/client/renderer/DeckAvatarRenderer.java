package mattonfire.dnd.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.WitherSkeletonEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.AbstractSkeletonEntity;

/**
 * The Deck of Many Things' Avatar of Death: a wither skeleton in a black robe and hood (dyed leather it wears),
 * with a netherite hoe for a scythe, drawn a little taller than a wither skeleton.
 */
public class DeckAvatarRenderer extends WitherSkeletonEntityRenderer {
    /** A wither skeleton is drawn at 1.2; the Avatar at 1.3. */
    public static final float SCALE = 1.3F;

    public DeckAvatarRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void scale(AbstractSkeletonEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(SCALE, SCALE, SCALE);
    }
}
