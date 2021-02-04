package mattonfire.dnd.classes.gui;

import org.lwjgl.glfw.GLFW;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;

public class Screen extends CottonClientScreen {

    public Screen(GuiDescription description) {
        super(description);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}
