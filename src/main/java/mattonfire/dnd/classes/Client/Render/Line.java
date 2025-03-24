package mattonfire.dnd.classes.Client.Render;

import mattonfire.dnd.classes.Client.DndClassesClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

public class Line {
    public Vec3d startPos;
    public Vec3d endPos;
    public Color color;

    public Line(Vec3d posA, Vec3d posB, Color lineColor) {
        startPos = posA;
        endPos = posB;
        // colors
        color = lineColor;
    }

    public void Draw(WorldRenderContext context) {
        Vec3d playerPos = DndClassesClient.MC.player.getCameraPosVec(0);
        // calculate relative position using given position (absolute) - player position
        // (absolute)
        Vec3d start = new Vec3d(this.startPos.x - playerPos.x, this.startPos.y - playerPos.y,
                this.startPos.z - playerPos.z);
        Vec3d end = new Vec3d(this.endPos.x - playerPos.x, this.endPos.y - playerPos.y, this.endPos.z - playerPos.z);
        Renderer.drawLine3D(
                context.matrixStack().peek().getPositionMatrix(),
                start,
                end,
                this.color.r, this.color.g, this.color.b, this.color.a);

    }
}