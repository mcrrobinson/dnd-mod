package mattonfire.dnd.classes;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModPotions {

    public static final RegistryEntry<Potion> FREEZE_POTION = registerPotion("freeze",
            new Potion(new StatusEffectInstance(ModEffects.FREEZE, 3600)));

    private static RegistryEntry<Potion> registerPotion(String name, Potion potion) {
        return Registry.registerReference(Registries.POTION, Identifier.of(DnDClasses.MOD_ID, name), potion);
    };

    public static void registerPotions() {
        DnDClasses.LOGGER.info("Registering Mod Potions for " + DnDClasses.MOD_ID);
    }
}
