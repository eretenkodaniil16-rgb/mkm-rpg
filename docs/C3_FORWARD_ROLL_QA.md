# C3 Forward Roll Choreography Fix + Automated Animation QA

## Reported defect (Minecraft 26.3 / C3 0.0.5-fabric.0)

The player confirmed all eight dodges and the new full-body animation system work, but forward (G+W) feels physically implausible and choreographically wrong. Other directions appear acceptable.

### Root cause in source

1. The C2.1 `AvatarRollAnimationMixin` rotated the vanilla player **around positive local X** for forward dodge. The avatar looks toward **negative local Z**. Rotating positive X sends the top of the body toward positive Z: this visually produces a *backflip during forward travel*.
2. The renderer rotated around a **0.75 block** high pivot while the player's model is approximately 1.8 blocks tall, so in inverted positions the silhouette could project below the ground plane.
3. A uniform full eight-tick 360° spin continued after the initial velocity impulse had decayed. The authored `dodge_0` pose was symmetrical in both legs and lacked a readable shoulder plant / push-off / recovery.

These affect **visual animation**, not server physics. The user-observed 'broken physics' is consistent with a mismatch between visible body kinematics and collision movement; the actual client/world physics has **not** been measured and should not be described as fixed.

## Focused changes

- `ForwardRollChoreography`: forward-only rotation about **negative X**, turning at 6–76% progress and leaving time to settle. The 0.9-block mid-body pivot and dynamic vertical correction prevent the projected rectangular avatar proxy from passing below the flat reference plane during a full rotation.
- The existing `AvatarRollAnimationMixin` uses this **only** for exact direction `FORWARD`. All other seven directions preserve their C3.0 renderer and their existing movement.
- `sword/dodge_0.json`: new asymmetrical bone keyframes for a planted step, bent knees, shoulder tuck, followed by a push-off and stand-up. Markers `takeoff`, `shoulder_contact`, `land` are aligned with these stages.
- Server-side `DodgeMotion`, `CombatService`, attack phases, stamina, collision, hitbox and the C2 combo mechanics are **unchanged**.

## Automated animation self-check

Every CI run on this branch performs:
- JUnit: correct forward rotation sign, bounded and monotone angles, smoothness, projected non-penetrating ground proxy in **201 samples**, standing start/end, nonfinite-input handling and unchanged server directional impulse;
- Python validation of all 12 indexed C3 clips, start/end keyframe coverage, finite angles, shoulder/landing markers and asymmetric forward crouch;
- an independent 201-frame flat-ground silhouette projection check;
- an SVG storyboard (`forward-roll-storyboard.svg`) and machine-readable `report.json`, uploaded as the GitHub Actions artifact `mkm-c3-animation-qa-report`;
- Gradle build/tests, Fabric API bundling, dedicated-server and Xvfb graphical-client **bootstrap** smoke tests, installer packaging.

### What this check does **not** prove

- It cannot evaluate visual quality like a human animator or AAA motion editor.
- The ground silhouette is a bounding **proxy**, not the real multi-bone Minecraft model, weapon or armor; model limbs may still clip the terrain.
- The test does not run a live world and does not verify server/client interpolation under lag, collisions near walls or the player actually performing a roll in the GUI.
- FPS, network desync, caves, slopes, particles and animations in first person still require real in-game testing. Independent captured in-world replay is a future QA milestone.

## User validation

1. Back up any important world. Install **only** `mkm-rpg-0.0.5-fabric.1.jar` in the Fabric 26.3 `mods` directory.
2. Press F5 and face an identifiable block or direction on flat ground.
3. Hold **W**, tap **G**. The torso should lean *forward* and the body roll *over the leading shoulder* without a backward somersault. The animation finishes before the end of the dodge state so the character can settle back into a standing pose.
4. Compare G+S, G+A, G+D and all diagonals to C3.0 — no behavioral change expected.
5. Look for head, feet, sword clipping, snapping, any actual teleport, and verify stamina usage unchanged. Test against a low wall but avoid inferring server collision guarantees from a visual smoke.
6. Confirm 3-hit R combo and K character screen still function.

The existing C3.0 remains available. Keep this PR draft until in-game Windows third-person confirmation.

## Next high-quality animation milestones

A static pose/proxy selfcheck is only a first gate. Production animations need a Blender rig with root and shoulder control, real video/screenshot captured from the Minecraft client, frame-by-frame collision/foot-plant analysis, objective regression diffs and human aesthetic review. Then add camera-aware first-person animation, richer inverse kinematics, multi-agent action replication and motion profile tuning.
