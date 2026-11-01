# Equipment D1 — vanilla compatibility and modifier composition

Status: isolated **draft** implementation based on `feature/action-combat-c2-state-hud`; not yet runtime-accepted. Minecraft 26.3 / Fabric / Java 25. Do not merge before CI and dedicated-server/client acceptance.

## Existing damage path: verified code boundary

C2 `LivingEntityCombatMixin` transforms only *direct* `DamageTypes.PLAYER_ATTACK` before vanilla defenses. `CombatEvents.modifyIncoming` currently multiplies vanilla damage by `PHYSICAL_POWER × MELEE_DAMAGE`, and by 1.65 for an eligible Shift-heavy action. Server-side R combo ultimately calls `ServerPlayer.attack(mob)`, retaining normal vanilla attack strength, weapon attributes, target selection, line of sight, armor, shields, enchantments and immunity handling. `AFTER_DAMAGE` drives mob poise only for actual positive damage. No hidden hit/dodge rolls.

## D1 integration

- Equipped vanilla mainhand/offhand and head/chest/legs/feet are read from the existing player inventory. Other inventory slots never grant stats. There is no custom save attachment or secondary attribute calculator.
- The platform-neutral `EquipmentRules` converts recognized *vanilla* IDs to additive modifier deltas; `EquipmentSnapshots` obtains those IDs and passes contributions to the existing `PlayerModifierCalculator`. One final `ModifierDefinition` clamp produces an immutable `PlayerModifierSnapshot`.
- Weapons: swords (slashing/melee), axes (slashing/stagger), spear-like weapons and trident (piercing/penetration), mace (blunt/stagger), bows/crossbows (piercing/ranged; ranged damage path still **not wired**). Unknown/modded items have neutral RPG additions but retain their vanilla behavior.
- Quality is currently a derived material **grade**, not a separate tier, random rarity, affix, durability or serialized upgrade: standard; refined (diamond, trident/mace); masterwork (netherite). It is not displayed as a rarity in the inventory.
- Vanilla armor/toughness remain authoritative and are **never added to `PlayerModifierId.ARMOR`**; armor pieces add small MKM physical resistance (max +3.2 percentage points for full netherite). Netherite also adds a small `STAGGER_RESISTANCE` contribution. Shield grants `BLOCK_STABILITY` in the snapshot, but a *custom stamina block/parry mechanic is not yet active*.
- Direct player melee damage against another **player** passes through MKM physical + selected type resistance, with positive resistance partly bypassed by `ARMOR_PENETRATION`; vanilla armor and shield then resolve normally. This parameter does **not** ignore Minecraft armor/toughness. Direct melee against mobs retains the existing C2 path with offensive equipment bonuses, and poise uses updated stagger power. Other physical damage, arrows, TNT, spells, mobs hitting players and NPC-specific defenses remain unchanged pending explicit integration.
- `CombatService`, `ActionTimelineRules`, animation clips/markers, movement and timings are untouched. Current stamina costs are unchanged because D1 equipment does not modify stamina cost modifiers. Snapshot calculations are stateless and read current equipped items, avoiding stale per-player caches or relog drift.

## Interface contracts for parallel streams

- **Progression (B)** owns character schema and attribute allocation. D1 only supplies `Map<PlayerModifierId, Double>` contributions; eventual progression buffs should use the same aggregator's contribution API, not rewrite CharacterData.
- **Magic (C)** may consume `EquipmentSnapshots.snapshot(player, character)` for item-derived caster stats, but may not invoke `PhysicalMitigationRules` for spells; use a separate magical damage/resistance path in the same overall authoritative damage pipeline. D1 adds no magic power or mana values.
- **Animation (A)** may read weapon family/physical category for presentation selection but must **not** use animation events to authorize damage or change physical swing timing. Mainhand `EquipmentRules.profile(itemId, Slot.MAIN_HAND)` is the read-only presentation classification.
- **UI/UX (G)** can render derived grade and family and show composed `PlayerModifierSnapshot`. This is not a completed gear inventory interface.
- **Future D2**: data-driven item-definition table including `mkm:*`, validated custom quality component/affixes, type-aware defense for NPCs, block/parry stamina transactions, vanilla-armor penetration if justified by an independently tested integration hook, ranged profiles and full multiplayer smoke.

## Test and acceptance

Pure JUnit tests cover item category/grade, correct slot rules, stacking, mixed character+equipment clamp, melee type resistance, penetration and invalid inputs. Runtime checklist: hold sword/axe/mace and compare `/mkm modifiers`; change armor slots and inspect physical resistance; shift-heavy and R combo against mob; PvP with/without armor and shield; rejoin world; dedicated server boots without any client class dependency; F5/camera actions unaffected.

**No runtime validation is claimed until a successful GitHub CI result and actual game smoke.**
