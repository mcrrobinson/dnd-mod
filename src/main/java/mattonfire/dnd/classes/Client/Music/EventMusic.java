package mattonfire.dnd.classes.Client.Music;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Music.DungeonMusic;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.mixin.MusicTrackerAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.MusicType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Picks the mod's music for game events, in priority order: dragon fight, low health, dungeon,
 * Nether Fortress, travelling, night.
 * Hooked into MinecraftClient.getMusicType() by MinecraftClientMusicMixin.
 */
public class EventMusic {
    public enum Event {
        NONE(null),
        // Starts straight away over whatever is playing, and loops while the fight lasts
        DRAGON_FIGHT(new MusicSound(entry(ModSounds.MUSIC_DRAGON_FIGHT), 0, 0, true)),
        // Like the fight music: straight away, looping until the player recovers or dies
        LOW_HEALTH(new MusicSound(entry(ModSounds.MUSIC_LOW_HEALTH), 0, 0, true)),
        DUNGEON(new MusicSound(entry(ModSounds.MUSIC_DUNGEON), 600, 2400, false)),
        NETHER_FORTRESS(new MusicSound(entry(ModSounds.MUSIC_NETHER_FORTRESS), 200, 1200, false)),
        TRAVEL(new MusicSound(entry(ModSounds.MUSIC_TRAVEL), 1200, 6000, false)),
        NIGHT(new MusicSound(entry(ModSounds.MUSIC_NIGHT), 1200, 6000, false));

        public final MusicSound music;

        Event(MusicSound music) {
            this.music = music;
        }
    }

    // Travelling: covered TRAVEL_START_DISTANCE blocks over the last TRAVEL_WINDOW_SECONDS,
    // and keeps counting until that drops below TRAVEL_STOP_DISTANCE.
    private static final int TRAVEL_WINDOW_SECONDS = 30;
    private static final double TRAVEL_START_DISTANCE = 80.0;
    private static final double TRAVEL_STOP_DISTANCE = 30.0;
    // A jump bigger than this in one second is a teleport, not travel
    private static final double TELEPORT_DISTANCE = 100.0;
    // Quiet gap after a dragon fight ends before other music can start
    private static final int AFTER_FIGHT_SILENCE_TICKS = 400;

    // Low health: starts below LOW_HEALTH_START of max health while in combat, and keeps going until
    // health is back above LOW_HEALTH_STOP, the player dies, or there's been no combat for a while
    private static final float LOW_HEALTH_START = 0.25F;
    private static final float LOW_HEALTH_STOP = 0.4F;
    // In combat: hurt in the last COMBAT_HURT_TICKS, or a monster within COMBAT_MONSTER_RANGE blocks
    private static final int COMBAT_HURT_TICKS = 200;
    private static final double COMBAT_MONSTER_RANGE = 12.0;
    private static final int LOW_HEALTH_OUT_OF_COMBAT_TICKS = 600;
    // Quiet gap after the low health music ends before other music can start
    private static final int AFTER_LOW_HEALTH_SILENCE_TICKS = 100;

    private static final Vec3d[] positions = new Vec3d[TRAVEL_WINDOW_SECONDS + 1];
    private static int positionCount = 0;
    private static int nextPosition = 0;
    private static World lastWorld = null;

    private static boolean inDungeon = false;
    private static boolean nearFortress = false;
    private static boolean travelling = false;
    private static boolean lowHealth = false;
    private static float lastHealth = -1;
    private static int ticksSinceHurt = Integer.MAX_VALUE;
    private static int ticksSinceCombat = Integer.MAX_VALUE;
    private static Event current = Event.NONE;

