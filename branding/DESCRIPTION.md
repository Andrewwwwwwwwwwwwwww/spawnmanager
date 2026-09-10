# Spawn Manager — listing copy

**GitHub description:**

Server-side spawn protection and wilderness travel: an exact world spawn, a circular no-grief zone around it, and /wild with placeable portals. Vanilla clients need nothing.

**GitHub topics:**

minecraft, minecraft-mod, fabric, fabricmc, server-side, spawn-protection, griefing, survival, multiplayer, java

**Summary (CurseForge summary field, 255 char max):**

Set the world spawn to an exact block and protect the area around it: no griefing, no damage, no mobs, no explosions. Adds /wild random travel and placeable wild portals. Server-side, so players join with vanilla clients.

---

# Description (paste into the CurseForge description editor in Markdown mode)

## Spawn Manager

Your spawn build, left alone

Spawn Manager pins the world spawn to an exact block, wraps a protected zone around it, and gives players a way out into the wild. Everything runs on the server, so **players join with unmodified clients** and still get all of it.

## An exact spawn block

Vanilla puts new players on the nearest safe spot it can find near the world spawn, which is rarely the block you built for them. `/spawnmanager setexactspawn <pos>` sets it precisely, and tab-completion takes relative coordinates the way `/fill` does. New players and bed-less respawns land on that block, not above it or beside it.

`/spawn` sends anyone back there afterwards.

## A protected zone around it

A circular area around spawn, 32 blocks by default, where non-op players cannot:

- Break or place blocks
- Open containers, including chests, barrels, hoppers, shulker boxes and furnaces, and chest-type entities like chest minecarts and chest boats
- Take or swap armour on armour stands, take or rotate items in item frames, or break armour stands, item frames and paintings, by hand or with a projectile
- Use redstone, including levers, buttons, repeaters and comparators

And inside that zone:

- **Players take no damage.** Nobody is killed while they read the rules.
- **Hostile mobs will not spawn.** No creeper wandering into the shop.
- **Every explosion is cancelled**, from TNT, creepers, ghasts, withers, end crystals, beds and respawn anchors, so nothing can crater the build.

Players are told when they cross out of the protected area, so the boundary is never a surprise.

## Wilderness travel

`/wild` scatters a player to a random safe spot inside the Overworld world border. It avoids lava, the void and the spawn zone, so nobody arrives in a hole. There is a per-player cooldown, fifteen minutes by default, which operators bypass, and a configurable maximum scatter distance.

**Wild portals** put that on the map instead of in a command. `/wild place <width> [height] [depth]` stamps a fully three-dimensional region at your feet that teleports anyone who steps into it, with no cooldown. Each axis is independent, so `/wild place 5` gives you 5x3x5, `/wild place 5 5` gives 5x5x5, and `/wild place 5 5 7` gives 5x5x7. Drop one into an archway in the spawn build and players walk into the wild.

## Commands

| Command | Permission | What it does |
|---|---|---|
| `/spawn` | everyone | Teleport to the exact spawn block. |
| `/wild` | everyone | Teleport to a random safe spot in the Overworld. |
| `/spawnmanager setexactspawn <pos>` | op 2 | Set the world spawn to an exact block. |
| `/spawnmanager setspawnradius <radius>` | op 2 | Change the protection radius, in blocks. |
| `/spawnmanager setwildradius <radius>` | op 2 | Maximum `/wild` scatter distance. 0 uses the full border. |
| `/spawnmanager setwildcooldown <seconds>` | op 2 | The per-player `/wild` cooldown. 0 removes it. |
| `/spawnmanager status` | op 2 | The protected zone, the toggles, and your position. |
| `/wild place <width> [height] [depth]` | op 2 | Place a 3D wild-portal region at your feet. |
| `/wild remove` | op 2 | Remove the portal you are standing in, or the nearest within five blocks. |
| `/wild list` | op 2 | List every wild portal. |
| `/wild enable` and `/wild disable` | op 2 | Turn wild travel on or off, command and portals together. |
| `/spawn enable` and `/spawn disable` | op 2 | Turn the `/spawn` command on or off. |

## Configuration

Every setting has an operator command, so you never have to stop the server to change one. The config lives at `config/spawnmanager.json`, and wild portals are saved per world at `<world>/spawnmanager/wildportals.json`.

If you play singleplayer with **Mod Menu** installed, its settings screen edits everything. On a server it points you at the config file and the commands instead, since a client cannot change a server's configuration.

## Overworld only

Protection and wild travel apply to the Overworld. The Nether and the End are left exactly as they were.

## Installation

Spawn Manager is **server-side**. Only the server needs it, and players join with an unmodified client.

1. Drop the jar into the server's `mods` folder, along with [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Start the server once to generate `config/spawnmanager.json`
3. Stand where you want spawn to be and run `/spawnmanager setexactspawn ~ ~ ~`

Singleplayer works too, running on the integrated server.

Not affiliated with or endorsed by Mojang.
