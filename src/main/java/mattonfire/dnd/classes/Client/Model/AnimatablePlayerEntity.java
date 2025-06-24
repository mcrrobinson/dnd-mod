
package mattonfire.dnd.classes.Client.Model;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.EntityType;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

public class AnimatablePlayerEntity extends PlayerEntity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // World world, BlockPos pos, float yaw,
    public AnimatablePlayerEntity(EntityType<? extends PlayerEntity> type, World world, BlockPos pos, float yaw,
            GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Register animation controllers here if needed
    }

    @Override
    public double getTick(Object object) {
        return this.age;
    }

    @Override
    public boolean isCreative() {
        // Return true if you want this entity to be considered in creative mode,
        // otherwise false
        return false;
    }

    @Override
    public boolean isSpectator() {
        // Return true if you want this entity to be considered a spectator, otherwise
        // false
        return false;
    }
}