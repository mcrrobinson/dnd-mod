package mattonfire.dnd.classes.Client;

import net.minecraft.util.math.Vec3d;

public class MySphereRenderState {
    public static boolean shouldRenderSphere = false;
    public static Vec3d spherePos = Vec3d.ZERO;
    public static long startTick = 0;
    /** How far the sphere grows: the blast's reach. */
    public static float maxRadius = 0;
    public static final long DURATION_TICKS = 20; // 1 second at 20 TPS
}