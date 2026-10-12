package mattonfire.dnd.classes.Client.Render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/** Draws the Wizard power-up's expanding sphere (see DndClassesClient). */
public class RenderUtils {
    public static void renderSolidSphere(MatrixStack matrices, Vec3d pos, float radius, int argb, Vec3d cameraPos,
            int stacks, int slices) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        matrices.push();
        matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        for (int i = 0; i < stacks; ++i) {
            double phi1 = Math.PI * i / stacks;
            double phi2 = Math.PI * (i + 1) / stacks;

            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int j = 0; j <= slices; ++j) {
                double theta = 2 * Math.PI * j / slices;

                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta));

                float x2 = (float) (radius * Math.sin(phi2) * Math.cos(theta));
                float y2 = (float) (radius * Math.cos(phi2));
                float z2 = (float) (radius * Math.sin(phi2) * Math.sin(theta));

                buffer.vertex(matrices.peek().getPositionMatrix(), x1, y1, z1).color(r, g, b, a).next();
                buffer.vertex(matrices.peek().getPositionMatrix(), x2, y2, z2).color(r, g, b, a).next();
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
