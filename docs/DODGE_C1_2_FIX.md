# Action Combat C1.2: player dodge movement synchronization

## Failure reported in 0.0.3-fabric.3
The player presses `G + W`: stamina is charged, but the avatar hardly moves. The `DodgeRequest` serverbound payload and stamina rules work. However, `CombatService.tryDodge` applied `player.push` on the *server only*. Minecraft's own-player movement is locally simulated on the client, so a server-side player velocity update must be sent to the controlling player explicitly.

## Fix
- Calculate a **bounded horizontal impulse** with `DodgeMotion.fromFacing`: direction 0 forward, 1 backward, 2 left, 3 right, using server-side look direction and the server-side `DODGE_DISTANCE` modifier.
- The neutral impulse is 1.25 blocks/tick initial speed; bounded at 2.50 by the modifier. It is a momentary velocity, *not* teleportation or a persistent speed buff. Vanilla collision, friction and terrain apply normally.
- Apply `ServerPlayer.setDeltaMovement` instead of stacking a weak `push`.
- Send `ClientboundSetEntityMotionPacket(player)` to the player through `player.connection` immediately after setting velocity, so the local client receives authoritative movement.
- Avoid charging stamina when direction, look vector or modifier is invalid. Existing server-approved cooldown, ground and stamina checks stay unchanged.
- Pure JUnit tests for directional normalization, left/right/back/forward, invalid inputs, bounded modifiers.
- Version `0.0.3-fabric.4` and installer package updated consistently.

## Manual checks
1. Install only `mkm-rpg-0.0.3-fabric.4.jar` (remove previous MKM JAR) in Fabric 26.3 + Loader 0.20.1. Fabric API is embedded.
2. On flat unobstructed ground in survival, press `G + W` once. The player should make a clearly visible lunge in their facing direction. Try `G + A`, `G + S`, `G + D`.
3. Confirm a single stamina charge per successful action, no further movement when cooldown/resources fail, no movement in midair or while riding.
4. Test against walls/edges, sprinting, different FPS and multiplayer latency. The server remains authoritative; no client-supplied velocity.
5. The action is *still not* an animated dodge roll, grants no invulnerability and is not meant to pass through blocks. Dedicated-server CI does not prove client motion feel; user runtime check remains required.

### Why not replace it with a teleport?
Teleporting produces a visible location jump but can bypass the collision sweep and trigger invalid movement. An owner-directed vanilla motion packet is a narrower, safer repair. Animation state machine remains C2.
