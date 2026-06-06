package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnProtection;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops mobs from spawning inside the spawn-protection radius. {@code checkSpawnRules} is the
 * rules-based spawn gate (natural, spawner, and chunk-generation spawns), so returning false there
 * keeps the protected area clear. Player-driven spawns (spawn eggs, breeding, commands) use other
 * spawn reasons that don't go through this check, so those still work.
 */
@Mixin(Mob.class)
public class MobSpawnMixin {

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private void spawnmanager$noSpawnNearSpawn(LevelAccessor level, EntitySpawnReason reason,
                                               CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        // Use the spawn-check level parameter (the mob's own level() may not be assigned yet).
        if (level instanceof net.minecraft.world.level.Level lvl
                && SpawnProtection.isProtected(lvl, self.getX(), self.getZ())) {
            cir.setReturnValue(false);
        }
    }
}
