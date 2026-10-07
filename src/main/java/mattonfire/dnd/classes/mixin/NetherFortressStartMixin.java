package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.SpawnReason;
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
        GoblinWarlordEntity warlord = ModEntityTypes.GOBLIN_WARLORD.create(world.toServerWorld());
        if (warlord == null) {
            return;
        }
        Direction facing = this.getFacing() == null ? Direction.NORTH : this.getFacing();
        warlord.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.asRotation(), 0.0F);
        warlord.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        warlord.setGuardPos(pos);
        warlord.setPersistent();
        world.spawnEntityAndPassengers(warlord);
    }
}
