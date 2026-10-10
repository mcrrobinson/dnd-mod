package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Mouse;

/** Lets DevScript move the cursor (for screen tooltips) without real mouse input. */
@Mixin(Mouse.class)
public interface MouseAccessor {
    @Accessor("x")
    void setX(double x);

    @Accessor("y")
    void setY(double y);
}
