# MKM Showcase-01 / Character M5 — Issue #75

**Scope:** live character divergence inside Minecraft, not a larger unused race catalog.
**Branch:** `feature/character-showcase-race-class-m5-issue75`, draft PR #87, stacked
strictly on QA5 PR #69 (Fabric 26.3, Java 25). **Version of QA test artifact:**
`0.0.7-fabric.1`. User decisions: PR #70, `MKM_GDD_USER_DECISIONS_115.md`,
S04–S06; original Character M4 comes from PR #55, with real v4 integration in #69.

## Architecture decisions

### Race models

| Alternative | Benefits | Risks | Decision |
|---|---|---|---|
| Numeric-only race | Easiest to balance | Not felt in gameplay; was the QA5 defect | Reject as sole identity |
| Narrative + active context | Unique investigation methods; server-validated choices; uses World quest state | Requires World dialogue patch; limited to authored POIs | **M5 vertical slice** |
| Full physiology/collision | High identity and replayability | Hitboxes, PvP, armor, MNAE articulation, doors, shaders, multiplayer desync | Later, behind separate engine acceptance |

### Class models

- New Character-only spell/damage code: **rejected**; risks a duplicate combat engine,
  XP authority and client/server disagreement.
- Disable normal Minecraft weapons and H Renewal based on classes: **rejected now**;
  breaks older saves, vanilla mining and existing UI. Restriction policy requires
  explicit approval, respec/migration and anti-exploit QA.
- **Selected:** Character checks `mkm:warrior`/`mkm:mage`; Combat Core owns actual
  stamina+shield+Resistance; Ability Core owns mana+cooldown+Absorption. One public
  V intent carries only a one-byte code, **never client amounts, targets or class ID**.

## Runtime: four combinations are playable

Human / Warrior, Human / Mage, Dwarf / Warrior, Dwarf / Mage: each gets
**one racial authored investigation action and one distinct class activation**.

**Human: "Persuade the caravan"** — talk to real server-tagged Ilra, select the
race-specific choice. Ilra reveals undead/sunlight clue without PER12; World
sets the original `FordQuestState.foundTracks` once. Existing World claim
still computes +25 XP for the clue and pays via Character v4
`grantExperienceOnce`; the race never writes XP.

**Dwarf: "Read stone runes"** — same real NPC choice, but **must equip a vanilla
pickaxe in MAIN_HAND**. Server verifies `ItemTags.PICKAXES` rather than trusting
the disabled UI button. World sets `foundTracks` once; when an accepted
encounter reservation exists, reveals its authoritative coordinate anchor,
even if the chunk is not currently loaded. No world scan/chunk forcing,
loot duplication or XP writer.

**Warrior: "Brace" (V)** — while alive, grounded, idle in Combat timeline
and holding a shield in either main/offhand, CombatService charges 24
base stamina via existing `CombatRules.heavy` (same final
`PlayerModifierSnapshot.STAMINA_COST` as vanilla MKM combat), imposes a
180-server-tick persisted cooldown, and grants real **Resistance I** for
60 ticks (100 after mentoring). Natural Minecraft status VFX and incoming
damage reduction; NO fake/second attack. The normal shield and right-click
guard behavior remain vanilla-compatible. A non-warrior packet cannot reach
this action, even when forged directly.

**Mage: "Arcane Ward" (V)** — new explicit `AbilityCatalog.MAGE_WARD`
`mkm:mage_ward` (instant, self, BARRIER), executed inside AbilityService.
Costs 25 base mana via existing `AbilityRules.spend`, shares the existing
`ManaLedger` cooldown/regeneration contracts, and applies **Absorption I**
for 100 ticks (180 after mentoring). Two real additional yellow hearts
visible in the Minecraft HUD; cooldown 180 ticks. The previously
design-only `mkm:arcane_barrier` remains non-executable. H Renewal stays
available to legacy players and other classes until the Ability team
introduces a negotiated spell-unlock system.

**Trainer: "Ilra's mentorship"** — after the clue is discovered and
`mkm:ford_warden` quest reaches `rewarded`, a **level >=2** origin-selected
character may choose the new stage-3 NPC dialogue option **once**. The server
validates v4/known origin, quest v3 stage, `foundTracks`, character level and
not-previously-unlocked node. Only then a durable `mkm:ford_mentorship`
node is inserted; it extends the class action duration, with no XP/point
refund and no new weapon unlock, no level respec and no respec tree.

## Exact player save contract and backwards compatibility

**Untouched:** `mkm:character` schema v4 with long `experience`, seven
attributes `strength,dexterity,vitality,endurance,intelligence,willpower,perception`,
`earned_points` integer, `allocated_points` seven integers, server-private
`claimed_reward_ids` array. **Untouched:** `mkm:character_origin` v1
`schema_version,revision,race_id,class_id`, persist/copyOnDeath/owner-sync.
World quest `mkm:ford_quest` schema v3 fields unchanged; no floor-based
XP repair.

**New optional** `mkm:origin_progress` player attachment:
```json
{"schema_version":1,"unlocked_nodes":["mkm:ford_mentorship"]}
```
- Default for any existing or new player lacking it: `{"schema_version":1,"unlocked_nodes":[]}`.
- Persistent, copies on death, syncs to owner only, no other player's IDs.
- Immutable, capped at 24 unique length-bounded namespaced IDs.
- Future schema preserved on decode but **not executable** and never re-written
  by the trainer; unsupported unknown IDs preserved inert in v1.
- No visual poses, animation clips, race geometry or field overwrites in
  CharacterData. Reconnect does not re-grant a choice or mastery.
