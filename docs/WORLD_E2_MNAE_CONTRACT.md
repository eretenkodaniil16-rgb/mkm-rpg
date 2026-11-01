# MKM World E2 x MNAE — semantic NPC presentation contract

**Scope:** world E2 integration branch `feature/world-e2-mnae-m2-hardening`; stacked onto E2 PR #26/E1 PR #18. Authoritative architecture: [MNAE Issue #33](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/33), [MNAE ADR](https://github.com/eretenkodaniil16-rgb/mkm-rpg/blob/docs/mkm-native-animation-engine-design/docs/MKM_NATIVE_ANIMATION_ENGINE.md).

## Core ownership rule

**World Core** owns quest state, NPC behavior, AI decisions, targeting, kill attribution, dialogue checks, PER threshold, reward authorization and persistence. **MNAE** owns rigs, authored clips, blending, pose sampling, viewer network replication, renderer, first/third-person and optional camera motion. **A cue is information about an already authorized world event, not permission to change gameplay.** Never apply damage, stamina charges, quest stage, experience or mob spawning from a pose/clip/marker.

## Implemented semantic contract

The World side defines `com.eretenkodaniil.mkm.quests.presentation.NpcVisualSignals`:

`Intent(actorUuid, rigProfile, cue, startTick, sequence)`

- Actor UUID: real server-owned world entity UUID; for E2 Ilra or the owned raider.
- Rig profile: explicit `mkm:rig/humanoid_v1` for vanilla Villager/Husk; future different skeleton families may use `mkm:rig/creature/<family>` without pretending they have human bones.
- Cue ID: immutable namespaced semantic meaning, **not an animation clip filename**.
- Start tick: source server-world game time for presentation context; MNAE handles client interpolation and network drift.
- Sequence: nonnegative, process-local cosmetic sequence used to reject duplicate/out-of-order viewer packets. Not persisted; reset on restart. MNAE must handle lifecycle epoch/respawn for observers when replicating.
- Priority: advisory semantic default, MNAE makes final blend-layer decision; death overrides gestures, idle/wait never blocks combat.
- `installObserver(Consumer<Intent>)`: deliberate no-op until the MNAE integration adapter is installed. **E2 does not implement any custom clip player, pose mixer, renderer or client animation packets.**

| Cue ID | When World emits or may request | Visual design intent | Authority |
| --- | --- | --- | --- |
| `mkm:npc/idle` | Stable fallback (catalog entry; not broadcast every tick) | subtle breathing/weight shift | cosmetic |
| `mkm:npc/wait` | Player declines/exits dialogue | step back, calm return | cosmetic |
| `mkm:npc/greet` | Player opens validated dialogue with Ilra | orient head/shoulders toward player; short greeting | cosmetic |
| `mkm:npc/talk` | Server-side dialogue fallback | speaking hands/head | cosmetic |
| `mkm:npc/react` | PER 12 investigation successfully records evidence | attentive surprise, point at tracks | cosmetic |
| `mkm:npc/reward` | M2 reward receipt accepted, quest transitioned to rewarded | acknowledgement/gift gesture; *not* a source of XP | cosmetic |
| `mkm:npc/threat` | Raider created successfully, target assigned by vanilla Husk AI | hostile posture/challenge | cosmetic |
| `mkm:npc/death` | Server-confirmed tagged raider death | optional death gesture. If vanilla death animation exists, MNAE must prevent double poses | cosmetic |

Sequence invariants:
- `greet` may be emitted on every legitimate opening of the same dialogue; no quest side effects.
- `react` only after `FordQuestRules.inspect` changes state; PER 10 failure has no earned-track signal.
- `threat` only after a successfully spawned mob and authorized quest acceptance.
- `reward` only after Character M2 `grantExperienceOnce(...)` confirms and quest stage becomes rewarded. Repeated claims never emit a second reward event.
- `death` only after actual Minecraft entity death callback, not a client marker; no hit is performed by MNAE.
- None of these events are persisted as quest state. Disconnect/reload can safely fall back to default idle without replaying old gestures.

## MNAE implementation request

