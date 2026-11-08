# MKM SHOWCASE-01 / Issue #80 — Performance M1 experiment and measurement contract

**Target:** Minecraft 26.3 / Fabric 0.20.1 / Java 25, QA5 base `440dfe07f67d4cce1a8fc10f2bd62a5a2574e137`. This PR is a reversible performance *experiment*, **not** a measured FPS/MSPT improvement or Showcase release approval. Do not merge to historical NeoForge `main`.

## 1. Existing baseline and hypothesis

- Existing `/mkmperf ticks` captures only the server-tick callback span. It is **not full spark MSPT**. `/mkmenemy perf` is only the tagged-enemy sidecar time, allocations when supported, and coarse p95 upper bucket. Never substitute these for spark p50/p95/p99.
- Existing `BoundedPureWorkerPool` is **not instantiated in enemy runtime**, so no worker-thread AI execution is claimed. Pure `EnemyTactics.decide` work is trivial compared with expected LOS/pathfinding costs; no evidence supports enabling a pool. World, loot, target, XP, and CharacterData v4 remain server-owned.
- In QA5 `EnemyRuntime.acquireNearbyPlayer` uses `level.getEntitiesOfClass(ServerPlayer.class,...)` once per 20 ticks per phased mob. For 1–2 local players, a direct loop over `level.players()` may avoid the transient query list and entity-section scan. This is an **untested bottleneck hypothesis**, not an observed spark hotspot.
- Candidate JVM option: `-Dmkm.enemy.directPlayerScan=true`. Default **false** keeps the original path. Read on process startup; restart between conditions. `/mkmenemy perf` shows `playerScan=vanilla-spatial` or `playerScan=direct-experimental`.
- Candidate retains 16-block expanded mob AABB intersection, living non-spectator player filtering, closest distance and line-of-sight predicate; applies only on the existing 20-tick acquisition phase. It does not create new network messages, force-load chunks, deal damage or change saved data. Direct scan cost becomes O(number of players), not recommended for unprofiled high-player-count servers.

## 2. Mandatory actual-run matrix

Run **each** condition using the same world seed and independent *pre-run* world copies, and measure both baseline and candidate on the same host:

| Dimension | Conditions |
| --- | --- |
| AI cohort | 0 / 20 / 40 / 80 *actual loaded tagged enemies*, not just requested spawns |
| Players | One player; two clients connected to same server (2nd client changes RAM and CPU) |
| Locations | Open flat local fight, underground/cave, previously ungenerated chunks |
| Phase | Cold (fresh process + copied world); warm (after fixed JIT/world/cache warm-up). Do not compare cold to warm |
| Repeats | At least **2 paired trials per phase** (8 measurements per scenario). Five or more pairs recommended before high confidence |
| A/B variable | Original player acquisition vs `-Dmkm.enemy.directPlayerScan=true`, same exact MKM commit |
| Optional profiles | MKM-only; Sodium+Lithium+FerriteCore (applicable client/server sides); isolated Moonrise; isolated C2ME, each verified for 26.3 **before** test |
| Renderer | QA5 has **no actual R07 17-bone MNAE mob renderer**. Profile 17-joint skinning / HUD at 40 mobs only on a later separately identified integrated candidate, not via QA5 proxy |

Use fresh disposable saves with fixed seed (example test seed `20261108`; never overwrite production worlds). Keep difficulty, coordinates, game rules, time, render distance, simulation distance, Java flags, JVM memory, hardware, GPU driver, resolution, shader selection, Fabric/MKM/dependency hashes, view angle and movement path constant *within each pair*. Disable unrelated background load. Record OS/CPU/GPU/RAM/heap and startup logs.

Use `/mkmenemy stress 20`, `40`, or `80` on known solid terrain. These commands **attempt** to place the requested entities and report the actual spawned count; do not assume 80 were spawned or remain loaded. `/mkmenemy clear` discards tagged prototypes without XP; never use it to clean authored encounters. Record actual entity and chunk counts from real telemetry/spark. A 0 cohort requires a clean new world and confirmation that no tagged enemies remain. Stress spawns are hostile and persistent; isolate trials.

## 3. Evidence collection — never fill gaps with invented numbers

