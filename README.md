# MKM — Minecraft RPG

MKM is a large-scale RPG extension of Minecraft: real-time Minecraft combat stays recognizable, while persistent character builds, action abilities, magic, equipment, quests and authored encounters add role-playing depth. No initiative turns or hidden hit/miss accuracy rolls for ordinary physical attacks.

## Active development platform

- **Minecraft 26.3, Fabric Loader 0.20.1, Java 25, Fabric Loom.**
- The distributed Fabric mod embeds the official Fabric API as a nested mod; install Minecraft and Fabric Loader.
- The `main` branch still contains the original **NeoForge 26.2 template**. It is **not** the active executable source for 26.3.
- The working no-animation gameplay checkpoint is `feature/action-combat-c2-state-hud` (C2.0, 0.0.4-fabric.0).
- C2.1 / C3 animations are under **separate stacked draft PRs**. Do not merge them without in-game visual acceptance.

## Current playable prototype

- Persistent and owner-synchronized character state: XP, level derived from XP and seven attributes (STR, DEX, VIT, END, INT, WIL, PER).
- Deterministic derived stats via `PlayerModifierSnapshot`; these formulas are initial tuning values.
- Character Sheet via **K** or right-click of `MKM Codex`, and a dedicated Creative inventory tab.
- Server-owned combat: stamina, three-step **R** combo, **G + movement** eight-way dodge, heavy attack, mob poise/stagger and stamina/action HUD.
- Fabric networking validates client action intents. Dedicated-server and headless client startup are part of CI. User-visible animation quality requires actual in-world testing.

Magic casting, attribute-point allocation, equipment-derived modifiers, dialogue, quests, faction systems and authored RPG encounters are *not yet complete*.

## Project structure and next milestones

- [Current Fabric workstreams](docs/FABRIC_RPG_WORKSTREAMS.md) — canonical current status, subsystem ownership, roadmap and branch integration.
- [Game design](docs/GAME_DESIGN.md) — gameplay pillars.
- [Technical architecture](docs/ARCHITECTURE.md) — server/client boundaries.
- [Primary attributes](docs/PRIMARY_ATTRIBUTES.md) and [modifier vocabulary](docs/PLAYER_MODIFIERS.md).
- [Historical roadmap](docs/ROADMAP.md) and [technical decisions](docs/TECHNICAL_DECISIONS.md).
- [Agent/change rules](AGENTS.md).

Next gameplay priority is a vertical slice of **earned XP and server-validated attribute-point allocation**, then the first ability/spell and the first NPC/quest encounter. Animation quality work remains in its own branch family.

## Development

Use a Java 25 JDK and the selected Fabric branch:

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

On Windows, use `gradlew.bat`. Run relevant JUnit tests and dedicated-server checks for every gameplay change. The existing GitHub Actions workflow creates versioned build and installer artifacts.

## Engineering rules

- Logical server owns saves, damage, resources, rewards, quests and all other gameplay outcomes.
- Client collects intent and renders synchronized state; never trust client-supplied damage or XP.
- Keep a single modular Fabric mod; add domain packages only when actual functionality requires them.
- Keep `mkm:*` identifiers and persistent schema migration compatible.
- One focused branch/PR per subsystem, preferably from the reviewed C2 gameplay baseline until C3 is integrated.
- Do not silently replace or modify the animation pipeline while developing progression, abilities or quests.

## License

All Rights Reserved until an explicit license is adopted.
