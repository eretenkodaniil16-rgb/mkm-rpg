# MKM R07 — one articulated Blender-to-Fabric vertical slice

Status: **experimental / technical integration only**. Do not call this cinematic-accepted or merge into historical NeoForge main. Based on MNAE P0 PR #46, itself **CharacterData v2** and not safely upgradable over integrated PR #30 (schema v3/v4 migration unresolved). Matches user Issue #43 and PR #49 workflow G0→S1→...→G8 evidence gates.

## G0 — frozen immutable source

- Original accepted static authoring asset: annotated tag `mkm-guards-r06-approved`, peeled commit `5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8`.
- Blender 4.0.2, 17 bones, three poses on frames 1/20/40, one bone-bound sword, nine orthographic rendered PNGs and manifest. Archived GitHub Actions authoring run 41962585609 PASS.
- R07 reproducibly loads the exact tagged binary via `git show`. It **never edits** `assets/animation/approved/mkm_sword_guards_r06/`.
- **Selected Mid Guard** frame20 (`MKM_GUARD_MID_02`). Compared to High Outer (face-clearance proxy 0.079 Blender units), Mid Guard (0.358) has a larger initial head clearance and lower weapon position, without Low Mobile's very deep 58–60° knees. All three R06 source poses retain prior approval. Blender visual approval is NOT Minecraft acceptance.

## S2/S3 motion design: one 14-tick sword cut

- 43 authored 60fps frames, first at Mid Guard, intermediate hand IK targets at frames 5/9/12/16/22/29/36, end at Mid Guard.
- Ordered phases: guard, anticipation, windup, impact, follow-through, recovery, guard-return. Contact frame16 uses (16-1)/42 = 5/14, exactly the original server-authoritative visual marker.
- A single right-hand sword remains rigidly hand-bound. Offhand and armor have no safe mapped articulated presentation yet: intentional fallback to old renderer.
- No additional server swing/damage events, no hitbox/root motion/stamina edits.

## Export contract v3 (original, stable field names)

`tools/animation/r07/export_r07_blender.py` invoked by `.github/workflows/mkm-r07-articulated-vertical.yml`. The CI checks out immutable R06 tag, then Blender evaluates the full constraint graph/IK on each of 43 frames.

`r07_mid_guard_cut_v3.json`:
- `format:3`, `id:mkm:r07/mid_guard_sword_cut`, `source.approved_tag` + fixed source SHA and source .blend SHA256, `duration_ticks:14`, `fps:60`.
- Coordinate mapping: Blender right X, forward -Y, up Z to **MC model pixels** right X, down Y, forward -Z, with scale 24/1.8; quaternion order xyzw, all single-unit scales, no shear.
- 17 `bones` each with name, parent, rest pivot/rest quaternion. Validated tree and exactly 17.
- Separate `mesh_bindings`: 15+ rigid cuboids each bound to actual bone (including upper-arm, forearm, hand, thigh, shin, foot), with relative box geometry and skin-atlas UV hint.
- `frames` (43) every bone stores evaluated world `pivot`, rest-to-pose world `quaternion`, local translation/rotation, and scale. Retaining both spaces lets QA expose where transform precision was lost.
- `weapon_socket`: actual bound R06 right hand; model-space hand-bone-local sword handle center and blade tip, not arbitrary world-space weapon rotation. Item rendering is separately corrected only in experimental model path.
- `markers`: visual events only, especially `impact` at frame16. Never call server damage from marker.

Loader `ArticulatedClipV3` enforces byte size <=1MiB, joint count 17, exactly 43 frames, 14 ticks, valid parents and markers, quaternion normalization and finite data. Fail-closed if missing/incompatible export.

## S5 — experimental Minecraft 26.3 renderer

- F8 ON selects MNAE baseline vs C3; **F9** enables 17-bone R07 only for local player's 14-tick FIRST R sword attack with empty offhand, third-person camera, no armor. F8 OFF remains C3. F8 ON/F9 OFF = MNAE six-part; F8 ON/F9 ON = 17-bone for eligible swing; all other actions use old C3/MNAE fallback. HUD labels modes.
- `ArticulatedAvatarLayer` bakes a Minecraft 64×64 skin-atlas model once and submits **17 independently moving bone segments**, each with its own Blender world quaternion and world pivot. This does not bake/eliminate elbows, knees, feet.
- `PlayerModelAnimationMixin` temporarily suppresses vanilla six-part cubes for **only the eligible render frame**, resets skipDraw on every setup, and exclusively hands off articulated rendering. Avoids double bodies.
- The vanilla mainhand `translateToHand` path is redirected to the exact R07 hand socket only for eligible MAIN_HAND sword; otherwise vanilla shield, offhand, first-person, and regular item layer behavior are preserved.
- Gate requires the articulated renderer layer to have been registered and baked, otherwise cannot hide normal player (fails safe). Geometry and client source are original, no PAL dependency.
- Current exported skin UVs are **approximate mapping hints**. No verified Steve/Alex skin/armor texture fidelity; model shape is intentionally optimized for first unarmored right-hand test.
- The actual shader pass/held-item visual placement must still be inspected with a real F5 60fps player recording. Client bootstrap CI != playing the 14 tick attack.

## S6 QA evidence and open defects

1. Immutable source tag, Blender scene and nine original pictures: PASS by GitHub.
2. Exporter outputs new editable Blender derived .blend, 43-frame 17-bone v3 JSON, nine Blender images: pending per run.
3. Blender head/sword segment proxy clearance, toe XY drift and exact visual impact: measured by exporter; not proof of game-world armor clearance, real foot contact, item-layer transform or camera occlusion.
4. Java 25 Gradle/JUnit compilation + strict v3 parser + 17-bone distinct segment assertions, Fabric API and startup smoke: pending per run.
5. Real third-person F5, fixed camera A/B of C3 vs MNAE six vs R07-17, sword/main and left hand, real skins (Steve/Alex), weapon/head/armor intersections, remote players: **NOT DONE / visual gate RED**.
6. Armor and shield: explicit safe fallback, not claimed supported. Left physical main arm: mirrored mathematical candidate, not visually QA approved. First-person: intentionally unchanged.

## S7/G8 future architecture (do not implement extra actors now)

Define a reusable `RigDefinition` with joints/rest-binds, bone-local sockets, renderer skin/mesh bind tables, attachment/armor profiles and locomotion tags. `PoseGraph` + action priorities drive joint poses without item-specific hardcoding. Avatar-specific `RigProfile` maps MC skins/first-person armor/weapon; humanoids may share biped chain definitions; quadrupeds need spine/neck/tail, four leg IK contacts; flying actors need shoulder wing chains, body bank and air support; bosses may require multi-part segmented meshes, joint-dependent LOD and bound renderer budget. All remain client cosmetic and never own hitboxes or damage. Only expand after F5 proof for this one sword strike.

## Go/no-go criteria

GO only after 60fps actual Minecraft F5 demonstrates visible independently articulated elbow/knee/feet and correct hand grip with no head/armor intersections, test Steve/Alex/left/right, and no duplicated render. Until then this is a **first working codepath experiment**, not accepted game cinematic animation or safe world upgrade. Main remains untouched.
