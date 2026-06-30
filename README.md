# uxmSkyblock

[![JitPack](https://jitpack.io/v/UXPLIMA/uxmSkyblock.svg)](https://jitpack.io/#UXPLIMA/uxmSkyblock)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper 1.21.4](https://img.shields.io/badge/Paper-1.21.4-brightgreen.svg)](https://papermc.io/)
[![Cross-server](https://img.shields.io/badge/cross--server-ready-success.svg)](#cross-server-networks)
[![Status](https://img.shields.io/badge/version-0.4.0-blue.svg)](#)

A complete, **config-first** Skyblock plugin for **Paper/Spigot 1.21.4** on **Java 21**. Everything a
player ever touches — commands, messages, menus, roles, upgrades, block values, generators — lives in
YAML, so you can rename, retheme, or fully translate the plugin without writing a single line of code.

It is built **config-first on purpose**. Nothing player-facing is hard-coded: the command name and its
aliases, every GUI layout, every message, every role and permission, and every upgrade tier are yours to
edit. New keys from updates merge into your existing files automatically, so upgrading never wipes your
work.

It runs fine on a single box and scales out to a **multi-backend network** behind Velocity/BungeeCord
when you need it to — and a lightweight module loader lets you bolt on new mechanics without forking.

---

## Table of contents

- [Why uxmSkyblock](#why-uxmskyblock)
- [Requirements](#requirements)
- [Feature tour](#feature-tour)
  - [Island teams & custom roles](#island-teams--custom-roles)
  - [Upgrades](#upgrades)
  - [Generators & obsidian](#generators--obsidian)
  - [Island bank](#island-bank)
  - [Warps](#warps)
  - [Leaderboard & follow-hologram](#leaderboard--follow-hologram)
  - [Protection & visitor controls](#protection--visitor-controls)
  - [PlaceholderAPI](#placeholderapi)
- [Commands](#commands)
- [Configuration](#configuration)
- [Cross-server networks](#cross-server-networks)
- [Installation](#installation)
- [Building from source](#building-from-source)
- [Using the API](#using-the-api)
- [Modules](#modules)
- [Team](#team)
- [Contributing](#contributing)
- [License](#license)

---

## Why uxmSkyblock

- **Config-first, end to end.** Commands, aliases, messages, menus, roles, block values, levels, and
  upgrades are all defined in YAML. Retheme or translate the whole plugin without touching code, and let
  the migrator merge new keys on update so your edits survive.
- **Real teams, real roles.** Ships with sensible built-in ranks (visitor → member → architect →
  moderator → owner), and owners can invent their own roles on the fly and grant exactly the permissions
  they want — per island.
- **Upgrades that change the game.** Team size, island size, crop doubling, mob drops, spawner speed, and
  weighted block generators, each with as many tiers as you care to define.
- **Single server or whole network.** The same jar runs standalone or syncs islands across multiple
  backends through a shared database and Redis behind a proxy.
- **Extensible by design.** Drop an add-on jar into `modules/` and reuse the core island API, storage, and
  proxy layer — no forking, no second standalone plugin.
- **Plays well with others.** Optional Vault, PlaceholderAPI, and FastAsyncWorldEdit hooks; menus still
  render `%skyblock_...%` placeholders even on servers without PlaceholderAPI installed.

## Requirements

| | |
| --- | --- |
| Server | Paper/Spigot **1.21.4** |
| Java | **21** |
| Economy | Vault + any economy plugin — *optional*, enables upgrade costs and the bank |
| Placeholders | PlaceholderAPI — *optional*, adds the `%skyblock_...%` placeholders |
| Schematics | FastAsyncWorldEdit — *optional*, used for schematic-based island creation |
| Storage | SQLite (default) or MySQL — switchable in `config.yml` |
| Network | Redis + a shared MySQL database — *optional*, only for cross-server setups |

---

## Feature tour

### Island teams & custom roles

Islands are teams. The built-in ladder runs **visitor → member → architect → moderator → owner**, and
owners can create their own roles and hand out precisely the permissions they want:

```
/is role create builder
/is role perm builder block_place on
/is role <player> builder
```

### Upgrades

Open `/is upgrade` for a menu of tier-based upgrades — team size, island size, crop doubling, mob drops,
spawner speed, and the block generator. Every tier and its cost is defined in `upgrades.yml`, so you decide
how deep the progression goes.

### Generators & obsidian

Cobble generators are **weighted per upgrade tier**, so high-level islands can roll ores or obsidian
instead of plain cobblestone. Right-click obsidian with an empty bucket to pull the lava back out, so an
obsidian farm never wastes a drop.

### Island bank

A shared, Vault-backed balance per island. Members deposit from their own wallet; trusted ranks withdraw.

```
/is bank deposit 1000
/is bank withdraw 250
```

### Warps

Set named public warps up to the limit a player's rank allows (`skyblock.warps.<n>`), reachable by name or
through the warp menu.

```
/is setwarp shop
/is warp <player> shop
```

### Leaderboard & follow-hologram

`/is top` prints the island rankings. `/is top holo` spawns a personal floating leaderboard that **trails
the player** and is visible only to them.

### Protection & visitor controls

Per-island border colors, visitor toggles, bans, and a full island lock — all from the settings menu or
`/is settings`.

### PlaceholderAPI

Exposes `%skyblock_level%`, `%skyblock_points%`, `%skyblock_bank%`, `%skyblock_flag_pvp%`, and more — out
of the box. Menus render these placeholders even on servers without PlaceholderAPI installed.

---

## Commands

The command name and every alias are configurable. By default the command is `/island` with aliases
`/is`, `/sb`, `/ada`, `/sky`. All sub-commands support tab completion.

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

New keys from plugin updates are merged into your existing files automatically, so upgrading never wipes
your settings.

## Cross-server networks

uxmSkyblock can keep islands in sync across **multiple backend servers** behind Velocity or BungeeCord. Point
every backend at one shared **MySQL** database and one **Redis** instance, flip on the proxy layer in
`config.yml`, and island data, membership, and visits stay consistent network-wide.

You can also run a server that hosts **only a module** (for example a pure Chunklock server) by setting
`island.enabled: false`. The `/island` command and the island world stay off, but the core managers —
economy, block values, storage, proxy, and the module loader — keep running so the module has everything
it needs.

---

## Installation

1. Drop the jar into your server's `plugins/` folder.
2. (Optional) Install **Vault** + an economy plugin, **PlaceholderAPI**, and/or **FastAsyncWorldEdit**.
3. Start the server once to generate the config, then edit the YAML files under `plugins/Skyblock/`.
4. (Optional, networks) Set your MySQL + Redis credentials and enable the proxy layer in `config.yml`, then
   repeat on every backend.

## Building from source

Requires JDK 21.

```bash
./gradlew shadowJar
```

The shaded jar lands in `build/libs/`.

## Using the API

The core is published through [JitPack](https://jitpack.io), so other plugins and modules can compile
against its API.

**Gradle**

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.UXPLIMA:uxmSkyblock:0.4.0'
}
```

**Maven**

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.UXPLIMA</groupId>
    <artifactId>uxmSkyblock</artifactId>
    <version>0.4.0</version>
    <scope>provided</scope>
</dependency>
```

Use `compileOnly`/`provided`: the core runs as its own plugin on the server, so you only compile against it
— you don't bundle it.

**What the API gives you**

- `SkyblockPlugin` — the entry point: `getIslandManager()`, `getRoleManager()`, `getEconomy()`,
  `getTopService()`, `getWarpService()`, and more.
- `Island` / `IslandManager` — islands, members, roles, points, bank, warps.
- `RoleData` / `RoleResolver` — built-in and per-island custom roles.
- `IslandCreateEvent` / `IslandDeleteEvent` — Bukkit events to listen for.
- `SkyblockModule` + `ModuleContext` — write a drop-in module (see [Modules](#modules)).

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

uxmSkyblock has its own add-on system, so you can bolt new mechanics onto islands without forking the plugin
or shipping a second standalone plugin. A module is just a small jar you drop into:

```
plugins/Skyblock/modules/
```

It loads on top of the core and gets full access to the island API — islands, members, roles, points,
levels, upgrades, the economy hook, storage, and the proxy layer. A module can register its own listeners
and commands (cleaned up automatically when it unloads) and keep its own `config.yml` and data folder,
completely separate from the main plugin.

Because modules share the core's classloader, they also share its database connection and proxy messaging.
An add-on can store its data in the same MySQL database and stay in sync across a multi-backend network for
free, instead of reinventing storage and cross-server syncing.

Each module jar carries a `module.yml` (name, version, main class) and a main class that hooks into three
points:

| Hook | When | What you do |
| --- | --- | --- |
| `onLoad` | core is loading | grab what you need from the core API |
| `onEnable` | server starting | register listeners, commands, and tasks |
| `onDisable` | reload/shutdown | tear down cleanly |

**Chunklock** is the first module built on this. It turns the world into a shared grid where each island
starts boxed in, and players expand their territory chunk by chunk by dropping the items the next chunk
costs. It reuses the core island system end to end — membership, roles, bans, warps, visiting, and upgrades
all work exactly as they do on a normal island — and stays in sync across servers through the same database
and Redis channel the core uses.

---

## Team

uxmSkyblock is built and maintained by two developers:

| | Role |
| --- | --- |
| [**@UXPLIMA**](https://github.com/UXPLIMA) (siracozmen01) | Maintainer — networking, API, tooling |
| [**@UXPLIMA**](https://github.com/UXPLIMA) (cengiz1x) | Maintainer — core island systems, gameplay |

## Contributing

Pull requests and issues are welcome. If you're adding a player-facing feature, keep it config-first —
expose strings in `messages.yml`, layouts in `menus/`, and tunables in the relevant YAML — so server owners
can customize it without a rebuild.

## License

Free to use. Pull requests and issues are welcome.
