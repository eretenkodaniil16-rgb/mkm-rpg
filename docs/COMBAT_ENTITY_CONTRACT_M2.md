# MKM Combat Entity Contract M2 — server combat core and optional enemy features

**Status:** draft, based on Combat MNAE PR #36 (which is based on Fabric integration PR #30). Java 25, Minecraft 26.3, Fabric. The actual old NeoForge `main` is NOT a valid merge target. No newly authored weapons, character classes, enemy entities, ability damage or animations are shipped by this patch.

## 1. What was audited and who owns it

| Existing component | Current owner | May be shared with non-player LivingEntity? | M2 decision |
| --- | --- | --- | --- |
| `WeaponPresentationResolver`, `AttackPresentationEvent` | read-only Combat→MNAE hand/item/animation metadata | yes, only with real action hand and entity held items | canonical family from `EquipmentRules`; expose direct-melee damage type but never damage authority |
| `EquipmentRules` | vanilla item families and *slot-specific* bonuses | item classification yes; player equipment stat deltas no | `presentationFamily` and `meleeDamageType` are the only item taxonomy source |
| `CombatService` | `ServerPlayer` stamina, R timeline, G dodge, Shift heavy, server target selection | **NO**: it uses `CharacterData`, player HUD/network, stamina and movement | entirely unchanged |
| `PlayerModifierSnapshot` / `PlayerModifierCalculator` | player attributes, equipment, future race/class bonuses | **NO**: mobs need no full player mana/XP/char schema | one calculator and one final clamp, no enemy duplicate |
| `ShieldGuardService` / `GuardRules` | verified vanilla shield blocks of `ServerPlayer` | shield cost **NO** until a mob resource system exists; pure formula is reusable only with explicit resource contract | ×1.0 default, ×1.5 startup experiment only; duplicate receipts preserved |
| `PhysicalMitigationRules` | one additional MKM typed physical resistance before native armor | **YES**: its pure numeric core | share exactly one primitive resistance calculation, not two divergent formulas |
| `PoiseRules` + server `POISE` attachment | buildup/stagger on LivingEntity | **YES** | optionally inject a registered mob's poise resistance and attack stagger power; default 1.0 unchanged |
| `CombatEvents` + `LivingEntityCombatMixin` | one server-owned vanilla `hurtServer` modification path and `AFTER_DAMAGE` | **YES** | opt-in mob profile incorporated in same callback; no additional `hurt`, attack or projectile callback |
| `HitConfirm` | server→owner C3 confirmed R-combo hits only | server proof required | no cosmetic preview can generate confirmed damage; no normal-LMB S2C observer sync in this PR |

## 2. Architecture alternatives

**Option A — one generic Player Combat Core for all LivingEntity.** Attractive short-term API uniformity, but mobs would inherit player-only stamina, mana, character save codec, progression and player HUD. A duplicate character setup for every zombie causes allocations, net synchronization, save migrations and potentially duplicates vanilla attack paths. Rejected.

**Option B — completely separate AI damage calculator.** Avoids player records, but inevitably creates conflicting armor/poise/item-family formulas and risks double application of vanilla armor and MKM resistance. Rejected.

**Chosen — shared immutable physical/action contract + separate opt-in server mob adapter.** `EquipmentRules` classifies gear, `PhysicalMitigationRules` is the *one* additional MKM physical defense formula, `PoiseRules` remains the *one* poise calculator, and `CombatEvents` remains the *one* server damage hook. Only `ServerEnemyCombatAdapter` converts a real Mob to an optional immutable `EnemyCombatProfile`. No new damage engine is introduced.

## 3. Weapon, action hand and damage channel

`EquipmentRules.presentationFamily(itemId)` is the canonical semantic weapon family for both hands; it delegates to existing MAIN_HAND item classification for *presentation only*. `EquipmentRules.profile(itemId, OFF_HAND)` still independently determines any **actual offhand equipment bonus**.

`EquipmentRules.meleeDamageType(itemId)` describes a **direct impact**, not a fired projectile: sword/axe = slashing; mace/fists/shield/pickaxe/bow/crossbow used as melee = blunt; trident/spear melee = piercing. A fired arrow/trident still uses separate `RangedShotSnapshot` launch-time PIERCING profile (snapshot remains non-wired). Do not fetch the current mainhand when a projectile hits.

`WeaponCombatProfiles.forFamily(family, isActuallyEmpty)` consumes the family already returned by EquipmentRules: no separate weapon registry. `WeaponPresentationResolver.Item` now includes `meleeDamageType` alongside item ID and family. `AttackPresentationEvent.meleeDamageType()` exposes an optional read-only value when a real `HandContext` is present, while old no-context event constructors remain supported.

Vanilla LMB = **MAIN_HAND** regardless of left/right preferred physical arm. A sword in OFF_HAND with empty MAIN_HAND is still an unarmed mainhand attack, not an automatic offhand sword strike. Unsupported nonempty pickaxe is not treated as an unarmed fist. MNAE does not supply damage type, penetration, cooldown or hit proof to the server.

## 4. Optional enemy API and explicit server registration

