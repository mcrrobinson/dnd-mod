package mattonfire.dnd.entity.kobold.ai;

import java.util.EnumSet;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.entity.kobold.KoboldTrapmakerEntity;
import mattonfire.dnd.entity.kobold.KoboldTraps;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A Kobold Trapmaker out of a fight fixes its tribe's traps (design 10, 4.2): it walks to a spent or disarmed kobold
 * trap within {@value #RANGE} blocks ({@link KoboldTraps}), tinkers with it for {@value #WORK_TICKS} ticks ("tink
 * tink", sparks) and re-arms it. A rockfall's rubble is cleared and a pit's lid put back ({@code rearm}). Logged as
 * {@code [Trap] kobold rearm <kind> at x y z (Kobold Trapmaker)}.
 */
public class KoboldRearmGoal extends Goal {
    public static final double RANGE = 24.0D;
    public static final int WORK_TICKS = 60;
    /** Close enough to work on it. */
    private static final double REACH = 2.5D;
    /** How often it looks for a trap to fix. */
    private static final int SCAN_INTERVAL = 40;
    /** Gives up on a trap it can't reach after this long. */
    private static final int GIVE_UP_TICKS = 600;

    private final KoboldTrapmakerEntity kobold;
    @Nullable
    private TrapTriggerBlockEntity trap;
    private Vec3d spot = Vec3d.ZERO;
    private int nextScan;
    private int ticks;
    private int working;

    public KoboldRearmGoal(KoboldTrapmakerEntity kobold) {
        this.kobold = kobold;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.kobold.getTarget() != null || this.kobold.age < this.nextScan
                || !(this.kobold.world instanceof ServerWorld world)) {
            return false;
        }
        this.nextScan = this.kobold.age + SCAN_INTERVAL;
        List<TrapTriggerBlockEntity> traps = KoboldTraps.needingRepair(world, this.kobold.getBlockPos(), RANGE);
        if (traps.isEmpty()) {
            return false;
        }
        this.trap = traps.get(0);
        this.spot = workSpot(this.trap);
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return this.trap != null && !this.trap.isRemoved() && !this.trap.isArmed() && this.kobold.getTarget() == null
                && this.ticks < GIVE_UP_TICKS;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.working = 0;
        this.kobold.getNavigation().startMovingTo(this.spot.x, this.spot.y, this.spot.z, 1.0D);
    }

    @Override
    public void stop() {
        this.trap = null;
        this.kobold.getNavigation().stop();
        // Didn't make it: look again later rather than straight away
        this.nextScan = this.kobold.age + SCAN_INTERVAL * 3;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.trap == null) {
            return;
        }
        this.ticks++;
        this.kobold.getLookControl().lookAt(this.spot.x, this.spot.y, this.spot.z);
        double dx = this.kobold.getX() - this.spot.x;
        double dz = this.kobold.getZ() - this.spot.z;
        if (dx * dx + dz * dz > REACH * REACH || Math.abs(this.kobold.getY() - this.spot.y) > 2.5D) {
            this.working = 0;
            if (this.ticks % 20 == 0 || this.kobold.getNavigation().isIdle()) {
                this.kobold.getNavigation().startMovingTo(this.spot.x, this.spot.y, this.spot.z, 1.0D);
            }
            return;
        }
        this.kobold.getNavigation().stop();
        ServerWorld world = (ServerWorld) this.kobold.world;
        if (this.working % 10 == 0) {
            world.playSound(null, BlockPos.ofFloored(this.spot), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.25F, 2.0F);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.spot.x, this.spot.y + 0.2D, this.spot.z, 4, 0.3D, 0.1D,
                    0.3D, 0.05D);
        }
        if (++this.working >= WORK_TICKS) {
            TrapTriggerBlockEntity done = this.trap;
            DnDClasses.LOGGER.info("[Trap] kobold rearm {} at {} ({})", done.kind().id(), done.getPos().toShortString(),
                    this.kobold.getName().getString());
            done.rearm("kobold rearm by " + this.kobold.getName().getString());
            this.trap = null;
        }
    }

    /** Where to stand to fix it: the middle of its tiles, on the floor. */
    static Vec3d workSpot(TrapTriggerBlockEntity trap) {
        List<BlockPos> tiles = new java.util.ArrayList<>(trap.rubbleTiles());
        if (tiles.isEmpty()) {
            for (BlockPos p : trap.linked()) {
                if (trap.getWorld() != null && trap.getWorld().getBlockState(p).getBlock()
                        instanceof mattonfire.dnd.classes.Blocks.SpikeTrapBlock) {
                    tiles.add(p.up());
                }
            }
        }
        if (tiles.isEmpty()) {
            tiles.add(trap.getPos().up());
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos p : tiles) {
            x += p.getX() + 0.5D;
            y += p.getY();
            z += p.getZ() + 0.5D;
        }
        return new Vec3d(x / tiles.size(), y / tiles.size(), z / tiles.size());
    }
}
