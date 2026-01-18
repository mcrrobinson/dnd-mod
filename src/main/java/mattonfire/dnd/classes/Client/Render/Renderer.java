package mattonfire.dnd.classes.Client.Render;

import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/*
 * Draws a line. Called by Line.Draw(). Line.Draw() is called in HaxTestClient
 * for each line in RenderUtils.lineToDrawList.
 */
public class Renderer {

    public static void drawLine3D(Matrix4f matrix4f, Vec3d pos, Vec3d pos2, float r, float g, float b, float a) {
        RenderSystem.setShaderColor(r, g, b, a);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionProgram);

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);

        bufferBuilder.vertex(matrix4f, (float) pos.x, (float) pos.y, (float) pos.z).next();
        bufferBuilder.vertex(matrix4f, (float) pos2.x, (float) pos2.y, (float) pos2.z).next();

        tessellator.draw();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        // RenderSystem.enableDepthTest();
        // RenderSystem.disableBlend();

        // Tessellator tessellator = Tessellator.getInstance();
        // BufferBuilder bufferBuilder = tessellator.getBuffer();
        // bufferBuilder.clear();
    }
}