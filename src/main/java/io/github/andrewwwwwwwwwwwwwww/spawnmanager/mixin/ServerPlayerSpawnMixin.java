package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Places brand-new players on the EXACT set world-spawn block instead of vanilla's upward
 * "nearest safe spot" search, which drops them onto whatever sits above the spawn point (the
 * reported "new players spawn many blocks above" bug). Mirrors {@code /spawn} and the
 * death-respawn handler, both of which use {@code getRespawnData().pos()}.
 *
 * <p>{@code ServerPlayer.adjustSpawnLocation(ServerLevel, BlockPos)} is the vanilla method that
 * fudges a new player's initial position, and it is only called from
 * {@code PlayerList.placeNewPlayer} — so this does NOT touch returning players (they load their
 * saved position from disk). It returns {@code BlockPos}, so this uses
 * {@link CallbackInfoReturnable} (a plain CallbackInfo would fail to apply).
 */
@Mixin(net.minecraft.server.level.ServerPlayer.class)
public abstract class ServerPlayerSpawnMixin {

    @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void spawnmanager$exactNewPlayerSpawn(ServerLevel level, BlockPos spawn,
                                                  CallbackInfoReturnable<BlockPos> cir) {
        LevelData.RespawnData rd = level.getRespawnData();
        if (rd != null) {
            cir.setReturnValue(rd.pos());
        }
    }
}
