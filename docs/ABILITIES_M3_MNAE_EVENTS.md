# MKM RPG — Magic ↔ MNAE event contract (M3 draft, 2026-11-01)

**Base:** Abilities M2 draft PR #28, stacked on M1 PR #19, Minecraft 26.3 /
Fabric / Java 25. MNAE ownership: [architecture issue #33](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/33),
[design PR #34](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/34).
**This PR does not implement or replace MNAE.** It adds the Magic-owned server
success event and a small client-facing entry point for future renderer binding.

## Gameplay authority and lifecycle

| Phase | Who can authorize it | Meaning and usage |
| --- | --- | --- |
| `STARTED` | Combat Core, when it reserves the existing ActionTimeline | Cast accepted for windup; not an effect, not an HP/damage change |
| `APPLIED` | Ability/Core after actual server effect, or Combat-owned damage executor after successful impact | Effect applied; may trigger successful effect-only VFX |
| `INTERRUPTED` | Combat Core authoritative interrupt/cancel | Cast cancelled; no assumption of successful damage or healing |
| `FINISHED` | Combat Core, after recovery/action release | Action finished; may stop looping cosmetic overlays |

These are **semantic states of gameplay**, not new animation clip timelines.
All phases are defined as wire-level enum values, but **only `APPLIED` is emitted
by the current instant `mkm:renewal`**: after the server validates
Combat timeline idle, heavy recovery, health deficit, mana, cooldown and stats,
debits mana and actually increases health using `player.heal(...)`.
The server **does not invent a 14-tick gameplay cast or delay**: Renewal's
existing `castTicks=0` and Combat timeline remain unchanged.

An applied effect does not require a preceding `STARTED` signal for an
instant spell. An `APPLIED` visual gesture may run **14 cosmetic ticks** while
the server has already healed the player. No `FINISHED` message is sent
for a nonexistent gameplay cast phase. Later noninstant casts MUST use the
Combat-owned begin/release/interrupt/recovery contract, not a second timer.

If the validation rejects casting, the server sends **no confirmation**.
Full health, invalid actor, spectator, cooldown, low mana, action busy
or an unauthorized/unknown ID never produce the Renewal `APPLIED` visual.
No animation marker or receipt acknowledgement may mutate the player.

## On-wire S2C payload

Packet ID `mkm:ability_visual_event`, version **2** (v1 packets rejected; client/server must use the same mod build). Registered as
`PayloadTypeRegistry.clientboundPlay()` only. Owner-targeted with
`ServerPlayNetworking.send(player,...)`.

| Field | Format | Bound/meaning |
| --- | --- | --- |
| version | unsigned byte | exact `1` |
| abilityId | length-prefixed ASCII | <=64 bytes, validated stable `mkm:*` |
| visualActionId | length-prefixed ASCII | <=64 bytes, validated `mkm:*` |
| actorEntityId | signed int32 | strictly positive |
| sequence | signed int32 | strictly positive, server-managed per player session |
| hand | unsigned byte | `MAIN_HAND=0`, `OFF_HAND=1` |
| phase | unsigned byte | `STARTED=0`, `APPLIED=1`, `INTERRUPTED=2`, `FINISHED=3` |
| serverTick | signed int64 | non-negative server game time for diagnostics/sync |
| visualDurationTicks | signed int32 | 0–100 inclusive, **cosmetic only** |
| dimensionId | length-prefixed ASCII | 1–128 bytes, validated namespace/path; authoritative level key |

Maximum encoded length is **282 bytes**, including all prefix bytes.
Packet deliberately does NOT contain health amount, damage, target ID,
mana cost, cooldown, player stats or entity coordinates. These remain
server-authoritative and are never echoed from a client cast request.

Current successful Renewal payload:
- `abilityId = mkm:renewal`
- `visualActionId = mkm:renewal_cast`
- `hand = MAIN_HAND` (MNAE maps to physical left/right using avatar settings)
- `phase = APPLIED`
- `visualDurationTicks = 14`, about 0.7 s at 20 TPS
- `actorEntityId`, `sequence`, `serverTick`, `dimensionId` supplied only by the server.

The decoder also rejects **trailing bytes and any frame over 282 bytes**
instead of tolerating ambiguous packet extensions. New fields require an
explicit protocol version rather than appending unversioned data.

The client validates the exact Renewal action ID/phase/hand/duration, local
actor ID, exact live client dimension and monotonically increasing sequence, then presents one visual event.
It resets replay state on join/disconnect **and when the client replaces
its local player or level (respawn/dimension travel)**. Sending a duplicate or reordered
S2C event cannot spawn another owner-local effect. This does not constitute
a crypto replay defence against a malicious or compromised server; the
server remains the trusted authority. Owner-only sync is sufficient for
the current local prototype. Observer broadcasting is MNAE-5, **not**
implicitly enabled by this change.

**Client integration seam:**
`AbilityVisualInbox.installMnaeListener(Consumer<AbilityVisualEvent>, Runnable onStop)`
(new overload; one-argument legacy registration remains supported). MNAE
should register the confirmation consumer **and** the cosmetic cancellation
callback. `onStop` runs when a newer visual supersedes Renewal, the
local avatar or dimension changes, the client disconnects/rejoins, or
the local 14-tick cosmetic lease expires. It never cancels or refunds
server-authoritative healing/mana. The 14 ticks are counted by **client
game ticks after receipt**, avoiding presentation lasting forever with network
latency. World-time rollback also expires the cosmetic lease without affecting
server cooldown. No standalone animation player is implemented in Magic.
It receives only owner-verified server-confirmed events on the client thread.
`latestConfirmed()` is an optional diagnostic getter. MNAE owns clip
selection, pose blending, left-handed transforms, masking and first-person
rendering. The `mkm:renewal_cast` field is a **semantic action ID**, not
a guaranteed asset path. The current native MNAE M1 (PR #40) uses authored
asset IDs such as `mkm:mnae/sword_cut`; its future action resolver should map
`mkm:renewal_cast` to e.g. `mkm:mnae/renewal_cast` (authoring decision) rather
than embedding clip paths in server packet semantics. This is an MNAE-3
implementation obligation, not an asset currently present in PR #37. No animation-engine math/classes live inside `abilities`.

## First Renewal look — for the animation team

Target asset: `mkm:renewal_cast`, start from a one-shot `APPLIED` event.
**Reference beat (14 cosmetic ticks; not gameplay timing):**
- 0–3: lift the active main hand toward the sternum; small shoulder/elbow
  lead-in, shoulders stable, minimal displacement, other hand maintains item.
- 4–9: open hand gently outward; a short pale green/gold glow above the palm,
  a few small particles orbiting the avatar's upper chest, then floating out.
- 10–14: hand returns to its previous locomotion/weapon pose with a smooth
  cross-fade. No player displacement, hitbox change, aim correction, camera roll,
  damage flash or intrusive screen overlay.

Suggested restrained color: mint/soft gold to contrast with mana's violet
bar; low intensity and reduced-motion option. Position the glow using the
MNAE hand/forearm socket instead of guessing a screen pixel. Keep first-person
viewmodel, left-handed mode and equipment attachments in the MNAE adapter.
The temporary fallback is **eight END_ROD particles near the torso only**,
shown once if no MNAE listener is registered; this is not the final gesture
or engine implementation.

Success confirmation is not a per-frame marker, and an `APPLIED` event must
not trigger `player.heal` (already complete), `player.attack`, stamina debit,
mana changes or a new ActionTimeline.

## Shared HUD layout with Combat

`ResourceHudLayout` provides a shared left-side column for resource bars
at scaled viewport widths >=120 and heights >=140, and hides both on smaller
viewports. Mana sits **70 GUI px above** stamina, with room for labels and
readiness, while each bar ends strictly before the vertical screen center.
At 200x200 the bar is 76 px wide; at 320x240, 136 px; at 480x270 and
larger, max 148 px. Compact abbreviated text is used on narrow viewports.
All visible labels, localized statuses and dynamically rebound hotkeys now
pass through `ResourceHudText.fit` measured with Minecraft's **real font
pixel width**; this prevents text escaping the left-side column even for
multi-key mappings, Cyrillic, or narrow 120–200px scaled viewports. The
truncation utility respects Unicode code points and uses ellipsis only
when it fits. This improves F5 line of sight without moving the model or
camera.

**Integration requirement for #30/#31:** C3.7 adds extra version/render
diagnostics in `MkmCombatHud`. During merge retain those diagnostics but
reposition them into a dedicated debug overlay; do not overlay the mana
column. Equipment-based maximum stamina and mana must use the shared
`EquipmentSnapshots.snapshot` when Equipment D1 is present, consistent
with the M2 adapter and combined-branch tests. Do not overwrite either
workstream's character/equipment code.

## Tests and limitations

JUnit: successful/failed healing eligibility, well-formed and adversarial
network decode (version, ID, enums, truncation, duration, trailing/oversized
frames), replay/actor filter, viewport bounds and spacing, measured-pixel
text clamping and Unicode. Existing M1 tests cover mana,
cooldown, save migration, world rollback and server eligibility.

CI Java 25 build, JUnit, bundled Fabric API, dedicated-server boot,
headless-Xvfb client boot and installer packaging. Headless boot is not
an in-game hand-gesture, multiplayer latency or FPS test.

**Manual acceptance required before merge:**
1. Windows 26.3 in-world damaged player casts H once: actual HP increases,
   mana falls, cooldown starts, one small confirm visual appears.
2. Full HP, low mana, busy combat, spectator, invalid packet, H spam:
   **no** success visual and no inappropriate resource debit.
3. Rejoin/respawn/dimension change: verify event replay state and existing
   mana/cooldown attachments. Remote players do not yet see VFX.
4. Left-handed and right-handed 1st/3rd person: MNAE uses actual leading
   hand and does not duplicate vanilla arms or item rendering.
5. F5 at 200x200 / 320x240 / regular GUI scale with C3 diagnostic enabled:
   both bars readable and neither obscures the avatar.
6. Long play or high latency: no double visual, leaked session state,
   renderer exceptions or unintended healing/damage.

**Arcane Bolt** remains disabled until Combat Core approves an authoritative
magic damage source + mitigation + PvP permission contract and one cast
timeline/interrupt protocol. Do not infer hit success from an animation.

## M3.1 security and integration update (issue #43)

The packet moved from v1 to **v2** to include `dimensionId`. A v1 event received
in a different dimension could have been treated as new after the old
client scene was replaced and the replay gate reset. Now the gate rejects
mismatched dimensions **before updating its sequence counter**. It also
rejects duplicate or reordered sequence values within a scene. The
server-owned sequence never wraps to 1 in an active session. Neither
network delivery nor the client renderer can claim a successful heal.

`MAIN_HAND` describes the **logical item/action hand** and **not physical
right arm**. MNAE must map the logical main hand through the user's actual
left/right dominant-arm preference and its `WeaponPresentationResolver`.
Do not bind Renewal to sword, axe, pickaxe or grip family. The semantic
`mkm:renewal_cast` action is a distinct small one-shot gesture: after
`APPLIED`, optionally overlay it on the current animation for at most 14
cosmetic ticks, then return to the original stance/locomotion and held-item
transform. The MNAE owner owns transitions, fades, physical hand pivots,
weapon grip and skeleton space; Magic only sends a bounded event and onStop.

**Save safety:** Isolated PR #37 still has CharacterData schema v2. Combined
PR #42 retains CharacterData schema **v3** as verified against PR #30 and
calls real `EquipmentSnapshots`. Character PR #35 adds **schema v4** with
keyed claim receipts; PR #42 does NOT contain v4. Never load v4 saves with
PR #42 or claim it can downgrade v4 safely. Test with a backed-up v3 save
and a matching dedicated server/client build. The full future integration
must retain v4 receipts rather than regenerate them from XP.
