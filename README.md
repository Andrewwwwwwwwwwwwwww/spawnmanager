# Spawn Manager

A server-side Fabric mod that sets the world spawn to an exact block, protects the surrounding area, and adds random-wilderness travel (`/wild` + placeable wild portals). Works with vanilla clients.

## Features

- **Exact spawn point** — `/spawnmanager setexactspawn <pos>` sets the world spawn to a precise block. Tab-completion supports relative coordinates (`~ ~ ~`) just like `/fill`.
- **Spawn protection** — A configurable circular zone around spawn (default 32 blocks) where:
  - Non-op players can't break blocks
  - Non-op players can't open containers (chests, barrels, hoppers, shulker boxes,
    furnaces, etc.) or chest-type entities (chest/hopper minecarts, chest boats)
  - Non-op players can't use redstone (levers, buttons, repeaters, comparators, etc.)
  - Players take no damage
  - **Hostile mobs won't spawn** inside the zone
  - **All explosions** are cancelled (TNT, creepers, ghasts, withers, end crystals,
    beds/respawn anchors) so nothing — mob or player — can damage the spawn build
  - Players are notified when they leave the protected area
- **Wilderness travel** — `/wild` scatters a player to a random safe spot inside the Overworld
  world border (avoids lava, the void, and the spawn zone). Per-player cooldown (default 15 min,
  ops bypass) and a max scatter distance are configurable.
- **Wild portals** — moderators can `/wild place <width> [height] [depth]` to stamp a fully 3D
  region that teleports any player who steps into it (no cooldown) — drop one into the spawn build.
  Each axis is independent: `/wild place 5` → 5×3×5, `/wild place 5 5` → 5×5×5, `/wild place 5 5 7` → 5×5×7.
- **On/off switches** — `/wild enable|disable` and `/spawn enable|disable` (op) turn wild travel
  (command + portals) or the `/spawn` command on and off. While wild travel is disabled, neither the
  `/wild` command nor wild portals do anything.
- **Fully configurable** — every setting has an op command, and the **Mod Menu** screen edits them all
  in singleplayer (on a server it points you at the config file + commands, since a client can't change
  a server's config).
- **Overworld only** — protection and wild travel are restricted to the Overworld; Nether/End are untouched.
- **Persistent** — config saved to `config/spawnmanager.json`; wild portals to `<world>/spawnmanager/wildportals.json`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/spawn` | Anyone | Teleport to the exact spawn block (works for vanilla clients too). |
| `/wild` | Anyone | Teleport to a random safe spot in the Overworld (per-player cooldown). |
| `/spawnmanager setexactspawn <pos>` | Op (2+) | Set the world spawn to an exact block. |
| `/spawnmanager setspawnradius <radius>` | Op (2+) | Change the protection radius in blocks. |
| `/spawnmanager setwildradius <radius>` | Op (2+) | Change the max `/wild` scatter distance (0 = full border). |
| `/spawnmanager setwildcooldown <seconds>` | Op (2+) | Change the `/wild` per-player cooldown (0 = none). |
| `/spawnmanager status` | Op (2+) | Show the protected zone, the wild/spawn toggles, and your position. |
| `/wild place <width> [height] [depth]` | Op (2+) | Place a 3D wild-portal region at your feet. |
| `/wild remove` | Op (2+) | Remove the wild portal you're standing in (or nearest within 5 blocks). |
| `/wild list` | Op (2+) | List all wild portals. |
| `/wild enable` / `/wild disable` | Op (2+) | Turn wild travel (command + portals) on/off. |
| `/spawn enable` / `/spawn disable` | Op (2+) | Turn the `/spawn` command on/off. |

## Config (`config/spawnmanager.json`)

| Key | Default | Meaning |
| --- | --- | --- |
| `protectionRadius` | 32 | Spawn-protection radius in blocks. |
| `wildRadius` | 10000 | Max `/wild` distance from the world-border centre (0 = full border). |
| `wildCooldownSeconds` | 900 | `/wild` cooldown per player (ops bypass). |
| `wildEnabled` | true | Master switch for wild travel (the `/wild` command and wild portals). |
| `spawnEnabled` | true | Whether the `/spawn` command is usable. |

In singleplayer these can all be edited through **Mod Menu** instead of the JSON file.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Drop the Spawn Manager JAR into your `mods` folder.
3. Start the server. The config file will be created at `config/spawnmanager.json` on first run.

## License

All Rights Reserved. See the [LICENSE](LICENSE) file — these mods are proprietary.
