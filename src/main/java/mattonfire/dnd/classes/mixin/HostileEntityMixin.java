package mattonfire.dnd.classes.mixin;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(MobEntity.class)
public class HostileEntityMixin {
    private LivingEntity target;

    @Inject(at = @At("RETURN"), method = "setTarget")
    public void setTarget(@Nullable LivingEntity target, CallbackInfo info) {
        if (target instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) target;

            if (((PlayerEntityExt) player).getDndClass() == DndCharacter.BARD) {
                UUID playerId = player.getUuid();

                // If player is NOT already affected, add them
                if (!DnDClasses.effectTimestamps.containsKey(playerId)) {
                    DnDClasses.effectTimestamps.put(playerId, (long) player.getServer().getTicks());
                }

                this.target = null;
            }
        }
    }

}
