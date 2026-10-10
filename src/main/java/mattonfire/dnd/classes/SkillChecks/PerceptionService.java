package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.Skill;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Passive Perception and the things it can notice ({@link Perceivable}). The one place that decides whether a
 * player notices something passively: traps ({@link TrapSense}), the mimic tell and anything registered with
 * {@link #hide} all ask {@link #noticesPassively}.
 *
 * <pre>{@code
 * // A hidden door that opens once someone finds it
 * PerceptionService.Hidden door = PerceptionService.hide(world, pos, 15, Skill.INVESTIGATION,
 *         (player, searched) -> openDoor(world, pos));
 * // ...and when the door is gone:
 * door.remove();
 * }</pre>
 *
 * Registrations live in memory only: re-register on load (e.g. from a block entity's first tick).
 */
public final class PerceptionService {
    /** How close you must be for passive Perception to notice something registered with {@link #hide}. */
    public static final double PASSIVE_RANGE = 6.0;
    /** How far a Search (V) reaches. */
    public static final double SEARCH_RANGE = 8.0;
    /** Ticks between passive checks of the {@link #hide} registrations. */
    public static final int PASSIVE_INTERVAL = 20;

    /** Called when a player notices a hidden thing. */
    @FunctionalInterface
    public interface NoticeListener {
        void noticed(ServerPlayerEntity player, boolean searched);
    }

    private static final Map<RegistryKey<World>, List<Hidden>> HIDDEN = new HashMap<>();

    private PerceptionService() {
    }

    // ---------------------------------------------------------------- passive scores

    /**
     * The passive score used against a {@link Perceivable} of this skill: passive Perception, or for
     * INVESTIGATION the better of passive Perception and passive Investigation.
     */
    public static int passive(ServerPlayerEntity player, Skill skill) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        int perception = sheet.passive(Skill.PERCEPTION);
        return skill == Skill.INVESTIGATION ? Math.max(perception, sheet.passive(Skill.INVESTIGATION))
                : skill == Skill.PERCEPTION ? perception : sheet.passive(skill);
    }

    /** Whether the player's passive score for the skill meets the DC. */
    public static boolean noticesPassively(ServerPlayerEntity player, Skill skill, int dc) {
        return passive(player, skill) >= dc;
    }

    /** The check bonus a Search roll uses against something of this skill (see {@link #passive}). */
    public static int searchBonus(ServerPlayerEntity player, Skill skill) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        int perception = sheet.check(Skill.PERCEPTION);
        return skill == Skill.INVESTIGATION ? Math.max(perception, sheet.check(Skill.INVESTIGATION))
                : skill == Skill.PERCEPTION ? perception : sheet.check(skill);
    }

    /**
     * Passive check of one perceivable against the players in range: each one it is hidden from and whose
     * passive score meets its DC notices it. Call it from the perceivable's own tick (every 20 ticks is plenty).
     *
     * @return how many players noticed it
     */
    public static int passiveCheck(ServerWorld world, Perceivable thing, double range) {
        Vec3d at = thing.perceptionPos();
        int noticed = 0;
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.squaredDistanceTo(at) <= range * range)) {
            if (thing.hiddenFrom(player) && noticesPassively(player, thing.perceptionSkill(), thing.perceptionDc())) {
                DnDClasses.LOGGER.info("[Perception] {} notices {} (passive {} vs DC {})", player.getEntityName(),
                        describe(thing), passive(player, thing.perceptionSkill()), thing.perceptionDc());
                thing.perceive(player, false);
                noticed++;
            }
        }
        return noticed;
    }

    // ---------------------------------------------------------------- registry

    /** Hides a block position: noticed passively within {@link #PASSIVE_RANGE}, or by a Search. */
    public static Hidden hide(ServerWorld world, BlockPos pos, int dc, Skill skill, NoticeListener onNoticed) {
        return add(world, new Hidden(world.getRegistryKey(), pos.toImmutable(), null, dc, skill, onNoticed));
    }

    /** Hides an entity (dropped automatically once it's removed). */
    public static Hidden hide(ServerWorld world, Entity entity, int dc, Skill skill, NoticeListener onNoticed) {
        return add(world, new Hidden(world.getRegistryKey(), null, entity, dc, skill, onNoticed));
    }

    private static synchronized Hidden add(ServerWorld world, Hidden hidden) {
        HIDDEN.computeIfAbsent(world.getRegistryKey(), k -> new ArrayList<>()).add(hidden);
        return hidden;
    }

    private static synchronized List<Hidden> registered(ServerWorld world) {
        List<Hidden> list = HIDDEN.get(world.getRegistryKey());
        if (list == null) {
            return List.of();
        }
        list.removeIf(h -> h.removed || h.entity != null && h.entity.isRemoved());
        return new ArrayList<>(list);
    }

    /**
     * Everything within range of a point that can be noticed: {@link #hide} registrations, entities and loaded
     * block entities that implement {@link Perceivable}.
     */
    public static List<Perceivable> near(ServerWorld world, Vec3d centre, double range) {
        List<Perceivable> found = new ArrayList<>();
        double rangeSq = range * range;
        for (Hidden h : registered(world)) {
            if (h.perceptionPos().squaredDistanceTo(centre) <= rangeSq) {
                found.add(h);
            }
        }
        Box box = new Box(centre, centre).expand(range);
        for (Entity e : world.getOtherEntities(null, box, e -> e instanceof Perceivable)) {
            if (((Perceivable) e).perceptionPos().squaredDistanceTo(centre) <= rangeSq) {
                found.add((Perceivable) e);
            }
        }
        int minX = ChunkSectionPos.getSectionCoord(box.minX);
        int maxX = ChunkSectionPos.getSectionCoord(box.maxX);
        int minZ = ChunkSectionPos.getSectionCoord(box.minZ);
        int maxZ = ChunkSectionPos.getSectionCoord(box.maxZ);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof Perceivable p && p.perceptionPos().squaredDistanceTo(centre) <= rangeSq) {
                        found.add(p);
                    }
                }
            }
        }
        return found;
    }

    static String describe(Perceivable thing) {
        if (thing instanceof Entity e) {
            return e.getType().getUntranslatedName() + " " + e.getBlockPos().toShortString();
        }
        if (thing instanceof BlockEntity be) {
            return be.getType().toString().replaceAll(".*\\.", "") + " " + be.getPos().toShortString();
        }
        return thing.toString();
    }

    /** Registers the passive tick for {@link #hide} registrations. Called once at startup. */
    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % PASSIVE_INTERVAL != 0) {
                return;
            }
            for (Hidden h : registered(world)) {
                passiveCheck(world, h, PASSIVE_RANGE);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PerceptionService.class) {
                HIDDEN.clear();
            }
        });
    }

    /** A {@link #hide} registration. Each player notices it once. */
    public static final class Hidden implements Perceivable {
        private final RegistryKey<World> world;
        @Nullable
        private final BlockPos pos;
        @Nullable
        private final Entity entity;
        private final int dc;
        private final Skill skill;
        private final NoticeListener onNoticed;
        private final Set<UUID> noticedBy = new HashSet<>();
        private volatile boolean removed;

        private Hidden(RegistryKey<World> world, @Nullable BlockPos pos, @Nullable Entity entity, int dc, Skill skill,
                NoticeListener onNoticed) {
            this.world = world;
            this.pos = pos;
            this.entity = entity;
            this.dc = dc;
            this.skill = skill;
            this.onNoticed = onNoticed;
        }

        /** Stops it being noticed (the door opened, the trap was dismantled). */
        public void remove() {
            removed = true;
        }

        public boolean noticedBy(ServerPlayerEntity player) {
            return noticedBy.contains(player.getUuid());
        }

        @Nullable
        public BlockPos pos() {
            return pos;
        }

        @Nullable
        public Entity entity() {
            return entity;
        }

        @Override
        public int perceptionDc() {
            return dc;
        }

        @Override
        public Skill perceptionSkill() {
            return skill;
        }

        @Override
        public Vec3d perceptionPos() {
            return entity != null ? entity.getPos() : Vec3d.ofCenter(pos);
        }

        @Override
        public boolean hiddenFrom(ServerPlayerEntity player) {
            return !removed && player.getWorld().getRegistryKey() == world && !noticedBy.contains(player.getUuid());
        }

        @Override
        public void perceive(ServerPlayerEntity player, boolean searched) {
            if (noticedBy.add(player.getUuid())) {
                onNoticed.noticed(player, searched);
            }
        }

        @Override
        public String toString() {
            return "hidden " + skill.name().toLowerCase() + " DC " + dc + " at "
                    + (entity != null ? entity.getBlockPos() : pos).toShortString();
        }
    }
}
