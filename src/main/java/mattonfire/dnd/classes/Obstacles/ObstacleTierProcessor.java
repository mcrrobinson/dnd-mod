package mattonfire.dnd.classes.Obstacles;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * {@code dndclasses:obstacle_tier}: a structure processor for NBT templates and jigsaw pieces that contain
 * obstacle blocks. Obstacle blocks saved in a template keep their block entity NBT; this processor rewrites it
 * as the template is placed:
 * <ul>
 * <li>{@code "tier"} ({@code easy}, {@code medium}, {@code hard}, {@code very_hard}): every obstacle gets this
 * tier. Otherwise {@code "depth"} (a dungeon floor number) picks one with {@link #tierForDepth}. With neither,
 * the tier saved in the template stays.</li>
 * <li>{@code "critical"} (optional): overrides the saved {@code Critical} flag.</li>
 * <li>{@code GroupSeed} is always mixed with the placement's origin, so two copies of a template don't join
 * into one group when they touch, while the groups inside one copy stay as the author painted them.</li>
 * </ul>
 * Example processor list entry: {@code {"processor_type": "dndclasses:obstacle_tier", "depth": 3}}.
 */
public class ObstacleTierProcessor extends StructureProcessor {
    public static final Codec<Tier> TIER_CODEC = StringIdentifiable.createCodec(Tier::values);

    public static final Codec<ObstacleTierProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TIER_CODEC.optionalFieldOf("tier").forGetter(p -> Optional.ofNullable(p.tier)),
            Codec.INT.optionalFieldOf("depth").forGetter(p -> Optional.ofNullable(p.depth)),
            Codec.BOOL.optionalFieldOf("critical").forGetter(p -> Optional.ofNullable(p.critical)))
            .apply(instance, (tier, depth, critical) -> new ObstacleTierProcessor(tier.orElse(null),
                    depth.orElse(null), critical.orElse(null))));

    public static final StructureProcessorType<ObstacleTierProcessor> TYPE = Registry.register(
            Registries.STRUCTURE_PROCESSOR, new Identifier(DnDClasses.MOD_ID, "obstacle_tier"), () -> CODEC);

    @Nullable
    private final Tier tier;
    @Nullable
    private final Integer depth;
    @Nullable
    private final Boolean critical;

    public ObstacleTierProcessor(@Nullable Tier tier, @Nullable Integer depth, @Nullable Boolean critical) {
        this.tier = tier;
        this.depth = depth;
        this.critical = critical;
    }

    /** A fixed tier for every obstacle in the template. */
    public static ObstacleTierProcessor ofTier(Tier tier) {
        return new ObstacleTierProcessor(tier, null, null);
    }

    /** The tier for a dungeon floor's depth. */
    public static ObstacleTierProcessor ofDepth(int depth) {
        return new ObstacleTierProcessor(null, depth, null);
    }

    /**
     * The design's DC ladder by depth: the surface (0 or less) is Easy, floors 1-2 are Medium, floors 3 and deeper
     * are Hard. Very Hard (boss antechambers) is only ever set explicitly with {@code "tier"}.
     */
    public static Tier tierForDepth(int depth) {
        if (depth <= 0) {
            return Tier.EASY;
        }
        return depth <= 2 ? Tier.MEDIUM : Tier.HARD;
    }

    /** Called once, from {@link ObstacleTypes#register()}, to load the class (which registers the type). */
    static void register() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot,
            StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo,
            StructurePlacementData data) {
        if (!(currentBlockInfo.state.getBlock() instanceof ObstacleBlock)) {
            return currentBlockInfo;
        }
        NbtCompound nbt = rewrite(currentBlockInfo.nbt, pos);
        return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos, currentBlockInfo.state, nbt);
    }

    /** The obstacle block entity NBT to place, for a template placed with its origin at {@code origin}. */
    public NbtCompound rewrite(@Nullable NbtCompound saved, BlockPos origin) {
        NbtCompound nbt = saved == null ? new NbtCompound() : saved.copy();
        Tier chosen = tier != null ? tier : depth != null ? tierForDepth(depth) : null;
        if (chosen != null) {
            nbt.putString("Tier", chosen.asString());
        } else if (!nbt.contains("Tier")) {
            nbt.putString("Tier", Tier.MEDIUM.asString());
        }
        if (critical != null) {
            nbt.putBoolean("Critical", critical);
        }
        nbt.putLong("GroupSeed", mixSeed(nbt.getLong("GroupSeed"), origin));
        return nbt;
    }

    /** The saved group seed, made unique to this placement. Same inputs, same seed, so a group stays one group. */
    public static long mixSeed(long saved, BlockPos origin) {
        return MathHelper.hashCode(origin.getX(), origin.getY(), origin.getZ()) * 0x9E3779B97F4A7C15L ^ saved;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
