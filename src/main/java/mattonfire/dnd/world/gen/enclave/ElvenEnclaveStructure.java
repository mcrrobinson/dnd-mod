package mattonfire.dnd.world.gen.enclave;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * An elven enclave: treetop halls round a giant Heart Tree in the birch and flower forests, joined by
 * rope bridges, with a Moonwell, an archery glade and flower gardens on the forest floor. Laid out afresh
 * for every enclave by {@link ElvenEnclavePlanner}.
 */
public class ElvenEnclaveStructure extends Structure {
    public static final Codec<ElvenEnclaveStructure> CODEC = createCodec(ElvenEnclaveStructure::new);

    private static final Identifier GOBLIN_CAMPS = new Identifier(DnDClasses.MOD_ID, "goblin_camps");
    /** Chunks between an enclave and a goblin camp's start (camps check the same the other way round). */
    public static final int CAMP_CLEARANCE = 8;

    public ElvenEnclaveStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        if (StructureProximity.near(context, GOBLIN_CAMPS, CAMP_CLEARANCE)) {
            return Optional.empty();
        }
        EnclaveTerrain terrain = new EnclaveTerrain(context);
        if (!terrain.isGoodSite(x, z)) {
            return Optional.empty();
        }
        int y = terrain.ground(x, z);
        return Optional.of(new StructurePosition(new BlockPos(x, y, z),
                collector -> ElvenEnclavePlanner.plan(collector, terrain, context.random(), x, y, z)));
    }

    @Override
    public StructureType<?> getType() {
        return ElvenEnclaveStructures.ELVEN_ENCLAVE;
    }
}
