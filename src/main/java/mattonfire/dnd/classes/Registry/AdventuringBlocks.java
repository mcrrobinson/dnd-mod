package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.adventuring.BedrollBlock;
import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlock;
import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlockEntity;
import mattonfire.dnd.classes.Blocks.adventuring.TavernBenchBlock;
import mattonfire.dnd.classes.Blocks.adventuring.TavernTableBlock;
import mattonfire.dnd.classes.Blocks.adventuring.WantedPosterBlock;
import mattonfire.dnd.classes.Rest.BedrollItem;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

/**
 * The shared adventuring blocks (Building Blocks: "Shared adventuring blocks"): blocks that aren't tied to one
 * structure but give the mod's systems a physical form. Resources come from
 * {@code tools/adventuring_block_textures.py}; they're listed in the DnD Building Blocks tab after the families.
 * <ul>
 * <li>Bedrolls (red, green, blue, brown): a long rest like a bed, without setting your spawn. The red one is the
 * party camp's Bedroll ({@code PartyCamp.BEDROLL}); every colour makes camp too.</li>
 * <li>The Adventurer's Signpost, naming a dungeon and its Challenge tier at its entrance.</li>
 * <li>The Wanted Poster, wall-hung dressing for taverns and gates.</li>
 * <li>Tavern tables and benches in oak, spruce, dark oak and Heartwood.</li>
 * </ul>
 */
public final class AdventuringBlocks {
    /** The party camp's red Bedroll; dye it green, blue or brown. */
    public static final BedrollBlock BEDROLL = bedroll(DyeColor.RED);
    public static final BedrollBlock GREEN_BEDROLL = bedroll(DyeColor.GREEN);
    public static final BedrollBlock BLUE_BEDROLL = bedroll(DyeColor.BLUE);
    public static final BedrollBlock BROWN_BEDROLL = bedroll(DyeColor.BROWN);
    /** The red Bedroll's item: {@code PartyCamp.BEDROLL}. */
    public static final Item BEDROLL_ITEM = new BedrollItem(BEDROLL, new FabricItemSettings().maxCount(1));

    public static final SignpostBlock SIGNPOST = new SignpostBlock(FabricBlockSettings.copyOf(Blocks.OAK_FENCE)
            .strength(1.0F).nonOpaque());
    public static final BlockEntityType<SignpostBlockEntity> SIGNPOST_ENTITY =
            FabricBlockEntityTypeBuilder.create(SignpostBlockEntity::new, SIGNPOST).build(null);
    public static final WantedPosterBlock WANTED_POSTER = new WantedPosterBlock(FabricBlockSettings.of(Material.DECORATION,
            MapColor.PALE_YELLOW).noCollision().strength(0.3F).sounds(BlockSoundGroup.WOOL));

    /** Tables and benches by wood: oak, spruce, dark oak, heartwood. */
    public static final Map<String, TavernTableBlock> TABLES = new LinkedHashMap<>();
    public static final Map<String, TavernBenchBlock> BENCHES = new LinkedHashMap<>();
    public static final TavernTableBlock OAK_TAVERN_TABLE = table("oak", Blocks.OAK_PLANKS);
    public static final TavernTableBlock SPRUCE_TAVERN_TABLE = table("spruce", Blocks.SPRUCE_PLANKS);
    public static final TavernTableBlock DARK_OAK_TAVERN_TABLE = table("dark_oak", Blocks.DARK_OAK_PLANKS);
    public static final TavernTableBlock HEARTWOOD_TAVERN_TABLE = table("heartwood", ModBlockFamilies.HEARTWOOD_PLANKS.base());
    public static final TavernBenchBlock OAK_TAVERN_BENCH = bench("oak", Blocks.OAK_PLANKS);
    public static final TavernBenchBlock SPRUCE_TAVERN_BENCH = bench("spruce", Blocks.SPRUCE_PLANKS);
    public static final TavernBenchBlock DARK_OAK_TAVERN_BENCH = bench("dark_oak", Blocks.DARK_OAK_PLANKS);
    public static final TavernBenchBlock HEARTWOOD_TAVERN_BENCH = bench("heartwood", ModBlockFamilies.HEARTWOOD_PLANKS.base());

    private static final List<Item> ITEMS = new ArrayList<>();

    private AdventuringBlocks() {
    }

    private static BedrollBlock bedroll(DyeColor color) {
        return new BedrollBlock(color, FabricBlockSettings.copyOf(Blocks.WHITE_BED).sounds(BlockSoundGroup.WOOL)
                .mapColor(color.getMapColor()));
    }

    private static TavernTableBlock table(String wood, Block planks) {
        TavernTableBlock table = new TavernTableBlock(FabricBlockSettings.copyOf(planks).nonOpaque());
        TABLES.put(wood, table);
        return table;
    }

    private static TavernBenchBlock bench(String wood, Block planks) {
        TavernBenchBlock bench = new TavernBenchBlock(FabricBlockSettings.copyOf(planks).nonOpaque());
        BENCHES.put(wood, bench);
        return bench;
    }

    public static void register() {
        register("bedroll", BEDROLL, BEDROLL_ITEM);
        register("green_bedroll", GREEN_BEDROLL, new BedrollItem(GREEN_BEDROLL, new FabricItemSettings().maxCount(1)));
        register("blue_bedroll", BLUE_BEDROLL, new BedrollItem(BLUE_BEDROLL, new FabricItemSettings().maxCount(1)));
        register("brown_bedroll", BROWN_BEDROLL, new BedrollItem(BROWN_BEDROLL, new FabricItemSettings().maxCount(1)));
        register("adventurers_signpost", SIGNPOST, new BlockItem(SIGNPOST, new FabricItemSettings()));
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "adventurers_signpost"),
                SIGNPOST_ENTITY);
        register("wanted_poster", WANTED_POSTER, new BlockItem(WANTED_POSTER, new FabricItemSettings()));
        TABLES.forEach((wood, table) -> register(wood + "_tavern_table", table, new BlockItem(table, new FabricItemSettings())));
        BENCHES.forEach((wood, bench) -> register(wood + "_tavern_bench", bench, new BlockItem(bench, new FabricItemSettings())));

        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        for (Block block : List.of(BEDROLL, GREEN_BEDROLL, BLUE_BEDROLL, BROWN_BEDROLL, WANTED_POSTER)) {
            fire.add(block, 30, 60);
        }
        fire.add(SIGNPOST, 5, 20);
        TABLES.values().forEach(table -> fire.add(table, 5, 20));
        BENCHES.values().forEach(bench -> fire.add(bench, 5, 20));

        // A bedroll is somewhere to sleep, not a home: it never sets the sleeper's spawn point.
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, pos) ->
                !(player.getWorld().getBlockState(pos).getBlock() instanceof BedrollBlock));

        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> ITEMS.forEach(entries::add));
    }

    private static void register(String name, Block block, Item item) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, item);
        ITEMS.add(item);
    }

    /** Blocks the client draws cut out (the poster's torn edges). */
    public static List<Block> cutoutBlocks() {
        return List.of(WANTED_POSTER);
    }
}