`EnemyCombatProfile` is an immutable bounded set of:
- natural attack's physical type (slashing, piercing, blunt) and delivery (direct melee, projectile, none);
- **bonus** generic physical defense and signed typed resistance/vulnerability, limited to safe ranges; no replacement of native armor;
- poise resistance (0.25–3.0) and offensive stagger power (0–3.0).

`EnemyCombatFeatureRegistry` is empty by default, rejects duplicate IDs and `minecraft:player`, and is sealed before game server starts. **Nothing currently registers a vanilla zombie/skeleton or a new custom enemy**, so ordinary enemies retain exactly their previous vanilla+MKM behavior. Registration example for a future separately approved custom Mob:
```java
ServerEnemyCombatAdapter.registration().register(
    "mkm:armored_warden",
    new EnemyCombatProfile(
        EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
        PhysicalDamageType.BLUNT,
        0.30D,
        Map.of(PhysicalDamageType.SLASHING, 0.10D),
        2.0D,
        1.0D));
```
AI/World must perform registration in **server mod initialization before SERVER_STARTING**. Game code uses `ServerEnemyCombatAdapter.features(Mob)` only on the server. For different armor variants of the *same* vanilla mob type, a future instance-specific provider is required: this type-ID registry alone does **not** describe per-instance variations or persisted elite status.

For a registered enemy damaged by a player, `CombatEvents.modifyIncoming` first retains existing player offense/heavy authorization, then applies the **one** optional MKM resistance calculation before vanilla armor. No new damage call or target selection occurs. Unregistered mobs skip it entirely. On `AFTER_DAMAGE` only real positive unblocked server health damage contributes to PoiseRules; profile resistance changes the amount entering poise and poise resistance adjusts buildup. No custom health, shield, damage immunity, or hitbox changes.

For Mob→Player melee, a registered natural attack type applies only when no recognized melee weapon is actually held; recognized equipped weapons use the same EquipmentRules direct-melee classification. Incoming player resistance continues using the existing typed defense path. Ranged damage and spell effects are unchanged and must have their own server attribution and original launch/cast snapshot when approved.

## 5. Player race/class deltas — one snapshot, no second calculator

The base integration still saves CharacterData schema **v3**; the separately drafted Character M3 #35 uses **v4**. M2 neither changes fields nor migrates old worlds. Never downgrade a v4 world into this branch without a controlled migration.

`CombatModifierContributions.combine(verifiedEquipment, serverFeatures)` accepts one slot-based equipment map and a bounded list of named `RACE`, `CLASS`, `STATUS` contributions. It rejects duplicate `(kind, featureId)` receipts, nonfinite deltas, malformed keys and aggregate overflow; equipment **cannot be supplied a second time** as a feature source. `PlayerCombatModifierAccess.snapshot` delegates exactly once to the original `PlayerModifierCalculator.calculate`, which still owns base attribute scaling and the final safety clamp.

`EquipmentSnapshots.snapshot(player, character)` delegates to this shared bridge with an **empty feature list**, preserving all current player stats. A future Character M3 server actor will verify race/class/status entitlements and pass a trusted list through the existing overload `EquipmentSnapshots.snapshot(player, character, serverFeatures)`. **No race/class bonuses are actually enabled in M2**, and UI previews or client animation events cannot be authoritative passive sources.

Character #35 already has a thin `CharacterModifierAccess` and a canonical `PlayerModifierCalculator`. Integration must retain the calculator, not install a duplicate; this Combat M2 layer is only safe source accounting.

## 6. Future special attack policy — no new skills activated

`ServerSpecialAttackContract.Intent` describes a *potential* native melee or vanilla projectile technique: action ID, delivery, logical active hand, canonical family, damage type, stage and server-owned attempt ID. `ServerEvidence` requires verified server thread/actor/target/dimension/reach+LOS/cooldown/resources/once-only receipt. `assess` returns:

| Origin | Evidence | Result |
| --- | --- | --- |
| MNAE/client local prediction | any | `PRESENTATION_ONLY` |
| Server AI / Character | missing any required check | `REJECTED` |
| Server AI / Character | all checks verified | `READY_FOR_VANILLA` |

`ServerAttackReceiptGate` rejects reused, stale or out-of-order server-issued attempt IDs for each actor; it is pure and **not yet attached to a live skill**. These contracts never attack, hurt, allocate movement or select a mob on their own. AI or Character must route future accepted actions to the *existing* vanilla resolution **once**, using the attacker's actual server-controlled target/weapon. Magic must keep its spell damage/heal ownership and must not apply physical resistance again on top of magical effects. Cosmetic events are emitted only AFTER authoritative outcomes.

A policy result is **not sufficient** to bypass vanilla attack speed, reach, shields, protection enchantments, PvP restrictions or immunity. Implementers must use the actual native damage route and test it, not synthesize a second `DamageSource` from animation impact markers.

## 7. Shield ×1.5 study

