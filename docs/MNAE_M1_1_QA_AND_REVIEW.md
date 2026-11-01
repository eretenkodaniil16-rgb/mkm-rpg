# MNAE M1.1 — playback correctness, asset hardening and real Blender preview

**Base:** MNAE M1 draft PR #40. **Branch:** `feature/mnae-m1-1-transitions-blender-review`. **Minecraft:** 26.3/Fabric/Java 25. **Version:** `0.0.5-fabric.7`. Do not merge to `main`. This is technical polish, not in-world cinematic acceptance.

## Defects identified in M1

1. `AnimationPlayer.speed(multiplier)` rewrote the multiplier against the original clip start time; an in-progress speed change jumped the animation phase. Now it is only allowed when stopped; `setSpeedAt(multiplier, gameTickWithPartial)` integrates elapsed clip ticks before switching rate.
2. `AnimationPlayer.play()` changed clips using a map override which didn't include keys only present in the outgoing pose. These would stay at their old rotation and then snap off when the transition ended. `PoseMixer.crossfade` blends the union of outgoing and incoming bones, falling back to identity for missing channels. `stop(now,fadeTicks)` applies identical decay to neutral and is deterministic. Existing immediate `stop()` remains explicit.
3. The original Blender script keyed six bones but supplied no mesh geometry. A camera could render an empty scene. It now builds visible constrained cubic mesh pieces, a reference floor, a camera and lighting in the saved .blend; proper action slots are selected for Blender 4.4+, while 4.0 retains legacy FCurve support. CI generates ten diagnostic frames (five sword/five punch), **not Minecraft frames**.
4. The source parser could accept arbitrarily large JSON before validating individual tracks. It now refuses payloads above 256 KiB and rejects >48 tracks, >256 keys per track or >64 presentation markers **before creating clip objects**. JUnit tests cover oversized and malformed input.
5. M1 previously classified *every non-empty item* as a one-handed sword in the native adapter. Now only empty hands and `ItemTags.SWORDS` activate the two native demos; other items fall back to existing C3.8 rendering. This affects **presentation only**. Future Equipment Core profiles must replace temporary tag checks.

## Unchanged gameplay contracts

- R special-combo phase ownership, 14 tick first strike and 5/14 authoritative impact marker remain server-owned.
- No change to `CombatService`, `ActionTimelineRules`, `DamageSource`, `DodgeMotion`, stamina, mana, damage, knockback, or collision.
- F8 A/B defaults C3.8. Renderer applies one path per avatar frame, with fallback if v2 data is unavailable.
- No PAL or external animation runtime. No first-person override, normal LMB duplicate damage, or arbitrary scripting.
- Unarmed MNAE = straight punch; tagged-sword MNAE = single cut. Other combo stages/dodges remain legacy. Remote observer rendering remains unimplemented.

## Automated acceptance

Run `./gradlew build` with Java 25; use CI to assert: math and clip tests, interpolation continuity, phase-preserving speed changes, outgoing-only fade, oversized file rejection, unchanged C3 library + impact timings, dedicated-server and virtual-display client boot. Blender CI on PR push installs distro Blender and runs:
`blender -b --factory-startup --python tools/animation/blender/mnae_two_strikes.py -- --verify --blender --render`
It must produce `mnae_m1_source.blend` and ten PNG frames (artifact `mnae-m1-1-blender-source-and-frames`). These are constrained **six-piece rig diagnostic renders**. They cannot establish gameplay quality, foot IK, skin or armor contact.

## Required manual F5 film capture

Record C3.8 vs MNAE (F8) on identical flat-world terrain at 60 FPS with same camera, 1 sword strike and 1 unarmed strike, followed by walk/run/jump and left-handed/off-hand shield. Test unsuited held items (pickaxe, axe, bow, shield), confirm native sword demo does not play on them, and verify no doubled arm transforms. The HUD pose delta only proves model setup, not onscreen pixels.

## Known constraints

Vanilla body pieces still have no elbow, wrist, knee or ankle articulation; Blender source rig is necessarily reduced to six drawable segments. Current MNAE demo only operates for local third-person R combo step 1; normal LMB presentation and first-person are gated by gameplay QA. Static Workbench frame renders don't validate Minecraft graphics, attachment points or collision.

## Reviewer handoff

Review source in `feature/mnae-m1-1-transitions-blender-review` against PR #40. Confirm speed invariance, idempotent clamp, masking/mix, A/B exclusivity, future Equipment D3 crossmerge with confirmed-hit VFX, and Java25/26.3 client startup. Do not merge PRs until player video validates at least these two gestures.
