# MKM Encounter Director M1 — World, NPC, Quests, Ecosystem

**Implementation:** draft PR #54, stacked on Fabric CharacterData v4 integration PR #45 (and World E2.1 #39). **Build:** Minecraft Java 26.3, Fabric 0.20.1, Java 25. **Status:** first operator-triggered pilot; no ambient worldgen, no new mobs/AI/animations/Combat damage hooks.

## 1. World E2.1 safety audit and storage comparison

### Existing Ilra E2.1 contract (do not regress)
- Server-authorized NPC Villager interaction, player <=6-block check, request stage + nonce, 60-second single-use session; wrong NPC/packet replay fail closed. `FordDialogueRules` is separate from GUI; PER>=12 can reveal tracks before or after accepting.
- Journal J comes only from owner-server snapshots; unsolicited pushes never open UI and response ID is correlated to an explicit request. Character's PER and XP are server-side.
- Player `mkm:ford_quest` schema v3 tracks stage, encounter UUID/warden UUID, tracks, legacy floor (decode-only). World `mkm:ford_encounter_registry` retains owner→(raider UUID, Warden UUID, spawn coordinates); `mkm:ford_encounter_outcomes` persists verified death tombstones for both online/offline owners. Unloaded is **unknown**, never dead; blind respawn is disallowed.
- CharacterData v4 owns one immutable attachment transaction containing experience, level points and up to 4096 stable receipt IDs. Ilra uses `mkm:ford_warden/complete`; the new event uses **a distinct** `mkm:encounter/ford_ambush` receipt. Server `grantExperienceOnce` MUST happen before marking either quest/encounter rewarded. No legacy XP-floor top-up. Same reward ID is player-scoped.
- Tested pure state/packet/codec/reward logic in #39/#45. **Not verified:** real two-client dialogues, actual unload/reload and crash/hard-power loss. World and player attachments have different physical save boundaries; not ACID.

| Persistence strategy | Unloaded chunk | Restart | Duplicate risk | Fit |
| --- | --- | --- | --- | --- |
| Entity tags only | Entity not queryable; cannot conclude death | UUID/tag persists if entity saved | High if looking only at loaded mobs | Identity evidence, **not** authoritative event lifecycle |
| Player attachment only | Works for solo quest but can race with world save | Per player | Cross-owner or mob ambiguity | Ilra narrative and Character receipts |
| Per-`ServerLevel` persistent attachment | No chunk forcing, roster/terminal event independent of entity cache | Saved with world-level data | Low with reserved UUIDs, but still cross-file crash windows | **M1 selected**: small world-owned encounter ledger |
| Per-chunk state | Affects unload/save lifecycle; hard to inspect other chunks | Per chunk | Boundary crossing and orphan recovery complex | Future dense procedural encounters |
| Minecraft `SavedData` index + per-region journals | Chunk-independent, scalable to many ids with indexing and migrations | World/level saved | Needs explicit checkpoint design and versioning | **M2 evaluation** after load/scale benchmark |
| External SQLite/service | Survives process but creates new deployment/transaction constraints | External | Needs two-phase reconciliation with entity files | Not appropriate for first Fabric mod release |

Neither a UUID nor a loaded entity query proves death. Do not call `level.getEntity(uuid)==null` a reason to spawn a replacement. Entity `setPersistenceRequired()` avoids normal despawn but does not guarantee survival from Peaceful, commands, other mods or file corruption.

## 2. Director ownership

World owns **where/why** an encounter is authorized, biome/time/danger gate, maximum roster, persistent world event UUID/role ledger, quest link, frozen reward amount, all authoritative stage transitions, and explicit exclusion of previous terminal events. Character M3 owns XP, points and server-private reward receipts. AI owns **what creatures decide** once spawned: patrol waypoints, threat assessment, ranged spacing, aggro coordination, role/faction reactions and AI tick budget. Combat owns authoritative vanilla/LMB damage/guard/effects (no animation marker may deal damage). MNAE owns cosmetic rig, clip, network and render if it subscribes to `NpcVisualSignals` (not implemented by World).

Only allowlisted vanilla entity IDs can spawn in M1. The following persistent vanilla entity tags are factual hints, **not authorization by themselves**: `mkm.encounter.ford_ambush.v1`, `mkm.encounter.owner.<UUID>`, `mkm.encounter.role.frontline|leader`, `mkm.encounter.elite.v1`. AI must not edit `mkm:encounter_director_v1` or award XP on its own.

## 3. Authoring data versus server code

**Minecraft Java 26.3** uses datapack version 121.0. Natural mob-spawn lists/costs moved from biome fields to **`minecraft:gameplay/natural_mob_spawns` Environment Attributes**. Configured features also moved to `worldgen/feature`, and Fabric supports reloadable custom registries. This first Director deliberately avoids manipulating these vanilla tables and avoids custom biome/worldgen registry injection.

