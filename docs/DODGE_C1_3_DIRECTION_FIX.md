# Action Combat C1.3 — direction-sensitive dodge

## User-reported behavior
Version 0.0.3-fabric.4 successfully sends server-authorized momentum to the controlling client, but the dodge only seems to move forward, regardless of intended directional input.

## Why the implementation changed
Fabric 26.3 now has `LocalPlayer.input.keyPresses`, a per-tick `Input` record holding the same movement booleans that Minecraft's `KeyboardInput` uses to move the player. The old code polled separate `client.options.keyDown/Left/Right.isDown()` values only when `G` was consumed and often fell back to the constant `direction = 0`. This could be observed as a forward dodge even if a movement key was pressed.

Now client code reads `client.player.input.keyPresses` for forward/backward/left/right and maps to eight possible `DodgeRequest` intent values. This is independent of WASD's physical key positions, so rebinding movement keys is supported.

## Controls
- G: forward (fallback when there is no movement input)
- W + G: forward
- S + G: backward
- A + G: left
- D + G: right
- W + A + G: forward-left
- W + D + G: forward-right
- S + A + G: backward-left
- S + D + G: backward-right

On the server, `DodgeMotion` recomputes the resulting horizontal velocity using the server player's orientation and bound `DODGE_DISTANCE`. Every diagonal impulse is normalized, so diagonal dodges do not cover more distance than cardinal dodges. Invalid wire directions are rejected before stamina is spent. The on-screen message now includes the resolved direction (e.g. `Dodge Left`), simplifying testing.

## Verification
Run the Minecraft 26.3 Fabric client with `mkm-rpg-0.0.3-fabric.5.jar` (and no other MKM JAR), face a distinctive landmark and press each combination while **holding the movement key first**, then pressing G. Check that character position changes relative to the facing, and the on-screen message matches. Confirm G alone is forward, diagonals do not travel farther, each valid dodge costs stamina once, cooldown and ground checks remain active, and servers reject malformed inputs.

CI covers the direction mapping, motion normalization, unit tests, dedicated-server boot and installer packaging. A real client test is still required for input event timing and visual presentation. This remains an unanimated dodge impulse; C2 adds state/animation.
