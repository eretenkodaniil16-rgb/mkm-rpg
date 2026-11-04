# MKM Sword Slash R02 — reference-first Blender experiment

**Source only.** Start with [the R02 motion plan and reference register](../../../../docs/workflow/MKM_SWORD_SLASH_R02_MOTION_PLAN.md). This pilot stacks on R01 PR #48, which stacks on toolchain PR #47. The unified development process is in a separate draft PR #49 and must not be assumed merged.

## Goal

Create an *original* 14-tick right-hand one-handed diagonal sword slash with body/hip weight transfer, planted right foot, a short left step, right-wrist IK and baked rigid sword grip socket. The visual contact frame is 16 at 60FPS, representing tick 5/14. No MNAE-compatible animation asset or game damage event is generated.

## Reproducible source checks without Blender

From repository root:

    python tools/animation/pilots/r02/validate_r02.py
    python -m unittest discover -s tools/animation/pilots/r02/tests -p 'test_*.py' -v
    python -m compileall -q tools/animation/pilots/r02

## Generate editable .blend and 43 continuous PNG frames

    blender -b --factory-startup --python tools/animation/pilots/r02/blender_sword_slash_r02.py -- --output-dir build/reports/sword-r02 --render

GitHub Actions workflow sword-r02-animation-qa.yml installs Blender and ffmpeg on a temporary Ubuntu runner, then generates:
- mkm_sword_slash_r02.blend (editable 17-bone source)
- manifest.json (frame-by-frame wrist target, sword tip path, head clearance proxy, planted-foot drift)
- review_*.png (ten key poses)
- mkm_sword_slash_r02_contact_sheet.png (10-pose overview)
- mkm_sword_slash_r02_60fps.mp4 (native 0.70 second motion)
- mkm_sword_slash_r02_slow_review.mp4 (3x slowed for critical review)
- mkm_sword_slash_r02_repeat.mp4 (repeating slowed viewing video)

All generated source content should be visually checked before any source acceptance. The rigid sword socket is baked from the animated hand/wrist at every frame. It is not yet calibrated against Minecraft actual held-item layer or the six vanilla body pieces. The geometric head and planted-foot checks are proxies and cannot replace real gameplay footage, actual collision or animation aesthetics.

## QA and safe boundaries

- 14 game ticks, source 60FPS, visual contact frame 16; no client deciding actual damage.
- Right-handed one-handed sword is the only R02 fixture; axe/offhand/left-handed are still MNAE Issue #43 QA matrix items.
- Do not place assets in src/main/resources or merge into the game until an articulated rig-to-render path, weapon handle/tip socket, skin/armor, and real F5 source-versus-game review exist.
- No gameplay code, persistence, networking, resource allocation, paid services or third-party animation assets are changed by this PR.
- If CI fails or the motion looks bad, report FAIL, make minimal authored corrections and rerun; do not call an animated Blender success an in-game PASS.

Reference source license caveats and exact phase plan are recorded in the linked design document.
