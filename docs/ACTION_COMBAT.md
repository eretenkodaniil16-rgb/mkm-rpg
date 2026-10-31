# Action Combat C1 — vertical slice

## Direction

MKM deliberately selects **Variant C: Action-RPG combat**, retaining Minecraft movement and hit detection but expanding the combat loop with stamina-backed actions, dodge, poise and later animation/combo states. This is **C1**, not the finished entire replacement.

### Controls

- Left click: vanilla attack with MKM physical-power/melee-damage scaling.
- Shift + valid left-click melee hit: heavy strike (1.65x the MKM-scaled strike) if enough stamina and the action is off cooldown.
- G + WASD: directional dodge. If no movement key is held, dodge forward. The server computes and bounds displacement from the player's facing direction.
- K / MKM Codex: existing Character Sheet; shows synchronized current and maximum stamina.

### Resource rules

- Heavy base cost: 25 stamina; base recovery 12 ticks.
- Dodge base cost: 20 stamina; cooldown 18 ticks and recovery 8 ticks.
- Both use the existing stamina-cost modifier; endurance affects maximum stamina, regeneration and delay.
- Normal Minecraft strikes cost no stamina.
- Regeneration starts after the calculated delay. The current state is temporary and owner-synced; it resets on reconnect/death. It is not added to CharacterData schema v2.
- No invulnerability frames for dodge in C1.
- Server is authoritative. Clients send a one-byte direction intent, not velocity, cooldown or stamina.

### Physical hit and poise

NeoForge 26.2 LivingIncomingDamageEvent is used after hit/vulnerability checks but before vanilla shield/armor reductions. Only direct player melee (`DamageTypes.PLAYER_ATTACK`) is modified. Projectiles, fall damage, magic and mob damage are untouched.

Pre-mitigation damage is multiplied by the existing `physical_power * melee_damage` values and additionally by the heavy multiplier if the server approves a charged attack. Vanilla shield, enchantment, armor and absorption remain in their normal pipeline.

Mob targets accumulate transient poise after the final damage-before-health stage, with decay and a threshold; once reached the C1 stagger prototype temporarily applies strong slowness. This is **not** yet a full animation/AI interruption.

### Boundaries and future work

Do not assume this is an animation-complete overhaul. C2 should add animation-facing combat states and real telegraph/interrupt integration after runtime tests. A dedicated stamina HUD, weapon-specific attack styles, combos and careful PvP rules come later. No Mixins are introduced in C1.

### Verification

1. Minecraft 26.2, NeoForge 26.2.0.77, Java 25 and MKM 0.0.3.
2. Verify Character Sheet and synced stamina; verify /mkm commands and existing Character Core.
3. Punch zombie normally; check vanilla hit behavior and the MKM melee multiplier.
4. Shift + hit repeatedly; ensure 25 stamina consumption is gated, recovery is enforced, and no unexpected repeated charging occurs.
5. G + directional input on ground; verify server movement, cooldown and stamina.
6. Build poise through consecutive hits; verify short mob slowdown only after threshold.
7. Confirm armor/shield interactions and multiplayer; test save/rejoin/death and dedicated-server startup.

This feature branch is not ready for merging without the runtime checklist.
