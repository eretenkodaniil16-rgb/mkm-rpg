---
name: mkm-combat-animator
description: Make grounded MKM RPG sword, axe, dodge and spell motions in Blender with correct grips, key poses and export contracts while respecting server-authoritative combat.
---

# MKM RPG — combat animator

Use this skill for authoring or improving MKM attacks, rolling, dodging, blocking and spellcasting clips.

1. Inspect existing rig, active hand, item family, animation action, export format, current Issue #43 and relevant existing MNAE/Combat code before changing movement. Do not duplicate equipment classifiers.
2. Use approved or permissively licensed motion reference. Specify duration and phases: anticipation, load/weight transfer, acceleration, contact, follow-through, recovery. Camera and frame rate must be fixed for comparison.
3. Animate hips and shoulders independently. Use IK for planted feet and an explicit hand–weapon grip socket. Record the weapon-tip trajectory and verify the blade never crosses the head in intended key poses.
4. Avoid whole-body 360-degree spins for rolls by default. Support-phase motion must reconcile to server displacement, collision, stamina and grounded contact. Do not introduce damage, hitboxes, invulnerability frames or gameplay root motion.
5. Validate left and right hand and sword, axe, shield, empty hand as independent cases. If a weapon is in offhand but no action selects it as the attacking hand, do not pose it as an attacking weapon.
6. Render at least nine evenly sampled frames and extra frames at impact/transition. Review grip drift, foot slip, head/armor interference, curve overshoot and continuity back to idle.
7. Source .blend and fully articulated motions are authoring assets. Existing Blender-to-MKM six-part exporter may only export the compatible subset; unsupported hierarchical bones need a separate MNAE renderer integration review.
8. Deliver exact action names, durations, authored key poses, impact alignment, fixtures, review frames and limitations. Final Minecraft visual acceptance requires real F5 60fps replay and actual renderer diagnostics, not just Blender preview.
