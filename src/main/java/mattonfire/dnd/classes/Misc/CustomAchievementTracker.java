package mattonfire.dnd.classes.Misc;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;

public class CustomAchievementTracker {
    private static final String ACHIEVEMENT_TAG = "CustomAchievements";

    // Get saved achievements from the player's persistent data
    public static Set<String> getAchievements(ServerPlayerEntity player) {
        Set<String> achievements = new HashSet<>();
        NbtCompound playerData = getPlayerPersistentData(player);

        if (playerData.contains(ACHIEVEMENT_TAG)) {
            String[] savedAchievements = playerData.getString(ACHIEVEMENT_TAG).split(",");
            for (String achievement : savedAchievements) {
                if (!achievement.isEmpty()) {
                    achievements.add(achievement);
                }
            }
        }
        return achievements;
    }

    // Add a new achievement to the player's data
    public static void addAchievement(ServerPlayerEntity player, String achievement) {
        Set<String> achievements = getAchievements(player);
        if (achievements.add(achievement)) {
            saveAchievements(player, achievements);
        }
    }

    // Save updated achievements back to player's persistent data
    private static void saveAchievements(ServerPlayerEntity player, Set<String> achievements) {
        NbtCompound playerData = getPlayerPersistentData(player);
        playerData.putString(ACHIEVEMENT_TAG, String.join(",", achievements));
    }

    // Fetch or create the player's persistent NBT data
    private static NbtCompound getPlayerPersistentData(ServerPlayerEntity player) {
        NbtCompound persistentData = new NbtCompound();
        player.writeCustomDataToNbt(persistentData);
        return persistentData;
    }
}
