# MKM Integration, QA and Release Gates v1.0

Normative workflow: [MKM_UNIFIED_WORKFLOW.md](MKM_UNIFIED_WORKFLOW.md). Domain instructions: [MKM_WORKSTREAM_PROTOCOLS.md](MKM_WORKSTREAM_PROTOCOLS.md). Work item and handoff forms: [MKM_WORK_ITEM_TEMPLATE.md](MKM_WORK_ITEM_TEMPLATE.md).

**Baseline:** Minecraft 26.3 / Fabric / Java 25 experimental integrated PR #30. Historical NeoForge main is not the Fabric release target. No open draft PR is implicitly integrated.

## 1. Evidence ladder

| Level | What is actually demonstrated | What remains unknown |
| --- | --- | --- |
| E0 SPECIFIED | scenario, design and acceptance criteria | functioning code |
| E1 GENERATED | committed code, models and tests | execution |
| E2 STATIC PASS | bounded validation, compile, JUnit or deterministic simulation at exact SHA | target client gameplay |
| E3 RUNTIME PASS | actual Blender/server/client startup or script execution with observed outputs | interactive Minecraft visual quality |
| E4 GAMEPLAY PASS | repeatable real Minecraft client/server scenario with evidence and exact artifact | full cross-system release quality |
| E5 ACCEPTED | independent QA/integrator and relevant user acceptance, known risks recorded | guarantees of future bug-free versions |

Each gate is PASS / FAIL / BLOCKED / UNVERIFIED / NOT APPLICABLE with a reason. A generated Blender render is not a Minecraft gameplay PASS. A headless client boot is not interactive GUI or F5 video proof. A codec roundtrip is not actual save/load/cutover proof.

## 2. Interfaces that must remain single-owner

| Producer | Consumer | Contract | Negative case to test |
| --- | --- | --- | --- |
| Character | World | keyed grantExperienceOnce and durable receipt | duplicate claim, crash between character and quest commits |
| Character + Equipment | Combat / Magic / UI | one PlayerModifierSnapshot + equipment contributions | slot swap, double stats, stale preview |
| Combat + Equipment | MNAE | accepted action, attacking hand, family and sequence | offhand sword, axe sword fallback, canceled hit |
| Magic | MNAE | APPLIED after actual successful healing | full health, no mana, old dimension/event replay |
| World | UI and MNAE | server-owned dialogue stage and semantic NPC signal | forged choice, missing NPC, wrong owner |
| MNAE | QA | rig/clip/transform/weapon grip with actual game renderer evidence | six-part render of articulated source |
| Integrator | Release | integrated SHA and JAR digest, schema matrix, test and rollback evidence | incompatible downgrade or duplicate installed JAR |

Cross-owner interface changes require both owners' approval. A source PR without merged code is not a live dependency.

## 3. Verification matrix

| Area | Applicable mandatory evidence |
| --- | --- |
| Java/resource changes | Java 25 Gradle build, relevant unit/contract tests, resource bounds |
| Server lifecycle | dedicated Fabric server boot; no client-only common linkage |
| Client UI/HUD | real client interaction, small/large GUI scale, F1/F5, localization, key conflicts |
| Animation and Blender | 9+ review frames and timed preview; separate actual Minecraft F5 60fps A/B |
| Combat/equipment | LMB single vanilla damage, mining, action timing, shield, dodge, weapon/offhand and PvP |
| Abilities | Renewal success/failure, once-only cost/effect/visual, state reset |
| World/AI | dialogue PER threshold, journal, quest replay, chunk unload, server restart, two owners |
| Persistence/network | old schema migration, backups, invalid/out-of-order packets, relog/death/crash cut |
| Packaging | exact integrated SHA, one JAR, installer upgrade/rollback, artifact SHA256 |
| Performance | benchmark where AI pathing, rendering or resource volume is changed |

For pure docs-only PRs, explicitly mark these code/runtime gates NOT APPLICABLE. For source-only animation demos, E3 Blender is not E4 game.

## 4. Save migration and crash-cut checklist

1. Take and verify a restorable backup of a disposable existing world before any schema update.
2. Record the exact baseline branch/SHA, mod version and persisted CharacterData schema. Integrated PR #30 has v3; some isolated PR branches have v2; M3/v4 is proposed in Character PR #35 and World integration PR #45.
3. Test **supported** forward migrations, preservation of earned/spent credits, XP receipts, equipment, abilities and quest state. Any unknown newer schema or destructive backward downgrade is a hard BLOCK.
4. Test relog, death/respawn, server restart and two players separately. State explicitly that a unit-level codec roundtrip is not the same as a live world upgrade.
5. Inject failures between character XP receipt and quest REWARDED writes. World and Character attachments are not implicitly ACID; refuse to invent historical reward payments.
6. Re-send the same dialogue/cast/reward packet and corrupt/out-of-order variants. No extra item, XP, mana change, healing or incorrect quest transition may occur.
7. Keep backup/restore, downgrade policy and recovery notes in the integrated PR.

## 5. Integrated playable vertical slice

At exact integrated HEAD on a backup test world:
1. Open Character Sheet and verify attributes, credits, equip sword/shield and correct modifier snapshot.
2. Test normal LMB on entity and block mining, R combo, guard, G dodge eight ways; compare mainhand/offhand and left-handed item family routing.
3. Cast Renewal in valid and invalid states; observe server costs, once-only APPLIED VFX and no stale inbox events.
4. Talk to Ilra/Ford Warden, test PER below/above gate, journal J, accept/decline/replay, encounter identity, enemy death and once-only XP.
5. Disconnect and reload, change dimension, unload/reload the quest chunk, restart dedicated server. Repeat reward and cast requests.
6. Use two players to verify privacy/ownership and NPC/quest state isolation. Observer visuals must be marked unsupported if not implemented.
7. Record fixed-camera 60fps F5 sequences of sword, axe, offhand and dodges in same conditions and exact renderer/F8 state.
8. Log exact JAR SHA256, Minecraft/Fabric/Java versions, source PRs, screenshots/log IDs and ALL missing checks.

This scenario is a **future acceptance suite**, not a claim that today's draft branches already coexist in one game build.

## 6. Hard stops and release signoff

- **P0 STOP:** corrupted or lost player progress, non-migratable saves, duplicate XP/loot/damage, client-authoritative outcomes, exploitable network event, server crash, unsafe installer or unauthorized billing.
- **P1 release blocker for impacted feature:** wrong active hand, weapon grip through head, broken NPC dialogue, irrecoverable quest stage, missed successful spell or repeated animation event.
- **P2 tracked polish:** secondary gesture timing, decorative styling and non-blocking layout issues.
- If real client proof is unavailable for a player-facing feature, it may remain a DRAFT with static/Blender evidence but cannot be called gameplay-accepted or released.
- Release requires owning domain review, independent QA, integrator signoff and user review of subjective animation/play-feel. Record the build SHA, artifact checksum, tested world migration, rollout/rollback and open risks.
- Do **not** merge to historical NeoForge main and do not merge a stacked experimental branch by assumption.

## 7. Correction principle

A failed gate triggers a causal diagnosis. Return to S2/S3 for incorrect reference/design, S4/S5 for incorrect implementation or S6 for invalid test setup. Fix the smallest owning component, add a regression fixture, re-test the exact integrated commit, and update the recorded status. Never replace an observed FAIL with a statement about how many unit tests passed.
