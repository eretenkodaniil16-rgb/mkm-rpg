# MKM

MKM is a large-scale RPG conversion mod for Minecraft. The project targets a middle ground between a total conversion and a CRPG: Minecraft remains the world and interaction foundation, while character progression, combat abilities, quests, dialogue, factions, encounters and long-form RPG progression are built as first-class systems.

## Technology baseline

- Minecraft 26.2
- NeoForge 26.2.0.77
- Java 25
- ModDevGradle
- Gradle Wrapper included in the repository

## Current milestone

`0.0.2 — Character Core`

The foundation establishes a stable common bootstrap, dedicated-server-safe architecture, build automation, persistence boundaries, synchronization conventions and project documentation. Character Core now proves the first real RPG state path: persistent player XP and prototype attributes, derived levels, owner-only synchronization and diagnostic/admin commands.

See:

- `docs/GAME_DESIGN.md` — product vision and RPG design pillars
- `docs/ARCHITECTURE.md` — technical boundaries and system design
- `docs/CHARACTER_CORE.md` — current player-state implementation and prototype progression
- `docs/ROADMAP.md` — implementation sequence
- `docs/TECHNICAL_DECISIONS.md` — durable architectural decisions
- `AGENTS.md` — rules for AI-assisted development

## Character Core prototype

Current player RPG state contains:

- total experience;
- derived level;
- Strength, Dexterity and Vitality prototype attributes;
- a versioned persistence schema;
- copy-on-death persistence;
- server-to-owner synchronization.

Command diagnostics:

```text
/mkm
/mkm help
/mkm ping
/mkm stats
/mkm xp add <amount>
/mkm xp set <amount>
```

`/mkm`, `/mkm ping` and `/mkm stats` are available to normal players. XP mutation commands require gamemaster/operator permission (or cheats in an integrated single-player world).

## MKM creative tab

MKM registers its own searchable Creative Mode inventory tab. The tab is backed by the central MKM item registry, so future MKM items can appear there automatically rather than being manually added to creative inventory code one by one. Until the first content items are registered, the tab uses a vanilla Nether Star as its icon.

## Development

Prerequisites:

1. Install a Java 25 JDK.
2. Clone the repository.
3. Open it as a Gradle project in IntelliJ IDEA or another compatible IDE.

Useful commands:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

On Windows PowerShell or Command Prompt, use `gradlew.bat` instead of `./gradlew` when appropriate.

GitHub Actions builds the project on every push and pull request and uploads the resulting mod JAR as a short-lived workflow artifact after a successful build.

## Engineering principles

- The server is authoritative for gameplay state.
- Common code must be safe on a dedicated server.
- Client code is presentation and input, not the source of truth.
- RPG systems should be modular but remain in one mod codebase until a real boundary requires separation.
- Balance/content data should become data-driven where practical instead of being scattered through Java constants.
- Public APIs and saved-data formats should change deliberately and with migration in mind.
- New systems should enter through small vertical slices rather than large untested framework rewrites.

## License

All Rights Reserved until the project adopts an explicit license.
