package mattonfire.dnd.world.gen.fortress;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A dwarven fortress: a great gate on a mountainside opening onto a pillared hall carved into the
 * rock, with a throne room and treasury at its end and forges, barracks, mead halls and mines off
 * its sides. Laid out afresh for every fortress by {@link FortressPlanner}.
 */
public class DwarvenFortressStructure extends Structure {
    public static final Codec<DwarvenFortressStructure> CODEC = createCodec(DwarvenFortressStructure::new);

    public DwarvenFortressStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        FortressPlanner.Terrain terrain = new FortressPlanner.Terrain(context);
        return terrain.findSite(chunk.getCenterX(), chunk.getCenterZ()).map(site -> new StructurePosition(site.pos(),
                collector -> FortressPlanner.plan(collector, context.random(), site)));
    }

    @Override
    public StructureType<?> getType() {
        return DwarvenFortressStructures.DWARVEN_FORTRESS;
    }
}
