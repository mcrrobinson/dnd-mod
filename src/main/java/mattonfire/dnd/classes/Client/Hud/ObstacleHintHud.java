package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleText;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Looking at a sealed obstacle within {@value #RANGE} blocks shows what it is, how hard it looks and
 * who can deal with it, just above the crosshair (the d20 panel goes below it). If you can't, it
 * names the classes that can, the fallback, and the nearest player whose class could.
 */
public final class ObstacleHintHud {
    public static final double RANGE = 5.0;
    /** How far away a player can be and still be suggested ("Ask Mira (Wizard)"). */
    public static final double ASK_RANGE = 32.0;

    private ObstacleHintHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private record Line(Text text, int color) {
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null || client.currentScreen != null || player.isSpectator()) {
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || hit.getPos().squaredDistanceTo(player.getEyePos()) > RANGE * RANGE) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (!ObstacleBlock.isSealed(state)) {
            return;
        }
        ObstacleType type = ((ObstacleBlock) state.getBlock()).type();
        Tier tier = client.world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(ObstacleText.name(type, tier).formatted(Formatting.BOLD), 0xC89BFF));
        Eligibility eligibility = type.eligibility(D20.classOf(player));
        Skill skill = type.skill();
        if (player.isCreative()) {
            lines.add(new Line(Text.translatable("obstacle.dndclasses.hint.creative"), 0xA0A0A0));
        } else if (eligibility.canTry() && skill != null) {
            // The synced character sheet: the same bonus the server rolls with
            int modifier = AbilityScores.sheet(player).check(skill);
            lines.add(new Line(ObstacleText.you(type), 0x9CE89C));
            lines.add(new Line(Text.translatable(type.allowsTakeYourTime(tier) ? "obstacle.dndclasses.hint.roll"
                    : "obstacle.dndclasses.hint.roll_only", Text.translatable(skill.translationKey()),
                    (modifier < 0 ? "" : "+") + modifier, tier.dc), 0xA0A0A0));
        } else {
            lines.add(new Line(ObstacleText.who(type), 0xE0E0E0));
            lines.add(new Line(ObstacleText.fallback(type), 0xA0A0A0));
            PlayerEntity ask = nearbySolver(client, player, type);
            if (ask != null) {
                lines.add(new Line(Text.translatable("obstacle.dndclasses.hint.ask", ask.getDisplayName(),
                        Progression.name(((PlayerEntityExt) ask).getDndClass())), 0xFFD23F));
            }
        }

        TextRenderer text = client.textRenderer;
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, text.getWidth(line.text()));
        }
        int left = (client.getWindow().getScaledWidth() - width) / 2;
        int height = lines.size() * 11;
        // Above the crosshair, clear of the d20 panel, the chat and the action bar below it
        int top = client.getWindow().getScaledHeight() / 2 - 16 - height;
        DrawableHelper.fill(matrices, left - 4, top - 3, left + width + 4, top + height + 1, 0x90000000);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            text.drawWithShadow(matrices, line.text(), left + (width - text.getWidth(line.text())) / 2.0f,
                    top + i * 11, line.color() | 0xFF000000);
        }
    }

    /** The nearest other player whose class could attempt it, if any. */
    private static PlayerEntity nearbySolver(MinecraftClient client, PlayerEntity self, ObstacleType type) {
        PlayerEntity best = null;
        double bestDistance = ASK_RANGE * ASK_RANGE;
        Vec3d at = self.getPos();
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == self || !(other instanceof PlayerEntityExt ext) || ext.getDndClass() == null
                    || ext.getDndClass() == DndCharacter.NONE || !type.eligibility(ext.getDndClass()).canTry()) {
                continue;
            }
            double distance = other.getPos().squaredDistanceTo(at);
            if (distance < bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }
}
