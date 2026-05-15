# Spawn Manager Changelog

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
