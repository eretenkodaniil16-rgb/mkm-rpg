# MKM Enemy AI V1 — two-host vertical slice (DRAFT)

**Platform:** Minecraft 26.3 / Fabric / Java 25. **Base:** World E2.1 + CharacterData v4, draft PR #45, not historical NeoForge `main`. Do not merge into `main` by ancestry accident.

## Decision record: three architectures

| Candidate | Maintainability | Behavior variety | Runtime cost | MC compatibility |
|---|---|---|---|---|
| One Java entity/AI per species | Poor after ~15, duplicated fixes | High but expensive | Natively good if well built | High, but duplicated render/Goal code |
| Entirely data-driven family | Good for numeric variants | Low for truly different locomotion | Good until universal interpreter becomes complex | Risk of forcing one anatomy/navigation model |
| **Hybrid: host + typed definition + shared tactical policies** | **Best for 30–60** | **High: specialized host mechanics, shared reusable goals** | **Bounded per loaded mob** | **Uses Minecraft's native AI and damage** |

**Chosen:** hybrid. The first two host forms are existing `EntityTypes.HUSK` and `EntityTypes.SPIDER`, carrying distinct persistent prototype tags. This is **not yet custom EntityType registration**, and it deliberately avoids unsupported temporary client renderers. Full species registration should only follow a server/client vertical QA pass. `EnemyDefinition.id()` is independent of the host and remains stable when art or EntityType changes.

## Runtime architecture

```text
World Encounter Director [future]       GM /mkmenemy spawn (now)
            |                                     |
            +--------- spawn request ------------+
                              |
                    EnemyDefinition / Catalog
                              |
                  vanilla Husk   vanilla Spider
                      |                |
                 native Mob goals/navigation/attack/loot
                      |                |
      ServerEntityEvents ENTITY_LOAD / ENTITY_UNLOAD
                              |
              EnemyService: loaded UUID -> EnemyRuntime
                    5-tick phases / local queries
                              |
                 EnemyTactics (pure decisions)
                  /       |         |         \
               patrol  investigate retreat   flank
                              |
     CombatEvents/vanilla damage | Magic (future ability intent)
                              |
             CharacterEvents single XP authority
             EnemyService once-marked extra loot
             NpcVisualSignals semantic MNAE event
```

No global every-tick entity search. `EnemyService` loops **only registered loaded prototype mobs**, with a per-dimension controller ceiling of 512. Runtime memory is not saved and is removed on unload/death/server stop. Entity tags, UUID, vanilla attribute values and native state are saved with the Minecraft entity. The patrol anchor and enemy-last-seen position reset on chunk reload; World's persistent authored encounter reservations are **not** modified or replaced.

### Concrete prototype forms

| | Road Raider | Cave Stalker |
|---|---|---|
| Stable ID | `mkm:enemy/road_raider` | `mkm:enemy/cave_stalker` |
| Vanilla host | Husk, humanoid | Spider, arthropod |
| Combat role | skirmisher / guard candidate | ambusher |
| Level-1 health / base attack | 24 HP / 4 | 20 HP / 4.5 |
| Speed | 0.245 | 0.315 |
| AI | patrol, acquire/chase, 120-tick last-seen investigation, low-HP retreat | hunt, flank, native wall climbing/leap/attack, 65-tick last-seen investigation |
| Host animations | vanilla Husk | vanilla Spider |
| Future rig | `mkm:rig/humanoid_v1` | `mkm:rig/creature/spider_v1` |
| Kill XP | 25, existing CharacterEvents | 35, existing CharacterEvents |
| Additional drop | `mkm:raider_insignia` | `mkm:stalker_fang` |

Both item icons temporarily reuse vanilla textures. Special drop occurs once per observed death callback via persistent marker, **in addition to** vanilla drops. There is no ACID transaction spanning entity death and item entity creation, so a crash/save-cut duplication test is a necessary future gate; the marker does not prove exact-once drops across an arbitrary partial save. Quests and other enemies still receive their original 25 XP.

### Server AI rules and invariants

