package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Accessor("hungerManager")
    HungerManager getHungerManager();
}
