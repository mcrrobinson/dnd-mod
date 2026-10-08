package mattonfire.dnd.classes.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public final class FAConfig {

    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve(DnDClasses.MOD_ID).resolve(DnDClasses.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigValues values;

    static {
        loadDefaults();
    }

    private FAConfig() {
    }

    public static void save() {
        File configFile = CONFIG_FILE_PATH.toFile();
        File parentFile = configFile.getParentFile();

        if(parentFile == null) {
            return;
        }

        if(!parentFile.mkdirs() && !parentFile.isDirectory()) {
            DnDClasses.LOGGER.warn("Failed to save config: Directory could not be created.");
            return;
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(values, writer);
        } catch (IOException e) {
            DnDClasses.LOGGER.warn("Failed to save config: ", e);
        }
    }

    public static void load() {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH.toFile())) {
            values = GSON.fromJson(reader, ConfigValues.class);
        } catch (IOException | JsonParseException e) {
            DnDClasses.LOGGER.warn("Failed to read config (default parameters will be used): ", e);
            loadDefaults();
        }
        if (values == null) { // Empty file
            DnDClasses.LOGGER.warn("Config file is empty, default parameters will be used");
            loadDefaults();
        }
    }

    public static void loadDefaults() {
        values = new ConfigValues(
                true,
                true,
                true,
                250
        );
    }

    public static boolean exists() {
        return CONFIG_FILE_PATH.toFile().exists();
    }

    public static ConfigValues getValues() {
        return values;
    }
}