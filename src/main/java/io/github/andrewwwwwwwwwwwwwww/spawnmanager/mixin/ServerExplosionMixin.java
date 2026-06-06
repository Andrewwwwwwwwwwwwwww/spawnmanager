package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnProtection;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables ALL explosions whose centre lies inside the spawn-protection radius — creepers,
 * ghast/wither blasts, TNT, end crystals, beds/respawn anchors, etc. The whole server-side
 * explosion is cancelled at the source, so neither blocks nor entities take any damage. This
 * keeps decorations like end crystals safe at spawn.
 *
 * NOTE: {@code ServerExplosion.explode()} returns {@code int}, so this MUST use
 * {@link CallbackInfoReturnable} (returning 0 = "nothing happened"); using a plain CallbackInfo
 * makes the mixin fail to apply and crashes the server on the first explosion.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void spawnmanager$cancelExplosionsAtSpawn(CallbackInfoReturnable<Integer> cir) {
        try {
            Explosion self = (Explosion) (Object) this;
            Vec3 center = self.center();
            if (SpawnProtection.isProtected(self.level(), center.x, center.z)) {
                cir.setReturnValue(0);
            }
        } catch (Throwable ignored) {
        }
    }
}
