package mattonfire.dnd.world.gen.village;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A hobbit village: a green in the middle with hobbit holes, gardens, an orchard, market stalls,
 * a pond and maybe an inn scattered round it, joined by winding lanes. Laid out afresh for every
 * village by {@link HobbitVillagePlanner}.
 */
public class HobbitVillageStructure extends Structure {
    public static final Codec<HobbitVillageStructure> CODEC = createCodec(HobbitVillageStructure::new);

    public HobbitVillageStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        HobbitVillagePlanner.Terrain terrain = new HobbitVillagePlanner.Terrain(context);
        if (!terrain.isGoodSite(x, z)) {
            return Optional.empty();
        }
        int y = terrain.ground(x, z);
        return Optional.of(new StructurePosition(new BlockPos(x, y, z),
                collector -> HobbitVillagePlanner.plan(collector, terrain, context.random(), x, y, z)));
    }

    @Override
    public StructureType<?> getType() {
        return HobbitVillageStructures.HOBBIT_VILLAGE;
    }
}
