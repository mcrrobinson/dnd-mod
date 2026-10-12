package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.classes.Obstacles.Tier;
import net.minecraft.structure.MineshaftGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * {@value #CHANCE_PERCENT}% of vanilla mineshaft corridors (two segments or longer) have a Collapsed Passage (Easy):
 * a 3x3x2 plug of rubble across the corridor, between two of its support frames, where no chest cart or spawner
 * goes. The roll and the spot come from the world seed and the corridor's position, so every chunk of the
 * corridor agrees.
 */
@Mixin(MineshaftGenerator.MineshaftCorridor.class)
public abstract class MineshaftCollapseMixin extends StructurePiece {
    private static final int CHANCE_PERCENT = 10;
    /** Vanilla corridors come in 5-block segments, with a support frame at z = 2 of each. */
    private static final int SEGMENT = 5;

    protected MineshaftCollapseMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dndclasses$collapse(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot,
            CallbackInfo ci) {
        Direction facing = this.getFacing();
        if (facing == null) {
            return;
        }
        BlockBox box = this.getBoundingBox();
        int segments = (facing.getAxis() == Direction.Axis.Z ? box.getBlockCountZ() : box.getBlockCountX()) / SEGMENT;
        if (segments < 2) {
            return;
        }
        Random roll = Random.create(world.getSeed() ^ StructureObstacles.seedAt(new BlockPos(box.getMinX(),
                box.getMinY(), box.getMinZ())));
        if (roll.nextInt(100) >= CHANCE_PERCENT) {
            return;
        }
        // z = 4 and 5 of a segment: after its frame (z = 2) and chest cart (z = 3), before the next segment's
        int z = roll.nextInt(segments - 1) * SEGMENT + 4;
        BlockPos first = this.offsetPos(0, 0, z).toImmutable();
        long seed = StructureObstacles.seedAt(first);
        for (int dz = 0; dz <= 1; dz++) {
            for (int x = 0; x <= 2; x++) {
                for (int y = 0; y <= 2; y++) {
                    ObstaclePlacer.place(world, chunkBox, this.offsetPos(x, y, z + dz).toImmutable(),
                            ObstacleTypes.COLLAPSED_PASSAGE, Tier.EASY, false, seed);
                }
            }
        }
    }
}
