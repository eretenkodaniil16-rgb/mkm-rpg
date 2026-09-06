# Character Core

This document describes the first persistent RPG-state implementation. The mechanics are intentionally small; the goal is to prove persistence, synchronization and server authority before broader RPG systems depend on them.

## Storage model

Player RPG data is stored as a NeoForge data attachment registered as `mkm:character`.

The attachment is:

- serialized with a `MapCodec` into player data;
- copied across death/respawn;
- synchronized through NeoForge's attachment synchronization;
- visible only to the owning player during attachment synchronization;
- represented as an immutable Java record so every mutation replaces the attachment and therefore has an explicit synchronization point.

This choice avoids a custom player save file and avoids duplicating state into world `SavedData`, which is not the correct ownership model for entity-specific data.

## Schema v1

Persisted fields:

- `schema_version`
- `experience`
- `strength`
- `dexterity`
- `vitality`

Defaults:

- schema version: `1`
- experience: `0`
- prototype attributes: `10 / 10 / 10`

Attributes are currently clamped to `1..100` as a defensive storage invariant. This range is not a final game-balance commitment.

Level is deliberately not persisted. It is deterministically derived from total experience, which prevents duplicated fields from becoming inconsistent.

## Prototype progression

The first curve is deliberately simple:

```text
Level 1:   0 total XP
Level 2: 100 total XP
Level 3: 300 total XP
Level 4: 600 total XP
...
```

The threshold is triangular: `100 * (level - 1) * level / 2`, currently capped at level 100.

This is test/prototype tuning, not the final RPG progression model. The formula is isolated in `ProgressionRules` so it can change without changing the persistence schema.

## Synchronization and authority

The logical server owns all authoritative character mutations.

NeoForge attachment synchronization is used for server-to-client character state. A custom payload is intentionally not added for this state because the platform already provides lifecycle-aware initial and update synchronization for attachments.

Future client actions such as activating an ability will use narrow client-to-server intent payloads when needed. Those requests will be validated on the server and will never directly author authoritative results.

## Debug/admin commands

The prototype exposes:

```text
/mkm stats
/mkm xp add <amount>
/mkm xp set <amount>
```

`/mkm stats` is available to a player and displays the caller's RPG state.

XP mutation commands require the Minecraft gamemaster permission level and currently affect only the command's player. Target selectors can be added later when there is a real administration use case.

## Next verification

The implementation is considered technically usable only after:

1. `./gradlew build` succeeds in CI;
2. a development client can join a world and use `/mkm stats`;
3. XP survives save, exit and reload;
4. XP survives death/respawn as intended;
5. a dedicated development server starts without client-class linkage failures.
