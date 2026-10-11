package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Obstacles.VanillaTraps;
import net.minecraft.structure.DesertTempleGenerator;
import net.minecraft.structure.JungleTempleGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Registers the desert pyramid's pressure-plate trap and the jungle temple's tripwire traps as hidden traps
 * ({@link VanillaTraps}) as each chunk of the temple is generated.
 */
@Mixin({DesertTempleGenerator.class, JungleTempleGenerator.class})
public abstract class TempleTrapsMixin {
    @Inject(method = "generate", at = @At("RETURN"))
    private void dndclasses$registerTraps(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot,
            CallbackInfo ci) {
        Object self = this;
        VanillaTraps.recordGenerated(world, ((StructurePiece) self).getBoundingBox(), chunkBox,
                self instanceof DesertTempleGenerator ? VanillaTraps.Kind.PRESSURE_PLATE : VanillaTraps.Kind.TRIPWIRE);
    }
}
