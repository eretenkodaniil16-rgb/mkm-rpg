# MKM SHOWCASE-01 · WORLD — Issue #78
**Code branch:** `feature/world-showcase01-ilra-tower-issue78` (draft PR #90).
**Correct base:** `integration/fabric-v4-world-origins-enemy-perf-qa5` / PR #69, **CharacterData v4 retained**.
**Sources reused:** Ilra E2.1 #39/#45, Encounter Director #54, Enemy AI #56, Environment #60, unified 115-choice product spec #70, Showcase Epic #71.
**Scope:** one authored 10–15-minute route, **not** procedural worldgen or a new global AI/damage pipeline.

## Player-visible acceptance route (no commands AFTER preparation)

Environment architect/operator prepares a **disposable backed-up world** with the same version of the MKM client + server (26.3/Fabric/Java25) and a suitable flat/empty Overworld area in a supported biome (plains/forest/river/dappled forest). All 23 × 14 × 21 future tower voxels must be AIR, required foundation positions have support ≤4 blocks below, and all chunks must already be loaded.

Place operator/Ilra starting position at least 60–150 blocks from the intended tower site, while **tower chunks are loaded**. Example coordinates are illustrative and **not** guaranteed compatible with real terrain. One protected preparation command installs tower variant 1, registers its world anchor, and places Ilra near the operator:

```mcfunction
/mkmshowcase setup 1 100 65 100
```

`Y` is the first AIR layer above the site's ground. If a tower was previously installed via Environment #60, the operator may instead link it using `/mkmshowcase bind 100 65 100`; place Ilra with existing `/mkmworld place` if needed. Rebinding a configured world is intentionally disallowed; no forced replacement. To reproduce a night spawn in a short QA session, operator may set `/time set night` **before testing**; the normal player never uses commands.

1. **Talk to Ilra** (right-click). Existing E2.1 GUI and PER 12 trace investigation remain intact; a new line of dialogue announces the missing watch and saves a *separate* ShowCase progress flag. J opens the joint journal showing approximate rune position at tower origin + (7,5,6). If players find the rune first, discovery is recorded without Ilra.
2. **Travel to and explore the actual ruined watchtower** from Environment #60. Six original 16×16 blocks, 23×14×19 NBT structure, bridge/camp, ladder and a glowing rune slate are genuinely present; textures are vanilla-compatible. The existing loot barrel remains vanilla loot; **no repeatable invented item reward**.
3. **Right-click the upper rune slate**. A native Minecraft GUI offers two *irreversible* routes:
   - **Protect the ford:** available to anyone; the three-enemy encounter pays 100 MKM quest XP to its recorded owner and, once cleared, creates a named noncombatant `Хранитель переправы` near the tower (if safe space exists). The keeper UUID is reserved in persistent world data before insertion. This is the region's small, saved life change; never respawn duplicate.
   - **Read and seal the ruin:** requires either PER ≥12 or selected Mage class from the existing Origin M4; a Dwarf Mage and a Human Warrior (PER12) both qualify. On completion pays 125 MKM quest XP and, only if airspace is unobstructed, installs a vanilla soul lantern over the rune slate, displays particles and records distinct lore/outcome in J. Other players' blocks are never overwritten.
4. **Threat:** encounter data uses the original `mkm:ford_ambush` JSON with night/biome/danger gates. A group of 3 appears at a reserved ground anchor just beyond the tower's approach: `Road Raider` (Husk; **Enemy V1 humanoid tactical AI**), `Cave Stalker` (Spider; **Enemy V1 arachnid flanking AI**), and the vanilla Skeleton leader (bow, iron helmet, vanilla ranged goal). This adapts the pre-existing Encounter Director; no second AI scheduler, kill XP writer, damage rule, animation runtime or chunk ticket.
5. **Fight the group, then return to the rune.** The world ledger is authoritative. Each member's exact UUID, native family and owner tag must match a server `AFTER_DEATH` event; unloaded/out-of-range entities are **not** dead. Once all three are confirmed dead, right-click the rune again, then press the finish button. World marks terminal and Character M3 writes the one-file receipt + XP/level credits. Repeat clicks/relog must NOT pay again.
6. **Read J after restart.** Branch, outcome, coordinates and confirmed remaining member count are owner-local snapshots. Two players independently follow the same tower story with separate event keys and separate CharacterData v4 receipts; the world permits two 3-member groups (global authored budget six), but the common keeper/lantern visual effects exist at most once per prepared region.

## Event identity and save durability

- New **world** `mkm:showcase_01_site`: immutable site origin, configured bit and single keeper UUID. New **player** `mkm:showcase_01_progress`: v1 `unknown→found→active→completed`, frozen `defend|study` choice, death-copy semantics. Old `mkm:ford_quest` v3, `mkm:ford_encounter_registry` and CharacterData v4 stay structurally untouched.
- New authored groups are stored in the existing **world** `mkm:encounter_director_v1` under `mkm:ford_ambush/<ownerUuid>_<branch>`. The branch is part of the event key to permit reconciliation if player data rolls back. Old GM `mkm:ford_ambush` event key remains a separate, backward-compatible occurrence.
- During story choice, the immutable personal branch is stored before world-spawn reservation. Encounter Director reserves **all 3 UUIDs** and frozen reward amount before inserting entities. A partially spawned or unloaded group never triggers speculative replacement. At most six actively reserved authored members across this ServerLevel; historical terminal records cannot be evicted or reactivated.
- On login/talk/rune interaction, an event still in world storage but missing from player's stage is safely re-associated, including the original branch. A character receipt committed before a world `claimed` flag is retried via Character M3 `ALREADY_GRANTED`; never top up legacy XP floors. World `claimed` with missing Character receipt requires human audit.
- Two independent world saves maintain independent level attachment instances. **This is not an ACID transaction** across entity file, player file and world-level attachments. An abrupt unsaved power loss may still yield an orphan; no dishonest exactly-once guarantee for a half-written checkpoint. Snapshot with `/save-all` and test restart explicitly.
- A helper NPC is created at most once. Its UUID is reserved before entity insertion; if insertion fails, do **not** silently create another on each login. A missing neutral NPC/region seal after rollback requires manual audit. No global per-tick region simulation.

## Exact boundaries and safe fallbacks

**World:** authoring site, story, ecology, region state, spawn budget, branch and relationship to NPC, reward eligibility, delayed terminal transition.  
**Enemy AI:** consumes read-only faction/role tag and owns tactical movement and detection. Already included from PR #56; World tags existing prototype entities and initializes their published level-based attributes once at spawn.  
**Combat:** only existing Combat/CharacterEvents receive real damage/death callbacks; event never fabricates hits. EnemyService may drop one prototype material token via its own death guard; Story reward gives **only XP**, no repeated inventory grant.  
**MNAE:** optional semantic `NpcVisualSignals`, no animation skeleton, model/pose or damage changes.  
**Environment:** 3 original templates from PR #60, safe blank-space preflight; no worldgen or player-construction replacement.

### Manual QA matrix / strict remaining gates

- [ ] Actual in-Minecraft screenshot + F5 gameplay video of Ilra conversation, J, tower traversal, rune UI, both AI families and end effect (not a Blender-only render).
- [ ] Both choices tried in *separate backed-up save snapshots*, with Warrior PER <12 rejected for study and Mage PER <12 accepted. Defend 100 XP + new Villager; Study 125 XP + soul lantern/lore; no second claim.
- [ ] 2 simultaneous clients A/B: both see different personal stages/J and own 3 UUIDs, only owner gets quest XP, killer may get normal kill XP; no private info in other player's UI.
- [ ] Loaded/unloaded chunks and server shutdown/restart with 1, 2 or 3 living members; entries remain active, never respawn missing entity. Validate no old GM event overwrite, no duplicate player rewards.
- [ ] Crash-cut combinations: world event present/player stage missing (reconcile chosen branch), player state present/world missing (fail closed), receipt present/world DEFEATED (finalize without second XP), CLAIMED but no receipt (manual audit).
- [ ] Setup rejects occupied area, non-Overworld, missing support, missing templates, out-of-world border, unsupported chunk, Peaceful and biome/night start gates; repeated setup/bind rejected.
- [ ] CharacterData v3→v4 migration preserves points and XP; no rewrite or client sync of private receipts.
- [ ] GUI @320×240 and 1920×1080 with and without optional shaders; stable vanilla mining/block/interact; role/weapon/offhand unaffected; client and server build SHA identical.

**Release gate:** green Java25 JUnit, Fabric API JAR integrity, real server and Xvfb client startup, tower NBT verifier; then independent manual playtest. CI alone does NOT prove 10–15-minute gameplay acceptance.

## Next architecture, not implemented

For rare large dungeons: separate encounter definition/region anchor/room wave state and stable region-issued UUIDs, with bounded per-chunk activation rather than forced loads. For factions: stored reputation, explicit relationship matrix, settlements only when physically loaded, no unbounded per-tick scans. For off-chunk events: record abstract, deterministic progress markers for already-authored happenings; only materialize entities upon legitimate nearby chunk loading after conflict-free reconciliation. Do not simulate hundreds of remote NPC pathfinders or auto-place overwriting structures.
