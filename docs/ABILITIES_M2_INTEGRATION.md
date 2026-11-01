# MKM RPG — Magic M2 integration proposal (draft, 2026-11-01)

Base: PR #19 `feature/abilities-m1-renewal`. This is a **stacked draft** and is not a merge approval.
Platform: Minecraft 26.3 / Fabric / Java 25. Current source of truth for general gameplay remains the C2
Fabric baseline; Progression #15, Equipment #16/#22/#23 and C3.7 #25 are concurrent drafts.

## Findings from inspected code

| Source | Actual contract | Effect on magic |
| --- | --- | --- |
| Progression #15 | `CharacterData` v3, earned/allocation points; `CharacterService` owns server updates | No learned-spell ledger or unlock events. **Do not add independent persistence** |
| Equipment #16/#23 | `EquipmentSnapshots.snapshot(player, character)` composes gear into the central `PlayerModifierCalculator.calculate(data, additiveDeltas)` with one final clamp | Use **this exact snapshot** when Equipment D1 has been integrated; do not apply gear bonuses a second time |
| Equipment D1 `EquipmentRules` | Vanilla weapon/armor bonuses are *physical* and block-related | No CURRENT mana/magic bonuses from vanilla iron/diamond gear; this is intentional neutral behavior |
| Equipment D2/D3 | `CombatService` and `CombatEvents` read the equipped snapshot; D3 binds heavy provenance to source and tick | Must not use D2 physical resistances to resolve magical damage |
| C3.7 | `ActionTimeline.Kind` has NONE, LIGHT_COMBO, DODGE; `Phase` has IDLE/WINDUP/ACTIVE/RECOVERY/DODGING; extra dodge direction and animation events | There is **no SPELL phase**. Magic must not add a parallel timeline or claim C3 visuals |

## Runtime changes in this M2 draft

- `AbilityModifierAccess.snapshot(player)` is the **single call site** for resolving Character-derived stats for AbilityService and the ability HUD. It currently delegates to the M1 `PlayerModifierCalculator.calculate(CharacterService.get(player))` because the Equipment branch is unmerged and is not a dependency of the M1 stack.
- **Mandatory integration patch after PR #16 is accepted:** replace that adapter's body with
  `EquipmentSnapshots.snapshot(player, CharacterService.get(player))` and add the import.
  No change to the constructor/schema of `CharacterData`, Combat or the equipment engine.
  Integration must run a dedicated JUnit test in the combined branch comparing the real
  `EquipmentSnapshots.snapshot(player, character)` against the ability HUD and cast values.
- `RenewalParameters.from(PlayerModifierSnapshot)` resolves max mana, regen, cost, delay,
  cooldown and healing once. The same record drives server and informational HUD. Only
  snapshot IDs influence formulas; it creates no alternative INT/WIL mechanics.
- `AbilityAccessRules` describes grant composition and keeps **separate** grant
  eligibility vs executable-handler registration. Renewal remains freely accessible for M1;
  having an ID in a learned or equipment set **does not** make an unimplemented spell executable.
- `ArcaneBoltTargetRules` is a pure defensive predicate.
- `ArcaneBoltTargetProbe.acquire(caster, stats, combatPermission)` is a **read-only server-side**
  raycast preparation using the player's current eye/look vector, the `SPELL_RANGE`
  snapshot modifier, block collider ray clip, first intersected living entity and
  an obligatory Combat-owned permission predicate. An excluded entity blocks the
  projectile rather than allowing an enemy behind it to be selected. Nothing
  calls this probe from `AbilityService` yet: there is **no registered damage
  or spell activation path**, no mana debit, no cast timeline and no hit VFX.
  In the combined Combat integration branch the permission predicate must
  implement real PvP configuration, team/faction protection, target immunity and
  PvE permissions; until approved it must never default to allow-all.

## Proposed Character Progression contract (requires B-owner approval)

Progression shall own a server-authoritative, persistent learned-ability-ID set plus
an explicit revision and migration of its own character schema (v4 or a reviewed
character-owned attachment). Equipment may provide **ephemeral** grants without
saving item-granted IDs as permanent character unlocks. Active access:

`registeredDefinition && registeredExecutableHandler && (publicGrant || learnedGrant || equippedGrant)`

All ID transitions, prerequisites, level gates, point spending, refunds and revision
validation belong to Character Core. The ability domain never infers learned spells
from raw INT/WIL or copies the Character allocation ledger. Avoid assigning class
or school prerequisites before the build design has been approved. For passives,
effect modifiers must be emitted into the *central* shared modifier aggregator,
never mutate `PlayerModifierSnapshot` or `CharacterData` directly.

## Proposed Combat Core cast transaction (requires Combat-owner approval)

**Only one action timeline:** the Combat Core C2 timeline continues to own action
occupancy, speed, interruption and recovery; Magic only stores mana/cooldowns and
ability definition parameters. Proposed server API:

1. `tryBeginAbility(player, abilityId, castTicks, recoveryTicks)` — verifies not
   comboing/dodging/heavy-recovering and reserves the existing action timeline;
   returns an opaque server-only cast token/ordinal. **Do not change current
   combat timing tables**.
2. On successful begin, Magic validates the learned/equipped grant and cost using
   *server* snapshot, atomically debits mana, reserves a cooldown. Policy proposal:
   charge and start cooldown at BEGIN; no refund on interruption (prevents zero-cost
   interruption spam). This is a **proposal**, not implemented for noninstant spells.
3. Combat owns `CAST_WINDUP`; Magic is notified at the single authoritative
   `RELEASE` transition with the original cast token. Revalidate live
   target/range/occlusion/permission before damage; if invalid, fizzle with no effect.
   A target can never be trusted only from the original packet.
4. Combat owns `INTERRUPTED` and `RECOVERY` transitions. On death/logout/dimension
   travel, cancel in-progress casts without creating a second action timeline.
5. Durable mana and cooldown state remain in `mkm:ability_resources`; ephemeral
   cast phase/token remain in Combat Core's volatile authority. One token can execute
   at most once; duplicate/delayed release messages do not deal repeated damage.

Need to explicitly decide which current vanilla LMB interactions may cancel or coexist
with spell casting, how stagger/guard interruption works, and whether cast speed is
rounded up per action to avoid zero-tick/animation desync.

## Proposed damaging-spell contract (requires Combat-owner approval)

Only Combat Core may expose an approved magical-damage entry point using the
standard Minecraft `DamageSource`/hurt pipeline, with `DamageType` (namespace
`mkm:arcane` subject to lore review), a single attacker identity and a confirmed
target. It must preserve vanilla armor/shield/invulnerability/team/PvP conventions
according to explicit damage-type policy and must not apply `PHYSICAL_POWER`,
`MELEE_DAMAGE` or D2 physical typed resistances. Player magical offense is based
on the **existing** snapshot: `MAGIC_POWER`, `SPELL_DAMAGE`,
`MAGIC_PENETRATION`, defender `MAGIC_RESISTANCE`; elemental resistance is
**not** automatically applied until schools/damage tags are finalized.

The cast-side contract must supply the *cast-time attacker snapshot* so switching
equipment while a projectile travels cannot change its attack scaling; defender
resistances are resolved at actual impact. No manual `setHealth` damage, no ad-hoc
armor bypass and no direct `player.attack(target)` disguised as a spell.

**Status:** no Combat implementation has yet committed to these signatures;
therefore the offensive spell stays **disabled** in the executable allowlist.

## Arcane Bolt M2 next playable milestone

`mkm:arcane_bolt`: existing definition; proposed 12-block BASE_RANGE before
the existing `SPELL_RANGE` modifier, 14 base mana, 8 cast ticks, 30 cooldown
ticks, 4 base magnitude (all provisional). Avoid hardcoded target UUID/client
coordinate in the current `CastAbilityRequest`; server should raycast from
current eye/look, clip against blocks, select the *first* living target and
verify target id, dimension, collision/occlusion, PvP config/teams and true
reach before RELEASE. If no legally hittable target, never deal damage.
Await damage contract before enabling or applying any attack effect.

## Animation presentation contract (requires Animation-owner approval)

Do not infer spell impact from a keyframe. Combat emits stable **server-confirmed**
cast transitions `STARTED`, `RELEASED`, `INTERRUPTED`, `FINISHED`, each
with ability ID and opaque cast token or increasing cast sequence. Clients map
those signals to spellbook/hand pose, VFX and audio; the animation renderer
cannot execute gameplay damage or pay mana. Spell impact/hit-confirm VFX are
triggered by authoritative damage results, not a local release marker.
Extend C3 clip profiles **only** after the Combat event names/cast timings
have been accepted. Owner-only cast sync and third-party observer presentation
need separate network decisions.

## QA gates

- M1 Renewal: unit numbers, use at full health and during heavy/combo/dodge,
  duplicate packet refusal, snapshot parity, cap/regen delay and resource sync.
- Persistence: versioned M1 schema 0 -> 1, unknown future fail closed, logout,
  world save/reload, death copy, dimension transfer, clock rollback.
- Network: max-size ID, malformed/truncated IDs, forbidden namespace, stale
  requests, owner-only state and arbitrary third-party packets.
- Next integration build: character M1 v3 save migration, equipment D1 snapshot
  with gear bonuses, animation C3.7 action timings unchanged, dedicated server +
  Xvfb client and actual multi-user gameplay.
- Actual game simulation, manual visual inspection and multiplayer state
  privacy **cannot be proven** by build/boot smoke tests alone.

**Review gate:** PR must remain draft until B/D/Combat/Animation contracts have
been checked, CI green and in-world Windows/F5/low-latency + high-latency
multiplayer accepted. Do not merge independent branches in this workstream.
