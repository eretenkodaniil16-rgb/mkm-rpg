# MKM SHOWCASE-01 / Issue #77 — Enemy AI S2

Status: implementation candidate / **DRAFT, not a released game benchmark**. Base: Fabric QA5 PR #69 `integration/fabric-v4-world-origins-enemy-perf-qa5` @ `440dfe07`, CharacterData v4. Historical `main` is NeoForge; never merge directly to it. This continues the existing V1 AI in PR #56 and World Encounter Director #54; it does not introduce a second damage/XP service.

## Design choice

| Architecture | Distinct motion & anatomy | Maintenance cost for 30–60 | MC compatibility | Decision |
|---|---|---|---|---|
| 60 independent EntityTypes / Java AI implementations | High | High duplication | Native but large renderer/goal/attribute surface | Rejected for this stage |
| Pure family interpreter, one entity shell | Poor for spiders, aerial and multipart bosses | Initially low, then complex interpreter | Native navigation compromises | Rejected as sole architecture |
| **Hybrid**: stable EnemyDefinition + specialized vanilla/custom host + reusable tactics | High | Low/shared code | Native GoalSelector/pathfinding/damage retained | **Chosen** |

Two real host models: Road Raider = Husk (biped/human inventory, native melee); Cave Stalker = Spider (native climbing and melee). The EnemyFactory creates and equips only; `EnemyService` indexes loaded tagged UUID controllers and `EnemyRuntime` owns transient perception. 48 *candidate* archetypes across eight families remain planned in `docs/ENEMY_CATALOG_VISION.md`; none are silently claimed implemented.

## Visible player journey

1. A World-placed Ilra is interacted with; player accepts the existing Ford quest (World E2.1 preserves dialogue nonce, owner, private raider and journal).
2. Player moves **24–96 blocks from a currently loaded Ilra**, in allowed Overworld biome (plains/forest/river/dappled_forest), at **night** and on the ground. The new `EncounterDirector.onPlayerTick` runs one **phase-spread check every 200 ticks**, calls the existing `start` only after all gates pass, and refuses an existing saved reservation. An already accepted or return-to-warden quest is eligible; an unstarted or completed quest is not. No `/mkmenemy spawn` or GM `/mkmencounter start` is needed for **the encounter**.
3. World reserves exact 3-mob UUID roster and frozen reward BEFORE inserting any entity; two tagged Raiders (armored shield guard and axe flanker), one tagged elite Spider Stalker. World ledger remains global/once per world in this pilot, so **two co-op players share one occurrence**, not one per player.
4. Guard carries visible helmet, chestplate, sword and shield. Guard periodically raises the vanilla shield if a visible target is close; a flanker with axe uses a short lateral waypoint only when a nearby same-area guard has a target. When first wounded, the raider broadcasts a bounded 12-block ally alert, then 60-tick per-entity rate limit.
5. Spider actively samples **BLOCK light** while tracking a visible target. With light >=10 it circles and suppresses the leap; in shadow with a target ~3–8 blocks away it telegraphs with smoke/soul flames, a bounded 4-point ground line (elite adds a two-sided sector), and a spider sound. Elite uses a longer 20-tick windup, late lateral feint and stronger velocity-only leap, **not 5× HP**. The leap does not damage: only vanilla native attacks can inflict harm. Combat armor, poise, i-frames and shield remain canonical.
6. All three deaths must match saved UUID, type and owner tag in the World ledger. An unloaded mob is NOT dead. Defeated owner returns to original **encounter anchor** within 16 blocks: server automatically uses existing Character v4 `grantExperienceOnce` and marks the world entry CLAIMED. Other players may help fight, but cannot claim this once-only encounter reward.

**Dependency limitation:** in QA5, placing Ilra itself remains a GM/World task. A future World POI-placement PR must create a player-discoverable warden/tower and pass an acceptance test. This branch does not bypass quest ownership by spawning Ilra globally and does not replace World generation.

## Persistence and authority matrix

| State | Authority | Saved across unload? | Notes |
|---|---|---|---|
| Actor UUID/attributes/equipment/definition tag/elite & guard tag | Vanilla entity NBT | Yes | No re-scaling when entity loads |
| 3 entity UUIDs, encounter phase, owner, reward amount and anchor | World EncounterLedger attachment | Yes | Never infer death from absence |
| Player XP, credits and claimed reward receipt | CharacterData v4, CharacterService | Yes | Existing CharacterEvents ONLY writer of ordinary kill XP |
| Last-visible location, windup/shield duration, cached patrol | EnemyRuntime | No | Reset on unload/reload; no chunk tickets |
| Particles/sounds and MNAE semantic INTENT | Visual only | No | World + EnemyService signals, no gameplay damage |
| Extra loot token | EnemyService death callback | Mostly | Entity-tagged for duplicate callback; **not transactional across partial save cuts** |

