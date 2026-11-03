---
name: mkm-animation-qa
description: Independently validate Blender-to-MNAE animation, hand and weapon family selection, grip, ground contact, runtime frame parity and regression risk for MKM RPG.
---

# MKM RPG — animation QA

- Inspect docs/MKM_FREE_BLENDER_TOOLCHAIN.md, AGENTS.md, Issue #43, the current Fabric integration branch and animation QA scripts. Do not assume historical MNAE and QA draft branches are merged.
- Source validation: finite and bounded transform data, correct animation IDs/timings, active hand, equipment family, world/local transform composition, registered weapon grip, and no accidental fallback to sword for axe/pickaxe.
- Ensure source frame comparisons are performed at fixed camera and include at least nine phases plus impact/recovery. Check weapon-tip path, handle-to-hand drift, foot contact, root compensation and intersections.
- Legacy parity: for 15 C3 clips and current interpolation contract where applicable, compare sampled poses rather than only keyframe equality. Keep semantic damage and authoritative gameplay out of cosmetic animation events.
- Game QA: use identical mainhand sword, active hand and first R strike in both F8 modes; inspect actual Minecraft F5 60fps output, renderer, actual clip, reason for fallback and JAR SHA.
- Matrix cases: mainhand sword; axe; pickaxe; bow; shield; offhand-only sword; empty hands; left-handed setting; combos; 8 dodge directions; grounded vs air; first-person; remote observer and dedicated server.
- Distinguish Blender render PASS, JUnit PASS and real Minecraft gameplay PASS. A HUD POSE APPLIED message or Blender proxy render does not prove final framebuffer pose.
- Final report must show PASS / FAIL / BLOCKED / UNVERIFIED with evidence paths, exact reproduction instructions and smallest proposed correction. Do not merge without migration safety and real-client visual evidence.
