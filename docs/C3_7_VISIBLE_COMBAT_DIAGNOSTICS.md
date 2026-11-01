# C3.7 — visibly distinct combat motion + real renderer diagnostics

User reports little or no visible change in C3.6. This is plausible: C3.3 only changed curve tangents; C3.4 attack leg angles peaked at ~0.11–0.22 radians (6–13°), and the grounded footwork weight was already zero at ~0.14 blocks/tick. C3.5 adjusted visual pivot maths; C3.6 only added small particles on confirmed damage.

## Changes
- Six attack clips: double additive left/right leg pitch offsets (up to ~0.44 radians / 25°), slightly amplify torso rotation. Preserve hand animation tracks, impact markers, clip durations, server physics, stamina and all dodge choreography.
- Partial stance contribution while moving on the ground: fade to a **0.2** additive weight beyond 0.25 blocks/tick; airborne contribution still zero. This is cosmetic, does not alter real footing or player velocity.
- Add truthful renderer instrumentation: `AnimationRenderProbe` records actual invocation from `PlayerModelAnimationMixin` **after** applying an authored clip. A server timeline or asset presence alone does not count as rendering.
- Bottom-left HUD shows actual mod version from FabricLoader plus `[C3.7]`; while a combat action is active it reports either `RENDERED mkm:...` or `NOT RENDERED (use F5)`. The clip status is intentionally shown only while the action is active.
- Unit and static self-checks adjusted to assert stronger, still bounded full-body tracks. All six original server impact markers remain untouched.

## Strict acceptance
1. Confirm HUD displays `MKM RPG 0.0.5-fabric.4 [C3.7]` — if not, an older JAR or another game instance is running. Check `mods`, startup `latest.log`, launcher profile.
2. Press F5 and R with an empty hand; during 3-stage combos look for `RENDERED mkm:unarmed/jab`, `cross`, `uppercut`. A `NOT RENDERED` status while the avatar is on screen is evidence to investigate the renderer bridge, not animation assets.
3. Test sword in main hand and look for `RENDERED mkm:sword/light_1...3`.
4. Compare stationary / walking / jumping. Verify legs do not unnaturally override vanilla gait or clip into armor.
5. Confirm all eight dodges, server hit timing, stamina and knockback match C3.6. No root-motion, IK or hitbox change.

**Important:** Renderer instrumentation shows that the renderer applied rotation data; it does not prove the quality of the visible poses. The vanilla avatar has no knee/ankle joints and this is not mocap-grade cinematic animation. The next quality pass must be based on player F5 footage and ideally a hierarchical cosmetic rig.

## QA
Gradle/JUnit, 15 asset static QA, forward-roll geometry QA, client + server smoke test, Windows packaging. Preserve C3.6 rollback. Do not merge until user confirms actual rendered movement.