**Current JSON:** `src/main/resources/data/mkm/encounter/ford_ambush.json` (overridable by server datapack; initial load and `/reload`). The server reload listener parses with a Codec, validates namespace, size, allowed mob IDs, biome list, day/night, 1–5 danger, roster exactly three, one elite and positive XP. Invalid/missing JSON **disables new starts** rather than silently falling back. A saved event stores actual entity UUIDs and **frozen rewardXp**; changing JSON after launch never modifies live or completed events.

**Future JSON fields** (not implemented): explicit biome tag selectors, difficulty bands and threat budget weights, weighted variants, regional biome ecology tables, route/safe-zone nodes, optional loot table reference and quest requirement ID, faction/relationship tag, level scaling band, eligible terrain + structure tags. Validate those with Codecs/datafixers, bounded sizes, resource identifiers and whitelist. JSON must never contain executable Java classes, damage formulas, reward grant calls or direct AI code.

**Remain Java/server-owned:** permission and authorization checks; whole-group placement planning; loaded chunk/collision checks; real active-count bounds; identity binding; authoritative death callback and mission result; receipt grant/retry; idempotence; failed-save reconciliation; TPS-budget enforcement; audit/repair tools. RNG must be seeded from persistent encounter identity if/when procedural variants are enabled.

## 4. First playable prototype — mkm:ford_ambush

**Story:** On a moonlit route near the ford, two desiccated raiders screen a mounted-lookout position while a bone sentry armed with a bow commands the ambush. This is an authored mini encounter, not new faction AI.

- **Place:** one operator-chosen location in normal Overworld terrain, biome `minecraft:plains`, `minecraft:forest`, `minecraft:river` or new `minecraft:dappled_forest`; nighttime (time tick >=13000 modulo 24000). Night prevents the vanilla skeleton from immediately burning; the elite also has an iron helmet. No generation of blocks, houses, navmesh or structures.
- **Actors:** 2 `minecraft:husk` `frontline` melee; 1 `minecraft:skeleton` `leader` (elite: vanilla bow + iron helmet). Vanilla AI scheduler and combat remain unchanged. All are persistently named/tagged. The difference in melee/ranged positions provides the first tactical group scenario.
- **Commands:** GM `/mkmencounter start` (requires standard gamemaster level, solid unloaded-safe positions within ~9 blocks, non-Peaceful). Everyone can query `/mkmencounter status`, but only the recorded owner sees actor UUIDs and exact location; the recorded owner can `/mkmencounter claim` within 16 blocks of the launch anchor after the third confirmed death. Use `/time set night` only in an isolated test world if needed.
- **Rewards:** exactly one `100 MKM XP` *quest* receipt for owner only, ID `mkm:encounter/ford_ambush`; vanilla combat kill XP (25 for a last hitter where existing CharacterEvents applies) is a SEPARATE mechanism. Neither this World PR nor MNAE synthesizes a damage event. The world event becomes `claimed` after the Character v4 receipt succeeds.
- **Two players:** A operates/owns start, B assists. Both can fight, only A can claim the World event reward; B remains eligible for ordinary kill XP if B lands the actual last hit. A and B's CharacterData v4 receipt sets are independent.
- **Limits:** exactly one occurrence per Overworld world save; immutable terminal record blocks re-creation. World attachment supports at most 128 records and max 6 simultaneously reserved nondead members by design; prototype actually needs 3. No per-tick global searches, no force-loaded chunks, no entity scanning on server tick. A failed spawn leaves `reserved` and must be reviewed rather than spawning more mobs.
- **States:** `absent -> reserved (persist roster UUIDs, reward, owner) -> active (all spawned) -> defeated (all UUIDs saw real AFTER_DEATH) -> claimed (M3 receipt confirmed)`. Member death booleans only change from false→true and never clear; saved owner, anchor, frozen reward and mob identity cannot be rewritten by a later transition. `EncounterLedger.Entry.mayAdvanceTo` enforces monotonic state and forbids terminal rollback. Replayed/unknown kill UUID does nothing.
- **Crash behavior:** `active` persists through chunk unload/relog/restart independently of actual loaded entities; a missing loaded mob is UNKNOWN, not dead. There is no respawn after load and no global per-tick repair; operator must investigate genuinely removed mobs. An interruption between saving the Character receipt and World claimed stage retries as `ALREADY_GRANTED`. Lost world/player file saves can still disagree: **not true ACID or guaranteed power-loss exactly-once**.

## 5. Group/relationship/ecology design (future, NOT implemented)

The Director needs a finite-state **faction relation matrix** rather than treating every nearby hostile as an ally. Example proposals:

| Cohort | Other cohort | Default response | Who owns it |
| --- | --- | --- | --- |
| Same authored raider squad | Same squad | Friendly/coordination | AI reads Encounter group metadata; World provides ownership ID |
| Undead raiders | Vanilla undead | Neutral by default; avoid universal ally assumption | AI faction rules |
| Raider squad | Settlement watch | Hostile only in explicitly tagged territory | AI + settlement diplomacy data |
| Wolves/wildlife | Raiders | Vanilla AI first; no forced targeting | Vanilla/AI |
| Boss/miniboss escorts | Their leader | Defend/reposition, capped leash/range | AI |
| Different player-owned quest mobs | Each other | No cross-quest target scripting by World | AI with quest owner key |

