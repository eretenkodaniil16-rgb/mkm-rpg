# Player Modifier System v1

## Status

Implemented as the first Combat Core calculation boundary.

The system converts persistent Character Core attributes into a complete immutable `PlayerModifierSnapshot`. The snapshot is derived state and is intentionally **not persisted**.

Current calculation:

`modifier base + primary-attribute contributions -> hard safety clamp = snapshot value`

Equipment, weapons, skills, buffs and temporary effects are not connected yet. They will become additional sources later; Combat Core consumers should continue reading the snapshot rather than CharacterData fields directly.

## Definition contract

Every `PlayerModifierId` has exactly one `ModifierDefinition` containing a semantic unit, neutral base value, hard minimum/maximum and PRIMARY/SECONDARY contribution coefficients.

## Attribute curve

Persistent attributes retain the storage range `1..100` and neutral value `10`.

Modifier calculation uses:

`effectiveDelta = sign(delta) * 30 * abs(delta) / (30 + abs(delta))`

This keeps early investment meaningful while reducing runaway scaling at extreme values. Attribute 10 always contributes exactly zero.

## Aggregation

For each primary attribute:

1. calculate effective delta;
2. read PRIMARY/SECONDARY influence edges;
3. add `effectiveDelta * tierCoefficient`;
4. clamp every finished modifier to its hard safety bounds.

Multiple primary attributes may contribute to the same modifier and currently add before the final clamp.

## Neutral values

At all attributes = 10, examples are:

- max health 20;
- physical power 100%;
- melee/ranged damage 100%;
- attack speed 100%;
- max stamina 100 and regeneration 10/s;
- max mana 100 and regeneration 5/s;
- magic power/spell damage 100%;
- weak-point damage 125%.

These are engineering defaults, not final balance.

## Presentation and diagnostics

Character Sheet reads this calculated snapshot instead of a separate preview formula class.

`/mkm modifiers` prints a compact server-side snapshot for physical offense, stamina and magic.

## Tests

Gradle/JUnit tests verify registry completeness, neutral defaults, physical/magic build separation, resource-cost direction, hard bounds and diminishing returns.

## Next integration

Combat Core should consume `PlayerModifierSnapshot` in a real Minecraft hit/damage context. Weapon/equipment contributions should become new modifier sources rather than changing CharacterData or bypassing the calculator.
