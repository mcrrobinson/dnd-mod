package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Enchantments.FeatherfallEnchantment;
import mattonfire.dnd.classes.Enchantments.LungeEnchantment;
import mattonfire.dnd.classes.Enchantments.ReturningEnchantment;
import mattonfire.dnd.classes.Enchantments.VampiricEnchantment;
import mattonfire.dnd.classes.Enchantments.SmiteDragonsEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEnchantments {

        public static final Enchantment LUNGE_ENCHANTMENT = registerEnchantment("lunge",
                        new LungeEnchantment(Enchantment.Rarity.COMMON, EnchantmentTarget.WEAPON));

        public static final Enchantment GRID_MINER_ENCHANTMENT = registerEnchantment("grid_miner",
                        new LungeEnchantment(Enchantment.Rarity.COMMON, EnchantmentTarget.DIGGER));

        public static final Enchantment TREE_FELLER_ENCHANTMENT = registerEnchantment("tree_feller",
                        new LungeEnchantment(Enchantment.Rarity.COMMON, EnchantmentTarget.WEAPON));

        public static final Enchantment INVULNERABILITY_ENCHANTMENT = registerEnchantment("invulnerability",
                        new LungeEnchantment(Enchantment.Rarity.COMMON, EnchantmentTarget.WEAPON));

        public static final Enchantment RETURNING_ENCHANTMENT = registerEnchantment("returning",
                        new ReturningEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND));

        public static final Enchantment VAMPIRIC_ENCHANTMENT = registerEnchantment("vampiric",
                        new VampiricEnchantment(Enchantment.Rarity.RARE, EnchantmentTarget.WEAPON,
                                        EquipmentSlot.MAINHAND));

        public static final Enchantment SMITE_DRAGONS_ENCHANTMENT = registerEnchantment("smite_dragons",
                        new SmiteDragonsEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentTarget.WEAPON,
                                        EquipmentSlot.MAINHAND));

        public static final Enchantment FEATHERFALL_ENCHANTMENT = registerEnchantment("featherfall",
                        new FeatherfallEnchantment(Enchantment.Rarity.UNCOMMON));

        private static Enchantment registerEnchantment(String name, Enchantment enchantment) {
                return Registry.register(Registries.ENCHANTMENT, new Identifier(DnDClasses.MOD_ID, name), enchantment);
        }

        public static void registerEnchantments() {
                DnDClasses.LOGGER.info("Registering Mod Enchantments for " + DnDClasses.MOD_ID);
                mattonfire.dnd.classes.Featherfall.register();
        }
}
