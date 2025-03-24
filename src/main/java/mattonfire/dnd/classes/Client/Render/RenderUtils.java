package mattonfire.dnd.classes.Client.Render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

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
