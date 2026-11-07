# Combat Fencing Schools M1 — three tactical sword traditions (prototype)

Status: **DRAFT**, stacked on Combat Entity Contract M2 PR #59 → Combat MNAE M1 PR #36 → integrated Fabric PR #30. Do not merge into historical NeoForge main. Keep CharacterData persistence, native LMB, mining, server-owned R combos and MNAE skeleton unchanged.

## Intent, scope and limits

Introduce **small, verifiable gameplay elements inspired by fencing schools** rather than proclaim a faithful reconstruction of medieval/early-modern fighting systems. All three selected schools use existing Minecraft **SWORD** in the server-confirmed MAIN_HAND. This is an experiment in posture, pressure, guard economy and distance management, not an extra weapon, combo damage hook, parry collider, reach change, iframe mechanic or authored animation.

| School ID | Historical inspiration / stance cue | Existing derived-stat deltas from ONE calculator | Gameplay cost / trade-off |
| --- | --- | --- | --- |
| `liechtenauer` | German fencing, `mkm:guard/vom_tag` | MELEE_DAMAGE +0.04, STAGGER_POWER +0.15 | STAMINA_COST +0.12 (all existing stamina actions cost more) |
| `fiore` | Italian armizare, `mkm:guard/porta_di_ferro` | BLOCK_STABILITY +0.20, STAGGER_RESISTANCE +0.10 | MELEE_DAMAGE −0.06 |
| `destreza` | Iberian Verdadera Destreza, `mkm:guard/angulo_recto` | ARMOR_PENETRATION +0.06 (only MKM resistance), DODGE_COST −0.10 | STAGGER_POWER −0.10 |
| `none` | unchanged vanilla/MKM sword combat | none | none |

These are **balance hypotheses** expressed as additive PlayerModifierId deltas, not historical numeric claims. `ARMOR_PENETRATION` only bypasses MKM custom physical resistance, never vanilla armor/toughness; Destreza therefore has a narrower role against unmodified vanilla mobs. Guard bonuses help only when vanilla actually confirms a shield block; no unapproved magical sword parry is introduced. Sidearm/offhand sword does NOT activate any school if selected MAIN_HAND is empty or holds a tool.

## Player workflow / input

Players (no admin cheats required) use:

```
/mkm fencing
/mkm fencing liechtenauer
/mkm fencing fiore
/mkm fencing destreza
/mkm fencing none
/mkm modifiers
```

A requested selection is validated **on the server**: actor must be alive, not spectator/passenger, not currently using an item/shield and the original ActionTimeline must be idle. A non-NONE selection requires a real sword in MAIN_HAND via `EquipmentRules.presentationFamily`. A successful change has **20 game ticks (~1 s) warmup** before modifiers become active and an **80-tick (~4 s) change cooldown**, including switching to NONE. Repeating the selected name does not reset the clock or buy additional buffs. All selections are bounded enum values, not arbitrary client stat values.

A selected school becomes *inactive without removal* when a different item is equipped in MAIN_HAND, the player is not alive or they become a spectator. It becomes active again only when the sword is held and warmup has completed. On death the selected school resets. School selection is intentionally **volatile between logins** for this experimental phase; it is not in CharacterData v3/v4, so no saved-world migration or downgrade occurs.

## Contract and authority

`FencingSchool` is immutable metadata: stable school ID, MNAE **semantic** guard cue, and `CombatModifierContributions.Contribution` in Kind.STATUS. No new calculator exists: `EquipmentSnapshots.snapshot` collects native equipment once, adds the single server-valid school status contribution if any, then calls `PlayerCombatModifierAccess.snapshot` → original `PlayerModifierCalculator.calculate`. Character's race/class sources from M2 can coexist: distinct bounded source IDs are aggregated once and final clamping remains in Character. The chosen stance uses **no new CombatState ledger** or alternative shield math; current `GuardRules`, `PoiseRules` and direct player melee scaling consume the already-derived values in the existing server CombatEvents/CombatService path.

`FencingSchoolState` is a transient Fabric attachment `mkm:fencing_school` synchronized to **the owning player only** using `AttachmentSyncPredicate.targetOnly()`. The server alone updates it through the /mkm command, and MNAE may safely read this cue locally. `FencingSchoolRules.effectiveSchool(state, actualMainhandFamily, tick)` is the **same eligibility check** for both client presentation and server application. `visualGuardCue` is a semantic key, **NOT an existing native clip**; MNAE must not claim it rendered a distinct stance unless an actual authored clip exists. No player model, skeleton, Blender files or pose mixins were edited here.

Original `Minecraft.startAttack` / `player.attack` remain unchanged: ordinary LMB uses vanilla damage, range, hit/cooldown, shield and mining logic. R still uses the original one-time server-selected mob attack on ActionTimeline ACTIVE; the fencing state only changes the original derived stats, never schedules another attack. Local AttackPresentationEvents and pose markers **never authorize damage or grant a school**.

