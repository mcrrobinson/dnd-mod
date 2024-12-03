package mattonfire.dnd.classes.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(value = AbstractClientPlayerEntity.class, priority = 10)
public abstract class PreAbstractClientPlayerEntityMixin extends PlayerEntity {
  private float ZOOM_MODIFIER = 3.f;

  public PreAbstractClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
    super(world, pos, yaw, profile);
    // TODO Auto-generated constructor stub
  }

  @Inject(at = @At("TAIL"), method = "getSpeed()F", cancellable = true)
  private float getSpeed(CallbackInfoReturnable<Float> cb) {
    PlayerEntity playerEntity = (PlayerEntity) (Object) this;

    float modifier = 1.0F;

    if (playerEntity.isUsingItem() && playerEntity.getActiveItem().getItem() == Items.BOW) {
      int i = playerEntity.getItemUseTime();
      float g = (float) i / 20.0F;

      if (g > 1.0F) {
        g = 1.0F;
      } else {
        g *= g;
      }
      if (((PlayerEntityExt) playerEntity).dndClassExist() == 8) {
        modifier *= 1.0F - g * 0.15F * ZOOM_MODIFIER;
      } else {
        modifier *= 1.0F - g * 0.15F;
      }
    }
    return MathHelper.lerp(MinecraftClient.getInstance().options.fovEffectScale, 1.0F, modifier);
  }
}