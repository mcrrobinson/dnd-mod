package mattonfire.dnd.magic.items;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The extradimensional and fateful signature items (design 10, ticket I2): the Portable Hole (with its passage
 * block and the Astral rift) and the Deck of Many Things (with its curses and the Skull's Avatar of Death).
 * Registered here, apart from {@code ModItems}, with their magic defs.
 */
public final class PlanarItems {
    public static final Item PORTABLE_HOLE = Registry.register(Registries.ITEM,
            new Identifier(DnDClasses.MOD_ID, "portable_hole"),
            new PortableHoleItem(new FabricItemSettings().maxCount(1)));
    public static final Item DECK_OF_MANY_THINGS = Registry.register(Registries.ITEM,
            new Identifier(DnDClasses.MOD_ID, "deck_of_many_things"),
            new DeckOfManyThingsItem(new FabricItemSettings().maxCount(1)));

    public static final PortableHolePassageBlock PORTABLE_HOLE_PASSAGE = Registry.register(Registries.BLOCK,
            new Identifier(DnDClasses.MOD_ID, "portable_hole_passage"),
            new PortableHolePassageBlock(FabricBlockSettings.of(Material.PORTAL).strength(-1.0F, 3600000.0F)
                    .dropsNothing().noCollision().nonOpaque().sounds(BlockSoundGroup.WOOL)
                    .suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));

    public static final EntityType<DeckAvatarEntity> DECK_AVATAR = Registry.register(Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "deck_avatar"),
            FabricEntityTypeBuilder.<DeckAvatarEntity>create(SpawnGroup.MONSTER, DeckAvatarEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7F, 2.4F)).fireImmune().trackRangeChunks(8).build());

    private PlanarItems() {
    }

    public static void register() {
        MagicItems.register(MagicItemDef.of(PORTABLE_HOLE, MagicTier.RARE, MagicKind.WONDROUS, "cloth"));
        MagicItems.register(MagicItemDef.of(DECK_OF_MANY_THINGS, MagicTier.LEGENDARY, MagicKind.WONDROUS, "deck"));
        FabricDefaultAttributeRegistry.register(DECK_AVATAR, DeckAvatarEntity.createAvatarAttributes());
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(PORTABLE_HOLE);
            entries.add(DECK_OF_MANY_THINGS);
        });
        PortableHolePassages.register();
        AstralRift.register();
        DeckCurses.register();
    }
}
