# MKM C3.2 — Video-reviewed attack choreography and animation pacing

## Evidence and user feedback
Examined user-supplied Minecraft 26.3 F5 footage recorded 2026-11-01 (18.18 s, 1936×1088, 60 fps). Sampled the animation at roughly 0.12 s attack intervals and 0.08 s roll intervals and reviewed enlarged closeups.

- At approximately 3.2–5.8 seconds, the 3-hit R combo shows arm swings but little perceptual body-weight transfer, and the player is **holding nothing**.
- C3.1 always chose `mkm:one_handed_sword`, so empty-hand punches appeared as unarmed sword slashes.
- At approximately 6.3–6.7 seconds, the forward roll appears compressed into a brief flip, with inadequate plant and landing time.
- The centered stamina bar crosses the avatar's legs/waist during third-person footage.

## Implementation

1. New **unarmed** profile with three authored clips: offhand jab, dominant-hand cross and heavy uppercut; chest/head/guard movement accompanies each punch. Empty main-hand selects unarmed; occupied main-hand retains sword profile. This is a minimal selection rule—not yet a full weapon-class classifier.
2. Sword clips retained as separate animation family. Their torso rotation is strengthened and keyframe timelines re-timed to the longer server action phases.
3. Increase action timing **without moving damage away from ACTIVE**: three combo windup/recovery phases now (5,8), (4,7), (6,11) ticks plus 1 ACTIVE tick. Total 14, 12, and 18 ticks versus prior 11, 10, 16. Correctly reauthor all six impact markers for the new hit tick.
4. Dodge state and all eight clip durations increase from 8 to 10 server ticks (+25% presentation time). The **same one-shot server-authorized directional velocity** is applied; no distance, stamina or collision logic is changed. This is intentional slower visual movement / longer action lockout, not a speed-modifier exploit.
5. Move HUD to bottom-left to reveal the player body in centered F5 video.
6. Existing C3.1 forward-roll axis, grounded proxy compensation and asymmetric shoulder-grouping stay unchanged.

## Automated quality gate

- Gradle/JUnit phase tests updated for longer actions; resource loader asserts clip duration and authoritative impact marker alignment for both profiles.
- Existing 201-sample forward-roll silhouette and clip validation QA updated for 10 ticks and 15 clips.
- New `attack_timing_selfcheck.py` independently checks six attacks against **5/4/6 tick** server hit timings, requires distinct keyframe poses, left-hand jab and right-hand cross, generates `attack-report.json` and `attack-timing.svg` for comparison.
- GitHub Actions bundles both selfcheck reports and visual schematics, builds JAR, boots dedicated server and graphical client under virtual Xvfb display, validates nested Fabric API and installer.

## Manual in-game acceptance test

1. Replace 0.0.5-fabric.1 with **0.0.5-fabric.2**. Test in the same flat-ground third-person F5 location and record from the front/side.
2. **Empty hand + R:** three clearly different moves should appear: left jab, right cross, uppercut. Verify impact is not a purely decorative motion; server still resolves attack and stamina.
3. **Sword equipped + R:** three separately authored cutting animations, larger body weight transfer and correct hit phase. Test left-handed skin if available.
4. **W + G:** forward roll should now take ~0.5 s at 20 TPS, versus ~0.4 s before; other seven dodge animations should have the same slower cadence.
5. Confirm G movement distance and collision feel remain unchanged; acknowledge slight increase in dodge lockout to 10 ticks. Recheck stamina/HUD/K character menu.
6. Check arm clipping, sword alignment, crouch/ground contact and animation blending in a real world. CI has only geometry and boot checks, not true visual-quality acceptance.

## Quality limitations
- This is improved keyframe choreography, not a facial/cloth/IK/retargeted Blender AAA-quality animation set.
- There is no separate weapon-detection matrix: any nonempty main hand currently uses sword presentation.
- Client-only owner timeline means animations do not replicate to other players yet. First-person hand swing remains vanilla.
- Damage/target selection remains server-owned; visual markers never apply damage.
- QA schematic asserts curves and markers; it cannot judge realistic biomechanics or stop a model clipping armor. The submitted footage remains necessary for human visual inspection.
