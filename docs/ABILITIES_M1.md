# MKM RPG — Abilities / Magic M1 (draft integration contract)

**Baseline:** Minecraft 26.3 / Fabric / Java 25, based on **feature/action-combat-c2-state-hud**. This is an isolated vertical slice, not a complete spellbook.

## Decision and comparison

- Class per spell: flexible, but repeats resource, cooldown and validation logic.
- Arbitrary JSON scripting: flexible content, but insecure and difficult to audit for server authority.
- **Chosen: validated immutable definitions + allowlisted Java effects.** Data contains parameters; server handlers execute validated outcomes. Future data packs can map stable ability IDs to approved effect handlers without accepting executable scripts.

## Implementation and ownership

- **AbilityDefinition**: immutable mkm:* ID, ACTIVE/PASSIVE, targeting, resource kind, effect, cast/cooldown ticks, magnitudes.
- **AbilityCatalog**: five examples; only mkm:renewal is currently enabled at runtime.
- **ManaLedger / AbilityRules**: immutable, pure mana regeneration, per-spell cooldowns, atomic spending and delay; tick-based.
- **AbilityAttachments**: separate persistent, death-copied, owner-synced Fabric attachment mkm:ability_resources. No CharacterData migration.
- **AbilityService**: server-only eligibility check, resource payment and use of native Minecraft healing.
- **CastAbilityRequest**: bounded ID-only intent. Client cannot submit healing, damage, mana, duration, target coordinates or stats.
- **MkmAbilityHud**: synchronized mana and readiness. H key invokes renewal. Client cannot authorize results.

Character Core owns STR/DEX/VIT/END/INT/WIL/PER, XP and PlayerModifierSnapshot. Combat Core owns action timeline, stamina and the existing damage path. This branch does not change CombatService, CombatState or ActionTimelineRules.

## One playable spell: Renewal

ID: **mkm:renewal**. Active, self-targeted, instant.
Base mana cost **20**, cooldown **60 server ticks** (3 s), base healing **6 HP**. Balance provisional.

Before any debit the server checks the exact executable ID, character alive and not spectator/passenger, C2 action timeline idle, current HP below maximum, cooldown ready and sufficient mana. Accepted casts debit mana and reserve cooldown once and heal via vanilla ServerPlayer.heal. Repeated or forged intent fails to gain free healing.

Existing Character Core modifiers are consumed without a new attribute formula:
- max mana: MAX_MANA
- mana regen: MANA_REGENERATION per second
- post-cast regen delay: MANA_REGEN_DELAY in seconds, converted at 20 ticks/s
- mana cost: MANA_COST multiplier
- cooldown: base ticks divided by COOLDOWN_RECOVERY
- healing: base HP multiplied by HEALING_POWER and HEALING_RECEIVED

Intelligence and Willpower influence these only through the existing PrimaryAttributeInfluence + PlayerModifierCalculator, not new local coefficients.

## Demonstration-only ability definitions

- mkm:arcane_bolt — entity ray + magic damage (INT), **not executable**
- mkm:arcane_barrier — self/barrier (WIL), **not executable**
- mkm:repulsion_wave — cone/control (INT), **not executable**
- mkm:iron_will — passive modifier (WIL), **not active**

Future target modes include SELF, ENTITY_RAY, GROUND_POINT, AREA, CONE and DIRECTION. Server must enforce true raycast, range, occlusion, entity/team/PvP permissions and last-moment revalidation.

## Integration contract before further development

1. **Cast phases:** do not implement a second action state machine. Non-instant casts and interrupts require agreed coordination with existing C2 ActionTimeline, including simultaneous melee/dodge constraints.
2. **Damage and status effects:** reuse vanilla/Combat Core damage sources and mitigation; statuses/barriers require one agreed effect authority instead of arbitrary parallel buff state.
3. **Resource spending:** mana uses this isolated ledger; stamina skills must call the C2 stamina API after a mutually agreed atomic check.
4. **Progression and passives:** Character Core must define unlock/point allocation, passive modifier source aggregation and respec semantics. Renewal is currently available to every player for testing, without invented levels or unlocks.
5. **Equipment and data packs:** equipment grants and spell schools reference stable ability IDs. Data-driven content is validated; handler identifiers are allowlisted, not executable code.
6. **Lifecycle:** mana and cooldown survive relog and copy on respawn as a new persisted attachment. Game times are logical server ticks; reset/rollback, multi-dimension clock and versioned migration need runtime QA and follow-up policy.

## Acceptance and limitations

Unit JUnit checks: metadata validation, finite costs, cooldown scaling, regen delay/max, duplicate spend refusal, insufficient mana, passive and unimplemented cast refusal, immutable/capped cooldown maps.

Manual acceptance after Java 25 CI:
1. Verify Fabric 26.3 dedicated server and real client initialization, owner sync and hotkey/HUD.
2. Full HP H: no charge; damaged player H: once-heal + mana reduction + 3 s cooldown.
3. Spam H/malformed ID, attempt during R combo/G dodge, spectator and dead: no effect or cost.
4. Low mana blocks cast; regeneration restores. Test relog and death/respawn for resource and cooldown.
5. Verify client key rebinding and no regression in C2 combos, rolls, melee or HUD.
6. Verify extended INT/WIL builds, multiplayer and save persistence.

**Not implemented:** casting time execution, active projectile/raycast damage, AoE, passive application, learned abilities, spell schools, skill tree, mana potions, final HUD/layout, real 3D cast animation. Unit tests alone cannot certify dedicated-server safety or in-world playability.

Keep PR draft. Do not merge without review, CI and hands-on in-game testing.
