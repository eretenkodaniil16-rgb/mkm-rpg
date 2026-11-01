# C3.8 — actual pose evidence and biomechanical audit

Base: C3.7 PR #25, branch `feature/c3-7-visible-combat-diagnostics`. Remain a stacked **draft** PR. C3.7 is not visually approved.

## Proven vs unproven
- **Proven by CI:** Java 25/Fabric 26.3 compiler, JUnit, 15 JSON assets, authoritative impact marker alignment, script selfchecks, dedicated-server startup, headless client startup. New C3.8 tests check measured before/after model-part angles.
- **Not proven by CI:** actual Minecraft in-world F5 third-person frame shows intended limb poses; real feet contact Minecraft collision geometry; wrist/weapon grip; realistic locomotion co-animation; 3D armor and held-item clipping; multiple observers.
- C3.7's `RENDERED` string was misleading. It was set after `PlayerModel.setupAnim` applied the clip, **before** the model was necessarily drawn. It could also refer to a clip from the preceding action stage within a two-tick window. C3.8 labels this precisely as `POSE APPLIED`, records max per-axis angle difference against vanilla pose, requires the correct active clip ID and fresh tick, distinguishes neutral keyframes. Even this only proves the render *setup* modified ModelParts; it is not GPU framebuffer verification.

## Why earlier changes were visually weak
1. Six rigid native limbs; no elbow, wrist, knee or foot pivot. Swords/other nonempty items use the same profile, producing grip errors.
2. C3.3 changes spline continuity rather than choreography. C3.4 applies tiny leg offsets; C3.7 doubled them but still no center-of-mass/root motion or planted feet. During locomotion movement blending attenuates footwork.
3. Per-clip angles live on `PlayerModel` while global dodge spin lives on `AvatarRenderer`. Ground-height maths use a rectangular ideal silhouette and a flat plane, not the rendered skinned mesh or sampled block geometry.
4. Swing contact marks are server events; visually convincing follow-through depends on authored multi-part timing, foot stance and hand paths, not merely moving impact markers.
5. Client-only owner attachment means third-party observer animations are not yet synchronized. F5 is the supported visual QA target.

## What C3.8 does
- HUD reports `POSE APPLIED mkm:... X.Y deg`, `NEUTRAL KEYFRAME`, `DIFFERENT CLIP` or `POSE NOT OBSERVED (use F5)`. The measured difference is the maximum absolute local angle delta after applying the clip relative to vanilla setup for six parts.
- Independent pure-Python `tools/animation/qa/biomechanics_audit.py` validates all 15 clips, durations/impact timings, finite/bounded interpolation, per-bone angular velocity and angular acceleration (assuming 20 TPS), and publishes `biomechanics-report.json` plus `c3-8-attack-contact-sheet.svg`.
- Blender preview command (requires Blender installed):
  `blender -b --python tools/animation/blender/render_clip_review.py -- --clip sword/light_1 --output-dir build/reports/blender-review --frames 9`
  Produces controlled viewport-independent image sequences for comparable six-part proxy poses. The proxy does NOT assert real Minecraft rendering. Calibrate Blender axes and bones before approving authoring retargets.

## Human acceptance protocol (required before merging)
1. Verify **exact** JAR 0.0.5-fabric.5 from CI, no other MKM JAR in mods; check HUD version.
2. Record true 60fps Minecraft 26.3 F5 gameplay of each of 3 unarmed strikes, 3 one-handed sword strikes, and all 8 directional dodges on the same flat grid with front/side 3rd-person cameras. Include corresponding HUD text.
3. Test stationary, walking, sprinting, jumping, crouching, turning and first-person. Test left-handed skin, armor, shield, non-sword held item and steep terrain.
4. Compare root/center-of-mass travel, shoulder-hip counterrotation, arm/weapon trajectory, end-pose continuity, elbow/wrist grip approximation, leg motion, landing and visual floor intersection. Separate projection limitations from actual collision.
5. Inspect dedicated-server and multiplayer gameplay: observers do not yet see local action tracks. This is a known feature gap, not a passing test.

## Roadmap
R1 — correct authoritative render evidence, solve vanilla attack overlay conflicts, and collect baseline gameplay filmstrip.
R2 — Blender semantic armature: independent hip/pelvis, knees, ankles, shoulders, elbows, wrists, weapon-grip sockets; retarget to vanilla via documented lossy-bake path; maintain skin compatibility. Do not silently pretend true IK on six-part model.
R3 — opt-in cosmetic hierarchical player overlay with tested held-item/armor layers if vanilla rig cannot meet quality bar. No collision, stamina or root-motion gameplay changes.
R4 — contact-aware foot lock and support state for slopes / landings; use world geometry only on client for presentation and clearly separate from hitboxes.
R5 — equipment-specific animation profiles and data-driven hit/guard reactions; D3 exact source attribution and C3 confirmed-hit effect must be integrated by independent reviewer.
R6 — future magic: author windup/cast/release/recovery visual profiles reading server-authored ability state, never client-decided spell or damage.

## Regression invariants
- No modification to `CombatService`, `ActionTimelineRules`, `DodgeMotion`, `CombatRules`, `CombatEvents` or character schema in C3.8.
- Same 15 clips, same impact markers, same dodge timeline, same clientbound HitConfirm contract; no new damage logic or resource spending.
- No merge to main without recorded gameplay QA and independent review.
