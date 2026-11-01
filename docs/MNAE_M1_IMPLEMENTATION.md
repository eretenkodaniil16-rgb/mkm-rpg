# MNAE M1 — first native skeleton/pose engine, two-attack demo

Implements [Issue #33](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/33) and the design decisions of [PR #34](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/34) on top of C3.8 [PR #31](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/31). All changes are isolated in a stacked draft feature PR. No main merge.

## Implemented / deliberate boundaries
- Original independent Java math: bounded immutable `Vec3`, normalized `Quat` with shortest-path slerp, `Transform` translation/rotation/scale; `Skeleton` with parent cycle validation and bind/pivot composition; `NativeClip` validated immutable channels/events; `PoseSampler` normalized time; `PoseMixer` masked ordered override/additive layers; `AnimationPlayer` speed, transition and stop.
- Explicit units: translation in Minecraft ModelPart pixels (1/16 block), local Euler authoring in radians serialized as quaternions, scale unitless. CPU core imports no Minecraft client API; bridge exists separately.
- Strict declarative format 2 under `assets/mkm/mnae` with exactly TWO demo clips: `mkm:mnae/sword_cut` and `mkm:mnae/straight_punch`. Each uses 14 ticks, impact marker 5/14, at most six vanilla bones, authored anticipation / transfer / contact / follow-through / settle.
- `LegacyC3Importer` converts original immutable C3 tracks without changing IDs or events. C3 assets and full C3.8 sampling/render remain present for fallback.
- F8 toggles A/B (C3.8 DEFAULT). Native active only for R three-stage combo's **first** step, sword vs unarmed determined by empty main hand; follow-on combo and all eight dodges remain C3.8. Per render pass, EITHER native adapter OR old C3 additive application runs, never both.
- Native addon on six vanilla ModelParts supports minor authored pivot offsets (max 1.5px) and quaternion-derived rotations, follows left-hand setting, copies sleeve/pants/jacket pivots to avoid detached overlay layers.
- The client HUD names the active path and the actual ModelPart angular delta; it is setup-pose evidence, NOT confirmed pixels drawn.

## Reproducible authoring
`python3 tools/animation/blender/mnae_two_strikes.py --verify` confirms serialized data matches exact authored controls.
`blender -b --python tools/animation/blender/mnae_two_strikes.py -- --blender --render` creates an editable six-part proxy armature, saves `tools/animation/blender/mnae_m1_source.blend` and (with Blender runtime present) renders sample PNG frames under `build/reports/mnae-blender`. There is no proof the Blender executable was available in this development environment; images and binary .blend are not checked in yet. Keyframe source **script + generated JSON** are versioned. The CLI script's verification mode needs no Blender.

`python3 tools/animation/qa/mnae_side_by_side.py --out build/reports/animation-qa` creates a **schematic** side-by-side SVG contact sheet from existing C3 and new MNAE data and a JSON comparison. It cannot establish actual Minecraft appearance.

## Safety / limits
- No PAL runtime, source copies or 3rd-party animation API.
- No changes to CombatService, ActionTimelineRules, DamageSource, damage, stamina, dodge impulses, physical collision or character saves.
- Animation event markers NEVER execute attacks. Impact timing comes from the existing ActionTimeline only.
- Owner-only third-person avatar render. **Normal LMB, first-person viewmodel, remote observers, armor-aware IK, ground-sampled foot planting, and a fully articulated high-bone rig are intentionally NOT implemented in M1**; these require separately reviewed stages.
- Skeleton hierarchy exists in the pure core but vanilla adapter is necessarily a lossy six-part mapping; vanilla has no independently rendered elbows/knees/wrists. Do not describe the result as final cinematic quality.

## QA / acceptance
- Unit test quaternion stability, coordinate transforms, hierarchy cycles, blending masks/priorities, deterministic ticks, interruption/speed, legacy import, author clip schema and impact.
- CI: Fabric 26.3 Java25 Gradle build+tests, 15 existing C3 clips, two MNAE resources, Python Blender-source byte-accurate reproducibility, client/server boot smoke, installer.
- **Actual 60fps F5 Windows gameplay A/B is outstanding.** Run on level flat world: install only one MKM jar; F8 C3.8 and R stage1 with sword, F8 MNAE and repeat identical camera; repeat empty hand; compare start/impact/follow-through/recovery and left handed. Test sprint, jump, armor, shield and modded skins. If difference is not clear, stop to redesign rig/pivot rather than expand clip count.
- Equipment D3 integration is outside M1; authoritative effects remain C3.6 packet and must coexist with `PendingHitProvenance` during the reviewer merge.

## Next stages
M2: non-invasive vanilla LMB swing presentation without duplicate damage or mining regression; weapon family profiles from Equipment; explicit combat/cooldown priorities. M3: separately authored first-person arm+held-item transforms and shield/off-hand handling with no duplicate render. M4: opt-in cosmetic articulation and IK authoring plus armor attach points. M5: server-validated cosmetic action replication to observers and resource profiling.

MNAE M1 isn't accepted until video evidence shows a meaningful visual improvement over C3.8.