- Target selection is **server-owned**: valid living non-spectator local player, max 16-block scan, once per 20 ticks per entity, spread across 20 phases. Current targets are retained unless invalid/too distant. Damage changes focus to the attacker.
- When line of sight is lost, move only toward the **last observed block position**, never continuously toward a hidden player's current coordinates. Memory expires after 120 / 65 ticks; target is dropped. Native goals can reacquire normally.
- At or below 27% health, the raider moves away from the current target; the spider keeps aggressive flanking behavior. Pure decision code is independent of Minecraft.
- Path progress sampled every 10 ticks; 50 stuck ticks trigger a bounded reset and pursuit of a known point; **no teleportation**. True collision/nav validation requires runtime QA.
- The Spider's native attack and wall-climb remain enabled. This module only steers desired waypoints; it does not replace `goalSelector` or `targetSelector`. Vanilla can override sidecar waypoints, especially under obstruction: this is a known tactical reliability gate.
- On positive damage from a player, alert **at most 8 same-definition allies** via a local 12-block spatial query, at most once per 20 tick phase for each attacked source entity. No world-wide broadcast or unconditional O(n²) perception polling.
- Existing Combat Core's poise is only a movement slow/soft interruption for these mobs. Fully interruptible casting/attack wind-ups are a later Combat+Animation protocol.
- Difficulty Peaceful blocks GM spawns. No new entity spawn rules / biome injection / settlement budgets are active.

### Combat, Character, Magic, World and MNAE contracts

**Combat (implemented seam):** No custom damage computation. Native attack/armor/shield, mob attack callbacks, i-frames and physical mitigation remain the only authoritative hit path. Enemy levels set *vanilla* attribute bases once at manual spawn and never reapply those on entity reload. `CharacterEvents.onEntityDeath` is **the sole MKM kill XP writer**, and calls `EnemyService.killXp(victim)` for hostile mob subtype reward; its existing weak identity dedupe remains in place. Player combat values still use EquipmentSnapshots/PlayerModifierSnapshot. Future ranged weapons MUST use the vanilla projectile + existing damage provenance; no extra hit at animation time.

**Magic (planned protocol, not implemented):** `EnemyAbilityIntent(actorUUID, abilityId, targetUUID|serverBlockPos, requestedTick)` goes to a shared **server-validated AbilityService effect executor**. AI should only select intent; Magic must validate distance, line of sight, faction, available mana, cooldown, and interruptibility. No separate spell formulas, mana ledgers or client damage messages per enemy. Player and mob casters require different resource adapters, but shared effect resolution.

**World (handoff):** Future `SpawnRequest(definitionId, level, spawnAnchor, encounterId, faction, difficultyBudget, seed)` and `EnemyLifecycleEvent(entityUUID, definitionId, spawnReason, deathCause, dimension)` belong to World/Encounter Director. Never create a second copy just because a chunk is unloaded; World E2.1 FordEncounterRegistry and death tombstones remain authoritative for authored quest enemies. Vanilla temporary prototype spawn commands are **admin-only** and deliberately have no encounter ownership. A global population budget or biome spawn is not present in V1.

**MNAE (implemented semantic emitter, not rendered clip):** `NpcVisualSignals.Intent(actorUuid, rigProfile, cue, startTick, sequence)` is published after real server damage/death. V1 emits `REACT` and `DEATH`; default observer is a no-op and renderer failure cannot alter combat. Rig IDs must not be confused: humanoid and spider use different profiles. Animation owns clip selection, bones, blending, temporal priority and playback. Future event taxonomy: `mkm:enemy/notice`, `alert`, `windup`, `melee`, `ranged`, `cast`, `hurt`, `stagger`, `die`; only adopt after agreeing a versioned transport with Animation.

**Drops:** `EnemyDefinition` should eventually point to loot table/resource IDs, resolved by World/Loot. V1's two guaranteed experimental token items are ordinary registered Items with borrowed textures. Do not use them as a quest token without a separate one-shot Quest/Character ledger.

## Developer commands

