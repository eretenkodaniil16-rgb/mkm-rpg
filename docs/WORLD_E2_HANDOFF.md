# MKM RPG World E2 — engineering handoff (stacked on E1 / PR #18)

**Draft PR:** [#26](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/26)
**Branch:** `feature/world-e2-reward-encounter-dialogue` → base `feature/world-e1-ford-warden-quest`.
**Runtime baseline:** Minecraft 26.3 / Fabric Loader 0.20.1 / Java 25. The existing `main` still contains historical NeoForge; do not merge this stack straight into `main`.

## Scope implemented

- **Quest persistence v3.** `mkm:ford_quest` retains `stage`, `found_tracks`, `recovery_count`; additionally records an authored raider UUID, the accepting Warden UUID and a positive `reward_xp_floor` when claimed. V1/V2 state migrates; unknown future versions fail closed.
- **Reward repair (not a second XP engine).** Claim captures a server-computed **cumulative XP floor** (capped at Character M1's level-100 XP threshold), writes it along with `rewarded`, then calls `CharacterService.addExperience` to top up any gap. On next login, any saved `rewarded` record with a missing XP gap is reconciled, including if the server lost the Character attachment update in a saved checkpoint. Replays after `rewarded` cannot mint a new target. Old rewarded v1/v2 saves retain the completed state but do not invent historical XP.
- **Stable enemy identity.** One real spawned Husk UUID bound to the accepting player's quest (plus existing owner tag). A different enemy or another player's quest enemy cannot satisfy it. `recover` only locates the exact loaded entity or binds one unambiguous legacy tagged Husk. **It never auto-spawns another one on an unloaded/unknown UUID.** This removes the unbounded duplication risk rather than pretending chunk unload equals death.
- **Offline death replay.** `mkm:ford_encounter_outcomes` is a world-level persistent Fabric attachment for owner/encounter death tombstones when the player is offline. On login, the server compares them against the owner's active quest and consumes the matching event. A friend or environment can eliminate the target; only that target's quest owner gets quest completion. M1's ordinary mob-kill XP still belongs to its own attribution rules.
- **Dialogue and journal UI.** Right-clicking the tagged Ilra opens a lightweight, responsive screen: inspect PER (>=12), accept, decline, locate, claim; server sends a bounded `FordDialogueView` snapshot. A serverbound choice includes the clicked entity ID, expected stage and a byte choice ID. The server validates actual entity tag/type, range <=6, player alive/non-spectator, bound Warden identity, current stage and choice allowlist; client cannot send XP, attributes, spawn positions, outcomes or owner IDs. `J` requests owner-only quest journal state from the server with objective and investigation flag. Commands remain as development fallback.
- **End-of-quest consequence:** a successful PER investigation persists as `found_tracks`, yields the optional +25 MKM XP and alters the completed dialogue/journal clue. This is a small but durable consequence; shared settlements/faction standing are intentionally not created yet.

## Detailed QA sequence

1. With Java 25 and the Fabric E2 build, start non-Peaceful test world and use `/mkmworld place` once as GM on level terrain. Spawn should be adjacent, never intersect player; duplicate place within 48 blocks rejected.
2. Right-click Ilra. Verify actual dialogue window opens (not villager trade), choices clickable at scaled UI sizes. `J` opens objective journal. If old client lacks network support, `/mkmworld talk` chat fallback remains.
3. Test baseline PER 10 inspection: no clue or bonus; PER 12 via M1 legitimate attribute points: clue saved once; repeatedly click option; inspect result survives relog.
4. Decline: still `not_started`. Accept: one owned Husk spawned in a safe location; fail on blocked/Peaceful with no stage change. Try duplicate accept.
5. Fight the exact Husk with vanilla+MKM combat. Verify false enemy UUID doesn't count. Party test: B kills A's authored Husk => **only A's quest advances**; B can still complete B's different assigned encounter.
6. Disconnect owner A while B kills A's Husk in loaded world; reconnect A. Check world outcome replay, return objective and no second spawn. Try environmental kill similarly.
7. Verify `/mkmworld recover` with loaded enemy shows coordinates. Move away and unload entity: locate does **not** spawn a new one; return to previous terrain to reload. If raider truly deleted outside death event, GM intervention is required; this is a known service gap, not safe to auto-respawn.
8. Claim: verify exactly +75 MKM XP or +100 with tracks (observe XP immediately before and after claim; M1 eligible-kill +25 XP is separate). Spam choice packet/command; no additional payout. Relog and respawn; journal says completed and correct outcome.
9. Reward crash simulation: persist an in-memory `rewarded` state with a computed higher `reward_xp_floor` and an older Character XP; on login reconciliation must top up only the missing amount. Repeat login: no extra XP. An actual hard-crash save ordering simulation still requires an integration harness.
10. Test malformed future schema, invalid request stage, unauthorized NPC or >6-block interaction, dead/spectator, invalid choice ID and rapid reconnect.

## Important engineering limits

1. **Not an exactly-once atomic XP transaction.** The reward floor allows *recovery from a committed quest state with missing Character XP*; it does not prove atomicity if the Character attachment commits but the quest state rolls back, or if an administrator intentionally reduces XP below the recorded floor. E2 must not be advertised as full crash-exactly-once. Character M1 should provide a durable, single-character-attachment keyed award ledger + XP in one immutable state write (proposed `grantExperienceOnce(player, mkm:ford_warden_v1, amount)`). The E2 floor is a migration/repair bridge.
2. **Chunk lifecycle:** UUID query only resolves loaded enemies. Exact global entity presence across unloaded chunks is unknown; auto-respawn remains disabled. A future Encounter service should store dimension/home/UUID plus an authoritative lifecycle/tombstone and support bounded reattachment; no force-loading thousands of chunks or per-tick global scans.
3. **Crash ordering for world death tombstones:** world attachment save and offline player quest save are separate stores. The event queue is a best-effort durable bridge, not a global transaction. Tombstone list capped at 4,096 with an overflow fail-closed rather than unlimited memory.
4. **Equipment reward not yet handed out.** D1/D2/D3 branches define vanilla weapon profiles and combat modifiers, but not a durable `grantEquipmentOnce` API with inventory-full and replay semantics. Proposed first item: **one named vanilla iron sword** with existing Equipment D1 profile; actual grant should be implemented by the Equipment stream's award contract, not direct World inventory mutation.
5. **No live client acceptance yet** just because Gradle/JUnit/server smoke are green. Dialog rendering, interactions, world save/reload, respawn, separate clients and chunk states require real Minecraft test.
6. **Narrative scope:** player-private outcomes. No global village reputation/faction NPC flags until a second authored quest needs one.
7. **Security:** all client inputs are intents; live server validates, owner-only packets carry state. Potential double actions from stale clients should be rejected by stage. Check special interaction cases (offhand, spectator and id reuse).

## Specific requests to control chat / subsystem owners

- **Character M1 (#15):** confirm reward-key semantics, XP cap, v3→v4 migration plan for keyed ledger; do not independently mutate build credit counters in quests. E2 accesses existing `CharacterService` only.
- **Equipment D1/D2/D3 (#16, #22, #23):** accept one starter item reward definition and grant-once protocol (including inventory full, reconnect, item drop/trade, duplicate requests and server crash).
- **Combat / AI:** agree on how to attribute quest targets when killed by friend, ranged projectile, magic, pet or environment; E2 credits completion to the **target's original quest owner**, not whoever killed it.
- **UX:** review E2 dialogue, key J, accessibility, localization, screen scaling and session invalidation on travel/disconnect.
- **World:** implement a modest encounter anchor/chunk-safe lifecycle and follow-up authored site placement that is opt-in and does not supplant the normal Minecraft world.
- **QA:** run above manual cases in 2-player dedicated server, record results and screenshots; red CI or loss/duplication blocks merge.

**Review/merge:** E2 is stacked on E1, both remain draft. Control chat must decide upstream Fabric base and merge order with Character/Equipment/Animation; no direct merge to NeoForge `main`.