- As with all Minecraft/Fabric attachments, without an actual playerdata
  disk / cross-file crash test, CI codec roundtrip is **not proof** of
  crash-atomic persistence.

**New optional Combat-only** `mkm:warrior_brace_ready`: server-private
persistent long logical world tick, default 0; intentionally not death-copied.
One respawn clears this temporary cooldown, but does not clear the origin
or the learned mentor node. Mage ward cooldown remains in the existing
Ability Core persistent `ManaLedger`.

### Important runtime tradeoffs

1. These two racial paths converge on the same `foundTracks` clue and XP
   reward by design to preserve the existing quest. The social/gear gate,
   dialogue and dwarf coordinate lead are distinct. Further cultural quests
   must be authored by World.
2. The warrior uses vanilla Resistance I; it reduces damage beyond shields,
   not a custom parry/counterattack animation. Balance numbers provisional.
3. Mage ward uses vanilla Absorption I, not a free Arcane Bolt or damaging spell.
   Magic M4 #58 may later provide additional authoritatively implemented
   spells; **never** copy its v2/v3 CharacterData over QA5 v4.
4. Class state and response live on the server. Client does not send
   stamina/mana/cooldown/damage/healing amounts.
5. NPC dialogue session nonce is still consumed by
   `FordDialogueSessions.consume` before the new choices are evaluated.
   `FordDialogueRules` contains the new stage-specific choice IDs 6/7/8.
6. **Test on a backed-up copy of 0.0.5-fabric.4/QA5 world**. Never downgrade
   the *migrated* world to a previous v3 playerdata codec.

## Manual in-game smoke (not satisfied by automated CI)

Start dedicated server and matching client at the **same SHA and JAR**.
Do not use a production world. Normal survival is sufficient except for one
GM setup step and optional item provisioning. In a new world as a GM, stand
on flat solid ground, difficulty non-Peaceful, then run:
```
/mkmworld place
/mkm stats
/mkmworld status
```
Ilra spawns nearby (if terrain collision permits). Optional GM speed-up:
`/give @s minecraft:shield` and `/give @s minecraft:iron_pickaxe`.
These are **test setup**, not required mechanics in survival: players can craft
them normally.

1. Press K → “Race / Class” and choose Human/Warrior. Confirm.
   Reopen K, inspect the permanent choice and listed **V** action.
   Equip a shield, stand grounded, press V. Observe stamina decline,
   Resistance status particles/icon, reduced incoming mob damage and
   server denial if retried before cooldown. Save/quit/relog; selected
   origin must persist.
2. Right click Ilra, choose Human “persuade caravan”. If PER <12 this
   **must still succeed**. Journal J now shows discovered undead clue.
   Fight the correctly attributed raider and complete the original
   quest, or use World QA control to stage a verified completion.
   Finish claim normally: 75/100 XP paid exactly once, never on relog.
3. When level >=2 and stage is rewarded, right-click Ilra, select
   mentorship. Check K origin lesson status: acquired; after cooldown
   V brace lasts 100 instead of 60 ticks. Repeat mentor request:
   no second unlock or XP/point grant.
4. On a **separate player UUID or clean saved world**, choose
   Dwarf/Mage. Equip the pickaxe; Ilra's dwarf rune choice becomes
   enabled, the human persuasion choice does not appear. It cannot
   be activated server-side without the pickaxe. Once accepted,
   reading runes prints reserved encounter coordinates. V creates
   yellow Absorption hearts and reduces the mana meter, not stamina.
   After mentor, ward lasts 180 vs 100 ticks.
5. Repeat as Human/Mage and Dwarf/Warrior to prove all **2×2**
   crossings retain cultural+class interaction. Default PER10,
   PER12, already-discovered clues, old unselected origin and
   future/unknown origin schemas must fail/succeed correctly.
6. With **two simultaneous players**, attack each other's raider,
   relog one client, and forge/duplicate race/class+dialogue+V
   C2S: no class cross-casting, no duplicate quest XP, no other player's
   origin progress. Observe real shield+leftclick mining unaffected.
7. Die and respawn, relog, restart server; compare snapshot of all
   seven stats, XP, earned/spent credits, reward receipts, origin v1,
   `origin_progress`, vanilla inventory and quest v3 before/after.
   Check no fields are removed. Test unknown/future schema only on
   an isolated throwaway copy.
8. Record F5 and first-person in-game footage of shield brace,
   absorption hearts and world dialogue. CI cannot claim visual
   acceptance without this evidence.

### QA acceptance ledger

| Gate | Automated | Live Minecraft |
| --- | --- | --- |
| 2×2 pure origin permissions and mentor predicates | JUnit | 4 actual player selections |
| XP/claimed receipts untouched | codec regression | relog/death/2P + claim replay |
| World session/quest stage validation | JUnit / existing tests | right-click NPC, fake C2S |
| Warrior stamina and cooldown | CombatRules JUnit | shield V and damage |
| Mage mana and cooldown | AbilityRules JUnit | absorption hearts visible |
| origin_progress disk+sync | Codec/stream JUnit | real death/relog |
| 0.0.7-fabric.1 packaging | Gradle + PowerShell CI | same JAR client and server |
| Visual proof and 60fps footage | **NOT** | Pending |

**Integration/QA:** MkmMod, MkmClient, OriginClassActionService,
FordQuestService, FordDialogueScreen, CombatService, AbilityService, locales,
and packaging are contested files. Cherry-pick **by method and authority
boundary**, not overwrite competing World #78 / Combat #74 / Magic #76
work. PR remains draft and out of NeoForge `main` until reviewed.
