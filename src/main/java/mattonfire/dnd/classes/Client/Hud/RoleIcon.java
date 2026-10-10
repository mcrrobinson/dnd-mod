package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PartyRole;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Draws the 7x7 party role icons from {@code textures/gui/roles.png}. */
@Environment(EnvType.CLIENT)
public final class RoleIcon {
    public static final Identifier TEXTURE = new Identifier(DnDClasses.MOD_ID, PartyRole.ICON_TEXTURE);

    private RoleIcon() {
    }

    public static void draw(MatrixStack matrices, int x, int y, PartyRole role) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        DrawableHelper.drawTexture(matrices, x, y, role.iconU(), 0, PartyRole.ICON_SIZE, PartyRole.ICON_SIZE,
                PartyRole.ICON_TEXTURE_WIDTH, PartyRole.ICON_TEXTURE_HEIGHT);
    }
}
