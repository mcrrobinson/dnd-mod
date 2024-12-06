package mattonfire.dnd.classes;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent STEEL_ON_STEEL = registerSoundEvent("steel_on_steel");
    public static final SoundEvent AWAKE_CART = registerSoundEvent("awake_cart");
    public static final SoundEvent TOOTH_AND_CLAW = registerSoundEvent("tooth_and_claw");
    public static final SoundEvent SILENT_FOOTSTEPS = registerSoundEvent("silent_footsteps");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        DnDClasses.LOGGER.info("Registering Mod Sounds for " + DnDClasses.MOD_ID);
    }
}