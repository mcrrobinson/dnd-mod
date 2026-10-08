package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A hittable section of a dragon (wing, neck, head, tail), modelled on vanilla's EnderDragonPart.
 * Parts are never added to the world; WorldMixin exposes them to hit detection and they forward
 * damage to their owner.
 * <p>
 * A part covers a group of model bones as many small boxes that follow the model's shape; its
 * bounding box only encloses them for broad-phase lookups, and ProjectileUtilMixin makes raycasts
 * test the small boxes instead.
 */
public class DragonPart extends Entity {
    public final MobEntity owner;
    public final String name;
    // Returned for rays that miss every shape, so vanilla's raycast finds no hit
    private static final Box MISS = new Box(0, -1.0E9, 0, 0, -1.0E9, 0);
    // Longest edge of the small boxes a cube is cut into, in blocks
    private static final double SLICE = 0.5;

    private Box partBox;
    private List<Box> shapes = List.of();
    // Not Long.MIN_VALUE: worldTime - MIN_VALUE overflows, and the part would count as rendered forever
    private long renderedAt = Long.MIN_VALUE / 2;
    private long renderStamp = Long.MIN_VALUE;
    // The pose (DragonPartLayout.Pose) and placement the current shapes were built for, so a dragon
    // that hasn't changed pose or turned doesn't rebuild them; null after the renderer set them
    private Object pose;
    private double poseX, poseY, poseZ;
    private float poseYaw;

    public DragonPart(MobEntity owner, String name) {
        super(owner.getType(), owner.world);
        this.owner = owner;
        this.name = name;
    }

    public void setShapes(List<Box> shapes) {
        if (shapes.isEmpty()) {
            // A part whose bones are all hidden: put it somewhere nothing can reach
            this.shapes = shapes;
            this.partBox = MISS;
            this.setBoundingBox(MISS);
            return;
        }
        Box box = shapes.get(0);
        for (Box shape : shapes) {
            box = box.union(shape);
        }
        this.shapes = shapes;
        this.partBox = box;
        this.setPos((box.minX + box.maxX) / 2, box.minY, (box.minZ + box.maxZ) / 2);
        this.setBoundingBox(box);
    }

    /** Client only: the shapes were taken from the animated model, so the rest pose shouldn't overwrite them. */
    public void setRenderedShapes(List<Box> shapes, long worldTime) {
        this.setShapes(shapes);
        this.renderedAt = worldTime;
        this.pose = null;
    }

    public boolean hasRenderedShapes(long worldTime) {
        return worldTime - this.renderedAt <= 2;
    }

    /** Client only: an id for the half tick the renderer last fitted the shapes in, so it can skip refits. */
    public long getRenderStamp() {
        return this.renderStamp;
    }

    public void setRenderStamp(long stamp) {
        this.renderStamp = stamp;
    }

    /** Shapes built from {@code pose} for a dragon at (x, y, z) facing {@code yaw}. */
    public void setPoseShapes(List<Box> shapes, Object pose, double x, double y, double z, float yaw) {
        this.setShapes(shapes);
        this.pose = pose;
        this.poseX = x;
        this.poseY = y;
        this.poseZ = z;
        this.poseYaw = yaw;
    }

    /** Whether the current shapes were built from this pose (same object) and facing, wherever the dragon was. */
    public boolean hasPoseShapes(Object pose, float yaw) {
        return this.pose == pose && this.poseYaw == yaw;
    }

    public boolean isPosedAt(double x, double y, double z) {
        return this.poseX == x && this.poseY == y && this.poseZ == z;
    }

    /** Shifts the posed shapes to a dragon that moved without turning or changing pose. */
    public void movePoseShapes(double x, double y, double z) {
        double dx = x - this.poseX, dy = y - this.poseY, dz = z - this.poseZ;
        List<Box> moved = new ArrayList<>(this.shapes.size());
        for (Box shape : this.shapes) {
            moved.add(shape.offset(dx, dy, dz));
        }
        this.setShapes(moved);
        this.poseX = x;
        this.poseY = y;
        this.poseZ = z;
    }

    public List<Box> getShapes() {
        return this.shapes;
    }

