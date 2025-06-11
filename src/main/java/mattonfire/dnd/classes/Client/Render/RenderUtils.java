package mattonfire.dnd.classes.Client.Render;

import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/*
 * Utility function for rendering. Most importantly contains lineToRenderList.
 */
public class RenderUtils {
    public static ArrayList<Line> lineToRenderList = new ArrayList<>();

    public static void drawLine(Vec3d posA, Vec3d posB, Color color) {
        lineToRenderList.add(new Line(posA, posB, color));
    }

    public static void drawCubeAtBlock(BlockPos blockPos, Color cubeColor) {
        Vec3d posA = new Vec3d(blockPos.getX() + 0, blockPos.getY(), blockPos.getZ() + 0);
        Vec3d posB = new Vec3d(posA.x + 0, posA.y + 0, posA.z + 1);
        Vec3d posC = new Vec3d(posA.x + 1, posA.y + 0, posA.z + 0);
        Vec3d posD = new Vec3d(posA.x + 1, posA.y + 0, posA.z + 1);
        Vec3d posE = new Vec3d(posA.x + 0, posA.y + 1, posA.z + 0);
        Vec3d posF = new Vec3d(posA.x + 0, posA.y + 1, posA.z + 1);
        Vec3d posG = new Vec3d(posA.x + 1, posA.y + 1, posA.z + 0);
        Vec3d posH = new Vec3d(posA.x + 1, posA.y + 1, posA.z + 1);

        drawLine(posA, posB, cubeColor);
        drawLine(posB, posD, cubeColor);
        drawLine(posD, posC, cubeColor);
        drawLine(posC, posA, cubeColor);
        drawLine(posA, posE, cubeColor);
        drawLine(posB, posF, cubeColor);
        drawLine(posD, posH, cubeColor);
        drawLine(posC, posG, cubeColor);
        drawLine(posE, posF, cubeColor);
        drawLine(posF, posH, cubeColor);
        drawLine(posH, posG, cubeColor);
        drawLine(posG, posE, cubeColor);
    }

    public static void drawCircleAtPos(Vec3d pos, Color circleColor, double radius, int segments) {
        // Center the circle on the given position
        Vec3d center = new Vec3d(pos.x, pos.y, pos.z);

        // Loop through the number of segments to create the circular shape
        Vec3d prevPoint = null;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            Vec3d currentPoint = new Vec3d(x, center.y, z);

            // Draw a line from the previous point to the current point
            if (prevPoint != null) {
                drawLine(prevPoint, currentPoint, circleColor);
            }

            prevPoint = currentPoint;
        }
    }

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

    public static void drawCubeAtPos(Vec3d pos, Color cubeColor) {
        // Offset the position to center the cube on the user
        Vec3d posA = new Vec3d(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5);
        Vec3d posB = new Vec3d(posA.x + 0, posA.y + 0, posA.z + 1);
        Vec3d posC = new Vec3d(posA.x + 1, posA.y + 0, posA.z + 0);
        Vec3d posD = new Vec3d(posA.x + 1, posA.y + 0, posA.z + 1);
        Vec3d posE = new Vec3d(posA.x + 0, posA.y + 1, posA.z + 0);
        Vec3d posF = new Vec3d(posA.x + 0, posA.y + 1, posA.z + 1);
        Vec3d posG = new Vec3d(posA.x + 1, posA.y + 1, posA.z + 0);
        Vec3d posH = new Vec3d(posA.x + 1, posA.y + 1, posA.z + 1);

        drawLine(posA, posB, cubeColor);
        drawLine(posB, posD, cubeColor);
        drawLine(posD, posC, cubeColor);
        drawLine(posC, posA, cubeColor);
        drawLine(posA, posE, cubeColor);
        drawLine(posB, posF, cubeColor);
        drawLine(posD, posH, cubeColor);
        drawLine(posC, posG, cubeColor);
        drawLine(posE, posF, cubeColor);
        drawLine(posF, posH, cubeColor);
        drawLine(posH, posG, cubeColor);
        drawLine(posG, posE, cubeColor);
    }

    public static void clear() {
        lineToRenderList.clear();
    }
}
