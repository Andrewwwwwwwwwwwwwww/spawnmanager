package io.github.andrewwwwwwwwwwwwwww.spawnmanager.client;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpawnManagerConfigScreen extends Screen {
    private final Screen parent;
    private final boolean remoteServer;
    private EditBox radiusField;

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
            addRenderableWidget(new StringWidget(0, 60, this.width, 12,
                Component.literal("Settings on this server are managed remotely.")
                    .withStyle(ChatFormatting.GRAY), this.font));
            addRenderableWidget(new StringWidget(0, 80, this.width, 12,
                Component.literal("Use these commands (op required):")
                    .withStyle(ChatFormatting.GRAY), this.font));
            addRenderableWidget(new StringWidget(0, 104, this.width, 12,
                Component.literal("/spawn — teleport to spawn")
                    .withStyle(ChatFormatting.AQUA), this.font));
            addRenderableWidget(new StringWidget(0, 120, this.width, 12,
                Component.literal("/spawnmanager setexactspawn <pos>")
                    .withStyle(ChatFormatting.AQUA), this.font));
            addRenderableWidget(new StringWidget(0, 136, this.width, 12,
                Component.literal("/spawnmanager setspawnradius <radius>")
                    .withStyle(ChatFormatting.AQUA), this.font));

            addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.minecraft.setScreen(parent)
            ).bounds(cx - 50, this.height - 30, 100, 20).build());
            return;
        }

        addRenderableWidget(new StringWidget(0, 65, this.width, 12,
            Component.literal("Protection Radius (blocks)"), this.font));

        this.radiusField = new EditBox(this.font, cx - 60, 85, 120, 20, Component.literal("Radius"));
        this.radiusField.setMaxLength(5);
        this.radiusField.setValue(String.valueOf(SpawnConfig.protectionRadius));
        addRenderableWidget(this.radiusField);

        addRenderableWidget(Button.builder(
            Component.literal("Save"),
            btn -> {
                try {
                    int v = Math.max(0, Integer.parseInt(this.radiusField.getValue()));
                    SpawnConfig.protectionRadius = v;
                    SpawnConfig.save();
                } catch (NumberFormatException ignored) {}
                this.minecraft.setScreen(parent);
            }
        ).bounds(cx - 105, this.height - 30, 100, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> this.minecraft.setScreen(parent)
        ).bounds(cx + 5, this.height - 30, 100, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
