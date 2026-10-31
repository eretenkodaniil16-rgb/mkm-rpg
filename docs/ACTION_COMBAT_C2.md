# Action Combat C2.0: real-time action phases, light combo and first HUD

Built on the user-verified `0.0.3-fabric.5` directional dodge implementation. C2 is an **incremental, opt-in combat slice**, not yet a full replacement of vanilla combat or a custom skeleton-animation system.

## Player-facing changes

- **R**: one queued sword/weapon attack; no selected target or client-authorized damage. Each attack runs server-owned `WINDUP → ACTIVE (one server tick) → RECOVERY → IDLE`.
- Tap **R again only after recovery** and before the short combo window closes to trigger attack 2, then attack 3. Pressing R during an action is ignored; no queue buffer is implemented yet.
- Three stages: stage 1 windup 4 ticks/recovery 6 ticks, stage 2 windup 3/recovery 6, stage 3 windup 5/recovery 10; active = 1 tick.
- Costs 8 base stamina per successful action (modified by the same server-side stamina-cost stat as dodge), never per client repeat or while recovering.
- In the ACTIVE transition the server sends the standard vanilla hand swing and attempts **at most one** attack on the closest living `Mob` in a 3.5-block radius, in the player's front cone and line of sight. Vanilla attack mechanics then compute the actual damage. A miss still costs stamina. Players, item frames, armor stands and other non-mob entities are not currently targetable by this special action.
- **G** still uses all eight user-verified dodge directions, stamina and velocity synchronization; dodge now also occupies a transient `DODGING` phase. Combo cannot start during a dodge. Heavy attacks are disallowed while C2 has an active action.
- **New HUD** over the hotbar: stamina bar, resource numbers, action phase and combo stage. Fabric 26.3's HUD extractor API and synchronized attachments drive it; resource values remain server-owned.
- **K** character sheet and **Shift + vanilla attack** remain; normal LMB combat is deliberately untouched. The new R action does *not* implement a new 3-D skeletal animation. A vanilla arm swing is played on the active frame. Fully animated rolls, upper/lower-body blending and attack animation assets are the next milestone C2.1.

## Architecture and safety

- `ActionTimeline` and `ActionTimelineRules` are pure deterministic immutable state with server game-time ticks and explicit transition rules.
- A separate transient Fabric owner-synchronized attachment `mkm:action_timeline` prevents persistence/schema migration and keeps the server authoritative.
- A minimal `mkm:combo` byte packet carries only intent `1`. The server chooses attack timing, direction, target, resource cost and final damage. No client-supplied entity ID or velocity.
- Dodging, repeated attacks, heavy action, and resource exhaustion have mutually exclusive gates.
- Regression tests cover phase ordering, combo window, blocking overlapping actions, resource refusal and previous dodge calculations.

## Runtime smoke test

1. Back up any important worlds and replace the previous MKM JAR with `mkm-rpg-0.0.4-fabric.0.jar`. Keep Fabric Loader 0.20.1 and Minecraft 26.3. The full official Fabric API is nested in MKM; do not install a separate copy solely for MKM.
2. Start a world and confirm the bar appears near the hotbar and character sheet still opens using K.
3. Stand ~2 blocks from a zombie. Press **R** once; confirm `WINDUP`, then hand swing and hit, followed by `RECOVERY`.
4. After recovery, press R again before the window closes (16 ticks = 0.8 seconds) and again for stage 3. Check the stages show `1/3 → 2/3 → 3/3`. At stage 3 the next attack restarts the chain.
5. Hold/spam R through recovery: the server must reject duplicate attacks and extra stamina charges.
6. Confirm G+W/A/S/D/diagonals still move in intended directions; combo cannot start during a dodge; Shift+LMB still behaves as earlier.
7. Test dedicated multiplayer server, low stamina, obstacles, mobs outside cone and client key rebinding. Check the server can launch without client HUD dependencies.

## Known limits

This is a state-machine and presentation milestone, **not finished animation combat**. Vanilla melee attacks are still possible during C2 action phases; full mutual exclusion will require explicitly intercepting vanilla attack events, tested for compatibility. The server-side special attack uses vanilla hit cooldown and damage mitigation, so its feel depends on weapon and attack strength; tuning follows user runtime feedback. No invincibility during dodging. The 1-tick ACTIVE phase is intentionally brief and visually signaled by a vanilla arm swing. As with all C2 code, Java compile and server smoke tests alone cannot verify a graphical client.
