# MNAE P0 — Issue #43 weapon identity, actual hand and render hierarchy

Base PR #41 (`feature/mnae-m1-1-transitions-blender-review`); math parity ported from QA PR #38. This stacked branch is **NOT a replacement for integrated PR #30** (different CharacterData schema). Do not merge to main, do not migrate existing user worlds without independent migration QA. Test version `0.0.5-fabric.8`.

## What user video actually proves

Issue #43 documents visual QA only by PRIVATE user video. Do not publish private footage or frames on GitHub. It demonstrates C3.8 sword first-stage when mainhand sword is selected versus offhand sword and selected EMPTY mainhand later; these are not equivalent A/B. It also shows axe triggering `mkm:sword/light_2`. Unsupported axes mapping is a confirmed code defect independent of video interpretation.

## Fixes

- Introduce `WeaponPresentationResolver` with actual mainhand AND offhand, family `EMPTY/SWORD/AXE/PICKAXE/BOW/SHIELD/OTHER`, server-owned R combo phase/step, physical primary arm from `AvatarRenderState.mainArm`, and explicit renderer `C3/MNAE/VANILLA` + reason. R combo is a **MAIN_HAND** attack; having an offhand sword does NOT make it an offhand sword strike. Do not invent an offhand action from inventory.
- On sword/empty mainhand: C3.8 legacy all three stages, MNAE only first-stage demo with F8 ON, C3 fallback for stages 2-3. Other weapons: intentionally use VANILLA neutral presentation, NOT sword clips. Offhand-only weapon + selected empty mainhand: VANILLA/no authored combo to prevent offhand blade accompanying bogus unarmed strikes. Offhand shield + empty mainhand: unarmed mainhand pose, shield arm protected from authored offhand animation.
- LocalCombatAnimation no longer uses deprecated `profileForEmptyMainHand` in runtime. Profile chosen from resolved family and action.
- Mixin one pose path per frame, no double application; unsupported weapons do not enter C2.1 sword-only fallback. Diagnostic HUD shows expected vs actual render path, true applied clip, families, action/hand/arm, combo step and fallback reason, refreshed from actual ModelPart setup. `POSE APPLIED` is **not** GPU framebuffer proof.
- MNAE native bridge now builds a six-part **runtime** skeleton using actual PlayerModel pivot offsets, then evaluates `Skeleton.world` to propagate body rotation into child head and shoulder **pivots and orientations**, and composes resulting presentation offsets onto vanilla look/walk. Left-handed retarget mirrors motion and swaps arm/leg targets. Offhand blade/shield arm receives no authored hand swing.
- `WeaponGripSocket` defines a pure model-space hand-end, handle and tip transform rooted at the correct physical arm. Unit tests include neutral main/off hand positions, torso rotation to grip/socket and segment clearance. This is a **mathematical preview**, not an API hook replacing Minecraft's ItemInHandLayer transform; actual weapon alignment, tip-through-head, item axes and armor collision are not yet validated. Do NOT claim the weapon-position bug is fully fixed in-game.
- Exact legacy C3 PCHIP sampling and positive **uniform** scale restriction from QA PR #38 ported without discarding M1.1 speed-preserving transition and 256 KiB clip bound.

## Scope exclusions / why

- No new animations or arbitrary pose-angle amplification; 2 native demo assets + 15 existing C3 clips remain.
- No damage, stamina, skill, timeline, camera, hitbox, dodge displacement, packets, inventory/game state or server CombatService changes.
- The six original vanilla cubes cannot express elbow and wrist articulation, IK foot locks or real parent-child ModelPart geometry. Runtime adapter emulates *visual* body parent motion on its sibling parts; root motion and world collision are never changed.
- The Blender proxy still lacks an actual held item. True grip calibration requires a skin/armor/ItemInHandLayer axis fixture in Minecraft plus two camera angles; visual test gate remains RED.
- **Roll analysis:** AvatarRollAnimationMixin applies full global rigid 360-degree spin with ideal cuboid ground-pivot compensation. It cannot hold a planted palm, knee or foot and may still float on terrain; without a captured frame-by-frame in-world contact measurement changing 8 dodges is premature. Evaluate grounded crouch/shoulder-support and **step dodge** side/back as alternative in future PR.
- Vanilla LMB #36 / Equipment D3 is separately owned and requires integration review; no new attacks from animation. The R mainhand contract stays as-is.

## Automated and manual gates

1. Java25 Gradle build and JUnit; verify PR38 uniform scale/shear rejection and 15 C3 assets sampled at 201 timestamps. Verify speed, transition and resource bounds remain green.
2. Deterministic seven-family routing: mainhand sword, offhand-only sword + empty selected mainhand, mainhand axe, pickaxe, bow, shield, offhand shield, left-leading hand, stages 1/2/3 and all dodges. Ensure nothing maps an axe or pickaxe to sword.
3. `WeaponGripSocketTest` validates nominal local-to-model model-pixel transforms and head proximity calculations for right/left grips. No claim of collision clearance in Minecraft.
4. Client F5: capture 60 FPS fixed camera SAME hotbar/same mainhand sword first R strike F8 OFF then ON; repeat empty both hands jab. Compare before/anticipation/impact/follow-through/settle. Show HUD. Repeat offhand-only sword and axe/pickaxe with each F8 value. Record two dodge directions, forward and side. Include first-person screenshot only to show no new override.
5. Repeat with left-handed main arm and offhand shield and armor; manually check actual rendered held-item grip vs head/torso. If sword still crosses face, **block visual merge**, require ItemInHandLayer calibration/explicit hand socket integration before any new clips.

## Reviewer / integrated-game caveat

Issue #43 requires integration on #30 schema v3 or Character v4 before giving the mod as an upgrade to a real save. This branch is stacked on historical MNAE #41 (CharacterData v2); CI success only establishes isolated test build, not schema-safe upgrade. Ask integration reviewer to cherry-pick scoped animation-only commits into schema-correct base with #36 read-only presentation event and Equipment D3 exact-hit attribution; do not blindly merge #40–#41 into #30.
