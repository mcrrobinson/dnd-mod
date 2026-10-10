package mattonfire.dnd.world.gen.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A dungeon: 9-13 rooms on a planned route below the surface, reached by a stair from an entrance
 * on the surface. The layout comes from {@link DungeonPlanner}; the theme (a field of the structure
 * JSON) picks the blocks.
 */
public class DungeonStructure extends Structure {
    public static final Codec<DungeonStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            configCodecBuilder(instance),
            DungeonTheme.CODEC.fieldOf("theme").forGetter(DungeonStructure::theme)
    ).apply(instance, DungeonStructure::new));

    /**
     * Keeps this many chunks clear of these structures. Like {@code DragonLairStructure}, this only
     * counts ones that really generate there, unlike a structure set's exclusion_zone.
     */
    private static final int CLEARANCE = 8;
    private static final List<RegistryKey<StructureSet>> KEEP_CLEAR_OF = List.of(
            set("hobbit_villages"), set("dwarven_fortresses"), set("beholder_lairs"));

    private final DungeonTheme theme;

    public DungeonStructure(Config config, DungeonTheme theme) {
        super(config);
        this.theme = theme;
    }

    public DungeonTheme theme() {
        return this.theme;
    }

    private static RegistryKey<StructureSet> set(String name) {
        return RegistryKey.of(RegistryKeys.STRUCTURE_SET, new Identifier(DnDClasses.MOD_ID, name));
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        Optional<DungeonPlanner.Site> site = new DungeonPlanner.Terrain(context).findSite(this.theme);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        Optional<DungeonPlanner.Layout> layout = DungeonPlanner.plan(context.random(), site.get(), this.theme);
        if (layout.isEmpty() || nearOtherStructure(context)) {
            return Optional.empty();
        }
        return Optional.of(new StructurePosition(site.get().entrance(), collector -> layout.get().addPieces(collector)));
    }

    /** Whether one of {@link #KEEP_CLEAR_OF} starts within {@link #CLEARANCE} chunks of this one. */
    private static boolean nearOtherStructure(Context context) {
        Registry<StructureSet> sets = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        ChunkPos chunk = context.chunkPos();
        for (RegistryKey<StructureSet> key : KEEP_CLEAR_OF) {
            StructureSet set = sets.get(key);
            if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
                continue;
            }
            for (int x = chunk.x - CLEARANCE; x <= chunk.x + CLEARANCE; x++) {
                for (int z = chunk.z - CLEARANCE; z <= chunk.z + CLEARANCE; z++) {
                    ChunkPos start = placement.getStartChunk(context.seed(), x, z);
                    if (start.x != x || start.z != z) {
                        continue;
                    }
                    for (StructureSet.WeightedEntry entry : set.structures()) {
                        Structure other = entry.structure().value();
                        Context there = new Context(context.dynamicRegistryManager(), context.chunkGenerator(),
                                context.biomeSource(), context.noiseConfig(), context.structureTemplateManager(),
                                context.seed(), start, context.world(), other.getValidBiomes()::contains);
                        if (other.getValidStructurePosition(there).isPresent()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public StructureType<?> getType() {
        return DungeonStructures.DUNGEON;
    }
}