Old QA5 encounters that already reserved **Skeleton leader** retain their serialized type/UUID. New definition allows a Spider leader, but does not rewrite existing ledger member records; `EncounterDirector.onDeath` recognizes both old Skeleton and new Spider. Old skeleton encounter may still be completed and claimed with the same receipt key.

## AI budgets and correctness

- Only registered, loaded prototype UUIDs are iterated each tick; behavior work is spread over 5-tick phases. Native MC pathfinding is preserved, no global per-tick `ServerLevel.getAllEntities` search, no background threads.
- Player acquisition is a bounded 16-block spatial query every ~20 ticks per searching enemy. Guard detection is a local 10-block query every 20 ticks for a relevant flanker. Damage alerts query a local 12-block neighborhood with a maximum 8 recipients and 60-tick cooldown.
- Spider block light is sampled on its phased tactical tick; physical impulse only after a telegraph, valid LOS, distance and shadow check. Armor and shield use Minecraft's equipment, not new bespoke damage reduction math.
- No source-side `setTarget` to entities in unloaded chunks. No global collection is scanned for every entity; cross-dimension loaded maps are keyed by ServerLevel.
- `/mkmenemy perf` reports loaded count, phase-tick average/max, bucketed p95 upper bound, allocation estimate when ThreadMXBean is enabled, and target/guard/light query & leap counters. This is **not** full game MSPT or an objective 40-mob pass.
- `/mkmenemy stress 20|40|80` attempts mixed types on safe level terrain, reports how many were **actually spawned**, then resets module telemetry. GM-only `clear` discards only non-World-owned test prototypes, no loot/XP.
- Off-thread immutable planning snapshots could be evaluated only *after* profiling and without passing Minecraft Entity/Level references across threads. Not enabled now.

## 20/40/80 experimental protocol (must be run, not merely planned)

On cloned non-production world, Java 25 + exactly the matching Fabric 26.3 build, with client and server profiles held constant:

1. Baseline: restart, backup saves, 120 seconds idle; record full MSPT p50/p95/p99 via spark/JFR, RSS, GC, framerate.
2. Test 20: flat enclosed test ground, `/mkmenemy clear` only in dedicated QA world; `/mkmenemy stress 20`. Verify actual loaded number =20. Stand in visual range; after 2-min warmup collect 5-min readings (`/mkmenemy perf`, spark).
3. Repeat fresh baseline/restart for 40 and 80. Note 80 is **stress safety probe**, not promised user gameplay target. Explicitly distinguish module query counts from full server tick timings and normalize samples by elapsed tick count.
4. Repeat 40 with obstacles, different Y levels, torch-lit vs dark area, two connected players. Inspect spawn/AI/GC allocations with JFR. Compare optional optimizer backends only in cloned worlds per Performance M0 #64; never bundle Moonrise+C2ME simultaneously.
5. Unload chunks while windup active; reload and verify no delayed leap/duplicate spawn, same UUID/attributes/equipment, original World roster. Restart server and retest. Verify death exactly once, Character XP once per victim and encounter receipt once per owner.
6. Explicitly test guards with shield, low-health retreat, flanker with guard alive/dead, player switching and last-visible-position investigation behind blocks; evaluate native GoalSelector interference. Test Spider at block light values 0/9/10/15, target distance 2/5/9 blocks, collision & levitation under jump.
7. Read `/mkmperf ticks` as a passive instrumentation proxy, **not** authoritative MSPT; spark/JFR is the acceptance reference. Fail any release acceptance if real measurements or interactive two-client evidence are missing.

**Performance targets (design hypotheses only):** 20 TPS; 40 loaded engaged enemies without pathological stepwise AI allocation growth; p95 full-server MSPT <50 ms; module p95 ideally <1 ms; no size growth of indexed controllers after repeated unload cycles. No claim currently that these are achieved.

## Remaining gameplay gates / known defects

- No original player/NPC render-layer skin swap or fully original spider model. Different vanilla silhouettes, role-specific equipment and telegraph are visible in multiplayer but **do not satisfy final art-level acceptance**. Original item icons are included in this PR.
- The vanilla melee/path goals can compete with sidecar navigation when tactical retreat or flanking begins. Meaningful reliability of retreat, collision-safe pounce and shield block MUST be confirmed by recorded gameplay; if insufficient, integrate a selective, prioritized Goal instead of disabling all vanilla AI.
- World once-per-save roster and the original GM-only Ilra placement constrain replayability and unscripted POI discovery. World workstream owns further placement, encounter cadence and faction ecology.
- Combat XP dedupe uses a live-object weak map; there is no global cross-save-cut exactly-once transaction for ordinary kill XP or dropped items. Quest/encounter receipt is stored atomically only *within* player CharacterData; world/player files are separate saves.
- MNAE semantic THREAT/REACT/DEATH cues are sent with the correct species rig profile; the current renderer may be a no-op and no new articulated enemy rig is promised.
- No measured 20/40/80 actual-Minecraft benchmark, two-client combat footage, crash-save-cut acceptance or full animation handoff completion in a headless CI build. Keep PR draft.
