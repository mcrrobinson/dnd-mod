package mattonfire.dnd.entity.kobold;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import mattonfire.dnd.classes.Abilities.Advantage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Kobold courage, Pack Tactics and Sunlight Sensitivity (design 10, 3.1-3.2).
 * <p>
 * A kobold is <b>brave</b> when any of these holds:
 * <ol>
 * <li>a Kobold Dragonshield within {@value #RALLY_RANGE} blocks is alive (or it is one);</li>
 * <li>at least {@value #PACK} other kobolds are within {@value #PACK_RANGE} blocks, or {@value #DREAD_PACK} against a
 * dragon-slayer ({@link KoboldTribute#isDreaded});</li>
 * <li>it's inside its home: a {@link #registerHome} rule (K2's warrens, D2's lair).</li>
 * </ol>
 * Otherwise it's craven: it won't start a melee fight, keeps its distance, and runs from a player who hits it.
 */
public final class KoboldCourage {
    /** A Dragonshield rallies kobolds this close (and gives them Speed I). */
    public static final double RALLY_RANGE = 12.0D;
    /** Other kobolds this close count for the pack rule. */
    public static final double PACK_RANGE = 8.0D;
    /** Others needed for a pack of 3. */
    public static final int PACK = 2;
    /** Others needed against a dragon-slayer. */
    public static final int DREAD_PACK = 4;
    /** Pack Tactics: another kobold this close to the target. */
    public static final double PACK_TACTICS_RANGE = 2.0D;
    /** Sunlight Sensitivity: sky light at the eyes at least this, by day in the Overworld. */
    public static final int SUNLIGHT_LEVEL = 12;

    private static final List<Predicate<KoboldEntity>> HOMES = new CopyOnWriteArrayList<>();
    private static final List<Function<KoboldEntity, Vec3d>> REFUGES = new CopyOnWriteArrayList<>();

    private KoboldCourage() {
    }

    /**
     * Adds a "this kobold is at home" rule: kobolds are always brave at home. K2 registers "inside a warren piece",
     * D2 "within 48 blocks of its lair".
     */
    public static void registerHome(Predicate<KoboldEntity> home) {
        HOMES.add(home);
    }

    /**
     * Adds a bolt-hole rule: where a badly hurt kobold runs to (null if the rule has none for it). K2's warrens
     * register their 1 x 1 bolt-holes, which kobolds fit through and players don't.
     */
    public static void registerRefuge(Function<KoboldEntity, Vec3d> refuge) {
        REFUGES.add(refuge);
    }

    /** The first refuge a rule gives {@code kobold}, or null. */
    @Nullable
    public static Vec3d refugeOf(KoboldEntity kobold) {
        for (Function<KoboldEntity, Vec3d> refuge : REFUGES) {
            Vec3d spot = refuge.apply(kobold);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    public static boolean atHome(KoboldEntity kobold) {
        for (Predicate<KoboldEntity> home : HOMES) {
            if (home.test(kobold)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code kobold} is brave, against {@code against} (a dragon-slayer needs a bigger pack) or anyone. */
    public static boolean isBrave(KoboldEntity kobold, @Nullable PlayerEntity against) {
        if (kobold instanceof KoboldDragonshieldEntity || leaderNear(kobold)) {
            return true;
        }
        int needed = against != null && KoboldTribute.isDreaded(against) ? DREAD_PACK : PACK;
        int others = kobold.world.getEntitiesByClass(KoboldEntity.class, kobold.getBoundingBox().expand(PACK_RANGE),
                other -> other != kobold && other.isAlive() && other.squaredDistanceTo(kobold) <= PACK_RANGE * PACK_RANGE)
                .size();
        return others >= needed || atHome(kobold);
    }

    /** A living Dragonshield within {@value #RALLY_RANGE} blocks. */
    public static boolean leaderNear(LivingEntity entity) {
        return !entity.world.getEntitiesByClass(KoboldDragonshieldEntity.class,
                entity.getBoundingBox().expand(RALLY_RANGE),
                leader -> leader != entity && leader.isAlive() && leader.squaredDistanceTo(entity) <= RALLY_RANGE * RALLY_RANGE)
                .isEmpty();
    }

    // ---- The attack modes (ConditionEffects.registerMobAttackMode) ----

    /** Pack Tactics: advantage when another living kobold is within {@value #PACK_TACTICS_RANGE} blocks of the target. */
    public static Advantage packTactics(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof KoboldEntity)) {
            return Advantage.NORMAL;
        }
        boolean ally = !target.world.getEntitiesByClass(KoboldEntity.class,
                target.getBoundingBox().expand(PACK_TACTICS_RANGE),
                other -> other != attacker && other != target && other.isAlive()).isEmpty();
        return ally ? Advantage.ADVANTAGE : Advantage.NORMAL;
    }

    /** Sunlight Sensitivity: disadvantage in bright Overworld daylight. */
    public static Advantage sunlightSensitivity(LivingEntity attacker, LivingEntity target) {
        return attacker instanceof KoboldEntity && isSunlit(attacker) ? Advantage.DISADVANTAGE : Advantage.NORMAL;
    }

    /** By day in the Overworld with sky light {@value #SUNLIGHT_LEVEL}+ at the eyes. */
    public static boolean isSunlit(LivingEntity entity) {
        World world = entity.world;
        if (world.getRegistryKey() != World.OVERWORLD || !world.isDay()) {
            return false;
        }
        BlockPos eyes = BlockPos.ofFloored(entity.getX(), entity.getEyeY(), entity.getZ());
        return world.getLightLevel(LightType.SKY, eyes) >= SUNLIGHT_LEVEL;
    }
}
