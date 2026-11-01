# MKM RPG — integrated Magic M3 QA preview (NOT a release)

**Branch:** `integration/abilities-m3-mnae-c3-7-qa-preview`  
**Base:** integrated Fabric 26.3 C3.7 PR #30  
**Reviewed source:** isolated Magic M2 PR #28 + M3 PR #37 (stacked on #19).

## Why this preview exists

The isolated Magic M3 branch in #37 inherits **CharacterData schema v2**
from its original C2 baseline (mod version 0.0.4-fabric.0), while the
user-tested integrated PR #30 has **CharacterData schema v3**, allocated
points and version 0.0.5-fabric.4. Installing isolated #37 over an existing
#30 world risks dropping v3-only player fields on subsequent saves.
**Do not use isolated #37 artifacts to replace the existing integrated world.**

This integration preview is built directly on #30 and **retains all #30
CharacterData/Progression M1, Equipment D1-D3, Combat C3.7, C3 hit feedback,
World E1, translations, item registrations and existing data schema**.
Only the Magic M2/M3 changes are layered over that baseline.

This is still NOT safe to load without a world backup if the world has
since been touched by **Character v4 PR #29/#35, World E2 PR #26/#39,**
or another newer schema. Use a copied/disposable test world.

## Integrated runtime components

- `AbilityService`: original server validation and persistent `ManaLedger`
  retained; now uses shared `RenewalParameters`, sends `APPLIED` only
  after real positive healing.
- `AbilityModifierAccess.snapshot`: delegates to the real, existing
  `EquipmentSnapshots.snapshot(player, CharacterService.get(player))`,
  exactly as C3.7 Combat Core does. There is no second gear or primary
  stat calculator, and no duplicate application of deltas.
- `AbilityNetworking`: original narrow C2S cast request + strictly bounded
  clientbound-only `mkm:ability_visual_event` **v2** (v1 is rejected).
- `MkmClient`: **preserves** Character allocation receipts and C3.6
  `HitConfirm` VFX, while adding MNAE Renewal confirmation listener and
  per-client-session reset.
- `MkmCombatHud`: **preserves** equipped MAX_STAMINA, C3.7 version
  diagnostic and actual `AnimationRenderProbe` status. Uses the shared
  resource-left-column + pixel-clamped labels. On GUI height below 250,
  optional version diagnostics are hidden to avoid covering the primary
  mana and stamina bars; the animation status is still clipped.
- `MkmAbilityHud`: reads the exact equipped snapshot via the new
  adapter; no change to the existing character or combat allocations.
- `ArcaneBoltTargetProbe`: remains a READ-ONLY server preflight with
  mandatory Combat permission predicate. **Arcane Bolt is disabled**
  pending actual approved Combat DamageSource/mitigation and spell
  action-timeline contracts.
- `AbilityVisualInbox`: MNAE listener seam for first-party engine,
  server-confirmed `APPLIED` only. No extra animation engine.

## Build and acceptance

JUnit includes isolated M1/M2/M3 rules, packet fuzz/bounds, cooldown
rollback/serialization, Unicode/pixel HUD bounds, Character M1 allocated
stats, Equipment D1-D3 physical mitigation and C3 renderer tests, plus
a combined pure equipment/Magic parameter test.

CI includes Gradle Java 25, embedded Fabric API, dedicated-server and
Xvfb client boot, Windows packaging. CI passing does NOT prove actual
in-game rendering or two-player authority.

Manual checks on copied integrated v3 world:
1. Load with K Character Sheet, allocation credits, equipment modifiers,
   G dodge, R combos, hostile kill XP and Ford Warden quest; all still work.
2. Damage self; press H once; confirm actual HP gain, mana cost/cooldown,
   one cosmetic burst; spam H / full HP / combat busy: no false APPLIED.
3. At GUI 200x200, 320x240 and regular scales verify left HUD,
   readable dynamic key labels, version/debug statuses and F5 avatar.
4. Relog, respawn, change dimensions and test two clients: no stale VFX
   or leaked mana, no duplicate damage or owner-to-observer event.
5. Verify versioned saves with a backup, and run original 60fps C3.7
   visual comparison before claiming MNAE clips work.

## Limitations

- This is **not** an integration of MNAE native PR #40, independent QA
  fixes #38, Combat LMB presentation PR #36, Character v4 #35, or
  World E2.1 #39. MNAE still must author the actual Renewal hand gesture.
- Current PR #30 engine is C3.7, not C3.8 MNAE; no first-person
  hand gesture from native MNAE is claimed.
- Do not merge into main or release the preview installer.
- Resolve branch ancestry and all save migrations in a future
  Control/QA-approved integration before public use.

## November 1 M3.1 stabilisation / hard QA boundary

- Existing PR #42 commit `d72f3690` passed all CI steps in [run 41268543666](https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41268543666): Java25 build/JUnit, Fabric API bundling, dedicated server startup, Xvfb client startup and Windows artifact packaging. **That success applies to the original commit only.** Latest v2 changes require a new green build before integrator use.
- v2 confirmation includes the server dimension identifier. The client tests exact local world/actor before advancing its replay sequence; old-dimension events can no longer fire after a portal travel. V1 is intentionally incompatible.
- The MNAE listener now optionally includes an `onStop` callback. It is invoked when the 14-tick cosmetic lease expires, an event is superseded or client player/world changes. There is no parallel gameplay cast timeline. MNAE owns the actual visual fade.
- **Character v4 is not in this branch:** the v4 character record has a persistent `claimedRewardIds` ledger absent from v3. Loading a v4 save with this v3-based preview could silently discard reward receipts on save. **Do not upgrade/downgrade v4 worlds using this JAR.** Testing must use a disposable backed-up v3 world and retain independent migration review before any release.
- Equipment snapshot uses `EquipmentSnapshots.snapshot(player, CharacterService.get(player))` and is shared with integrated Combat. A later v4 merge must preserve that single source rather than recompute magical item deltas separately.
