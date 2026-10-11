package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;

/** Lets DevScript's {@code trade} step highlight the trade it picks, like clicking it would. */
@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("selectedIndex")
    void setSelectedIndex(int index);
}
