# Spawn Manager Changelog

## [2.0.0] - 2026-06-27

### Changed
- **Stable 2.0.0 release.** No gameplay changes from 1.6.0 — this marks the mod stable and aligns it with
  the unified release across the mod suite.
- **Jar filenames now include the Minecraft version** (e.g. `spawnmanager-2.0.0+mc26.2.jar`) so the 26.2
  and 26.1.2 downloads are unambiguous.
- A parallel **MC 26.1.2** build is now published (`spawnmanager-2.0.0+mc26.1.2.jar`) for players still on
  the older version.

## [1.6.0] - 2026-06-16

### Changed
- **Updated to Minecraft 26.2.** Bumped `minecraft_version` to `26.2`, Fabric API to `0.152.1+26.2`,
  and Fabric Loader to `0.19.3`. Loom stays on `1.16.2` (builds 26.2 fine; 1.17.x would force a
  Gradle 9.5 wrapper bump). `fabric.mod.json` now depends on `minecraft ~26.2`, `fabricloader
  >=0.19.3`, and `fabric-api >=0.152.1`.
- **Fixed the only 26.2 API break:** `Minecraft.setScreen(Screen)` was renamed to
  `setScreenAndShow(Screen)` — updated the four call sites in the Mod Menu config screen. The
  spawn-protection mixins (`SpawnPlacements.checkSpawnRules`, `ServerExplosion.explode()`) compiled
  unchanged against 26.2.

## [1.5.5] - 2026-06-06

### Fixed
- **Respawn now lands on the EXACT set spawn block.** Players without a bed/anchor were respawning
  a block above the spawn (vanilla searches for the "nearest safe spot"). They're now teleported to
  the precise `setexactspawn` position on death, matching `/spawn`. Players with a bed/anchor are
  unaffected, and End-return respawns are left alone.

## [1.5.4] - 2026-06-06

### Fixed
- **Mobs still spawning in the radius.** The previous hook (`Mob.checkSpawnRules`) ran before the mob
  was positioned, so the location check was wrong. Now hooks the natural spawner's
  `SpawnPlacements.checkSpawnRules`, which gets the actual spawn BlockPos — reliably blocks
  natural/structure spawns in the zone.

## [1.5.3] - 2026-06-06

### Fixed
- **CRITICAL: server crash on every explosion.** `ServerExplosion.explode()` returns `int`, but the
  explosion mixin used `CallbackInfo` (void) with `cancellable=true`. That makes the mixin fail to
  *apply* the first time the class loads (any TNT/creeper explosion) → server crash. Now uses
  `CallbackInfoReturnable<Integer>` and returns 0 to cancel. (Explosion protection actually works now.)

## [1.5.2] - 2026-06-05

### Fixed
- Mob-spawn blocking now checks the **spawn-check level parameter** instead of the mob's own
  `level()` (which may not be assigned yet during the check) — should reliably stop mobs spawning
  in the radius.
- Explosion handler wrapped defensively (note: the real crash cause was the wrong callback type,
  fixed in 1.5.3).

## [1.5.1] - 2026-06-05

### Added
- **No redstone interaction inside the spawn radius** — non-op players can't use levers, buttons,
  repeaters, comparators, or daylight detectors in the zone (ops bypass). Reuses the existing
  block-use protection.

## [1.5.0] - 2026-06-05

### Added
- **No mob spawning inside the spawn-protection radius** — natural, spawner, and chunk-generation
  spawns are blocked in the zone (`MobSpawnMixin` on `Mob.checkSpawnRules`). Player-driven spawns
  (spawn eggs, breeding, commands) still work, as they don't use the rules-based spawn check.

## [1.4.2] - 2026-06-05

### Changed
- **`/wild` now has a per-player cooldown** (config `wildCooldownSeconds`, default **900 = 15 min**;
  ops bypass). The cooldown only starts on a successful teleport.
