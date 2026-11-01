# Technical Decisions

This file records durable architectural choices. It is not a changelog. Decisions may be superseded, but should not be silently rewritten once implementation depends on them.

## TD-001 — Technology baseline

**Status:** Superseded by TD-013 for the active Fabric runtime; retained as history

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

## TD-008 — Player RPG persistence uses NeoForge data attachments

**Status:** Superseded by TD-014 for Fabric data attachments

Player-specific RPG state is stored in a registered NeoForge `AttachmentType` on the player entity. The schema uses a `MapCodec`, and the attachment opts into copy-on-death behavior.

**Rationale:** Character state is entity-specific. NeoForge attachments provide native entity ownership, persistence and respawn-copy semantics without inventing a parallel save-file layer or misusing world-scoped `SavedData`.

**Consequence:** `mkm:character` is the persistence root for Character Core. Schema evolution must remain deliberate and versioned.

## TD-009 — Character state uses owner-only attachment synchronization

**Status:** Superseded by TD-014 for Fabric data attachments

Server-to-client Character Core synchronization uses NeoForge attachment synchronization and sends the player attachment only to its owning player.

**Rationale:** The platform already synchronizes attachment creation/replacement and initial state. A second custom synchronization protocol would duplicate lifecycle behavior and create additional desynchronization risk.

**Consequence:** Custom network payloads are reserved for cases the attachment mechanism does not express well, especially validated client-to-server gameplay intent such as future ability activation.

## TD-010 — Character state is immutable and level is derived

**Status:** Accepted

`CharacterData` is immutable. Mutations replace the whole attachment value through `setData`. Total experience is persisted; character level is derived from progression rules instead of stored independently.

**Rationale:** Immutable replacement gives persistence/synchronization a clear mutation boundary. Deriving level prevents duplicated persisted values from drifting apart.

**Consequence:** Balance changes to the prototype XP curve can be isolated in progression rules without changing the stored schema. Any future field that can be safely derived should be evaluated before being persisted redundantly.

## TD-011 — Seven primary attributes and versioned migration

**Status:** Accepted

Character Core uses seven persistent primary attributes: Strength, Dexterity, Vitality, Endurance, Intelligence, Willpower and Perception.

Schema v2 adds END/INT/WIL/PER while preserving the schema-v1 XP/STR/DEX/VIT field names. Missing v2 attribute fields decode to the neutral default `10`.

**Rationale:** Physical durability and stamina economy need separate build axes, as do offensive/technical magic and mana/mental resilience. Perception provides a distinct weak-point/ranged axis without introducing hidden accuracy rolls.

**Consequence:** Future combat and magic systems consume derived modifier snapshots from these attributes rather than adding more ad-hoc persisted combat numbers. Attribute-to-modifier relationships distinguish primary and secondary influence, while numerical coefficients remain tunable Combat Core work.

## TD-012 — Derived player modifier snapshot is the combat-facing character contract

**Status:** Accepted

Combat-facing player values are calculated into an immutable `PlayerModifierSnapshot` from persistent primary attributes and centralized `ModifierDefinition` records.

The snapshot uses explicit units, neutral bases, hard safety bounds and PRIMARY/SECONDARY attribute coefficients. Raw attributes use a shared soft-cap transform before contributing.

**Rationale:** Combat, weapons, spells and future equipment should not each reinterpret STR/DEX/VIT/END/INT/WIL/PER independently. A single derived contract prevents formula drift and lets new contribution sources be added without changing persistent CharacterData.

**Consequence:** Final modifier values are not persisted. Combat Core and later systems should read the snapshot. Equipment, skills, buffs and weapon rules will extend the contribution pipeline rather than writing ad-hoc derived fields into saves.

## Pending decisions

The following still require implementation research/prototyping before acceptance:

- long-term balance tuning of the initial modifier coefficients and soft-cap span;
- ability resource/cooldown model beyond the accepted stamina/mana vocabulary;
- client-to-server payload conventions for interactive abilities and UI actions;
- quest/dialogue data representation;
- whether social/reward systems justify additional persistent attributes such as Presence or Luck;
- compatibility/migration policy for the first public release.

## TD-013 — Active runtime moved to Minecraft 26.3 and Fabric

**Status:** Accepted for active gameplay development

**Decision:** Minecraft 26.3, Fabric Loader 0.20.1, Fabric Loom and Java 25 replace the initial NeoForge 26.2/ModDevGradle baseline (TD-001). The mod ID `mkm` and Java package namespace stay unchanged.

**Evidence:** `port/fabric-26.3` and descendant Fabric branches compile, run dedicated-server and virtual-client CI smoke checks, and distribute nested Fabric API in the mod JAR. The repository's default `main` still contains the original NeoForge template and is not the source of the current gameplay build.

**Consequence:** New gameplay features must use Fabric APIs and a reviewed Fabric branch. Legacy NeoForge documents are historical. Integrate stacked draft PRs in dependency order after review, not via forced replacement of `main`.

## TD-014 — Fabric data attachments preserve the Character Core authority contract

**Status:** Accepted for active gameplay development

**Decision:** `CharacterAttachments.CHARACTER` uses `net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry`, persistent `CharacterData.CODEC.codec()`, `copyOnDeath()` and owner-only `syncWith(CharacterData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())`. Authoritative mutations replace immutable values with `player.setAttached(...)`. The seven-stat saved schema remains v2.

**Rationale:** The Fabric port retains lifecycle and anti-desynchronization requirements from TD-008 and TD-009 without retaining their platform-specific code. Transient combat stamina/timeline is separately attached and owner-synced, not stored in persistent CharacterData.

**Consequence:** Future rewards, allocation, abilities, equipment and quest systems must not edit owner-only state directly on clients. Changes to CharacterData require explicit migration. Persistent quest/world state belongs to the owning scope.
