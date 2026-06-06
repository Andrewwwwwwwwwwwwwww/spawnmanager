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
    /** Max distance from the world-border centre that /wild scatters players. 0 = full border. */
    public static int wildRadius = 10000;
    /** Cooldown (seconds) between uses of the /wild command per player. Portals are exempt. Ops bypass. */
    public static int wildCooldownSeconds = 900; // 15 minutes

    public static void load() {
        Path configPath = configPath();
        if (Files.exists(configPath)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
                if (json.has("protectionRadius")) {
                    protectionRadius = json.get("protectionRadius").getAsInt();
                }
                if (json.has("wildRadius")) {
                    wildRadius = json.get("wildRadius").getAsInt();
                }
                if (json.has("wildCooldownSeconds")) {
                    wildCooldownSeconds = json.get("wildCooldownSeconds").getAsInt();
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
        json.addProperty("wildRadius", wildRadius);
        json.addProperty("wildCooldownSeconds", wildCooldownSeconds);
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
