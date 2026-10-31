# AGENTS.md

This repository is developed with substantial AI assistance. Agents must optimize for correctness, maintainability and compatibility, not for the amount of code produced.

## Read before changing code

Before implementing a non-trivial feature, read the relevant parts of:

- `docs/GAME_DESIGN.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/TECHNICAL_DECISIONS.md`

Inspect the existing implementation before proposing a replacement. Do not create parallel frameworks for capabilities the project already has.

## Core constraints

1. Target Minecraft 26.2, NeoForge 26.2.x and Java 25 unless a deliberate technical decision changes the baseline.
2. Preserve dedicated-server compatibility. Common/server code must not import or reference client-only Minecraft classes.
3. The server is authoritative for RPG state, combat outcomes, progression, quests, inventory-affecting actions and other gameplay decisions.
4. Treat network messages as untrusted input. Validate sender state, permissions, ranges, identifiers and requested actions on the server.
5. Prefer a modular monolith. Separate domains with packages and interfaces where useful, but do not create independent subprojects or abstraction layers without a concrete need.
6. Prefer composition and explicit domain services over large inheritance hierarchies.
7. Keep registry/bootstrap classes small. Domain logic does not belong in the mod entry point.
8. Prefer data-driven definitions for content and balance values when that improves iteration, datapackability or compatibility.
9. Do not add Mixins or Access Transformers merely for convenience. Use public NeoForge/Minecraft APIs first. Any invasive hook requires documented justification.
10. Do not silently break save formats, registry identifiers, network protocol assumptions or public data schemas. Add migration/versioning strategy when those become persistent contracts.

## Change discipline

- Keep each PR focused on one milestone or coherent subsystem.
- Avoid unrelated refactors while implementing a feature.
- Do not mass-create empty packages/classes for hypothetical future systems.
- Add the smallest stable extension point that current requirements justify.
- Update architecture/decision documentation when a change introduces a durable constraint.
- If documentation conflicts with working code, investigate which is authoritative and resolve the inconsistency explicitly.

## Verification

Before considering a coding task complete:

1. Run `./gradlew build` or the Windows equivalent.
2. Run relevant GameTests/unit tests when present.
3. For changes touching client code, verify a client dev run.
4. For changes touching common/server code, verify dedicated-server startup when practical.
5. Check that no client-only imports leaked into common/server packages.
6. Review logs for registry, networking, serialization and data migration warnings.

Do not claim verification that was not actually performed. If the execution environment cannot run Minecraft/Gradle, state that limitation in the PR description.

## Package direction

The current base package is `com.eretenkodaniil.mkm`.

Expected domains will emerge incrementally, likely including character/progression, combat, abilities, quests, dialogue, factions, NPC/AI, world/encounters, data, networking and client presentation. Create a package only when the corresponding implementation exists.

Client-only code belongs beneath `com.eretenkodaniil.mkm.client` or another clearly client-scoped package.

## Design priority

When several implementations are viable, prefer the one that is:

1. correct on multiplayer/dedicated server;
2. testable;
3. explicit about ownership of state;
4. compatible with save migration and future content growth;
5. simple enough to understand without reconstructing hidden coupling;
6. performant enough for Minecraft's tick model without premature optimization.
