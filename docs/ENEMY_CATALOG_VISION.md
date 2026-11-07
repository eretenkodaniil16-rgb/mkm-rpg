# MKM Enemy Catalog — design candidates, NOT 48 implemented entities

**Scope:** 48 proposed archetypes in eight families; 2 tagged vanilla-host prototypes currently have Java runtime (Road Raider and Cave Stalker). The rest are backlog designs, not registered `EntityType` values or loot tables. The system should evolve toward 30–60 actual variants over multiple vertical slices.

**Tier:** I normal, II advanced, III elite, IV miniboss, V boss. Distinct abilities should primarily reuse Magic's effect system, Combat's damage attribution and reusable behavior goals, not one Java file per species.

| Family | Tier I | Tier I–II | Tier II | Tier III | Tier IV | Tier V |
|---|---|---|---|---|---|---|
| Border Raiders | **Road Raider** (patrol, retreat; implemented) | Shield Sentry (block/hold) | Poacher (ranged, reposition) | Raider Bannerlord (squad buffs) | Bridge Reaver (rush/guard-break) | Ironclad Warlord (phase duel) |
| Burrow & Spider | **Cave Stalker** (flank, climb; implemented) | Burrowling (burrow escape) | Venom Weaver (web zones) | Nest Sentinel (protect eggs) | Brood Mother (reinforcements) | Silk Sovereign (multi-anchor boss) |
| Restless Dead | Grave Wanderer (slow pursuit) | Bone Archer (ranged cover) | Rot Knight (poise tank) | Tomb Cantor (support hexes) | Grave Herald (summon waves) | Ossuary Regent (phase arena) |
| Fungal Swarm | Spore Mite (swarmer) | Mycelial Guard (defensive) | Blight Thrower (spore projectile) | Root Enchanter (healing) | Fungal Colossus (zone denial) | Heart of the Grove (mycelial network) |
| Flamebound | Emberling (rush) | Cinder Hound (flank pack) | Ash Marksman (ranged) | Pyre Cultist (caster) | Molten Bulwark (hazard aura) | Furnace Colossus (telegraphed boss) |
| Marshborn | Mire Crawler (slow trap) | Fen Hunter (ambush water) | Reed Spearman (reach) | Bog Witch (debuff) | Mire Lurker (hidden miniboss) | Drowned Matriarch (tidal arena) |
| Arcane Cult | Initiate (melee+sigil) | Rune Adept (ranged magic) | Leech Acolyte (resource drain) | Hexbinder (crowd control) | Astral Harbinger (ritual support) | Void Hierophant (multi-stage spell boss) |
| Ancient Constructs | Rust Warden (guard) | Clockwork Hound (patrol) | Ballista Automaton (ranged) | Aegis Golem (shield) | Siege Walker (weakpoint boss) | Archive Titan (multi-part boss) |

## Planned reusable packages

- **Locomotion:** grounded biped; climbing arthropod; burrow; aquatic; aerial; stationary construct. Pathfinding profiles must be host-specialized, not approximated with direct teleports.
- **Combat role:** vanguard, skirmisher, archer, ambusher, artillery, controller, healer/support, summoner, leader, boss.
- **Sensors:** visual LOS; hearing noise; damage alert; fixed patrol markers; scent/trail with expiring memory; allied local reports.
- **Tactics:** advance, seek cover, hold, retreat, investigate, flank, windup/cast, defend teammate, regroup, disengage.
- **Resources:** vanilla weapon/armor/shield and shared Combat mitigation; Magic mana/cooldown adapter when supported; deterministic loot table and existing Character XP ledger.
- **Difficulty:** level tier, role modifiers and count budgets; never scale arbitrary damage without Combat's authoritative hit path.
- **Identity:** versioned `mkm:enemy/...` definitions; migration alias list for renames; stable IDs in saves and quest references; data definitions can evolve without per-variant Java subclass.

## Production sequencing

1. **V1**: Road Raider + Cave Stalker; prove runtime, rendering fallback, chunk lifecycle, 40-mob perf.
2. **V2**: One archer and one support caster on common Combat/Magic contracts; no duplicate projectile damage.
3. **V3**: A true boss with telegraphed phases and MNAE rig integration, plus faction/squad message semantics.
4. **V4**: Encounter Director spawn budgeting, authored encounter reservations and persistence, loot-table-driven reward composition.
5. Only then expand catalog incrementally by biome/faction and performance evidence.

Acceptance for every new family: one server/AI test, one renderer/rig test, one persistence/restart test, one reward/dupe test, one 40-mob mixed-arena regression check. No automatic 60-type code generation.
