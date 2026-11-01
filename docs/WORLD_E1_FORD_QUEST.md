# MKM World E1 — «Смотрительница брода»

**Status:** draft integration candidate. **Runtime:** Minecraft 26.3 / Fabric 0.20.1 / Java 25.
**Base:** `feature/action-combat-c2-state-hud`. **Implementation branch:** `feature/world-e1-ford-warden-quest`.

## Game experience

A single **authored encounter** placed by an operator in a normal Minecraft world. No replacement world generator, automatic villages, global faction simulation, or speculative RPG framework.

**Logline:** A supply caravan vanished at a ford. Warden Ilra suspects a raider. The player can decline and return later, investigate tracks using PER, accept responsibility, defeat a wandering Husk in real-time Minecraft combat, and collect MKM XP. The outcome survives relog and death; completion is a permanent player-specific fact.

| Stage | In-game player action | Authoritative server outcome |
| --- | --- | --- |
| Not started | Right-click Ilra / `/mkmworld talk` | Dialogue shown; no mutation |
| Not started / active | `/mkmworld inspect` | PER >= 12 records tracks, potential +25 quest XP; no hidden random roll |
| Not started | `/mkmworld leave` | Explicit refusal, quest stays unstarted |
| Not started | `/mkmworld accept` | Spawns one persistent owned Husk; only then changes stage to active |
| Active | Fight tagged Husk (normal Minecraft/MKM combat) | Confirmed owner kill changes stage to return |
| Active | `/mkmworld recover` near Ilra | Local search; at most 2 recovery spawns over the life of this quest; only successful spawn consumes a recovery |
| Return | `/mkmworld claim` near Ilra | One-time +75 MKM character XP, or +100 with tracks; moves stage to rewarded |
| Rewarded | Talk/claim/relog | No second reward or reset |

The protagonist is a **sandbox player**, not teleported into a quest instance: may build, explore, craft, fight unrelated enemies, and return later. Vanilla villagers outside the tagged pilot actor are unaffected. The Husk uses normal Minecraft hostile AI; no custom choreography or D&D initiative.

## Quick QA

1. In a **test world** on Easy/Normal/Hard with cheats, stand on level, safe, open ground. `/mkmworld place` creates Ilra. Repeating it near Ilra should refuse to add duplicate NPC.
2. Right-click. Test `/mkmworld leave` and `/mkmworld talk`: status remains `not_started`. Run `/mkmworld status`.
3. Try `/mkmworld inspect` with baseline PER 10: must fail without mutation. With PER >= 12 (via approved Character build allocation), must record the observation **once**.
4. `/mkmworld accept` spawns a single Husk nearby, while Peaceful/blocked spawn MUST NOT accept. Repeating the request MUST NOT spawn again.
5. Defeat owned raider by direct melee; verify stage `return_to_warden`. Attempt premature claim before this: must not pay.
6. Return to Ilra, claim. Verify `/mkm stats` differs by exactly 75 or 100 MKM XP (separate from vanilla XP). Spam claim: no additional MKM XP.
7. Quit/reload, die/respawn, change dimension, reconnect. Stage and XP remain. Two players have independent quest progress; only the owner killing their own raider gets credited.
8. Simulate a missing raider with `/mkmworld recover`. Ensure existing raider within 48 blocks blocks recovery and repeated recover cannot exceed the persisted 2-use budget.
9. Try commands away from NPC, as spectator/dead, or with future schema / invalid stage. Server should refuse state changes.

**CI gate:** Gradle build + pure JUnit tests; verify bundled Fabric API JAR, Windows installer, dedicated server initialization. These gates do not replace client/manual persistence, interaction or multiplayer smoke tests.

## State, compatibility and implementation boundaries

- Owner: player attachment `mkm:ford_quest`; immutable `FordQuestState`, Fabric persistence, `copyOnDeath()`. Version **2** adds `recovery_count` to v1 (`schema_version`, `stage`, `found_tracks`); v1 migrates with default 0; unknown future schema and stage fail closed.
- Authored world actors: vanilla Villager and Husk with explicit persistent entity tags and gamerule-compatible AI. Only gamemasters place the pilot actor. One actor within a local 48-block radius; no worldgen or world-scoped save yet.
- Client: no authoritative client request packet in E1; commands and entity interaction route to the server. No dependency on client classes.
- Progression: reward uses **existing** `CharacterService.addExperience`, not Minecraft XP or quest-specific stat calculators.
- Equipment: **not yet granted**. D1/D2 equipment branches provide stat calculation and vanilla items, but there is not yet an agreed idempotent loot-grant API. Do not build a competing inventory registry.
- Combat: existing Minecraft physical hit validation, vanilla enemy AI and MKM C2 combat, with no rewrites of `CombatService`, `ActionTimelineRules`, attack markers or animations.

## Known limits and blockers

1. **Crash atomicity:** marking quest `rewarded` and adding XP mutate different attachments. Rapid-repeat/relogin cannot mint a second quest award, but an abrupt server crash in between may lose XP. The Character stream must provide **persisted keyed grant-once** (or a combined atomic transaction) before production-grade exactly-once semantics are claimed.
2. **Recovery:** 2 permitted successful recovery spawns cap repeated farming, but cannot guarantee rescue after multiple disappearances. The original raider can wander beyond the 48-block local search; duplicate enemies may then exist. Need a durable encounter identity / chunk unload strategy in Encounter stream. This cap is intentionally a *temporary defense*, not the final subsystem.
3. **Attribution:** direct `DamageSource.getEntity() instanceof ServerPlayer` only; no pets, DoT, environment or party sharing. Crediting an arrow is not guaranteed until a dedicated integration test.
4. **Quest identity:** intentionally player-global pilot progress, not shared between players or tied to a particular Warden UUID or dimension. A full campaign needs explicit world/party scope.
5. **Dialogue:** commands substitute for a modal conversation UI. No speech checks beyond deterministic PER threshold and meaningful accept/decline; no faction ledger, consequences across settlements or dungeon generation yet.
6. **Character M1 interaction:** if M1 is integrated, its prototype hostile kill payout can *also* grant 25 MKM XP independently of the quest award. Verify quest payout as a **delta at claim**, not total XP since start. PER 12 is unreachable from fresh baseline 10 without spending progress credits or an administrative test setup.

## Integration decision requests

See `docs/WORLD_E1_TEAM_HANDOFF.md` for cross-chat ownership, exact requests, non-goals, merge sequencing, acceptance and decision checklist.