1. Implement an adapter **owned by MNAE**, installed in World `NpcVisualSignals` at server initialization after the MNAE observer sync transport exists. It serializes only whitelisted cue ID/profile/actor/startTick/sequence plus validated world scope, bounded packet sizes; no actor/player XP/quest state in a cosmetic packet.
2. On the client, resolve the actor in its CURRENT level and visual rig; discard missing/out-of-range/world-mismatched/replayed signals without forcing chunk loads, moving hitboxes or applying gameplay.
3. Map `mkm:rig/humanoid_v1` onto MNAE humanoid skeleton once, using a proper vanilla Villager/Husk adapter or custom geometry where needed. **Do not assume PlayerModel's six parts map directly to villager geometry**. Account for held item, armor, left-handed stance, view distance and optional third-party cosmetic rigs.
4. Use a separate `mkm:rig/creature/<family>` bind-pose and bone hierarchy for quadrupeds, birds, crawlers, etc. Do not stretch the humanoid rig to fit them. Common semantic cue IDs need not share clips or skeletal channels.
5. MNAE controls clip priorities, interruption/fades, missing-profile fallback and reduced-motion accessibility. Keep legacy C3 and new MNAE rendering **mutually exclusive** per actor to avoid double transforms.
6. Dedicated server must load without client renderer imports; observer install should be optional. World without MNAE presenter must continue to play identically, simply with vanilla poses.
7. Verify 2 clients seeing the **same NPC** greet/react/reward and raider threat/death in F5 and first person, 60fps capture after MNAE adapter implementation; authoritative quest state must match even if packet is dropped/reordered. Measure CPU/alloc overhead and limit repeated reaction bursts.

## Character M2 integration — actual World E2 grant ordering

This integration branch vendors **upstream unchanged** Character M2/M1 Character source/UI tests in order to CI-test the integration as a whole. Character Core ownership remains M2 branch `feature/character-quest-rewards-m2`.

1. Server validates actual Warden, distance <=6, current `return_to_warden` stage, and authenticated player.
2. Call `CharacterService.grantExperienceOnce(player, "mkm:ford_warden/complete", 75|100)`.
3. On `GRANTED` or `ALREADY_GRANTED`, mark E2 quest `rewarded`, then emit optional `mkm:npc/reward`. On `INVALID_REQUEST` / `LEDGER_FULL`, do NOT change the quest and do NOT emit reward.
4. On resumed/retried claim after a crash between character receipt and quest stage save, `ALREADY_GRANTED` repairs the quest stage without minting XP. Repeated `rewarded` stage never calls grant.
5. The older E2 `reward_xp_floor` field remains in quest v3 saved data for backward decoding only; **auto-repair based on cumulative XP was removed**. Legacy `rewarded` quest saves without a receipt are intentionally not credited automatically, because historical payment status is ambiguous; requires backed-up manual adjudication.
6. XP and build credits remain Character Core authority, with vanilla XP independent. Equipment's item reward is **not implemented**: wait for a durable idempotent Equipment grant API before exposing a new item.

## World/encounter lifecycle status and remaining edge cases

- Authored enemy UUID + Warden UUID persist in player quest v3. `/mkmworld recover` locates an exact loaded entity; it never creates another because a chunk is unloaded. This intentionally protects against duplication but may block an encounter after entity deletion outside AFTER_DEATH; needs dedicated encounter lifecycle stored in SavedData.
- Death of an owned Husk by another player/environment credits the matching quest owner, not the killer. When the owner is offline, a bounded `mkm:ford_encounter_outcomes` world attachment records a tombstone replayed on login.
- Tombstones, world-save and player-save do not form a single ACID transaction. A genuine two-client loaded/unloaded-chunk and abrupt-server-crash smoke is **mandatory**. Unit tests do not prove these lifecycle scenarios.
- M1 still grants separate +25 XP for a valid hostile kill; measure E2 reward as **claim-specific XP delta**, not total since fight.
- Spawn requires local level ground and non-Peaceful difficulty, and is not integrated into automatic worldgen. An optional datapack-defined landmark/structure placement could later instantiate Ilra without replacing vanilla terrain.

## Manual acceptance gate

**Client UI:** Right click tagged Ilra; correct dialogue instead of trade screen; choose decline, PER 10 failure, PER 12 success, accept, locate and claim; stale/fake stage must resync rather than advance; offhand/spectator/distant/duplicate requests fail. At 320x240 scaled GUI and 1920x1080 verify readable buttons and J journal without forced menu interruption.

**Persistence:** save/reload and respawn, legacy quest v1/v2 and Character v3->v4, reward claim/replay and server process restart; two independent player accounts each with their own enemy UUID/receipt.

**MNAE:** no visual engine is installed by this World PR; inspect published cue semantics in a test observer, then use a separate MNAE integration PR for observer network/render integration and filmed verification. Do not call headless build 'visual acceptance'.

**Merge:** requires control chat review of stacked E1 #18, E2 #26, Character M2 and MNAE #33. This feature branch is a code integration demonstration, not permission to merge all upstream drafts into legacy NeoForge main.
