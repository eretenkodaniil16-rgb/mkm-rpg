# MKM

MKM is a large-scale RPG conversion mod for Minecraft. The project targets a middle ground between a total conversion and a CRPG: Minecraft remains the world and interaction foundation, while character progression, combat abilities, quests, dialogue, factions, encounters and long-form RPG progression are built as first-class systems.

## Technology baseline

- Minecraft 26.2
- NeoForge 26.2.0.77
- Java 25
- ModDevGradle
- Gradle Wrapper included in the repository

## Current milestone

`0.0.1 — Foundation`

The current milestone is deliberately technical. Before content production begins, MKM must establish a stable common bootstrap, dedicated-server-safe architecture, build automation, persistence boundaries, networking conventions and project documentation.

See:

- `docs/GAME_DESIGN.md` — product vision and RPG design pillars
- `docs/ARCHITECTURE.md` — technical boundaries and system design
- `docs/ROADMAP.md` — implementation sequence
- `docs/TECHNICAL_DECISIONS.md` — durable architectural decisions
- `AGENTS.md` — rules for AI-assisted development

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

The repository CI builds the project on every push and pull request.

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
