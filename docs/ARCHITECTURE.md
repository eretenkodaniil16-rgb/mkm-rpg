# MKM Architecture — active Fabric 26.3 baseline

> Updated for the Fabric port and the verified C2 gameplay baseline. The original NeoForge 26.2 decision history remains in `TECHNICAL_DECISIONS.md`, superseded by TD-013/TD-014. See `FABRIC_RPG_WORKSTREAMS.md` for current status, future systems and integration priorities.

## Architecture at a glance

Minecraft 26.3 · Fabric Loader 0.20.1 · Fabric Loom · Java 25 · one `mkm` mod artifact.

MKM is a **modular monolith**: domain logic is split by game system, not separate Gradle subprojects. Only implement new packages when a vertical slice needs them.

```text
client input -> narrowly scoped Fabric C2S request (action intent)
                  -> server validation / domain service
                  -> authoritative RPG state + vanilla game actions
                  -> Fabric attachment owner sync or narrow S2C result
                  -> client HUD / Character Sheet / animation
```

## Existing code responsibilities

- `MkmMod`: common Fabric bootstrap, registries and wiring, not a gameplay god-object.
- `character/`: immutable `CharacterData`, progression rules, attachment lifecycle and `CharacterService`.
- `character/attribute/` and `character/modifier/`: seven primary inputs, definition registry, deterministic `PlayerModifierCalculator` and `PlayerModifierSnapshot`.
- `combat/`: `CombatRules`, `CombatService`, stamina, transient poise, server-owned `ActionTimelineRules`, dodge direction/motion, heavy/combo handling.
- `combat/net/`: Fabric packets carrying **requested actions**, not client-declared outcomes.
- `registry/`: registered items and dedicated MKM creative tab.
- `client/`: Fabric client initialization, input, Combat HUD and `client/gui/CharacterSheetScreen`.
- `mixin/`: targeted hook for vanilla melee scaling; animation branch adds client-only render hooks.
- `src/main/resources/assets/mkm/`: models, translations and, on C3, indexed versioned JSON clips.
- `tools/animation/`: separate Blender export, geometry checks and animation QA.

No implemented generic abilities, quests, NPC, factions or equipment-modifier service yet; their intended contracts appear in `FABRIC_RPG_WORKSTREAMS.md`.

## Persistence and migration

`mkm:character` uses **Fabric API Data Attachments** on the player. `CharacterAttachments.CHARACTER` is declared with `AttachmentRegistry.create`, `persistent(CharacterData.CODEC.codec())`, `copyOnDeath()` and `syncWith(CharacterData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())`.

Current `CharacterData` is immutable, schema v2; fields are total RPG XP and STR/DEX/VIT/END/INT/WIL/PER. Level is **derived**, not saved. Missing v1-era attributes default to 10; a future v3 allocation ledger must have explicit migration and cannot silently discard v2 values. Domain changes replace the attachment via `setAttached`; they do not edit a private client cache.

Combat uses **separate volatile** owner-synced attachments for stamina/timeline; mob poise remains server-owned. Do not persist temporary action state just to simplify client rendering. NPC/quest/world state must use ownership-aware storage rather than overloading the character attachment.

## Authority and networking

Every incoming client command requires independent server checks: action kind, sender/alive state, combat phase, available resource, cooldown, requested direction/target/range, ownership and any world restrictions. Combat performs vanilla damage resolution on the server. Never trust an animation marker or a client-supplied velocity as game truth.

Fabric Data Attachments provide owner-only Character Core and Combat State synchronization. Use explicit Fabric networking for client intent and for events not covered by the attachment lifecycle. Rate-limit state emissions; avoid syncing values the client does not need.

Multiplayer correctness is a requirement for every new game domain. Other players' third-person animations are **not** guaranteed by the current owner-only C3 timeline; this is a known separate animation task, not a combat-authority bug.

## Cross-system contracts

- Character builds, future equipment and buffs contribute through one calculated `PlayerModifierSnapshot`, rather than divergent per-system formulas.
- Future abilities pay costs and apply effects from a server-owned service and reference stable `mkm:*` identifiers.
- Future quest completion is event-driven, idempotent and player-/world-scoped by design; it grants XP/rewards through authoritative reward services.
- The server determines attack ticks and collision. Animation profiles and JSON timeline markers **may represent** action timing but never authorize hits, invulnerability or world movement.
- No speculative universal event bus, generic ORM, universal skill language or empty package families.

## Data, compatibility and performance

Use Java for mechanics/invariants and versioned JSON/data-pack definitions for content-heavy spells, quests and encounters where validation is practical. Document any change to public registry IDs, saved schemas, C2S protocols and game balance coefficients. Existing C3 animation runtime is only experimental until real F5 QA passes.

Keep hot paths bounded: server tick operations, local entity queries and regeneration checks must not require unbounded world scans. Prefer explicit event hooks and measured optimizations.

## Testing and integration

1. Unit-test pure formulas, state transitions, costs and malformed inputs.
2. Test migration v1→v2 and any future v2→v3 with real serialized fixtures.
3. Run Gradle/JUnit and CI checks for bundled Fabric API, headless client and dedicated server.
4. Manually test every new visible feature in Minecraft 26.3 on Windows, including save/reload, death, invalid packets, latency and second-player behavior where relevant.
5. Keep animation PRs and gameplay feature PRs isolated until separately reviewed. `main` currently remains the legacy NeoForge template; do not branch new Fabric features from it.

A successful build is not proof that movement looks correct or that a complete RPG loop exists.
