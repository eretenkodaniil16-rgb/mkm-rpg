# C3.3 — Continuous motion curves (first cinematic-motion pass)

## Concrete C3.2 problem
`AnimationClip.sample` applied independent smoothstep easing between *every two adjacent* authored bone keys. This forces the derivative to zero at every intermediate keyframe. On a fast punch or weapon combo, even smoothly authored continuous arm or torso rotation looks stop-start, especially around anticipation/recovery keys.

## Change
- Replace per-segment smoothstep sampling with *shape-preserving cubic Hermite* curves using weighted harmonic interior tangents for nonuniformly spaced keys.
- At an interior key where the authored rotation keeps the same direction, preserve nonzero continuous angular velocity (C1 continuity); at a local maximum, minimum or held key, set the tangent to zero to avoid overshoot.
- Keep zero starting/ending angular velocities to blend cleanly into zero-offset vanilla pose.
- Clamp each sampled axis to its adjacent authored values as a numerical safety guard.
- No asset format changes; same 15 authored clips and the same server-authoritative action durations, impact markers, stamina, dodge physics and collision.

## Automated checks
JUnit verifies: (a) nonzero continuous angular velocity through a monotone intermediate key; (b) accurate authored key values; (c) shape preservation across irregularly timed reversals and constant axes; and (d) original interpolation and invalid-track checks.

## Acceptance and limitations
The patch improves **temporal smoothness**, not the completeness of the avatar's physical rig. It does *not* yet implement hips/root displacement, foot planting, IK, wrist articulation, weapon grip or multiplayer replication. Test both unarmed and sword combos at 60fps in F5 before merging. Watch for any exaggerated apparent speeds around impact: if necessary, tune authored keys, not server hit timing.

## Further cinematic milestones
1. Visual reference sheet: same F5 side/front captures at 60fps for 3 attacks + 8 dodges; check body centroid, head stabilization, feet support, grip and arm clipping.
2. Author lower-body counter-motion and torso-pelvis coordination without altering collision or locomotion.
3. Add per-action blending with an explicit idle/guard pose, transitions that do not snap between combo steps.
4. Weapon-specific grip/handedness plus hit-confirm client VFX/audio tied to authoritative events.
5. Consider true Blender-driven hierarchical rig with extra joints / contact-aware foot placement for filmic-quality combat beyond the vanilla six-part skeleton.
