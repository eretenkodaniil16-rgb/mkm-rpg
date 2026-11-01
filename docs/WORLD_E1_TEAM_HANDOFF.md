# MKM World E1 — coordination handoff to control chat and subsystem leads

**For forwarding after all parallel chats have read their relevant branches.** This is a **proposal + implemented draft**, not approval to merge.
**Source:** [draft PR #18](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/18), branch `feature/world-e1-ford-warden-quest`, base `feature/action-combat-c2-state-hud`.
**Authoritative design:** `docs/GAME_DESIGN.md`, `docs/FABRIC_RPG_WORKSTREAMS.md` on branch `docs/fabric-26-3-gameplay-workstreams`.

## Executive decision

Keep **one** reproducible, authored mini-scenario before expanding worldgen or NPC libraries. The E1 implementation demonstrates a server-owned quest state, tagged vanilla NPC/hostile encounter, an attribute check, dialog choice, persistent completion and Character Core XP. Existing code stays isolated from the action combat animation line.

Do **not** merge this PR solely because CI is green: in-game interaction/persistence/multiplayer acceptance, dependency contracts and merge ordering are still outstanding.

## Actual branch snapshot (checked 2026-11-01)

| Stream | Current branch | World E1 integration constraint |
| --- | --- | --- |
| B / character | `feature/character-progression-m1` | v3 attribute allocation + 25 XP per eligible hostile kill; E1 directly uses `CharacterService.addExperience` for quest XP |
| C / magic | `feature/abilities-m1-renewal` | first cast `mkm:renewal` only; E1 must not fabricate mana or spell-based damage |
| D / equipment | `feature/equipment-weapon-modifiers-d1` → `feature/equipment-guard-mitigation-d2` | vanilla weapon profiles and shield guard; **no quest-grade idempotent equipment grant yet** |
| A / animation | `feature/action-combat-c2-1-animations` and descendant C3 branches | presentational only; E1 does not change combat choreography |
| E / world | `feature/world-e1-ford-warden-quest` | one pilot quest; keep its state player-scoped, no global world ticks |

**Important:** these are concurrent drafts, not merged features. Do not assume D1/D2 or Character M1 are present in the E1 build or in `main`. Current `main` is historical NeoForge; Fabric C2 remains the branch baseline.

## Required contract votes / requests

| Owner | Concrete contract required | Why / acceptance |
| --- | --- | --- |
| **B Character** | `grantExperienceOnce(player, rewardId, amount)` with persistent keyed redemption and migration of E1 `rewarded` states; use existing v3 schema and derived snapshot | Cross-attachment failure must not lose or double-issue XP; post-claim build points through existing M1 |
| **D Equipment** | `grantEquipmentOnce(player, rewardId, equipmentId)`, using existing central item definitions; explicit full-inventory handling (pending claim, durable mail, or drop policy) | Adds real material award without parallel item or rarity system; repeated claim must not duplicate loot |
| **Combat** | `EnemyDefeated(killer, target, context, encounterId)` or narrow verified attribution hook; decide player projectile/pet/DoT support | No rewriting damage rules; avoid false positives and missed non-melee kills |
| **Encounter/AI** | server-owned encounter key/entity UUID, despawn/chunk unload recovery, bounded roaming and spawn-site validation, non-farmable recover | Replace temporary 2-recovery limit and 48-block search without global tick scans |
| **UX / Dialogue** | authenticated `DialogueChoiceIntent(npcEntityId, dialogueNodeId, choiceId)`, server validates range, stage, PER, sender and node; owner-only quest journal sync | Replace command-only choices without trusting client-submitted success/attributes |
| **Factions** | idempotent `QuestFinalized(playerId, questId, outcomeId)` consumer, world-vs-player reputation scope | Only after a second meaningful faction outcome; no ad hoc counter in `FordQuestState` |
| **World state** | documented player/world/party ownership; future shared bridge / settlement flags via world saved data | Avoid storing a shared 'bridge secured' flag separately on every character |

Do not rename existing persisted identifiers without explicit migration: `mkm:character`, `mkm:ford_quest`, `mkm:combat`. Keep `mkm:*` IDs for gameplay definitions. Provisional Java signatures above are **requests**, not APIs already implemented.

## Game design decisions to approve collectively

1. **Quest ownership:** per player for E1 (implemented). For campaigns, choose shared/party state and how PvP/competing outcomes interact.
2. **Dialogue checks:** guaranteed threshold (implemented PER >=12) versus random roll or hybrid. Recommend deterministic for E1; any future random check must be server-side, disclosed and tested.
3. **Completion:** single use forever versus repeatable quests. Recommend single use for authored narrative; repeatable bounties need separate identity/reward keys.
4. **World integration:** manually placed pilot now; later biome-appropriate authored landmarks and optional datapack-defined placement, never mandatory global world replacement.
5. **Rewards:** 75/100 MKM XP prototype. Together with B's 25 hostile-kill XP, a fresh level-1 player reaches 100 XP after kill + claim (unless changes in progression baseline). Tune once branches integrate.
6. **NPC dialogue UI:** suggest small focused panel with 2–4 choices, clear requirement/failure and native keyboard/controller navigation, **not** a huge text wall.
7. **Narrative impact:** a saved quest result should later unlock one consequential interaction in a subsequent mission. Don't add generic faction points until the world demonstrates a second outcome.

## Proposed merge / test sequence

1. Keep `feature/world-e1-ford-warden-quest` as **draft PR into C2** and check green CI.
2. Control chat reviews B/C/D/A ancestry, chooses stable Fabric integration base and resolves overlapping `MkmMod`, event, HUD and snapshot changes. **Do not independently merge stacked draft PRs into `main`.**
3. Add B's keyed XP grant. Wire E1 claim to it and test crash/retry; decide scope of kill XP. Confirm save migration from E1 schema v1 and v2.
4. Add D's item grant only once the Equipment API exists and is integration-tested; no ad hoc vanilla drops from E1 until then.
5. Client smoke + dedicated multiplayer checks: talk, inspect PER 10/12, refuse, accept, enemy death, reward, repeated claim, 2 players, relog, respawn, chunk unload, blocked spawn, Peaceful, admin duplicate placement and malicious command attempts.
6. Merge after review; subsequent E2 should deepen consequences/reputation rather than multiply featureless NPCs.

## Acceptance ownership

- **World lead:** quest state graph, authored content, explicit state migration and save tests.
- **Character lead:** grant-once, XP ledger and progression integration.
- **Equipment lead:** single item award definition and idempotent inventory semantics.
- **Combat/AI leads:** kill attribution, enemy identity and lifecycle.
- **UX lead:** dialogues and journal, keyboard/F5 compatibility.
- **Control chat:** integration base, ancestry/merge order, final scenario test approval.

**Stop conditions:** red CI; repeated XP/item claim; unbounded recovered spawns; a tagged NPC exploitable without proximity or permissions; client-side writes to world/XP; save corruption; a new independent XP/item/attribute engine; breaking CombatService or animation timeline with no separate review.
