# MKM Sword Slash R03 — Compact Right-Shoulder Guard (proposed start pose)

Status: **pose specification only**; not approved animation, not a tested Blender model and not an MNAE/Minecraft runtime asset. Independent branch stacked on R02 PR #50. Fabric 26.3 / Java 25 experiment. Do not change current game save, damage, weapon presentation, or established vanilla idle.

## Why change the starting pose?

R02 tried to carry a downward-held sword up to the right shoulder and reverse its direction in a fraction of a 14-tick clip. Its free world-space sword quaternion and independent wrist IK generated a visually strange cut. A **compact ready guard already near the right shoulder** reduces the preparatory arc, avoids an abrupt inversion of the sword and provides a short, repeatable path to contact.

Reference: [J. M. Waite, *Lessons in Sabre*, plate II / engaging guard](https://www.gutenberg.org/cache/epub/67257/pg67257-images.html) positions the sword arm in front of the right shoulder with a bent elbow. [Sydney Sabre: tierce/guard 3](https://sydneysabre.com/2022/10/10/week-0-intro-to-sabre/) recommends an upright blade at the sword-arm side with the elbow drawn in. [Joachim Meyer high guard description](https://www.wiktenauer.com/wiki/Joachim_Meyer/Rebecca_L._R._Garber_Rapier_Ch3_2023) distinguishes cuts from high side guards. These sources support the *guard principle*, not the exact pose coordinates for our voxel character.

## Selected R03 starting stance

Coordinate conventions are consistent with R01 Blender source: player faces -Y; right is +X; Z is up; units are Blender scene units.

- **Right hand / grip**: just outside the right side of upper chest near the shoulder, target XYZ **[0.54, -0.23, 1.17]**. These are proposed IK authoring targets, not accepted wrist geometry.
- **Weapon direction**: tip upwards, forwards and to the right, normalized approximate **[+0.55, -0.18, +0.81]**; blade entirely to the right of the head and outward from the shoulder. Tip preview with 0.62-unit blade: near XYZ **[0.88, -0.34, 1.67]**.
- **Elbow / wrist**: elbow lower and to the right of the grip, bent approximately 65–100 degrees; avoid an unnaturally flexed wrist. The sword's actual hilt orientation must be **rigidly constrained to the hand bone and its palm coordinate frame**, not animated by an independent world quaternion as in R02.
- **Torso**: upright with modest right-side loading; pelvis yaw -7°, upper spine additional yaw -9°. Keep shoulders visible and the head looking at the opponent. Do not collapse the chest or tilt the pelvis far forward.
- **Feet**: right planted near [0.13, -0.045, 0.07], left slightly ahead near [-0.13, -0.105, 0.07], knees unlocked, no mandatory gameplay displacement.
- **Left hand**: down/relaxed near left waist and ready for shield/offhand variations in future. R03 only covers a right-mainhand one-handed sword.

This is **not** a high overhead guard that places the sword behind the head and is **not** the vanilla always-on idle. It is a temporary *ready key pose* for the sword action.

## Provisional 14-tick R03 action (visual only)

| Approx ticks | Stage | What must be visible |
| ---: | --- | --- |
| 0–1.5 | READY / minimal load | Sword already high outside right shoulder, no snap |
| 1.5–3 | COIL | Small hip-to-shoulder pre-rotation; left foot prepares |
| 3–5 | STRIKE | Controlled descending diagonal right-high → left-low; lead with blade as one rigid gripped object |
| **5** | **CONTACT REFERENCE** | Blade in front of target line; elbow and wrist aligned; game damage remains server-owned |
| 5–7.5 | FOLLOW | Controlled deceleration, no back-of-head crossing |
| 7.5–14 | RECOVER | Blade returns around right side, not through face; settle at same compact guard |

These phases deliberately simplify R02's low idle → high windup → slash → low idle. **Do not** start coding interpolation just because this table exists: first demonstrate the single pose and a 3D source storyboard in the real rig.

## Non-negotiable technical and visual gates

1. **One source of weapon orientation**: the sword grip must follow the hand/palm bone through one fixed bind transform; NO separate world-space sword orientation channel and no Euler-only wrist guess. Validate relative weapon-to-hand transform stays constant.
2. **Anatomical feasibility**: actual IK must reach the guard without shoulder translation, joint stretching or excessive wrist flexion. If a real pose requires wrist bending beyond sensible limits, move the shoulder/elbow or revise guard.
3. **Head silhouette**: preview from fixed front, side and 3/4 cameras; no weapon clipping or occluding the face in the approved view. Distance-to-head-center alone is insufficient.
4. **Foot support**: no planted foot sliding and no unintentional origin movement. Treat root motion as visual only; do not alter gameplay collision.
5. **Entry and exit transitions**: test vanilla idle → combat ready → sword attack → idle. If MNAE lacks a continuous combat-ready stance, blend into guard instead of instantly snapping on frame 1. A right-shoulder source pose is **not** a drop-in replacement for default player idle.
6. **User visual acceptance of a three-angle, static Blender render** before authoring the entire R03 animation. Real in-game F5 still requires MNAE Issue #43 resolution.

## Immediate deliverable / next narrow work item

Produce **one static guard .blend** with the same voxel rig that will be animated, and **three fixed-angle images** (front, right side, 3/4). Measure actual wrist/weapon bind and head/armor clearance. If it visually reads as a normal compact sword guard, then create the seven key poses on that exact rig. No new full sword animation or gameplay PR until this static-pose gate is passed.

## Current evaluation

Proposal only. Position and direction above are deliberate design approximations, **not measurements from an already rendered/accepted Blender frame**. The geometric tip estimate is not proof of skin/armor clearance.