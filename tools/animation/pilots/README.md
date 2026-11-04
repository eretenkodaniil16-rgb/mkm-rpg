# Sword Slash R01 — articulated Blender authoring pilot

This folder contains an experimental Minecraft-style sword animation source for MKM RPG. It does not alter the game or generate a shippable MNAE clip.

## Files

- sword_slash_r01.json: 60 FPS authoring poses, frames 1..43, 14 game ticks, visual contact on tick 5 / Blender frame 16.
- validate_sword_pilot.py: no-dependency checks of poses, bound angles, neutral endpoints and tick timing.
- blender_sword_slash_r01.py: Blender background generator of an editable 17-bone voxel warrior, armor and right-hand sword, leg IK targets, action, nine scene-space samples and review render frames.
- tests/test_sword_pilot_spec.py: independent offline validation tests.
- .github/workflows/sword-pilot-static-qa.yml: static CI and actual headless Blender render job with downloadable artifact.

## Run offline tests

From the repository root:

    python tools/animation/pilots/validate_sword_pilot.py
    python -m unittest discover -s tools/animation/pilots/tests -p "test_*.py" -v
    python -m compileall -q tools/animation/pilots

## Generate editable Blender source

    blender -b --factory-startup --python tools/animation/pilots/blender_sword_slash_r01.py -- --output-dir build/reports/sword-pilot --render

After **successful** execution, the folder contains mkm_sword_slash_r01.blend, manifest.json, and nine PNGs named review_001.png through review_043.png for specific pose phases.

The workflow can execute in GitHub Actions on a free Ubuntu runner with Blender installed by apt. A workflow failure means there is **no verified source artifact**. Inspect the logs and output before assuming a Blender model exists.

## Technical boundaries

- No reimplementation of MNAE, no damage, stamina, hitbox, server movement, character data or gameplay changes.
- The 17-bone source rig cannot be represented faithfully by current six-part vanilla avatar or exported losslessly using tools/animation/export_blender_to_mkm.py. The pilot intentionally stays outside src/main/resources.
- The leg IK targets are Blender authoring helpers, not player collision or terrain-following physics.
- The sword geometry shares the hand.R bone; that guarantees *authoring* attachment, not held-item mapping in the Minecraft renderer.
- Frame 16 is aligned to a declared visual impact reference (tick 5), not a source of combat events.
- Nine Blender renders and QA metrics do not validate the Minecraft framebuffer. In-game F5 60 FPS and actual hand/equipment profiles are outstanding in Issue #43.
- No third-party paid services or accounts needed to generate the source.

## Real review checklist

Check the nine frames in sequence: right-handed grip, weapon tip through anticipation/contact/recovery, no head/armor intersection, both feet planted, no sudden rotations, neutral transition. Adjust motion source if checks fail. Only after the MNAE hierarchy and held-item contract are repaired may a separate engine-format export and real F5 A/B test be accepted.
