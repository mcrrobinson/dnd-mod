package mattonfire.dnd.classes;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
        public static final Item STAFF_OF_ICE = registerItem("staff_of_ice",
                        new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item STAFF_OF_FIRE = registerItem("staff_of_fire",
                        new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item STAFF_OF_LIGHTNING = registerItem("staff_of_lightning",
                        new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item MONK_STAFF = registerItem("monk_staff",
                        new MonkStaff(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item MUSIC_DISC_STEEL_ON_STEEL = registerItem("music_disc_steel_on_steel",
                        new MusicDiscItem(7, ModSounds.STEEL_ON_STEEL, new FabricItemSettings().maxCount(1), 98));
        public static final Item MUSIC_DISC_AWAKE_CART = registerItem("music_disc_awake_cart",
                        new MusicDiscItem(7, ModSounds.AWAKE_CART, new FabricItemSettings().maxCount(1), 88));
        public static final Item MUSIC_DISC_TOOTH_AND_CLAW = registerItem("music_disc_tooth_and_claw",
                        new MusicDiscItem(7, ModSounds.TOOTH_AND_CLAW, new FabricItemSettings().maxCount(1), 105));
        public static final Item MUSIC_DISC_SILENT_FOOTSTEPS = registerItem("music_disc_silent_footsteps",
                        new MusicDiscItem(7, ModSounds.SILENT_FOOTSTEPS, new FabricItemSettings().maxCount(1), 170));

        private static Item registerItem(String name, Item item) {
                return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), item);
        }

        public static void registerModItems() {
                DnDClasses.LOGGER.info("Registering Mod Items for " + DnDClasses.MOD_ID);
        }
}