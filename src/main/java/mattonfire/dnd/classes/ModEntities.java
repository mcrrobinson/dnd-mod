package mattonfire.dnd.classes;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static BlockEntityType<FastBrewingStandBlockEntity> FAST_BREWING_STAND;

    public static void registerBlockEntities() {
        FAST_BREWING_STAND = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(DnDClasses.MOD_ID, "fast_brewing_stand_entity"),
                FabricBlockEntityTypeBuilder.create(
                        FastBrewingStandBlockEntity::new,
                        ModBlocks.FAST_BREWING_STAND_BLOCK).build(null));
    }
}
