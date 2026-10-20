package mattonfire.dnd.classes.Rest;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The invisible seat a player rides during a campfire short rest, so they sit on the ground
 * with their legs out, facing the fire. Riding gives the sitting pose, the lower camera and the
 * multiplayer sync for free.
 *
 * <p>The seat is never saved, can't be hit, and removes itself (standing its rider up where they
 * sat down) as soon as it has no rider or its rider is no longer resting, so it can't outlive the
 * rest whichever way that ends. Sneaking gets up too, once the sneak used to sit down has been
 * let go (vanilla's sneak-to-dismount is turned off for seats in {@code RestSeatDismountMixin}).
 */
public class RestSeatEntity extends Entity {
    /** How high the sitting player's hips are above the ground. */
    public static final double HIP_HEIGHT = 0.1;
    /** A rider's feet are this far below their vehicle ({@code PlayerEntity.getHeightOffset}). */
    private static final double RIDER_OFFSET = 0.35;
    /** A full-size player's hips are this far above their feet. */
    private static final double HIPS = 0.75;

    /** Where the rider stood before sitting, and stands again when they get up. */
    private Vec3d standPos;
    /** Whether the rider has let go of sneak since sitting down, so a new sneak gets them up. */
    private boolean sneakReleased;

    public RestSeatEntity(EntityType<? extends RestSeatEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /** Sits the player down on a new seat where they stand, turned to face {@code campfire}. */
    public static void sit(ServerPlayerEntity player, BlockPos campfire) {
        standUp(player);
        Vec3d fire = Vec3d.ofCenter(campfire);
        float yaw = (float) (MathHelper.atan2(fire.z - player.getZ(), fire.x - player.getX())
                * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
        RestSeatEntity seat = new RestSeatEntity(ModEntityTypes.REST_SEAT, player.getWorld());
        seat.standPos = player.getPos();
        seat.refreshPositionAndAngles(player.getX(), player.getY() - sitDrop(player), player.getZ(), yaw, 0);
        player.getWorld().spawnEntity(seat);
        // Turning the player needs a teleport: their own client owns their rotation.
        player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), yaw, player.getPitch());
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
        if (!player.startRiding(seat, true)) {
            seat.discard();
        }
    }

    /** Stands the player up if they're sitting on a seat (the seat then removes itself). */
    public static void standUp(ServerPlayerEntity player) {
        if (player.getVehicle() instanceof RestSeatEntity seat) {
            player.stopRiding();
            seat.discard();
        }
    }

    /**
     * How far below the player's feet their seat goes, so their hips rest {@value #HIP_HEIGHT} above
     * the ground: 0.3 for a full-size player, less for the smaller races ({@code RaceSize}).
     */
    static double sitDrop(PlayerEntity player) {
        double scale = player.getDimensions(EntityPose.STANDING).height / EntityType.PLAYER.getHeight();
        return HIPS * scale - HIP_HEIGHT - RIDER_OFFSET;
    }

    public static boolean isSeated(Entity entity) {
        return entity.getVehicle() instanceof RestSeatEntity;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        Entity rider = getFirstPassenger();
        if (!(rider instanceof ServerPlayerEntity player) || !player.isAlive()
                || !RestSession.isResting(player)) {
            if (rider != null) {
                rider.stopRiding();
            }
            discard();
            return;
        }
        if (!player.isSneaking()) {
            sneakReleased = true;
        } else if (sneakReleased) {
            RestSession.cancel(player, "you got up");
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        return standPos != null ? standPos : getPos().add(0, HIPS - HIP_HEIGHT - RIDER_OFFSET, 0);
    }

    @Override
    public double getMountedHeightOffset() {
        return 0;
    }

    @Override
    public boolean shouldSave() {
        return false;
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
}
