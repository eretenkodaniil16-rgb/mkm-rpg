# MKM RPG — approved Blender sword guard source R06

**Status:** APPROVED authoring baseline (2026-11-08). This is a **static Blender design/source milestone**, not a completed combat animation, not an MNAE runtime export, and not a Minecraft implementation.

## Canonical frozen sources
- Repository branch: `tools/three-guards-blender-source-v1` (PR #53).
- **Immutable approved snapshot:** tag `mkm-guards-r06-approved` after archive commit. Check that tag out rather than a moving branch for reproducibility.
- Permanent asset folder on that branch/tag: `assets/animation/approved/mkm_sword_guards_r06/`.
- File `MKM_Three_Sword_Guards_R06.blend`: one 17-bone authoring rig, three selected static pose frames **1, 20, 40**, one right-hand-bound sword, three orthographic cameras.
- `stance_01_front/back/side.png`, `stance_02_front/back/side.png`, `stance_03_front/back/side.png`: nine actual Blender Workbench reference renders.
- `manifest.json`: Blender geometry, projection and IK QA results.
- `pose_targets_r06.json`: immutable approved pose targets and weapon vectors.
- Reproducible generator: `tools/animation/pilots/guard_studies/blender_three_guards.py`, on that tag.

## Frozen guard identities
1. `MKM_GUARD_HIGH_OUTER_01`: right hand and sword **in front of the head**; blade nearly horizontal, pointing inward. Target `wrist_xyz=[0.29,-0.44,1.57]`, `blade_direction_world=[-0.97,-0.02,0.22]`.
2. `MKM_GUARD_MID_02`: previously approved chest-height protective diagonal; **must not be altered**. Target `wrist_xyz=[0.52,-0.35,0.90]`, `blade_direction_world=[-0.45,-0.66,0.60]`.
3. `MKM_GUARD_LOW_MOBILE_03`: lowered, staggered stance; blade points **inward and about 35° down from horizontal**. Target `wrist_xyz=[0.52,-0.38,0.84]`, `blade_direction_world=[-0.79,-0.34,-0.49]`; measured front projection **−145.08°**.

## Acceptance evidence
- GitHub Actions run: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41962585609
- Blender geometry test result: PASS, all three guards. This includes hand attachment, wrist reach, head/floor clearances, actual knee flexion ranges, and projected sword angles.
- **Visual acceptance:** user explicitly approved all three R06 poses for use as future **source models**.

## Preservation and iteration rules
- Treat archived R06 files as **read-only golden reference**. Never overwrite `.blend`, poses or reference images in place.
- Create new branches or versioned asset folders for new animations, rest transitions, timing curves, character mesh/detail improvements, retargeting or MNAE implementation.
- Do not conflate geometric Blender authoring QA with in-game F5 render acceptance or full-body animation quality.
- If the archive has not yet been committed, see GitHub Actions workflow `mkm-archive-guards-r06.yml`. Its job renders Blender and commits immutable assets with the approved tag.
