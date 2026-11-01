# C3.5 — Directional roll ground-contact proxy

## Context
C3.1 fixed forward G+W by rolling about negative local X and calculating ground offset for a simple rotating body envelope. The seven other directions in C3.2 continued to use a 0.75-block pivot, no ground offset, and a positive local X direction for forward diagonals. That can make those moves appear to flip backwards or clip through a flat reference ground plane.

## Change
- Keep **forward G+W exactly on the tested C3.1 trajectory**.
- For the other seven dodges, use `DirectionalRollChoreography`, a pure, deterministic third-person **visual** sampler with a 0.9-block pivot and an analytical minimum-height correction for a conservative cuboid.
- Change the X-axis sign for diagonal/backward components to match the avatar's local -Z forward vector; lateral Z-axis sign remains the existing right/left behavior.
- Stage visual rotation over 6% plant, 78% tumble, 16% settle with zero angular velocity at boundaries. No changes to actual motion or stamina: the server's one-shot dodge impulse and action duration are unchanged.
- 201-sample unit QA for all seven roll directions, including normalized axis, grounded silhouette, finite values, no reverse angular motion, no frame-scale jumps and untouched server impulse.

## QA & limitations
This is **not** collision detection or physical ragdoll: it only aligns a projected bounding silhouette to an ideal horizontal ground plane. Slopes, 3D terrain, neighboring blocks, armor and dynamic entities can still clip in-game. The existing six-part avatar cannot perform a true shoulder roll with contact-aware hands and knees.

Test all 8 directions in third-person over flat terrain and slopes, with sword, shield and armor. Compare forward to C3.2 and diagonals to old captures; verify there is no backwards spin or apparent feet-through-ground. In multiplayer and on low TPS, visually verify that rendered roll phase remains aligned with the server dodge timeline. Draft until F5 gameplay review.
