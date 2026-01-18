package mattonfire.dnd.classes.Client;

import net.minecraft.util.math.Vec3d;

public class SphereRenderData {
    public Vec3d pos;
    public float radius;
    public int colorARGB;
    public long endTick;

    public SphereRenderData(Vec3d pos, float radius, int colorARGB, long endTick) {
        this.pos = pos;
        this.radius = radius;
        this.colorARGB = colorARGB;
        this.endTick = endTick;
    }
}