package mattonfire.dnd.world.underdark.duergar;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;
import net.minecraft.world.gen.structure.Structure;

/**
 * {@code dndclasses:away_from_duergar_holds}: drops a feature's position if its chunk or a chunk next to it belongs to
 * a Duergar Hold. The Grey Reaches' large dripstone and dripstone clusters use it: a column from a neighbouring chunk,
 * decorated after the hold was built, used to drop into the lava basin or the yard.
 */
public final class AwayFromHoldsPlacement extends PlacementModifier {
    public static final AwayFromHoldsPlacement INSTANCE = new AwayFromHoldsPlacement();
    public static final Codec<AwayFromHoldsPlacement> CODEC = Codec.unit(() -> INSTANCE);
    public static final PlacementModifierType<AwayFromHoldsPlacement> TYPE = Registry.register(
            Registries.PLACEMENT_MODIFIER_TYPE, DuergarHolds.id("away_from_duergar_holds"), () -> CODEC);

    private AwayFromHoldsPlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
        StructureWorldAccess world = context.getWorld();
        Structure hold = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(DuergarHolds.KEY);
        if (hold == null) {
            return Stream.of(pos);
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (world.isChunkLoaded(cx + dx, cz + dz)
                        && !world.getChunk(cx + dx, cz + dz).getStructureReferences(hold).isEmpty()) {
                    return Stream.empty();
                }
            }
        }
        return Stream.of(pos);
    }

    @Override
    public PlacementModifierType<?> getType() {
        return TYPE;
    }
}
