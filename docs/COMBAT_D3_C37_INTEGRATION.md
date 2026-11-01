# MKM Combat compatibility probe: Equipment D1-D3 + C3.7 (incl. C3.6)

**Status:** independent integration **draft**, based on `feature/c3-7-visible-combat-diagnostics`, which includes C3.6. No merger to historical NeoForge `main`. Runtime Minecraft 26.3 / Fabric / Java 25.

## Exact conflict resolution

Source copies of D1-D3 provide `EquipmentRules`, `EquipmentSnapshots`, `PhysicalMitigationRules`, `GuardRules`, `ShieldGuardService`, `PoiseRules`, `PendingHitProvenance` and the shared `PlayerModifierCalculator` overload. The combined `CombatEvents` is D3's damage/defense/poise logic **plus** C3.6's `HitConfirm` event emission. It does not use C3's original `Map<LivingEntity,Boolean>` flag. The C3.7 `CombatService` keeps combo/dodge/timeline timings and replaces only its central stats getter with `EquipmentSnapshots.snapshot` to avoid ignoring equipped stats.

C3.6/C3.7 client receiver, `HitFeedbackRules`, VFX renderer, C3 JSON animation clips, `ActionTimelineRules`, pose mixins and all movement timings are UNCHANGED. The existing clientbound `HitConfirm` packet remains cosmetic. It emits only after actual finite positive, unblocked **direct player melee** damage to a **Mob**, and only while the server action timeline is `LIGHT_COMBO/ACTIVE`, with `ServerPlayNetworking.canSend`. A miss, shield block or vanilla non-combo hit does not create a confirm. PvP visual confirmation is deliberately not extended by this slice.

## Damage and protection contract

Exactly one existing `LivingEntityCombatMixin` at `hurtServer` argument entry is responsible for MKM pre-vanilla transform. Direct player melee scales once by `PHYSICAL_POWER * MELEE_DAMAGE` and optional paid Shift-heavy multiplier. For PvP only, one MKM typed resistance pass occurs after offensive scaling. Incoming direct mob melee and arrow/trident damage against players receive one defensive MKM resistance pass, no RPG offensive multipliers. Vanilla attack selection/strength, enchantments, shields, armor/toughness and death processing remain in the original Minecraft pipeline. `ARMOR_PENETRATION` bypasses only MKM positive physical resistance; it does **not** bypass vanilla armor.

One confirmed vanilla shield block debits guard stamina in `AFTER_DAMAGE`, with server cooldown for exhaustion. This is after resolution: it does NOT retroactively cause blocked damage. `STAGGER_RESISTANCE` reduces incoming player poise buildup, while stagger remains a short Slowness effect (not a true action interruption).

## Future launch-time ranged snapshot, NOT ACTIVE

`RangedShotSnapshot` captures immutable weapon ID, physical and ranged multipliers, MKM penetration, physical type and logical server tick. Capture is allowlisted to vanilla bow, crossbow or thrown trident at the **actual projectile spawn/throw transaction**. Pure `scaleVanillaProjectile` and `scaleAndMitigate` compute a single optional pre-vanilla transform; no runtime hook is installed. In later projectile integration: attach this **only on the server** to the projectile at launch, keep it independent of later weapon changes, and replace D3's fallback arrow/trident MKM resistance branch instead of applying both. Ensure owner, projectile identity, world/dimension, despawn and modded-projectile policy. Projectiles without a trusted snapshot continue vanilla offense and D3 defensive-only resistance. No client-supplied damage multipliers.

## Independent QA/checklist for checking chat

1. Assert `CombatEvents` has **one** normal `HitConfirm` send guarded by actual positive, unblocked mob damage + LIGHT_COMBO/ACTIVE. C3.7 `HitFeedbackParticles` unchanged.
2. Confirm `PENDING_HEAVY` uses `PendingHitProvenance<LivingEntity,DamageSource>`, source-object identity and one-tick bound; verify cancel/alternate source tests.
3. Assert exactly one `LivingEntityCombatMixin`, no second melee transform and no shadow `PlayerModifierCalculator` or equipment-specific character data store.
4. Check `CombatService.modifiers` uses `EquipmentSnapshots` and C3.7 dodge uses direction in `ActionTimelineRules.beginDodge(..., direction)`; all animation assets unchanged.
5. Run `./gradlew clean build` under Java 25, inspect JUnit test counts, nested Fabric API JAR, and dedicated-server smoke.
6. Manual vanilla armor/shield/enchant check with same attacker and weapon, neutral/nonneutral armor and RPG resistances; confirm damage once and no double armor mitigation.
7. Shield-facing and non-facing hits, guard cooldown/reuse, zero-stamina, double shields, repeated hits, respawn/reconnect, high latency PvP.
8. PvE (zombie/skeleton/armored mob), PvP (two players with unequal equipment), projectiles (bow/crossbow/trident), block and poise, LIGHT_COMBO hit-confirm only; visual F5 first/third person.
9. Verify separate progression/magic/quest branches' MkmMod registration, CharacterSheet and MkmCommands conflicts before final shared integration; no tested claim that these branches already co-build.

## Handoff to main chat

Implemented: D1-D3 server equipment/guard/poise integration on top of C3.7 with C3.6 authoritative hit feedback preserved, one canonical snapshot path, pure immutable ranged capture prototype and focused JUnit tests. Remaining: real in-game client QA, actual projectile launch hooking, inventory item grade data components and typed mob/NPC resistances, coordinated B/C/E integration. Suggested next: accept this combined *draft* as an integration probe, run full runtime acceptance, then reconcile B/C/E in dependency order. Do not merge to `main` until the full Fabric baseline is reviewed.

## Follow-up correction: improvised melee with ranged weapons

The held bow/crossbow has a piercing **projectile** profile, but if the player hits with it in direct melee, its physical type is **blunt**. `EquipmentRules.meleePhysicalType` is used by `EquipmentSnapshots.heldPhysicalType`; `RangedShotSnapshot` retains piercing projectile damage. This avoids misapplying piercing resistance to a melee bow strike and preserves all vanilla damage mechanics.
