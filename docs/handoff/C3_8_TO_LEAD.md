# Handoff for project lead — animation review C3.8

Status: code prepared in draft `feature/c3-8-pose-evidence-motion-qa` stacked atop C3.7 PR #25; not merged.

## Visual assessment (evidence hierarchy)
1. C3.7 improved numeric leg rotation but user observed no convincing change; in-world renderer frames for C3.7 have **not** been supplied to this workstream. Thus no defensible "cinematic improvement in gameplay" claim.
2. Exact code audit: all animation clips affect only 6 native pieces, without independent knees, elbows, wrists, ankles or root center of mass. During walking and jumping additive poses conflict with/attenuate vanilla gait. Roll contact model uses flat plane and a rectangular approximation, not Minecraft terrain. Held nonempty item always maps to one-handed sword.
3. C3.8 separates model-part pose evidence from on-screen pixels and generates per-clip angular-motion statistics/contact sheets for side-by-side authoring. Blender exporter is retained; preview script added for reproducible schematic frame sequences. Neither is a real in-game F5 capture.
4. Next investment priority is a richer cosmetic rig/retarget pipeline, **after** game-side evidence and compatibility tests. Do not mass-author 30 weapons on unvalidated six-piece rig.

## Required lead decisions
- Approve opt-in cosmetic high-bone-count render rig as a future milestone ONLY after vanilla compatibility, armor layers, animations and budget reviewed.
- Agree weapon-class animation profiles via Equipment D3/D1 stable item classification; unarmed, sword, axe, polearm, shield casting must not be inferred merely from nonempty main hand.
- Separate animations from damage, stamina, movement and saves. Explicitly maintain old JAR rollback and avoid main merges.

## Request to capture
With CI-produced 0.0.5-fabric.5: Minecraft F5 at 60 fps (side and front), 3 punches, 3 sword swings and eight rolls, stationary then walking, test left-hand and shield. Show `POSE APPLIED` with max delta. Videos and clip screenshots required to advance QA from "technical PASS" to "visually accepted".

## Remaining defects
No objective actual-game footage yet; weapon grip and torso-pelvis transfer underdeveloped; roll world-ground contact unverified; no extended hierarchical rig; owner-only animation synchronization. Ground-truth comparison and cinematic grading remain pending.
