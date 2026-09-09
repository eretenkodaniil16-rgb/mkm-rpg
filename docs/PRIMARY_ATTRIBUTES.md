# Primary Attributes

## Status

The seven-attribute set is now the accepted Character Core baseline and is persisted by `CharacterData` schema v2.

| ID | Name | Abbrev. | Role |
| --- | --- | --- | --- |
| `strength` | Strength | STR | Raw physical force, heavy melee output, stagger and knockback. |
| `dexterity` | Dexterity | DEX | Attack cadence, weapon handling, mobility and agile physical combat. |
| `vitality` | Vitality | VIT | Health, bodily resilience, recovery and resistance to physical pressure. |
| `endurance` | Endurance | END | Stamina economy, sustained exertion, blocking endurance and movement endurance. |
| `intelligence` | Intelligence | INT | Offensive spell scaling, spell shaping and magical penetration/control. |
| `willpower` | Willpower | WIL | Mana economy, concentration, magical defense, support magic and status resistance. |
| `perception` | Perception | PER | Weak-point exploitation, ranged effectiveness and situational attack quality. |

Default stored value is `10`; the current defensive storage clamp remains `1..100`. Those limits are storage invariants, not final balance caps.

## Persistence

Schema v2 persists all seven attributes:

- `strength`
- `dexterity`
- `vitality`
- `endurance`
- `intelligence`
- `willpower`
- `perception`

Schema-v1 saves remain loadable. END/INT/WIL/PER were absent in v1, so the v2 codec supplies the neutral default value `10` when those fields are missing. The in-memory value is then treated as schema v2.

## Influence tiers

Attribute-to-modifier relationships now distinguish two tiers:

- **Primary** — the attribute is intended to be a major source of that modifier.
- **Secondary** — the attribute may contribute, but should not dominate the modifier by itself.

This tiering resolves intentional double dependencies without committing to final numerical coefficients.

### Strength

Primary:

- `physical_power`
- `melee_damage`
- `stagger_power`
- `knockback_power`

Secondary:

- `armor_penetration`
- `block_stability`

### Dexterity

Primary:

- `attack_speed`
- `ranged_damage`
- `backstab_damage`
- `dodge_speed`
- `interaction_speed`

Secondary:

- `movement_speed`
- `dodge_distance`

### Vitality

Primary:

- `max_health`
- `healing_received`
- `physical_resistance`
- `stagger_resistance`
- `knockback_resistance`

Secondary:

- `health_regeneration`
- `slashing_resistance`
- `piercing_resistance`
- `blunt_resistance`
- `status_duration_resistance`

### Endurance

Primary:

- `max_stamina`
- `stamina_regeneration`
- `stamina_regen_delay`
- `stamina_cost`
- `dodge_cost`
- `block_stability`

Secondary:

- `sprint_speed`
- `jump_power`
- `fall_damage_resistance`

### Intelligence

Primary:

- `magic_power`
- `spell_damage`
- `magic_penetration`
- `cast_speed`
- `spell_range`
- `spell_status_power`

Secondary:

- `spell_critical_damage`
- `spell_area`
- `spell_projectile_speed`
- `spell_duration`

### Willpower

Primary:

- `max_mana`
- `mana_regeneration`
- `mana_regen_delay`
- `mana_cost`
- `magic_resistance`
- `status_resistance`
- `concentration_stability`
- `healing_power`
- `barrier_power`

Secondary:

- `elemental_resistance`
- `status_duration_resistance`

### Perception

Primary:

- `critical_damage`
- `weak_point_damage`

Secondary:

- `ranged_damage`
- `backstab_damage`
- `spell_range`

## Intentional multi-attribute dependencies

The following overlaps are deliberate:

- `block_stability`: primarily END, secondarily STR;
- `ranged_damage`: primarily DEX, secondarily PER;
- `backstab_damage`: primarily DEX, secondarily PER;
- `spell_range`: primarily INT, secondarily PER;
- `status_duration_resistance`: secondary input from both VIT and WIL.

Equipment, skills, buffs, weapon rules and spell definitions may later contribute much more strongly than primary attributes to specific modifiers.

## Modifiers still not driven by a primary attribute

These remain predominantly system/equipment/skill-driven for now:

- `armor`
- `block_efficiency`
- `critical_resistance`
- `cooldown_recovery`
- `experience_gain`
- `loot_bonus`

## Current preview formulas

The Character Sheet exposes deliberately simple tuning previews so all seven attributes can be tested in-game:

- VIT -> maximum health preview;
- STR -> physical power preview;
- DEX -> attack speed preview;
- END -> maximum stamina preview;
- INT -> magic power preview;
- WIL -> maximum mana preview;
- PER -> weak-point damage preview.

These previews are **not authoritative Combat Core formulas**. Their purpose is to verify persistence, synchronization, presentation and the qualitative role of each attribute before final coefficients, diminishing returns and caps are accepted.

## Next step

Define the modifier-definition layer: units, base values, aggregation semantics, caps and first numerical primary/secondary coefficients. Only then should Combat Core consume the derived modifier snapshot for real damage, stamina, movement and magic rules.
