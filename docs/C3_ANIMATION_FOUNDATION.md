# C3 Animation Foundation — architecture, format and content pipeline

## Objective and quality threshold

Move MKM from hardcoded Minecraft model-part rotation switches to a **versioned data-driven animation pipeline**. This is an engineering foundation for high-quality cinematic combat, not an assertion that the first set of programmer-authored poses already matches AAA animation quality.

The previous user-verified **C2.1** is the rollback checkpoint. All C3 work remains isolated in this branch and draft PR until gameplay and visuals are tested.

## Architecture

```
Server action (20 TPS)
  CombatService + ActionTimelineRules
      |  validates stamina, cooldown, direction, hit timing and target
      v
Owner-synchronized ActionTimeline attachment
      |  kind, phase, combo step, phase deadline, dodge direction
      v
Client LocalCombatAnimation
      |
      +-- AnimationController (semantic action → clip, server-phase time → clip time)
      |    +-- AnimationProfile (weapon → action → clip ID)
      |    +-- AnimationLibrary (validated JSON resources)
      |    +-- AnimationClip (bone tracks, markers, easing, additive pose)
      |
      +-- PlayerModelAnimationMixin (render-only additive model parts)
      +-- AvatarRollAnimationMixin (C2.1 full-body visual transform)
```

**Never permit JSON events to inflict damage or move a hitbox.** Clip `impact` is a visual synchronization marker validated against the authoritative server tick. Its presence can later trigger approved client-only sounds, VFX and local hit-stop *after an independently verified server hit event*.

## Asset contract — schema v1

All files reside in `src/main/resources/assets/mkm/animations` and are indexed by `index.json`.

Example clip:

```json
{
  "format": 1,
  "id": "mkm:sword/light_1",
  "duration_ticks": 11,
  "layer": "upper_body",
  "tracks": {
    "right_arm": [
      {"t": 0.0, "r": [0, 0, 0]},
      {"t": 0.36, "r": [-1.8, 0.5, 0.1]},
      {"t": 1.0, "r": [0, 0, 0]}
    ]
  },
  "markers": [{"name": "impact", "t": 0.363636}]
}
```

- Time `t` is normalized 0–1; rotations are **radians** about Minecraft model-part local axes, additive to vanilla locomotion.
- Tracks are strictly ordered, at most 256 keys per bone; they must span 0→1. C3 supports head, body, left/right arm and leg. Clips must be a finite 2–200 ticks. The loader checks sizes, invalid axes, identifiers and NaN/Infinity.
- Layer is `upper_body` or `full_body`; upper-body clips cannot transform legs. Native Minecraft jacket/sleeves/pants copy the final bone rotations.
- Interpolation is per-segment smoothstep. This is compatible with external motion authoring but **not yet quaternion interpolation, IK, inertialization or advanced retargeting**.
- Versioned profiles map `combo_1`, `combo_2`, `combo_3`, `dodge_0`…`dodge_7` and `combat_idle` to clip IDs. Future profiles can add two-handed swords, spears, daggers and spellcasting without editing client Mixins.
- Combo durations are locked to C2 server timings. The required `impact` marker must coincide with the end of WINDUP (server hit). Dodge duration is locked to `DODGE_ACTIVE_TICKS`.
- If a library cannot load, the *existing C2.1 player pose renderer* continues as a fallback. No mod/character/world-save data schema changes.

## First vertical slice

Included keyframe resources:
- One-handed sword: three combo stages with anticipation, torso counterrotation, follow-through and settling.
- Eight directional dodge clips (forward/back/left/right + diagonals) with crouch/tuck/land keys.
- Combat idle breathing reference clip for later locomotion blending.

These are **starter clips** to exercise the runtime and prove external authorability. The next production-quality pass must replace them with reviewed and refined Blender actions; current poses do not imply mocap, advanced cloth, full-body IK or AAA polish. The separate C2.1 360° avatar rotation is currently retained while data-driven dodge tucking replaces hardcoded limb transforms.

## Blender workflow (no extra runtime mod)

1. Create/rest-pose a humanoid rig in Blender with arms and body mapped to the Minecraft character. Aim for consistent skeleton proportions and weapon hand grip.
2. Record an Action for each strike or dodge. Keep the root in place; velocity is still computed by the server.
3. Edit `tools/animation/mkm_humanoid_mapping.json` to match real Blender bone names and calibrate local XYZ handedness.
4. Export via:

```sh
blender -b rig.blend --python tools/animation/export_blender_to_mkm.py -- \
  --armature MKM_RIG --action sword_light_1 \
  --clip-id mkm:sword/light_1 --duration-ticks 11 \
  --layer upper_body --impact-tick 4 \
  --mapping tools/animation/mkm_humanoid_mapping.json \
  --output src/main/resources/assets/mkm/animations/sword/light_1.json
```

5. Register new clips in `index.json`, update the `one_handed_sword` profile if necessary, then run `./gradlew test` to check bounds and hit markers.
6. Test in third-person in the game. This script only exports local rotations; **rig bind-pose retarget calibration and motion review are required**. Blender must be installed for export, not for playing MKM.

## QA and acceptance criteria

- Unit: clip validation, ordered time, interpolation, data immutability, corrupt-resource fail-closed, server hit marker equality and all eight dodge durations.
- CI: compile Java 25, JUnit tests, packaged complete Fabric API, actual dedicated server boot, graphical client bootstrap under Xvfb, installer packaging.
- User QA: three *distinct* strikes with clear anticipation/impact/recovery; smooth transitions and no pose snap; correct handedness, skins and armor; no fall-through on G eight ways; all established C2.1 mechanics intact.
- **Multiplayer**: current action timeline is still owner-only, so other players will *not* see C3 animations yet. Dedicated observer sync must be added before release.
- **First-person**: third-person body only. Hand-specific first-person clip rendering, animation-blend trees, inverse kinematics, procedural foot grounding, authored camera work, enemy reactions and hit-confirm VFX are **future increments**.
- Performance: avoid file reads in the render loop; the validated immutable library loads once on first local action. No network downloads or extra mod dependencies.

## Follow-on C3 milestones

C3.1: Blender calibration plus one professionally authored 3-combo/8-dodge sword set; first-person and proper transition blends.

C3.2: Observer-targeted animation replication with deterministic start ticks, explicit cancellation and rollback tests.

C3.3: hit-confirm event stream with bounded hit-stop, animation trails, audio and procedural reactive recoil that never stalls the server or desynchronizes damage.

C3.4: locomotion/attack blend tree, IK foot/weapon constraints and independent weapon-profile variants.
