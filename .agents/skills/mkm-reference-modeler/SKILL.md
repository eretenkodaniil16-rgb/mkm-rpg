---
name: mkm-reference-modeler
description: Create and audit Minecraft-styled Blender character models from reference sheets for MKM RPG, with geometry, UV, rig and weapon-socket contracts.
---

# MKM RPG — reference modeler

Use this skill for MKM reference-to-Blender character, weapon, clothing, texture or armor jobs.

- Read AGENTS.md, docs/MKM_FREE_BLENDER_TOOLCHAIN.md, the existing Blender exporter and Issue #43. The repository's current integration baseline is Fabric 26.3; old NeoForge docs may lag it.
- Establish front, side, back orthographic references, scale, axes and origin. Identify any details inferred from a single-view image rather than confirmed.
- Use a disposable Blender file. Separate humanoid meshes, clothing/armor, skin, sword and other held items. Favor deliberate voxel/cuboid shape rather than random triangulated generated geometry.
- Author an articulated reference rig with pelvis/spine, shoulders, elbows, hands, thighs, knees, ankles and foot contact markers. Explicitly name right and left hand grip sockets.
- Produce front, side, back and 3/4 reference renders and an object/bone inventory. Review UV atlas coverage, normal direction, handedness, item pivots, armor intersection and silhouette consistency.
- Retarget only to a documented target rig. Never claim the vanilla six-part Minecraft model can reproduce elbow/knee movement or weapon grip hierarchy. The existing six-bone exporter is a lossy compatibility path.
- Do not download paid assets, submit secrets or edit live project resources without review.
- Handoff: .blend source, reference images, UV/mesh names, bone/socket dictionary, axes, test renders, known gaps and version provenance. Blender QA is not in-game PASS.
