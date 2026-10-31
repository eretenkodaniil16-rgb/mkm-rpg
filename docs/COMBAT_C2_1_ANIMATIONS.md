# MKM Combat C2.1 — native Minecraft 26.3 player-model animation

Built on the user-tested C2.0 (0.0.4-fabric.0) action-state machine, and the verified C1.3 eight-way dodge. This release does not modify attack damage rules, client permissions, network authority, or the physical player hitbox.

## Implementation

- The **server's** existing `ActionTimeline` now carries one bounded byte of dodge direction. This is only *intent metadata for rendering*, not a velocity provided by the client. Fabric's owner-only timeline attachment codec includes it.
- `CombatAnimationMath` computes clamped animation progress from the synced phase deadline and game-time. With `smoothstep`, transitions accelerate and decelerate smoothly. A separate pure-Java JUnit suite checks every direction, duration, 360° endpoints and server hit-timing independence.
- `PlayerModelAnimationMixin` applies additive torso, arm, leg and head rotations after vanilla `PlayerModel.setupAnim(AvatarRenderState)`. Three distinct stage poses follow the existing `WINDUP → ACTIVE → RECOVERY` machine: diagonal cut, crossing horizontal cut, and stronger overhead finish.
- For `DODGING`, the body tucks: arms close to the head, torso leans, legs fold. `AvatarRollAnimationMixin` applies a smooth full 360-degree third-person whole-body rotation about a waist-level pivot. Axis is computed from the eight server-synced dodge directions, including diagonals.
- Model transformations are **visual only**. Server-side action states, collision, impulses, damage, timers, stamina costs, and cooldown logic remain authoritative and unchanged.
- The player's own avatar uses the owner-synchronized timeline. Other players will still see vanilla movement because broadcasting timelines to tracking entities has not yet been implemented; do not claim multiplayer-visible third-person animations.
- No additional player animation library or user-installed dependency; Minecraft 26.3's model and renderer classes are used directly via client-only Mixins.

## Manual QA (essential)

Install `mkm-rpg-0.0.4-fabric.1.jar` into a Fabric Loader 0.20.1 / Minecraft 26.3 client after removing any older MKM JAR; the official Fabric API remains nested. Use a backup test world.

1. Press **F5** for third-person view to see the body. On level ground press **G** with movement keys W, A, S, D and diagonals (hold direction first). Verify a visible full-body roll that corresponds to directional movement. The actual hitbox must not teleport through terrain.
2. Verify stamina is charged exactly once and the position change is no greater than C2.0. Roll animation should finish in eight game ticks (approximately 0.4s at a stable 20 TPS).
3. With a zombie within reach, press **R**. Check an animated first diagonal strike, then R again after recovery for an animated crossing strike, then third overhead strike. There must be no damage before the existing ACTIVE transition.
4. Ensure **K** opens the character menu; the HUD and action phases remain visible and intact.
5. Check the first-person view: vanilla hand animation remains; the C2.1 full-body model effect is visible **only in third-person**. Dedicated first-person arm animations are planned separately.
6. Test different skins (Steve/Alex), left-handed main arm, sprinting, low stamina, obstacles, death/respawn, and high ping. Sync only affects the owning client.
7. Run the same JAR on a dedicated server and verify it does not load client-only classes.

## Safety and limits

This is **C2.1 visual MVP**, not an authored Blender animation rig. The roll is a scripted 360° renderer transform and can visually clip against ground/armor, because collisions do not follow model pose. There are no iframes. Full rigged/retargeted animations, other-player state broadcast, first-person arms, animation blending with modded models and collision-safe root motion remain future work.

CI must pass Java 25 compile, JUnit rules, embedded Fabric API validation, dedicated-server boot and a virtual-display client boot test. A client boot does not prove that animation looks correct in third-person; **real in-game F5 visual QA remains required**.
