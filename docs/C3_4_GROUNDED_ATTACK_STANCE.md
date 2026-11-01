# C3.4 — Grounded full-body attack stance (visual only)

C3.3 already introduces shape-preserving continuous angular velocity across monotone keyframe intervals. C3.4 adds **subtle authored leg counter-motion to six attacks**: three one-handed sword hits and three unarmed hits, each aligned to the pre-existing server impact time. No timing or hit marker changes.

## Implementation
- Six attack JSON assets are now `full_body` and include LEFT_LEG / RIGHT_LEG local rotations from neutral to anticipation, impact, settle and neutral. Legs counterbalance the existing body and arm keyframes; no run animation is replaced.
- `CombatStanceMath.attackLegWeight` fades the attack-leg additive offset from full at up to 0.025 blocks/tick to zero at 0.14 blocks/tick. While airborne, leg attack offset is zero. Dodge clips are unaffected and keep the full authored leg rotation.
- The rendering mixin mirrors the attack stance legs when the player is left-handed while leaving the original directional dodge pose mapping unchanged. This is client visual code, with no movement authority.
- New JUnit tests cover six full-body clips, leg-rotation bounds, impact alignment and ground/movement gating. Existing animation-library validation and Fabric smoke tests still apply.

## What it cannot yet do
Vanilla PlayerModel has one blocky segment per leg and no dedicated hip, knee, ankle, wrist or foot IK chains. This adds an illusion of stance and weight transfer but **does not guarantee planted feet** and cannot prevent every visual foot slide or armor conflict. It does not use root motion. Real planted-feet IK needs an extended hierarchical rig; a separate, opt-in cosmetic model bridge should be considered only after the existing mixin works across armor, skins and mod packs.

## Acceptance
1. At 60fps F5, test all 3 punches and 3 sword attacks stationary; look for coordinated left/right stance changes.
2. Repeat each while walking/running and jumping; vanilla gait should dominate and no authored leg swing should fight locomotion.
3. Repeat as a left-handed avatar; feet and torso should mirror the strike stance.
4. Test all eight dodges against C3.2 video; no visual or server movement regression.
5. Validate server hit frames, stamina, action timing, player hitbox, first-person and mixed armor. No client-world physics is modified.

Stay draft until player-captured footage confirms the above.
