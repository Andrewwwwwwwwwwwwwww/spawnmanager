package io.github.andrewwwwwwwwwwwwwww.spawnmanager;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpawnManager implements ModInitializer {
    private static final int TICK_INTERVAL = 5;
    private final Set<UUID> playersInZone = new HashSet<>();

    private boolean isInZone(double x, double z, BlockPos spawnPos) {
        double dx = x - spawnPos.getX();
        double dz = z - spawnPos.getZ();
        return dx * dx + dz * dz <= (double) SpawnConfig.protectionRadius * SpawnConfig.protectionRadius;
    }

    /** True when a non-op player is interacting inside the protected Overworld zone. Ops bypass. */
    private boolean isProtectedFromContainerAccess(Player player, Level world, double x, double z) {
        if (player instanceof ServerPlayer sp && Commands.LEVEL_GAMEMASTERS.check(sp.permissions())) return false;
        return SpawnProtection.isProtected(world, x, z);
    }

    private void notifyContainerBlocked(Player player) {
        player.sendSystemMessage(Component.literal("You cannot open containers near the spawn point.")
            .withStyle(ChatFormatting.RED));
    }

    private void notifyRedstoneBlocked(Player player) {
        player.sendSystemMessage(Component.literal("You cannot use redstone near the spawn point.")
            .withStyle(ChatFormatting.RED));
    }

    /** Redstone components a player interacts with by right-clicking (levers, buttons, etc.). */
    private static boolean isRedstoneInteractive(BlockState state) {
        return state.is(BlockTags.BUTTONS)
            || state.is(Blocks.LEVER)
            || state.is(Blocks.REPEATER)
            || state.is(Blocks.COMPARATOR)
            || state.is(Blocks.DAYLIGHT_DETECTOR);
    }

    @Override
    public void onInitialize() {
        SpawnConfig.load();

        // /wild + physical wild portals
        ServerLifecycleEvents.SERVER_STARTED.register(WildTravel::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(srv -> WildTravel.save());
        ServerTickEvents.END_SERVER_TICK.register(WildTravel::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WildTravel.register(dispatcher);
            dispatcher.register(
                Commands.literal("spawn")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer player)) {
                            source.sendFailure(Component.literal("This command must be run by a player."));
                            return 0;
                        }
                        ServerLevel overworld = source.getServer().overworld();
                        LevelData.RespawnData respawnData = overworld.getRespawnData();
                        if (respawnData == null) {
                            source.sendFailure(Component.literal("World spawn is not set."));
                            return 0;
                        }
                        BlockPos pos = respawnData.pos();
                        player.teleportTo(overworld,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            java.util.Set.of(),
                            player.getYRot(),
                            player.getXRot(),
                            true);
                        source.sendSuccess(() -> Component.literal("Teleported to spawn.")
                            .withStyle(ChatFormatting.GREEN), false);
                        return 1;
                    })
            );

            dispatcher.register(
                Commands.literal("spawnmanager")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("setexactspawn")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
                                ServerLevel level = source.getLevel();

                                if (!(level.getLevelData() instanceof WritableLevelData writableLevelData)) {
                                    source.sendFailure(Component.literal("Unable to set spawn: level data is not writable."));
                                    return 0;
                                }

                                LevelData.RespawnData spawnData = LevelData.RespawnData.of(
                                    level.dimension(), pos, 0.0f, 0.0f
                                );
                                writableLevelData.setSpawn(spawnData);

                                level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, source.getServer());

                                source.sendSuccess(() -> Component.literal(
                                    "Exact spawn set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), true);
                                return 1;
                            })))
                    .then(Commands.literal("setspawnradius")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                SpawnConfig.protectionRadius = radius;
                                SpawnConfig.save();
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Spawn protection radius set to " + radius), true);
                                return 1;
                            })))
                    .then(Commands.literal("status")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerLevel overworld = source.getServer().overworld();
                            LevelData.RespawnData rd = overworld.getRespawnData();
                            int r = SpawnConfig.protectionRadius;
                            if (rd == null) {
                                source.sendSuccess(() -> Component.literal(
                                    "No world spawn is set — container/block/damage protection is INACTIVE."), false);
                                return 1;
                            }
                            BlockPos c = rd.pos();
                            source.sendSuccess(() -> Component.literal(
                                "Protected zone: centre " + c.getX() + ", " + c.getZ()
                                    + "  |  radius " + r + " blocks  (Overworld only)")
                                .withStyle(ChatFormatting.AQUA), false);
                            if (source.getEntity() instanceof ServerPlayer p) {
                                double dx = p.getX() - c.getX();
                                double dz = p.getZ() - c.getZ();
                                double dist = Math.sqrt(dx * dx + dz * dz);
                                boolean inside = p.level().dimension().equals(Level.OVERWORLD) && dist <= r;
                                source.sendSuccess(() -> Component.literal(String.format(
                                    "You are %.1f blocks from centre — %s", dist,
                                    inside ? "INSIDE the protected zone" : "outside the zone"))
                                    .withStyle(inside ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
                            }
                            return 1;
                        }))
            );
        });

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

        // Block non-ops from opening container blocks (chests, barrels, hoppers, shulker
        // boxes, furnaces, etc.) inside the protected zone.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            if (isProtectedFromContainerAccess(player, world, pos.getX(), pos.getZ())) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof Container || blockEntity instanceof MenuProvider) {
                    notifyContainerBlocked(player);
                    return InteractionResult.FAIL;
                }
                if (isRedstoneInteractive(world.getBlockState(pos))) {
                    notifyRedstoneBlocked(player);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        // Block non-ops from opening chest-type entities (chest/hopper minecarts, chest
        // boats) inside the protected zone.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof Container
                && isProtectedFromContainerAccess(player, world, entity.getX(), entity.getZ())) {
                notifyContainerBlocked(player);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
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
            ServerLevel overworld = server.overworld();
            if (overworld == null || overworld.getGameTime() % TICK_INTERVAL != 0L) return;

            LevelData.RespawnData overworldSpawn = overworld.getRespawnData();
            BlockPos spawnPos = overworldSpawn != null ? overworldSpawn.pos() : null;

            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                ServerLevel level = (ServerLevel) sp.level();

                if (!level.dimension().equals(Level.OVERWORLD)) {
                    if (playersInZone.remove(sp.getUUID())) {
                        sp.sendSystemMessage(Component.literal("You are leaving the protected spawn area.").withStyle(ChatFormatting.GREEN));
                    }
                    continue;
                }

                if (spawnPos == null) continue;

                boolean inZone = isInZone(sp.getX(), sp.getZ(), spawnPos);
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
