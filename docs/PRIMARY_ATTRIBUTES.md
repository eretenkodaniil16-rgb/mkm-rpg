# Primary Attributes

## Status

This document defines the first reviewable MKM primary-attribute candidate set. It is deliberately introduced before any final numerical formulas or persistence migration.

The current persisted `CharacterData` fields `strength`, `dexterity` and `vitality` remain the Character Core prototype. They are not yet migrated because changing the save schema before the attribute set is reviewed would create unnecessary compatibility work.

The candidate set contains seven combat/build attributes:

| ID | Name | Abbrev. | Role |
| --- | --- | --- | --- |
| `strength` | Strength | STR | Raw physical force, heavy melee output, stagger and knockback. |
| `dexterity` | Dexterity | DEX | Attack cadence, precision-oriented physical combat and mobility. |
| `vitality` | Vitality | VIT | Health, bodily resilience, recovery and resistance to physical pressure. |
| `endurance` | Endurance | END | Stamina economy, sustained exertion, blocking endurance and movement endurance. |
| `intelligence` | Intelligence | INT | Offensive spell scaling, spell shaping and magical penetration/control. |
| `willpower` | Willpower | WIL | Mana economy, magical resistance, concentration, support magic and hostile-status resistance. |
| `perception` | Perception | PER | Weak-point exploitation, ranged effectiveness and situational attack quality. |

This is a candidate set rather than a final balance commitment. Values, caps, point costs and level-up rules are intentionally unresolved.

## Why Vitality and Endurance are separate

Health and stamina are intentionally separated so one attribute does not become the universally optimal defensive and action-economy choice.

- Vitality is about surviving damage and recovering from bodily stress.
- Endurance is about how long the character can keep performing demanding actions.

This also leaves room for a heavy build with high Vitality but mediocre stamina, or a mobile fighter with high Endurance but lower raw health.

## Why Intelligence and Willpower are separate

Magic is planned as a first-class path rather than one generic `magic` statistic.

- Intelligence primarily governs offensive spell construction, scaling and technical control.
- Willpower primarily governs magical resource economy, concentration, defensive magic and resistance.

A future spell school may deliberately scale from one, both, or neither. The spell system must not assume every magical effect uses the same attribute formula.

## Attribute-to-modifier influence

The mapping below defines **influence relationships only**. It does not define coefficients.

A modifier may receive input from more than one primary attribute and may also receive larger contributions from equipment, skills, buffs, weapon rules or spell definitions.

### Strength

Primary influences:

- `physical_power`
- `melee_damage`
- `armor_penetration`
- `stagger_power`
- `knockback_power`
- `block_stability`

### Dexterity

Primary influences:

- `attack_speed`
- `ranged_damage`
- `backstab_damage`
- `movement_speed`
- `dodge_speed`
- `dodge_distance`
- `interaction_speed`

### Vitality

Primary influences:

- `max_health`
- `health_regeneration`
- `healing_received`
- `physical_resistance`
- `slashing_resistance`
- `piercing_resistance`
- `blunt_resistance`
- `stagger_resistance`
- `knockback_resistance`
- `status_duration_resistance`

### Endurance

Primary influences:

- `max_stamina`
- `stamina_regeneration`
- `stamina_regen_delay`
- `stamina_cost`
- `dodge_cost`
- `sprint_speed`
- `jump_power`
- `fall_damage_resistance`
- `block_stability`

### Intelligence

Primary influences:

- `magic_power`
- `spell_damage`
- `magic_penetration`
- `spell_critical_damage`
- `cast_speed`
- `spell_range`
- `spell_area`
- `spell_projectile_speed`
- `spell_duration`
- `spell_status_power`

### Willpower

Primary influences:

- `max_mana`
- `mana_regeneration`
- `mana_regen_delay`
- `mana_cost`
- `magic_resistance`
- `elemental_resistance`
- `status_resistance`
- `status_duration_resistance`
- `concentration_stability`
- `healing_power`
- `barrier_power`

### Perception

Primary influences:

- `ranged_damage`
- `critical_damage`
- `weak_point_damage`
- `backstab_damage`
- `spell_range`

## Modifiers intentionally not assigned to a primary attribute yet

Some accepted player modifiers should remain predominantly equipment-, skill- or system-driven until their design is clearer:

- `armor`
- `block_efficiency`
- `critical_resistance`
- `cooldown_recovery`
- `experience_gain`
- `loot_bonus`

This avoids forcing every modifier to scale from a base attribute simply for symmetry.

## Social and reward attributes

A social attribute such as Charisma/Presence and a reward-oriented attribute such as Luck are intentionally not added to this combat/build set yet. Dialogue checks, reputation, trade and loot systems should define their own needs before another persistent primary attribute is introduced.

If one of those systems proves that a new primary attribute is valuable, it should be added before the public persistence schema is declared stable.

## Implementation boundary

The Java domain vocabulary mirrors this candidate through `PrimaryAttribute`, `PlayerModifierId` and `PrimaryAttributeInfluence`.

This code is deliberately formula-free. It gives later Character Core and Combat Core work compile-time identifiers and a reviewable influence graph without prematurely changing player saves.

## Next step

Review this seven-attribute set and the influence graph. After approval:

1. choose base values and allowed ranges;
2. define how attribute points are acquired;
3. define initial attribute-to-modifier coefficients and diminishing returns/caps;
4. migrate `CharacterData` from the STR/DEX/VIT prototype to the approved schema;
5. update the Character Sheet to display the full authoritative set.