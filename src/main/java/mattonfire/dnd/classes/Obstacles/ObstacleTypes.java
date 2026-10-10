package mattonfire.dnd.classes.Obstacles;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.types.BloodSealType;
import mattonfire.dnd.classes.Obstacles.types.BridgeWinchType;
import mattonfire.dnd.classes.Obstacles.types.DesecratedAltarType;
import mattonfire.dnd.classes.Obstacles.types.DreadWardType;
import mattonfire.dnd.classes.Obstacles.types.HolyWardType;
import mattonfire.dnd.classes.Obstacles.types.OssuaryGateType;
import mattonfire.dnd.classes.Obstacles.types.ChasmType;
import mattonfire.dnd.classes.Obstacles.types.CollapsedPassageType;
import mattonfire.dnd.classes.Obstacles.types.CrumblingLedgeType;
import mattonfire.dnd.classes.Obstacles.types.HeavyBoulderType;
import mattonfire.dnd.classes.Obstacles.types.PortcullisType;
import mattonfire.dnd.classes.Obstacles.types.PortcullisWinchType;
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

    // Physical obstacles: Barbarians, Fighters and Monks
    public static final CollapsedPassageType COLLAPSED_PASSAGE = new CollapsedPassageType(id("collapsed_passage"));
    public static final HeavyBoulderType HEAVY_BOULDER = new HeavyBoulderType(id("heavy_boulder"));
    public static final PortcullisType PORTCULLIS = new PortcullisType(id("portcullis"));
    public static final PortcullisWinchType PORTCULLIS_WINCH = new PortcullisWinchType(id("portcullis_winch"),
            PORTCULLIS);
    public static final ChasmType CHASM = new ChasmType(id("chasm"));
    public static final BridgeWinchType BRIDGE_WINCH = new BridgeWinchType(id("bridge_winch"), CHASM);
    public static final CrumblingLedgeType CRUMBLING_LEDGE = new CrumblingLedgeType(id("crumbling_ledge"));

    public static final ObstacleBlock COLLAPSED_PASSAGE_BLOCK = register(COLLAPSED_PASSAGE,
            solidSettings(Material.STONE, BlockSoundGroup.GRAVEL));
    public static final ObstacleBlock HEAVY_BOULDER_BLOCK = register(HEAVY_BOULDER,
            solidSettings(Material.STONE, BlockSoundGroup.STONE));
    public static final ObstacleBlock PORTCULLIS_BLOCK = register(PORTCULLIS,
            solidSettings(Material.METAL, BlockSoundGroup.CHAIN));
    public static final ObstacleBlock PORTCULLIS_WINCH_BLOCK = register(PORTCULLIS_WINCH,
            solidSettings(Material.WOOD, BlockSoundGroup.WOOD));
    public static final ObstacleBlock CHASM_BLOCK = register(CHASM, solidSettings(Material.WOOD, BlockSoundGroup.WOOD));
    public static final ObstacleBlock BRIDGE_WINCH_BLOCK = register(BRIDGE_WINCH,
            solidSettings(Material.WOOD, BlockSoundGroup.WOOD));
    public static final ObstacleBlock CRUMBLING_LEDGE_BLOCK = register(CRUMBLING_LEDGE,
            solidSettings(Material.STONE, BlockSoundGroup.STONE));

    // Divine and death obstacles: Clerics, Paladins, Necromancers and Blood Hunters
    public static final HolyWardType HOLY_WARD = new HolyWardType(id("holy_ward"));
    public static final DesecratedAltarType DESECRATED_ALTAR = new DesecratedAltarType(id("desecrated_altar"));
    public static final DreadWardType DREAD_WARD = new DreadWardType(id("dread_ward"));
    public static final OssuaryGateType OSSUARY_GATE = new OssuaryGateType(id("ossuary_gate"));
    public static final BloodSealType BLOOD_SEAL = new BloodSealType(id("blood_seal"));

    public static final ObstacleBlock HOLY_WARD_BLOCK = register(HOLY_WARD,
            glowSettings(Material.STONE, BlockSoundGroup.STONE, 8));
    public static final ObstacleBlock DESECRATED_ALTAR_BLOCK = register(DESECRATED_ALTAR,
            solidSettings(Material.STONE, BlockSoundGroup.GILDED_BLACKSTONE)
                    .luminance(state -> state.get(ObstacleBlock.STATE) == ObstacleState.SEALED ? 10 : 12));
    public static final ObstacleBlock DREAD_WARD_BLOCK = register(DREAD_WARD,
            solidSettings(Material.STONE, BlockSoundGroup.DEEPSLATE_BRICKS));
    public static final ObstacleBlock OSSUARY_GATE_BLOCK = register(OSSUARY_GATE,
            solidSettings(Material.STONE, BlockSoundGroup.BONE));
    public static final ObstacleBlock BLOOD_SEAL_BLOCK = register(BLOOD_SEAL,
            glowSettings(Material.STONE, BlockSoundGroup.NETHER_WART, 4));

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

    /** Rubble, boulders, gates and machinery: no glow. Still non-opaque, as open blocks may be see-through. */
    private static FabricBlockSettings solidSettings(Material material, BlockSoundGroup sounds) {
        return FabricBlockSettings.of(material)
                .sounds(sounds)
                .nonOpaque()
                .dropsNothing()
                .allowsSpawning((state, world, pos, type) -> false)
                .solidBlock((state, world, pos) -> false)
                .suffocates((state, world, pos) -> false)
                .blockVision((state, world, pos) -> false);
    }

    /** Like {@link #solidSettings}, glowing at {@code light} while sealed (wards and sigils). */
    private static FabricBlockSettings glowSettings(Material material, BlockSoundGroup sounds, int light) {
        return solidSettings(material, sounds)
                .luminance(state -> state.get(ObstacleBlock.STATE) == ObstacleState.SEALED ? light : 0);
    }

    /** Registers a type with its block and block item, under the type's id. */
    private static ObstacleBlock register(ObstacleType type, FabricBlockSettings settings) {
        settings.strength(type.hardness(), type.blastResistance());
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
        CryptSleepers.register();
        for (ObstacleType type : TYPES.values()) {
            type.registerEvents();
        }
    }
}