Cheats/GM permissions required:
```mcfunction
/mkmenemy spawn raider
/mkmenemy spawn raider 10
/mkmenemy spawn stalker
/mkmenemy spawn stalker 10
/mkmenemy stress 20
/mkmenemy stress 40
/mkmenemy clear
/mkmenemy perf reset
/mkmenemy perf
```
In non-Peaceful difficulty, a safe nearby solid-footed position is required. Spawns are persistent and immediately target the command issuer. For repeatable tests on flat terrain use \`/mkmenemy stress 20\` or \`40\`; this attempts a grid of alternating types and reports the **actual** count successfully spawned. The stress command resets module telemetry. \`/mkmenemy clear\` discards nearby tagged prototypes without death, loot or XP; never use it on authored encounters. `/mkmenemy perf` shows service-only loaded/peak, mean/max and bucketed p95-upper-bound per-server-tick runtime and **optional** current-thread allocated bytes using the platform's ThreadMXBean. It does **not** measure full Minecraft MSPT, total Java heap, GPU or per-tick percentile. Use \`/mkmenemy perf reset\` after the cohort reaches 20 or 40 entities, then collect at least 5 minutes of data. Only ticks containing at least one tracked loaded mob are sampled. Reset does not affect gameplay state.

## Verification & release gate

JUnit:
- Stable ID/tag independence, invalid definition rejection, distinct host types, bounded level scaling and read-back.
- LOS lost → investigate → expire; low-health retreat, spider flank/close attack, stuck recovery.
- Phased 40-ID policy scheduler: exactly 8/40 decision slots per simulated tick (not a measured server benchmark).

Interactive QA, not waived by headless CI:
1. Back up world. Start Java 25 Fabric 26.3 server and two real clients. Use normal difficulty, start from 0 enemies, capture /tick query baseline and JFR or profiler.
2. Spawn one of each. From high and low blocks/behind walls, check nav success, no through-wall current-coordinate tracking, last-visible-point investigation, memory expiration and target switching under real hits. Test water, slabs, ladders, fences, doorways and 2-block gaps.
3. At low health Raider must retreat and stop abusing invisible pursuit; Spider must approach via wall or flank, not use the humanoid attack rig. Observe soft poise and vanilla attack damage once per hit with shield/armor.
4. Kill each twice; verify one additional item token per death, vanilla loot retained, and player Character v4 XP increments **once**, by 25/35 (plus ordinary quest reward only if legitimately due). Kill another untagged vanilla mob: still 25. Repeat with player B killing another player's mob.
5. Unload their chunk, save/restart, reload: same entity UUID and level-scaled attributes; no multiplied HP or new duplicate spawn. Kill after reload: no duplicate XP or drops. Check original Ford quest raider's UUID, reservation and XP unaffected.
6. Use \`/mkmenemy stress 20\` and then \`/mkmenemy clear\` followed by \`/mkmenemy stress 40\`, recording the actual successfully spawned count (on flat terrain). For each count: record 2-minute warmup + 5-minute test, /mkmenemy perf mean/max/alloc, full server MSPT p50/p95/p99 and GC/jfr allocation rate. Target, navigation and hits should remain functional in multiplayer.
7. Candidate acceptance budgets, **not measured results**: 20 TPS retained; p95 full-server MSPT < 50 ms; enemy module p95 < 1 ms at 40 active; controller allocation aim < 16 KiB per tick; no unbounded loaded-map retention after unload. The histogram p95 is an **upper bound**, not an exact percentile; validate tail latency with JFR/YourKit, and measure full-server MSPT separately.
8. Run `./gradlew clean test build --no-daemon`, dedicated server smoke, client under Xvfb, plus interactive third-person/first-person checks. Verify no damage/animation regressions. Do not merge without these observations.

### Known incomplete work

This version does **not** implement bespoke EntityTypes/model registration, entity-specific animation clips, terrain-aware squad formations, ranged/magic/support mobs, persistent long-term tactical memories, worldgen/spawn budgets, faction diplomacy, elite loot tables, gameplay-enforced attack interrupts or measured 20–40-player-combat TPS/allocations. The manual QA ledger must be attached to the PR before claiming any of these.