    private static RegistryEntry<SoundEvent> entry(SoundEvent sound) {
        return RegistryEntry.of(sound);
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DungeonMusic.S2C_IN_DUNGEON,
                (client, handler, buf, sender) -> {
                    boolean value = buf.readBoolean();
                    client.execute(() -> inDungeon = value);
                });
        ClientPlayNetworking.registerGlobalReceiver(DungeonMusic.S2C_NEAR_FORTRESS,
                (client, handler, buf, sender) -> {
                    boolean value = buf.readBoolean();
                    client.execute(() -> nearFortress = value);
                });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            inDungeon = false;
            nearFortress = false;
            resetLowHealth();
            resetTravel();
        });
        ClientTickEvents.END_CLIENT_TICK.register(EventMusic::tick);
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (player.world != lastWorld) {
            lastWorld = player.world;
            resetTravel();
        }
        if (player.age % 20 == 0) {
            sampleTravel(player.getPos());
        }
        updateLowHealth(player);

        Event previous = current;
        current = pick(client);
        if (current != previous) {
            DnDClasses.LOGGER.info("[EventMusic] {} -> {}", previous, current);
            // Fight music would otherwise play to the end of the track after the fight
            if (previous == Event.DRAGON_FIGHT && client.getMusicTracker().isPlayingType(previous.music)) {
                client.getMusicTracker().stop();
                ((MusicTrackerAccessor) client.getMusicTracker()).setTimeUntilNextSong(AFTER_FIGHT_SILENCE_TICKS);
            }
            // Same for the low health music once the player recovers or dies
            if (previous == Event.LOW_HEALTH && client.getMusicTracker().isPlayingType(previous.music)) {
                client.getMusicTracker().stop();
                ((MusicTrackerAccessor) client.getMusicTracker()).setTimeUntilNextSong(AFTER_LOW_HEALTH_SILENCE_TICKS);
            }
        }
    }

    private static Event pick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        // Wyvern boss bars set the dragon music flag; vanilla only uses it in the End
        if (player.world.getRegistryKey() != World.END && client.inGameHud.getBossBarHud().shouldPlayDragonMusic()) {
            return Event.DRAGON_FIGHT;
        }
        if (lowHealth) {
            return Event.LOW_HEALTH;
        }
        if (inDungeon) {
            return Event.DUNGEON;
        }
        if (nearFortress) {
            return Event.NETHER_FORTRESS;
        }
        if (player.world.getRegistryKey() != World.OVERWORLD) {
            return Event.NONE;
        }
        if (travelling) {
            return Event.TRAVEL;
        }
        long timeOfDay = player.world.getTimeOfDay() % 24000L;
        if (timeOfDay >= 13000L && timeOfDay < 23000L) {
            return Event.NIGHT;
        }
        return Event.NONE;
    }

    /** Music to play instead of the vanilla choice, or null to keep vanilla's. */
    public static MusicSound getMusic(MusicSound vanilla) {
        if (current == Event.NONE) {
            return null;
        }
        // Keep vanilla's underwater, menu, credits and End music, except during a fight
        boolean vanillaSpecial = vanilla == MusicType.UNDERWATER || vanilla == MusicType.MENU
                || vanilla == MusicType.CREDITS || vanilla == MusicType.END || vanilla == MusicType.DRAGON;
        if (vanillaSpecial && current != Event.DRAGON_FIGHT) {
            return null;
        }
        return current.music;
    }

    private static void updateLowHealth(ClientPlayerEntity player) {
        float health = player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) {
            ticksSinceHurt = 0;
        } else if (ticksSinceHurt < Integer.MAX_VALUE) {
            ticksSinceHurt++;
        }
        lastHealth = health;

        boolean monsterNearby = player.age % 10 == 0 ? isMonsterNearby(player) : ticksSinceCombat == 0;
        boolean inCombat = ticksSinceHurt < COMBAT_HURT_TICKS || monsterNearby;
        if (inCombat) {
            ticksSinceCombat = 0;
        } else if (ticksSinceCombat < Integer.MAX_VALUE) {
            ticksSinceCombat++;
        }

        if (player.isDead() || player.isCreative() || player.isSpectator()) {
            lowHealth = false;
            return;
        }
        float fraction = health / player.getMaxHealth();
        boolean was = lowHealth;
        lowHealth = lowHealth
                ? fraction < LOW_HEALTH_STOP && ticksSinceCombat < LOW_HEALTH_OUT_OF_COMBAT_TICKS
                : fraction < LOW_HEALTH_START && inCombat;
        if (lowHealth != was) {
            DnDClasses.LOGGER.info("[EventMusic] low health {} ({}/{})", lowHealth, health, player.getMaxHealth());
        }
    }

    private static boolean isMonsterNearby(ClientPlayerEntity player) {
        Box box = player.getBoundingBox().expand(COMBAT_MONSTER_RANGE);
        return !player.world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity instanceof Monster && entity.isAlive()).isEmpty();
    }

    private static void resetLowHealth() {
        lowHealth = false;
        lastHealth = -1;
        ticksSinceHurt = Integer.MAX_VALUE;
        ticksSinceCombat = Integer.MAX_VALUE;
    }

    private static void sampleTravel(Vec3d pos) {
        Vec3d last = positionCount == 0 ? null : positions[(nextPosition + positions.length - 1) % positions.length];
        if (last != null && horizontalDistance(last, pos) > TELEPORT_DISTANCE) {
            resetTravel();
        }
        positions[nextPosition] = pos;
        nextPosition = (nextPosition + 1) % positions.length;
        positionCount = Math.min(positionCount + 1, positions.length);
        if (positionCount < positions.length) {
            return;
        }
        // nextPosition now points at the oldest sample
        double distance = horizontalDistance(positions[nextPosition], pos);
        travelling = travelling ? distance >= TRAVEL_STOP_DISTANCE : distance >= TRAVEL_START_DISTANCE;
    }

    private static void resetTravel() {
        positionCount = 0;
        nextPosition = 0;
        travelling = false;
    }

    private static double horizontalDistance(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
