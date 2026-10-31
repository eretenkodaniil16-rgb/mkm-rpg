# Architecture

## Baseline

MKM currently targets Minecraft 26.2, NeoForge 26.2.0.77, Java 25 and ModDevGradle.

The project should evolve as a modular monolith: one NeoForge mod artifact, with explicit domain boundaries inside the codebase. Splitting into multiple Gradle subprojects is not justified until a concrete boundary requires it.

## Authority model

The dedicated server is the source of truth for gameplay state.

Server-authoritative concerns include, at minimum:

- character level/experience and persistent build state;
- attributes and derived combat values;
- ability eligibility, costs, cooldowns and outcomes;
- damage/healing/status application;
- quest state and world-state flags;
- faction/reputation changes;
- inventory/equipment-affecting actions;
- encounter and boss state where persistence matters.

The client owns presentation, input collection, local interpolation/animation and UI state. A client request is not evidence that a gameplay action is valid.

## Common vs client code

The common entry point must be loadable on a dedicated server without resolving client-only classes.

Recommended direction:

```text
com.eretenkodaniil.mkm
  MkmMod.java
  <domain packages as systems are introduced>
  client/
    <client-only UI, rendering and input>
```

Do not create empty package trees in anticipation of future work.

## Domain boundaries

Expected domains include character/progression, combat, abilities, quests, dialogue, factions, NPC/AI, world/encounters, networking, data and client presentation.

Each domain should own its rules and expose only the contracts needed by other domains. Avoid central "god" managers that know every subsystem.

Cross-domain communication should prefer explicit service calls/events with well-defined ownership over mutable global state.

## Persistence

Player-specific RPG state uses a NeoForge data attachment on the player entity. The first registered root is `mkm:character`.

The Character Core attachment is serialized with a `MapCodec`, carries an explicit schema version and currently opts into copy-on-death. It is represented as an immutable value; authoritative changes replace the attachment instead of mutating hidden fields in place.

Key principles:

- stable identifiers over Java class names;
- version saved payloads from the first schema;
- keep derived values derivable where practical rather than persisting redundant copies;
- distinguish player-scoped, world-scoped and encounter-scoped data;
- define migration behavior before changing persistent schemas after public releases.

The player attachment decision does not imply that every future persistent system belongs on a player. World/campaign state and large encounter state should use storage appropriate to their ownership and lifecycle.

## Networking and synchronization

Character Core server-to-client state uses NeoForge's built-in attachment synchronization. The `mkm:character` attachment is synchronized only to its owning player.

This avoids creating a second protocol for a lifecycle that the attachment system already understands. Replacing the immutable attachment through `setData` is also the synchronization boundary for normal Character Core mutations.

Custom payloads should be introduced only when needed, especially for client-to-server intent or for state that does not fit attachment synchronization.

Rules for custom networking:

- validate all client-originated requests server-side;
- prefer intent messages ("attempt ability X at target Y") over client-authored results ("deal 20 damage");
- keep payloads versionable and narrowly scoped;
- avoid syncing information the client does not need;
- separate initial/full synchronization from incremental updates when state size justifies it;
- never use network timing as an implicit game-state transaction model.

## Data-driven content

Content-heavy systems should support externalized definitions where practical. Likely candidates include abilities, classes/perks, dialogue, quests, encounter definitions and balance tables.

Data-driven does not mean "everything in JSON". Complex invariants and runtime behavior remain code. Definitions should describe content; Java should enforce semantics and validation.

## Registries and identifiers

`mkm` is the permanent project namespace unless a deliberate breaking decision is made.

Registry identifiers should be treated as compatibility contracts once published. Rename/migration decisions must be explicit.

## Testing strategy

The project should use multiple levels of verification as systems appear:

- ordinary Java unit tests for pure rules/calculations;
- NeoForge/Minecraft GameTests for world-integrated mechanics;
- client dev runs for UI/rendering/input;
- dedicated-server startup/integration checks for common code;
- GitHub Actions as the minimum build gate.

Pure domain rules should be kept sufficiently decoupled from Minecraft objects where that materially improves testability, but not at the cost of unnecessary abstraction.

## Performance model

Minecraft is tick-driven. Avoid per-tick global scans, repeated allocation-heavy queries and unbounded work across entities/players.

Optimize from profiling and known algorithmic risks. Do not prematurely build caching layers without measured or obvious need.

## Invasive techniques

Mixins, access transformers, reflection and bytecode-level hooks are escalation mechanisms, not defaults. Prefer public Minecraft/NeoForge APIs. Any invasive technique should document:

- why public APIs are insufficient;
- compatibility risk;
- failure mode on version update;
- how the behavior is tested.

## Foundation success criteria

The `0.0.1` foundation is complete when the project can demonstrate:

1. clean MKM identity and package namespace;
2. reproducible CI build;
3. client dev startup;
4. dedicated-server-safe bootstrap and server startup;
5. a working player persistence path ready for Character Core iteration;
6. a working synchronization path for player RPG state;
7. project rules/documentation sufficient for further AI-assisted development.
