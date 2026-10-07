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
    private long renderedAt = Long.MIN_VALUE;

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
    }

    public boolean hasRenderedShapes(long worldTime) {
        return worldTime - this.renderedAt <= 2;
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
        int nx = Math.max(1, (int) Math.ceil(ex.length() / SLICE));
        int ny = Math.max(1, (int) Math.ceil(ey.length() / SLICE));
        int nz = Math.max(1, (int) Math.ceil(ez.length() / SLICE));
        Vec3d dx = ex.multiply(1.0 / nx);
        Vec3d dy = ey.multiply(1.0 / ny);
        Vec3d dz = ez.multiply(1.0 / nz);
        // Offsets from a slice's base corner to its min and max corners
        double loX = Math.min(0, dx.x) + Math.min(0, dy.x) + Math.min(0, dz.x);
        double loY = Math.min(0, dx.y) + Math.min(0, dy.y) + Math.min(0, dz.y);
        double loZ = Math.min(0, dx.z) + Math.min(0, dy.z) + Math.min(0, dz.z);
        double hiX = Math.max(0, dx.x) + Math.max(0, dy.x) + Math.max(0, dz.x);
        double hiY = Math.max(0, dx.y) + Math.max(0, dy.y) + Math.max(0, dz.y);
        double hiZ = Math.max(0, dx.z) + Math.max(0, dy.z) + Math.max(0, dz.z);
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                for (int k = 0; k < nz; k++) {
                    double x = o.x + dx.x * i + dy.x * j + dz.x * k;
                    double y = o.y + dx.y * i + dy.y * j + dz.y * k;
                    double z = o.z + dx.z * i + dy.z * j + dz.z * k;
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