1. **spark:** record authoritative server tick p50, p95, p99 MSPT, profiler start/stop period, sample count, main-thread flamegraph URL and identifiable hotspots per run. Store original spark export/link, not only a copied summary. `/mkmperf ticks` and `/mkmenemy perf` are corroborating subsystem diagnostics only.
2. **Client FPS:** capture average and 1%-low with an external frame-time tool such as PresentMon/CapFrameX on the *same client*, fixed camera/route; for a headless dedicated server use JSON `null` and mark it non-applicable.
3. **Memory/GC:** Java25 JFR or `jcmd`/GC logs record allocation rate, pauses, heap; distinguish client and server processes. OS RAM and process RSS are separately logged.
4. **Chunks:** record actual generation-complete chunks and elapsed window; use chunks/second only when comparing an equivalent new-chunk travel script. Do not interpret FPS as chunk throughput.
5. **MNAE/UI:** only where R07 and relevant 40-mob HUD/rig integration is installed, collect render-thread flamegraph / allocation samples with rig enabled vs disabled, equal camera/draw conditions. Assess per-joint CPU/GPU cost and pose-cache/atlas/LOD *after* those traces; no claimed pose-cache saving in this QA5 branch.
6. **Repeatability:** archive cold and warm first runs separately, include negative results and p99 regressions, and repeat with 2 players + chunk unload/relog. Baseline values cannot be assumed from earlier CI or synthetic unit tests.

### A/B JSON contract

Create one manifest per specific scenario/cohort, e.g. `build/reports/performance/combat40_players2.json`:

```json
{
  "runs": [
    {
      "arm": "baseline",
      "phase": "cold",
      "trial": "01",
      "environment": {
        "minecraft": "26.3",
        "java": "25",
        "hardware": "fill exact CPU/GPU/RAM/OS",
        "mkm_sha": "fill exact SHA",
        "world_seed": "20261108",
        "world_copy_sha256": "SHA256 of identical pre-run save snapshot",
        "scenario": "combat40",
        "player_count": 2,
        "entity_count": 40,
        "render_distance": 10,
        "simulation_distance": 6,
        "duration_seconds": 600,
        "fabric_loader": "0.20.1",
        "jvm_args": "-Xmx4G",
        "modlist": ["mkm 0.0.6-fabric.0-qa.5"],
        "experiment_setting": "directPlayerScan=false"
      },
      "metrics": {
        "tick_p50_ms": null,
        "tick_p95_ms": null,
        "tick_p99_ms": null,
        "fps_average": null,
        "fps_1pct_low": null,
        "gc_pause_total_ms": null,
        "allocated_bytes": null,
        "ai_callback_mean_us": null,
        "loaded_entities": null,
        "loaded_chunks": null,
        "heap_peak_mb": null,
        "chunks_per_second": null
      },
      "evidence": {
        "mspt_source": "spark",
        "profile_url": "",
        "flamegraph_url": ""
      }
    }
  ]
}
```

**This is a deliberately INVALID / unmeasured template**. Replace all `null` core timing values with actual recorded spark numbers and add matching candidate, warm, and second-trial runs. `world_copy_sha256` must be identical across A/B paired copies. Do not put fake values in issue or PR reports.

Run:

```bash
python3 -m unittest discover -s tools/performance -p "test_*.py" -v
python3 tools/performance/showcase_ab.py PATH_TO_REAL_RUNS.json --out build/reports/performance/comparison.json
```

The paired comparator rejects mismatched worlds/runtime/hardware, mixed arms, nonmonotone percentile data, and mixed Moonrise+C2ME configurations. It requires 2 cold + 2 warm **pairs** with spark provenance, at least one flamegraph and all recommended metrics before a *provisional* 10% p95 gain **or** 20% chunk throughput gain can pass. **Every pair** must avoid more than 5% p99 regression. Its paired-bootstrap interval is descriptive; 2 pairs give very weak statistical certainty. A green comparator cannot verify URL authenticity, correct recording, correct game logic or user-visible quality.

## 4. Safety gates and outcomes

- `./gradlew build` with Java25; dedicated-server boot and client boot; no client-only imports in common code; no new Mixins/authoritative damage or XP hooks.
- Behavioral A/B: single/two player, near/far, spectator, dead player, through-wall, bounding-box edge, equidistant players, tracked mob death/unload and relog; confirm target stays equivalent in the same world with same RNG and player locations.
- Verify 20/40/80 actual load, idle vs combat and different dimensions. Run 10–15 minute Showcase script with combat/mining, quest/encounter reward one-shot, save v4, relog, two-client interactions; 30-minute stress recommended.
- Do **not** enable direct scan by default until spark on control machine confirms improvement without p99 or correctness regression. Rollback: remove JVM flag and restart; no save migration needed.
- Do not infer any 26.3 optimizer binary exists from this document: confirm specific Fabric release metadata and license for each external mod. C2ME is **separate experimental copy-world profile**. Moonrise and C2ME must not coexist.
- Changes to MNAE R07, Magic M4, Fencing and Watchtower remain separate draft streams. This branch is **not** a playable omnibus or a client graphics optimization.

**Measured MSPT/FPS, GC/flamegraph, chunk throughput, 2-player game QA and MNAE render cost: NOT YET MEASURED.** The local/synthetic comparator tests are correctness tests for the *tool*, not fabricated performance data.
