# Magic M4.1 — spell circles, schools and level-based mana

**Status:** draft tuning + compiled metadata layer, **not** a spell-tree release.  
**Source:** Magic M4 PR #58, Character Core on integrated Fabric PR #42
(CharacterData v3), design compatibility with Character v4 PR #35.
Do not install this experimental branch on a v4 saved world.
This work does not modify CharacterData, MNAE, damage hooks or the Combat
ActionTimeline. M4's breaking C2S protocol still requires matched mod builds.

## Three different levels — do not conflate them

1. **Character level**: existing Character Core `CharacterData.level()`,
   1..100, derived from saved MKM XP with `ProgressionRules`; two
   attribute allocation points per RPG level. Vanilla XP is separate.
2. **Spell circle/tier**: magic complexity **I..X**; unlock gates
   `SpellProgression.minimumLevel(tier)`. This metadata does not
   grant or execute the spell by itself.
3. **Spell proficiency/mastery** (future only): practice, specialisation,
   learned upgrades and optional upcasting must have a separate
   Character-owned persistence/API design. It is NOT implicit in level
   or tier, and is not implemented in this draft.

| Circle | Earliest character level |
| --- | ---: |
| I | 1 |
| II | 10 |
| III | 20 |
| IV | 30 |
| V | 40 |
| VI | 50 |
| VII | 60 |
| VIII | 70 |
| IX | 80 |
| X | 90 |

Level 100 is intended for final specialisations/upgrades and the cap,
not an invented circle XI. `SpellProgression.meetsLevel` rejects unknown
abilities and invalid levels. `AbilityGrantPolicy.executable` requires:
**authoritative grant + enough character level + ACTIVE kind + explicit
server executor registration**. AI has a separately validated actor
level (currently prototype level I). A tier grant or UI selection
never authorises an unimplemented effect or damage.

## Schools

Schools are independent of classes/races, targets and resource type.
One class can learn several schools; one school supports multiple
mechanisms. They are namespaced metadata via `SpellSchool` and
`SpellProfile`, not separate execution engines:

| School | Role | Example |
| --- | --- | --- |
| Restoration | Heal, cleanse, regeneration | `mkm:renewal` — circle I, executable |
| Evocation | Elemental/arcane direct effects and impulses | `mkm:arcane_bolt` — I; `mkm:repulsion_wave` — IV, both disabled |
| Abjuration | Wards, shields, barriers | `mkm:arcane_barrier` — III, disabled |
| Conjuration | Summoning and conjured constructs | No executor/ability yet |
| Transmutation | Matter, mobility and alteration | No executor/ability yet |
| Illusion | Concealment, decoys and sensory distortion | No executor/ability yet |
| Enchantment | Mind, focus and magical influence | `mkm:ancestral_focus` — II, disabled |
| Necromancy | Death and life-force manipulation | No executor/ability yet |
| Divination | Detection, foresight and knowledge | No executor/ability yet |
| NONE | Not a spell; warrior/racial passive | `iron_will`, `stone_resilience`, tier 0 |

Naming, school balance and actual class spell lists require design
review. No passive stat application or new damaging spell is enabled by
these descriptors. A future multi-school tag can be introduced only with
a schema version and validated catalog migration.

## Mana: what grows, what does not

**Existing verified Character Core constants:**

- `PlayerModifierId.MAX_MANA` baseline **100 MP**, cap **300 MP**,
  primary influence from **WILLPOWER**, coefficient **+3 MP per
  effective Willpower point**.
- Soft-capped effective characteristic delta in Character Core:
  `effectiveDelta(WIL) = sign(WIL-10)*30*abs(WIL-10)/(30+abs(WIL-10))`.
  Baseline WIL 10 -> no additional capacity; WIL 40 -> effective
  +15 points -> **+45 MP** from Willpower.
- Base mana regen **5 MP/s**, WIL coefficient +0.20 MP/s per effective
  Willpower point; WIL 40 => **8 MP/s** (before other modifiers).
  WIL also reduces regen delay and mana cost and increases healing power.
  INT raises MAGIC_POWER, SPELL_DAMAGE, CAST_SPEED, SPELL_RANGE,
  and similar spell efficacy; **not MAX_MANA**.
- Approved Character level range **1..100**, **2 attribute points per
  level**. Therefore allocating points to Willpower already increases
  mana indirectly. That bonus is **not** reproduced in Magic code.

