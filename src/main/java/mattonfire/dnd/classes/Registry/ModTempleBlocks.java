package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.Temple.AncestorReliefBlock;
import mattonfire.dnd.classes.Blocks.Temple.IncenseBrazierBlock;
import mattonfire.dnd.classes.Blocks.Temple.PaperLanternBlock;
import mattonfire.dnd.classes.Blocks.Temple.PrayerWheelBlock;
import mattonfire.dnd.classes.Blocks.Temple.SparringPostBlock;
import mattonfire.dnd.classes.Race.DragonAncestry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Dragon Temple's one-off building blocks (the Building Blocks epic, docs/structures/dragon-temples.md):
 * the Ancestor Relief, Prayer Wheel, the three Incense Braziers, the Paper Lantern and the Sparring Post. The
 * temple's materials with shapes (Dragonscale Tiles, Temple Lacquer Planks, Breath-Scorched Stone) are
 * {@link ModBlockFamilies}. Resources come from {@code tools/temple_blocks.py}.
 */
public final class ModTempleBlocks {
    private static final List<Block> TAB = new ArrayList<>();

    public static final AncestorReliefBlock ANCESTOR_RELIEF = new AncestorReliefBlock(
            FabricBlockSettings.copyOf(Blocks.CHISELED_DEEPSLATE));

    public static final PrayerWheelBlock PRAYER_WHEEL = new PrayerWheelBlock(
            FabricBlockSettings.of(Material.METAL, MapColor.RED).strength(2.0F, 6.0F).requiresTool()
                    .sounds(BlockSoundGroup.COPPER).nonOpaque());

    public static final IncenseBrazierBlock EMBER_INCENSE_BRAZIER = brazier(0xE8502A);
    public static final IncenseBrazierBlock FROST_INCENSE_BRAZIER = brazier(0xA8DCFF);
    public static final IncenseBrazierBlock STORM_INCENSE_BRAZIER = brazier(0x2EC4C0);

    public static final PaperLanternBlock PAPER_LANTERN = new PaperLanternBlock(
            FabricBlockSettings.of(Material.WOOL, MapColor.PALE_YELLOW).strength(0.5F).luminance(state -> 14)
                    .sounds(BlockSoundGroup.BAMBOO_WOOD).nonOpaque());

    public static final SparringPostBlock SPARRING_POST = new SparringPostBlock(
            FabricBlockSettings.copyOf(Blocks.HAY_BLOCK).strength(1.5F).sounds(BlockSoundGroup.WOOD));

    private ModTempleBlocks() {
    }

    private static IncenseBrazierBlock brazier(int smoke) {
        return new IncenseBrazierBlock(smoke, FabricBlockSettings.of(Material.METAL, MapColor.BROWN)
                .strength(3.5F).requiresTool().luminance(state -> 13).sounds(BlockSoundGroup.LANTERN).nonOpaque());
    }

    /** The incense brazier for an ancestry (Ember for NONE). */
    public static Block brazier(DragonAncestry kind) {
        return switch (kind) {
            case FROST -> FROST_INCENSE_BRAZIER;
            case STORM -> STORM_INCENSE_BRAZIER;
            default -> EMBER_INCENSE_BRAZIER;
        };
    }

    /** These blocks in creative-tab order. */
    public static List<Block> tabBlocks() {
        return Collections.unmodifiableList(TAB);
    }

    public static void register() {
        add("ancestor_relief", ANCESTOR_RELIEF);
        add("prayer_wheel", PRAYER_WHEEL);
        add("ember_incense_brazier", EMBER_INCENSE_BRAZIER);
        add("frost_incense_brazier", FROST_INCENSE_BRAZIER);
        add("storm_incense_brazier", STORM_INCENSE_BRAZIER);
        add("paper_lantern", PAPER_LANTERN);
        add("sparring_post", SPARRING_POST);
        SparringPostBlock.register();
    }

    private static void add(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
        TAB.add(block);
    }
}
