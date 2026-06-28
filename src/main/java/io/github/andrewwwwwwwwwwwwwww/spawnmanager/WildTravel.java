package io.github.andrewwwwwwwwwwwwwww.spawnmanager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /wild}: scatter a player to a random safe spot inside the Overworld world border.
 * Moderators can also place adjustable-size "wild portal" regions ({@code /wild place <size> [height]})
 * that trigger the same teleport when a player steps inside — drop one into the spawn build.
 */
public final class WildTravel {
    private WildTravel() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnManager");
    private static final com.google.gson.Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_TRIES = 32;
    private static final int DEFAULT_SIZE = 3;
    private static final int DEFAULT_HEIGHT = 3;

    /** An axis-aligned box (inclusive) that teleports any player standing inside it. */
    public record WildPortal(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX
                && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ;
        }
        int centerX() { return (minX + maxX) / 2; }
        int centerZ() { return (minZ + maxZ) / 2; }
        String describe() {
            int w = maxX - minX + 1, h = maxY - minY + 1, d = maxZ - minZ + 1;
            return "centre " + centerX() + ", " + minY + ", " + centerZ() + " (" + w + "x" + h + "x" + d + ")";
        }
    }

    private static final List<WildPortal> portals = new ArrayList<>();
    /** /wild command cooldown only (gametick when each player may use it again). Portals are exempt. */
    private static final Map<UUID, Long> commandCooldown = new HashMap<>();
    private static Path savePath = null;
    /** Per-world toggle for ALL wild travel (the /wild command AND wild portals). Persisted in wildportals.json. */
    private static boolean wildEnabled = true;

    // ---- lifecycle ----

    public static void load(MinecraftServer server) {
        portals.clear();
        commandCooldown.clear();
        wildEnabled = true;
        savePath = server.getWorldPath(LevelResource.ROOT).resolve("spawnmanager").resolve("wildportals.json");
        if (!Files.exists(savePath)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(savePath)).getAsJsonObject();
            if (json.has("wildEnabled")) wildEnabled = json.get("wildEnabled").getAsBoolean();
            if (json.has("portals")) {
                for (JsonElement e : json.getAsJsonArray("portals")) {
                    JsonObject o = e.getAsJsonObject();
                    if (o.has("minX")) {
                        portals.add(new WildPortal(o.get("minX").getAsInt(), o.get("minY").getAsInt(), o.get("minZ").getAsInt(),
                                o.get("maxX").getAsInt(), o.get("maxY").getAsInt(), o.get("maxZ").getAsInt()));
                    } else if (o.has("x")) { // legacy single-block portal → 1-wide, 3-tall region
                        int x = o.get("x").getAsInt(), y = o.get("y").getAsInt(), z = o.get("z").getAsInt();
                        portals.add(new WildPortal(x, y, z, x, y + 2, z));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load wild portals", e);
        }
    }

    public static void save() {
        if (savePath == null) return;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("wildEnabled", wildEnabled);
            JsonArray arr = new JsonArray();
            for (WildPortal p : portals) {
                JsonObject o = new JsonObject();
                o.addProperty("minX", p.minX()); o.addProperty("minY", p.minY()); o.addProperty("minZ", p.minZ());
                o.addProperty("maxX", p.maxX()); o.addProperty("maxY", p.maxY()); o.addProperty("maxZ", p.maxZ());
                arr.add(o);
            }
            json.add("portals", arr);
            Files.createDirectories(savePath.getParent());
            Files.writeString(savePath, GSON.toJson(json));
        } catch (Exception e) {
            LOGGER.error("Failed to save wild portals", e);
        }
    }

    // ---- per-tick portal detection ----

    public static void tick(MinecraftServer server) {
        if (!wildEnabled || portals.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            if (!level.dimension().equals(Level.OVERWORLD)) continue;
            // Portals have no cooldown — but the teleport itself moves the player out of the
            // region the same tick, so it won't re-fire while they stand still.
            if (inAnyPortal(player.blockPosition())) teleport(player);
        }
    }

    private static boolean inAnyPortal(BlockPos p) {
        for (WildPortal portal : portals) if (portal.contains(p)) return true;
        return false;
    }

    // ---- the teleport ----

    /** Scatter the player to a random safe spot within the Overworld border. */
    public static boolean teleport(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.literal("/wild only works in the Overworld.").withStyle(ChatFormatting.RED));
            return false;
        }

        WorldBorder border = level.getWorldBorder();
        RandomSource rng = level.getRandom();
        double cx = border.getCenterX();
        double cz = border.getCenterZ();
        double half = border.getSize() / 2.0 - 8.0;
        double radius = SpawnConfig.wildRadius > 0 ? Math.min(half, SpawnConfig.wildRadius) : half;
        if (radius < 1) radius = Math.max(1, half);

        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            int bx = (int) Math.floor(cx + (rng.nextDouble() * 2 - 1) * radius);
            int bz = (int) Math.floor(cz + (rng.nextDouble() * 2 - 1) * radius);
            if (!border.isWithinBounds(bx, bz)) continue;
            if (SpawnProtection.isProtected(level, bx, bz)) continue; // don't land back inside spawn

            level.getChunk(bx >> 4, bz >> 4); // force-generate the column
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            BlockPos ground = new BlockPos(bx, y - 1, bz);
            BlockState gs = level.getBlockState(ground);
            if (gs.isAir()) continue;                              // void / no floor
            if (gs.getFluidState().is(Fluids.LAVA)) continue;      // no lava landings

            player.teleportTo(level, bx + 0.5, y, bz + 0.5, java.util.Set.of(),
                    player.getYRot(), player.getXRot(), true);
            player.sendSystemMessage(Component.literal("Whoosh! Off into the wild — "
                    + bx + ", " + y + ", " + bz).withStyle(ChatFormatting.GREEN));
            return true;
        }
        player.sendSystemMessage(Component.literal("Couldn't find a safe spot — try again.").withStyle(ChatFormatting.RED));
        return false;
    }

    // ---- commands ----

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wild")
                .executes(ctx -> runWildCommand(ctx.getSource()))
                .then(Commands.literal("place")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> placePortal(ctx.getSource(), DEFAULT_SIZE, DEFAULT_HEIGHT))
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> placePortal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "size"), DEFAULT_HEIGHT))
                                .then(Commands.argument("height", IntegerArgumentType.integer(1, 32))
                                        .executes(ctx -> placePortal(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "size"),
                                                IntegerArgumentType.getInteger(ctx, "height"))))))
                .then(Commands.literal("remove")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> removePortal(ctx.getSource())))
                .then(Commands.literal("list")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> listPortals(ctx.getSource())))
                .then(Commands.literal("enable")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("disable")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> setEnabled(ctx.getSource(), false)))
        );
    }

    /** Op toggle: enable/disable wild travel on this world (command + portals) and persist it. */
    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        wildEnabled = enabled;
        save();
        source.sendSuccess(() -> Component.literal(
                "Wild travel " + (enabled ? "enabled" : "disabled") + " on this world.")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }

    /** /wild from the command: enforce the per-player cooldown (ops bypass), then teleport. */
    private static int runWildCommand(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer p)) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        if (!wildEnabled) {
            p.sendSystemMessage(Component.literal("Wild travel is disabled on this world.").withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean op = Commands.LEVEL_GAMEMASTERS.check(p.permissions());
        long now = clock(p);
        if (!op) {
            Long until = commandCooldown.get(p.getUUID());
            if (until != null && now < until) {
                long secsLeft = Math.max(1, (until - now) / 20L);
                p.sendSystemMessage(Component.literal("You must wait " + formatTime(secsLeft)
                        + " before using /wild again.").withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        boolean ok = teleport(p);
        if (ok && !op && SpawnConfig.wildCooldownSeconds > 0) {
            commandCooldown.put(p.getUUID(), now + SpawnConfig.wildCooldownSeconds * 20L);
        }
        return ok ? 1 : 0;
    }

    private static long clock(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        return server != null ? server.overworld().getGameTime() : 0L;
    }

    private static String formatTime(long seconds) {
        long m = seconds / 60, s = seconds % 60;
        return m > 0 ? (m + "m " + s + "s") : (s + "s");
    }

    private static int placePortal(CommandSourceStack source, int size, int height) {
        if (!(source.getEntity() instanceof ServerPlayer p)) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        if (!p.level().dimension().equals(Level.OVERWORLD)) {
            source.sendFailure(Component.literal("Wild portals can only be placed in the Overworld."));
            return 0;
        }
        BlockPos f = p.blockPosition();
        int halfLow = (size - 1) / 2;       // centre the footprint on the player
        int minX = f.getX() - halfLow, maxX = minX + size - 1;
        int minZ = f.getZ() - halfLow, maxZ = minZ + size - 1;
        int minY = f.getY(), maxY = f.getY() + height - 1;
        WildPortal portal = new WildPortal(minX, minY, minZ, maxX, maxY, maxZ);
        portals.add(portal);
        save();
        source.sendSuccess(() -> Component.literal("Wild portal placed — " + portal.describe()
                + ". Players who step inside are sent into the wild.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removePortal(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer p)) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        BlockPos feet = p.blockPosition();
        WildPortal best = null;
        double bestD = Double.MAX_VALUE;
        for (WildPortal portal : portals) {
            if (portal.contains(feet)) { best = portal; break; } // standing in one → remove it
            double dx = portal.centerX() - feet.getX(), dz = portal.centerZ() - feet.getZ();
            double d = dx * dx + dz * dz;
            if (d <= 25.0 && d < bestD) { bestD = d; best = portal; }
        }
        if (best == null) {
            source.sendFailure(Component.literal("No wild portal where you're standing (or within 5 blocks)."));
            return 0;
        }
        portals.remove(best);
        save();
        WildPortal removed = best;
        source.sendSuccess(() -> Component.literal("Removed wild portal — " + removed.describe())
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int listPortals(CommandSourceStack source) {
        if (portals.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No wild portals placed.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Wild portals (" + portals.size() + "):")
                .withStyle(ChatFormatting.AQUA), false);
        for (WildPortal p : portals) {
            source.sendSuccess(() -> Component.literal(" • " + p.describe()).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