**Candidate encounter map (design only):**

| Environment | Passive/neutral life | Hostile ecology | Patrol/faction idea | Elite/mini-boss gate |
| --- | --- | --- | --- | --- |
| Plains / meadow / river | Farm animals, horses | Night raiders, drowned near water | Ford road patrols | Bone sentry (M1) |
| Forest / dappled forest | Foxes, wolves, livestock | Raiders, spiders at night | Abandoned camp watch | Veteran ambusher |
| Taiga / snowy taiga | Wolves, foxes | Strays, winter bands | Hunter outposts | Frost ranged commander |
| Swamp | Frogs, slimes | Drowned, witches | Marsh smugglers | Mire alchemist |
| Desert / badlands | Rabbits, camels | Husks, marauders | Caravan pressure points | Dune champion |
| Caves / underground | Bats and local passive life | Spiders, skeletons, cave-hostiles | Vertical tunnel patrols | Brood guardian |
| Future ruins/dungeons | Context-specific | Scripted wave defenders | Room/door triggers, no uncontrolled respawns | Keyed room mini-boss |
| Future settlements | Villagers/livestock | Attackers only when triggered | Guards and faction territory | Faction captain |
| Nether/End (future) | Dimension-native | Vanilla native hostile sets | Stronghold route encounters | Biome/dimension-specific boss anchors |

These rows are proposals, not claims that Minecraft implements these factions or bespoke AI.

## 6. Performance budget / conflict with vanilla spawn

M1 spawns exactly three persistent vanilla mobs on one GM command, and DOES NOT edit Minecraft 26.3 natural-spawn Environment Attributes. World collision/loaded-chunk checks are local and executed once. The Director does not tick mobs itself, force chunk loads, or emit constant NPC visual cues. Persistent authored mobs still consume normal vanilla AI tick budget and may not count exactly like naturally spawned mobs toward every vanilla mob cap: **benchmark rather than assume**.

Before enabling automatic/procedural encounters: benchmark dedicated server p50/p95/p99 MSPT with 1/4/16/64 authored mobs and vanilla baseline, actual nearby loaded chunks, AI pathfinding, entities per player, chunk-load bursts, heap/GC and cross-dimension active reservations. Require performance regression upper bounds, species/biome caps, spawn cooldowns, avoidance near player bases, claim/progression anti-farming, dimension/region quotas and deletion reconciliation. Never treat static regional quotas as an excuse to force-load unloaded chunks.

## 7. Handoffs

**«Враги и AI»**: consume read-only encounter ID, `owner UUID`, `role`, `elite`, group roster and native mob type. Future separate contract `EncounterSpawnContext` should pass safe territory/role info from World to AI; no AI writes to ledger, spawn limits or XP. Implement pathfinding, role behaviors and compatible faction matrix with bounded expensive searches.

**«Бой»**: all hit detection, damage, guard and kill credit remain vanilla/MKM Combat/CharacterEvents. Use existing AFTER_DEATH callback only for encounter objective. Elite helmet/bow are vanilla equipment; no damage multipliers, reach changes or second kill-damage path. Test sword, shield, offhand, bow and 2-player killer/owner split.

**«Проверяющий»**: verify pure `EncounterDefinitionTest` and `EncounterLedgerTest`, full CI including dedicated server + Xvfb client; then live two-account test. Check night forest/plains/river biome gate, collision rejection with no record, permission denial, all member UUIDs persisted, `/save-all` + full restart, run away/unload/return, other player's last kill, duplicate event/death request, award replay/relog/level cap, `/reload` config validation, legacy E2 Ilra stability. **A headless PASS is not a gameplay PASS.** Test with a disposable backed-up save.

**Known merge gate:** draft PR #54 cannot merge until explicit 2-player, unload/restart, player-v4 migration, and client-server tests are accepted; the MNAE Issue #43 weapon/hand/rig problems remain outside this PR.

### Verified technical guardrails and evidence gates

- Full CI for this branch compiles Java 25, JUnit and dedicated Fabric 26.3 client/server. The strengthened dedicated-server smoke **also requires** the log marker `Loaded encounter mkm:ford_ambush with 3 mobs`, proving the JSON datapack was read during startup rather than merely packaged. Do not equate this with actual command execution or a visual encounter.
- The vanilla `AbstractSkeleton` constructor selects an initial goal with empty equipment. After explicitly equipping the prototype's bow, World calls vanilla `reassessWeaponGoal()` to select the actual ranged bow goal, without implementing its own AI scheduler.
- CLI `status` redacts owner UUIDs and precise spawn anchors for other players. Repeated death, late packet, other-player claims and re-spawn attempts remain non-authoritative. The encounter is world-shared; the owner only receives its one authored reward.
