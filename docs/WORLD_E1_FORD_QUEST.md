# E1 pilot — The Ford Warden / Смотрительница брода

**Branch:** `feature/world-e1-ford-warden-quest` (base: `feature/action-combat-c2-state-hud`).
**Runtime:** Fabric / Minecraft 26.3 / Java 25; server authoritative.

## Gameplay / reproducible smoke procedure

1. Start a creative test world with cheats enabled and set a non-Peaceful difficulty. Stand on open, level terrain with at least a few empty blocks within ~8 blocks.
2. Use `/mkmworld place` as a gamemaster. This places a tagged, stationary, protected vanilla villager named Ильра (no global generation).
3. Right-click her (or `/mkmworld talk`) to see dialogue. Choose `/mkmworld inspect` for a deterministic PER >= 12 observation, or `/mkmworld accept` directly. `/mkmworld leave` explicitly refuses without changing state.
4. Acceptance spawns one persistent vanilla Husk with a player-owner tag; fight normally using Minecraft/MKM real-time combat. A verified owner kill advances the quest to `return_to_warden`. If the mob disappears, use `/mkmworld recover` near the NPC (scans only a local area). Other players' kills do not earn quest credit.
5. Return to the same NPC and `/mkmworld claim` for 75 MKM character XP, or 100 after a successful tracks inspection. Verify `/mkm stats`, `/mkmworld status` and repeat `/mkmworld claim`.
6. Save and quit, reload, and verify `rewarded` persists and the reward cannot be reclaimed. Also test after respawn, and with two players independently accepting the quest.

## Server architecture

- Player-scoped state: `mkm:ford_quest` persistent Fabric attachment, schema v1, copy-on-death. Unknown stage and future schema versions fail closed.
- Local authored world content: vanilla Villager/Husk entities with persistent, namespaced scoreboard tags; no invasive mixins, custom entity registry, worldgen, global tick scans or duplication of vanilla AI.
- Dialogue: right-click talks; each action is currently a chat-command choice. Server verifies proximity to an authored warden, player spectator mode, attributes and state transitions. Dialogue UI / journal are deferred to the UX stream.
- Combat event: `ServerLivingEntityEvents.AFTER_DEATH` checks Husks with matching owner identity and moves the player's quest state. It never modifies attack choreography or damage.
- Reward: exclusively `CharacterService.addExperience` (not vanilla levels). Equipment domain is not implemented on this base; **no provisional second item registry/loot system** is introduced.

## Deliberate limitations / important contract

- Claim marks `rewarded` **before** adding XP to avoid duplicate payout on ordinary repeat requests and reconnects. Cross-attachment crash atomicity is **not guaranteed**: a hard crash between the attachment change and Character Core's XP mutation may lose the XP. The Character Core workstream should expose `grantExperienceOnce(player, rewardKey, amount)` with a persisted redemption ledger; Equipment should provide equivalent idempotent grant semantics. Until then **do not claim exactly-once under power loss**.
- Encounter ownership is individual, not party-shared. Recover checks within 48 blocks; an existing raider that wandered further away may coexist with a recovered one. Only the first valid death can advance state, and reward is still single-use.
- World state is local to tagged persistent entities. Global faction reputation, world flags, structured journal, dialogue GUI, terrain generation and encounter-boundary AI are not yet implemented. Vanilla villager interactions outside this tagged actor are preserved.
- Spawn may fail on obstructed terrain or Peaceful. No quest state advances unless the original encounter entity is successfully created. World generation, protected region and dungeon design are deliberately postponed.
- State schema has no implicit reset. Future general quest journal migration must explicitly map `mkm:ford_quest` and its `rewarded` status without erasing past completion.

## Integration contracts to control chat

1. **Character Core:** introduce a durable `grantExperienceOnce(player, ResourceLocation rewardId, long amount)` service and atomic/idempotent XP ledger; reconcile existing completed quest saves.
2. **Equipment:** design `grantEquipmentOnce(player, questRewardId, equipmentDefinitionId)`, inventory-full behavior and stable registry/data definitions. Do not create a shadow item implementation here.
3. **Combat:** provide a reliable killer/participation event abstraction for ranged, pets, DoT and parties; this MVP credits only direct `DamageSource.getEntity() == owner`.
4. **AI/encounters:** implement bounded encounter territory, reattach/chunk-unload handling, safe local respawn, no escalation from global scans; keep existing combat damage/timeline.
5. **UX/dialogue:** player-local quest journal and modal dialogue choices, authenticated server intent and response packets; commands are currently the fallback.
6. **Factions/reputation:** reputation service should consume a quest-finalized event with idempotency key; no independent reputation totals here.
7. **World/persistence:** future global flags need world-scoped SavedData or equivalent, never stored redundantly per player.

## Verification gate

Pure JUnit tests exercise order, duplicate, unknown stages and perception. CI must compile/test against Fabric 26.3 and dedicated server must load and run this flow; client smoke and persistence/respawn/multiplayer checks remain required for acceptance. No manual runtime success is asserted merely by committing code.
