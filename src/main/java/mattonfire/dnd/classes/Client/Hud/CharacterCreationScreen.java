package mattonfire.dnd.classes.Client.Hud;

import mattonfire.dnd.classes.Client.Keybinds.ModKeybinds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * The screen for {@link CharacterCreationHud}. Escape doesn't close it ({@link ModKeybinds}), and when the
 * window or GUI scale changes across the wide/compact boundary it rebuilds the layout with the same picks.
 */
@Environment(EnvType.CLIENT)
public class CharacterCreationScreen extends ModKeybinds {
    private final CharacterCreationHud hud;

    public CharacterCreationScreen(CharacterCreationHud hud) {
        super(hud);
        this.hud = hud;
    }

    @Override
    protected void reposition(int screenWidth, int screenHeight) {
        super.reposition(screenWidth, screenHeight);
        if (!hud.fits(screenWidth)) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.currentScreen == this) {
                    client.setScreen(new CharacterCreationScreen(new CharacterCreationHud(hud.choice())));
                }
            });
        }
    }
}