**New M4.1 prototype level growth**:
```text
derivedMana = EquipmentSnapshots.snapshot(player, character).get(MAX_MANA)
levelBonus  = (CharacterData.level() - 1) * 1.0 MP
maxMana     = clamp(derivedMana + levelBonus, 0, 300)
```
`ManaCapacityPolicy.maximum` uses the authoritative **equipped**
`PlayerModifierSnapshot` and level, adding only the level term once,
then reusing Character Core's `ModifierDefinitions.MAX_MANA` cap. All
server paths (login, 5-tick regen, Renewal cost validation) and owner
HUD call the same method. `RenewalParameters.from(stats, level)` keeps
a one-argument compatibility overload with level I for AI fixtures.
AI prototype retains its fixed archetype `PROTOTYPE_TERMS`: player
level does not inflate hostile resources.

**Illustrative results without magic equipment:**

| Character level | WIL 10 | WIL 40 |
| ---: | ---: | ---: |
| 1 | 100 MP | 145 MP |
| 10 | 109 MP | 154 MP |
| 50 | 149 MP | 194 MP |
| 100 | 199 MP | 244 MP |

Equipping a future `MAX_MANA +100` item at level 100 with WIL 40
would result in 300 MP after the cap, **not 344 MP**. Current shipped
equipment is primarily physical: this is a formula test for a future
validated modifier delta, **not a newly registered item**.

**Important balance semantics:** Leveling does **not** instantly refill
mana, reset cooldowns, increase spell damage, or reduce mana cost.
The current numeric mana in the existing persistent `ManaLedger`
remains unchanged and naturally regenerates toward the new cap; a
downward cap change is clamped on a later server resource update.
Mana regeneration rate, cost multipliers, healing and cast speed remain
driven by the existing snapshot (particularly WIL/INT and equipment).
No additive class-specific mana bonus is assigned yet, since
CharacterData v3/v4 has no authoritative class selection.

**Design caveat:** Character Core currently clamps MAX_MANA inside
`PlayerModifierCalculator`. Adding a nonnegative level bonus after
this snapshot and clamping again produces the intended result for
the current equipment rules. Future negative mana penalties, multi-class
bonuses, or mana curses must be combined **before one final clamp** in
Character Core's reviewed aggregator. This is a controlled temporary
adapter, not a rival Character modifier calculator.

## Spell mana costs

Current numeric **baseCost** values in `AbilityCatalog` remain the
source of truth for the respective prototypes. Character WIL's
`MANA_COST` multiplier is applied by existing `AbilityRules.scaledCost`.
No arbitrary new universal tier-based price is applied retroactively,
because doing so would silently change accepted Renewal cost and
partially implemented Combat/Magic semantics.

When implementing new circles, designers should generally make
higher-circle effects cost more but use *explicit per-spell server
definitions* to allow cheap utility and expensive area effects within
the same school/tier. Propose costs alongside target validation,
effect magnitude, area/range and cooldown; server enforces all values.
Later **upcasting** a lower-tier spell to a higher tier requires
a separately reviewed server-side rank/permission/cost/effect contract,
not a client-selected damage coefficient or MNAE animation marker.

## Persistence, multiplayer and QA acceptance

- No CharacterData fields edited. Character v4's `claimedRewardIds`
  remain outside this branch and must be preserved when a v4-compatible
  integrated project is later built. Do not run the v3 prototype on
  real v4 worlds.
- Same `PlayerModifierSnapshot` source for Combat and Magic. Avoid
  reapplying Willpower or equipped bonuses in the school/tier policy.
- Server owns level and cost. Owner HUD uses owner-synced
  `CharacterData.level()`; never trusts an arbitrary C2S level.
- Tests: ten unlock thresholds, tier-0 nonspells, missing catalog
  profiles, invalid level/rank, max mana at levels 1/10/50/100,
  Willpower 40 and item delta cap, identical server/HUD computation,
  unchanged Renewal cost and healing at same WIL, v2 C2S duplicate
  gate and existing M3 APPLIED confirmation tests.
- Manual: XP level-up with low/current mana, RPG points invested in
  WIL, relog and copy-on-death mana ledger, equipment swapping,
  GUI scaling/F5, two clients, dimension switch, AI tagged healer.
  CI passing alone is not a substitute for gameplay balance tests.