## Technical and regression verification

New JUnit contract suites:
- 4 IDs, 3 immutable source maps, intended semantic guard cues; no mixed or stacked schools;
- idle/alive selection, 20-tick warmup, 80-tick cooldown, no reset via repeated selections, a blocked switch during R or shield use;
- sword-only MAIN_HAND, not offhand sword, axe, mace, bow, shield, pickaxe or empty slot; unsupported items get no bonuses or false visual cue;
- exactly original player stats with NONE; German pressure increases existing PoiseRules buildup and CombatRules melee scale, Fiore improves existing confirmed-shield guard cost for both ×1.0/×1.5, Destreza changes original armor penetration/dodge cost without movement speed or attack speed modification;
- PlayerModifierCalculator baseline parity and identical final caps for all schools, no duplicate STATUS contribution;
- existing M2 server-only special attacks, World/AI defense, R C3 timeline and MNAE no-double-damage CI gate remain unchanged.

CI on this draft branch should run Java25 Gradle/JUnit, Fabric API embedded JAR, server boot and client under Xvfb, C3 animation QA, the one-damage-hook static check, and Windows installer packaging. The test matrix **does not yet prove** actual PvP or visible third-person guard animation.

### Manual acceptance required (not yet done)
1. Survival sword in actual MAIN_HAND, another sword in OFF_HAND with empty main hand, axe, pickaxe, shield, bow; /mkm fencing show and reject as specified.
2. Two players PvP, including left-handed players, switch attempts during active R, while shielding and during mining; verify no second damage event and vanilla block breaking unchanged.
3. Shield + Fiore against a confirmed source of 10 damage with server `-Dmkm.guard.costFactor=1.0` vs `1.5`; compare against exact item/character stability and stamina ledger. Validate shield durability and enchantments.
4. German stagger buildup on the same mob, with native armor/poise, no hit from an animation cue.
5. Destreza against a registered armored test mob vs ordinary vanilla zombie; no artificially enlarged reach, rerouted targets or changed base vanilla armor formula.
6. Relog, death/respawn, dimension change, dedicated server and 2-player command isolation; server-selected guard should not leak to another player's owner-only UI.
7. Only once an actual MNAE guard clip exists, controlled 60-fps F5 comparison with correct mainhand item; `visualGuardCue` alone is insufficient evidence.

## Integration handoffs

**Character:** races/classes remain authoritatively resolved by Character stream. This M1 selection is universal for testing and NOT a persistent class unlock; Character M4 may later impose trusted unlock prerequisites. Never store animation poses or school tick timestamps in character save records. Compose equipment + class/race + the one school STATUS in the single calculator without double-counting.

**Animation / MNAE:** owner-synced `CombatAttachments.FENCING_SCHOOL` carries `FencingSchoolState`; call `FencingSchoolRules.visualGuardCue(state, EquipmentRules.presentationFamily(actualMainhandId), gameTick)` for read-only guard cue, respect empty Optional and no authored guard animation fallback. Keep `WeaponPresentationResolver` as weapon/hand/action source. Do not create a separate weapon classifier, damage packet or fabricate a rendered clip.

**AI/World:** `EnemyCombatProfile` and existing server Mob adapter from M2 remain independent. Schools are currently only ServerPlayer postures; a special Mob must not acquire player progression/stamina or these STATUS modifiers without a separate tested server-only policy.

**Magic:** schools never alter spell level, school of magic, elemental damage or mana. `PlayerModifierCalculator` stays single source for any future class school passive. Keep only confirmed server effects in AbilityService.

**QA/Integration:** stack PR #59 before this fencing branch. Existing integrated Character v4 PR #62 and Enemy AI #56 are separate stacks; never merge this branch directly into historical NeoForge main. Do real two-player combat and world-save migration testing before shipping.

### Read-only fencing technique vocabulary for MNAE

`FencingSchoolMotionCues.forEvent(fencingState, originalAttackPresentationEvent, localTick)` now provides an **optional** named technique for an already-existing normal LMB or future server-origin R-combo presentation event. The pattern is semantic only, not a rendered clip, not a hit packet and never a new attack. Stages 1/2/3: Liechtenauer `oberhau/zwerchhau/unterhau`, Fiore `mandritto/riverso/punta`, Destreza `tajo/reves/estocada`. These are illustrative choreography labels, not assertions of exact historical technique execution. The selector rejects early warmup, unknown/non-sword mainhand, offhand-only sword, legacy no-context events, canceled/interrupted sequences and unsupported action kinds. It preserves the original attack's left/right physical arm. **If MNAE has no authored clip for the cue, it must not claim an animation was played**; keep the native fallback and do not add damage or alter action timings. The MNAE damage-authority static gate now includes both pure fencing rules and motion cues.
