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


## M1 hardening / engineering review (November 2026)

### Shared validation and combat interlock

`RenewalEligibility` is a pure deterministic preflight used by the authoritative
`AbilityService` and client HUD. This avoids conflicting "READY" indicators.
The final server gate also checks the existing C2 `CombatState.heavyReadyTick`
in addition to the action timeline, preventing healing during heavy-strike
recovery. It does not change CombatService, ActionTimelineRules, movement,
vanilla attack recovery or any animation code. This is a narrow interim
read-only interlock, **not** a generalized cast-phase contract.

All effect amounts, costs, resources, health and timestamps come from the
logical server. The client supplies only a bounded identifier. The
`AbilityIntentCodec` enforces ASCII, a maximum packet length and stable
`mkm:` IDs without path-traversal segments. Unknown well-formed IDs are
denied by the server's explicit executable allowlist.

### Persistence and clock invariants

`ManaLedgerCodec` persists `schema_version=1`. The pre-review unversioned
format decodes as schema 0 and is upgraded on the next save. Unknown future
schema values fail decoding instead of silently being rewritten as v1.
Codec roundtrip and legacy migration have isolated JUnit tests.

Logical server game time is used for mana regeneration and deadlines.
If a restored world moves game time backward, the remaining regeneration
delay and cooldown durations are rebased relative to the new clock; the
resource does not become permanently locked until an unreachable future
timestamp. Expired cooldown records are removed when reserving a new cast
to avoid exhausting the bounded 64-entry ledger. The 200-tick catch-up cap
on mana regeneration remains an intentional economy/abuse-prevention
policy, **not** a real-world elapsed-time reward. Offline regeneration,
death penalties, cross-dimension time behavior and long-term migrations
remain decisions for later milestones.

### HUD and accessibility

Mana, hotkey and cast-state strings have English and Russian localization.
The HUD reads the current keybinding instead of always displaying H.
States include ready, full health, busy (C2 or heavy recovery), low mana
and cooldown. Layout is informational only; exact runtime appearance
must still be verified at different GUI scales and alongside C3 combat
animation/HUD branches.

### CI and QA scope

The branch CI executes Java 25 Gradle compilation/JUnit, verifies that the
mod JAR embeds Fabric API, boots a dedicated server, smoke-tests a client
under Xvfb and validates/packages the Windows installer. This is a
**bootstrap smoke test**, not a gameplay render, interactive cast or
multiplayer test. Pure tests cover resource/cooldown limits, world clock
rebasing, repeated requests, stable ID validation, wire encoding,
schema migration and server-equivalent preflight rules.

**Outstanding mandatory manual gate before merge:**
1. Real Minecraft 26.3 Windows client cast H while injured, full health,
   under cooldown, with low mana, while comboing and recovering from heavy.
2. Spam H and attempt invalid/forged network IDs on a controlled server;
   check no free healing/duplicate debit and no server disconnect.
3. Save/relog, game-time rollback, respawn and dimensional travel;
   inspect persisted mana and cooldowns.
4. Verify two clients do not see each other's private mana ledger.
5. Check nondefault keybindings, Russian localization, GUI scaling and
   side-by-side visual compatibility with the independent C3 branch.
6. Explicit integration review with Character Core progression/unlocks
   and Combat Core damage/interrupt contracts.

Do not merge before acceptance; the branch remains a draft PR.
