# MKM Hybrid Performance Engine — M0 baseline and roadmap

**Status:** experimental, diagnostics-only runtime in `feature/performance-hybrid-m0-diagnostics`; target Minecraft **26.3** / Fabric 0.20.1 / Java **25**. Coordinating [Issue #57](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/57). **No claimed FPS/TPS improvement yet.**

## 1. Architectural decision: hybrid, not all optimizers at once

MKM uses **three layers**:

1. **Optional third-party optimizers installed by the player/server administrator**, version-pinned, unbundled, with explicit compatibility profiles. MKM never downloads other mods, changes their configuration or clones their implementations. Mod licenses are respected.
2. **MKM first-party performance runtime** for cheap, bounded telemetry, immutable resource caches and eventual off-thread *pure* planning for enemies/encounters. Server thread exclusively owns actual world, character, combat, loot, XP, resource and quest mutation.
3. **Reproducible QA/benchmark harness** deciding which optimizations should be enabled. Select based on observed bottleneck, test backward compatibility, save copies, 1–2 players and many mobs. Do not optimize by anecdote or by peak FPS alone.

Minecraft already uses multiple threads (render, networking, chunk tasks); moving arbitrary entity ticks, physics, world writes or rendering API calls off-thread is dangerous and is explicitly **out of scope**.

## 2. Public solutions inventory, with 26.3 caveats

| Area | Candidate | Tested/verified upstream role | MKM plan |
| --- | --- | --- | --- |
| Client terrain | [Sodium](https://modrinth.com/mod/sodium) | Rendering optimization; 26.3 Fabric versions | Optional recommended **client** baseline |
| Game logic | [Lithium](https://modrinth.com/mod/lithium) | Reduces server/game-logic cost; 26.3 versions | Optional client + integrated/dedicated server baseline, backup first |
| Memory | [FerriteCore](https://modrinth.com/mod/ferrite-core) | Reduces memory allocations/storage; 26.3 versions | Optional on both sides |
| UI/entity renderer | [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) | Batches immediate rendering; 26.3 client builds | Optional, measure overlap with Sodium |
| Hidden entities | [Entity Culling](https://modrinth.com/mod/entityculling) | Asynchronous culling; 26.3 client builds | Optional, measure in dense scene |
| Extra culling | [More Culling](https://modrinth.com/mod/moreculling) | More face/visibility pruning | Verify exact 26.3 release/dependencies and MNAE costume compatibility |
| Background FPS | [Dynamic FPS](https://modrinth.com/mod/dynamic-fps) | Saves resources when client unfocused | Optional for laptops; **not** proof of in-game FPS gain |
| Chunk engine A | [Moonrise](https://modrinth.com/mod/moonrise-opt) | Replaces chunk/entity retrieval pipelines, 26.3 | Optional A/B profile; *must not combine with C2ME* |
| Chunk engine B | [C2ME](https://modrinth.com/mod/c2me-fabric) | Parallel chunk loading/generation/I/O, 26.3 builds are experimental/alpha | Optional **separate experimental** profile on cloned saves; use no Moonrise |
| Lighting | [ScalableLux](https://modrinth.com/mod/scalable-lux) | Chunk-lighting optimization | Only after verifying specific 26.3 C2ME combination |
| Networking | [Krypton](https://modrinth.com/mod/krypton) | Network-path optimizations | Optional multiplayer-specific profiling; verify exact 26.3 artifact |
| Player scaling | [VMP](https://modrinth.com/mod/vmp-fabric) | Player/entity scaling | Only if dozens of players; integration testing first |
| World pregen | [Chunky](https://modrinth.com/mod/chunky) | Pre-generates world outside active play | Admin/development workflow for structures/encounters, not runtime MKM dependency |
| Profiling | [spark](https://modrinth.com/mod/spark) | Profile main thread, TPS/MSPT, allocations | **Recommended development tool**, not bundled |
| Shaders | [Iris](https://modrinth.com/mod/iris) | Enables shader packs; may reduce FPS | Optional *quality* profile, not optimization baseline |
| Platform-specific GPU | [Nvidium](https://modrinth.com/mod/nvidium) | NVIDIA mesh-shader-specific optimization | **Not suitable for Ryzen 7 5800U integrated AMD GPU** |
| Historical/uncertain | ModernFix, Noisium, LazyDFU, Starlight | Old/version-specific optimizations | **Do not install without a verified 26.3 Fabric binary and documented compatibility** |
| Native/Rust experiments | [Ferrite](https://modrinth.com/mod/ferrite) (different from FerriteCore) | Native Rust/Fabric performance claims | Experimental isolated benchmark only: JNI/native portability, security and gain must be proven |
| Alternative platform | Paper/Purpur/Pufferfish/Horizon | Independent server/game implementations | **Not drop-in Fabric mod replacements**; incompatible with current MKM Fabric jars/runtime design |

**Primary conflict:** Moonrise explicitly declares C2ME incompatible. It automatically negotiates some Lithium/FerriteCore overlaps but compatibility must be checked per release. Do not install both chunk engines. Source: [Moonrise's mod description](https://modrinth.com/mod/moonrise-opt).

**License policy:** no third-party optimizer jars automatically embedded in MKM; official Fabric API remains packaged as before. Linked mods are optional and user-installed under their authors' licenses.

## 3. Optional deployment profiles (choose exactly ONE chunk strategy)

| Profile | Client | Integrated/dedicated server | Appropriate for |
| --- | --- | --- | --- |
| **A — Conservative** | Sodium, Lithium, FerriteCore; benchmark ImmediatelyFast and Entity Culling separately | Lithium, FerriteCore | Baseline for compatibility, multiplayer and persistent saves |
| **B — Chunk throughput** | A with C2ME as necessary | A with **C2ME** (and separately verified ScalableLux) | World exploration, chunk generation; **cloned test save only** while alpha |
| **C — Moonrise** | A with Moonrise if verified | A with **Moonrise instead of C2ME** | Entity/chunk heavy servers; verify mods affecting internals |
| **D — Cinematic** | Profile A + optional Iris shaderpack, measure FPS impact | A or compatible selected chunk strategy | Screenshots/filming, not maximum performance |
| **E — Laptop economy** | Profile A + Dynamic FPS on background | A, reduce simulation distance only as user setting | Limited RAM and competing software |

All profiles are **suggestions**, not implemented download/install presets. `/mkmperf compat` is observational and never auto-removes conflicting mods.

## 4. Implemented M0 code and operational limits

- `TickTimeWindow`: 512-sample ring, START/END server-tick callback span, p50/p95/p99/max printed via operator command `/mkmperf ticks`. One `nanoTime` pair per tick; sorting occurs only on command. **This is an approximation, not spark's authoritative MSPT**, and must not be used alone to claim speedups.
- `OptimizationCompatibility`: detects Fabric-loaded optional mod IDs and warns on **Moonrise + C2ME** and unverified ModernFix. `/mkmperf compat` shows installed candidates and chosen/ambiguous chunk backend. It cannot prove absence of conflicts in unknown mods.
- `PerformanceBudget`: passive/no-data, normal, caution, busy thresholds, approximate p95/p99. It does **not** adjust mob AI, spawn rate, damage, physics or server ticks.
- `BoundedPureWorkerPool`: an unused **opt-in** utility for future immutable planning snapshots, with a bounded in-flight permit count, rejection instead of tick blocking, and owner-thread callback with stale-state predicate. It is not currently instantiated, so **no additional worker is actually doing MKM AI work** and there is **no performance win claimed**.
- The new command requires operator/game-master permissions and does not mutate world/saves; existing `/mkm` and gameplay remain unchanged.
- `MkmMod` registers the tick diagnostic at the end of its own hook setup. No new mixins, network packets, persistence schemas or runtime dependencies.

## 5. Next modules — staged, measured gates

**M1 — Repeatable profiler capture (QA + AI/World):** use spark with 0/20/40/80 active enemies, identical seed and scripted movement, same render/simulation distances, 1–2 clients, main-thread flames, chunk throughput, mean/p95/p99 MSPT and FPS median/1%-low. Compare profile A vs B vs C on cloned worlds, confirm observed bottleneck before designing optimization.

**M2 — Enemy pure planning (Enemies & AI):** after integrating V1 from #56, create **primitive immutable snapshots** with positions, candidate threats, costs and generation ID on the owning thread; submit expensive scoring only to worker pool; validate entity alive, UUID, dimension, generation and maximum age on owner thread; synchronous fallback under queue pressure; no direct off-thread Minecraft calls. Enable with default OFF until A/B improvement proves useful.

**M3 — Encounter/structure generation (World + Visual):** precompute validated, bounded structure placement and encounter choice *from immutable seed/biome metadata*, no off-thread `ServerLevel`/NBT world mutation; respect engine's existing worldgen scheduling and chunk locking. Coordinate with #54 and visual watchtower branch.

**M4 — MNAE/render (Animation):** preload immutable rigs and animation clips, memoize constant transforms, measure per-frame allocations and joint/bone cost; only consider background tasks for asset baking if the render pipeline explicitly supports it. Actual rendering stays client render thread.

**M5 — Networking and memory (QA + Combat/Magic):** count actual packets, rate-limit redundant *cosmetic* events without dropping authoritative events; review attachment sync, data pack reload caching, optional JVM GC (Java25 G1 vs ZGC as controlled A/B). No blind GC flag templates or mandatory Rust/JNI native code.

**M6 — Hardening:** one versioned optional optimizer matrix, rollback/backup script, integration with approved Fabric v4 branch; stress crashes/unload/respawn/quests and race conditions. Publish exact SHA and environment profile.

## 6. Benchmark acceptance thresholds

- **Measure before enabling.** Require a reproducible >=10% p95 MSPT improvement on same workload, or >=20% chunk throughput where that is the bottleneck; no meaningful p99 regression and no correctness regression. These are **acceptance targets**, not measured gains.
- FPS/low-percentile frame times also matter; include GPU bottlenecks, memory, CPU utilization and actual mod list/versions.
- Test at 20/40/80 active entities; samples should cover at least multiple sustained runs after warm-up. Compare with fixed Java25, RAM and hardware.
- For Ryzen 7 5800U / 16 GiB physical RAM: initial worker-cap experiment <=4 threads **only if profiling warrants it**; leave headroom for client/render, OS and other processes. Avoid arbitrarily assigning most of memory to Java when running local AI.
- Dedicated server and integrated client boot, 2-player, death/relog, XP/quest idempotence, chunk unload/reload, legacy save v3->v4, MNAE and weapon rendering remain the game-safety gate. No direct merge to legacy NeoForge `main`.

## 7. Pre-release checklist

- [ ] Actual Java25 Gradle/JUnit CI on current SHA.
- [ ] Dedicated server/Xvfb client boot tests pass.
- [ ] Operator `/mkmperf ticks` and `/mkmperf compat` behave in-game.
- [ ] No class loading from client-only optimization code in dedicated server.
- [ ] Exact mod list, versions, backend selections and hashes recorded.
- [ ] No C2ME + Moonrise combination distributed.
- [ ] One backed-up save for every migration/chunk-engine experiment.
- [ ] No performance claim without measured before/after on identical workload.
- [ ] Artifact has unique version identity and is not installed over any unbacked world.

M0 establishes the **measurement and safety contract**, not universal acceleration.