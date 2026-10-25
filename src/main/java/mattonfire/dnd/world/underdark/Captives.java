package mattonfire.dnd.world.underdark;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.entity.SettlementNpcEntity;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Reputation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Captives (design 9, 8.1; docs/structures/drow-outposts.md): surface folk the drow (and later the duergar and the
 * mind flayers) keep in their pens. Any mob can be one; it's the {@code Captive:1b} flag in its NBT (written by
 * {@code CaptiveMobMixin}, so {@code /summon dndclasses:hobbit ~ ~ ~ {Captive:1b}} makes one).
 * <ul>
 * <li>A captive has no AI and kneels (the crouching pose), and it never despawns. Drow don't attack it.</li>
 * <li>Opening its Chain Cage (a picked lock, or by hand once it's unlocked) or breaking the cage frees every captive
 * within {@value #CAGE_REACH} blocks of it (across; a block and a half up or down). A freed captive thanks you, gets its AI back, walks off and is gone
 * {@value #WALK_OFF_TICKS} ticks later.</li>
 * <li>The rescuer gains {@value #REPUTATION} with the captive's faction ({@link Reputation.Source#RESCUE}): a hobbit
 * for the hobbits, a mountain dwarf for the Mountain Dwarves, an elf for the Sylvan Court.</li>
 * </ul>
 */
public final class Captives {
    /** The NBT flag (a byte). */
    public static final String NBT = "Captive";
    /** When the captive was freed (world time), while it walks off. */
    public static final String FREED_NBT = "CaptiveFreedAt";
    /** How far from an opened cage captives are freed. */
    public static final double CAGE_REACH = 2.5D;
    /** Reputation with the captive's faction for freeing it. */
    public static final int REPUTATION = 40;
    /** A freed captive is gone this long after (60 s). */
    public static final int WALK_OFF_TICKS = 1200;
    /** How far a freed captive heads off at a time. */
    private static final int WALK_OFF_RANGE = 16;

    /** Implemented on every mob by {@code CaptiveMobMixin}. */
    public interface Holder {
        boolean dndclasses$isCaptive();

        void dndclasses$setCaptive(boolean captive);

        /** World time it was freed, or 0. */
        long dndclasses$freedAt();

        void dndclasses$setFreedAt(long time);
    }

    private Captives() {
    }

    /** Whether {@code entity} is a captive (still in its pen). */
    public static boolean isCaptive(@Nullable Entity entity) {
        return entity instanceof Holder holder && holder.dndclasses$isCaptive();
    }

    /** Makes {@code mob} a captive: call before it's added to the world (worldgen saves entities at once). */
    public static void bind(MobEntity mob) {
        ((Holder) mob).dndclasses$setCaptive(true);
        mob.setAiDisabled(true);
        mob.setPersistent();
        mob.setPose(EntityPose.CROUCHING);
    }

    /**
     * A cage at {@code cage} was opened or broken: frees the captives within {@link #CAGE_REACH} blocks of it, for
     * {@code rescuer} (null: nobody gains reputation). Returns how many were freed.
     */
    public static int freeNear(ServerWorld world, BlockPos cage, @Nullable PlayerEntity rescuer) {
        Box box = new Box(cage).expand(CAGE_REACH, 1.5D, CAGE_REACH);
        Vec3d at = Vec3d.ofBottomCenter(cage);
        int freed = 0;
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, mob -> isCaptive(mob)
                && sq(mob.getX() - at.x) + sq(mob.getZ() - at.z) <= CAGE_REACH * CAGE_REACH)) {
            free(mob, rescuer);
            freed++;
        }
        return freed;
    }

    private static double sq(double d) {
        return d * d;
    }

    /** Frees {@code mob}: thanks, reputation, and it walks off. */
    public static void free(MobEntity mob, @Nullable PlayerEntity rescuer) {
        Holder holder = (Holder) mob;
        if (!holder.dndclasses$isCaptive() || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        holder.dndclasses$setCaptive(false);
        holder.dndclasses$setFreedAt(Math.max(1L, world.getTime()));
        mob.setAiDisabled(false);
        mob.setPose(EntityPose.STANDING);
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        Faction faction = Reputation.factionOf(mob);
        String name = mob.getDisplayName().getString();
        if (rescuer instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("message.dndclasses.captive.thanks", mob.getDisplayName())
                    .formatted(Formatting.GREEN), false);
            if (faction != null) {
                Reputation.add(player, faction, REPUTATION, Reputation.Source.RESCUE);
            }
        }
        DnDClasses.LOGGER.info("[Captive] {} freed by {} ({})", name,
                rescuer == null ? "nobody" : rescuer.getName().getString(),
                faction == null ? "no faction" : "+" + REPUTATION + " " + faction.id());
        walkOff(mob, rescuer == null ? null : rescuer.getPos());
    }

    /** Every tick of a captive or a freed one (server side): keeps it kneeling, or walks it off and away. */
    public static void tick(MobEntity mob) {
        Holder holder = (Holder) mob;
        if (holder.dndclasses$isCaptive()) {
            if (!mob.isAiDisabled()) {
                mob.setAiDisabled(true);
            }
            if (mob.getPose() != EntityPose.CROUCHING) {
                mob.setPose(EntityPose.CROUCHING);
            }
            return;
        }
        long freedAt = holder.dndclasses$freedAt();
        if (freedAt <= 0L || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        long since = world.getTime() - freedAt;
        if (since >= WALK_OFF_TICKS || since < 0L) {
            world.spawnParticles(ParticleTypes.POOF, mob.getX(), mob.getBodyY(0.5D), mob.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.02D);
            DnDClasses.LOGGER.info("[Captive] {} has gone home", mob.getDisplayName().getString());
            mob.discard();
        } else if (since % 40L == 20L && mob.getNavigation().isIdle()) {
            walkOff(mob, null);
        }
    }

    /** Sets {@code mob} walking off, away from {@code from} if given. */
    private static void walkOff(MobEntity mob, @Nullable Vec3d from) {
        if (!(mob instanceof PathAwareEntity walker)) {
            return;
        }
        Vec3d target = from != null
                ? NoPenaltyTargeting.findFrom(walker, WALK_OFF_RANGE, 4, from)
                : NoPenaltyTargeting.find(walker, WALK_OFF_RANGE, 4);
        if (target == null) {
            return;
        }
        // Its home moves with it, so the folk's keep-near-home goals lead it away from the pen, not back in.
        BlockPos home = BlockPos.ofFloored(target);
        if (mob instanceof SettlementNpcEntity npc) {
            npc.setHome(home, WALK_OFF_RANGE);
        } else if (mob instanceof MountainDwarfEntity dwarf) {
            dwarf.setHome(home);
        } else {
            mob.setPositionTarget(home, WALK_OFF_RANGE);
        }
        walker.getNavigation().startMovingTo(target.x, target.y, target.z, 1.0D);
    }
}
