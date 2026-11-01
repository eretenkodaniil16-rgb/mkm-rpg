# Character M3 — World E2 reward recovery, single modifier snapshot, MNAE boundary

Status: integration contract + pure Java implementation, stacked on Character M2 PR #29.
Branch: `feature/character-m3-crosscore-mnae-contract` → `feature/character-quest-rewards-m2`.
**No MNAE runtime files, combat damage logic or magic runtime modified.**

## Sources audited (2026-11-01)

- Character M1 PR #15, M2 PR #29, schema v4, `grantExperienceOnce`.
- World E2 draft #26, `FordQuestService.claim/onLogin/repairReward`,
  `FordQuestState` schema v3 and `FordQuestRules.claimAtExperience`.
- Equipment D1–D3 draft #16/#22/#23 and integrated experiment PR #30:
  `EquipmentSnapshots.snapshot(player, data)` delegates to
  `PlayerModifierCalculator.calculate(data, EquipmentRules.aggregate(equipped))`.
- Magic M2 draft #28: `AbilityModifierAccess` and `RenewalParameters`;
  action eligibility still owned by Combat; `arcane_bolt` not executable.
- First-party MNAE decision [issue #33](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/33)
  overrides Player Animation Library integration proposals.

## P0: World E2 XP floor and M2 receipt MUST NOT operate simultaneously

Existing E2 implements this sequence:
```java
FordQuestState rewarded = FordQuestRules.claimAtExperience(
    previous, before, ProgressionRules.experienceForLevel(100));
update(player, rewarded);
repairReward(player); // separate XP write, later onLogin also tops up to rewardXpFloor
```

This **cannot be combined unchanged** with M2's immutable keyed redemption.
`reward_xp_floor` is a historical monotonic XP **lower bound**, not evidence that
an XP reward was or was not delivered. It can top up after legitimate loss of XP,
admin correction, game economy tuning, or when the character ledger already claims
the same ID. The result can overpay despite an individually idempotent grant API.

M3 provides `QuestRewardRecovery.decide(character, rewardId,
readyToClaim, alreadyRewarded, legacyXpFloor)` with outcomes:
- `GRANT_ONCE`: validated E2 stage `return_to_warden`, absent receipt.
- `FINALIZE_QUEST`: validated stage `return_to_warden`, receipt present.
- `COMPLETE`: `rewarded` and receipt present, **never top up XP**.
- `MANUAL_REVIEW`: `rewarded` without receipt, even if legacy floor is present.
  Do not guess whether a historical E2 payout was granted.
- `REJECT`: invalid stage/ID/future CharacterData schema.

### Exact World E2 integration procedure — World owner owns actual source edits

1. In `FordQuestService.claim`, keep the existing authenticated NPC/distance, stage,
   owner/quest validation. Use fixed `mkm:ford_warden/complete` (per-player scoped
   via `mkm:character`; do not use NPC display name or regenerated encounter UUID).
2. Classify using `QuestRewardRecovery.decide(CharacterService.get(player),
   id, stage.equals(RETURN_TO_WARDEN), stage.equals(REWARDED), rewardXpFloor)`.
3. On `GRANT_ONCE`, call `CharacterService.grantExperienceOnce(player, id,
   FordQuestRules.rewardExperience(previous))`. Only statuses `GRANTED` and
   `ALREADY_GRANTED` permit progressing quest stage.
4. On `FINALIZE_QUEST`, do **not** grant again; set quest stage `REWARDED`.
   With either successful outcome, `FordQuestRules.claim(previous)` provides
   the existing stage transition. Do not write quest REWARDED before the character
   receipt; do not mint a new reward ID after an interruption.
5. On `COMPLETE`, no mutation. `MANUAL_REVIEW` must stay completed pending
   admin audit with backup; no automatic payout. Invalid/future state fails closed.
6. **Remove or explicitly disable `repairReward` calls from BOTH claim and
   `onLogin`** after M2 receipt integration; a claimed receipt means XP was
   already accounted, irrespective of current XP and `reward_xp_floor`.
   Keep the legacy floor serialized unchanged solely for migration/audit.
7. E2 `FordQuestState` remains schema v3 and `mkm:ford_quest` identifier remains
   unchanged. E2's encounter IDs/offline death ledger are a *different* concern.
8. On the logical server thread, in multiplayer A/B, both players can redeem their
   own identical stable ID once. A shared NPC or ally kill never implies a shared XP
   receipt. Do not let the client send IDs, XP amounts or completion flags.

**Crash-cut truth table (consistent Minecraft player-save checkpoints):**

| Character save | Quest save | Resolution |
| --- | --- | --- |
| old/no receipt | RETURN_TO_WARDEN | GRANT_ONCE |
| new/receipt + XP | RETURN_TO_WARDEN | FINALIZE_QUEST |
| new/receipt + XP | REWARDED | COMPLETE |
| old/no receipt | REWARDED | MANUAL_REVIEW; legacy ambiguity |

This is **idempotent and recoverable under consistent persisted player snapshots**.
It does **not** provide an fsync'ed distributed transaction across different saved
player/world files. Power loss may roll back the entire unsaved interval. Hard
durability needs server-level backup/transaction tests; report such failures.

## MNAE ownership boundary

Character Core owns:
- XP, level, primary seven stats, earned/spent credits, reward receipts.
- Immutable `PlayerModifierSnapshot` values and their definitions/clamps.
- Future durable unlock IDs and prerequisites after gameplay approval.

Combat owns:
- Attack timing, hit windows, stamina cost, damage, interruption, cooldown,
  weapon compatibility and server validation.
- New dynamic attack profiles resolve from ONE server-calculated snapshot, not
  frame position, bone ID, event marker or any client-supplied statistic.

Magic/Abilities owns:
- Mana/cooldown resources, eligibility and cast effects; reads the same
  authoritative snapshot with all equipped bonuses applied once.
- Spells/techniques unlocks may later be persisted by Character, but casting
  authorization and active execution remain server-side Combat/Ability.

MNAE owns **presentation only**: deterministic clip sampling, skeleton transforms,
poses, authored cosmetic timelines, avatar/first-person adapters and optional S2C
visual intents. It must NOT own `CharacterData`, alter player attributes, store
frame/pose/cosmetic runtime values in player save, authorize damage or issue XP.
When an attack is visually interrupted, only Combat's server result determines
whether damage/stamina/XP occurred. Client poses cannot be used as hit evidence.
See issue #33 non-negotiable invariants and milestone MNAE-0/1.

## Central snapshot access and integration with equipment

`CharacterModifierAccess.base(data)` and
`CharacterModifierAccess.withContributions(data, equippedAndPassiveDeltas)`
delegate to the **existing** `PlayerModifierCalculator`. No new formulas, caps,
attributes, or secondary modifier engine. `preview(staged, sameEquippedDeltas)`
reuses the exact equipped delta map with projected attributes.

When PR #30/Equipment D3 is reconciled, the authoritative shared resolver can be:
```java
Map<PlayerModifierId, Double> equipped = EquipmentRules.aggregate(serverEquippedSlots);
PlayerModifierSnapshot snapshot = CharacterModifierAccess.withContributions(
    CharacterService.get(serverPlayer), equipped);
PlayerModifierSnapshot preview = CharacterModifierAccess.preview(
    serverValidatedPreview, equipped);
```
However the already-integrated `EquipmentSnapshots.snapshot(player, data)`
in PR #30 already does precisely this via `PlayerModifierCalculator`; preserve it
and its equipped-only semantics. **Do not apply equipment bonuses twice** or use
client-side preview state for real attacks. Pure access can also back future
passive modifier composition after balance approval; not yet active in game.

Authoritative effects presently covered by derived IDs:

| Influence | Existing modifier keys | Consumer |
| --- | --- | --- |
| STR/DEX | PHYSICAL_POWER, MELEE_DAMAGE, ATTACK_SPEED | Combat weapon attacks |
| VIT | PHYSICAL_RESISTANCE, MAX_HEALTH, HEALING_RECEIVED | Guard/defense/health |
| END | MAX_STAMINA, STAMINA_REGENERATION, STAMINA_COST, DODGE_COST, STAMINA_REGEN_DELAY | Combat cost/resource |
| INT | MAGIC_POWER, SPELL_DAMAGE | Ability formulas |
| WIL | MAX_MANA, MANA_REGENERATION, MANA_COST, HEALING_POWER | Renewal / mana |
| PER | WEAK_POINT_DAMAGE, CRITICAL_DAMAGE | Combat accuracy/damage profile if adopted |

A modifier definition existing in the snapshot does **not prove** every dynamic
weapon profile consumes it. MNAE visual swing amplitude must NOT scale base damage
unless Combat accepts a corresponding rule. Do not add a second hit/damage pipeline.

## Special combat techniques and spells (proposal only)

Use canonical namespaced IDs and server-owned, monotone revisioned unlock records,
eventually in a carefully migrated Character schema or dedicated authoritative
ability attachment (decision pending). Unlock input must be a server-evaluated
progression/quest grant; client may only request use of an already-granted ability.
Prerequisites from the existing seven stats/level, no eighth stat or second earned
points counter. Equipped/temporary grants must not persist as permanent unlocks;
respec must not erase claimed quest XP. MNAE receives a *cosmetic action ID* only
AFTER the Combat/Ability server authorizes an action; it never validates unlocks.
Do not implement class trees, granted spell effects or new cast timelines here.

## Verification and mandatory manual gates

- Unit tests: E2 stage/receipt crash matrix, legacy floor ambiguity, per-player
  independence, malformed/future schema, exact modifier snapshot composition,
  previews with equipment deltas, STR/DEX/VIT/END/INT/WIL paths, finite/clamps.
- Existing M1/M2 tests: schema v1/v2/v3/v4 persistence, 3,000 randomized allocation
  attempts, Netty packet roundtrips, duplicate/overspend/stale, reward ledger caps.
- CI: Java 25 Gradle, embedded Fabric API, dedicated-server and Xvfb client boot,
  PowerShell installer and ZIP/JAR artifacts.
- **Still manual:** actual death/respawn, relog, full restart after interrupted
  claim, two-client multiplayer, E2 onLogin offline encounters, real held weapon
  swaps, D3 guard and Renewal, MNAE LMB/F5 render. Startup smoke cannot verify these.

## Handoff order and merge safety

Do not retarget PR #15 or #29; this PR stacks on #29. QA/control integrates
Character M1 -> M2 -> M3 plus E2 #26 (through E1 #18), Equipment D1–D3
and Magic M2 #28 into the **separate Fabric integration branch** #30 or a
successor. E2 must remove XP-floor repairs before claiming atomic quest
rewards. MNAE issue #33 remains independent; no animation files touched.
Never merge directly to historical NeoForge `main` without review.
