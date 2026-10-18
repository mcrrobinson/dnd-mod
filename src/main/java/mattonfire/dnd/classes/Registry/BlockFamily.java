package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WoodType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * A decorative building material and its shapes (stairs, slab, wall, ...), registered from one call and
 * listed in the DnD Building Blocks tab ({@link ModItemGroup#BUILDING_BLOCKS_ITEMGROUP}). Families are
 * declared in {@link ModBlockFamilies}.
 * <p>
 * Ids follow vanilla: the base is {@code <name>} (e.g. {@code runic_deepslate_bricks}), the shapes are
 * {@code <stem>_<shape>} with the stem dropping a trailing "s" from bricks and tiles
 * ({@code runic_deepslate_brick_stairs}). Blockstates, models, loot tables, tags, recipes and lang entries
 * come from {@code tools/block_families.py}, which lists the same families; {@link #register()} warns in the
 * log when a generated file is missing.
 * <p>
 * Every shape copies the base's settings (hardness, tool, sound, map colour). Doors and trapdoors also
 * become non-opaque, and the client draws them cut out ({@link #cutoutBlocks()}).
 */
public final class BlockFamily {
    /** A family's shapes, in the order the creative tab lists them. */
    public enum Shape {
        BASE(""), STAIRS("_stairs"), SLAB("_slab"), WALL("_wall"), PILLAR("_pillar"), FENCE("_fence"),
        FENCE_GATE("_fence_gate"), DOOR("_door"), TRAPDOOR("_trapdoor");

        private final String suffix;

        Shape(String suffix) {
            this.suffix = suffix;
        }
    }

    private static final List<BlockFamily> ALL = new ArrayList<>();

    private final String name;
    private final String stem;
    private final Map<Shape, Block> blocks;

    private BlockFamily(String name, String stem, Map<Shape, Block> blocks) {
        this.name = name;
        this.stem = stem;
        this.blocks = blocks;
    }

    /** A family named {@code name} that copies the settings of the vanilla block {@code material}. */
    public static Builder builder(String name, Block material) {
        return new Builder(name, FabricBlockSettings.copyOf(material));
    }

    /** A family named {@code name} with its own settings. */
    public static Builder builder(String name, AbstractBlock.Settings settings) {
        return new Builder(name, settings);
    }

    /** Every family made so far, in declaration order. */
    public static List<BlockFamily> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The base block's id path, e.g. {@code runic_deepslate_bricks}. */
    public String name() {
        return this.name;
    }

    /** The id path of {@code shape}, e.g. {@code runic_deepslate_brick_stairs}. */
    public String id(Shape shape) {
        return shape == Shape.BASE ? this.name : this.stem + shape.suffix;
    }

    public boolean has(Shape shape) {
        return this.blocks.containsKey(shape);
    }

    /** The family's {@code shape}. Throws if the family doesn't have it. */
    public Block get(Shape shape) {
        Block block = this.blocks.get(shape);
        if (block == null) {
            throw new IllegalArgumentException(this.name + " has no " + shape);
        }
        return block;
    }

    public Block base() {
        return get(Shape.BASE);
    }

    public Block stairs() {
        return get(Shape.STAIRS);
    }

    public Block slab() {
        return get(Shape.SLAB);
    }

    public Block wall() {
        return get(Shape.WALL);
    }

    public Block pillar() {
        return get(Shape.PILLAR);
    }

    public Block fence() {
        return get(Shape.FENCE);
    }

    public Block fenceGate() {
        return get(Shape.FENCE_GATE);
    }

    public Block door() {
        return get(Shape.DOOR);
    }

    public Block trapdoor() {
        return get(Shape.TRAPDOOR);
    }

    /** The family's blocks in tab order. */
    public List<Block> blocks() {
        return List.copyOf(this.blocks.values());
    }

    /** Doors and trapdoors of every family: the client renders them cut out. */
    public static List<Block> cutoutBlocks() {
        List<Block> cutout = new ArrayList<>();
        for (BlockFamily family : ALL) {
            for (Shape shape : new Shape[] { Shape.DOOR, Shape.TRAPDOOR }) {
                if (family.has(shape)) {
                    cutout.add(family.get(shape));
                }
            }
        }
        return cutout;
    }

    /** Registers the blocks and their items. Call once per family, from {@link ModBlockFamilies#register()}. */
    public void register() {
        for (Map.Entry<Shape, Block> entry : this.blocks.entrySet()) {
            Identifier id = new Identifier(DnDClasses.MOD_ID, id(entry.getKey()));
            Block block = entry.getValue();
            Registry.register(Registries.BLOCK, id, block);
            Registry.register(Registries.ITEM, id, entry.getKey() == Shape.DOOR
                    ? new TallBlockItem(block, new FabricItemSettings())
                    : new BlockItem(block, new FabricItemSettings()));
            String blockstate = "/assets/" + DnDClasses.MOD_ID + "/blockstates/" + id.getPath() + ".json";
            String loot = "/data/" + DnDClasses.MOD_ID + "/loot_tables/blocks/" + id.getPath() + ".json";
            if (BlockFamily.class.getResource(blockstate) == null || BlockFamily.class.getResource(loot) == null) {
                DnDClasses.LOGGER.warn("[BlockFamily] {} has no generated resources; run tools/block_families.py", id);
            }
        }
    }

    public static final class Builder {
        private final String name;
        private final AbstractBlock.Settings settings;
        private String stem;
        private Function<AbstractBlock.Settings, Block> baseFactory = Block::new;
        private final Map<Shape, Function<AbstractBlock.Settings, Block>> shapes = new EnumMap<>(Shape.class);

        private Builder(String name, AbstractBlock.Settings settings) {
            this.name = name;
            this.settings = settings;
            this.stem = name.endsWith("bricks") || name.endsWith("tiles") ? name.substring(0, name.length() - 1)
                    : name;
        }

        /** The prefix of the shapes' ids, if it isn't the default (the name, minus "s" for bricks and tiles). */
        public Builder stem(String stem) {
            this.stem = stem;
            return this;
        }

        /** The base block lies along an axis, like a log ({@code <name>} and {@code <name>_top} textures). */
        public Builder axisBase() {
            this.baseFactory = PillarBlock::new;
            return this;
        }

        public Builder stairs() {
            return shape(Shape.STAIRS, null);
        }

        public Builder slab() {
            return shape(Shape.SLAB, SlabBlock::new);
        }

        public Builder wall() {
            return shape(Shape.WALL, WallBlock::new);
        }

        /** A pillar shape beside the base, like the quartz pillar. */
        public Builder pillar() {
            return shape(Shape.PILLAR, PillarBlock::new);
        }

        public Builder fence() {
            return shape(Shape.FENCE, FenceBlock::new);
        }

        /** A fence gate with {@code woodType}'s open and close sounds. */
        public Builder fenceGate(WoodType woodType) {
            return shape(Shape.FENCE_GATE, settings -> new FenceGateBlock(settings, woodType));
        }

        /** A door with {@code setType}'s sounds; iron-like sets can only be opened by redstone. */
        public Builder door(BlockSetType setType) {
            return shape(Shape.DOOR, settings -> new DoorBlock(settings.nonOpaque(), setType));
        }

        /** A trapdoor with {@code setType}'s sounds; iron-like sets can only be opened by redstone. */
        public Builder trapdoor(BlockSetType setType) {
            return shape(Shape.TRAPDOOR,
                    settings -> new TrapdoorBlock(settings.nonOpaque().allowsSpawning((s, w, p, t) -> false), setType));
        }

        /** Stairs, slab and wall: the usual stone set. */
        public Builder stoneShapes() {
            return stairs().slab().wall();
        }

        private Builder shape(Shape shape, Function<AbstractBlock.Settings, Block> factory) {
            this.shapes.put(shape, factory);
            return this;
        }

        /** Makes the blocks (and adds the family to {@link #all()}). Register them with {@link #register()}. */
        public BlockFamily build() {
            Map<Shape, Block> blocks = new EnumMap<>(Shape.class);
            Block base = this.baseFactory.apply(FabricBlockSettings.copyOf(this.settings));
            blocks.put(Shape.BASE, base);
            for (Map.Entry<Shape, Function<AbstractBlock.Settings, Block>> entry : this.shapes.entrySet()) {
                FabricBlockSettings copy = FabricBlockSettings.copyOf(this.settings);
                blocks.put(entry.getKey(), entry.getKey() == Shape.STAIRS
                        ? new StairsBlock(base.getDefaultState(), copy)
                        : entry.getValue().apply(copy));
            }
            BlockFamily family = new BlockFamily(this.name, this.stem, blocks);
            ALL.add(family);
            return family;
        }
    }
}
