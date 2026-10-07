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
    public static final SoundEvent MUSIC_LICH_FIGHT = registerSoundEvent("music.lich_fight");

    // Short synthesized stings (tools/music-gen) played when a class special fires, one per class
    public static final SoundEvent STING_BARBARIAN = registerSoundEvent("sting.barbarian");
    public static final SoundEvent STING_BARD = registerSoundEvent("sting.bard");
    public static final SoundEvent STING_CLERIC = registerSoundEvent("sting.cleric");
    public static final SoundEvent STING_DRUID = registerSoundEvent("sting.druid");
    public static final SoundEvent STING_FIGHTER = registerSoundEvent("sting.fighter");
    public static final SoundEvent STING_MONK = registerSoundEvent("sting.monk");
    public static final SoundEvent STING_PALADIN = registerSoundEvent("sting.paladin");
    public static final SoundEvent STING_RANGER = registerSoundEvent("sting.ranger");
    public static final SoundEvent STING_ROGUE = registerSoundEvent("sting.rogue");
    public static final SoundEvent STING_NECROMANCER = registerSoundEvent("sting.necromancer");
    public static final SoundEvent STING_WARLOCK = registerSoundEvent("sting.warlock");
    public static final SoundEvent STING_WIZARD = registerSoundEvent("sting.wizard");
    public static final SoundEvent STING_ARTIFICER = registerSoundEvent("sting.artificer");
    public static final SoundEvent STING_BLOODHUNTER = registerSoundEvent("sting.bloodhunter");
    public static final SoundEvent STING_ALCHEMIST = registerSoundEvent("sting.alchemist");

    public static final SoundEvent WIZARD_EXPLOSION = registerSoundEvent("wizard_explosion");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        DnDClasses.LOGGER.info("Registering Mod Sounds for " + DnDClasses.MOD_ID);
    }
}