package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.BrewingStandBlockEntity;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccessor {
    @Accessor("brewTime")
    int getBrewTime();
}
