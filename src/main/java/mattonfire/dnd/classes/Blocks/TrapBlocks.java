package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

/** Dungeon trap blocks (see {@link TrapTriggerBlockEntity}). All unbreakable and drop nothing. */
public final class TrapBlocks {
    /** Scoreboard tag on a dart launcher's arrows (damage logs, tests). */
    public static final String DART_TAG = "dndclasses.trap_dart";

    private static FabricBlockSettings stone() {
        return FabricBlockSettings.of(Material.STONE).strength(-1.0F, 3600000.0F).dropsNothing()
                .sounds(BlockSoundGroup.STONE).allowsSpawning((state, world, pos, type) -> false);
    }

    public static final TrapTriggerBlock TRAP_TRIGGER = new TrapTriggerBlock(stone());
    public static final BlockEntityType<TrapTriggerBlockEntity> TRAP_TRIGGER_ENTITY = FabricBlockEntityTypeBuilder
            .create(TrapTriggerBlockEntity::new, TRAP_TRIGGER).build(null);
    public static final DartLauncherBlock DART_LAUNCHER = new DartLauncherBlock(stone());
    public static final FlameVentBlock FLAME_VENT = new FlameVentBlock(
            stone().luminance(state -> state.get(Properties.LIT) ? 13 : 0));
    public static final CrumblingFloorBlock CRUMBLING_FLOOR = new CrumblingFloorBlock(stone());

    private TrapBlocks() {
    }

    private static void block(String id, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, id), block);
    }

    public static void register() {
        block("trap_trigger", TRAP_TRIGGER);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "trap_trigger"),
                TRAP_TRIGGER_ENTITY);
        block("dart_launcher", DART_LAUNCHER);
        block("flame_vent", FLAME_VENT);
        block("crumbling_floor", CRUMBLING_FLOOR);
    }
}
