# MKM RPG — SHOWCASE-01 / Issue #73, R07-on-CharacterData-v4 vertical slice

**Status: draft engineering result, NOT real 60 FPS game visual acceptance.**
**Branch:** `feature/showcase-mnae-r07-fabric-v4`, based exclusively on QA5 `integration/fabric-v4-world-origins-enemy-perf-qa5` (PR #69, base SHA `440dfe07f67d4cce1a8fc10f2bd62a5a2574e137`).
**Reference:** Issue #73, PR #70 115-decision GDD, PR #63 old R07, PR #46 weapon bug, PR #49 evidence workflow, Issue #43. **Source R06 remains immutable.**

## 1. Exactly what was ported — and what was NOT

Ported from old PR #63 *only* `Vec3`, `Quat`, `ArticulatedClipV3`, 17-joint `ArticulatedAvatarLayer`, Blender `export_r07_blender.py` and matching 43-frame quaternion/socket goldens. QA5's `MkmClient`, `MkmCombatHud`, `LocalCombatAnimation`, `AnimationController` and two render mixins were surgically extended, never replaced with CharacterData v2 implementations.

**No modifications to:** `CharacterData`, `CharacterEvents`, `MkmMod`, `CombatService`, `CombatEvents`, `DamageSource`, server `ActionTimelineRules`, stamina, movement, physics, XP, World, NPC, Encounter, Enemy AI, Equipment or inventory persistence. No PAL dependency, new melee damage callback, player hitbox, or state attachment.

**A/B/C modes:** F8 OFF is **QA5 C3** (based on C3.7; not mislabeled historical C3.8), F8 ON/F9 OFF is `R07 SIX_PART_DERIVED` (same exact R07 17-bone sampled data deliberately projected into six independent vanilla parts), F8 ON/F9 ON is `R07_17` (17 independently drawn cuboids) for eligible local-player sword combo1. The six-part comparison is NOT the old independent six-part MNAE animations; it isolates rig articulation on the same source/timing.

## 2. Frozen asset provenance (automatically verified)

R06 annotated tag `mkm-guards-r06-approved` peels to `5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8`. Source .blend SHA-256 `024d2f85e0bef85ec3bb493d0011ceaf7c147c41131048635db76d7305c05581`. Mid Guard frame20 `MKM_GUARD_MID_02`, 17 R06 bones. Exporter creates a **new derivative** source `MKM_R07_Mid_Guard_Cut_Derived.blend`, 43 samples at 60 fps (14 game ticks), and nine Blender frames. Only that derivative is altered; approved .blend is not rewritten.

The bounded schema v3 parser now checks both exact source SHAs, tag, rest pose and coordinate/quaternion conventions. `r07_mid_guard_cut_v3.json` is generated during GitHub Actions before the Java build and embedded as a resource in the experimental JAR.

## 3. Blender → Fabric transform chain

| Stage | Coordinate space / numeric contract | Risk/test |
|---|---|---|
| R06 source | Blender meters, right X / forward -Y / up Z, 17 bones, constraint-evaluated after IK | Frozen SHA/tag, source read-only |
| R07 exporter | `B = [[1,0,0],[0,0,-1],[0,1,0]]`, scale `24/1.8` Minecraft model pixels/meter | Geometry and all 43 quaternion/socket goldens |
| v3 `pivot` | `(x*s, 24-z*s, y*s)`, model root coordinates, scale is strictly unit | Validation of 17 complete samples; no shear |
| v3 quaternion | `R_model=B*R_blender*B^-1`, xyzw, normalized, world rest-to-evaluated bone delta | Quaternion interpolation + 43-frame attachment golden |
| v3 local channels | Local translation/rotation relative to parent; preserve complete hierarchy | Serialized, not down-baked to six parts |
| v3 mesh binding | R06 rigid vertices relative to their bone rest pivot, 21 voxel cuboids, 64×64 UV hints | Visual skeleton exists; real skin seam acceptance open |
| Fabric R07-17 | One `ModelPart` for each articulated bone with independent pivot/orientation, cached geometry per renderer | Client integration smoke + game F5 required |
| Vanilla held item | `translateToHand` redirected to computed socket only on supported mainhand R07 sword | Blender mathematical golden PASS; extra `ItemInHandLayer` transform is **NOT calibrated in world** |
| C3 fallback | Original QA5 PlayerModel + avatar rotations and standard held-item behavior | No double geometry; unsupported items stay vanilla |

For the old R07 Blender exporter, 43-frame source reported spherical *proxy* minimum blade–head clearance **0.16931 Blender units** and fixed IK toe XY drift **0**. These are model-space controls, **not** proof of shader item/armor/camera clearance.

## 4. Gameplay/render isolation

- New `R07ModeGate` is pure and tested: chosen renderer is exactly one of `C3_FALLBACK`, `SIX_DERIVED`, `ARTICULATED_17`; false/missing asset/layer, wrong player, wrong weapon, offhand occupied, armor or first person returns C3.
- Only owner-synchronized R combo stage1 can activate R07, never generic LMB/mining or enemy damage; same normalized `AnimationController` phase at server windup impact 5/14.
- All F8/F9 toggles are client-local; no network packet, no damage authority.
- Experimental layer caches a 17-joint geometry per renderer and submits on eligible F5 frames; previous PlayerModel visibility values are saved/restored to avoid cross-avatar or cross-mod invisible-body regression.
- Unsupported axe/pickaxe/bow/offhand-only weapons no longer get a sword clip or unarmed sword-bearing attack; original dodge clips remain available.
- A newly isolated `DodgeVisualChoreography` introduces **prototypes** for forward tuck, side step and backward hop; all 8 directions have distinct bounded root tilt/hop, **no rigid 360°**. This is *not* foot/palm IK contact or final athletic choreography; actual terrain contact is still pending.
- No remote observer sync yet: the old timeline is owner-only. **Two-player spectator acceptance is pending, explicitly non-green.** Server-authorized visual identity must be designed without client-authoritative hits.

## 5. CI and visual gates

E0: QA5 lineage, v4 preserved by `compare_commits` no CharacterData or XP/gameplay files changed.
E1: immutable R06 tag/Blender source and 43 v3 samples regenerated in CI, 9 genuine Blender comparison images.
E2: Java25 Gradle/JUnit, 43-frame Blender↔Java grip/tip golden, skeleton/schema safety, dodge/three-way gate tests and Python 60fps capture protocol tests.
E3: embedded Fabric API/JAR validation, dedicated server and virtual-display client startup. Source download failures in Loom must be reported as infrastructure failures, not visual pass.
E4 **RED**: user-controlled same-camera Minecraft F5 60fps recordings for C3 QA5 vs six-derived vs full R07; main/offhand, Steve/Alex, armor, head/torso/feet, blade tip, jump/run/ground, and two-player spectator. Follow `tools/animation/r07/README_F5_RU.md`.
E5 **RED**: v4 world copy, saved-state migration, equipment/remote network review, independent QA and release sign-off.

**Do not merge to historical NeoForge main and do not ship as production version.** This is an isolated QA6-R07 test candidate, not an omnibus Showcase-01.
