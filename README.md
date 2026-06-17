# Skyblock

A complete, config-first Skyblock plugin for Paper/Spigot **1.21.4**. Everything a
player touches — commands, messages, menus, roles, upgrades, block values — lives
in YAML, so you can rename, retheme, or translate the whole thing without writing
a line of code. It runs fine on a single server and scales out to a multi-backend
network when you need it to.

Built for Java 21.

## Highlights

- **Real island teams with custom roles.** Ships with sensible built-in ranks
  (visitor → member → architect → moderator → owner), and owners can create their
  own roles on the fly and hand out exactly the permissions they want
  (`/is role create builder`, then `/is role perm builder block_place on`).
- **Upgrades that matter.** Team size, island size, crop doubling, mob drops,
  spawner speed, and weighted block generators — each with as many tiers as you
  care to define in `upgrades.yml`.
- **Custom generators, including obsidian.** Generators are weighted per tier, so
  high-level islands can roll ores or obsidian instead of plain cobblestone.
  Right-click obsidian with an empty bucket to pull the lava back out, so an
  obsidian farm never wastes a drop.
- **Island bank.** A shared balance per island backed by Vault. Members deposit
  from their own wallet; trusted ranks withdraw.
- **Named warps with per-rank limits.** Set as many public warps as a player's
  rank allows (`skyblock.warps.<n>`), reachable by name or through the warp menu.
- **Leaderboard + a hologram that follows you.** `/is top` prints the rankings;
  `/is top holo` spawns a personal floating leaderboard that trails the player and
  is only visible to them.
- **Per-island border colors, visitor controls, bans, and island lock.**
- **PlaceholderAPI support** out of the box (`%skyblock_level%`, `%skyblock_points%`,
  `%skyblock_bank%`, `%skyblock_flag_pvp%`, and more). Menus still render these
  placeholders even on servers without PlaceholderAPI installed.
- **MySQL or SQLite** storage, switchable in the config.
- **Cross-server ready.** An optional proxy layer keeps islands in sync across
  multiple backends behind Velocity/BungeeCord using a shared database and Redis.
- **Extensible.** A lightweight module loader lets you drop add-on jars into
  `plugins/Skyblock/modules/` and reuse the core island API.

## Commands

The command name and every alias are configurable. By default the command is
`/island` with aliases `/is`, `/sb`, `/ada`, `/sky`.

| Command | Description |
| --- | --- |
| `/is` | Open the main menu |
| `/is create [type]` | Create your island |
| `/is home` | Teleport to your island |
| `/is visit <player>` | Visit another player's island |
| `/is sethome` | Set your island spawn |
| `/is settings` | Open the settings menu (flags, lock, time) |
| `/is upgrade` | Open the upgrades menu |
| `/is border <BLUE\|GREEN\|RED>` | Change your island border color |
| `/is block` | Browse what every block is worth |
| `/is bank [deposit\|withdraw] <amount>` | Manage the island bank |
| `/is top [holo]` | Show the leaderboard, or toggle the following hologram |
| `/is setwarp <name>` / `/is delwarp <name>` | Manage public warps |
| `/is warp <player> [name]` | Warp to a public island warp |
| `/is invite\|kick\|ban\|trust\|transfer ...` | Manage your team |
| `/is role <player> <role>` | Assign a role |
| `/is role create\|delete\|perm\|list ...` | Manage custom roles |

All sub-commands support tab completion.

## Configuration

| File | What it controls |
| --- | --- |
| `config.yml` | World, command names/aliases, storage, proxy, warps, generators |
| `messages.yml` | Every player-facing message |
| `menus/*.yml` | All GUIs (main, settings, upgrades, warps, block values, help) |
| `roles.yml` | Built-in role names and permissions |
| `block-values.yml` | Points per block |
| `levels.yml` | Level thresholds |
| `upgrades.yml` | Upgrade tiers and costs |

New keys from plugin updates are merged into your existing files automatically, so
upgrading never wipes your settings.

## Dependencies

- **Paper/Spigot 1.21.4** (required)
- **Vault** + an economy plugin — optional; enables upgrade costs and the bank
- **PlaceholderAPI** — optional; adds the `%skyblock_...%` placeholders
- **FastAsyncWorldEdit** — optional; used for schematic-based island creation

## Building

```bash
./gradlew shadowJar
```

The shaded jar lands in `build/libs/`.

## Using SkyblockCore as a dependency

The core is published through [JitPack](https://jitpack.io), so other plugins and
modules can compile against its API.

**Gradle**

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Dsngxddd:SkyblockCore:0.4.0'
}
```

**Maven**

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Dsngxddd</groupId>
    <artifactId>SkyblockCore</artifactId>
    <version>0.4.0</version>
    <scope>provided</scope>
</dependency>
```

Use `compileOnly`/`provided`: the core runs as its own plugin on the server, so
you only compile against it — you don't bundle it.

**What the API gives you**

- `SkyblockPlugin` — the entry point: `getIslandManager()`, `getRoleManager()`,
  `getEconomy()`, `getTopService()`, `getWarpService()`, and more.
- `Island` / `IslandManager` — islands, members, roles, points, bank, warps.
- `RoleData` / `RoleResolver` — built-in and per-island custom roles.
- `IslandCreateEvent` / `IslandDeleteEvent` — Bukkit events to listen for.
- `SkyblockModule` + `ModuleContext` — write a drop-in module (see below).

**Example** — read and modify an island from your own plugin:

```java
SkyblockPlugin core = (SkyblockPlugin) Bukkit.getPluginManager().getPlugin("Skyblock");
Island island = core.getIslandManager().getByMember(player.getUniqueId());
if (island != null) {
    island.depositBank(1000);
    core.getIslandManager().saveAsync(island);
}
```

## Modules

Skyblock has its own add-on system, so you can bolt new mechanics onto islands
without forking the plugin or shipping a second standalone plugin. A module is
just a small jar you drop into:

```
plugins/Skyblock/modules/
```

It loads on top of the core and gets full access to the island API — islands,
members, roles, points, levels, upgrades, the economy hook, storage, and the
proxy layer. A module can register its own listeners and commands (they're cleaned
up automatically when the module unloads) and keep its own `config.yml` and data
folder, completely separate from the main plugin.

Because modules share the core's classloader, they also share its database
connection and proxy messaging. That means an add-on can store its data in the
same MySQL database and stay in sync across a multi-backend network for free,
instead of reinventing storage and cross-server syncing.

Each module jar carries a `module.yml` (name, version, main class) and a main
class that hooks into three points:

- `onLoad` — grab what you need from the core API
- `onEnable` — register listeners, commands, and tasks
- `onDisable` — tear down cleanly on reload or shutdown

**Chunklock** is the first module built on this. It turns the world into a shared
grid where each island starts boxed in, and players expand their territory chunk
by chunk by dropping the items the next chunk costs. It reuses the core island
system end to end — membership, roles, bans, warps, visiting, and upgrades all
work exactly as they do on a normal island — and stays in sync across servers
through the same database and Redis channel the core uses.

To run a server that hosts only a module (for example a pure Chunklock server),
set `island.enabled: false`. The `/island` command and island world stay off, but
the core managers — economy, block values, storage, proxy, and the module loader
— keep running so the module has everything it needs.

## License

Free to use. Pull requests and issues are welcome.
