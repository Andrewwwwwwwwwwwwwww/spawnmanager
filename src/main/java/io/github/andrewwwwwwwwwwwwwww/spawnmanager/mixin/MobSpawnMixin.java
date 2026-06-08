package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops mobs from spawning inside the spawn-protection radius. We hook the natural spawner's gate
 * {@code SpawnPlacements.checkSpawnRules}, which receives the actual spawn {@link BlockPos} (unlike
 * the per-mob {@code checkSpawnRules}, where the mob isn't positioned yet). Returning false rejects
 * the spawn position. Player-driven spawns (eggs, breeding, commands) don't go through this gate.
 */
@Mixin(SpawnPlacements.class)
public class MobSpawnMixin {

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void spawnmanager$noSpawnNearSpawn(EntityType<?> type, ServerLevelAccessor level,
                                                      EntitySpawnReason reason, BlockPos pos, RandomSource random,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (SpawnProtection.isProtected(level.getLevel(), pos.getX(), pos.getZ())) {
            cir.setReturnValue(false);
        }
    }
}
