package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Rest.Charges;
import mattonfire.dnd.classes.Rest.RestSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;

/**
 * Draws the mana bar. Called from {@code InGameHudMixin} at the end of
 * {@code InGameHud.renderStatusBars}, so it shares the health/food bars'
 * visibility (hidden in creative and spectator) and draws beneath chat.
 */
public final class PowerupOverlay {
    private static final Identifier FULL_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/full.png");
    private static final Identifier EMPTY_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/empty.png");
    private static final Identifier CHARGE_FULL = new Identifier(DnDClasses.MOD_ID, "textures/power/charge_full.png");
    private static final Identifier CHARGE_EMPTY = new Identifier(DnDClasses.MOD_ID,
            "textures/power/charge_empty.png");
    private static final Identifier CHARGE_TEMP = new Identifier(DnDClasses.MOD_ID, "textures/power/charge_temp.png");
    private static final int GEM = 7;
    /** Right edge of the mana pips, which the gems line up with. */
    private static final int BAR_RIGHT = 91;

    private PowerupOverlay() {
    }

    /**
     * Top of the mana row: the first free row above vanilla's right-hand status bars. Mirrors
     * {@code InGameHud.renderStatusBars}/{@code renderMountHealth}: food (or the first row of
     * mount hearts) at height - 39, extra mount heart rows 10px apiece above it, then the air
     * bubbles while they're showing.
     */
    private static int barY(PlayerEntity player, int scaledHeight) {
        int y = scaledHeight - 49;
        if (player.getVehicle() instanceof LivingEntity mount && mount.isLiving()) {
            int hearts = Math.min((int) (mount.getMaxHealth() + 0.5F) / 2, 30);
            int rows = (int) Math.ceil(hearts / 10.0);
            if (rows > 1) {
                y -= (rows - 1) * 10;
            }
        }
        if (player.isSubmergedIn(FluidTags.WATER) || player.getAir() < player.getMaxAir()) {
            y -= 10;
        }
        return y;
    }

    public static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        int x = client.getWindow().getScaledWidth() / 2;
        int y = barY(client.player, client.getWindow().getScaledHeight());
        int mana = ((IEntityDataSaver) client.player).getPersistentData().getInt("mana");

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, EMPTY_POWER);
        for (int i = 0; i < DnDClasses.MANA_ICONS; i++) {
            DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y, 0, 0, 9, 9, 9, 9);
        }

        RenderSystem.setShaderTexture(0, FULL_POWER);
        for (int i = 0; i < Math.min(mana, DnDClasses.MANA_ICONS); i++) {
            DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y, 0, 0, 9, 9, 9, 9);
        }

        RestSnapshot rest = RestSnapshot.client;
        boolean charges = rest.enabled() && rest.max() > 0;
        if (charges) {
            renderCharges(matrices, rest, x, y);
        }

        // The Dragonborn Breath Weapon's cooldown, right of the pips
        mattonfire.dnd.classes.Client.BreathWeaponClient.renderIcon(matrices, x + BAR_RIGHT + 2, y);
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        // Underline the pips the equipped active costs, gold once it can be used.
        SkillNode active = ClassProgress.client.activeNode();
        if (active != null) {
            int cost = Math.min(mattonfire.dnd.classes.Progression.ClassSkills.manaCostFor(ClassProgress.client, active),
                    DnDClasses.MANA_ICONS);
            boolean affordable = mana >= cost && (!charges || rest.charges() + rest.temp() >= Charges.cost(active));
            int color = affordable ? 0xFFE0B040 : 0xFF505050;
            // In the 1px gap below the pips, above the food (or air/mount) row
            DrawableHelper.fill(matrices, x + 10, y + 9, x + 10 + cost * 9 - 1, y + 10, color);
        }

        // Vanilla keeps drawing (mount health) with the GUI icons texture it bound earlier.
        RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
    }

    /**
     * The charge gems in the row above the mana pips, right-aligned with them:
     * temporary charges (gold) on the right, then full, then empty.
     */
    private static void renderCharges(MatrixStack matrices, RestSnapshot rest, int x, int y) {
        int max = Math.min(rest.max(), Charges.MAX_SHOWN);
        int temp = Math.min(rest.temp(), 3);
        int full = Math.min(rest.charges(), max);
        int gemY = y - GEM - 1;
        int right = x + BAR_RIGHT;
        for (int i = 0; i < temp + max; i++) {
            Identifier texture = i < temp ? CHARGE_TEMP : i < temp + full ? CHARGE_FULL : CHARGE_EMPTY;
            RenderSystem.setShaderTexture(0, texture);
            DrawableHelper.drawTexture(matrices, right - (i + 1) * (GEM + 1) + 1, gemY, 0, 0, GEM, GEM, GEM, GEM);
        }

        // A rest in progress: a thin bar left of the gems, filling up.
        if (rest.sessionKind() != 0 && rest.sessionTotal() > 0) {
            int barRight = right - (temp + max) * (GEM + 1) - 2;
            int barLeft = x + 10;
            if (barRight - barLeft > 10) {
                int filled = (barRight - barLeft) * Math.min(rest.sessionProgress(), rest.sessionTotal())
                        / rest.sessionTotal();
                int barY = gemY + GEM / 2 - 1;
                DrawableHelper.fill(matrices, barLeft, barY, barRight, barY + 3, 0xA0000000);
                DrawableHelper.fill(matrices, barLeft, barY, barLeft + filled, barY + 3,
                        rest.sessionKind() == 1 ? 0xFFE0B040 : 0xFF60A0F0);
            }
        }
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
    }
}
