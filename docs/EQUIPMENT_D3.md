# MKM D3 — heavy-hit provenance and guard resource hardening

**Draft stacked on D2.** Fabric 26.3, Java 25. Does not alter Minecraft hit detection, ActionTimelineRules, animation clips or packet protocols.

## Defect fixed
D2's pending Shift-heavy flag was indexed only by the target. If `hurtServer` marked a heavy hit but no corresponding `AFTER_DAMAGE` happened, a later unrelated source hitting the same target could inherit the marker and incorrectly increase poise. D3 binds a pending marker to the **exact damage source object**, target and server game tick. Each new direct player attack clears old per-target state. A callback consumes markers only once, and only when source identity and tick match (one tick tolerance). Canceled, unrelated or delayed callbacks cannot inherit it.

## Additional guard invariants
Pure GuardRules rejects nonfinite and negative maximum stamina, regeneration and invalid delay. ShieldGuardService silently skips malformed nonfinite/negative damage supplied by other integrations rather than causing a server exception. Legitimate blocked zero damage keeps the baseline stamina cost.

## Test evidence
New pure JUnit cases cover once-only matching, cancelled events, unrelated attackers, stale and backwards-time callbacks, independent targets, invalid guard inputs and bounded cost. GitHub CI must build and boot a dedicated server. Manual shield, PvP and client verification remains outstanding.

## Cross-workstream coordination
- Animation: C3.6 also modifies CombatEvents to send confirmed hit feedback. During integration preserve both the confirmed-hit packet and exact heavy-hit provenance; neither animation markers nor VFX decide combat results.
- Progression: no character data, modifier registry or allocation change.
- Magic: no effect on spell damage or casting.
- World: no quest or world save changes.
- Equipment: D2 shield stamina mechanic retained unchanged for valid inputs.

This is a correctness gate, not a content expansion. Keep draft pending client gameplay acceptance.
