package io.github.andrewwwwwwwwwwwwwww.spawnmanager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

/**
 * Shared spawn-protection geometry. Used by the main mod (block breaking, player damage,
 * container access) and by the {@code ServerExplosion} mixin (mob griefing), so the zone
 * definition lives in exactly one place.
 */
public final class SpawnProtection {
    private SpawnProtection() {}

    /**
     * @return true if the given world position is inside the Overworld spawn-protection
     * radius. Returns false for non-Overworld dimensions, when no world spawn is set, or
     * when the level is not a server level.
     */
    public static boolean isProtected(Level level, double x, double z) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return false;

        LevelData.RespawnData respawnData = serverLevel.getRespawnData();
        if (respawnData == null) return false;

        BlockPos spawnPos = respawnData.pos();
        double dx = x - spawnPos.getX();
        double dz = z - spawnPos.getZ();
        int radius = SpawnConfig.protectionRadius;
        return dx * dx + dz * dz <= (double) radius * radius;
    }
}
