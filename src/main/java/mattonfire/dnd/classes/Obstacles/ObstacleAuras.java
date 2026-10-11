package mattonfire.dnd.classes.Obstacles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Vector3f;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.types.HiddenDoorType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Lockpicking;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Per-player obstacle senses, ticked on the server:
 * <ul>
 * <li><b>Ranger passive Perception</b> (every {@value #PASSIVE_INTERVAL} ticks): each Hidden Door within
 * {@link HiddenDoorType#PASSIVE_RANGE} blocks of a Ranger, still hidden from them, is found if their passive
 * score (10 + the better of Perception and Investigation) meets its DC.</li>
 * <li><b>Outlines</b> (every {@value #OUTLINE_INTERVAL} ticks): a found, still-sealed Hidden Door is outlined in
 * gold dust for every player who knows about it, within {@value #OUTLINE_RANGE} blocks. Only they see it.</li>
 * <li><b>Lock glint</b> (every {@value #OUTLINE_INTERVAL} ticks): Rogues and Rangers see a glint of gold on locked
 * chests within {@value #GLINT_RANGE} blocks.</li>
 * </ul>
 */
public final class ObstacleAuras {
    public static final int PASSIVE_INTERVAL = 20;
    public static final int OUTLINE_INTERVAL = 10;
    public static final int OUTLINE_RANGE = 16;
    public static final int GLINT_RANGE = 6;

    private static final DustParticleEffect OUTLINE = new DustParticleEffect(new Vector3f(1.0f, 0.82f, 0.25f), 0.8f);
    private static final DustParticleEffect GLINT = new DustParticleEffect(new Vector3f(1.0f, 0.9f, 0.35f), 0.6f);

    private ObstacleAuras() {
    }

    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ObstacleAuras::tick);
    }

    private static void tick(ServerWorld world) {
        long time = world.getTime();
        boolean passive = time % PASSIVE_INTERVAL == 0;
        boolean outline = time % OUTLINE_INTERVAL == 0;
        if (!passive && !outline) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator())) {
            DndCharacter dndClass = D20.classOf(player);
            if (passive && dndClass == DndCharacter.RANGER && !player.isCreative()) {
                rangerSenses(world, player);
            }
            if (outline) {
                outlineKnownDoors(world, player);
            }
            if (outline && (dndClass == DndCharacter.ROGUE || dndClass == DndCharacter.RANGER)) {
                lockGlint(world, player);
            }
        }
    }

    /** A Ranger's passive Perception against the hidden doors round them. */
    private static void rangerSenses(ServerWorld world, ServerPlayerEntity ranger) {
        int range = (int) Math.ceil(HiddenDoorType.PASSIVE_RANGE);
        double rangeSq = HiddenDoorType.PASSIVE_RANGE * HiddenDoorType.PASSIVE_RANGE;
        for (BlockPos pos : ObstacleIndex.get(world).within(ranger.getBlockPos(), range)) {
            if (ranger.squaredDistanceTo(pos.toCenterPos()) > rangeSq) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!ObstacleBlock.isSealed(state) || !(((ObstacleBlock) state.getBlock()).type() instanceof HiddenDoorType door)
                    || !door.hiddenFrom(world, pos, ranger)) {
                continue;
            }
            Tier tier = world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;
            if (PerceptionService.noticesPassively(ranger, Skill.INVESTIGATION, tier.dc)) {
                door.reveal(world, pos, ranger, HiddenDoorType.How.PASSIVE);
            }
        }
    }

    /** Gold outlines round the found doors this player knows about. */
    private static void outlineKnownDoors(ServerWorld world, ServerPlayerEntity player) {
        Set<BlockPos> done = new HashSet<>();
        for (BlockPos pos : ObstacleIndex.get(world).within(player.getBlockPos(), OUTLINE_RANGE)) {
            if (done.contains(pos)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!ObstacleBlock.isSealed(state) || !(((ObstacleBlock) state.getBlock()).type() instanceof HiddenDoorType)
                    || !HiddenDoorType.knows(world, pos, player.getUuid())) {
                continue;
            }
            List<BlockPos> group = ObstacleGroups.group(world, pos);
            done.addAll(group);
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos part : group) {
                minX = Math.min(minX, part.getX());
                minY = Math.min(minY, part.getY());
                minZ = Math.min(minZ, part.getZ());
                maxX = Math.max(maxX, part.getX() + 1);
                maxY = Math.max(maxY, part.getY() + 1);
                maxZ = Math.max(maxZ, part.getZ() + 1);
            }
            box(world, player, minX - 0.02, minY - 0.02, minZ - 0.02, maxX + 0.02, maxY + 0.02, maxZ + 0.02);
        }
    }

    /** The 12 edges of a box in dust, one point every quarter block, sent to one player. */
    private static void box(ServerWorld world, ServerPlayerEntity player, double x0, double y0, double z0, double x1,
            double y1, double z1) {
        double[] xs = {x0, x1};
        double[] ys = {y0, y1};
        double[] zs = {z0, z1};
        for (double y : ys) {
            for (double z : zs) {
                line(world, player, x0, y, z, x1, y, z);
            }
            for (double x : xs) {
                line(world, player, x, y, z0, x, y, z1);
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                line(world, player, x, y0, z, x, y1, z);
            }
        }
    }

    private static void line(ServerWorld world, ServerPlayerEntity player, double x0, double y0, double z0, double x1,
            double y1, double z1) {
        double length = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
        int steps = Math.max(1, (int) Math.round(length * 4));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            world.spawnParticles(player, OUTLINE, true, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** A glint of gold on each locked chest near a Rogue or Ranger. */
    private static void lockGlint(ServerWorld world, ServerPlayerEntity player) {
        BlockPos at = player.getBlockPos();
        int minX = ChunkSectionPos.getSectionCoord(at.getX() - GLINT_RANGE);
        int maxX = ChunkSectionPos.getSectionCoord(at.getX() + GLINT_RANGE);
        int minZ = ChunkSectionPos.getSectionCoord(at.getZ() - GLINT_RANGE);
        int maxZ = ChunkSectionPos.getSectionCoord(at.getZ() + GLINT_RANGE);
        double rangeSq = (double) GLINT_RANGE * GLINT_RANGE;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    if (be instanceof ChestBlockEntity && pos.getSquaredDistance(at) <= rangeSq
                            && Lockpicking.isLocked(world, pos)) {
                        world.spawnParticles(player, GLINT, true, pos.getX() + 0.5, pos.getY() + 0.75,
                                pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
                    }
                }
            }
        }
    }
}
