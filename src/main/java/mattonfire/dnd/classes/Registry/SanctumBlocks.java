package mattonfire.dnd.classes.Registry;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.CarvedPanelBlock;
import mattonfire.dnd.classes.Blocks.CenserBlock;
import mattonfire.dnd.classes.Blocks.ContractLecternBlock;
import mattonfire.dnd.classes.Blocks.SoulfireSconceBlock;
import mattonfire.dnd.classes.Blocks.SulphurVentBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Brimstone Sanctum's one-off building blocks (Building Blocks epic); its materials are block families in
 * {@link ModBlockFamilies}. Listed in the DnD Building Blocks tab after the families. Resources are hand-written,
 * textures from {@code tools/brimstone_block_textures.py}. See docs/structures/brimstone-sanctums.md.
 */
public final class SanctumBlocks {
    /** Polished blackstone engraved with a glowing summoning circle: the Pact Hall altar and the floor before it. */
    public static final Block PACT_SIGIL_TILE = new Block(FabricBlockSettings.copyOf(Blocks.POLISHED_BLACKSTONE)
            .luminance(state -> 6));

    /** Blackstone carved with the Covenant's laws in burning script, facing out. */
    public static final CarvedPanelBlock INFERNAL_SCRIPT_PANEL = new CarvedPanelBlock(
            FabricBlockSettings.copyOf(Blocks.POLISHED_BLACKSTONE).luminance(state -> 4));

    /** Right-click reads the Covenant's law. */
    public static final ContractLecternBlock CONTRACT_LECTERN = new ContractLecternBlock(
            FabricBlockSettings.copyOf(Blocks.POLISHED_BLACKSTONE).nonOpaque());

    /** Brimstone breathing yellow fumes. */
    public static final SulphurVentBlock SULPHUR_VENT = new SulphurVentBlock(FabricBlockSettings.copyOf(Blocks.BASALT)
            .luminance(state -> 3));

    /** A wall-mounted soul flame on a black iron bracket (light 10, like a soul torch). */
    public static final SoulfireSconceBlock SOULFIRE_SCONCE = new SoulfireSconceBlock(
            FabricBlockSettings.of(Material.DECORATION).noCollision().strength(0.5F).sounds(BlockSoundGroup.LANTERN)
                    .luminance(state -> 10));

    /** A brass censer of embers on a chain (light 12). */
    public static final CenserBlock CHAIN_HUNG_CENSER = new CenserBlock(FabricBlockSettings.copyOf(Blocks.LANTERN)
            .luminance(state -> 12));

    private SanctumBlocks() {
    }

    /** In creative-tab order. */
    public static List<Block> all() {
        return List.of(PACT_SIGIL_TILE, INFERNAL_SCRIPT_PANEL, CONTRACT_LECTERN, SULPHUR_VENT, SOULFIRE_SCONCE,
                CHAIN_HUNG_CENSER);
    }

    /** Drawn cut out: the sconce's flame and the censer's chain have see-through pixels. */
    public static List<Block> cutoutBlocks() {
        return List.of(SOULFIRE_SCONCE, CHAIN_HUNG_CENSER);
    }

    public static void register() {
        register("pact_sigil_tile", PACT_SIGIL_TILE);
        register("infernal_script_panel", INFERNAL_SCRIPT_PANEL);
        register("contract_lectern", CONTRACT_LECTERN);
        register("sulphur_vent", SULPHUR_VENT);
        register("soulfire_sconce", SOULFIRE_SCONCE);
        register("chain_hung_censer", CHAIN_HUNG_CENSER);
    }

    private static void register(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
