package mattonfire.dnd.world.gen.ancient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.dragon.ChromaticColor;
import mattonfire.dnd.world.gen.StructureProximity;
import mattonfire.dnd.world.gen.TerrainColumns;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * An Ancient dragon's lair (design 10, 6.1): a volcanic caldera raised on a rise or mesa, with the hoard hall under
 * it. The site is the highest ground within {@value #SEARCH} blocks of the allowed land nearest the chunk's middle
 * (looked for up to {@value #BIOME_SEARCH} blocks away); it has to stand at least
 * {@value #MIN_HEIGHT} above sea level, the ground round the cone's foot has to sit at least {@value #MIN_DROP} lower
 * on average (a rise, not a pit), and it must be dry. It keeps {@value #CLEARANCE} chunks from the orc strongholds,
 * dragon and frost lairs, dwarven fortresses and dragon temples that really generate near it.
 * <p>
 * The mouth faces the lowest side. The crater floor sits {@value AncientLairLayout#CRATER_DEPTH} below a rim that
 * rises at least {@value AncientLairLayout#RISE} above the ground round it, or that's cut down into the hilltop when
 * the hill is higher still.
 */
public class AncientLairStructure extends Structure {
    public static final Codec<AncientLairStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            configCodecBuilder(instance),
            Codec.STRING.xmap(AncientLairLayout::colorOf, ChromaticColor::id).fieldOf("color").forGetter(s -> s.color))
            .apply(instance, AncientLairStructure::new));

    /** How far from the chunk's middle allowed land is looked for (the pieces must stay within 8 chunks of it). */
    static final int BIOME_SEARCH = 48;
    static final int SEARCH = 20;
    private static final int STEP = 4;
    static final int MIN_HEIGHT = 20;
    static final int MIN_DROP = 6;
    static final int CLEARANCE = 10;
    private static final int RING_SAMPLES = 16;
    private static final List<Identifier> KEEP_CLEAR_OF = List.of(id("orc_strongholds"), id("dragon_lairs"),
            id("frost_lairs"), id("dwarven_fortresses"), id("dragon_temples"));

    private final ChromaticColor color;
    /** Why sites were turned down, counted for the seeds check ({@code /dndhome ancientlairseeds}). */
    public static final Map<String, Integer> REJECTS = new ConcurrentHashMap<>();

    private static Optional<StructurePosition> reject(String why) {
        REJECTS.merge(why, 1, Integer::sum);
        return Optional.empty();
    }

    public AncientLairStructure(Config config, ChromaticColor color) {
        super(config);
        this.color = color;
    }

    public ChromaticColor color() {
        return this.color;
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        TerrainColumns columns = new TerrainColumns(context);
        // The allowed land nearest the chunk's middle, within a few chunks: badlands and peaks come in patches, so
        // a grid slot just off one still finds it.
        int nearX = 0;
        int nearZ = 0;
        int nearest = Integer.MAX_VALUE;
        for (int dx = -BIOME_SEARCH; dx <= BIOME_SEARCH; dx += 16) {
            for (int dz = -BIOME_SEARCH; dz <= BIOME_SEARCH; dz += 16) {
                int distance = dx * dx + dz * dz;
                if (distance < nearest && columns.biomeAllowed(chunk.getCenterX() + dx, chunk.getCenterZ() + dz)) {
                    nearest = distance;
                    nearX = chunk.getCenterX() + dx;
                    nearZ = chunk.getCenterZ() + dz;
                }
            }
        }
        if (nearest == Integer.MAX_VALUE) {
            return reject("biome");
        }
        // The highest ground round there...
        int bestX = 0;
        int bestZ = 0;
        int top = Integer.MIN_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx += STEP) {
            for (int dz = -SEARCH; dz <= SEARCH; dz += STEP) {
                int x = nearX + dx;
                int z = nearZ + dz;
                int y = columns.ground(x, z);
                if (y > top) {
                    top = y;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        int seaLevel = context.chunkGenerator().getSeaLevel();
        if (top < seaLevel + MIN_HEIGHT) {
            return reject("low");
        }
        if (columns.water(bestX, bestZ) || !columns.biomeAllowed(bestX, bestZ)) {
            return reject("site biome or water");
        }
        // ...on a rise: the ground round the cone's foot falls away, and it's all dry land.
        int sum = 0;
        for (int i = 0; i < RING_SAMPLES; i++) {
            double angle = Math.PI * 2.0D * i / RING_SAMPLES;
            int x = bestX + (int) Math.round(Math.cos(angle) * AncientLairLayout.BASE_RADIUS);
            int z = bestZ + (int) Math.round(Math.sin(angle) * AncientLairLayout.BASE_RADIUS);
            if (columns.water(x, z)) {
                return reject("wet foot");
            }
            sum += columns.ground(x, z);
            int mx = bestX + (int) Math.round(Math.cos(angle) * AncientLairLayout.FLOOR_RADIUS);
            int mz = bestZ + (int) Math.round(Math.sin(angle) * AncientLairLayout.FLOOR_RADIUS);
            if (columns.water(mx, mz)) {
                return reject("wet crater");
            }
        }
        int base = Math.round(sum / (float) RING_SAMPLES);
        if (top - base < MIN_DROP) {
            return reject("flat");
        }
        for (Identifier set : KEEP_CLEAR_OF) {
            if (StructureProximity.nearGenerating(context, set, CLEARANCE)) {
                return reject("near " + set.getPath());
            }
        }
        REJECTS.merge("ok", 1, Integer::sum);
        // The mouth opens on the lowest side.
        Direction front = Direction.NORTH;
        int lowest = Integer.MAX_VALUE;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            int height = 0;
            for (int s = -1; s <= 1; s++) {
                Direction right = direction.rotateYClockwise();
                int x = bestX + direction.getOffsetX() * AncientLairLayout.BASE_RADIUS + right.getOffsetX() * s * 8;
                int z = bestZ + direction.getOffsetZ() * AncientLairLayout.BASE_RADIUS + right.getOffsetZ() * s * 8;
                height += columns.ground(x, z);
            }
            if (height < lowest) {
                lowest = height;
                front = direction;
            }
        }
        int rim = Math.max(base + AncientLairLayout.RISE, top + 2);
        BlockPos center = new BlockPos(bestX, rim - AncientLairLayout.CRATER_DEPTH, bestZ);
        AncientLairLayout layout = new AncientLairLayout(this.color, center, base, front, context.random().nextLong());
        return Optional.of(new StructurePosition(center, collector -> {
            collector.addPiece(new CalderaPiece(layout));
            collector.addPiece(new HoardHallPiece(layout));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return AncientLairStructures.ANCIENT_LAIR;
    }
}
