# Spawn Manager

A Fabric mod for Minecraft that lets server admins set the world spawn to an exact block (with no random radius) and protects the surrounding area from block breaking and player damage.

## Features

- **Exact spawn point** — `/spawnmanager setexactspawn <pos>` sets the world spawn to a precise block. Tab-completion supports relative coordinates (`~ ~ ~`) just like `/fill`.
- **Spawn protection** — A configurable circular zone around spawn (default 32 blocks) where:
  - Non-op players can't break blocks
  - Players take no damage
  - Players are notified when they leave the protected area
- **Overworld only** — protection logic is restricted to the overworld; Nether/End are untouched.
- **Persistent config** — radius is saved to `config/spawnmanager.json` and survives restarts.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/spawn` | Anyone | Teleport to the exact spawn block (works for vanilla clients too). |
| `/spawnmanager setexactspawn <pos>` | Op (2+) | Set the world spawn to an exact block. |
| `/spawnmanager setspawnradius <radius>` | Op (2+) | Change the protection radius in blocks. |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Drop the Spawn Manager JAR into your `mods` folder.
3. Start the server. The config file will be created at `config/spawnmanager.json` on first run.

## License

MIT
