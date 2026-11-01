# Handoff for independent reviewer — C3.8

Repository eretenkodaniil16-rgb/mkm-rpg. Compare `feature/c3-8-pose-evidence-motion-qa` against `feature/c3-7-visible-combat-diagnostics`. The chain #9 #10 #11 #12 #14 #17 #20 #21 #25 is stacked; do NOT merge to main.

**Verify** Java 25 / Minecraft 26.3 / Fabric Loader 0.20.1 / Fabric API nested, CI artifact SHA integrity, script smoke, dedicated server and client startup. Check the `PlayerModelAnimationMixin` before/after local-part angle calculation and `AnimationRenderProbe.status(expectedClip)` so stale or neutral samples are not falsely represented as actual rendering. Ensure no HUD crashes with missing client level or missing animation profiles. The before/after sampling does NOT prove actual framebuffer output; do not approve visually without real F5 capture.

**Collision and biomechanics** Run `tools/animation/qa/biomechanics_audit.py` and inspect all 15 clips. Confirm combo impact markers align C2 authoritative tick rules and all eight dodge durations remain 10 ticks; measure actual foot clipping, view, armor and weapon in a playable client. Plot is SIDE-PLANE proxy, not geometry/collision test. Validate no negative server effects or modified per-hit properties. Require manual left-handed and locomotion footage.

**D3 conflict gate** Equipment PR #23 branch `feature/equipment-hit-provenance-d3` changes `CombatEvents` via `PendingHitProvenance<LivingEntity, DamageSource>` with exact source identity and tick-bound consumption, shield/mitigation, PvP. Animation C3.6 adds owner-only HitConfirm after *positive, unblocked* damage during LIGHT_COMBO/ACTIVE. Integration MUST preserve both: source-bound heavy attribution and mitigation, one-hit consumption, guard block callbacks, and clientbound VFX only after actual damage. Do not override D3 with C3.6's WeakHashMap<LivingEntity, Boolean>. Add mixed integration tests for blocked heavy, stale markers, unrelated damage source, normal combo, critical/stagger and PvP. Animation clips must never cause damage.

**Do not claim** proof of actual rendered combat, real IK, grounded feet, cinematic quality or multiplayer replication solely from CI.

Reviewer acceptance: CI PASS + measured renderer evidence in real client + F5 capture + no regression in Equipment D3 integration.