- **Wild portals have no cooldown** — stepping into a portal always teleports immediately (the
  teleport moves you out of the region the same tick, so it won't loop).

## [1.4.1] - 2026-06-05

### Changed
- **Wild portals are now adjustable-size regions.** `/wild place <size> [height]` stamps a
  `size`×`size` footprint (1–64) of the given `height` (1–32, default 3) centred on you — so you
  can make anything from a 1-block step to a big plaza. `/wild remove` removes the portal you're
  standing in (or the nearest within 5 blocks); `/wild list` shows each region's centre and size.
  Existing single-block portals are auto-upgraded to 1×3 regions on load.

## [1.4.0] - 2026-06-05

### Added
- **`/wild`** (anyone) — teleports you to a random safe spot inside the Overworld world border.
  Avoids lava, the void, and the spawn-protection zone; Overworld only (refuses in Nether/End).
  Scatter distance is capped by the new `wildRadius` config (default 10000 blocks from border
  centre; set 0 to use the full border).
- **Wild portals** (moderators) — `/wild place` drops an invisible "wild portal" trigger at your
  feet; any player who steps into it gets the same random teleport. Built for dropping into the
  spawn build. Also `/wild remove` (nearest within 3 blocks) and `/wild list`. Portals persist in
  `<world>/spawnmanager/wildportals.json`, with a 5-second per-player cooldown.

## [1.3.1] - 2026-06-04

### Added
- `/spawnmanager status` — prints the protected zone's centre and radius, and (when run by a
  player) how far you are from the centre and whether you're inside it. Useful for diagnosing
  why a container/block is or isn't protected.

### Changed
- **All explosions disabled at spawn** — the `ServerExplosion` mixin now cancels *every*
  explosion centred inside the protection radius, not just mob-caused ones. This covers TNT,
  end crystals, beds/respawn anchors, etc., so decorations like end crystals are no longer
  destroyed. Neither blocks nor entities take explosion damage in the zone.

## [1.3.0] - 2026-06-02

### Added
- **Container protection** — non-op players can no longer open container blocks (chests,
  barrels, hoppers, shulker boxes, furnaces, dispensers, etc.) or chest-type entities
  (chest/hopper minecarts, chest boats) inside the spawn-protection radius. Ops bypass.
- **Mob griefing protection** — explosions caused by mobs (creepers, ghast fireballs,
  withers, etc.) are cancelled entirely when their centre is inside the protection radius,
  so mob griefing can no longer alter the spawn build's appearance. Player- and TNT-driven
  explosions are unaffected. Implemented via a `ServerExplosion` mixin.

### Changed
- Zone geometry extracted into a shared `SpawnProtection` helper so block breaking, player
  damage immunity, container access, and the explosion mixin all use one definition.

---

## [1.1.1] - 2026-05-15

### Changed
- `environment` changed from `"*"` to `"server"` — Spawn Manager is purely server-side; players do not need it installed on their client.
- Fabric Loom pinned to `1.16.2` (stable) — was previously on `1.16-SNAPSHOT`.
- Fabric API dependency tightened to `>=0.148.2` instead of the wildcard `*`.

### Added
- MIT `LICENSE` file added to the repository and packaged in the JAR.
- Mod icon.

---

## [1.1.0] - 2026-05-15

### Added
- Repackaged to `io.github.andrewwwwwwwwwwwwwww.spawnmanager` namespace.
- SLF4J logger replaces raw stderr output.
- `fabric.mod.json` contact metadata (homepage, sources, issues).
- README.md with full feature and install documentation.

### Changed
- Server tick interval optimized — zone checks run every 5 ticks instead of every tick, reducing overhead on busy servers.

---

## [1.0.1] - 2026-05-15

### Fixed
- Unchecked cast removed from `/spawnmanager setexactspawn`.
- `Gson` instance in `SpawnConfig` reused instead of re-created on each save.

---

## [1.0.0] - 2026-05-15

### Added
- Initial release.
- `/spawnmanager setexactspawn <pos>` — sets the world spawn to an exact block with no random radius. Tab-completion supports relative coordinates (`~ ~ ~`).
- `/spawnmanager setspawnradius <radius>` — configures the circular protection zone radius (default 32 blocks).
- Spawn protection zone: non-op players cannot break blocks within the radius.
- Damage immunity inside the spawn zone — players take no damage while in the protected area.
- Player zone-exit notification — a green message is shown when a player walks out of the protected area.
- Protection is Overworld-only; Nether and End are unaffected.
- Persistent config saved to `config/spawnmanager.json`.
