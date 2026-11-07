# Magic M4 — one effect engine, multiple activation policies (draft)

**Date:** 2026-11-08. **Branch:** `feature/abilities-m4-unified-actors-ai`,
stacked on Magic M3 integrated QA PR #42 (based on Fabric #30, CharacterData v3).
The standalone Magic M3 PR #37 has CharacterData v2 and MUST NOT be used over
existing v3/v4 worlds. **No changes to CharacterData, combat damage/timeline,
AI movement/attack goals or MNAE rendering have been made.**

## Architectural comparison

| Criterion | Player/Mob spell engine duplication | One effect executor + activation policies (chosen) |
| --- | --- | --- |
| Server authority | Two independent validations can diverge | Both call the same pure spend/cooldown plan and native effect |
| Class/race / AI grants | New unrelated catalogs, unsafe drift | One `AbilityCatalog`, separate validated grant contexts |
| Target/PvP/obstruction | Duplicate raycasts and damage hooks | Shared proof contract with server-only fact provider; unsupported shapes deny |
| Resource/cooldown | Two inconsistent balance implementations | Same `ManaLedger` + `AbilityRules`; *separate actor-specific persistent attachment* |
| Interrupt | Multiple competing action timelines | Player Combat owns ActionTimeline; Mob's vanilla behavior owns AI decisions; instant Renewal has no invented windup |
| Performance | Two tick loops/effect dispatchers | One small 20-tick AI sample on tagged mobs near players, capped per player |
| VFX | Client-driven effects can fabricate success | APPLIED only after positive server health change; MNAE receives semantic event only |
| Persistence | New character class fields in Magic | None; only Character Core can migrate class/race/grants |

**Decision:** common effect mechanism, separate trusted actor adapters.

## What is functional in this prototype

1. `RenewalCastPlan.spendIfAllowed(...)` is a *pure* transaction plan for
   **both** Player and hostile mob. Checks actor, busy/interruptibility, health
   deficit, regen, cooldown, mana balance using existing `RenewalEligibility`
   and `AbilityRules`. Returns immutable paid ledger, or null without mutation.
2. `RenewalEffect.healSelf(LivingEntity, float)` is the **single native
   server-side healing effect**. Returns actual positive HP delta. Caller
   persists the paid ledger *before* applying it. Only a positive actual
   delta can produce `APPLIED`, not a cast request or animation marker.
3. Existing `AbilityService.tryCast(ServerPlayer, ...)` retains Combat's
   `ActionTimeline.idle()` and `heavyReadyTick` conditions, uses
   `AbilityModifierAccess.snapshot(player)` delegating to combined
   `EquipmentSnapshots`; preserves `RenewalParameters` derived from
   `PlayerModifierSnapshot` **exactly once**. Added one-request-per-server-
   tick actor-local throttle; native mana cooldown provides durable duplicate
   rejection across relog. No C2S changes.
4. **Opt-in hostile healer** (existing vanilla hostile `Monster`, *no new
   entity*, no client-side caster control):
   - Explicit scoreboard tag `mkm_arcane_healer`.
   - Existing vanilla AI must currently target a living, nonspectator
     `ServerPlayer` in the same world, within 16 blocks and visible.
   - Server scans at most once per 20 ticks; only within 16 blocks of active
     players; checks up to 32 tagged candidates per player per scan; duplicate
     mob UUIDs are suppressed. No arbitrary loaded-world scan.
   - Caster must be alive, active AI, not a passenger or using an item,
     and below max HP. It uses the same `ManaLedger` and `AbilityRules`
     via `AbilityAttachments.HOSTILE` (persistent, **server-only**, no
     `copyOnDeath`/owner sync). Base hostile mana 100, regen 1/s, Renewal
     cost 20, cooldown 60 ticks, healing 6 HP, regeneration delay 40 ticks.
     Player's current INT/WIL/equipment are **never** stolen by AI.
   - Returns actual native positive healing. No damage/knockback/attack events.
     Server builds a semantic `AbilityVisualEvent` via
     `HostileRenewalVisuals`. Current observer bridge is a **no-op** until
     a reviewed MNAE-5 observer transport subscribes; it does **not** misuse
     the owner-only `AbilityVisualPacket` from Magic M3.
5. Passive and active **catalog-only** grants defined by
   `AbilityArchetypes` + `AbilityGrantPolicy`, without class-state
   migration or unreviewed passive modifier application.

To smoke-test on a disposable world with the M4 prototype on matching
client/server: summon a hostile zombie, run
`/tag @e[type=minecraft:zombie,sort=nearest,limit=1] add mkm_arcane_healer`,
let the zombie legitimately target a survival-mode player and damage
the zombie. With a valid live target and line of sight, it can use
server-native Renewal on itself; it must not heal a player or damage
anyone. Remove the tag to disable. The prototype uses the normal
vanilla mob goal and no autonomous ranged caster attack or new 3D rig.

## Warrior / Mage / race matrix (proposals, NOT a class feature release)

