# MKM Character Core M2 — durable quest XP receipts / integration contract

Status: draft review. **Branch** `feature/character-quest-rewards-m2` stacked on
`feature/character-progression-m1` (draft #15). Minecraft 26.3, Fabric, Java 25.

## Severity / root cause

World E1 (`feature/world-e1-ford-warden-quest`, draft #18) currently calls
`update(player, REWARDED)` **before** `CharacterService.addExperience(player, amount)`.
Quest and XP live on separate Fabric attachments. A persisted intermediate
snapshot can mark the quest complete while leaving XP unpaid. Reversing just
the calls without a receipt allows multiple payouts after a crash/retry.
Neither independent attachment write is an ACID transaction.

## New server-only Character Core API

```java
CharacterRewardRules.Result r =
    CharacterService.grantExperienceOnce(
        player, "mkm:ford_warden/complete", FordQuestRules.rewardExperience(state));
// r.status(): GRANTED, ALREADY_GRANTED, INVALID_REQUEST, LEDGER_FULL
// r.confirmed() == true for GRANTED or ALREADY_GRANTED
// r.creditedExperience(): actual added XP (may be 0 at level cap)
```

**All authority stays in CharacterService**. No second XP engine, no new
attribute model, no new damage events. `CharacterRewardRules.grantOnce` is a
pure deterministic mutation used by the server adapter and unit tests.

The `mkm:character` persistent attachment changes schema **v3 -> v4**:
`claimed_reward_ids: [namespaced-id]`. The ID list, XP, earned level credits,
allocated attributes and all existing stats are one immutable `CharacterData`.
The **single** `setAttached(CharacterAttachments.CHARACTER, result.character())`
is the atomic in-memory character transition and the player-save serialization
contains both XP and receipt. Duplicate receipts NEVER write the attachment.
The receipt list is **excluded from owner stream sync**: the client receives
unchanged XP, earned points, attribute allocation and modifier inputs, but
cannot read or forge the redemption ledger.

IDs are immutable content contracts. Use `mkm:ford_warden/complete` for the E1
single-use story quest, scoped implicitly to the player whose attachment is
being modified; do not append player UUID. For future repeatable activities,
use a durable, server-generated **instance ID** recorded in quest state before
the reward becomes eligible, e.g. `mkm:bounty/contract_1234`. The same event
**must reuse** the same ID on retry. Do not derive IDs from timestamps, packet
sequence numbers, display names or current XP. The contract forbids invalid
IDs, overlong IDs and XP amounts beyond 1,000,000 in a single claim. Existing
receipt IDs are never evicted. The bounded ledger has capacity 4,096; when full
the service returns LEDGER_FULL without paying or marking a quest complete.
This limit is an explicit scale-review gate, not a FIFO cache.

## Required E1 integration (on World branch, NOT modified here)

1. Check server actor, distance, quest schema, current stage and PER bonus
   **using existing FordQuestService/FordQuestRules**.
2. For stage `return_to_warden`, call `grantExperienceOnce(player,
   "mkm:ford_warden/complete", FordQuestRules.rewardExperience(previous))`
   **before** `update(player, rewarded)`.
3. If result is `GRANTED` or `ALREADY_GRANTED`, transition quest to
   `rewarded`; for `ALREADY_GRANTED`, report "previously credited" rather than
   another +XP. If INVALID_REQUEST or LEDGER_FULL, leave quest claimable, log
   diagnostic (no silent failure).
4. IMPORTANT: if stage is already `rewarded`, refuse payment regardless of
   receipt absence. This protects old E1 v1/v2 saves which might have been
   marked rewarded before crash; existing data cannot determine reliably whether
   old XP was paid. Do **not** auto-repair that ambiguous historical case by
   guessing/awarding again. Use a backed-up save and explicit admin adjudication.
5. The new API must be invoked on the **logical server thread**; do not retry
   from async threads or network clients. `FordQuestService.claim` is already
   a server command handler. Do not change `mkm:ford_quest` or quest stage names.

### Crash-cut matrix

| Persistent snapshot captures | Quest stage | Character receipt/XP | Next claim |
| --- | --- | --- | --- |
| Before Character commit | return_to_warden | absent / unpaid | grant once |
| After Character commit, before quest transition | return_to_warden | present / paid | ALREADY_GRANTED, mark rewarded |
| After quest transition | rewarded | present / paid | refuse claim |
| Legacy E1 inconsistent save | rewarded | absent / unknown | fail closed; manual audit |

These are **logically consistent saved player snapshots**. On sudden power
loss before the server writes the newest player file, the entire unsaved
progress can roll back. This solution does NOT promise durability beyond the
server's actual save/backup guarantees; it prevents duplicate/lost awards
*between the two logically ordered attachment operations in a saved snapshot*.
For hard guarantees beyond a player file use an external transactional journal
or server-owned append-only DB with explicit fsync, which is intentionally out
of scope. Never call the two-attachment design "fully ACID".

## XP composition / balancing

Existing rules are **unchanged**: levels 1–100, 2 credits/level, cap 40, XP
threshold `50 × level × (level − 1)`. Server-credited hostile kill: 25 XP.
E1 claim: 75 XP base or 100 XP if PER 12 investigation succeeds.

- Fresh player: qualifying kill 25 XP + base E1 75 XP = level 2 at 100 XP,
  earning 2 points **once**. Retrying claim changes neither XP nor credits.
- With PER bonus: 25 + 100 = 125 XP, still level 2 with 2 credits.
- At level cap: the quest receipt is still recorded (and remains idempotent)
  even when credited XP is zero. Vanilla XP remains independent.
- The 25-XP hostile kill uses a different reward mechanism because repeatable
  kills are real discrete events. Do not reuse quest static reward IDs for
  each kill. Spawner farming is a separate gameplay economy question.

## Schema and compatibility gates

- v1/v2 -> v4: preserve existing stats, initialize missing 7 attributes at 10,
  derive earned credits from the existing level, zero allocation ledger.
- v3 -> v4: **keep** earned credits and seven allocated counters exactly, keep XP
  and all seven primary attributes. Only initialize `claimed_reward_ids=[]`.
- v4 -> v4: persist exact immutable receipt IDs; reject duplicates/malformed
  list/cap overflow instead of silently dropping receipts.
- Future schema -> current: no downgrade; reward and allocation writes fail
  closed until a deliberate migration. Downgrading the mod version risks
  losing receipts; always back up `world/playerdata` before switching.
- `copyOnDeath()` retains receipts, XP and spending across respawn. Relog and
  dimension transfer reuse the player attachment. Client owner sync excludes
  receipts to reduce traffic and prevent metadata disclosure. Both client and
  server should use the matching MKM version.

## Equipment D1–D3 / abilities integration

**Do not import draft D3 into this Character branch.** D1/D3 introduce
`EquipmentSnapshots.snapshot(player, character)` and the existing
`PlayerModifierCalculator.calculate(character, equipmentDeltas)` overload.
Upon integration, preserve D1 equipped modifiers for BOTH actual and staged
Character Sheet snapshots:

```java
PlayerModifierSnapshot actual = EquipmentSnapshots.snapshot(player, data);
PlayerModifierSnapshot preview = EquipmentSnapshots.snapshot(player, previewData);
```

The equipment deltas must be applied once, through the same calculator, before
the common final clamps; avoid calculating independent stats or adding deltas
to already-clamped values. Display attribution as **base / attributes /
equipment / final** later only if genuine source breakdown is implemented.
Do not present unequipped inventory items as active bonuses. D2 physical
mitigation and D3 exact-hit provenance remain owned by Equipment/Combat.

Magic M1 `mkm:renewal` reads `HEALING_POWER`, `HEALING_RECEIVED`,
`MAX_MANA`, `MANA_REGENERATION`, `MANA_COST` from
`PlayerModifierSnapshot`; vitality, willpower and intelligence must continue
working without a new mana or healing formula. END drives MAX_STAMINA and
STAMINA_REGENERATION; STR drives PHYSICAL_POWER/MELEE_DAMAGE; VIT drives
physical resistance and received healing; WIL drives mana/healing power.
The tests verify these paths and preservation of same source values across
XP claims and allocations. Magic's own ledger remains separate; no magic
code is modified here.

## Later builds / talents / respec (design only)

- **Passive talents:** stable talent IDs and immutable server-owned unlock
  ledger; eligibility based on CharacterData stats/level; additive modifier
  deltas feed the existing PlayerModifierCalculator aggregator; never invent
  second combat calculations.
- **Build archetypes:** derived suggestions/templates, not new character
  characteristics. Class/classless decision and numeric scaling are deferred.
- **Respec:** explicit server transaction with cost/permissions/cooldown and a
  persisted allocation baseline; never just decrement spent counters or XP.
  Must preserve claimed reward IDs and prevent refunds of unearned credits.
- **Account/party rewards:** require explicit owner scope and consistent quest
  instance ID. This API is intentionally per-player, not a party transaction.
- **Future reward types:** item and reputation grants need their own durable
  semantics and full-inventory recovery; do not misuse XP receipts as proof an
  item was delivered.

## Testing / acceptance checklist

JUnit: v1/v2/v3 migration; v4 persistence roundtrip; replay and changed-amount
retry; crash-cut snapshots; full ledger refusal; malformed/duplicate IDs;
XP cap, stats/points immutability, kill+quest sum, and shared modifier pathways.

CI: Java 25 Gradle build, nested Fabric API, dedicated-server startup,
Windows installer packaging. Live acceptance (not covered by unit tests):
saved v3 world upgrade with backup, death/respawn, relog/restart, E1 claim
interruption before/after Character commit, simultaneous multiplayer claims,
client/singleplayer sheet preview, D3 equipment snapshot and Renewal cast.
E1 branch must integrate this API and pass these before end-to-end
"exactly-once E1 claim" can be declared.

**No branch merging in this task.** All other streams remain independent drafts.
