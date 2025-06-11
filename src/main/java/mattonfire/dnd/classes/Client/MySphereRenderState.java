package mattonfire.dnd.classes.Client;

import net.minecraft.util.math.Vec3d;

public class MySphereRenderState {
    public static boolean shouldRenderSphere = false;
    public static Vec3d spherePos = Vec3d.ZERO;
    public static long startTick = 0;
    public static final long DURATION_TICKS = 20; // 2 seconds at 20 TPS
}