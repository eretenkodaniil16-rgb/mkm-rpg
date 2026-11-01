# Character M3 — 0.0.5-fabric.4 world upgrade to v4 + World E2.1 PR #39

**Character draft:** [PR #35](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/35), stacked on #29 → #15.
**World E2.1:** [PR #39](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/39), stacked on #26 → #18.
**Last user-tested integrated Fabric build:** experimental [PR #30](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/30), `0.0.5-fabric.4`, CharacterData v3.
**MNAE:** Issue #43, entirely independent; do **not** import animation-branch CharacterData v2 over real v3/v4 player data.

## 1. Verified actual source state (2026-11-01)

- On PR #30, `gradle.properties` is `mod_version=0.0.5-fabric.4`.
- PR #30's `CharacterData` has v3 persisted `earned_points` and
  `allocated_points`; seven base stats + `experience`; its codec does **not**
  know `claimed_reward_ids`.
- **Source blob hashes match exactly across PR #39 and PR #35** for the three
  M2 core files at inspection time: `CharacterData.java` blob
  `2ac3504a61af70e600f94340ec3ab98eeb0ddb3d`,
  `CharacterRewardRules.java` `60edfc908addfeb73285300a72a49ba3c9334337`,
  and `CharacterService.java` `a64dcec0a4f850ca1d7075c7dcbd2d18a6d39092`.
  PR #39 incorporated M2, **not yet all M3**.
- PR #39's `FordQuestService.claim` checks the current quest stage, calls
  `grantExperienceOnce(player, "mkm:ford_warden/complete", 75|100)`, checks
  `confirmed()`, and **then** writes quest `rewarded`.
  Its `onLogin` **does not** use `reward_xp_floor` to mint XP.
  This fixes the older E2 #26 collision. DO NOT reintroduce `repairReward`.
- **Version discrepancy:** PR #39's `gradle.properties` currently declares
  `mod_version=0.0.4-fabric.0` despite carrying the newer M2 v4 codec. This
  is **not** suitable as a distributed upgrade from `0.0.5-fabric.4`.
  The common Fabric integration must assign and verify one reviewed version
  and update JAR manifest, installer, ZIP, artifact names and in-game version.

## 2. Exact character persisted record

The logical identifier is the Fabric player data attachment `mkm:character`.
`CharacterAttachments.CHARACTER` registers the persistent codec,
`copyOnDeath()` and `syncWith(..., AttachmentSyncPredicate.targetOnly())`.
The exact Fabric/NBT enclosing path is owned by Fabric serialization: do not
modify raw playerdata keys by guessing their physical NBT position.

| Field | v3 / 0.0.5-fabric.4 | v4 / Character M2–M3 | Semantics |
| --- | --- | --- | --- |
| `schema_version` | integer `3` | integer `4` | version marker; absent means v1/v2 legacy |
| `experience` | nonnegative long | same | MKM XP, separate from vanilla experience |
| `strength` | integer | same | STR |
| `dexterity` | integer | same | DEX |
| `vitality` | integer | same | VIT |
| `endurance` | integer | same | END |
| `intelligence` | integer | same | INT |
| `willpower` | integer | same | WIL |
| `perception` | integer | same | PER |
| `earned_points` | integer | same | historical earned build credits; NEVER recompute to a smaller value |
| `allocated_points` | **7 ordered integers** | same | absolute spent credits in STR/DEX/VIT/END/INT/WIL/PER order |
| `claimed_reward_ids` | **not present** | bounded list of stable namespaced strings | **server-only** paid/redeemed receipts; v3 upgrade initializes empty |

On v3→v4 migration: preserve all XP, seven attributes, `earned_points`
and all **seven allocation ledger integers verbatim**. Initialize an empty
receipt ledger because v3 cannot prove previous paid quests. For old v1/v2
(absent or old marker), stats and XP survive and no historical spending is
invented. No new stat, respec or skill tree. V4 never persists animation state,
poses, weapon transforms or cosmetic cues. Real equipped items remain in
vanilla `ItemStack` / armor / inventory, not in `mkm:character`.

The owner-facing `CharacterData.STREAM_CODEC` intentionally excludes
`claimed_reward_ids` from client synchronization, while preserving XP,
stats, and allocated/earned points. **Never treat the client mirror as a
source for quest redemption decisions.** A server-only receipt still survives
server save/reload and Fabric's `copyOnDeath` registration.

The World quest is a **different** player attachment `mkm:ford_quest`:
v3 `stage`, `found_tracks`, `recovery_count`, `encounter_id`,
`warden_id`, `reward_xp_floor`. The floor is backward-compatible audit
data **only** in E2.1. The encounter registry and offline death outcomes are
separate **world-level** attachments. No single-attachment guarantee covers
simultaneous writes to the separate world-level records.

## 3. Mandatory backup and rollback policy

Before opening a user's existing `0.0.5-fabric.4` world in a newer combined
build, **stop Minecraft/the dedicated server and copy the ENTIRE world folder**
(including `level.dat`, `playerdata/`, dimensions/region data and mod
attachments) to a different directory or storage volume. Create a SHA-256
manifest for the copy and preserve the original without modification.
For a multiplayer server, also back up relevant server config and the
mods directory to reconstruct the old running environment.

Never test the upgrader on the only save. Minecraft `Save and Quit` /
server `stop` should finish before copying; an in-use copy may be internally
inconsistent. First run the new candidate against a **copy** of the backup.
After migration, a return to the old `0.0.5-fabric.4` build on the same
upgraded world is **UNSAFE**: its v3 codec lacks `claimed_reward_ids` and may
re-save CharacterData while dropping those receipts, permitting repeat
quest payments after returning to v4. To roll back, restore the entire
original pre-upgrade backup, not merely the old mod JAR.

The legacy v3 decoder can also drop v4 data on an animation-only branch that
still carries CharacterData v2. Never ship MNAE PR #41 independently to this
real user world until combined world-preserving integration passes.

## 4. Golden fixtures and automated tests

`src/test/resources/migration/character_v3_0_0_5_fabric_4_*.json` are
**synthetic records with the exact persisted field names/types of PR #30**,
not extracted real user NBT files. They cover:
- `allocated.json`: level-9 XP=4000, earned=16, spent=14,
  two free points, nonuniform attributes and all seven allocation entries.
- `max_level.json`: XP=650000 (historically above 495000 level cap),
  earned=198, spent=138, server still records a reward receipt at +0 XP.
- `legacy_admin.json`: STR=63 (old/admin stat above gameplay cap 40),
  spent ledger unchanged; cannot be increased by normal allocation but must
  not be clamped to 40 by migration.

`Fabric054CharacterV3MigrationTest` verifies exact old-value comparison,
v4 codec save/reload, safe spending of only remaining points, prevention of
stale request replay, receipt atomicity/duplicate, old XP > cap, physical
modifiers before/after migration with the **same equipment deltas**, and
rejection of malformed v3 allocation ledgers and future schema. In malformed v3\nrecords the codec currently throws `IllegalArgumentException` from its\nrecord constructor rather than returning `DataResult.error`; whether the\nFabric playerdata loader retains or resets the attachment on that exception\n**must be checked in a real throwaway world**. Existing
CharacterRewardRulesTest, CharacterOwnerSyncTest, QuestRewardRecoveryTest,
RewardReplayFuzzTest cover keyed XP, sync privacy, crash cut, two players.

A unit test of the codec **cannot** prove the actual Fabric attachment read,
respawn copy, file-system durability, or true world upgrade. That is why the
following gates are mandatory for QA.

## 5. Minimum QA acceptance matrix for the next COMBINED Fabric JAR

These are **release blockers**, not optional cosmetic tests.

| ID | Environment / procedure | Required evidence / pass criteria |
| --- | --- | --- |
| MIG-01 | Use a byte-preserved backup of an actual `0.0.5-fabric.4` world with v3 allocated points. Before: capture `/mkm stats`, K sheet, all seven stats, XP, earned/spent/free ledger; inspect one copy of raw playerdata through an NBT reader. After: upgraded build, relog and server restart. | Exactly same XP/attributes/earned and all seven spent counters. First v4 save has `schema_version=4`, empty receipts if none previously. No player reset, no item loss. |
| MIG-02 | Spend 1 unspent point with K; issue old duplicate/stale packet, relog, server restart. | Exactly one increase, credits decrement once; repeat rejected. UI preview and displayed stats agree with server. |
| MIG-03 | Die/respawn after claiming a World E2.1 reward; save, relog, restart and try repeating CLAIM. | `mkm:character` receipt survives `copyOnDeath()`, exactly one quest award ever; no loss of awarded XP, ledger or other items subject to Minecraft death rules. |
| MIG-04 | Run actual E2.1 `return_to_warden` → redeem → `rewarded` with 75 or 100 XP, force checkpoint after receipt but before stage via QA injection / controlled state fixture; resume claim. | `ALREADY_GRANTED` finalizes stage without second XP. Old `rewarded` + absent receipt is flagged for manual audit, not paid again. No `reward_xp_floor` login top-up. |
| MIG-05 | Two distinct connected users with independent v3 save histories (different earned/spent), their own raiders/quests; one kills other's raider. | Owner isolation: separate quest states and `mkm:character` ledgers. No cross-player allocation/XP; repeated network requests fail. |
| MIG-06 | Check vanilla inventory: sword/axe/shield/armor, equip/unequip and staged sheet preview; restart and check inventory. | ItemStack/equipment unchanged on migration, same `PlayerModifierSnapshot` is used for physical and mana paths; no double application of gear modifiers. |
| MIG-07 | Make one old E2 v3 `rewarded` quest save with `reward_xp_floor` but no Character M2 receipt. | Save remains intact; no automatic XP credit or quest reset; operator must audit before intervention. |
| MIG-08 | Run experimental MNAE/build integration with v3 save copy; switch F8/animation but repeat `/mkm stats`, K, quest and relog. | No CharacterData v2 downgrade, no drop of `allocated_points` or receipts; no animation frame/pose fields in CharacterData. |
| PKG-09 | Inspect final JAR metadata, `gradle.properties`, Windows package, filename, startup banners, modlist; launch matching client + dedicated server. | One clearly identified new version strictly above `0.0.5-fabric.4`; no outdated `0.0.4-fabric.0` PR #39 label and no mixed packet versions. |
| FAIL-10 | Restore the pre-upgrade world backup onto an old-version isolated instance; compare SHA-256 for the untouched archive. | Original world still loads with its original mod set. **Never** point old mod to an already-upgraded save. |
| CORRUPT-11 | On a **throwaway** copied test save, remove one v3 `allocated_points` list or shorten it to six entries; attempt loading under the combined build and capture all logs + playerdata hashes. | Constructor/codec rejects the malformed ledger; QA must verify game loader **does not silently initialize/reset the player's progress or overwrite the original playerdata**. Preserve backup and treat any reset as P0. |

The QA team must report for each ID **PASS / FAIL / NOT EXECUTED**,
build SHA/JAR checksum, Minecraft/Fabric/Java versions, test world backup ID,
number of real clients, before/after snapshots and any failure logs. Green
Gradle/JUnit/server/client smoke alone does not satisfy MIG-01–08 and CORRUPT-11.

## 6. Cross-workstream merge contract

1. Base common integration on **Fabric 26.3 branch with all game systems**,
   not historical NeoForge `main` and not MNAE-only CharacterData v2.
2. Preserve exact M2 CharacterData/RewardRules/Service sources from #39/this
   Character branch. Reconcile M3 additive calculator with Equipment's
   existing overload **once**, avoiding a duplicate or double gear bonuses.
3. Retain PR #39's `FordQuestService.claim` order and no-floor-repair
   `onLogin`. For quest `rewarded` with no receipt, keep safe
   `MANUAL_REVIEW` behavior; do not fabricate retrospective XP.
4. Leave character-stat and reward ownership entirely logical-server-side.
   Don't let MNAE Issue #43 cosmetics mutate XP, credits, life or equip state.
5. Before distributing to real players, QA validates the P0 migration and
   rollback matrix on a copy of an actual `0.0.5-fabric.4` save. No old JAR
   should be published as a safe downgrade path. Do not merge unfinished
   independent drafts blindly.

No skill tree, eighth characteristic, animation/weapon pose persistence,
new magic damage or unapproved combat balance changes are authorized here.
