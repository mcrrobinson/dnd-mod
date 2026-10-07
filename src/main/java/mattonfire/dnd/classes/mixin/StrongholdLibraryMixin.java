package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every stronghold library is haunted by a Lich. It stands in the open row at the back of the
 * library, and its phylactery goes in the open row across the middle (placed when the Lich first
 * ticks, since that spot may be in a chunk that isn't generated yet).
 */
@Mixin(StrongholdGenerator.Library.class)
public abstract class StrongholdLibraryMixin extends StructurePiece {
    protected StrongholdLibraryMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dnd$placeLich(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci) {
        // Floor at y = 0; bookshelf rows run across z = 3, 5, ... 11, leaving z = 8 and 12-13 open.
        BlockPos pos = this.offsetPos(6, 1, 12);
        // generate runs once per chunk the piece overlaps: only spawn from the chunk that holds the spot.
        if (!chunkBox.contains(pos) || world.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        LichEntity lich = ModEntityTypes.LICH.create(world.toServerWorld());
        if (lich == null) {
            return;
        }
        // Facing the entrance (at z = 0)
        Direction facing = this.getFacing() == null ? Direction.NORTH : this.getFacing();
        lich.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.getOpposite().asRotation(), 0.0F);
        lich.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        lich.setPhylacterySpot(this.offsetPos(7, 1, 8));
        lich.setPersistent();
        world.spawnEntityAndPassengers(lich);
    }
}
