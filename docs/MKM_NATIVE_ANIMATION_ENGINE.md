# MKM Native Animation Engine (MNAE) — design decision

**Status:** architecture proposal; user selected first-party original implementation, 2026-11-01. Related: [#33](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/33) (authoritative engine tasks), [#32](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/32) (gameplay/animation QA), [integration PR #30](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/30), [animation PR #31](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/31).

## Decision

Develop **an original, internal animation engine** for MKM RPG. No mandatory Player Animation Library or GeckoLib dependency for the player animation runtime. Do not copy, automatically translate or vendor library implementation. Existing C3 clip data and code are the migration baseline and rollback mechanism, not waste to be replaced wholesale.

PAL documentation is useful as a *requirements/quality benchmark*: blended layers with priorities, smooth fades, custom pivots, held-item transforms, first-person support, Blender/Blockbench import. Its implementation is not the new MNAE source. Its MIT licence permits copying with required notices, but independent original design is the express project choice.

## Why new infrastructure is needed

`LocalCombatAnimation.frame()` only evaluates owner-synced R/G action timelines, so vanilla LMB does not use authored MKM animations. `AnimationController.actionKey()` recognizes only combo and dodge; empty/non-empty mainhand is the only weapon profile split. `AnimationClip.Bone` has just six vanilla model parts and rotation-only Euler channels; `PlayerModelAnimationMixin` adds their angles in local third person. C3.8 angle-delta instrumentation tests setup, not actual framebuffer output. These interfaces prevent expressive grip, independent torso/pelvis movement and true first-person weapon handling. Simply increasing angles cannot create missing joints.

## Proposed minimal APIs (subject to implementation review)

```java
record BoneId(String name) {}
record Transform(Vec3 translation, Quaternion rotation, Vec3 scale) {}
record BoneDefinition(BoneId id, BoneId parent, Vec3 pivot, Transform bindPose) {}
record Skeleton(List<BoneDefinition> bones) {}
record AnimationKey(double time, Transform value, ChannelMask channels) {}
record AnimationTrack(BoneId bone, List<AnimationKey> keys) {}
record AnimationAsset(ResourceLocation id, int durationTicks, List<AnimationTrack> tracks) {}
record AnimationRequest(ResourceLocation actionId, ResourceLocation profileId,
                        long startTick, int sequence, AnimationPriority priority) {}
interface PoseSampler { Pose sample(AnimationAsset asset, double clockTicks); }
interface PoseMixer { Pose mix(Pose vanillaBase, List<LayerSample> layers); }
interface AvatarPoseAdapter { void applyPose(Pose pose, RenderContext context); }
```

These are **illustrative interface shapes**, not approved signatures or completed APIs. Keep math and loading independent of Minecraft client classes for pure unit tests; integrate on the Fabric client side only at the rendering boundary.

## Ownership boundaries

| Source | Owns | Forbidden |
| --- | --- | --- |
| Vanilla Minecraft + Combat Core | legal LMB attacks, hit validation, damage, shields, stamina, target selection and real movement | animation clips applying damage or server movement |
| MNAE core | immutable authored clips, hierarchy/transforms, deterministic sampling, fades, masks, local pose presentation | second damage or stamina calculator; overriding authoritative ActionTimeline |
| Client animation adapter | actual third-person ModelPart and first-person viewmodel application, hand/item/armor mapping | accidentally applying old and new pose systems twice |
| Magic Core | accepted spell state and owner/observer-friendly cast-confirm event | client VFX deciding heal success |
| Integration/QA | CI, actual render filmstrips and multiplayer compatibility | treating headless boot as proof of cinematic quality |

## Migration path

1. **MNAE-0:** retain and test C3.8 reference, capture exact video baseline, freeze the old asset IDs and markers.
2. **MNAE-1:** implement pure `Vec3`/`Quaternion`/`Transform`, skeleton hierarchy, deterministic clip sampler, clip loader, masks/layers, fade priority, old C3 JSON import, and selected adapter. Reproduce **one sword cut and one unarmed jab**, nothing more.
3. **MNAE-2:** connect *normal vanilla LMB* presentation in addition to R special actions. Keep one legal vanilla attack event, preserve mining and client/server correctness. Provide weapon-family classification via Equipment Core and no independent magic/damage engine.
4. **MNAE-3:** first-person viewmodel, hand-held-item and skin/armor tests, user-toggleable low-amplitude camera response, successful Renewal cast-only gesture.
5. **MNAE-4:** optional articulated cosmetic mesh for elbows, wrists, knees, ankles, weapon sockets and foot placement; Blender IK baked offline, client renderer owns only appearance.
6. **MNAE-5:** cosmetic S2C observer synchronization, integration with #30 and selected M2/E2 changes, performance/security/packaging acceptance.

Use a **feature flag** for old C3 renderer vs new MNAE; mutual exclusion is mandatory. Source JSON v1 remains loadable; new format gets a new schema version with coordinate/unit conversion tests. Only delete legacy support following a user-approved visual and gameplay acceptance milestone.

## Rendering / rig facts

A hierarchy of custom pivot transforms can improve bending and full-body movement within limitations of a vanilla model, but it does **not** automatically add physically rendered knees and elbows. Real joints require custom geometry or subdivision and attachment handling. The first-person viewmodel may need its own transform graph; merely reusing the third-person pose does not guarantee readable first-person gestures. Camera tilt and shaking are preferences, not mandatory combat effects.

## Formats and safety

- Declarative source files with versioned schema; no arbitrary script runtime in early versions.
- Resource loading validates IDs, parent graph acyclicity, finite positions/quaternions, clip duration and track/key counts, marker bounds and accumulated memory ceilings.
- Explicit documented world/Blender/ModelPart axes, radians/units and bind-pose conversion.
- Precompute immutable curves and lookup caches at resource-reload; no hot-path JSON decoding.
- Deterministic tick/partial-tick sampling, network sequence ordering and bounded cosmetic packet size; authoritative server retains hit timings.
- Avoid a separate published API/module before the first two attacks are materially better in actual Minecraft.

## Measurable acceptance and stop conditions

**MNAE-1 first acceptance:** real Windows/Fabric 26.3 in-world F5 front/side 60fps, side-by-side C3.8 vs MNAE with matching sequence and timing; one sword slash and one jab visibly communicate anticipation, contact, shoulder/hip connection and recovery; no hand/weapon or armor clipping; no server/timing/stamina changes; dedicated server and CI pass. Document failures, not only successes.

**First-person gate:** correct near-camera arm and item, no double rendering, no camera-forced collision/aim deviation, reduced motion option and valid left-hand configuration.

**Combat gate:** one and only one vanilla LMB attack event; no extra damage on misses, mining, Shift-heavy or R; no fake PvP damage, no observer drift from duplicated S2C events. Server tests required.

**Stop and redesign:** if real in-world footage is not better than C3.8, if custom geometry cannot preserve armor/skin, or if rendering causes unacceptable compatibility and performance regressions. Do not expand to dozens of clips until these pass.

## Immediate delegation

- **«анимки»:** MNAE-0/MNAE-1 core and two visual proofs; no broad rewrite.
- **«бой»:** LMB/hit/weapon presentation contract, charged input and shield-budget tuning, excluding rendering implementation.
- **«магия»:** Renewal event semantics and VFX interface; no new cast machine.
- **«перс и прогрессия»:** preserve modifier snapshot/progression migrations; no direct renderer coupling.
- **«мир»:** NPC animation requests as stable cosmetic IDs, prioritize integrating E2 dialogues.
- **«проверяющий»:** independent review and integration gate, especially #30/#31 interplay.
- **«главный»:** controls architecture decisions and acceptance; **Gemini** critiques architecture and feasibility independently.

Until integration acceptance, all new work remains in isolated draft PRs. Never merge directly to the legacy NeoForge `main`.
