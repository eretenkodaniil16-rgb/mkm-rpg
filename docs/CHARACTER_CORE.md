# Character Core

This document describes the persistent RPG-state implementation used by MKM. The current goal is to keep persistence, synchronization and server authority stable while the RPG formulas continue to evolve.

## Storage model

Player RPG data is stored as a NeoForge data attachment registered as `mkm:character`.

The attachment is:

- serialized with a `MapCodec` into player data;
- copied across death/respawn;
- synchronized through NeoForge attachment synchronization;
- visible only to the owning player during attachment synchronization;
- represented as an immutable Java record so every mutation replaces the attachment and therefore has an explicit synchronization point.

## Schema v2

Persisted fields:

- `schema_version`
- `experience`
- `strength`
- `dexterity`
- `vitality`
- `endurance`
- `intelligence`
- `willpower`
- `perception`

Defaults:

- schema version: `2`
- experience: `0`
- every primary attribute: `10`

Attributes remain clamped to `1..100` as a defensive storage invariant. This is not a final balance commitment.

Level is not persisted. It is deterministically derived from total experience.

### Migration from schema v1

Schema v1 stored only STR/DEX/VIT. Schema v2 keeps those field names and adds END/INT/WIL/PER as optional codec fields with default value `10`.

Therefore an existing schema-v1 save can be decoded without a destructive reset:

- existing XP/STR/DEX/VIT values are retained;
- END/INT/WIL/PER materialize at `10`;
- the in-memory record is normalized to schema v2;
- the next persisted replacement writes the complete v2 state.

## Primary attributes

The accepted primary set is:

- Strength (STR)
- Dexterity (DEX)
- Vitality (VIT)
- Endurance (END)
- Intelligence (INT)
- Willpower (WIL)
- Perception (PER)

The detailed role and modifier influence graph live in `PRIMARY_ATTRIBUTES.md` and `PLAYER_MODIFIERS.md`.

## Synchronization and authority

The logical server owns all authoritative character mutations.

NeoForge attachment synchronization is used for server-to-client character state. A custom payload is intentionally not added for this state because the platform already provides lifecycle-aware initial and update synchronization for attachments.

`CharacterService` is the server-facing mutation boundary for XP and primary attributes. `CharacterData` remains immutable and exposes replacement helpers rather than mutable fields.

Future client actions such as activating an ability will use narrow client-to-server intent payloads when needed. Those requests will be validated on the server.

## Prototype progression

The current XP curve remains deliberately simple:

```text
Level 1:   0 total XP
Level 2: 100 total XP
Level 3: 300 total XP
Level 4: 600 total XP
...
```

The threshold is triangular: `100 * (level - 1) * level / 2`, currently capped at level 100.

## Character Sheet

The Character Sheet now displays all seven synchronized primary attributes. It also shows one simple derived preview for each attribute so migration/sync and qualitative build roles can be tested before Combat Core makes modifier formulas authoritative.

The preview layer must not be treated as combat balance. It exists to validate the data path and UI.

## Debug/admin commands

The prototype exposes:

```text
/mkm stats
/mkm xp add <amount>
/mkm xp set <amount>
```

Command runtime registration still requires in-game verification on the target NeoForge installation.

## Verification still required

1. `./gradlew build` succeeds in CI;
2. a NeoForge client loads MKM and opens the Character Sheet;
3. an old schema-v1 world loads without losing XP/STR/DEX/VIT;
4. END/INT/WIL/PER appear at default `10` after migration;
5. save/exit/reload preserves all seven attributes;
6. death/respawn preserves all seven attributes;
7. a dedicated development server starts without client-class linkage failures.
