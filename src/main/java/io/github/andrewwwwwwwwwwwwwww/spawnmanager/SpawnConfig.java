package io.github.andrewwwwwwwwwwwwwww.spawnmanager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class SpawnConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnManager");
    private static final com.google.gson.Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static int protectionRadius = 32;

    public static void load() {
        Path configPath = configPath();
        if (Files.exists(configPath)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
                if (json.has("protectionRadius")) {
                    protectionRadius = json.get("protectionRadius").getAsInt();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load config", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        Path configPath = configPath();
        JsonObject json = new JsonObject();
        json.addProperty("protectionRadius", protectionRadius);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(json));
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("spawnmanager.json");
    }
}
