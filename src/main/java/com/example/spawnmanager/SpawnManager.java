package com.example.spawnmanager;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpawnManager implements ModInitializer {
    private static final int PROTECTION_RADIUS = 32;
    private final Set<UUID> playersInZone = new HashSet<>();

    private boolean isInZone(double x, double z, BlockPos spawnPos) {
        double dx = x - spawnPos.getX();
        double dz = z - spawnPos.getZ();
        return dx * dx + dz * dz <= (double) PROTECTION_RADIUS * PROTECTION_RADIUS;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("setexactspawn")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                    .then(Commands.argument("y", IntegerArgumentType.integer())
                    .then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            int x = IntegerArgumentType.getInteger(ctx, "x");
                            int y = IntegerArgumentType.getInteger(ctx, "y");
                            int z = IntegerArgumentType.getInteger(ctx, "z");
                            BlockPos pos = new BlockPos(x, y, z);
                            ServerLevel level = source.getLevel();

                            LevelData.RespawnData spawnData = LevelData.RespawnData.of(
                                level.dimension(), pos, 0.0f, 0.0f
                            );
                            ((WritableLevelData) level.getLevelData()).setSpawn(spawnData);

                            level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, source.getServer());

                            source.sendSuccess(() -> Component.literal(
                                "Exact spawn set to " + x + ", " + y + ", " + z), true);
                            return 1;
                        }))))
            )
        );

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel)) return true;
            if (player instanceof ServerPlayer sp && Commands.LEVEL_GAMEMASTERS.check(sp.permissions())) return true;
            if (!serverLevel.dimension().equals(Level.OVERWORLD)) return true;

            LevelData.RespawnData respawnData = serverLevel.getRespawnData();
            if (respawnData == null) return true;

            if (isInZone(pos.getX(), pos.getZ(), respawnData.pos())) {
                player.sendSystemMessage(Component.literal("You cannot break blocks near the spawn point.").withStyle(ChatFormatting.RED));
                return false;
            }
            return true;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer sp)) return true;
            ServerLevel level = (ServerLevel) sp.level();
            if (!level.dimension().equals(Level.OVERWORLD)) return true;

            LevelData.RespawnData respawnData = level.getRespawnData();
            if (respawnData == null) return true;

            return !isInZone(sp.getX(), sp.getZ(), respawnData.pos());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                ServerLevel level = (ServerLevel) sp.level();

                if (!level.dimension().equals(Level.OVERWORLD)) {
                    if (playersInZone.remove(sp.getUUID())) {
                        sp.sendSystemMessage(Component.literal("You are leaving the protected spawn area.").withStyle(ChatFormatting.GREEN));
                    }
                    continue;
                }

                LevelData.RespawnData respawnData = level.getRespawnData();
                if (respawnData == null) continue;

                boolean inZone = isInZone(sp.getX(), sp.getZ(), respawnData.pos());
                boolean wasInZone = playersInZone.contains(sp.getUUID());

                if (wasInZone && !inZone) {
                    sp.sendSystemMessage(Component.literal("You are leaving the protected spawn area.").withStyle(ChatFormatting.GREEN));
                    playersInZone.remove(sp.getUUID());
                } else if (inZone) {
                    playersInZone.add(sp.getUUID());
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            playersInZone.remove(handler.player.getUUID()));
    }
}
