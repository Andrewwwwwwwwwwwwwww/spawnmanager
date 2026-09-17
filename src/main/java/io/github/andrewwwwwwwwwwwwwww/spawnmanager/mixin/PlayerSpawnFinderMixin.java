package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Hands every player-spawn search the exact set spawn block instead of letting it look for its own
 * spot.
 *
 * <p>This is the hook that actually covers a brand-new player's first join. {@code PrepareSpawnTask}
 * runs during the login configuration phase, before a {@code ServerPlayer} exists at all, and asks
 * {@code PlayerSpawnFinder.findSpawn} where to put them. Left alone, that ends in
 * {@code fixupSpawnHeight}, which walks the position to the {@code MOTION_BLOCKING_NO_LEAVES}
 * heightmap - the terrain surface - so a spawn set underground or on a platform was ignored and
 * players arrived on the roof of the world instead.
 *
 * <p>{@code ServerPlayer.adjustSpawnLocation} funnels into the same method, so respawns are covered
 * here too. {@code MinecraftServer} uses {@code getSpawnPosInChunk} rather than {@code findSpawn},
 * which is why picking the world spawn at world creation is left alone.
 *
 * <p>Only engages when {@code respawnRadius} is 0, which is exactly what
 * {@code /spawnmanager setexactspawn} sets it to. Give the world a respawn radius back and vanilla
 * scattering returns.
 */
@Mixin(net.minecraft.server.level.PlayerSpawnFinder.class)
public abstract class PlayerSpawnFinderMixin {

    @Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
    private static void spawnmanager$exactSpawn(ServerLevel level, BlockPos suggestion,
                                                CallbackInfoReturnable<CompletableFuture<Vec3>> cir) {
        LevelData.RespawnData rd = level.getRespawnData();
        if (rd == null) return;
        if (!level.dimension().equals(rd.dimension())) return;
        if (level.getGameRules().get(GameRules.RESPAWN_RADIUS) != 0) return;

        BlockPos pos = rd.pos();
        // Bottom centre, so the player stands ON the block's own Y rather than inside the one above -
        // the same placement /spawn and the death-respawn handler use.
        cir.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(pos)));
    }
}
