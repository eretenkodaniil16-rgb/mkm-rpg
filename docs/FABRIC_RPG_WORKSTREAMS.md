# MKM RPG — gameplay workstreams and integration plan

**Status:** planning contract, 2026-11-01. **Branch baseline:** `feature/action-combat-c2-state-hud` (C2.0). This document is deliberately independent of the C3 animation branches.

## 1. Current verified implementation boundary

| System | State | Evidence / important limitation |
| --- | --- | --- |
| Platform | Implemented | Minecraft 26.3, Fabric Loader 0.20.1, Fabric API nested inside the distributed JAR, Java 25; Windows installer and CI build/smoke flow exist |
| Character core | Implemented prototype | Persistent XP and seven primary attributes (STR, DEX, VIT, END, INT, WIL, PER), schema-v2 migration, owner synchronization and Character Sheet |
| Player modifiers | Implemented prototype | Centralized `PlayerModifierSnapshot`; preliminary coefficients, not final balancing |
| Action combat C2 | Tested prototype | Server-owned stamina, three-step R combo/action timeline, G eight-way dodge, heavy attack, poise, HUD. Server validates action requests; gameplay balance not final |
| Animation C2.1/C3 | Separate *draft* PR chain | Data-driven clip library, experimental first/third-person scope and Blender exporter; visual acceptance not complete. Keep animation code isolated |
| Magic, abilities, allocation, equipment modifiers, NPC quests, dialogue, factions, encounters | Planned | No complete end-to-end gameplay loop claimed |

The default `main` branch still contains the original NeoForge starter. Do not develop new features from `main` before integrating the reviewed Fabric baseline. Older documentation mentioning NeoForge 26.2 is historical and must not be taken as the current runtime instruction.

## 2. Shared architecture (one Fabric mod, not a plugin framework)

```text
src/main/java/com/eretenkodaniil/mkm/
  MkmMod.java                  Fabric common bootstrap
  character/                  persistent stats, XP, progression, modifiers
  combat/                     server-owned action timeline, stamina, damage
    net/                      narrow validated combat intent packets
  registry/                   central item and creative-tab registration
  command/                    diagnostics / administrative commands
  client/                     client-only UI, inputs, rendering
    gui/                      Character Sheet
    animation/                action-to-clip mapping (C3 branch only)
  mixin/                      documented hooks only where Fabric events cannot serve
  [abilities, equipment, npc, dialogue, quests, factions, world] 
                              FUTURE packages: create only with implementation
src/main/resources/
  fabric.mod.json
  assets/mkm/                  client presentation, translations, models, clips
  data/mkm/                    future validated data-driven gameplay content
src/test/                     pure-rule unit tests and targeted integration tests
tools/animation/              separate authoring/QA pipeline
distribution/windows/         versioned install artifacts
docs/                         design decisions, test and migration contracts
```

**State ownership:** server owns player progression, ability execution, cooldowns, resource spending, damage, NPC, quests and loot. Client sends *intent*, displays synchronized results and animates. Derived modifiers come from the existing `PlayerModifierSnapshot`, not a second formula engine. Preserve `mkm:*` identifiers and explicit schema migrations.

**Composition:** gameplay `AbilityService` (when created) may consult character modifiers, combat eligibility and a generic resource ledger, but animation clips never execute damage or decide success. `QuestService` (when created) consumes explicit gameplay events and owns its own player/world state. No broad mutable central manager.

## 3. Parallel workstreams and dependencies

| Stream | Objective | Depends on | First acceptance test |
| --- | --- | --- | --- |
| A — Combat presentation | Polish locomotion, attacks, rolls, Blender clips | C2 action-state semantics | In-world F5 review, timing/marker consistency, dedicated-server safety |
| B — Progression and builds | Earn MKM XP during gameplay; acquire and allocate build points; clear respec policy | CharacterData v2, PlayerModifierSnapshot | Earn → level → spend → modifiers change → relog/respawn preserves values |
| C — Ability and magic | Generic validated ability activation; first offensive or support spell; mana/cooldown | B's character contract and existing combat snapshot | Invalid requests rejected; cast → cost → effect → cooldown; no client-authoritative damage |
| D — Equipment and itemization | Equipment-derived modifiers and stable item definitions | Central modifier aggregation + registry | Equip/unequip → deterministic snapshot; relog and dedicated server consistent |
| E — NPC, dialogue and quests | One talkable NPC, one quest state transition, reputation hook only if needed | B rewards + server quest state | Accept → complete → receive one reward → reload → cannot double claim |
| F — Encounters and world | One small authored encounter plus hostile AI prototype | C/D/E minimal implementations | Spawn encounter → complete objective → reward, no global per-tick entity scans |
| G — UI/UX and testing | Character/build UI, ability bar, quests journal and QA | Contracts from B-F | Mouse/keyboard/F5 and both network sides tested |

