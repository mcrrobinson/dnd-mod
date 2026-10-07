package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent STEEL_ON_STEEL = registerSoundEvent("steel_on_steel");
    public static final SoundEvent AWAKE_CART = registerSoundEvent("awake_cart");
    public static final SoundEvent TOOTH_AND_CLAW = registerSoundEvent("tooth_and_claw");
    public static final SoundEvent SILENT_FOOTSTEPS = registerSoundEvent("silent_footsteps");

    public static final SoundEvent MUSIC_BOX = registerSoundEvent("music_box");

    // Event music pools (sounds.json); night and travel mix in vanilla game music
    public static final SoundEvent MUSIC_DRAGON_FIGHT = registerSoundEvent("music.dragon_fight");
    public static final SoundEvent MUSIC_DUNGEON = registerSoundEvent("music.dungeon");
    public static final SoundEvent MUSIC_NIGHT = registerSoundEvent("music.night");
    public static final SoundEvent MUSIC_TRAVEL = registerSoundEvent("music.travel");
    public static final SoundEvent MUSIC_LOW_HEALTH = registerSoundEvent("music.low_health");
    public static final SoundEvent MUSIC_NETHER_FORTRESS = registerSoundEvent("music.nether_fortress");

    // Short stings (4 s cuts of the tracks) played when a class special fires
    public static final SoundEvent STING_STEEL_ON_STEEL = registerSoundEvent("sting.steel_on_steel");
    public static final SoundEvent STING_TOOTH_AND_CLAW = registerSoundEvent("sting.tooth_and_claw");
    public static final SoundEvent STING_AWAKE_CART = registerSoundEvent("sting.awake_cart");
    public static final SoundEvent STING_SILENT_FOOTSTEPS = registerSoundEvent("sting.silent_footsteps");
    public static final SoundEvent STING_MUSIC_BOX = registerSoundEvent("sting.music_box");

    public static final SoundEvent WIZARD_EXPLOSION = registerSoundEvent("wizard_explosion");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        DnDClasses.LOGGER.info("Registering Mod Sounds for " + DnDClasses.MOD_ID);
    }
}