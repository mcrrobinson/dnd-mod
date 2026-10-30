package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.KoboldEggClutchBlock;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.entity.SettlementGrudges;
import mattonfire.dnd.entity.kobold.KoboldCourage;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Kobold warrens (design 10, section 4; docs/structures/kobold-warrens.md): the structure type and its pieces, the
 * Kobold Egg Clutch, the warren law and the kobolds' "at home" and bolt-hole hooks. Where warrens generate is data:
 * {@code data/dndclasses/worldgen/structure/kobold_warren.json} and the {@code kobold_warrens} structure set. The trap
 * blocks are in {@code TrapBlocks}.
 */
public final class KoboldWarrens {
    public static final StructureType<KoboldWarrenStructure> KOBOLD_WARREN = Registry.register(Registries.STRUCTURE_TYPE,
            id("kobold_warren"), () -> KoboldWarrenStructure.CODEC);

    public static final StructurePieceType MOUTH = piece("kobold_warren_mouth", WarrenMouthPiece::new);
    public static final StructurePieceType TUNNEL = piece("kobold_warren_tunnel", WarrenTunnelPiece::new);
    public static final StructurePieceType DEN = piece("kobold_warren_den", WarrenDenPiece::new);
    public static final StructurePieceType SHRINE = piece("kobold_warren_shrine", WarrenShrinePiece::new);
    public static final StructurePieceType NURSERY = piece("kobold_warren_nursery", WarrenNurseryPiece::new);
    public static final StructurePieceType WORKSHOP = piece("kobold_warren_workshop", WarrenWorkshopPiece::new);
    public static final StructurePieceType LARDER = piece("kobold_warren_larder", WarrenLarderPiece::new);
    public static final StructurePieceType CHIEF = piece("kobold_warren_chief", WarrenChiefPiece::new);
    public static final StructurePieceType BOLT_HOLE = piece("kobold_warren_bolt_hole", BoltHolePiece::new);

    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, id("kobold_warren"));
    /** {@code #dndclasses:kobold_warrens}: the structures {@code /dndhome koboldwarren} looks for. */
    public static final TagKey<Structure> TAG = TagKey.of(RegistryKeys.STRUCTURE, id("kobold_warrens"));
    public static final Identifier SET = id("kobold_warrens");

    public static final KoboldEggClutchBlock KOBOLD_EGG_CLUTCH = new KoboldEggClutchBlock(FabricBlockSettings
            .of(Material.EGG, MapColor.RED).strength(0.5F).nonOpaque().dropsNothing().sounds(BlockSoundGroup.METAL)
            .luminance(state -> 0));
    public static Item KOBOLD_EGG_CLUTCH_ITEM;

    /** The warren law: the chief's hoard chest. */
    public static final SettlementGrudges.Rules LAW = SettlementGrudges.add(new KoboldWarrenLaw());

    private KoboldWarrens() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static StructurePieceType piece(String name, StructurePieceType.Simple loader) {
        return Registry.register(Registries.STRUCTURE_PIECE, id(name), loader);
    }

    public static void register() {
        Registry.register(Registries.BLOCK, id("kobold_egg_clutch"), KOBOLD_EGG_CLUTCH);
        KOBOLD_EGG_CLUTCH_ITEM = Registry.register(Registries.ITEM, id("kobold_egg_clutch"),
                new BlockItem(KOBOLD_EGG_CLUTCH, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> entries.add(KOBOLD_EGG_CLUTCH_ITEM));

        // Kobolds are always brave in their own warren, and a hurt one bolts for the nearest bolt-hole.
        KoboldCourage.registerHome(kobold -> kobold.world instanceof ServerWorld world
                && KoboldWarren.contains(world, kobold.getBlockPos()));
        KoboldCourage.registerRefuge(KoboldWarren::refuge);
    }
}
