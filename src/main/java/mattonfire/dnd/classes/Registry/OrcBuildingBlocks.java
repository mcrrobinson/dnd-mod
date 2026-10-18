package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mattonfire.dnd.classes.Blocks.orc.StakeWallBlock;
import mattonfire.dnd.classes.Blocks.orc.TuskRackBlock;
import mattonfire.dnd.classes.Blocks.orc.WarDrumBlock;
import mattonfire.dnd.classes.Blocks.orc.WarTotemBlock;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Orc Strongholds' one-off building blocks (Building Blocks epic): the hide carpets and rug, the War Totem,
 * the Sharpened Stake Wall, the Tusk Rack and the War Drum. The hide, mud brick and slag materials are block
 * families in {@link ModBlockFamilies}. Resources come from {@code tools/orc_blocks.py}, textures from
 * {@code tools/orc_block_textures.py}. See docs/structures/orc-strongholds.md.
 */
public final class OrcBuildingBlocks {
    private static final List<Block> ALL = new ArrayList<>();

    public static final Block TANNED_HIDE_CARPET = add("tanned_hide_carpet",
            new CarpetBlock(FabricBlockSettings.copyOf(Blocks.BROWN_CARPET)));
    public static final Block PALE_TANNED_HIDE_CARPET = add("pale_tanned_hide_carpet",
            new CarpetBlock(FabricBlockSettings.copyOf(Blocks.WHITE_CARPET)));
    public static final Block GREY_TANNED_HIDE_CARPET = add("grey_tanned_hide_carpet",
            new CarpetBlock(FabricBlockSettings.copyOf(Blocks.LIGHT_GRAY_CARPET)));
    /** A striped fur rug, carpet-height. */
    public static final Block HIDE_RUG = add("hide_rug", new CarpetBlock(FabricBlockSettings.copyOf(Blocks.BROWN_CARPET)));
    public static final Block WAR_TOTEM = add("war_totem", new WarTotemBlock(log()));
    public static final Block SHARPENED_STAKE_WALL = add("sharpened_stake_wall",
            new StakeWallBlock(log().nonOpaque()));
    public static final Block TUSK_RACK = add("tusk_rack", new TuskRackBlock(FabricBlockSettings.copyOf(Blocks.DARK_OAK_PLANKS)
            .strength(1.0F).sounds(BlockSoundGroup.BONE).noCollision().nonOpaque()));
    public static final Block WAR_DRUM = add("war_drum", new WarDrumBlock(FabricBlockSettings.copyOf(Blocks.BARREL)));

    private OrcBuildingBlocks() {
    }

    /**
     * A dark oak log's settings (hardness, axe, wood sound) with a fixed map colour: copying the log itself would
     * copy its colour-by-axis function, which fails on blocks without an axis.
     */
    private static FabricBlockSettings log() {
        return FabricBlockSettings.copyOf(Blocks.DARK_OAK_PLANKS).strength(2.0F).mapColor(MapColor.BROWN);
    }

    private static Block add(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
        ALL.add(block);
        return block;
    }

    /** The blocks in creative-tab order. */
    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Blocks drawn cut out on the client (see-through gaps). */
    public static Block[] cutoutBlocks() {
        return new Block[] { SHARPENED_STAKE_WALL, TUSK_RACK };
    }

    /** Loads the class, which registers the blocks. Call once, after {@link ModBlockFamilies#register()}. */
    public static void register() {
        DnDClasses.LOGGER.debug("[OrcBuildingBlocks] {} blocks", ALL.size());
    }
}
