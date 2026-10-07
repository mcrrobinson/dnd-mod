package mattonfire.dnd.tavern;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.InnkeeperEntity;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The hobbit tavern: the innkeeper, the bounty board and its notices, and the ale. Called from
 * the mod initializer.
 */
public final class Tavern {
    public static final BountyBoardBlock BOUNTY_BOARD = Registry.register(Registries.BLOCK, id("bounty_board"),
            new BountyBoardBlock(FabricBlockSettings.of(Material.WOOD).strength(1.0F).sounds(BlockSoundGroup.WOOD).nonOpaque()));

    public static final BlockEntityType<BountyBoardBlockEntity> BOUNTY_BOARD_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, id("bounty_board"),
            FabricBlockEntityTypeBuilder.create(BountyBoardBlockEntity::new, BOUNTY_BOARD).build(null));

    public static final EntityType<InnkeeperEntity> INNKEEPER = Registry.register(Registries.ENTITY_TYPE, id("innkeeper"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, InnkeeperEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 1.1f))
                    .build());

    public static final Item BOUNTY_BOARD_ITEM = item("bounty_board", new BlockItem(BOUNTY_BOARD, new FabricItemSettings()));
    public static final Item BOUNTY_NOTICE = item("bounty_notice", new BountyNoticeItem(new FabricItemSettings().maxCount(1)));
    public static final Item ALE = item("ale", new AleItem(new FabricItemSettings()));
    public static final Item INNKEEPER_SPAWN_EGG = item("innkeeper_spawn_egg",
            new SpawnEggItem(INNKEEPER, 0x7A4A1E, 0xE8C26A, new FabricItemSettings()));

    private Tavern() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static Item item(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(INNKEEPER, HobbitEntity.createHobbitAttributes());
        BountyRewards.register();
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(BOUNTY_BOARD_ITEM);
            entries.add(ALE);
            entries.add(INNKEEPER_SPAWN_EGG);
            for (Bounty bounty : Bounty.values()) {
                entries.add(BountyNoticeItem.create(bounty));
            }
        });
    }
}
