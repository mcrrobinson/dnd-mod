package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.classes.Obstacles.Tier;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.loot.LootTables;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * {@value #CHANCE_PERCENT}% of vanilla stronghold corridors (the 5x5x7 ones with side exits) hide a Hidden Door
 * (Hard) in a side wall without an exit. Behind it is a 5x5 closet with a stronghold corridor chest. A Ranger
 * spots the door passively; anyone else has to search the wall.
 *
 * <p>The closet sits outside the corridor's box, so it's only built where it can't clash: not inside any other
 * piece of the same stronghold, and only in chunks the corridor itself is generated in (a piece is built once per
 * chunk its box touches, so a closet poking into another chunk would come out half-built). The roll comes from
 * the world seed and the corridor's position, so every chunk agrees.
 */
@Mixin(StrongholdGenerator.Corridor.class)
public abstract class StrongholdHiddenClosetMixin extends StructurePiece {
    private static final int CHANCE_PERCENT = 15;
    /** The door's spot along the corridor (it's 7 long; side exits are at z = 2..4, the torches at 1 and 5). */
    private static final int DOOR_Z = 3;

    @Shadow
    @Final
    private boolean leftExitExists;
    @Shadow
    @Final
    private boolean rightExitExists;

    protected StrongholdHiddenClosetMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dndclasses$hiddenCloset(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot,
            CallbackInfo ci) {
        BlockBox box = this.getBoundingBox();
        Random roll = Random.create(world.getSeed() ^ StructureObstacles.seedAt(new BlockPos(box.getMinX(),
                box.getMinY(), box.getMinZ())) ^ 0x4D1DDE4L);
        if (roll.nextInt(100) >= CHANCE_PERCENT) {
            return;
        }
        boolean left = roll.nextBoolean();
        if (left ? this.leftExitExists : this.rightExitExists) {
            left = !left;
        }
        if (left ? this.leftExitExists : this.rightExitExists) {
            return;
        }
        // Local x of the wall the door is in, and the step outwards from it
        int wall = left ? 0 : 4;
        int out = left ? -1 : 1;
        BlockBox closet = BlockBox.create(this.offsetPos(wall + out, 0, DOOR_Z - 2),
                this.offsetPos(wall + 4 * out, 4, DOOR_Z + 2));
        if (!this.fitsCloset(closet)) {
            return;
        }
        BlockState bricks = Blocks.STONE_BRICKS.getDefaultState();
        for (int i = 1; i <= 4; i++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = DOOR_Z - 2; z <= DOOR_Z + 2; z++) {
                    boolean inside = i <= 3 && y >= 1 && y <= 3 && Math.abs(z - DOOR_Z) <= 1;
                    this.addBlock(world, inside ? Blocks.AIR.getDefaultState()
                            : y == 0 || i == 4 ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : bricks,
                            wall + i * out, y, z, chunkBox);
                }
            }
        }
        this.addBlock(world, Blocks.COBWEB.getDefaultState(), wall + out, 3, DOOR_Z + 1, chunkBox);
        this.addChest(world, chunkBox, roll, wall + 3 * out, 1, DOOR_Z, LootTables.STRONGHOLD_CORRIDOR_CHEST);
        BlockPos door = this.offsetPos(wall, 1, DOOR_Z).toImmutable();
        long seed = StructureObstacles.seedAt(door);
        ObstaclePlacer.place(world, chunkBox, door, ObstacleTypes.HIDDEN_DOOR, Tier.HARD, false, seed);
        ObstaclePlacer.place(world, chunkBox, door.up(), ObstacleTypes.HIDDEN_DOOR, Tier.HARD, false, seed);
        if (chunkBox.contains(door)) {
            DnDClasses.LOGGER.debug("[Obstacle] stronghold hidden closet at {}", door.toShortString());
        }
    }

    /** Whether the closet lies in chunks this corridor generates in, clear of every other piece of its stronghold. */
    private boolean fitsCloset(BlockBox closet) {
        BlockBox box = this.getBoundingBox();
        if (ChunkSectionPos.getSectionCoord(closet.getMinX()) < ChunkSectionPos.getSectionCoord(box.getMinX())
                || ChunkSectionPos.getSectionCoord(closet.getMaxX()) > ChunkSectionPos.getSectionCoord(box.getMaxX())
                || ChunkSectionPos.getSectionCoord(closet.getMinZ()) < ChunkSectionPos.getSectionCoord(box.getMinZ())
                || ChunkSectionPos.getSectionCoord(closet.getMaxZ()) > ChunkSectionPos.getSectionCoord(box.getMaxZ())) {
            return false;
        }
        Object self = this;
        StructureStart start = StructureObstacles.placing();
        if (start == null || !start.getChildren().contains(self)) {
            return false;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece != self && piece.getBoundingBox().intersects(closet)) {
                return false;
            }
        }
        return true;
    }
}