Current server startup `-Dmkm.guard.costFactor=1.5` remains an explicit opt-in; default is ×1.0. Existing `GuardRules` calculates `min(18, 4 + blockedDamage × 0.5) × STAMINA_COST × experiment / max(0.25, BLOCK_STABILITY)`.

| Blocked damage | Control ×1.0 | Opt-in ×1.5 |
| ---: | ---: | ---: |
| 2 | 5.0 | 7.5 |
| 6 | 7.0 | 10.5 |
| 10 | 9.0 | 13.5 |
| 20 | 14.0 | 21.0 |

`ShieldGuardService` still records a per-attempt server receipt on `ALLOW_DAMAGE` and consumes it once after a real vanilla shield block; it does not award a second health hit or invent guard damage. PvE/PvP source distinction does not enter the *formula*, but server gameplay tests are still required to establish Fabric callback ordering, survival shield actual blocking, same-tick repeated hits and protection/durability behavior. The proposed ×1.5 is not a validated live balance recommendation.

## 8. Tests, QA and release gates

Added pure JUnit:
- vanilla weapon and offhand matrix including bow/pickaxe/mace/trident, true air vs unsupported item, left physical hand and predictable cosmetic event;
- numeric equivalence of old player resistance versus shared player/mob math, physical vulnerabilities and penetration;
- synthetic ordinary vs armored vs high-poise enemy with different damage and stagger outcome, safe range validation and sealed registry behavior;
- equipment-only baseline modifiers exactly equal original calculator, additive race/class/status contributions, duplicate feature rejection and final cap;
- server-special-action negative checks, prediction-only policy, per-actor replay protection, positive server validation;
- PvE/PvP-equal guard cost ×1.0/×1.5, same-source duplicate receipt and separate genuine attempts;
- `HitFeedbackRules.validServerConfirmedHit` requires SERVER_AFTER_DAMAGE evidence, nonblocked and finite positive health damage.

CI must complete Java25 JUnit, animation timing regression, dedicated server boot, Xvfb client boot with unchanged MNAE hooks, Fabric API packaging and Windows installer syntax. **These are not player-operated multiplayer or actual shield visual checks**.

Manual review gates (not accomplished by this PR): two-player PvP sword/axe/trident and shield, creative/survival mining, native attack recharge, looting and armor enchantments; registered custom mob with native armor, weaknesses and server-approved poise; latency/predictive vs authoritative hit feedback; offhand and left-handed MNAE visible evidence; real World E2.1/Character v4 migration and recovery. No main merge before full integrated QA.

## 9. Workstream handoffs

**Character:** implement trusted role/race/class entitlement→`CombatModifierContributions.Contribution` maps only on server, then call existing calculator ONCE. Avoid persistent combat pose state in CharacterData. Integrate with #35 v4 migrations without ever loading a v4 world into v3 encoder.

**AI/World:** register approved special mob profiles by entity type during init, using `ServerEnemyCombatAdapter.registration()`; future per-instance elite traits need a persistence-backed provider and bounded cleanup. AI selects targets/cooldowns server-side, then invokes vanilla attack once, never MNAE to damage.

**Magic:** keep independent spell damage and Renewal confirmation. For elemental/magical attacks use Magic's own typed defense/cast outcomes, not the physical `EnemyCombatProfile.mitigateDirectPhysical` path and not a second hit.

**Animation/MNAE:** consume `AttackPresentationEvent.handContext`, `meleeDamageType()`, profile family and start/step as read-only data. `HitConfirm` remains a server→owner cosmetic acknowledgment for real MKM combo health hits; normal LMB preview is not a confirmed hit. No skeleton, Blender, held-item transform or client renderer changes here.

**QA/Integrator:** rebase stacked drafts carefully (#36→this M2; #35 v4 and #39/45 World remain separate). Test configured and unconfigured mob paths, left/offhand, shields and attack cooldown. Check repeated hit provenance and confirm no second damage callback or duplicate player armor defense; validate all platform-specific changes on dedicated server and two clients.

**Mob direct-melee type hardening:** `ServerEnemyCombatAdapter.selectDirectMeleeType(mainhandId, registeredProfile)` always prioritizes a recognized equipped melee weapon and only uses a registered *natural* damage type if `AttackDelivery.DIRECT_MELEE` applies. A projectile-only profile cannot change a real `DamageTypes.MOB_ATTACK` into a piercing attack by accident. The pure selection method is tested without loading Minecraft entities.

**Stagger delivery validation:** the mob's offensive stagger multiplier is also selected only if a registered profile is `DIRECT_MELEE` and the actual server damage source is direct mob melee; an otherwise projectile-only profile remains neutral for any unexpected physical melee action. A ranged projectile does not use this direct melee path.

### Automated no-double-damage architecture gate

GitHub Actions now runs `tools/combat/qa/combat_authority_selfcheck.py`. It fails if presentation LMB/MNAE contracts or future special-attack policy contain known damage, movement or server packet calls, or if more than one `LivingEntity.hurtServer` variable modification is present in MKM mixins. This is **static source inspection**, not a proof against arbitrary third-party mods or Minecraft/Fabric callback duplication. Server combat integration and live PvP/mining tests remain mandatory.
