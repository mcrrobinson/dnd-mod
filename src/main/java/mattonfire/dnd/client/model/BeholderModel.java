package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.BeholderEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/** The Beholder: tilts its body to its gaze and shuts the eyes that are closed (eyelid instead of eye). */
public class BeholderModel extends GeoModel<BeholderEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/beholder.geo.json");
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/entity/beholder/beholder.png");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/beholder.animation.json");

    @Override
    public Identifier getModelResource(BeholderEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(BeholderEntity object) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(BeholderEntity object) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(BeholderEntity beholder, long instanceId, AnimationState<BeholderEntity> animationState) {
        super.setCustomAnimations(beholder, instanceId, animationState);
        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (body != null && data != null) {
            body.setRotX(data.headPitch() * MathHelper.RADIANS_PER_DEGREE);
        }
        boolean shut = beholder.isCentralEyeShut();
        setHidden("central_eye", shut);
        setHidden("central_lid", !shut);
        BeholderEntity.Ray[] rays = BeholderEntity.Ray.values();
        for (int stalk = 0; stalk < BeholderEntity.STALKS; stalk++) {
            String name = rays[stalk / 2].id + (stalk % 2 == 0 ? "_left" : "_right");
            boolean closed = beholder.isEyeClosed(stalk);
            setHidden("eye_" + name, closed);
            setHidden("lid_" + name, !closed);
        }
    }

    private void setHidden(String bone, boolean hidden) {
        CoreGeoBone geoBone = this.getAnimationProcessor().getBone(bone);
        if (geoBone != null) {
            geoBone.setHidden(hidden);
        }
    }
}
