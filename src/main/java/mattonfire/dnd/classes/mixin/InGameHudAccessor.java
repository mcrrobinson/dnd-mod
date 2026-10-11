package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;

/** Lets DevScript log the action bar text, which never reaches the chat log. */
@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Accessor("overlayMessage")
    Text getOverlayMessage();

    @Accessor("overlayRemaining")
    int getOverlayRemaining();
}