| Future grant | Descriptor | Effect policy now |
| --- | --- | --- |
| Warrior class `mkm:warrior` | `mkm:iron_will` passive: *proposed* +5% stability/resolve; final affected PlayerModifierId subject to Character/Combat review | Catalog only; NEVER applied to stats or combat |
| Mage class `mkm:mage` | `mkm:arcane_bolt` class active, ray targeting, mana and 8-tick future cast; class signature planned | Catalog only; server executor **disabled** until reviewed Combat magic DamageSource/permission contract |
| Stoneborn race `mkm:stoneborn` | `mkm:stone_resilience` passive +5% defensive concept | Descriptor only, no active handler / side effects |
| Windborn race `mkm:windborn` | `mkm:ancestral_focus` active self-barrier concept | Descriptor only; requires separate barrier handler + Combat authorization |
| Baseline player | `mkm:renewal` | **Executable**, retains M3 backward compatibility before class selection |
| Hostile with explicit server tag | `mkm:renewal` | **Executable** via same engine, AI-only guard, server-private ledger |

These are at most two class and two race examples, not full trees.
An ability being *granted* is never sufficient for `executable`.
No data pack, race, equipment or serverbound ID can enable an unimplemented
damaging effect or passive stat application.

**Character team integration API** (without changes here):
supply `AbilityGrantPolicy.Grants(PLAYER, authoritativeClassId,
authoritativeRaceId, persistedLearnedIds, validatedEquipmentGrants, Set.of())`
from Character Core after v4-compatible class/race migrations. The
current v3/v4 CharacterData structures DO NOT persist class/race,
learned spells, or granted abilities; Magic must not fabricate them.
Class/race IDs here are preview constants, not an approved racial
taxonomy. Resolve passive bonuses through Character's **one**
PlayerModifierCalculator delta aggregation before its final clamp;
do not add another stat source in Magic.

## Target, resource, interruption, visual interfaces

- `AbilityTargetPolicy.allowed(definition, evidence, rangeBlocks)`:
  **SELF** needs verified actor==target, alive and same dimension. **ENTITY_RAY**
  needs Combat-owned permission, unobstructed block ray, first intersected
  entity, alive/different actor/same dimension, finite nonzero reach
  <=64 blocks and finite squared distance within reach. **All other
  shapes fail closed.** These facts must come from *server* raycast
  and Combat permission checker, not client intent. Existing
  `ArcaneBoltTargetProbe` remains read-only ServerPlayer-specific and
  requires Combat's permission predicate. Until that is signed off,
  `arcane_bolt` is never executable and causes zero damage.
- **Cost/cooldown**: `ManaLedger` immutable debit in
  `RenewalCastPlan`; player uses owner-only persistent ledger, mob
  uses independent **attachment instance of the same schema/rules**.
  No class/race state stored in either.
- **Interrupt**: existing instant Renewal has `castTicks=0`. Player
  Combat action busy/heavy recovery makes cast invalid. For future
  windups, Combat must define STARTED/INTERRUPTED/RELEASED/FINISHED on
  the *existing* ActionTimeline and authoritative once-only token,
  including death, item swap, unload, dimension switch, server cancel.
  AI may choose to start only when its server AI goal is valid; it
  cannot separately implement delayed combat damage timing. No new
  server cast timer exists in this PR.
- **MNAE**: player uses v2 `AbilityVisualEvent(mkm:renewal,
  mkm:renewal_cast, actorId, sequence, MAIN_HAND, APPLIED, serverTick,
  visualDurationTicks=14, dimensionId)`; M3 client inbox controls
  replay/expiry/onStop. **Logical** main hand, not physical right arm.
  Magic authors no Blender gestures or skeleton controller. AI uses
  **the same semantic event contract** but sends only into a server
  observer bridge with no network broadcast until MNAE-5 establishes
  tracking permissions, replay isolation, and remote-client pose API.
- **Combat owner:** magic damage must use native Combat DamageSource,
  magic-specific mitigation and PvP/team checks, not `player.attack`
  hacks or duplicated physical damage. Never infer damage from MNAE
  animation markers.

## Save and integration gates

- This branch is stacked directly on **#42 CharacterData v3**. It does
  NOT contain CharacterData v4 `claimedRewardIds`, and must **never**
  be used to load/save an existing v4 world. No actual CharacterData
  file was edited. On future reviewed integration, start from a
  v4-compatible project baseline and port the generic Magic API,
  retaining all reward receipts, allocated points and existing
  `EquipmentSnapshots` behavior. Do not merge these independent
  stacked feature trees without review.
- Exact tests: class/race/AI grants and denylist; actor/scope/LOS/range
  validation; pure hostile/player identical cost/cooldown; duplicate
  cast rejection, full health, busy, low mana, and future Arcane Bolt
  never executable; existing M3 packet/dimension/visual lease tests.
  Java25 Gradle/JUnit, dedicated Fabric server, Xvfb client, Windows
  artifact packaging. No in-world zombie AI/observer/F5 acceptance
  from headless CI.
- Manual: real hostile targeted AI and LOS with obstacles; 2 players;
  relog/restart with tagged hostile persisted ledger; save migration
  v3/v4 in separate copied worlds; debug MNAE visual availability.
  Confirm no extra damage, phantom APPLIED or unrelated mob changes.
- Keep draft PR, no changes to legacy NeoForge main.
