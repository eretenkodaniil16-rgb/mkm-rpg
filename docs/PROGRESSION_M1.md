# MKM RPG M1 — Character progression and allocation contract

Status: initial vertical slice; numerical tuning and runtime acceptance pending.
Base: `feature/action-combat-c2-state-hud`; Minecraft 26.3 / Fabric Loader 0.20.1 / Java 25.

## Rules and balance

- Keep the existing quadratic *cumulative* MKM XP curve:
  `XP(level) = 50 × (level - 1) × level`, levels 1–100.
- Level 1 = 0 XP; 2 = 100; 3 = 300; 10 = 4,500; 50 = 122,500; 100 = 495,000.
- Award **two permanent build credits on each first reached level after level 1**.
  Total maximum level-earned credits = 198. Credits are monotone; reducing XP using an
  administrative command does not refund or delete previously earned credits.
- Starting attribute value = 10 in each of STR/DEX/VIT/END/INT/WIL/PER.
  The **gameplay allocation cap is 40 per attribute**; the legacy storage clamp is still 1–100.
- Total spendable capacity of seven neutral starting attributes: `7 × (40 − 10) = 210` points,
  so max-level allocation (198) cannot fill all stats. A focused build can cap six attributes
  and spend 18 points in the seventh. No compulsory class or random stat growth.
- Existing `PlayerModifierCalculator` and `PlayerModifierSnapshot` remain the only derived
  modifier implementation. The existing diminishing-returns and individual modifier caps
  protect against excessive linear stat stacking; exact modifier coefficients still need
  combat/magic playtesting.
- Prototype reward = **25 MKM XP** to the killing player when a server-side hostile
  `Enemy` dies. Vanilla XP and item drops are unchanged. No XP for passive mobs,
  PvP or deaths not credited to a player. Repeated callback on the same entity cannot
  pay twice. This reward is deliberately a temporary source until enemy tiers, quest
  rewards and anti-farm rules are designed.
- Normal gameplay reward additions stop at the level-100 XP threshold; amounts already
  above the cap from historical saves or admin commands are preserved. This prevents
  repeated server attachment writes and owner-sync packets after max level. Admin
  `/mkm xp set` remains able to adjust stored XP for diagnostics.
- At 25 XP per qualifying kill, levels 2/3/10 require cumulative 4/12/180 kills.
  Level 100 would require 19,800 kills without other XP sources: **not acceptable as a
  final progression economy**. Add graded enemies/quests before long-form release.
- No respec in M1. A refund or respec requires a separately designed server transaction,
  not decreasing an allocation counter. Death has no RPG-XP or credit penalty.

## Persistence and authority

Schema `mkm:character` v3 adds `earned_points` and seven `allocated_points` counters.
The list order is the existing `PrimaryAttribute.values()` enum order; never reorder
the enum or the seven packet bytes without a migration/protocol change.

- Migrating v1/v2: preserve all seven existing attribute values (default missing ones to 10);
  initialize allocations to zero, and earn `2 × (current level − 1)` available credits.
  Old files may have **no** `schema_version` key because v2 omitted its default (2).
  Treat that as a legacy schema. Do not infer historic spending from an attribute value.
- v3: retain earned credits and allocation totals exactly; `spent = sum(allocations)`,
  `available = max(0, earned − spent)`. Malformed ledger entries fail decoding, while an
  over-spent ledger fails closed on spending rather than generating free credits.
- Server-to-client: owner-only Fabric attachment sync; all writes replace the immutable
  `CharacterData`. `.copyOnDeath()` preserves XP, stats and points on respawn.
- Client-to-server `mkm:allocate_attributes`: one four-byte correlation ID,
  a four-byte expected server spent-point revision and seven one-byte absolute
  cumulative allocation counters. No XP, claimed credit balance, target player ID
  or damage are client-controlled. Server verifies alive/non-spectator, schema,
  **exact spent revision**, monotonic counters, sufficient earned credits and cap 40.
  Apply all seven changes in **one immutable attachment replacement**. Replays,
  stale/conflicting requests and forged over-spending produce no mutation.
- Server-to-client `mkm:allocation_result`: correlation ID and accepted/rejected status
  **only**. It is not authoritative state. The owner-only attachment sync controls
  when the UI replaces the preview with committed attributes. A client blocks another
  submission until reply/sync or a timeout, and displays rejection and delayed-sync states.
- The client sheet stages `+/-` increments, previews derived modifiers using the existing
  `PlayerModifierSnapshot`, then confirms. Minus removes **staged changes only**;
  no respec is implied. On owner attachment update, clear staged edits.
  The layout has a compact mode for 320×240 scaled GUI viewports and a narrower
  essentials-only mode for small windows. It does not rewrite Combat or animation.

## Verification

JUnit tests: threshold and overflow boundaries, repeat earning, legacy JSON without schema,
v3 codec roundtrip, immutable ledger, debit, replay, stale/revision conflicts, forged
over-spend, legacy over-cap values, future-schema fail closed, modifier changes,
Netty packet roundtrips and wire counter bounds, compact viewport geometry and
3,000 deterministic randomized allocation attempts.

Manual acceptance gates still required after CI:
1. Fresh Fabric 26.3 world: kill four hostile mobs -> level 2, 2 unspent points.
2. K opens sheet; stage +2 STR, see a higher physical modifier, Confirm.
3. Re-open, confirm points zero and authoritative stats/snapshot updated.
4. Repeat the same network request; confirm no change; verify visible rejection,
   delayed response protection, stale revision rejection and too-large counters.
5. Leave and rejoin; exit and reload world; both allocations and XP stay.
6. Die and respawn; confirm copy-on-death and owner-only synchronization.
7. Load a pre-v3 player save and confirm migration *without* changing existing stats.
8. Dedicated-server log and a real multiplayer client smoke test, including display
   at different UI scales and disconnection during an unsent preview.

## Not in M1

No respec, class trees, skills/ability effects, level-based enemies, quest rewards or UI for
advanced builds. These belong to subsequent M1 tuning and M2/M3 feature work.

## Out-of-scope safety notes

- The current fixed 25-XP kill reward is deliberately **not anti-farm balanced**;
  hostile spawner farming and high-level progression cannot be treated as release-ready.
- Unit tests prove pure rules and Netty codec behavior; a dedicated-server startup smoke
  does **not** simulate save/reload, client rendering, combat kills or multiplayer.
- The Fabric v3 attachment and allocation packet require **matching client/server mod
  versions**. Treat this work as a synchronized mod update, not a drop-in network patch.
