# MNAE-1 independent QA remediation — scoped implementation

Baseline: native MNAE draft PR #40 (`e8f40441`), independent QA PR #38. **Do not merge into PR #30 or historical main without actual in-game visual acceptance.**

## Confirmed original failures
- Independent JUnit 63 executed, 2 failed at https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41267328620 : TRS nonuniform-scale/rotation shear loss and legacy C3 sampling mismatch.
- The developer's unmodified two-attack CI was green; these independent gates tested different invariants.

## Minimal audited changes
1. `Transform` now accepts only positive **uniform** scale. Quaternion+translation+3-scale representation cannot represent general affine shear from a scaled, rotated hierarchy, so the invalid case is rejected rather than miscomputed. Future articulated rigs needing nonuniform scale must use an explicit full affine/matrix path and tests.
2. `LegacyC3Importer.convert` keeps the immutable original C3 clip as a legacy interpolation policy inside the new `NativeClip` record. The native sampler evaluates the original C3 Euler PCHIP at runtime for imported C3 assets, converts sampled orientation to a quaternion and does NOT retime the clip or its marker. Format-v2 MNAE clips continue to use quaternion SQUAD plus continuous vector interpolation; no changes to their authored choreography.
3. Independent QA continues to check uniform multi-level composition, nonuniform rejection, two bundled demo clips, and C3 intermediate pose parity. The old renderer still remains as a safe fallback.

## Deliberate limitations and follow-on work
- This makes the **quaternion representation** of C3 interpolated rotations exact within numeric tolerance. On a ModelPart adapter that adds Euler angles, converting the quaternion back with principal-angle Euler decomposition can still change the additive component triplet near singularities; a dedicated legacy Euler render adapter is required if the imported C3 path becomes a runtime replacement.
- No full matrix hierarchy, first-person viewmodel, normal LMB combat presentation, independent observer support, Blender armature-to-ModelPart coordinate golden fixture or real in-game 60fps filmstrip has been demonstrated.
- Legacy C3 class contains no Minecraft API and is referenced here only from the pure compatibility bridge; MNAE runtime remains client-side.
- No damage/stamina/timeline/magic/character/world changes. Only validation, compatibility policy, test assertions and this QA document changed.

## Resource loading byte cap

The v2 `NativeClipLoader` additionally rejects JSON inputs over 1 MiB **before** building Gson objects. Track/bone/marker counts are still checked by `NativeClip` after parsing. This is a per-resource bound, not a global reload budget; external resource-pack support and atomic reload remain future work.