Streams may be designed in parallel but code should be merged along these dependencies. **Do not implement full schools of magic, class trees, dozens of weapons or world generation before the first playable RPG quest loop.**

## 4. Proposed order of the next milestones

### M1 — Gameplay progression vertical slice (recommended next implementation)

- Establish one server-issued MKM-XP reward event from a real gameplay outcome; keep Minecraft's vanilla XP independent.
- Persist an auditable allocation ledger (earned points, spent points/allocations) with schema-v2 compatibility and explicit v3 migration. Do not infer spent points from editable totals or client payloads.
- Validate spending against available points, caps and current schema on the server; atomic spend and snapshot refresh.
- Add one usable Character Sheet allocation interaction, with explicit preview and confirmation, rather than a debug-only command.
- Decide reward curve, points-per-level and respec behavior as balance decisions before final numerical tuning.
- Tests: load v2 world, earn XP, prevent repeat reward, spend points, reject forged/duplicate/out-of-range client actions, save/reload, die/respawn, dedicated-server boot.

### M2 — Ability / magic minimal slice

- Define stable ability identifiers, input/cast intent, range/target validation, authority, mana or stamina payment, cooldown and state synchronization.
- Integrate one spell with existing INT/WIL magic modifiers and one physical ability with END/STR/DEX where appropriate.
- Add only the UI needed to test these, not a comprehensive spellbook.
- Tests: no free casts, no repeated packet exploit, no effects through walls if unsupported, disconnect/respawn behavior and PvP permissions.

### M3 — One complete RPG interaction

- Add a small authored NPC/dialogue/quest/encounter, an equipment reward and a journal entry.
- Verify an end-to-end loop: discover NPC → choose dialogue → accept quest → fight/resolve encounter → reward → improve build → save/reload.
- Keep quest completion idempotent and world/player state correctly scoped.

### M4 — Scale after prototype review

- Class or classless tree choice, equipment/loot tiers, monster roles, faction standing, larger spell taxonomy, bosses, dungeon pipeline and long-form content.

## 5. Branches, integration and conflict avoidance

- The existing PRs #1–#12 are **stacked**; #12 is **not** a substitute for merging all earlier base branches. They are currently drafts. Treat C2.0 `feature/action-combat-c2-state-hud` as the reviewed, non-animation working base for isolated domain development.
- Animation work remains under `feature/action-combat-c2-1-animations` → `feature/animation-foundation-c3` → `fix/c3-forward-roll-animation-qa` → `feature/c3-2-animation-pacing-unarmed-combos`, with its own acceptance gate.
- Do not alter `ActionTimelineRules`, `CombatService`, animation JSON or mixins from independent progression/equipment branches unless absolutely needed. Keep non-animation changes additive and conflict-minimizing.
- Before final release, reconcile branch ancestry and merge in dependency order after user review and CI, so default `main` finally reflects the Fabric runtime. Do not force-update branches, bypass draft reviews or merge unverified animation work.
- A feature is **done** only after unit checks, migration tests if applicable, dedicated-server startup, client smoke and actual in-game acceptance of its user-visible behavior.

## 6. Decisions intentionally open

- Exact RPG-XP curve and number of build points earned per level.
- Attribute upper bounds for **gameplay** (storage clamp 1–100 is not a balance decision).
- Free/class-based/hybrid builds and whether respec exists.
- Mana regeneration and spell cooldown policy; spell schools and damage types.
- Death penalties, quest multiplayer sharing and NPC persistence scope.

These must be resolved before content-heavy implementations; they do not block the initial systems design.
