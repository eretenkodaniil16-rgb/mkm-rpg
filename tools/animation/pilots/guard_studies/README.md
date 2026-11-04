# MKM Three Sword Guards — real Blender output (source-only)

This study is generated from the user's three approved reference concepts and [pose specification](../../../../docs/workflow/MKM_FIRST_THREE_SWORD_STANCES.md). GitHub Actions runs Blender 4.0.2 without the user's laptop. All poses share a single **17-bone character rig** and a single **rigid hand.R-bound sword mesh**.

## How to reproduce

```bash
blender -b --factory-startup --python tools/animation/pilots/guard_studies/blender_three_guards.py -- --render --out build/reports/three-guards
```

Then open `MKM_Three_Sword_Guards_v1.blend`. Select frames **1 / 20 / 40** to switch **High Outer / Mid Guard / Low Mobile**. The transition between these discrete authoring poses is **constant** and is not a game-ready animation. Cameras `CAM_FRONT`, `CAM_BACK`, `CAM_SIDE` provide matching nine orthographic views.

## Observed QA on tested source commit 56925ae393886bf5e78512513e70506a03938686

[Actual GitHub Actions run](https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41916724829) **PASS**.

| Guard | Left knee bend | Right knee bend | Wrist IK error | Sword hand binding |
| --- | ---: | ---: | ---: | --- |
| 01 High Outer | 27.17° | 29.21° | 0.00006 scene units | 100% hand.R |
| 02 Mid Guard | 25.58° | 28.94° | 0.00005 scene units | 100% hand.R |
| 03 Low Mobile | 59.72° | 57.99° | 0.00002 scene units | 100% hand.R |

The source script measures blade axis alignment dot=1.0 in all three poses, and toe markers ~0.02 above the reference plane. Critical render, IK, sword and knee-range gates pass on this commit.

**IMPORTANT — appearance is still an early voxel placeholder.** The generated previews use the R01 simplified armor model, not the detailed character of the approved illustration. The frontal view of Low Mobile does not yet reproduce all of the requested wide stance and free-arm counterbalance. The pose is therefore **technical source PASS, visual acceptance PENDING**. It must not be represented as exact reproduction of the concept art.

Do not export 17-bone pose tracks via the old six-piece vanilla renderer. Minecraft MNAE actual held-item and real F5 validation remain blocked by Issue #43. No Java, gameplay, save data or resource files are changed.

## Suggested next correction

Improve model silhouette (belts/scarf/hair/boots and armor layering), broaden Low Mobile's frontal spacing, verify camera-space blade outline across 3 views and then obtain user approval. Only after this proceed to motion poses/actions.
