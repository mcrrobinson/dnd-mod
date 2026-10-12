package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Obstacles.types.CraftElementObstacles;
import mattonfire.dnd.entity.boss.StructureBosses;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.util.Identifier;
import net.minecraft.structure.NetherFortressGenerator;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every Nether Fortress gets one Goblin Warlord, standing in the middle of the bridge crossing the
 * fortress grows from (its start piece), the way ocean monuments place their elder guardians.
 * <p>
 * In one corner of that crossing hangs a small nether-brick vault of blaze rods behind a Fire Curtain
 * (Hard): a Warlock, or anyone under Fire Resistance, walks through and douses the brazier inside.
 */
@Mixin(NetherFortressGenerator.BridgeCrossing.class)
public abstract class NetherFortressStartMixin extends StructurePiece {
    protected NetherFortressStartMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dnd$placeWarlord(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                                  Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci) {
        if (!((Object) this instanceof NetherFortressGenerator.Start)) {
            return;
        }
        // The crossing's floor is at y = 4; the centre column is (9, 9).
        BlockPos pos = this.offsetPos(9, 5, 9);
        // generate runs once per chunk the piece overlaps: only spawn from the chunk that holds the spot.
        if (!chunkBox.contains(pos)) {
            return;
        }
        // On Peaceful this leaves a marker that raises the Warlord once the difficulty goes up.
        Direction facing = this.getFacing() == null ? Direction.NORTH : this.getFacing();
        StructureBosses.placeWarlord(world, pos, facing.asRotation());
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dnd$placeCache(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                                Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci) {
        if (!((Object) this instanceof NetherFortressGenerator.Start)) {
            return;
        }
        // The crossing is a plus of two 5-wide bridges (x and z 7..11, floor at y = 3..4, fences at y = 5), so its
        // corners are empty. The vault fills the corner x, z = 1..6 and opens onto the x = 7 bridge.
        BlockState bricks = Blocks.NETHER_BRICKS.getDefaultState();
        for (int x = 1; x <= 6; x++) {
            for (int z = 1; z <= 6; z++) {
                for (int y = 3; y <= 8; y++) {
                    boolean inside = x >= 2 && x <= 5 && z >= 2 && z <= 5 && y >= 5 && y <= 7;
                    this.addBlock(world, inside ? Blocks.AIR.getDefaultState()
                            : y == 8 && (x + z) % 2 == 0 ? Blocks.RED_NETHER_BRICKS.getDefaultState() : bricks, x, y, z, chunkBox);
                }
            }
        }
        for (int[] corner : new int[][]{{1, 1}, {1, 6}, {6, 1}}) {
            this.fillDownwards(world, bricks, corner[0], 2, corner[1], chunkBox);
        }
        // The way in, through the bridge's railing.
        this.addBlock(world, Blocks.AIR.getDefaultState(), 7, 5, 3, chunkBox);
        this.addBlock(world, Blocks.AIR.getDefaultState(), 7, 5, 4, chunkBox);
        this.addBlock(world, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true), 3, 7, 3, chunkBox);
        Random loot = Random.create(StructureObstacles.seedAt(this.offsetPos(0, 0, 0)));
        this.addChest(world, chunkBox, loot, 2, 5, 2, CACHE);
        // A 2x2x2 curtain across the doorway (x = 5..6, z = 3..4, y = 5..6) and its brazier inside.
        BlockPos curtain = this.offsetPos(5, 5, 3).toImmutable();
        long seed = StructureObstacles.seedAt(curtain);
        for (int x = 5; x <= 6; x++) {
            for (int z = 3; z <= 4; z++) {
                for (int y = 5; y <= 6; y++) {
                    ObstaclePlacer.place(world, chunkBox, this.offsetPos(x, y, z).toImmutable(),
                            CraftElementObstacles.FIRE_CURTAIN, Tier.HARD, false, seed);
                }
            }
        }
        ObstaclePlacer.winch(world, chunkBox, this.offsetPos(2, 5, 4).toImmutable(), CraftElementObstacles.FIRE_CURTAIN_BRAZIER,
                curtain, Tier.HARD, StructureObstacles.seedAt(curtain.down()));
    }

    private static final Identifier CACHE = new Identifier(DnDClasses.MOD_ID, "chests/nether_fortress_cache");
}
