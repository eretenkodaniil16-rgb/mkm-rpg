# Technical Decisions

This file records durable architectural choices. It is not a changelog. Decisions may be superseded, but should not be silently rewritten once implementation depends on them.

## TD-001 — Technology baseline

**Status:** Accepted

MKM targets Minecraft 26.2, NeoForge 26.2.x, Java 25 and ModDevGradle for the foundation milestone.

**Rationale:** The repository was created from the official 26.2 NeoForge MDK and already provides a working modern Gradle/toolchain baseline.

**Consequence:** Version upgrades are deliberate engineering work, not automatic dependency bumps.

## TD-002 — Permanent mod namespace

**Status:** Accepted

The mod id is `mkm` and the base Java package is `com.eretenkodaniil.mkm`.

**Rationale:** Registry/resource/network identifiers need a stable namespace from the start.

**Consequence:** Published identifiers under `mkm:*` are treated as compatibility contracts once saves/content depend on them.

## TD-003 — Server-authoritative gameplay

**Status:** Accepted

The logical server is authoritative for persistent RPG state and gameplay outcomes. The client submits intent and renders synchronized state.

**Rationale:** This is the safest base for multiplayer, dedicated servers, anti-desync behavior and future validation.

**Consequence:** Client-only calculations may be predictive/presentational but cannot become the authoritative source for progression, combat or quest changes.

## TD-004 — Modular monolith

**Status:** Accepted

MKM remains one mod artifact and one Gradle project until a concrete requirement justifies a split.

**Rationale:** Early multi-module decomposition would increase build/API overhead before domain boundaries are proven.

**Consequence:** Modularity is expressed first through package ownership, explicit contracts and tests.

## TD-005 — Dedicated-server compatibility from the first milestone

**Status:** Accepted

Common bootstrap and gameplay domains must be safe to load without Minecraft client classes.

**Rationale:** Retrofitting physical-side separation after substantial UI/gameplay work is high-risk.

**Consequence:** Client presentation code must live behind explicit client-only loading boundaries.

## TD-006 — Data-driven content where it creates leverage

**Status:** Accepted

Content-heavy definitions should become data-driven when doing so improves iteration, validation, packability or compatibility. Runtime invariants remain in Java.

**Rationale:** Large RPG content volumes are expensive to maintain when every tuning change requires code edits, while forcing all behavior into data creates an equally fragile pseudo-language.

**Consequence:** Each subsystem must decide separately what is definition data and what is executable semantics.

## TD-007 — No speculative framework expansion

**Status:** Accepted

The project will grow through vertical slices. Empty package trees, universal managers and abstractions for hypothetical future features are avoided.

**Rationale:** The dominant early risk is building the wrong framework before real cross-system requirements are known.

**Consequence:** Extension points should be added when a second real use case or a clearly established invariant justifies them.

## Pending decisions

The following require implementation research/prototyping before acceptance:

- concrete NeoForge 26.2 persistence API and schema strategy for player RPG state;
- concrete networking registration/payload conventions;
- final attribute/progression model;
- degree of vanilla combat replacement;
- ability resource/cooldown model;
- quest/dialogue data representation;
- compatibility/migration policy for the first public release.
