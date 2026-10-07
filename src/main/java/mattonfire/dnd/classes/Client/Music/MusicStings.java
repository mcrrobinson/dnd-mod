package mattonfire.dnd.classes.Client.Music;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/**
 * Plays a short musical sting when the player's class special fires, and ducks the background music
 * under it (SoundSystemMusicDuckMixin) so the track carries on quietly instead of being cut off.
 */
public class MusicStings {
    // The sting files are 4 s long (fade out over the last 1.5 s)
    private static final int STING_TICKS = 80;
    private static final float DUCKED_VOLUME = 0.3F;
    private static final float DUCK_DOWN_PER_TICK = 0.14F; // ~5 ticks to duck
    private static final float DUCK_UP_PER_TICK = 0.035F; // ~1 s to come back

    private static int duckTicksLeft = 0;
    private static float duckFactor = 1.0F;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MusicStings::tick);
    }

    /** Called when the server confirms the player's special fired. */
    public static void onSpecialFired(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        DndCharacter dndClass = ((PlayerEntityExt) client.player).getDndClass();
        SoundEvent sting = stingFor(dndClass);
        if (sting == null) {
            return;
        }
        DnDClasses.LOGGER.info("[EventMusic] sting {} for {}", sting.getId(), dndClass);
        client.getSoundManager().play(new StingSoundInstance(sting));
        duckTicksLeft = STING_TICKS;
    }

    private static SoundEvent stingFor(DndCharacter dndClass) {
        if (dndClass == null) {
            return null;
        }
        return switch (dndClass) {
            case FIGHTER, PALADIN, ARTIFICER -> ModSounds.STING_STEEL_ON_STEEL;
            case BARBARIAN, WARLOCK, WIZARD, DRUID -> ModSounds.STING_TOOTH_AND_CLAW;
            case RANGER, MONK -> ModSounds.STING_AWAKE_CART;
            case ROGUE, NECROMANCER, BLOODHUNTER -> ModSounds.STING_SILENT_FOOTSTEPS;
            case BARD, CLERIC, ALCHEMIST -> ModSounds.STING_MUSIC_BOX;
            default -> null;
        };
    }

    private static void tick(MinecraftClient client) {
        if (duckTicksLeft > 0) {
            duckTicksLeft--;
        }
        float target = duckTicksLeft > 0 ? DUCKED_VOLUME : 1.0F;
        if (duckFactor == target) {
            return;
        }
        duckFactor = duckFactor > target
                ? Math.max(target, duckFactor - DUCK_DOWN_PER_TICK)
                : Math.min(target, duckFactor + DUCK_UP_PER_TICK);
        // Re-applies every music source's volume, which goes through the duck mixin
        client.getSoundManager().updateSoundVolume(SoundCategory.MUSIC,
                client.options.getSoundVolume(SoundCategory.MUSIC));
    }

    /** How loud the background music plays right now (1 = normal). */
    public static float getDuckFactor() {
        return duckFactor;
    }

    public static boolean isSting(SoundInstance sound) {
        return sound instanceof StingSoundInstance;
    }

    /** Non-positional, in the music category so the music slider controls it. */
    private static class StingSoundInstance extends AbstractSoundInstance {
        StingSoundInstance(SoundEvent sound) {
            super(sound, SoundCategory.MUSIC, SoundInstance.createRandom());
            this.volume = 1.0F;
            this.relative = true;
            this.attenuationType = AttenuationType.NONE;
        }
    }
}
