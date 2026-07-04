package io.github.andrewwwwwwwwwwwwwww.spawnmanager.mixin;

import io.github.andrewwwwwwwwwwwwwww.spawnmanager.SpawnProtection;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes item frames (incl. glow frames) and paintings indestructible inside the spawn-protection
 * radius. This closes the projectile / fluid / piston damage vector that no Fabric event can cover
 * — these entities are NOT {@code LivingEntity}, so {@code ServerLivingEntityEvents.ALLOW_DAMAGE}
 * never sees them, and their damage doesn't go through the player-attack path that
 * {@code AttackEntityCallback} intercepts.
 *
 * <p>Targets BOTH {@code BlockAttachedEntity} (the base {@code hurtServer} used by paintings and
 * leash knots) AND {@code ItemFrame} (which OVERRIDES {@code hurtServer} — glow frames inherit it),
 * because a {@code BlockAttachedEntity}-only mixin would never fire for item frames.
 *
 * <p>{@code hurtServer(ServerLevel, DamageSource, float)} returns {@code boolean}, so this uses
 * {@link CallbackInfoReturnable} (returning {@code false} = "damage rejected, nothing happened");
 * a plain CallbackInfo would fail to apply and crash the server on the first hit. Ops bypass via
 * the damage source's owning entity.
 */
@Mixin({
    net.minecraft.world.entity.decoration.BlockAttachedEntity.class,
    net.minecraft.world.entity.decoration.ItemFrame.class,
})
public abstract class BlockAttachedEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void spawnmanager$protectDecorationsAtSpawn(ServerLevel level, DamageSource source, float amount,
                                                        CallbackInfoReturnable<Boolean> cir) {
        try {
            Entity self = (Entity) (Object) this;
            if (!SpawnProtection.isProtected(level, self.getX(), self.getZ())) return;
            if (source.getEntity() instanceof ServerPlayer op
                && Commands.LEVEL_GAMEMASTERS.check(op.permissions())) return;
            cir.setReturnValue(false);
        } catch (Throwable ignored) {
        }
    }
}
