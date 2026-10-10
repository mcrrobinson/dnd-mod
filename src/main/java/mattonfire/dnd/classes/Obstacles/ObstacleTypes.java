package mattonfire.dnd.classes.Obstacles;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The obstacle registry: every {@link ObstacleType}, its block and item, and the block entity type
 * they all share. Numbers live in the type classes, as constants.
 */
public final class ObstacleTypes {
    /** Obsidian's hardness: about 9.4 seconds per block with a diamond pickaxe. */
    public static final float SEAL_HARDNESS = 50.0f;
    /** Out of reach of TNT, creepers and withers. */
    public static final float BLAST_RESISTANCE = 3_600_000.0f;

    private static final Map<Identifier, ObstacleType> TYPES = new LinkedHashMap<>();
    private static final Map<ObstacleType, ObstacleBlock> BLOCKS = new LinkedHashMap<>();

    public static final ArcaneSealType LESSER_ARCANE_SEAL = new ArcaneSealType(id("lesser_arcane_seal"), false);
    public static final ArcaneSealType GREATER_ARCANE_SEAL = new ArcaneSealType(id("greater_arcane_seal"), true);

    public static final ObstacleBlock LESSER_ARCANE_SEAL_BLOCK = register(LESSER_ARCANE_SEAL, sealSettings());
    public static final ObstacleBlock GREATER_ARCANE_SEAL_BLOCK = register(GREATER_ARCANE_SEAL, sealSettings());

    public static final BlockEntityType<ObstacleBlockEntity> BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, id("obstacle"),
            FabricBlockEntityTypeBuilder.create(ObstacleBlockEntity::new,
                    BLOCKS.values().toArray(new ObstacleBlock[0])).build(null));

    private ObstacleTypes() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static FabricBlockSettings sealSettings() {
        return FabricBlockSettings.of(Material.AMETHYST)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                .nonOpaque()
                .dropsNothing()
                .luminance(state -> state.get(ObstacleBlock.STATE) == ObstacleState.SEALED ? 7 : 2)
                .allowsSpawning((state, world, pos, type) -> false)
                .solidBlock((state, world, pos) -> false)
                .suffocates((state, world, pos) -> false)
                .blockVision((state, world, pos) -> false);
    }

    /** Registers a type with its block and block item, under the type's id. */
    private static ObstacleBlock register(ObstacleType type, FabricBlockSettings settings) {
        settings.strength(type.breakable() ? SEAL_HARDNESS : -1.0f, BLAST_RESISTANCE);
        ObstacleBlock block = Registry.register(Registries.BLOCK, type.id(), new ObstacleBlock(type, settings));
        Registry.register(Registries.ITEM, type.id(), new BlockItem(block, new FabricItemSettings()));
        TYPES.put(type.id(), type);
        BLOCKS.put(type, block);
        return block;
    }

    @Nullable
    public static ObstacleType byId(Identifier id) {
        return TYPES.get(id);
    }

    public static Collection<ObstacleType> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static ObstacleBlock blockFor(ObstacleType type) {
        return BLOCKS.get(type);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            for (ObstacleBlock block : BLOCKS.values()) {
                entries.add(block);
            }
        });
        ObstacleInteractions.register();
    }
}
