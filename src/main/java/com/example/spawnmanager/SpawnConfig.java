package com.example.spawnmanager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class SpawnConfig {
    private static final com.google.gson.Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static int protectionRadius = 32;

    static void load() {
        Path configPath = configPath();
        if (Files.exists(configPath)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
                if (json.has("protectionRadius")) {
                    protectionRadius = json.get("protectionRadius").getAsInt();
                }
            } catch (Exception e) {
                System.err.println("[SpawnManager] Failed to load config: " + e.getMessage());
            }
        } else {
            save();
        }
    }

    static void save() {
        Path configPath = configPath();
        JsonObject json = new JsonObject();
        json.addProperty("protectionRadius", protectionRadius);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(json));
        } catch (Exception e) {
            System.err.println("[SpawnManager] Failed to save config: " + e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("spawnmanager.json");
    }
}