    /** The shape a ray from {@code from} to {@code to} hits first (or starts inside), or a box it can't hit. */
    public Box shapeHitBy(Vec3d from, Vec3d to) {
        Box nearest = MISS;
        double nearestDistance = Double.MAX_VALUE;
        for (Box shape : this.shapes) {
            if (shape.contains(from)) {
                return shape;
            }
            Optional<Vec3d> hit = shape.raycast(from, to);
            if (hit.isPresent()) {
                double distance = from.squaredDistanceTo(hit.get());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = shape;
                }
            }
        }
        return nearest;
    }

    /**
     * Adds a cube to {@code shapes} as small axis-aligned boxes, so a rotated cube isn't covered by one
     * big box that's mostly air. The cube is the parallelepiped at corner {@code o} spanned by edge
     * vectors {@code ex}, {@code ey} and {@code ez}; each edge is cut into slices of at most SLICE blocks.
     */
    public static void addCube(List<Box> shapes, Vec3d o, Vec3d ex, Vec3d ey, Vec3d ez) {
        addCube(shapes, o.x, o.y, o.z, ex.x, ex.y, ex.z, ey.x, ey.y, ey.z, ez.x, ez.y, ez.z);
    }

    /** {@link #addCube(List, Vec3d, Vec3d, Vec3d, Vec3d)} without the vectors, for code run every tick or frame. */
    public static void addCube(List<Box> shapes, double ox, double oy, double oz,
                               double exX, double exY, double exZ, double eyX, double eyY, double eyZ,
                               double ezX, double ezY, double ezZ) {
        int nx = Math.max(1, (int) Math.ceil(Math.sqrt(exX * exX + exY * exY + exZ * exZ) / SLICE));
        int ny = Math.max(1, (int) Math.ceil(Math.sqrt(eyX * eyX + eyY * eyY + eyZ * eyZ) / SLICE));
        int nz = Math.max(1, (int) Math.ceil(Math.sqrt(ezX * ezX + ezY * ezY + ezZ * ezZ) / SLICE));
        double dxX = exX / nx, dxY = exY / nx, dxZ = exZ / nx;
        double dyX = eyX / ny, dyY = eyY / ny, dyZ = eyZ / ny;
        double dzX = ezX / nz, dzY = ezY / nz, dzZ = ezZ / nz;
        // Offsets from a slice's base corner to its min and max corners
        double loX = Math.min(0, dxX) + Math.min(0, dyX) + Math.min(0, dzX);
        double loY = Math.min(0, dxY) + Math.min(0, dyY) + Math.min(0, dzY);
        double loZ = Math.min(0, dxZ) + Math.min(0, dyZ) + Math.min(0, dzZ);
        double hiX = Math.max(0, dxX) + Math.max(0, dyX) + Math.max(0, dzX);
        double hiY = Math.max(0, dxY) + Math.max(0, dyY) + Math.max(0, dzY);
        double hiZ = Math.max(0, dxZ) + Math.max(0, dyZ) + Math.max(0, dzZ);
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                for (int k = 0; k < nz; k++) {
                    double x = ox + dxX * i + dyX * j + dzX * k;
                    double y = oy + dxY * i + dyY * j + dzY * k;
                    double z = oz + dxZ * i + dyZ * j + dzZ * k;
                    shapes.add(new Box(x + loX, y + loY, z + loZ, x + hiX, y + hiY, z + hiZ));
                }
            }
        }
    }

    @Override
    protected Box calculateBoundingBox() {
        return this.partBox == null ? super.calculateBoundingBox() : this.partBox;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return this.owner.getPickBlockStack();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        // Its own attacks (fire breath) start inside its head
        if (source.getAttacker() == this.owner) {
            return false;
        }
        if (this.owner instanceof MultipartDragon dragon) {
            return dragon.damagePart(this, source, amount);
        }
        return this.owner.damage(source, amount);
    }

    /** Right-clicks on a part (taming, sitting, leads) go to the dragon. */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        return this.owner.interact(player, hand);
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        // Only the dragon itself gets struck, or a wide wingspan would catch its own lightning
    }

    @Override
    public boolean isPartOf(Entity entity) {
        return this == entity || this.owner == entity;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        if (this.partBox == null) {
            return super.getDimensions(pose);
        }
        return EntityDimensions.changing((float) this.partBox.getXLength(), (float) this.partBox.getYLength());
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
