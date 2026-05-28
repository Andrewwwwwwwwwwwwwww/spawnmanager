package io.github.andrewwwwwwwwwwwwwww.spawnmanager.client;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

        if (remoteServer) {
            this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.minecraft.setScreen(parent)
            ).bounds(cx - 50, this.height - 30, 100, 20).build());
            return;
        }

        this.radiusField = new EditBox(this.font, cx - 60, 90, 120, 20, Component.literal("Radius"));
        this.radiusField.setMaxLength(5);
        this.radiusField.setValue(String.valueOf(SpawnConfig.protectionRadius));
        this.addRenderableWidget(this.radiusField);

        this.addRenderableWidget(Button.builder(
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

        this.addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> this.minecraft.setScreen(parent)
        ).bounds(cx + 5, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, 20, 0xFFFFFF);

        if (remoteServer) {
            graphics.drawCenteredString(this.font,
                Component.literal("Settings on this server are managed remotely.")
                    .withStyle(ChatFormatting.GRAY), cx, 70, 0xAAAAAA);
            graphics.drawCenteredString(this.font,
                Component.literal("Use these commands (op required):")
                    .withStyle(ChatFormatting.GRAY), cx, 90, 0xAAAAAA);
            graphics.drawCenteredString(this.font,
                Component.literal("/spawn — teleport to spawn")
                    .withStyle(ChatFormatting.AQUA), cx, 112, 0xFFFFFF);
            graphics.drawCenteredString(this.font,
                Component.literal("/spawnmanager setexactspawn <pos>")
                    .withStyle(ChatFormatting.AQUA), cx, 126, 0xFFFFFF);
            graphics.drawCenteredString(this.font,
                Component.literal("/spawnmanager setspawnradius <radius>")
                    .withStyle(ChatFormatting.AQUA), cx, 140, 0xFFFFFF);
        } else {
            graphics.drawCenteredString(this.font, "Protection Radius (blocks)", cx, 70, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
