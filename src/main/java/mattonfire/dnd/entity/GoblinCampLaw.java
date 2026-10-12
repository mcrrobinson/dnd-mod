package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.camp.GoblinCampPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The goblin camps' grudge rule ({@link SettlementGrudges}): open (or smash) the camp chief's chest (the only
 * chest in a camp, in the tent facing the gate) and every goblin within {@link #RANGE} blocks comes for you, whatever
 * your standing with the goblins and whether or not they can see you. They hold the grudge while they fight
 * you and for {@link GoblinWarriorEntity#CAMP_GRUDGE_TICKS} after ({@link GoblinWarriorEntity#provokeByTheft}).
 */
final class GoblinCampLaw implements SettlementGrudges.Rules {
    /** The whole camp hears the chief's chest open: goblins this close to it. */
    static final double RANGE = 24.0D;
    private static final RegistryKey<Structure> CAMP = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "goblin_camp"));

    GoblinCampLaw() {
    }

    @Override
    public String id() {
        return "dndclasses:goblins";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        return (offence == SettlementGrudges.Offence.OPEN || offence == SettlementGrudges.Offence.BREAK)
                && blockEntity instanceof ChestBlockEntity
                && world instanceof ServerWorld serverWorld && inCamp(serverWorld, pos);
    }

    /** Whether {@code pos} is inside a goblin camp's piece. */
    static boolean inCamp(ServerWorld world, BlockPos pos) {
        Structure camp = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(CAMP);
        if (camp == null) {
            return false;
        }
        for (StructureStart start : world.getStructureAccessor().getStructureStarts(ChunkSectionPos.from(pos), camp)) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof GoblinCampPiece && piece.getBoundingBox().contains(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The fire of the goblin camp nearest {@code pos} (horizontally) within {@code range} blocks, looking only in
     * loaded chunks; null if there's none.
     */
    @Nullable
    static BlockPos campCenterNear(ServerWorld world, BlockPos pos, int range) {
        Structure camp = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(CAMP);
        if (camp == null) {
            return null;
        }
        BlockPos best = null;
        double bestSq = (double) range * range;
        int chunks = (range >> 4) + 1;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                if (!world.isChunkLoaded(cx + dx, cz + dz)) {
                    continue;
                }
                ChunkSectionPos section = ChunkSectionPos.from(cx + dx, 0, cz + dz);
                for (StructureStart start : world.getStructureAccessor().getStructureStarts(section, camp)) {
                    for (StructurePiece piece : start.getChildren()) {
                        if (piece instanceof GoblinCampPiece campPiece) {
                            BlockPos fire = campPiece.getCenter().up();
                            double ex = fire.getX() + 0.5D - pos.getX();
                            double ez = fire.getZ() + 0.5D - pos.getZ();
                            double sq = ex * ex + ez * ez;
                            if (sq <= bestSq) {
                                best = fire;
                                bestSq = sq;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    public double witnessRange(PlayerEntity player) {
        return RANGE;
    }

    @Override
    public List<GoblinWarriorEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(GoblinWarriorEntity.class, new Box(pos).expand(RANGE),
                GoblinWarriorEntity::isAlive);
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        ((GoblinWarriorEntity) guard).provokeByTheft(player);
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        // Goblins don't warn.
        this.provoke(guard, player);
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return SettlementGrudges.Verdict.GRUDGE;
    }
}
