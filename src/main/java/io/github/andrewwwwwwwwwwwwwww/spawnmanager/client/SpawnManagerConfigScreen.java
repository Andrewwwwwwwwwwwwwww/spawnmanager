package io.github.andrewwwwwwwwwwwwwww.spawnmanager.client;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mod Menu config screen. Spawn Manager is a server-side mod, so on a multiplayer server the
 * client can't change the server's settings — there we just point the user at the server config
 * file and the op commands. In singleplayer (or LAN host) the screen edits the local
 * {@code config/spawnmanager.json} directly.
 */
public class SpawnManagerConfigScreen extends Screen {
    private final Screen parent;
    private final boolean remoteServer;

    private EditBox radiusField;
    private EditBox wildRadiusField;
    private EditBox cooldownField;
    private boolean wildEnabledLocal;
    private boolean spawnEnabledLocal;

    public SpawnManagerConfigScreen(Screen parent) {
        super(Component.literal("Spawn Manager"));
        this.parent = parent;
        this.remoteServer = Minecraft.getInstance().getCurrentServer() != null;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        addRenderableWidget(new StringWidget(0, 18, this.width, 12, this.title, this.font));

        if (remoteServer) {
            initServerInfo(cx);
            return;
        }
        initEditable(cx);
    }

    /** Multiplayer: the server owns the settings — tell the user how to change them server-side. */
    private void initServerInfo(int cx) {
        addRenderableWidget(new StringWidget(0, 52, this.width, 12,
            Component.literal("These settings are managed by the server.")
                .withStyle(ChatFormatting.GRAY), this.font));
        addRenderableWidget(new StringWidget(0, 66, this.width, 12,
            Component.literal("Edit config/spawnmanager.json on the server, or use op commands:")
                .withStyle(ChatFormatting.GRAY), this.font));

        String[] commands = {
            "/spawnmanager setexactspawn <pos>",
            "/spawnmanager setspawnradius <radius>",
            "/spawnmanager setwildradius <radius>",
            "/spawnmanager setwildcooldown <seconds>",
            "/wild enable | /wild disable",
            "/spawn enable | /spawn disable",
        };
        int y = 90;
        for (String c : commands) {
            addRenderableWidget(new StringWidget(0, y, this.width, 12,
                Component.literal(c).withStyle(ChatFormatting.AQUA), this.font));
            y += 16;
        }

        addRenderableWidget(Button.builder(Component.literal("Back"),
            btn -> this.minecraft.setScreenAndShow(parent))
            .bounds(cx - 50, this.height - 30, 100, 20).build());
    }

    /** Singleplayer / LAN host: edit the local config directly. */
    private void initEditable(int cx) {
        this.wildEnabledLocal = SpawnConfig.wildEnabled;
        this.spawnEnabledLocal = SpawnConfig.spawnEnabled;

        int lblX = cx - 170, lblW = 160;
        int ctlX = cx + 2, ctlW = 130;

        // Protection radius
        label(lblX, 50, lblW, "Protection Radius (blocks)");
        this.radiusField = intField(ctlX, 50, ctlW, "Radius", SpawnConfig.protectionRadius);

        // Wild radius
        label(lblX, 76, lblW, "Wild Radius (0 = full border)");
        this.wildRadiusField = intField(ctlX, 76, ctlW, "Wild Radius", SpawnConfig.wildRadius);

        // Wild cooldown
        label(lblX, 102, lblW, "Wild Cooldown (seconds)");
        this.cooldownField = intField(ctlX, 102, ctlW, "Cooldown", SpawnConfig.wildCooldownSeconds);

        // Wild travel on/off
        label(lblX, 128, lblW, "Wild Travel (/wild + portals)");
        addRenderableWidget(Button.builder(toggleLabel(wildEnabledLocal), btn -> {
            wildEnabledLocal = !wildEnabledLocal;
            btn.setMessage(toggleLabel(wildEnabledLocal));
        }).bounds(ctlX, 128, ctlW, 20).build());

        // /spawn command on/off
        label(lblX, 154, lblW, "/spawn Command");
        addRenderableWidget(Button.builder(toggleLabel(spawnEnabledLocal), btn -> {
            spawnEnabledLocal = !spawnEnabledLocal;
            btn.setMessage(toggleLabel(spawnEnabledLocal));
        }).bounds(ctlX, 154, ctlW, 20).build());

        addRenderableWidget(new StringWidget(0, 182, this.width, 12,
            Component.literal("Spawn point is set in-game: /spawnmanager setexactspawn <pos>")
                .withStyle(ChatFormatting.DARK_GRAY), this.font));

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            SpawnConfig.protectionRadius = parseOr(radiusField, SpawnConfig.protectionRadius);
            SpawnConfig.wildRadius = parseOr(wildRadiusField, SpawnConfig.wildRadius);
            SpawnConfig.wildCooldownSeconds = parseOr(cooldownField, SpawnConfig.wildCooldownSeconds);
            SpawnConfig.wildEnabled = wildEnabledLocal;
            SpawnConfig.spawnEnabled = spawnEnabledLocal;
            SpawnConfig.save();
            this.minecraft.setScreenAndShow(parent);
        }).bounds(cx - 105, this.height - 30, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"),
            btn -> this.minecraft.setScreenAndShow(parent))
            .bounds(cx + 5, this.height - 30, 100, 20).build());
    }

    private void label(int x, int y, int w, String text) {
        addRenderableWidget(new StringWidget(x, y + 6, w, 10, Component.literal(text), this.font));
    }

    private EditBox intField(int x, int y, int w, String name, int value) {
        EditBox box = new EditBox(this.font, x, y, w, 20, Component.literal(name));
        box.setMaxLength(8);
        box.setValue(String.valueOf(value));
        addRenderableWidget(box);
        return box;
    }

    private static Component toggleLabel(boolean enabled) {
        return Component.literal(enabled ? "Enabled" : "Disabled")
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** Parse a non-negative int from the box, or keep the fallback if it's blank/invalid. */
    private static int parseOr(EditBox box, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(box.getValue().trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
